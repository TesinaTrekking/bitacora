package com.bitacora.trekking.controller;

import com.bitacora.trekking.dao.CheckpointDAO;
import com.bitacora.trekking.dao.DatabaseManager;
import com.bitacora.trekking.model.Checkpoint;
import com.bitacora.trekking.util.AlertUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador de la ventana principal: construye la interfaz, carga los datos
 * y orquesta las acciones de crear, editar y eliminar checkpoints.
 */
public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());
    private static final double SCENE_WIDTH = 1050.0;
    private static final double SCENE_HEIGHT = 600.0;

    private final CheckpointDAO checkpointDAO = new CheckpointDAO();
    private final ObservableList<Checkpoint> checkpointList = FXCollections.observableArrayList();

    // Vista filtrada sobre los datos completos: la tabla muestra el subconjunto
    // que cumple el filtro de texto, sin modificar la lista persistida.
    private final FilteredList<Checkpoint> filteredList = new FilteredList<>(checkpointList, checkpoint -> true);

    private TableView<Checkpoint> tableView;
    private Stage primaryStage;
    private Label counterLabel;

    /**
     * Inicializa la base de datos, carga los datos y muestra la ventana principal.
     *
     * @return {@code false} si la inicialización o la carga de datos falló
     */
    public boolean initialize(Stage primaryStage) {
        this.primaryStage = primaryStage;
        if (!initializeDatabase()) {
            return false;
        }
        if (!loadDataFromDatabase()) {
            return false;
        }

        primaryStage.setTitle("Gestión de Ruta - Bitácora (Conectado a SQLite)");
        primaryStage.setScene(buildScene());
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.show();
        return true;
    }

    private Scene buildScene() {
        // La tabla se crea antes que la barra para enlazar el estado de los botones
        // de acción a la selección actual.
        tableView = new TableView<>(filteredList);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setPlaceholder(new Label("No hay checkpoints registrados. Use \"+ Nuevo Checkpoint\" para agregar uno."));
        configureColumns();
        configureRowInteractions();

        VBox mainLayout = new VBox(15, buildTopBar(), tableView, buildFooter());
        mainLayout.setPadding(new Insets(15));
        VBox.setVgrow(tableView, Priority.ALWAYS);

        Scene scene = new Scene(mainLayout, SCENE_WIDTH, SCENE_HEIGHT);
        configureShortcuts(scene);
        return scene;
    }

    private HBox buildTopBar() {
        Label titleLabel = new Label("Bitácora");

        TextField filterField = new TextField();
        filterField.setPromptText("Filtrar por nombre, hora o descripción…");
        filterField.setPrefWidth(240);
        filterField.textProperty().addListener((obs, oldValue, newValue) ->
                filteredList.setPredicate(createFilterPredicate(newValue)));

        Button newButton = new Button("+ Nuevo Checkpoint");
        newButton.setOnAction(event -> showCreateDialog());

        // Editar/Eliminar actúan sobre la fila seleccionada: se deshabilitan sin selección.
        Button editButton = new Button("Editar");
        editButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
        editButton.setOnAction(event -> editSelected());

        Button deleteButton = new Button("Eliminar");
        deleteButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.setOnAction(event -> deleteSelected());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(15, titleLabel, spacer, filterField, newButton, editButton, deleteButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        return topBar;
    }

    private HBox buildFooter() {
        counterLabel = new Label();
        updateCounter();
        // El contador refleja tanto el total persistido como la cantidad visible
        // tras aplicar el filtro de texto (desambiguar "Mostrando X de Y").
        checkpointList.addListener((ListChangeListener<Checkpoint>) change -> updateCounter());
        filteredList.addListener((ListChangeListener<Checkpoint>) change -> updateCounter());

        HBox footer = new HBox(counterLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(0, 20, 0, 20));
        return footer;
    }

    private void updateCounter() {
        int total = checkpointList.size();
        int visibles = filteredList.size();
        String unidad = total == 1 ? " checkpoint" : " checkpoints";
        counterLabel.setText(total == visibles
                ? total + unidad
                : "Mostrando " + visibles + " de " + total + unidad);
    }

    /**
     * Predicate para el filtro en vivo: matchea (sin distinción de mayúsculas)
     * nombre, hora o descripción; texto vacío muestra todos los registros.
     */
    private static Predicate<Checkpoint> createFilterPredicate(String texto) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        if (filtro.isEmpty()) {
            return checkpoint -> true;
        }
        return checkpoint -> containsIgnoreCase(checkpoint.getNombre(), filtro)
                || containsIgnoreCase(checkpoint.getHora(), filtro)
                || containsIgnoreCase(checkpoint.getDescripcion(), filtro);
    }

    private static boolean containsIgnoreCase(String valor, String filtro) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(filtro);
    }

    private void configureShortcuts(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                this::showCreateDialog);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN),
                this::editSelected);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DELETE), this::deleteSelected);
    }

    private void editSelected() {
        Checkpoint selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showEditDialog(selected);
        }
    }

    private void deleteSelected() {
        Checkpoint selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            confirmAndDelete(selected);
        }
    }

    /**
     * Doble clic para editar y menú contextual por fila: ambos operan sobre el
     * checkpoint de la fila bajo el cursor (no sobre la selección, que puede no
     * reflejar un clic derecho).
     */
    private void configureRowInteractions() {
        tableView.setRowFactory(tv -> {
            TableRow<Checkpoint> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem editItem = new MenuItem("Editar");
            editItem.setOnAction(event -> {
                Checkpoint item = row.getItem();
                if (item != null) {
                    showEditDialog(item);
                }
            });
            MenuItem deleteItem = new MenuItem("Eliminar");
            deleteItem.setOnAction(event -> {
                Checkpoint item = row.getItem();
                if (item != null) {
                    confirmAndDelete(item);
                }
            });
            contextMenu.getItems().addAll(editItem, deleteItem);
            row.setContextMenu(contextMenu);
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showEditDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private void configureColumns() {
        TableColumn<Checkpoint, String> horaColumn = new TableColumn<>("Hora");
        horaColumn.setCellValueFactory(new PropertyValueFactory<>("hora"));
        horaColumn.setPrefWidth(80);

        TableColumn<Checkpoint, String> nombreColumn = new TableColumn<>("Checkpoint / Nombre");
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        nombreColumn.setPrefWidth(230);

        TableColumn<Checkpoint, Number> latitudColumn = new TableColumn<>("Latitud");
        latitudColumn.setCellValueFactory(new PropertyValueFactory<>("latitud"));
        latitudColumn.setPrefWidth(110);
        setNumericCellFactory(latitudColumn);

        TableColumn<Checkpoint, Number> longitudColumn = new TableColumn<>("Longitud");
        longitudColumn.setCellValueFactory(new PropertyValueFactory<>("longitud"));
        longitudColumn.setPrefWidth(110);
        setNumericCellFactory(longitudColumn);

        TableColumn<Checkpoint, String> descripcionColumn = new TableColumn<>("Descripción");
        descripcionColumn.setCellValueFactory(new PropertyValueFactory<>("descripcion"));

        tableView.getColumns().addAll(horaColumn, nombreColumn, latitudColumn, longitudColumn, descripcionColumn);
    }

    /**
     * Las columnas numéricas de coordenadas se alinean a la derecha y recortan
     * los decimales redundantes para una lectura más limpia.
     */
    private static void setNumericCellFactory(TableColumn<Checkpoint, Number> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "" : formatCoordinate(value.doubleValue()));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
    }

    private static String formatCoordinate(double value) {
        if (value == 0.0) {
            return "0.0";
        }
        String raw = String.valueOf(value);
        if (!raw.contains(".")) {
            return raw;
        }
        String trimmed = raw.replaceAll("0+$", "").replaceAll("\\.$", "");
        return trimmed.isEmpty() ? "0.0" : trimmed;
    }

    private boolean initializeDatabase() {
        try {
            DatabaseManager.initializeDatabase();
            return true;
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "No se pudo inicializar la base de datos", e);
            AlertUtils.showError(primaryStage, "Error de base de datos",
                    "No se pudo inicializar la base de datos.\n" + e.getMessage());
            Platform.exit();
            return false;
        }
    }

    private boolean loadDataFromDatabase() {
        checkpointList.clear();
        try {
            checkpointList.addAll(checkpointDAO.selectAll());
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar los datos desde la base de datos", e);
            AlertUtils.showError(primaryStage, "Error de base de datos",
                    "No se pudieron cargar los checkpoints.\n" + e.getMessage());
            return false;
        }
    }

    private void showCreateDialog() {
        CheckpointDialog dialog = new CheckpointDialog(primaryStage, null);
        Optional<Checkpoint> result = dialog.showAndWait();
        result.ifPresent(newCheckpoint -> {
            // Se persiste primero y se usa el id real generado para reflejar la fila en la UI.
            try {
                long newId = checkpointDAO.insert(newCheckpoint);
                if (newId != -1) {
                    newCheckpoint.setId(newId);
                    checkpointList.add(newCheckpoint);
                    // El registro recién creado queda seleccionado y visible para
                    // confirmar visualmente el alta.
                    tableView.getSelectionModel().select(newCheckpoint);
                    tableView.scrollTo(newCheckpoint);
                } else {
                    AlertUtils.showError(primaryStage, "Error al guardar",
                            "No se pudo guardar el checkpoint en la base de datos.");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al insertar checkpoint", e);
                AlertUtils.showError(primaryStage, "Error al guardar",
                        "No se pudo guardar el checkpoint en la base de datos.\n" + e.getMessage());
            }
        });
    }

    private void showEditDialog(Checkpoint item) {
        CheckpointDialog dialog = new CheckpointDialog(primaryStage, item);
        Optional<Checkpoint> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            item.setNombre(updated.getNombre());
            item.setHora(updated.getHora());
            item.setLatitud(updated.getLatitud());
            item.setLongitud(updated.getLongitud());
            item.setDescripcion(updated.getDescripcion());

            // Si la persistencia falla, se recargan los datos para revertir la UI.
            try {
                checkpointDAO.update(item);
                tableView.refresh();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al actualizar checkpoint id=" + item.getId(), e);
                loadDataFromDatabase();
                AlertUtils.showError(primaryStage, "Error al actualizar",
                        "No se pudieron guardar los cambios del checkpoint en la base de datos.\n" + e.getMessage());
            }
        });
    }

    private void confirmAndDelete(Checkpoint item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText("¿Eliminar Checkpoint?");
        alert.setContentText(
                "¿Está seguro de que desea eliminar '" + item.getNombre() + "'? Esta acción no se puede deshacer.");
        alert.initOwner(primaryStage);
        alert.initModality(Modality.WINDOW_MODAL);

        ButtonType confirmDelete = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmDelete, cancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == confirmDelete) {
            // Sólo se remueve de la UI si la BD confirmó el borrado, para mantener
            // la interfaz y la persistencia sincronizadas.
            try {
                if (checkpointDAO.delete(item.getId())) {
                    checkpointList.remove(item);
                } else {
                    AlertUtils.showError(primaryStage, "Error al eliminar",
                            "No se pudo eliminar el checkpoint de la base de datos.");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al eliminar checkpoint id=" + item.getId(), e);
                AlertUtils.showError(primaryStage, "Error al eliminar",
                        "No se pudo eliminar el checkpoint de la base de datos.\n" + e.getMessage());
            }
        }
    }
}