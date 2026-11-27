package co.pragma.talk.idor.seguridad;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registro de auditoría de los accesos denegados.
 * Todo lo que viene del cliente se sanea antes de escribirlo (CWE-117, log injection):
 * sin saltos de línea ni caracteres de control, y con un largo máximo.
 */
@Component
public class Auditoria {

    private static final Logger log = LoggerFactory.getLogger(Auditoria.class);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    public record Evento(String hora, String usuario, String operacion, String motivo,
                         String colegioAutorizado, String recurso, String ip) {
    }

    private final Deque<Evento> recientes = new ArrayDeque<>();

    public synchronized void accesoDenegado(String usuario, String operacion, String motivo,
                                            String colegioAutorizado, String recurso, String ip) {
        Evento e = new Evento(LocalTime.now().format(HORA), sanear(usuario), operacion, sanear(motivo),
                sanear(colegioAutorizado), sanear(recurso), sanear(ip));
        log.warn("AUDITORIA acceso denegado usuario={} operacion={} motivo={} colegioAutorizado={} recurso={} ip={}",
                e.usuario(), e.operacion(), e.motivo(), e.colegioAutorizado(), e.recurso(), e.ip());
        recientes.addFirst(e);
        while (recientes.size() > 20) {
            recientes.removeLast();
        }
    }

    public synchronized List<Evento> recientes() {
        return new ArrayList<>(recientes);
    }

    static String sanear(String valor) {
        if (valor == null) {
            return "-";
        }
        String limpio = valor.replaceAll("[\\p{Cntrl}\\u2028\\u2029]", "_");
        return limpio.length() > 80 ? limpio.substring(0, 80) + "…" : limpio;
    }
}
