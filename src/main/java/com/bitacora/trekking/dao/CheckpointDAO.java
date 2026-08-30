package com.bitacora.trekking.dao;

import com.bitacora.trekking.model.Checkpoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos (CRUD) de checkpoints contra la base de datos SQLite.
 *
 * <p>Todos los métodos abren y cierran sus recursos con {@code try-with-resources}
 * y propagan {@link SQLException} para que la capa de presentación informe al
 * usuario el motivo real del fallo.</p>
 */
public class CheckpointDAO {

    /**
     * Inserta un checkpoint en la base de datos y devuelve su identificador generado.
     *
     * @param checkpoint checkpoint a persistir (sin id o con id provisional)
     * @return el id real asignado por SQLite, o {@code -1} si el driver no lo reportó
     * @throws SQLException si falla la operación
     */
    public long insert(Checkpoint checkpoint) throws SQLException {
        String sql = "INSERT INTO checkpoints(nombre, hora, latitud, longitud, descripcion) VALUES(?,?,?,?,?)";
        long generatedId = -1;

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindCheckpointValues(pstmt, checkpoint);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedId = rs.getLong(1);
                }
            }
        }
        return generatedId;
    }

    /**
     * Devuelve todos los checkpoints de la tabla, en el orden de la base de datos.
     *
     * @return lista de checkpoints (vacía si no hay registros)
     * @throws SQLException si falla la operación
     */
    public List<Checkpoint> selectAll() throws SQLException {
        String sql = "SELECT * FROM checkpoints";
        List<Checkpoint> checkpoints = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                checkpoints.add(mapRow(rs));
            }
        }
        return checkpoints;
    }

    /**
     * Actualiza los campos de un checkpoint existente identificado por su id.
     *
     * @param checkpoint checkpoint con los valores actualizados
     * @return cantidad de filas afectadas (1 si se actualizó, 0 si el id no existe)
     * @throws SQLException si falla la operación
     */
    public int update(Checkpoint checkpoint) throws SQLException {
        String sql = "UPDATE checkpoints SET nombre = ?, hora = ?, latitud = ?, longitud = ?, "
                + "descripcion = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            bindCheckpointValues(pstmt, checkpoint);
            pstmt.setLong(6, checkpoint.getId());
            return pstmt.executeUpdate();
        }
    }

    /**
     * Elimina un checkpoint por su id.
     *
     * @param id identificador del checkpoint a eliminar
     * @return {@code true} si se eliminó alguna fila
     * @throws SQLException si falla la operación
     */
    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM checkpoints WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private static void bindCheckpointValues(PreparedStatement pstmt, Checkpoint checkpoint) throws SQLException {
        pstmt.setString(1, checkpoint.getNombre());
        pstmt.setString(2, checkpoint.getHora());
        pstmt.setDouble(3, checkpoint.getLatitud());
        pstmt.setDouble(4, checkpoint.getLongitud());
        pstmt.setString(5, checkpoint.getDescripcion());
    }

    private static Checkpoint mapRow(ResultSet rs) throws SQLException {
        return new Checkpoint(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("hora"),
                rs.getDouble("latitud"),
                rs.getDouble("longitud"),
                rs.getString("descripcion"));
    }
}
