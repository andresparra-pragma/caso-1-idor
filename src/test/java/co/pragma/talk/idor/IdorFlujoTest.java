package co.pragma.talk.idor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import co.pragma.talk.idor.dominio.MatriculasClient;

/**
 * Plan B de la demo: si algo falla en vivo, `mvnw test` muestra el mismo antes/después.
 * Lucía es coordinadora del Colegio San Martín (111001000101). Colegio Los Andes (105001000202) es otro colegio.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IdorFlujoTest {

    static final String SAN_MARTIN = "DANE-111001000101";

    @Autowired
    MockMvc mvc;

    @Autowired
    MatriculasClient matriculas;

    @AfterEach
    void restaurar() {
        matriculas.setDisponible(true);
    }

    private MockHttpServletRequestBuilder comoLucia(String url) {
        return get(url).with(httpBasic("lucia", "lucia123")).header("X-Colegio", SAN_MARTIN);
    }

    // ---------- Lo que sí funciona: login y gateway ----------

    @Test
    void sinLoginNoHayNada() throws Exception {
        mvc.perform(get("/api/vulnerable/colegios/111001000101/cursos").header("X-Colegio", SAN_MARTIN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elGatewayBloqueaUnHeaderDeOtroColegio() throws Exception {
        mvc.perform(get("/api/vulnerable/colegios/105001000202/cursos")
                        .with(httpBasic("lucia", "lucia123")).header("X-Colegio", "DANE-105001000202"))
                .andExpect(status().isForbidden());
    }

    @Test
    void luciaVeSusPropiosCursosEnAmbasVersiones() throws Exception {
        mvc.perform(comoLucia("/api/vulnerable/colegios/111001000101/cursos"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cursos.length()").value(2));
        mvc.perform(comoLucia("/api/seguro/colegios/111001000101/cursos"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cursos.length()").value(2));
        mvc.perform(comoLucia("/api/seguro/cursos/1001/estudiantes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estudiantes.length()").value(4));
    }

    // ---------- El ataque en la versión vulnerable ----------

    @Test
    void vulnerablePaso1CambiandoElDaneDeLaUrlVeLosCursosDeOtroColegio() throws Exception {
        mvc.perform(comoLucia("/api/vulnerable/colegios/105001000202/cursos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colegio.nombre").value("Colegio Los Andes"))
                .andExpect(jsonPath("$.cursos[0].codigo").value("2001"));
    }

    @Test
    void vulnerablePaso2ConElCodigoDelCursoVeEstudiantesYAcudientes() throws Exception {
        mvc.perform(comoLucia("/api/vulnerable/cursos/2001/estudiantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colegio.dane").value("105001000202"))
                .andExpect(jsonPath("$.estudiantes[0].documento").value("TI 1.001.229.384"))
                .andExpect(jsonPath("$.estudiantes[0].correoAcudiente").value("beatriz.restrepo@example.com"));
    }

    @Test
    void vulnerableRevelaSiUnCursoExiste() throws Exception {
        mvc.perform(comoLucia("/api/vulnerable/cursos/9999/estudiantes")).andExpect(status().isNotFound());
    }

    // ---------- La corrección ----------

    @Test
    void seguroDaneDeLaUrlDistintoAlDeLaSesionDa403() throws Exception {
        mvc.perform(comoLucia("/api/seguro/colegios/105001000202/cursos")).andExpect(status().isForbidden());
    }

    @Test
    void seguroCursoDeOtroColegioDa403() throws Exception {
        mvc.perform(comoLucia("/api/seguro/cursos/2001/estudiantes")).andExpect(status().isForbidden());
    }

    @Test
    void seguroNoRevelaSiElCursoExiste() throws Exception {
        mvc.perform(comoLucia("/api/seguro/cursos/9999/estudiantes")).andExpect(status().isForbidden());
    }

    @Test
    void seguroFailClosedSiNoPuedeVerificarLaPertenencia() throws Exception {
        matriculas.setDisponible(false);
        mvc.perform(comoLucia("/api/seguro/cursos/1001/estudiantes")).andExpect(status().isForbidden());
    }

    @Test
    void headerAusenteAmbiguoOInvalidoDa403() throws Exception {
        mvc.perform(get("/api/seguro/colegios/111001000101/cursos").with(httpBasic("lucia", "lucia123")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/seguro/colegios/111001000101/cursos").with(httpBasic("lucia", "lucia123"))
                        .header("X-Colegio", SAN_MARTIN).header("X-Colegio", "DANE-176001000303"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/seguro/colegios/111001000101/cursos").with(httpBasic("lucia", "lucia123"))
                        .header("X-Colegio", "111001000101"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cadaIntentoDenegadoQuedaEnAuditoria() throws Exception {
        mvc.perform(comoLucia("/api/seguro/cursos/2001/estudiantes")).andExpect(status().isForbidden());
        mvc.perform(get("/api/demo/auditoria").with(httpBasic("lucia", "lucia123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuario").value("lucia"))
                .andExpect(jsonPath("$[0].recurso").value("curso 2001"));
    }
}
