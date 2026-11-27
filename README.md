# Caso 1: ver datos de otro colegio cambiando un número en la URL

Este proyecto es una de las demos de la charla "Tu código tiene vulnerabilidades, y tus dependencias también" (PragmaTalk). Muestra un error de seguridad muy común, llamado IDOR, y cómo corregirlo.

> **Importante:** esta app tiene errores de seguridad a propósito, para poder mostrarlos. Úsala solo en tu computador. Funciona únicamente en `127.0.0.1` y todos los datos son inventados.

## Qué pasa en esta app

Es una plataforma para una red de colegios. Cada coordinador entra y ve los cursos y los estudiantes de su colegio.

Lucía es coordinadora del Colegio San Martín. Cuando entra, la dirección del navegador se ve así:

```
http://127.0.0.1:8082/#/colegios/111001000101/cursos
```

Ese número largo es el código del colegio. El problema es que si Lucía lo cambia por el código de otro colegio, la app le muestra los cursos de ese otro colegio. Y desde ahí puede ver nombres, documentos, acudientes y notas de estudiantes que no son suyos.

## Por qué pasa

La app sí pide usuario y contraseña. Además, antes de llegar al backend, cada petición pasa por un "portero" (un API Gateway) que revisa que Lucía tenga permiso sobre su colegio.

El detalle está en qué revisa ese portero. El colegio que Lucía eligió viaja en un encabezado de la petición (`X-Colegio`), y el portero solo mira ese encabezado. No mira el número de la URL. El backend, en cambio, usa el número de la URL. Resultado: el portero aprueba la petición y el backend entrega datos de otro colegio.

Hay un segundo problema. Para ver los estudiantes de un curso basta con el código del curso (por ejemplo, `2001`), y ese código no dice de qué colegio es. Como los códigos son números seguidos, un script puede recorrerlos todos y descargar los estudiantes de todos los colegios.

A este tipo de error se le llama **IDOR** (en español, referencia insegura a un objeto): el sistema sabe quién eres, pero no revisa si lo que pides es tuyo. Hace parte del "control de acceso roto", el riesgo número 1 de la lista OWASP Top 10.

## Cómo se corrige

La versión segura hace cuatro cosas:

1. Toma el colegio de lo que ya se validó (el encabezado que aprobó el portero), nunca de la URL. Si los dos no coinciden, responde 403 (acceso denegado).
2. Antes de mostrar estudiantes, confirma que el curso pertenece a ese colegio.
3. Si no puede confirmarlo, por ejemplo porque el servicio que lo sabe está caído, también niega el acceso. Es mejor un error que entregar datos ajenos.
4. Responde siempre el mismo mensaje, exista o no el curso, para no dar pistas. Y deja registro de cada intento.

Para ver la diferencia en el código, compara estos dos archivos:

- [`CursosVulnerableController.java`](src/main/java/co/pragma/talk/idor/web/CursosVulnerableController.java) (con el error)
- [`CursosSeguroController.java`](src/main/java/co/pragma/talk/idor/web/CursosSeguroController.java) (corregido)

## Cómo probarlo

Necesitas Java 17 o más nuevo. No hace falta instalar Maven: el proyecto ya trae lo necesario.

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux o Mac:

```bash
./mvnw spring-boot:run
```

Abre <http://127.0.0.1:8082> y entra con alguno de estos usuarios:

- `lucia` / `lucia123`: coordinadora del Colegio San Martín (y del Colegio La Esperanza).
- `pedro` / `pedro123`: rector del Colegio Los Andes.

Abajo a la derecha verás una barra oscura. No hace parte de la plataforma: sirve para la demo. Con ella cambias entre la versión con el error ("Vulnerable") y la corregida ("Seguro").

Prueba esto, con la barra en "Vulnerable":

1. Entra como `lucia`. Vas a ver los cursos del Colegio San Martín.
2. En la dirección del navegador, cambia `111001000101` por `105001000202` y presiona Enter. Ahora ves los cursos del Colegio Los Andes, que no es el de Lucía.
3. Entra a "Ver estudiantes" del curso 2001. Ves los estudiantes de otro colegio.
4. Pulsa "Ver petición" en la barra. En amarillo está el código de la URL; en verde, el colegio de Lucía. El backend usó el amarillo.
5. Ahora cambia la barra a "Seguro" y repite. Aparece "No tienes acceso a esta información". En "Auditoría" queda registrado cada intento.

Si usas Windows, también puedes ver el problema desde la terminal. Este script recorre los códigos de curso con la sesión de Lucía:

```powershell
.\enumerar.ps1                 # versión con el error: trae estudiantes de los 3 colegios
.\enumerar.ps1 -Modo seguro    # versión corregida: todo lo que no es de Lucía responde 403
```

## Pruebas automáticas

```bash
./mvnw test
```

Son 15 pruebas. Unas hacen el ataque contra la versión con el error y comprueban que funciona. Otras comprueban que la versión corregida lo bloquea en todos los casos: otro colegio, un curso ajeno, un curso que no existe, un encabezado que falta o viene mal, y el servicio de verificación caído.

Escribir el ataque como una prueba es la mejor forma de asegurarse de que el error no vuelva.

## Revisión del código con Fluid Attacks

En cada cambio, GitHub revisa el código con la herramienta gratuita de [Fluid Attacks](https://github.com/fluidattacks/sast-action). Esto se llama SAST: analizar el código sin ejecutarlo, buscando patrones peligrosos. Lo que encuentra aparece en la pestaña **Security**, en "Code scanning".

Aquí no bloquea nada, porque el código con errores está a propósito.

Una pregunta interesante: ¿esta herramienta detecta el IDOR? Probablemente no. Un IDOR no es un patrón de código peligroso, es una regla de negocio que falta ("¿este curso es de este colegio?"). Para encontrarlo sirven las pruebas automáticas como las de este proyecto, las pruebas con usuarios distintos sobre la app funcionando y la revisión de código entre personas.

## Qué hay en cada carpeta

- `src/main/java/.../dominio`: los datos inventados (colegios, cursos, estudiantes).
- `src/main/java/.../seguridad`: el login, el "portero" simulado, la lectura del colegio autorizado y el registro de intentos.
- `src/main/java/.../web`: los controladores con el error y corregidos.
- `src/main/resources/static`: las pantallas de la plataforma y la barra de la demo.
- `enumerar.ps1`: el script de la demo.

Hecho con Spring Boot 3.5, Spring Security y Java 17.
