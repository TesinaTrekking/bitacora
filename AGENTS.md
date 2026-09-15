# AGENTS.md

## Compilación y ejecución

```sh
mvn clean compile   # compilar (con limpieza)
mvn javafx:run      # lanzar la app JavaFX (requiere display)
```

No hay suite de tests, linter, formatter ni CI. `mvn clean compile` es la única verificación automatizada. En Linux, ejecutar la app requiere las librerías GTK3 del sistema instaladas.

La documentación oficial del proyecto (arquitectura, stack, CRUD y guía de ejecución, para presentación académica) vive en `README.md` en la raíz — si algo cambia de arquitectura, actualizarlo junto con el código.

## Proyecto

Java 17 / JavaFX 21 / SQLite — proyecto Maven de módulo único. Clase principal: `com.bitacora.trekking.App`.

**Base package** `com.bitacora.trekking` con subpaquetes organizados:

| Archivo | Paquete | Rol |
|---|---|---|
| `App.java` | raíz | Entry point. Construye la UI de forma programática (sin FXML) vía `MainController`. |
| `MainController.java` | `controller` | Controlador de la ventana principal: UI, carga de datos y acciones crear/editar/eliminar. |
| `CheckpointDialog.java` | `controller` | Diálogo modal de creación/edición. |
| `Checkpoint.java` | `model` | Modelo con propiedades observables de JavaFX (`LongProperty`, `StringProperty`, etc.). |
| `CheckpointDAO.java` | `dao` | Operaciones CRUD contra SQLite vía JDBC. |
| `DatabaseManager.java` | `dao` | Factory de conexiones + inicialización de esquema (`CREATE TABLE IF NOT EXISTS`). |
| `AlertUtils.java` | `util` | Alertas modales compartidas (`showError` / `showWarning`) para errores de BD y validación. |

## Convenciones

- Toda la UI se construye en código — no existen archivos FXML.
- Etiquetas y nombres de variables en español en todo el proyecto (el dominio es español).
- El modelo usa propiedades de JavaFX, no POJOs simples — siempre agregar métodos `*Property()` junto a getters/setters.
- UI de baja fidelidad: tema estándar Modena de JavaFX, sin archivos CSS. Layout/espaciado se hace en código (`GridPane`, `Insets`, etc.).
- Diálogos de formulario: layout de dos columnas (`GridPane` etiqueta | campo), `hgap`/`vgap = 10` y padding `10`. El botón principal es default (`setDefaultButton(true)`, responde a Enter) y Cancelar responde a Escape (`setCancelButton(true)`).
- El diálogo de creación arranca con todos los campos vacíos (sin pre-relleno); el de edición precarga la fila seleccionada.
- Las acciones de la ventana principal (Editar/Eliminar) se habilitan/deshabilitan enlazadas a la selección de la tabla — mantener `disableProperty().bind(selectionModel().selectedItemProperty().isNull())` para acciones guiadas por selección.
- Toda eliminación destructiva pide confirmación (`Alert.AlertType.CONFIRMATION`) antes de tocar SQLite.
- Uso de `java.util.logging.Logger` para registrar errores (no `printStackTrace()` ni `catch` vacíos).
- Javadoc en clases públicas y métodos no triviales de la capa DAO/controller; comentarios que describen decisiones ("por qué"), no los que repiten el código ("qué").

## Advertencias (Gotchas)

- **Archivo de BD SQLite** `checkpoints.db` vive en `~/.bitacora-checkpoints/` (ruta estable, independiente del working directory), definido en `DatabaseManager`. El working dir puede contener un `checkpoints.db` local no commiteado: está cubierto por `.gitignore` — evitar commitear datos reales.
- Existe `.gitignore` que excluye `target/` y `checkpoints.db`.
- `CheckpointDAO.insert/selectAll/update/delete` lanzan `SQLException` (se reportan vía `AlertUtils`); `update` devuelve cantidad de filas afectadas (`int`) y `delete` devuelve `boolean`. No tragar errores SQL en silencio — mostrarlos.
- `CheckpointDAO` abre una `Connection` nueva por operación (sin pooling — aceptable para este tamaño de app, tenerlo presente si escala).
