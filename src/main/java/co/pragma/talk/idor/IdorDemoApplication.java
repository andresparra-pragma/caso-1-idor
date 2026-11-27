package co.pragma.talk.idor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Caso 1 · IDOR en una plataforma académica para una red de colegios (datos ficticios).
 *
 * Patrón real: el API Gateway valida el header con el colegio del usuario,
 * pero el micro confía en los datos de la URL.
 */
@SpringBootApplication
public class IdorDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdorDemoApplication.class, args);
    }
}
