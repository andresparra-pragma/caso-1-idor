package co.pragma.talk.idor.seguridad;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Obtiene el colegio autorizado de la petición: el header {@code X-Colegio} que el API Gateway
 * ya validó contra los permisos del usuario. Formatos aceptados: {@code DANE-111001000101} o {@code DANE_111001000101}.
 *
 * Es fail-closed: si el header falta, llega con dos valores distintos o tiene un formato inválido,
 * no se adivina nada y se niega el acceso.
 */
@Component
public class ColegioAutorizadoResolver {

    public static final String HEADER = "X-Colegio";
    private static final Pattern FORMATO = Pattern.compile("^DANE[-_](\\d{12})$");

    public String resolver(HttpServletRequest request) {
        Set<String> valores = new LinkedHashSet<>();
        for (String v : Collections.list(request.getHeaders(HEADER))) {
            for (String parte : v.split(",")) {
                if (!parte.isBlank()) {
                    valores.add(parte.trim());
                }
            }
        }
        if (valores.isEmpty()) {
            throw new AccesoDenegadoException("header " + HEADER + " ausente");
        }
        Set<String> codigos = new LinkedHashSet<>();
        for (String v : valores) {
            Matcher m = FORMATO.matcher(v);
            if (!m.matches()) {
                throw new AccesoDenegadoException("header " + HEADER + " con formato inválido");
            }
            codigos.add(m.group(1));
        }
        if (codigos.size() > 1) {
            throw new AccesoDenegadoException("header " + HEADER + " ambiguo");
        }
        return codigos.iterator().next();
    }
}
