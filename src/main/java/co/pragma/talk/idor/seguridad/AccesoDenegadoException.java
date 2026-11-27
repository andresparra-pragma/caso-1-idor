package co.pragma.talk.idor.seguridad;

/**
 * Acceso denegado. El motivo es solo para la auditoría interna:
 * al cliente siempre se le responde el mismo 403 genérico.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String motivo) {
        super(motivo);
    }
}
