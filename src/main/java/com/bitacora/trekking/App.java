package com.bitacora.trekking;

import com.bitacora.trekking.controller.MainController;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Punto de entrada de la aplicación. Delega la construcción de la interfaz y la
 * lógica de la ventana principal en {@link MainController}.
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainController controller = new MainController();
        controller.initialize(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
