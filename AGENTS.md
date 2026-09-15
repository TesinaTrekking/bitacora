# AGENTS.md

## Compilación y ejecución

```sh
mvn clean compile   # compilar (con limpieza) — la única verificación automatizada
mvn javafx:run      # lanzar la app JavaFX (requiere display)
```

No hay tests, linter, formatter ni CI. En Linux, ejecutar la app requiere las librerías GTK3 del sistema.

La documentación oficial del proyecto (arquitectura, stack, CRUD y guía de ejecución) vive en `README.md` en la raíz — si algo cambia de arquitectura, actualizarlo junto con el código.

## Proyecto

Java 17 / JavaFX 21 / SQLite — proyecto Maven de módulo único. Clase principal: `com.bitacora.trekking.App`.

**Base package** `com.bitacora.trekking` con subpaquetes organizados:

| Archivo | Paquete | Rol |
|---|---|---|
| `App.java` | raíz | Entry point. Delega en `MainController`. |
| `MainController.java` | `controller` | Ventana principal: UI programática (sin FXML), carga de datos, acciones CRUD. |
| `CheckpointDialog.java` | `controller` | Diálogo modal de creación/edición con validación. |
| `Checkpoint.java` | `model` | Modelo con propiedades observables de JavaFX (`LongProperty`, `StringProperty`, etc.). |
| `CheckpointDAO.java` | `dao` | CRUD contra SQLite vía JDBC. |
| `DatabaseManager.java` | `dao` | Factory de conexiones + inicialización de esquema. |
| `AlertUtils.java` | `util` | Alertas modales compartidas (`showError` / `showWarning`). |

## Convenciones clave

- Toda la UI se construye en código — no existen archivos FXML.
- Nombres de variables, clases y paquetes en español (el dominio es español).
- Modelo con propiedades JavaFX, no POJOs simples — siempre incluir métodos `*Property()` junto a getters/setters.
- Diálogos de formulario: `GridPane` de dos columnas (etiqueta | campo), `hgap`/`vgap = 10`, padding `10`. Enter = confirmar, Escape = cancelar.
- Acciones guiadas por selección (Editar/Eliminar): mantener `disableProperty().bind(selectionModel().selectedItemProperty().isNull())`.
- Toda eliminación pide confirmación (`Alert.AlertType.CONFIRMATION`) antes de tocar SQLite.
- Errores se registran con `java.util.logging.Logger` (no `printStackTrace()` ni `catch` vacíos).
- DAO propaga `SQLException`; `update` devuelve `int` (filas afectadas), `delete` devuelve `boolean`.
- Javadoc en clases públicas y métodos no triviales; comentarios de "por qué", no de "qué".

## Advertencias (Gotchas)

- **BD SQLite** `checkpoints.db` vive en `~/.bitacora-checkpoints/` (ruta estable, definida en `DatabaseManager`), independiente del working directory. El `.gitignore` solo excluye `target/` — no hay exclusión explícita de `checkpoints.db`, así que el archivo local no debería commitearse.
- `CheckpointDAO` abre una `Connection` nueva por operación (sin pooling — aceptable para este tamaño de app).
- La app requiere un entorno gráfico para mostrar la ventana (no funciona en modo headless).
