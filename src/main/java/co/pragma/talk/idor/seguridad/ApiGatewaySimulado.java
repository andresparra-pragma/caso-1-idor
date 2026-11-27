package co.pragma.talk.idor.seguridad;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import co.pragma.talk.idor.dominio.UsuariosColegios;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simula lo que hace el API Gateway antes de que la petición llegue al micro:
 * verifica que el usuario autenticado pueda gestionar el colegio del header {@code X-Colegio}.
 *
 * Esto funciona bien... pero SOLO mira el header. No mira la URL ni los datos de negocio.
 * Ese hueco es el que explota el IDOR.
 */
@Component
public class ApiGatewaySimulado implements HandlerInterceptor {

    private final UsuariosColegios usuarios;
    private final ColegioAutorizadoResolver resolver;

    public ApiGatewaySimulado(UsuariosColegios usuarios, ColegioAutorizadoResolver resolver) {
        this.usuarios = usuarios;
        this.resolver = resolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Principal user = request.getUserPrincipal();
        String dane;
        try {
            dane = resolver.resolver(request);
        } catch (AccesoDenegadoException e) {
            response.sendError(HttpStatus.FORBIDDEN.value());
            return false;
        }
        if (user == null || !usuarios.puedeGestionar(user.getName(), dane)) {
            response.sendError(HttpStatus.FORBIDDEN.value());
            return false;
        }
        return true;
    }
}
