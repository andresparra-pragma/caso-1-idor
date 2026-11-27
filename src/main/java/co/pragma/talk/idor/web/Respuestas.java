package co.pragma.talk.idor.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import co.pragma.talk.idor.dominio.CatalogoRepository;
import co.pragma.talk.idor.dominio.Modelos.Estudiante;

final class Respuestas {

    private Respuestas() {
    }

    static Map<String, Object> estudiantes(CatalogoRepository catalogo, String codigo, List<Estudiante> estudiantes) {
        BigDecimal suma = estudiantes.stream().map(Estudiante::promedio).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = estudiantes.isEmpty() ? BigDecimal.ZERO
                : suma.divide(BigDecimal.valueOf(estudiantes.size()), 1, RoundingMode.HALF_UP);
        return Map.of(
                "curso", catalogo.curso(codigo).orElseThrow(),
                "colegio", catalogo.colegioDelCurso(codigo).orElseThrow(),
                "estudiantes", estudiantes,
                "promedioCurso", promedio);
    }
}
