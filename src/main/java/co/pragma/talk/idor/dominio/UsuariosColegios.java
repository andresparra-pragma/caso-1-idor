package co.pragma.talk.idor.dominio;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/** Qué colegios puede gestionar cada usuario de la plataforma (datos ficticios). */
@Component
public class UsuariosColegios {

    public record Perfil(String nombre, String cargo, List<String> colegios) {
    }

    private final Map<String, Perfil> perfiles = Map.of(
            "lucia", new Perfil("Lucía Fernández", "Coordinadora académica", List.of("111001000101", "176001000303")),
            "pedro", new Perfil("Pedro Salcedo", "Rector", List.of("105001000202")));

    public Perfil perfil(String username) {
        return perfiles.getOrDefault(username, new Perfil(username, "", List.of()));
    }

    public boolean puedeGestionar(String username, String dane) {
        return perfil(username).colegios().contains(dane);
    }
}
