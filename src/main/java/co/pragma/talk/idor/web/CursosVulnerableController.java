package co.pragma.talk.idor.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import co.pragma.talk.idor.dominio.CatalogoRepository;

/**
 * ❌ VERSIÓN VULNERABLE (IDOR / Broken Access Control).
 *
 * El API Gateway ya validó que el usuario puede gestionar el colegio del header X-Colegio.
 * Pero este controller confía en los datos de la URL:
 *  1. /colegios/{dane}/cursos          → usa el código DANE del path, no el del header.
 *  2. /cursos/{codigo}/estudiantes     → el código del curso no dice de qué colegio es, y no se verifica.
 */
@RestController
@RequestMapping("/api/vulnerable")
public class CursosVulnerableController {

    private final CatalogoRepository catalogo;

    public CursosVulnerableController(CatalogoRepository catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/colegios/{dane}/cursos")
    public Map<String, Object> cursos(@PathVariable String dane) {
        // Vulnerable: consulta el colegio que venga en la URL
        var colegio = catalogo.colegio(dane).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return Map.of("colegio", colegio, "cursos", catalogo.cursosDeColegio(dane));
    }

    @GetMapping("/cursos/{codigo}/estudiantes")
    public Map<String, Object> estudiantes(@PathVariable String codigo) {
        // Vulnerable: devuelve los estudiantes de cualquier curso, sea del colegio que sea
        var estudiantes = catalogo.estudiantes(codigo).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return Respuestas.estudiantes(catalogo, codigo, estudiantes);
    }
}
