# Bitácora Checkpoints

Sistema desktop de gestión de puntos de control (checkpoints) para bitácoras de ruta, con operaciones **CRUD** completas sobre una base de datos **SQLite** local. La interfaz está construida enteramente en **JavaFX** a través de código (sin FXML), siguiendo una arquitectura en capas.

El proyecto es un **prototipo de baja fidelidad** con fines académicos: su objetivo es demostrar un flujo CRUD completo y funcional (crear, listar, editar y eliminar), priorizando la usabilidad y la correcta separación de responsabilidades por sobre el refinamiento visual.

## Índice

- [Propósito Académico](#propósito-académico)
- [Stack Técnico](#stack-técnico)
- [Arquitectura y Patrones de Diseño](#arquitectura-y-patrones-de-diseño)
- [Metodología y Decisiones de UI/UX (Baja Fidelidad)](#metodología-y-decisiones-de-uiux-baja-fidelidad)
- [Lógica del CRUD y Persistencia](#lógica-del-crud-y-persistencia)
- [Guía de Instalación y Ejecución](#guía-de-instalación-y-ejecución)

## Propósito Académico

El proyecto se enmarca en un contexto universitario y persigue dos objetivos de formación:

1. **Prototipado rápido de baja fidelidad** de un sistema desktop: la aplicación implementa el ciclo completo de una herramienta de escritorio real con persistencia, sin invertir esfuerzo en estética, temas o estilos visuales.
2. **Práctica de CRUD persistente**: ejercitar las cuatro operaciones fundamentales de un sistema de información (Create, Read, Update, Delete) contra una base de datos relacional, integrando una API gráfica moderna.

El dominio elegido — una bitácora de ruta con puntos de control georreferenciados — permite modelar una entidad con atributos de distintos tipos (texto, hora y coordenadas numéricas), lo que obliga a implementar validación de datos y conversión de tipos.

## Stack Técnico

| Tecnología | Versión | Rol |
|---|---|---|
| **Java** | 17 | Lenguaje de programación y plataforma de ejecución |
| **JavaFX** | 21.0.2 | Framework de interfaz gráfica (OpenJFX) |
| **Maven** | 3.x | Gestión de dependencias y ciclo de vida de compilación |
| **SQLite** | — (vía `sqlite-jdbc` 3.45.2.0) | Motor de base de datos relacional embebida |

> **Nota**: Java, JavaFX y Maven fueron definidos por la consigna de la materia, por lo que aquí no se defiende su elección sino que se documenta su rol técnico dentro del prototipo.

### Lenguaje: Java

**Java 17** aporta el tipado estático del compilador (detecta errores de tipo en tiempo de compilación), el manejo estructurado de excepciones (`try-with-resources`, `SQLException` propagable) y las propiedades observables de JavaFX que alimentan el patrón MVC.

### Interfaz Gráfica: JavaFX

**JavaFX** (OpenJFX 21.0.2) es el framework de presentación: la UI se construye **enteramente por código** (sin FXML) mediante nodos (`Stage`, `Scene`, `Node`) y layouts, y sus **propiedades observables** (`LongProperty`, `StringProperty`, `DoubleProperty`) permiten que la vista reaccione automáticamente a los cambios del modelo sin sincronización manual.

### Gestión de Proyecto: Maven

**Maven** gestiona las dependencias declaradas en `pom.xml` (`javafx-controls`, `sqlite-jdbc`) y el ciclo de vida de compilación y ejecución (`mvn clean compile`, `mvn javafx:run`).

### Base de Datos: SQLite

**SQLite** se eligió como motor de base de datos relacional **severless** (sin servidor), **liviana** (embebida en la aplicación) e **integrada localmente**, lo que la hace ideal para prototipos: no requiere instalación ni configuración de un servicio externo, y el archivo `.db` resultante es portable. La persistencia se implementa con JDBC a través del driver `org.xerial:sqlite-jdbc`.

Particularidades de la integración:

- La base de datos **`checkpoints.db`** reside en un directorio estable del usuario (`~/.bitacora-checkpoints/`), independiente del directorio de trabajo del repositorio.
- El **esquema se inicializa automáticamente** al arrancar la aplicación (`CREATE TABLE IF NOT EXISTS`), eliminando pasos manuales de setup.

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

- **`Checkpoint`** — Entidad del dominio que representa un punto de control. Sus atributos se exponen como **propiedades observables de JavaFX** (`LongProperty`, `StringProperty`, `DoubleProperty`) para que la tabla y los diálogos reaccionen en tiempo real a los cambios. No es un POJO simple: cada campo cuenta con getter/setter y su correspondiente método `*Property()`. Atributos: `id`, `nombre`, `hora`, `latitud`, `longitud`, `descripcion`.

### Capa de Persistencia (DAO)

- **`CheckpointDAO`** — Implementa el patrón **Data Access Object (DAO)**: encapsula todas las consultas SQL (JDBC) en métodos de alto nivel, aislando la persistencia de la lógica de presentación. Usa `PreparedStatement` (mitigando inyección SQL) y `try-with-resources` para gestión automática de recursos. Expone `insert`, `selectAll`, `update` y `delete`, y **propaga `SQLException`** para que la UI informe el motivo real del fallo.

### Capa de Conexión (Util / Manager)

- **`DatabaseManager`** — Factory de conexiones (`DriverManager.getConnection`) y **inicialización automática del esquema**: crea el directorio de datos `~/.bitacora-checkpoints/` y ejecuta el `CREATE TABLE IF NOT EXISTS` para la tabla `checkpoints`. Es una clase final con constructor privado (utilitaria).
- **`AlertUtils`** — Utilidades compartidas de alertas modales (`showError` / `showWarning`) para errores de base de datos y validación, evitando duplicación de código de diálogos.

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

`CheckpointDAO` abre y cierra una **nueva `Connection` por operación** (sin pooling). Para el tamaño de este prototipo es aceptable porque SQLite local es liviano; la decisión queda documentada en el código con miras a una eventual migración a pooling si la aplicación escalara.

## Metodología y Decisiones de UI/UX (Baja Fidelidad)

### Filosofía del Prototipo

El prototipo concentra el esfuerzo en tres ejes funcionales por encima de lo visual:

1. **Usabilidad**: flujo intuitivo y consistentemente accesible.
2. **Flujo lógico**: orden de acciones claro (seleccionar → editar/eliminar; completar formulario → confirmar).
3. **Funcionalidad CRUD completa**: cada operación es observable y verificable en la tabla y en la base de datos.

No se utilizan hojas de estilo (CSS) ni paletas de colores: la aplicación conserva el **tema estándar Modena** que distribuye JavaFX, dejando explícita la prioridad de la lógica por sobre la estética.

### Reglas de UX Aplicadas

| Regla | Implementación |
|---|---|
| **Alineación en grid** | Los formularios usan `GridPane` de dos columnas (etiqueta \| campo) con alineación consistente. |
| **Espaciado uniforme** | `hgap`/`vgap = 10` entre celdas del grid y `padding` de `10` en el diálogo; la ventana principal usa `VBox` con espaciado de `15` y `Insets` de `15`. |
| **Botón principal por defecto** | El botón de confirmación responde a **Enter** (`setDefaultButton(true)`). |
| **Cancelación con Escape** | El botón Cancelar responde a **Escape** (`setCancelButton(true)`). |
| **Estados deshabilitados según selección** | Editar y Eliminar se deshabilitan en ausencia de selección mediante `disableProperty().bind(selectionModel().selectedItemProperty().isNull())`. |
| **Confirmación explícita** | Toda acción destructiva (eliminar) pide confirmación con `Alert.AlertType.CONFIRMATION` antes de tocar SQLite. |
| **Feedback modal** | Los errores de BD y validación se reportan con alertas modales (`AlertUtils`), bloqueando la ventana mientras están abiertas. |
| **Pre-información del formulario** | `TextArea` para descripción con 3 filas predefinidas; `promptText` en cada campo con ejemplos reales (p. ej. `Ej. 14:30`). |

### Composición de la Ventana Principal

La ventana principal (`950×600`, mínima `800×500`) se estructura como un `VBox` con una **barra superior** (título, espaciador elástico y botones `+ Nuevo Checkpoint`, `Editar`, `Eliminar`) y una **`TableView`** con cinco columnas que mapean los atributos del modelo mediante `PropertyValueFactory`: `Hora`, `Checkpoint / Nombre`, `Latitud`, `Longitud`, `Descripción`.

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
| **Create** | `CheckpointDAO.insert(Checkpoint)` | `INSERT` con `RETURN_GENERATED_KEYS`; devuelve el **id real generado** por SQLite (`-1` si el driver no lo reportó), que luego se refleja en la fila de la tabla. |
| **Read** | `CheckpointDAO.selectAll()` | `SELECT *` sobre la tabla; mapea cada fila a un `Checkpoint` y devuelve una `List` (vacía si no hay registros). Se ejecuta al arranque para poblar la tabla. |
| **Update** | `CheckpointDAO.update(Checkpoint)` | `UPDATE` por `id`; devuelve la **cantidad de filas afectadas** (`1` si se actualizó, `0` si el id no existe). Si la persistencia falla, la UI se recarga para descartar cambios fantasma. |
| **Delete** | `CheckpointDAO.delete(long)` | `DELETE` por `id`; devuelve `boolean` (`true` si eliminó alguna fila). Requiere confirmación previa y sólo se remueve la fila de la UI cuando la BD confirmó el borrado. |

Todas las operaciones:

- Usan `PreparedStatement` con parámetros bindeados — **no concatenación de strings** — mitigando inyección SQL.
- Cierran recursos con `try-with-resources` (Connection, Statement, ResultSet).
- Propaguan `SQLException` a la capa de presentación, que la reporta vía `AlertUtils.showError` y la registra con `java.util.logging.Logger`.

### Validaciones de Datos

La validación ocurre en `CheckpointDialog` al confirmar (`validateAndCreate`):

| Campo | Regla | Mensaje |
|---|---|---|
| **Nombre** | Obligatorio (no vacío tras `trim()`). | `Por favor ingrese un nombre para el Checkpoint.` |
| **Hora** | Formato `HH:mm` validado con `LocalTime.parse` + `DateTimeFormatter`. | `La hora debe tener el formato HH:mm (Ej. 14:30).` |
| **Latitud** | Decimal en el rango `[-90, 90]`. | `La latitud debe estar entre -90.0 y 90.0.` |
| **Longitud** | Decimal en el rango `[-180, 180]`. | `La longitud debe estar entre -180.0 y 180.0.` |
| **Coordenadas** | Parsing numérico con coma o punto (`replace(',', '.')`); si falla, se informa formato inválido. | `Latitud y Longitud deben ser valores numéricos decimales.` |

Adicionalmente, la **entrada se restringe en tiempo real** con `TextFormatter`:

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
# 1. Clonar el repositorio
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