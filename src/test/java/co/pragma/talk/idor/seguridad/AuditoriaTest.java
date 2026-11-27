package co.pragma.talk.idor.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditoriaTest {

    @Test
    void saneaSaltosDeLineaParaEvitarLogInjection() {
        String ataque = "2001\r\n2026-11-26 WARN AUDITORIA acceso permitido usuario=admin";
        assertThat(Auditoria.sanear(ataque)).doesNotContain("\n").doesNotContain("\r");
    }

    @Test
    void recortaValoresMuyLargos() {
        assertThat(Auditoria.sanear("x".repeat(500))).hasSizeLessThanOrEqualTo(81);
    }

    @Test
    void valorNuloSeRegistraComoGuion() {
        assertThat(Auditoria.sanear(null)).isEqualTo("-");
    }
}
