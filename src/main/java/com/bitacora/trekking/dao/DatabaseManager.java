package com.bitacora.trekking.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestiona la conexión con la base de datos SQLite y la inicialización del esquema.
 *
 * <p>La base de datos vive en un directorio estable del usuario
 * ({@code ~/.bitacora-checkpoints/}), independiente del directorio de trabajo.</p>
 */
public final class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private static final Path DATA_DIR =
            Paths.get(System.getProperty("user.home"), ".bitacora-checkpoints");
    private static final String URL = "jdbc:sqlite:" + DATA_DIR.resolve("checkpoints.db");

    private DatabaseManager() {
    }

    /**
     * Abre una conexión nueva. Para una aplicación pequeña es aceptable abrir y
     * cerrar una conexión por operación dado que SQLite local es liviano.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /**
     * Crea el directorio de datos (si no existe) y el esquema de la base de datos.
     *
     * @throws IllegalStateException si no se puede crear el directorio o ejecutar el esquema
     */
    public static void initializeDatabase() {
        createDataDirectory();

        String sql = "CREATE TABLE IF NOT EXISTS checkpoints ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nombre TEXT NOT NULL, "
                + "hora TEXT NOT NULL, "
                + "latitud REAL NOT NULL, "
                + "longitud REAL NOT NULL, "
                + "descripcion TEXT"
                + ");";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            LOGGER.info("Base de datos inicializada correctamente en " + DATA_DIR);
        } catch (SQLException e) {
            String error = "Error al inicializar la base de datos";
            LOGGER.log(Level.SEVERE, error, e);
            throw new IllegalStateException(error, e);
        }
    }

    private static void createDataDirectory() {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException e) {
            String error = "No se pudo crear el directorio de datos: " + DATA_DIR;
            LOGGER.log(Level.SEVERE, error, e);
            throw new IllegalStateException(error, e);
        }
    }
}
