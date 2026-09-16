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
├── docs/
│   └── diagrama_entidad_relacion.md          # Modelo entidad-relación y diccionario de datos
└── src/
    └── main/
        ├── java/com/bitacora/trekking/
        │   ├── App.java                              # Entry point JavaFX
        │   ├── controller/
        │   │   ├── MainController.java               # Ventana principal
        │   │   └── CheckpointDialog.java             # Diálogo crear/editar con validación inline
        │   ├── dao/
        │   │   ├── CheckpointDAO.java                # Operaciones CRUD
        │   │   └── DatabaseManager.java              # Conexión + esquema
        │   ├── model/
        │   │   └── Checkpoint.java                   # Entidad observable
        │   └── util/
        │       └── AlertUtils.java                   # Alertas modales y aplicación de tema CSS
        └── resources/
            └── css/
                └── styles.css                        # Hoja de estilos (tema claro outdoor trekking)
```

### Patrón de Conexión por Operación

`CheckpointDAO` abre una **nueva `Connection` por operación** (sin pooling). Aceptable para este tamaño de app; documentado para una eventual migración a pooling.

## Metodología y Decisiones de UI/UX

### Sistema Visual Outdoor y Filosofía de Diseño

La interfaz evoluciona la experiencia de usuario mediante un **tema claro inspirado en trekking y actividades outdoor** (`src/main/resources/css/styles.css`), balanceando una estética limpia y profesional con los requerimientos académicos del proyecto (cero dependencias externas adicionales):

- **Paleta inspirada en la naturaleza**: Acentos en verde bosque (`#2D6A4F`, `#1B4332`), fondos limpios en gris tenue (`#F8FAF9`), tarjetas blancas (`#FFFFFF`) y bordes suaves (`#E2E8F0`).
- **Jerarquía y legibilidad**: Tipografía moderna del sistema para etiquetas y controles, combinada con fuentes monoespaciadas para coordenadas técnicas.
- **Feedback inmediato y no invasivo**: Validación inline que guía al usuario directamente en el formulario sin interrumpir el flujo con ventanas emergentes.

### Reglas de UX Aplicadas

| Regla | Implementación |
|---|---|
| **Alineación en grid** | Los formularios usan `GridPane` de dos columnas (etiqueta \| campo) con espaciado de `12px` y alineación consistente. |
| **Espaciado uniforme** | La ventana principal usa `VBox` con espaciado de `14px` e `Insets` de `14px`; la barra superior actúa como tarjeta blanca con padding y bordes redondeados. |
| **Jerarquía semántica de botones** | Botón primario verde (`.btn-primary`) para `+ Nuevo Checkpoint`, secundario neutro (`.btn-secondary`) para `Editar` y peligro (`.btn-danger`) en rojo suave para `Eliminar`. |
| **Botón principal por defecto** | El botón de confirmación responde a **Enter** (`setDefaultButton(true)`). |
| **Cancelación con Escape** | El botón Cancelar responde a **Escape** (`setCancelButton(true)`). |
| **Estados deshabilitados según selección** | Editar y Eliminar se deshabilitan en ausencia de selección mediante `disableProperty().bind(selectionModel().selectedItemProperty().isNull())`. |
| **Búsqueda/filtro en vivo con botón '✕'** | Un `TextField` filtra la tabla al instante por nombre, hora o descripción mediante `FilteredList`. Incluye un botón interactivo `✕` a la derecha que aparece solo cuando hay texto y permite limpiarlo de un toque. |
| **Coordenadas técnicas monoespaciadas** | Las columnas de Latitud y Longitud utilizan tipografía monoespaciada alineada a la derecha (`.coordinate-cell`), garantizando una alineación decimal ordenada y limpia. |
| **Contador de registros** | Un badge estilizado (`.counter-badge`) en el pie refleja el total de checkpoints y, cuando hay filtro activo, el desglose "Mostrando X de N", actualizado con `ListChangeListener`. |
| **Doble clic para editar** | Un `TableRow` personalizado abre el diálogo de edición al hacer doble clic sobre una fila. |
| **Menú contextual** | Clic derecho sobre una fila ofrece "Editar" y "Eliminar", operando sobre el checkpoint de la fila bajo el cursor (no sobre la selección previa). |
| **Atajos de teclado** | `Ctrl+N` crea, `Ctrl+E` edita la fila seleccionada y `Delete` la elimina (con confirmación), vía *accelerators* de la `Scene`. |
| **Confirmación explícita** | Toda acción destructiva (eliminar) pide confirmación con diálogo modal estilizado antes de tocar SQLite. |
| **Feedback de validación inline** | En lugar de alertas modales que interrumpen la edición, los errores del formulario se informan con un banner superior en el diálogo y un borde rojo (`.field-error`) sobre el campo observado, conservando el foco para corregirlo inmediatamente. |
| **Foco inicial y reactividad** | El diálogo enfoca el primer campo al abrir (`requestFocus`) y selecciona el nombre en edición. Al tipear en un campo con error, el feedback visual se limpia de inmediato. |
| **Retroalimentación del alta** | El checkpoint recién creado queda seleccionado y visible en la tabla. La tabla vacía muestra un `placeholder` orientativo estilizado. |
| **Pre-información del formulario** | `TextArea` para descripción con 3 filas predefinidas; `promptText` en cada campo con ejemplos reales (p. ej. `Ej. 14:30`). |

### Composición de la Ventana Principal

La ventana principal (`1080×640`, mín. `850×520`) se estructura como un `VBox` con una **barra superior unificada** (identidad de marca con icono de montaña y subtítulo, espaciador elástico, buscador interactivo con botón de limpieza y botones de acción semánticos), la **`TableView`** con cinco columnas estilizadas y un **pie** con el badge contador de registros.

## Lógica del CRUD y Persistencia

La entidad persistida es la tabla `checkpoints`, cuyo esquema se define en `DatabaseManager`. Para un análisis detallado del modelo conceptual, relaciones y diccionario de datos, consultar [docs/diagrama_entidad_relacion.md](docs/diagrama_entidad_relacion.md).

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

La validación ocurre en `CheckpointDialog` al confirmar (`validateInput`), interceptando la acción mediante un filtro de eventos (`addEventFilter(ActionEvent.ACTION, ...)`). Si algún dato es inválido, se consume el evento (`event.consume()`), impidiendo el cierre del diálogo y desplegando el banner de error superior junto al resaltado (`.field-error`) del campo problemático:

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