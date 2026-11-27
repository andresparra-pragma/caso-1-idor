package co.pragma.talk.idor.dominio;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

/**
 * Simula el servicio externo de matrículas, que sabe qué cursos tiene cada colegio
 * (en la vida real, otro micro o un servicio legado). La versión segura lo consulta para
 * verificar que el curso pedido pertenezca al colegio del usuario.
 *
 * Se puede "tumbar" para mostrar el comportamiento fail-closed.
 */
@Component
public class MatriculasClient {

    private final CatalogoRepository catalogo;
    private final AtomicBoolean disponible = new AtomicBoolean(true);

    public MatriculasClient(CatalogoRepository catalogo) {
        this.catalogo = catalogo;
    }

    public List<String> codigosDeCursos(String daneColegio) {
        if (!disponible.get()) {
            throw new IllegalStateException("Servicio de matrículas no disponible");
        }
        return catalogo.cursosDeColegio(daneColegio).stream().map(Modelos.Curso::codigo).toList();
    }

    public void setDisponible(boolean valor) {
        disponible.set(valor);
    }

    public boolean isDisponible() {
        return disponible.get();
    }
}
