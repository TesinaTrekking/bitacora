package com.bitacora.trekking.controller;

import com.bitacora.trekking.dao.CheckpointDAO;
import com.bitacora.trekking.dao.DatabaseManager;
import com.bitacora.trekking.model.Checkpoint;
import com.bitacora.trekking.util.AlertUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador de la ventana principal: construye la interfaz, carga los datos
 * y orquesta las acciones de crear, editar y eliminar checkpoints.
 */
public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());
    private static final double SCENE_WIDTH = 950.0;
    private static final double SCENE_HEIGHT = 600.0;

    private final CheckpointDAO checkpointDAO = new CheckpointDAO();
    private final ObservableList<Checkpoint> checkpointList = FXCollections.observableArrayList();

    private TableView<Checkpoint> tableView;
    private Stage primaryStage;

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
        tableView = new TableView<>(checkpointList);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        configureColumns();

        VBox mainLayout = new VBox(15, buildTopBar(), tableView);
        mainLayout.setPadding(new Insets(15));
        VBox.setVgrow(tableView, Priority.ALWAYS);

        return new Scene(mainLayout, SCENE_WIDTH, SCENE_HEIGHT);
    }

    private HBox buildTopBar() {
        Label titleLabel = new Label("Bitácora");

        Button newButton = new Button("+ Nuevo Checkpoint");
        newButton.setOnAction(event -> showCreateDialog());

        // Editar/Eliminar actúan sobre la fila seleccionada: se deshabilitan sin selección.
        Button editButton = new Button("Editar");
        editButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
        editButton.setOnAction(event -> {
            Checkpoint selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditDialog(selected);
            }
        });

        Button deleteButton = new Button("Eliminar");
        deleteButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.setOnAction(event -> {
            Checkpoint selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                confirmAndDelete(selected);
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(15, titleLabel, spacer, newButton, editButton, deleteButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        return topBar;
    }

    private void configureColumns() {
        TableColumn<Checkpoint, String> horaColumn = new TableColumn<>("Hora");
        horaColumn.setCellValueFactory(new PropertyValueFactory<>("hora"));

        TableColumn<Checkpoint, String> nombreColumn = new TableColumn<>("Checkpoint / Nombre");
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        TableColumn<Checkpoint, Number> latitudColumn = new TableColumn<>("Latitud");
        latitudColumn.setCellValueFactory(new PropertyValueFactory<>("latitud"));

        TableColumn<Checkpoint, Number> longitudColumn = new TableColumn<>("Longitud");
        longitudColumn.setCellValueFactory(new PropertyValueFactory<>("longitud"));

        TableColumn<Checkpoint, String> descripcionColumn = new TableColumn<>("Descripción");
        descripcionColumn.setCellValueFactory(new PropertyValueFactory<>("descripcion"));

        tableView.getColumns().addAll(horaColumn, nombreColumn, latitudColumn, longitudColumn, descripcionColumn);
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
