# Bitácora Checkpoints

Sistema desktop de gestión de puntos de control (checkpoints) para bitácoras de ruta, con operaciones **CRUD** completas sobre **SQLite**. La interfaz se construye enteramente en **JavaFX** (sin FXML), siguiendo una arquitectura en capas.

**Prototipo de baja fidelidad** con fines académicos: demuestra un flujo CRUD completo (crear, listar, editar, eliminar), priorizando la usabilidad y la separación de responsabilidades por sobre el refinamiento visual.

## Índice

- [Propósito Académico](#propósito-académico)
- [Stack Técnico](#stack-técnico)
- [Arquitectura y Patrones de Diseño](#arquitectura-y-patrones-de-diseño)
- [Metodología y Decisiones de UI/UX (Baja Fidelidad)](#metodología-y-decisiones-de-uiux-baja-fidelidad)
- [Lógica del CRUD y Persistencia](#lógica-del-crud-y-persistencia)
- [Guía de Instalación y Ejecución](#guía-de-instalación-y-ejecución)

## Propósito Académico

El proyecto se enmarca en un contexto universitario con dos objetivos de formación:

1. **Prototipado de baja fidelidad**: ciclo completo de una app de escritorio con persistencia, sin esfuerzo en estética.
2. **Práctica de CRUD persistente**: las cuatro operaciones fundamentales contra una BD relacional con API gráfica.

El dominio elegido — una bitácora de ruta con puntos de control georreferenciados — permite modelar una entidad con atributos de distintos tipos (texto, hora y coordenadas numéricas), lo que obliga a implementar validación de datos y conversión de tipos.

## Stack Técnico

| Tecnología | Versión | Rol |
|---|---|---|
| **Java** | 17 | Lenguaje de programación y plataforma de ejecución |
| **JavaFX** | 21.0.2 | Framework de interfaz gráfica (OpenJFX) |
| **Maven** | 3.x | Gestión de dependencias y ciclo de vida de compilación |
| **SQLite** | — (vía `sqlite-jdbc` 3.45.2.0) | Motor de base de datos relacional embebida |

> **Nota**: Java, JavaFX y Maven fueron definidos por la consigna de la materia; aquí se documenta su rol técnico dentro del prototipo.

Java 17 provee tipado estático y manejo estructurado de excepciones. JavaFX aporta propiedades observables que alimentan el patrón MVC. Maven gestiona dependencias y ciclo de vida desde `pom.xml`.

**SQLite** es serverless y embebida — no requiere servicio externo. La persistencia se implementa vía JDBC (`org.xerial:sqlite-jdbc`). Particularidades:

- La BD **`checkpoints.db`** vive en `~/.bitacora-checkpoints/`, directorio estable independiente del working directory.
- El **esquema se inicializa automáticamente** al arrancar (`CREATE TABLE IF NOT EXISTS`).

## Arquitectura y Patrones de Diseño

La aplicación implementa una **arquitectura en capas** que separa la responsabilidad de presentación, dominio y persistencia. Cada capa sólo conoce a la capa inferior, garantizando bajo acoplamiento y alta cohesión.

```
┌─────────────────────────────────────────────┐
│  Capa de Presentación (UI / Controller)      │
│  App, MainController, CheckpointDialog       │
│  Construcción de vistas + orquestación CRUD  │
├─────────────────────────────────────────────┤
│  Capa de Modelo (Model)                      │
│  Checkpoint                                  │
│  Entidad con propiedades observables JavaFX  │
├─────────────────────────────────────────────┤
│  Capa de Persistencia (DAO)                  │
│  CheckpointDAO                               │
│  Consultas SQL (JDBC) aisladas de la UI      │
├─────────────────────────────────────────────┤
│  Capa de Conexión (Util / Manager)           │
│  DatabaseManager, AlertUtils                 │
│  Conexiones + esquema SQLite / alertas mod.  │
└─────────────────────────────────────────────┘
```

### Capa de Presentación (UI / Controller)

- **`App`** — Punto de entrada (`Application.launch`). Delega la construcción de la ventana en `MainController`.
- **`MainController`** — Controlador de la ventana principal: construye la escena programáticamente, carga los datos y orquesta las acciones crear/editar/eliminar.
- **`CheckpointDialog`** — Diálogo modal de creación/edición con validación de datos al confirmar.

La comunicación hacia abajo ocurre exclusivamente vía la capa DAO; la interfaz jamás ejecuta SQL directamente.

### Capa de Modelo (Model)

- **`Checkpoint`** — Entidad del dominio con **propiedades observables de JavaFX** (`LongProperty`, `StringProperty`, `DoubleProperty`) para reactividad en la tabla y diálogos. Cada campo tiene getter/setter y método `*Property()`. Atributos: `id`, `nombre`, `hora`, `latitud`, `longitud`, `descripcion`.

### Capa de Persistencia (DAO)

- **`CheckpointDAO`** — Patrón **DAO**: encapsula consultas SQL en métodos de alto nivel usando `PreparedStatement` y `try-with-resources`. Expone `insert`, `selectAll`, `update` y `delete`, y **propaga `SQLException`** para que la UI informe el fallo real.

### Capa de Conexión (Util / Manager)

- **`DatabaseManager`** — Factory de conexiones y **inicialización automática del esquema**: crea `~/.bitacora-checkpoints/` y ejecuta `CREATE TABLE IF NOT EXISTS`. Clase utilitaria (constructor privado).
- **`AlertUtils`** — Alertas modales compartidas (`showError` / `showWarning`) para errores de BD y validación.

### Árbol de Carpetas del Proyecto

```
bitacora/
├── .gitignore
├── AGENTS.md
├── pom.xml
└── src/main/java/com/bitacora/trekking/
    ├── App.java                              # Entry point JavaFX
    ├── controller/
    │   ├── MainController.java               # Ventana principal
    │   └── CheckpointDialog.java             # Diálogo crear/editar
    ├── dao/
    │   ├── CheckpointDAO.java                # Operaciones CRUD
    │   └── DatabaseManager.java              # Conexión + esquema
    ├── model/
    │   └── Checkpoint.java                   # Entidad observable
    └── util/
        └── AlertUtils.java                   # Alertas modales
```

### Patrón de Conexión por Operación

`CheckpointDAO` abre una **nueva `Connection` por operación** (sin pooling). Aceptable para este tamaño de app; documentado para una eventual migración a pooling.

## Metodología y Decisiones de UI/UX (Baja Fidelidad)

### Filosofía del Prototipo

El prototipo concentra el esfuerzo en tres ejes funcionales por encima de lo visual:

1. **Usabilidad**: flujo intuitivo y consistentemente accesible.
2. **Flujo lógico**: orden de acciones claro (seleccionar → editar/eliminar; completar formulario → confirmar).
3. **Funcionalidad CRUD completa**: cada operación es observable y verificable en la tabla y en la base de datos.

No se utilizan CSS ni paletas de colores: la aplicación conserva el **tema Modena** de JavaFX, priorizando la lógica por sobre la estética.

### Reglas de UX Aplicadas

| Regla | Implementación |
|---|---|
| **Alineación en grid** | Los formularios usan `GridPane` de dos columnas (etiqueta \| campo) con alineación consistente. |
| **Espaciado uniforme** | `hgap`/`vgap = 10` entre celdas del grid y `padding` de `10` en el diálogo; la ventana principal usa `VBox` con espaciado de `15` y `Insets` de `15`. |
| **Botón principal por defecto** | El botón de confirmación responde a **Enter** (`setDefaultButton(true)`). |
| **Cancelación con Escape** | El botón Cancelar responde a **Escape** (`setCancelButton(true)`). |
| **Estados deshabilitados según selección** | Editar y Eliminar se deshabilitan en ausencia de selección mediante `disableProperty().bind(selectionModel().selectedItemProperty().isNull())`. |
| **Búsqueda/filtro en vivo** | Un `TextField` filtra la tabla al instante por nombre, hora o descripción (sin distinguir mayúsculas) mediante un `FilteredList` que envuelve la lista persistida; texto vacío muestra todos los registros. |
| **Contador de registros** | Un `Label` en el pie refleja el total de checkpoints y, cuando hay filtro activo, el desglose "Mostrando X de N", actualizado con `ListChangeListener`. |
| **Doble clic para editar** | Un `TableRow` personalizado abre el diálogo de edición al hacer doble clic sobre una fila. |
| **Menú contextual** | Clic derecho sobre una fila ofrece "Editar" y "Eliminar", operando sobre el checkpoint de la fila bajo el cursor (no sobre la selección actual). |
| **Atajos de teclado** | `Ctrl+N` crea, `Ctrl+E` edita la fila seleccionada y `Delete` la elimina (con confirmación), vía *accelerators* de la `Scene`. |
| **Confirmación explícita** | Toda acción destructiva (eliminar) pide confirmación con `Alert.AlertType.CONFIRMATION` antes de tocar SQLite. |
| **Feedback modal** | Los errores de BD y validación se reportan con alertas modales (`AlertUtils`), bloqueando la ventana mientras están abiertas. |
| **Foco inicial y aislamiento del texto** | El diálogo enfoca el primer campo al abrir (`requestFocus`); en modo edición selecciona todo el nombre para sobrescribirlo de un toque. |
| **Retroalimentación del alta** | El checkpoint recién creado queda seleccionado y visible en la tabla. La tabla vacía muestra un `placeholder` orientativo. |
| **Pre-información del formulario** | `TextArea` para descripción con 3 filas predefinidas; `promptText` en cada campo con ejemplos reales (p. ej. `Ej. 14:30`). |

### Composición de la Ventana Principal

La ventana principal (`1050×600`, mín. `800×500`) se estructura como un `VBox` con una **barra superior** (título, espaciador, filtro, botones `+ Nuevo Checkpoint` / `Editar` / `Eliminar`), la **`TableView`** con cinco columnas (`PropertyValueFactory`) y un **pie** con contador de registros.

## Lógica del CRUD y Persistencia

La entidad persistida es la tabla `checkpoints`, cuyo esquema se define en `DatabaseManager`:

```sql
CREATE TABLE IF NOT EXISTS checkpoints (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    hora TEXT NOT NULL,
    latitud REAL NOT NULL,
    longitud REAL NOT NULL,
    descripcion TEXT
);
```

| Operación | Método JDBC | Detalle |
|---|---|---|
| **Create** | `CheckpointDAO.insert(Checkpoint)` | `INSERT` con `RETURN_GENERATED_KEYS`; devuelve el **id generado** (`-1` si el driver no lo reportó). |
| **Read** | `CheckpointDAO.selectAll()` | `SELECT *`; devuelve `List<Checkpoint>` (vacía si no hay registros). Se ejecuta al arranque. |
| **Update** | `CheckpointDAO.update(Checkpoint)` | `UPDATE` por `id`; devuelve **filas afectadas** (`1` o `0`). Si falla, la UI se recarga para revertir. |
| **Delete** | `CheckpointDAO.delete(long)` | `DELETE` por `id`; devuelve `boolean`. Solo se remueve de la UI si la BD confirmó el borrado. |

Todas las operaciones:

- Usan `PreparedStatement` con parámetros bindeados — **no concatenación de strings** — mitigando inyección SQL.
- Cierran recursos con `try-with-resources` (Connection, Statement, ResultSet).
- Propagan `SQLException` a la capa de presentación, que la reporta vía `AlertUtils.showError` y la registra con `java.util.logging.Logger`.

### Validaciones de Datos

La validación ocurre en `CheckpointDialog` al confirmar (`validateAndCreate`):

| Campo | Regla | Mensaje |
|---|---|---|
| **Nombre** | Obligatorio (no vacío tras `trim()`). | `Por favor ingrese un nombre para el Checkpoint.` |
| **Hora** | Formato `HH:mm` validado con `LocalTime.parse` + `DateTimeFormatter`. | `La hora debe tener el formato HH:mm (Ej. 14:30).` |
| **Latitud** | Decimal en el rango `[-90, 90]`. | `La latitud debe estar entre -90.0 y 90.0.` |
| **Longitud** | Decimal en el rango `[-180, 180]`. | `La longitud debe estar entre -180.0 y 180.0.` |
| **Coordenadas** | Parsing numérico con coma o punto (`replace(',', '.')`); si falla, se informa formato inválido. | `Latitud y Longitud deben ser valores numéricos decimales.` |

La **entrada se restringe en tiempo real** con `TextFormatter` (el rango se valida al guardar):

```java
// Permite un decimal: signo opcional, dígitos, y un separador , o . con
// dígitos decimales opcionales.
private static final String DECIMAL_PATTERN = "-?\\d*([.,]\\d*)?";

// Permite hora parcial: hasta dos dígitos y un ':' mientras se escribe.
private static final String HORA_PATTERN = "\\d{0,2}:?\\d{0,2}";
```

El patrón decimal **no limita la cantidad de dígitos ni el rango**; el rango se valida al momento de guardar. En modo edición, el diálogo **precarga la fila seleccionada**; en modo creación arranca con todos los campos vacíos.

## Guía de Instalación y Ejecución

### Prerrequisitos

| Herramienta | Versión mínima | Verificación |
|---|---|---|
| **JDK** | 17+ | `java -version` |
| **Maven** | 3.x | `mvn -version` |

> En entornos Linux, JavaFX requiere las bibliotecas del sistema **GTK3** presentes (el plugin `javafx-maven-plugin` descarga el runtime de OpenJFX automáticamente).

### Pasos para Clonar

```bash
git clone https://github.com/TesinaTrekking/bitacora.git
cd bitacora
```

### Compilar

```bash
mvn clean compile
```

Compila el proyecto con limpieza previa (`target/`). La única verificación automatizada del proyecto es que la compilación finalice sin errores.

### Ejecutar

```bash
mvn javafx:run
```

Lanza la aplicación JavaFX. Al primer arranque se crea el directorio `~/.bitacora-checkpoints/` y la base de datos `checkpoints.db` con el esquema de la tabla `checkpoints`.

### Notas de Ejecución

- La aplicación **requiere un entorno gráfico** para mostrar la ventana (no ejecuta en modo headless).