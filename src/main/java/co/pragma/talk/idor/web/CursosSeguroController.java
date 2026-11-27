package co.pragma.talk.idor.web;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.pragma.talk.idor.dominio.CatalogoRepository;
import co.pragma.talk.idor.dominio.MatriculasClient;
import co.pragma.talk.idor.seguridad.AccesoDenegadoException;
import co.pragma.talk.idor.seguridad.Auditoria;
import co.pragma.talk.idor.seguridad.ColegioAutorizadoResolver;
import jakarta.servlet.http.HttpServletRequest;

/**
 * ✅ VERSIÓN SEGURA.
 *
 *  - El colegio autorizado sale SOLO del header que validó el API Gateway, nunca de la URL.
 *  - /colegios/{dane}/cursos: si el DANE del path no es el autorizado → 403, sin consultar nada.
 *  - /cursos/{codigo}/estudiantes: se verifica que el curso pertenezca al colegio autorizado
 *    consultando el servicio de matrículas. Si ese servicio falla → 403 (fail closed).
 *  - Siempre el mismo 403 genérico (no revela si el recurso existe) y cada intento queda en auditoría.
 */
@RestController
@RequestMapping("/api/seguro")
public class CursosSeguroController {

    private final CatalogoRepository catalogo;
    private final MatriculasClient matriculas;
    private final ColegioAutorizadoResolver resolver;
    private final Auditoria auditoria;

    public CursosSeguroController(CatalogoRepository catalogo, MatriculasClient matriculas,
                                  ColegioAutorizadoResolver resolver, Auditoria auditoria) {
        this.catalogo = catalogo;
        this.matriculas = matriculas;
        this.resolver = resolver;
        this.auditoria = auditoria;
    }

    @GetMapping("/colegios/{dane}/cursos")
    public Map<String, Object> cursos(@PathVariable String dane, HttpServletRequest request, Principal user) {
        String colegioAutorizado = resolver.resolver(request);
        if (!colegioAutorizado.equals(dane)) {
            negar(user, request, "listar-cursos", "el DANE de la URL no es el de la sesión", colegioAutorizado, "colegio " + dane);
        }
        var colegio = catalogo.colegio(colegioAutorizado).orElseThrow(() -> new AccesoDenegadoException("colegio inexistente"));
        return Map.of("colegio", colegio, "cursos", catalogo.cursosDeColegio(colegioAutorizado));
    }

    @GetMapping("/cursos/{codigo}/estudiantes")
    public Map<String, Object> estudiantes(@PathVariable String codigo, HttpServletRequest request, Principal user) {
        String colegioAutorizado = resolver.resolver(request);

        List<String> cursosDelColegio;
        try {
            cursosDelColegio = matriculas.codigosDeCursos(colegioAutorizado);
        } catch (RuntimeException e) {
            // Fail closed: si no puedo verificar la pertenencia, no entrego los datos.
            negar(user, request, "ver-estudiantes", "no se pudo verificar la pertenencia del curso", colegioAutorizado, "curso " + codigo);
            return Map.of();
        }
        if (!cursosDelColegio.contains(codigo)) {
            negar(user, request, "ver-estudiantes", "el curso no pertenece al colegio de la sesión", colegioAutorizado, "curso " + codigo);
        }
        var estudiantes = catalogo.estudiantes(codigo).orElseThrow(() -> new AccesoDenegadoException("curso inexistente"));
        return Respuestas.estudiantes(catalogo, codigo, estudiantes);
    }

    private void negar(Principal user, HttpServletRequest request, String operacion, String motivo,
                       String colegioAutorizado, String recurso) {
        auditoria.accesoDenegado(user == null ? null : user.getName(), operacion, motivo,
                colegioAutorizado, recurso, request.getRemoteAddr());
        throw new AccesoDenegadoException(motivo);
    }
}
