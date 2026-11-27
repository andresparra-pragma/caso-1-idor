package co.pragma.talk.idor.dominio;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import co.pragma.talk.idor.dominio.Modelos.Colegio;
import co.pragma.talk.idor.dominio.Modelos.Curso;
import co.pragma.talk.idor.dominio.Modelos.Estudiante;

/**
 * Datos de la plataforma en memoria (ficticios). Simula la base de datos / servicios de consulta.
 * Fíjate que {@link #estudiantes(String)} recibe solo el código del curso: no sabe de qué colegio es.
 */
@Repository
public class CatalogoRepository {

    private final Map<String, Colegio> colegios = new LinkedHashMap<>();
    private final Map<String, List<Curso>> cursosPorColegio = new LinkedHashMap<>();
    private final Map<String, List<Estudiante>> estudiantesPorCurso = new LinkedHashMap<>();

    public CatalogoRepository() {
        colegio("111001000101", "Colegio San Martín", "Bogotá",
                curso("1001", "Matemáticas", "10° A", "Prof. Andrés Cuervo",
                        est("Mariana Ortiz", "TI 1.023.889.104", "Claudia Ortiz", "claudia.ortiz@example.com", "4.6"),
                        est("Samuel Rojas", "TI 1.023.902.551", "Jaime Rojas", "jaime.rojas@example.com", "3.9"),
                        est("Valeria Gómez", "TI 1.023.874.330", "Patricia Gómez", "patricia.gomez@example.com", "4.2"),
                        est("Nicolás Pardo", "TI 1.023.911.026", "Fernando Pardo", "fernando.pardo@example.com", "3.4")),
                curso("1002", "Ciencias naturales", "8° B", "Prof. Liliana Vega",
                        est("Isabella Cruz", "TI 1.025.330.718", "Marcela Cruz", "marcela.cruz@example.com", "4.8"),
                        est("Juan José Peña", "TI 1.025.341.902", "Óscar Peña", "oscar.pena@example.com", "3.7"),
                        est("Sofía Lozano", "TI 1.025.298.477", "Diana Lozano", "diana.lozano@example.com", "4.1")));
        colegio("105001000202", "Colegio Los Andes", "Medellín",
                curso("2001", "Física", "11° A", "Prof. Héctor Gaviria",
                        est("Emilio Restrepo", "TI 1.001.229.384", "Beatriz Restrepo", "beatriz.restrepo@example.com", "4.4"),
                        est("Antonia Vélez", "TI 1.001.231.770", "Carlos Vélez", "carlos.velez@example.com", "4.9"),
                        est("Jerónimo Uribe", "TI 1.001.244.015", "Mónica Uribe", "monica.uribe@example.com", "3.2"),
                        est("Luciana Zapata", "TI 1.001.218.662", "Gustavo Zapata", "gustavo.zapata@example.com", "4.0"),
                        est("Martín Arango", "TI 1.001.250.931", "Eliana Arango", "eliana.arango@example.com", "3.6")),
                curso("2002", "Inglés", "9° C", "Prof. Catalina Mesa",
                        est("Salomé Duque", "TI 1.001.302.118", "Juliana Duque", "juliana.duque@example.com", "4.5"),
                        est("Tomás Echeverri", "TI 1.001.318.447", "Ricardo Echeverri", "ricardo.echeverri@example.com", "3.8"),
                        est("Gabriela Montoya", "TI 1.001.299.506", "Lorena Montoya", "lorena.montoya@example.com", "4.3")));
        colegio("176001000303", "Colegio La Esperanza", "Cali",
                curso("3001", "Ciencias sociales", "7° A", "Prof. Yolanda Caicedo",
                        est("Dylan Mosquera", "TI 1.107.550.283", "Rosa Mosquera", "rosa.mosquera@example.com", "4.1"),
                        est("Ashley Rentería", "TI 1.107.561.940", "Wilson Rentería", "wilson.renteria@example.com", "4.7"),
                        est("Kevin Valencia", "TI 1.107.538.716", "Nancy Valencia", "nancy.valencia@example.com", "3.5")));
    }

    public Optional<Colegio> colegio(String dane) {
        return Optional.ofNullable(colegios.get(dane));
    }

    public List<Curso> cursosDeColegio(String dane) {
        return cursosPorColegio.getOrDefault(dane, List.of());
    }

    /** Estudiantes de un curso. Solo recibe el código: no puede filtrar por colegio. */
    public Optional<List<Estudiante>> estudiantes(String codigoCurso) {
        return Optional.ofNullable(estudiantesPorCurso.get(codigoCurso));
    }

    /** De qué colegio es un curso (solo para mostrarlo en la respuesta). */
    public Optional<Colegio> colegioDelCurso(String codigoCurso) {
        return cursosPorColegio.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(c -> c.codigo().equals(codigoCurso)))
                .findFirst()
                .map(e -> colegios.get(e.getKey()));
    }

    public Optional<Curso> curso(String codigoCurso) {
        return cursosPorColegio.values().stream().flatMap(List::stream)
                .filter(c -> c.codigo().equals(codigoCurso)).findFirst();
    }

    // ---------- carga de datos ----------

    private record CursoConEstudiantes(Curso curso, List<Estudiante> estudiantes) {
    }

    private void colegio(String dane, String nombre, String ciudad, CursoConEstudiantes... cursos) {
        colegios.put(dane, new Colegio(dane, nombre, ciudad));
        cursosPorColegio.put(dane, Arrays.stream(cursos).map(CursoConEstudiantes::curso).toList());
        for (CursoConEstudiantes c : cursos) {
            estudiantesPorCurso.put(c.curso().codigo(), c.estudiantes());
        }
    }

    private static CursoConEstudiantes curso(String codigo, String nombre, String grado, String docente, Estudiante... estudiantes) {
        return new CursoConEstudiantes(new Curso(codigo, nombre, grado, docente, estudiantes.length), List.of(estudiantes));
    }

    private static Estudiante est(String nombre, String documento, String acudiente, String correo, String promedio) {
        return new Estudiante(nombre, documento, acudiente, correo, new BigDecimal(promedio));
    }
}
