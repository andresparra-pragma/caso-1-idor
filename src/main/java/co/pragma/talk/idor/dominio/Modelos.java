package co.pragma.talk.idor.dominio;

import java.math.BigDecimal;

/** Modelos de la plataforma académica (datos ficticios). */
public final class Modelos {

    private Modelos() {
    }

    /** Colegio de la red, identificado por su código DANE. */
    public record Colegio(String dane, String nombre, String ciudad) {
    }

    /** Curso. Ojo: el código del curso NO dice de qué colegio es. */
    public record Curso(String codigo, String nombre, String grado, String docente, int estudiantes) {
    }

    /** Estudiante (menor de edad): datos personales, acudiente y promedio. */
    public record Estudiante(String nombre, String documento, String acudiente, String correoAcudiente, BigDecimal promedio) {
    }
}
