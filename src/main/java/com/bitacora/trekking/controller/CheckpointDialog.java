package com.bitacora.trekking.controller;

import com.bitacora.trekking.model.Checkpoint;
import com.bitacora.trekking.util.AlertUtils;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Diálogo modal de creación o edición de un checkpoint.
 *
 * <p>En modo creación los campos arrancan vacíos; en modo edición se precargan
 * los valores del checkpoint seleccionado. La validación ocurre al confirmar.</p>
 */
public class CheckpointDialog extends Dialog<Checkpoint> {

    private static final double LAT_MIN = -90.0;
    private static final double LAT_MAX = 90.0;
    private static final double LON_MIN = -180.0;
    private static final double LON_MAX = 180.0;
    private static final DateTimeFormatter HORA_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withLocale(Locale.ROOT);

    private static final String DECIMAL_PATTERN = "-?\\d*([.,]\\d*)?";
    private static final String HORA_PATTERN = "\\d{0,2}:?\\d{0,2}";

    private final TextField txtNombre = new TextField();
    private final TextField txtHora = new TextField();
    private final TextField txtLatitud = new TextField();
    private final TextField txtLongitud = new TextField();
    private final TextArea txtDescripcion = new TextArea();

    private final Checkpoint existingCheckpoint;

    public CheckpointDialog(Window owner, Checkpoint checkpointToEdit) {
        this.existingCheckpoint = checkpointToEdit;

        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle(checkpointToEdit == null ? "Crear Nuevo Checkpoint" : "Editar Checkpoint");

        DialogPane dialogPane = getDialogPane();
        configureButtons(dialogPane, checkpointToEdit != null);
        configureForm(dialogPane, checkpointToEdit);
        configureResultConverter(dialogPane);
    }

    private void configureButtons(DialogPane dialogPane, boolean isEditing) {
        // "Cancelar" queda a la izquierda del botón principal por la convención de ButtonData.
        ButtonType confirmar = new ButtonType(isEditing ? "Guardar" : "Confirmar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(confirmar, cancelar);

        // Enter dispara la acción principal y Escape cierra el diálogo.
        Button btnConfirmar = (Button) dialogPane.lookupButton(confirmar);
        btnConfirmar.setDefaultButton(true);
        Button btnCancelar = (Button) dialogPane.lookupButton(cancelar);
        btnCancelar.setCancelButton(true);
    }

    private void configureForm(DialogPane dialogPane, Checkpoint checkpointToEdit) {
        Label header = new Label(checkpointToEdit == null ? "Crear Nuevo Checkpoint" : "Editar Checkpoint");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setHalignment(HPos.LEFT);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHalignment(HPos.LEFT);
        fieldColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);

        txtNombre.setPromptText("Ej. Checkpoint Alfa");
        txtHora.setPromptText("Ej. 14:30");
        txtLatitud.setPromptText("Ej. -31.4135");
        txtLongitud.setPromptText("Ej. -64.1810");
        txtDescripcion.setPromptText("Descripción opcional de la parada o control...");
        txtDescripcion.setPrefRowCount(3);
        txtDescripcion.setPrefColumnCount(20);
        GridPane.setFillWidth(txtDescripcion, true);

        // Restringe la entrada; el rango decimal se valida al guardar.
        txtLatitud.setTextFormatter(createDecimalTextFormatter());
        txtLongitud.setTextFormatter(createDecimalTextFormatter());
        txtHora.setTextFormatter(createHoraTextFormatter());

        if (checkpointToEdit != null) {
            txtNombre.setText(checkpointToEdit.getNombre());
            txtHora.setText(checkpointToEdit.getHora());
            txtLatitud.setText(String.valueOf(checkpointToEdit.getLatitud()));
            txtLongitud.setText(String.valueOf(checkpointToEdit.getLongitud()));
            txtDescripcion.setText(checkpointToEdit.getDescripcion());
        }

        grid.addRow(0, new Label("Nombre de Checkpoint"), txtNombre);
        grid.addRow(1, new Label("Hora"), txtHora);
        grid.addRow(2, new Label("Latitud"), txtLatitud);
        grid.addRow(3, new Label("Longitud"), txtLongitud);
        grid.addRow(4, new Label("Descripción"), txtDescripcion);

        VBox mainLayout = new VBox(10, header, grid);
        mainLayout.setPadding(new Insets(10));
        dialogPane.setContent(mainLayout);
    }

    private void configureResultConverter(DialogPane dialogPane) {
        setResultConverter(dialogButton ->
                dialogButton != null && dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE
                        ? validateAndCreate()
                        : null);
    }

    private Checkpoint validateAndCreate() {
        String nombre = txtNombre.getText().trim();
        String hora = txtHora.getText().trim();
        String latitudRaw = txtLatitud.getText().trim();
        String longitudRaw = txtLongitud.getText().trim();
        String descripcion = txtDescripcion.getText().trim();

        if (nombre.isEmpty()) {
            showAlert("Campo Requerido", "Por favor ingrese un nombre para el Checkpoint.");
            return null;
        }
        if (!isHoraValida(hora)) {
            showAlert("Formato Inválido", "La hora debe tener el formato HH:mm (Ej. 14:30).");
            return null;
        }

        Double latitud = null;
        Double longitud = null;
        try {
            if (!latitudRaw.isEmpty()) {
                latitud = parseDecimal(latitudRaw);
            }
            if (!longitudRaw.isEmpty()) {
                longitud = parseDecimal(longitudRaw);
            }
        } catch (NumberFormatException ex) {
            showAlert("Formato Inválido", "Latitud y Longitud deben ser valores numéricos decimales.");
            return null;
        }
        if (latitud != null && (latitud < LAT_MIN || latitud > LAT_MAX)) {
            showAlert("Valor Fuera de Rango",
                    "La latitud debe estar entre " + LAT_MIN + " y " + LAT_MAX + ".");
            return null;
        }
        if (longitud != null && (longitud < LON_MIN || longitud > LON_MAX)) {
            showAlert("Valor Fuera de Rango",
                    "La longitud debe estar entre " + LON_MIN + " y " + LON_MAX + ".");
            return null;
        }

        long id = (existingCheckpoint != null) ? existingCheckpoint.getId() : System.currentTimeMillis();
        return new Checkpoint(id, nombre, hora,
                (latitud == null) ? 0.0 : latitud,
                (longitud == null) ? 0.0 : longitud,
                descripcion);
    }

    private static boolean isHoraValida(String hora) {
        try {
            LocalTime.parse(hora, HORA_FORMATTER);
            return true;
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }

    private static double parseDecimal(String raw) {
        return Double.parseDouble(raw.replace(',', '.'));
    }

    /**
     * Restringe la entrada a un número decimal (signo opcional, dígitos y un único
     * separador coma o punto) sin limitar la cantidad de dígitos ni el rango.
     */
    private static TextFormatter<String> createDecimalTextFormatter() {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.isEmpty() || newText.matches(DECIMAL_PATTERN) ? change : null;
        });
    }

    /**
     * Restringe la entrada a un formato HH:mm (dígitos y un único ':').
     */
    private static TextFormatter<String> createHoraTextFormatter() {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.isEmpty() || newText.matches(HORA_PATTERN) ? change : null;
        });
    }

    private void showAlert(String title, String message) {
        AlertUtils.showWarning(getOwner(), title, message);
    }
}
