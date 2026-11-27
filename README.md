# Caso 1 · IDOR (control de acceso roto)

Demo de la charla **"Tu código tiene vulnerabilidades, y tus dependencias también"** (PragmaTalk).

Una **plataforma académica para una red de colegios**: cada coordinador gestiona los cursos y estudiantes de *su*
colegio. Un API Gateway valida el colegio del usuario… pero el backend confía en lo que viene en la **URL**.
Cambiando un número, una coordinadora ve los cursos de otro colegio y, desde ahí, los datos de estudiantes menores
de edad.

> ⚠️ **App vulnerable a propósito.** Solo para aprender y demostrar. Escucha únicamente en `127.0.0.1`, usa datos
> ficticios y no debe desplegarse en ningún servidor. El escenario está inspirado en hallazgos reales y anonimizado:
> no contiene nombres de clientes, sistemas ni código real.

## El problema

```
GET /api/vulnerable/colegios/105001000202/cursos     ← código DANE de OTRO colegio en la URL
X-Colegio: DANE-111001000101                          ← el colegio de la usuaria (el gateway lo validó)
→ 200 OK  (cursos del otro colegio)
```

1. **El gateway valida el header, no la URL.** El header `X-Colegio` es legítimo y pasa. El backend vulnerable usa el
   código DANE del path.
2. **El recurso no trae a su dueño.** `GET /cursos/{codigo}/estudiantes` recibe solo el código del curso, que no dice
   de qué colegio es. Con un bucle sobre los códigos se enumeran los estudiantes de todos los colegios.

Categoría: **OWASP A01 – Broken Access Control** · **CWE-639** (Authorization Bypass Through User-Controlled Key).

## La corrección

| | Vulnerable (`/api/vulnerable`) | Seguro (`/api/seguro`) |
|---|---|---|
| `GET /colegios/{dane}/cursos` | usa el DANE de la URL | 403 si el DANE de la URL no es el del header validado |
| `GET /cursos/{codigo}/estudiantes` | devuelve cualquier curso (404 si no existe) | verifica que el curso pertenezca al colegio del header; si no, si no existe o si no se puede verificar → 403 |

- **La identidad del recurso sale solo de lo que ya se validó** (header del gateway o token), nunca de la URL →
  [`ColegioAutorizadoResolver`](src/main/java/co/pragma/talk/idor/seguridad/ColegioAutorizadoResolver.java):
  403 si el header falta, es ambiguo o tiene formato inválido.
- **Si el recurso no trae su dueño, se verifica la pertenencia** contra el servicio de matrículas →
  [`CursosSeguroController`](src/main/java/co/pragma/talk/idor/web/CursosSeguroController.java).
- **Fail closed:** si el servicio de verificación falla, se niega el acceso.
- **Siempre el mismo 403**, exista o no el recurso, para no confirmar qué códigos son válidos.
- **Auditoría** de cada acceso denegado, con los valores saneados para evitar *log injection* (CWE-117) →
  [`Auditoria`](src/main/java/co/pragma/talk/idor/seguridad/Auditoria.java).

Compara [`CursosVulnerableController`](src/main/java/co/pragma/talk/idor/web/CursosVulnerableController.java) con
[`CursosSeguroController`](src/main/java/co/pragma/talk/idor/web/CursosSeguroController.java).

## Cómo correrla

Requisitos: **Java 17+**. No hace falta instalar Maven: el proyecto trae el Maven Wrapper.

```bash
./mvnw spring-boot:run        # Linux / macOS
.\mvnw.cmd spring-boot:run    # Windows
```

Abre <http://127.0.0.1:8082>.

| Usuario | Contraseña | Rol |
|---|---|---|
| `lucia` | `lucia123` | Coordinadora del Colegio San Martín (y del Colegio La Esperanza) |
| `pedro` | `pedro123` | Rector del Colegio Los Andes |

Abajo a la derecha hay una **barra de presentador** (no es parte de la plataforma):

- **Backend: Vulnerable / Seguro:** cambia contra qué implementación se hacen las peticiones.
- **Ver petición:** la última llamada a la API, con el código de la URL frente al del header.
- **Auditoría:** los accesos denegados que registró el backend, y un interruptor para simular la caída del servicio
  de matrículas (fail closed).

## Recorrido de la demo

1. Inicia sesión como `lucia`. La URL es `#/colegios/111001000101/cursos`: los cursos del Colegio San Martín.
2. Con el backend en **Vulnerable**, cambia en la barra de direcciones `111001000101` por **`105001000202`** → cursos
   del **Colegio Los Andes**.
3. **Ver estudiantes** del curso 2001 → documentos, acudientes, correos y notas de estudiantes de otro colegio.
4. **Ver petición:** el header es el de San Martín (aprobado por el gateway); el DANE de la URL, el de Los Andes.
5. Cambia el backend a **Seguro** → la misma pantalla responde **403**. **Auditoría** muestra cada intento.
6. (Opcional) En Auditoría marca **Simular caída del servicio de matrículas** y abre un curso propio
   (`#/cursos/1001/estudiantes`) → también 403: si no se puede verificar, no se entregan los datos.

### Enumeración desde la terminal (Windows PowerShell)

```powershell
.\enumerar.ps1                 # vulnerable: estudiantes de los 3 colegios con la sesión de Lucía
.\enumerar.ps1 -Modo seguro    # seguro: 403 en todo lo ajeno, incluso en cursos que no existen
```

## Tests

```bash
./mvnw test
```

15 tests: el ataque en la versión vulnerable (`IdorFlujoTest`), la corrección (DANE distinto, curso ajeno, curso
inexistente, header ausente, ambiguo o inválido, fail closed, auditoría) y el saneamiento del log (`AuditoriaTest`).

## SAST con Fluid Attacks

[`.github/workflows/sast.yml`](.github/workflows/sast.yml) analiza el código con
[`fluidattacks/sast-action`](https://github.com/fluidattacks/sast-action) (open source, sin cuenta) en cada push a
`main` y en cada PR. Como este repo es vulnerable a propósito, el escaneo **reporta pero no bloquea**: los hallazgos
quedan en **Security → Code scanning**.

Una buena pregunta para la charla: **¿un SAST detecta un IDOR?** Un IDOR es lógica de autorización ("¿este curso es
de este colegio?"), no un patrón de código peligroso. Para atraparlo hacen falta pruebas de autorización (como los
tests de este repo), DAST con usuarios distintos o revisión de código.

## Estructura

```
src/main/java/co/pragma/talk/idor
├── dominio/      datos ficticios: colegios, cursos, estudiantes, servicio de matrículas
├── seguridad/    login, API Gateway simulado, resolver del colegio autorizado, auditoría
└── web/          controllers vulnerable y seguro, /api/me y endpoints de la barra de presentador
src/main/resources/static   la plataforma (login, cursos, estudiantes) y la barra de presentador
enumerar.ps1                script de enumeración para la demo
```

Stack: Spring Boot 3.5 · Spring Security · Java 17.
