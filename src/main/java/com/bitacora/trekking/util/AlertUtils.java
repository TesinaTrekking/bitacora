package com.bitacora.trekking.util;

import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * Utilidades para mostrar alertas modales compartidas por toda la aplicación.
 */
public final class AlertUtils {

    private AlertUtils() {
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
