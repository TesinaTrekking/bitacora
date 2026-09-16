package com.bitacora.trekking.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;

/**
 * Utilidades para mostrar alertas modales y aplicar el tema visual compartido.
 */
public final class AlertUtils {

    private AlertUtils() {
    }

    /**
     * Aplica la hoja de estilos de la aplicación a cualquier diálogo JavaFX.
     *
     * @param dialog Diálogo al que se le aplicarán los estilos
     */
    public static void applyTheme(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }
        DialogPane pane = dialog.getDialogPane();
        if (pane == null) {
            return;
        }
        URL cssUrl = AlertUtils.class.getResource("/css/styles.css");
        if (cssUrl != null) {
            String cssPath = cssUrl.toExternalForm();
            if (!pane.getStylesheets().contains(cssPath)) {
                pane.getStylesheets().add(cssPath);
            }
        }
    }

    public static void showError(Window owner, String title, String message) {
        showAlert(Alert.AlertType.ERROR, owner, title, message);
    }

    public static void showWarning(Window owner, String title, String message) {
        showAlert(Alert.AlertType.WARNING, owner, title, message);
    }

    private static void showAlert(Alert.AlertType type, Window owner, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        configureModal(alert, owner);
        applyTheme(alert);
        alert.showAndWait();
    }

    private static void configureModal(Alert alert, Window owner) {
        if (owner != null) {
            alert.initOwner(owner);
        }
        // Bloquea la ventana principal mientras está abierta, conservando título/propiedades propias.
        alert.initModality(Modality.WINDOW_MODAL);
    }
}
