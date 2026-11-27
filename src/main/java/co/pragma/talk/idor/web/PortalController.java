package co.pragma.talk.idor.web;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import co.pragma.talk.idor.dominio.CatalogoRepository;
import co.pragma.talk.idor.dominio.MatriculasClient;
import co.pragma.talk.idor.dominio.UsuariosColegios;
import co.pragma.talk.idor.seguridad.AccesoDenegadoException;
import co.pragma.talk.idor.seguridad.Auditoria;

@RestController
public class PortalController {

    private final UsuariosColegios usuarios;
    private final CatalogoRepository catalogo;
    private final Auditoria auditoria;
    private final MatriculasClient matriculas;

    public PortalController(UsuariosColegios usuarios, CatalogoRepository catalogo, Auditoria auditoria,
                            MatriculasClient matriculas) {
        this.usuarios = usuarios;
        this.catalogo = catalogo;
        this.auditoria = auditoria;
        this.matriculas = matriculas;
    }

    /** Usuario actual y los colegios que puede gestionar (para el selector de la plataforma). */
    @GetMapping("/api/me")
    public Map<String, Object> me(Principal user) {
        var perfil = usuarios.perfil(user.getName());
        List<Object> colegios = perfil.colegios().stream()
                .map(dane -> (Object) catalogo.colegio(dane).orElseThrow()).toList();
        return Map.of("usuario", user.getName(), "nombre", perfil.nombre(), "cargo", perfil.cargo(), "colegios", colegios);
    }

    // ---------- Solo para la barra de presentador de la demo ----------

    @GetMapping("/api/demo/auditoria")
    public List<Auditoria.Evento> auditoria() {
        return auditoria.recientes();
    }

    @GetMapping("/api/demo/matriculas")
    public Map<String, Object> estadoMatriculas() {
        return Map.of("disponible", matriculas.isDisponible());
    }

    @PostMapping("/api/demo/matriculas")
    public Map<String, Object> cambiarMatriculas(@RequestParam boolean disponible) {
        matriculas.setDisponible(disponible);
        return Map.of("disponible", matriculas.isDisponible());
    }

    @RestControllerAdvice
    static class Errores {
        /** Siempre el mismo 403: no revela si el recurso existe ni por qué se negó. */
        @ExceptionHandler(AccesoDenegadoException.class)
        ResponseEntity<Map<String, String>> denegado() {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No tienes acceso a este recurso"));
        }
    }
}
