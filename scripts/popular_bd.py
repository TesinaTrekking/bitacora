import sqlite3
import os

def popular_bd():
    # Ruta de la base de datos según DatabaseManager.java
    db_path = os.path.expanduser('~/.bitacora-checkpoints/checkpoints.db')

    # Crear directorio si por alguna razón no existe
    os.makedirs(os.path.dirname(db_path), exist_ok=True)

    # Esquema por si la BD aún no fue inicializada por la app
    schema = """
    CREATE TABLE IF NOT EXISTS checkpoints (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        nombre TEXT NOT NULL,
        hora TEXT NOT NULL,
        latitud REAL NOT NULL,
        longitud REAL NOT NULL,
        descripcion TEXT
    );
    """


    # Datos de prueba de la provincia de Córdoba
    datos_prueba = [
        ('Puentes Colgantes Copina', '08:15', -31.5727, -64.6983, 'Inicio del trekking de los viejos puentes colgantes.'),
        ('Los Gigantes (Casas Nuevas)', '09:30', -31.4236, -64.7944, 'Punto de registro y campamento base del macizo.'),
        ('Cerro Colorado', '11:00', -30.0961, -63.9288, 'Reserva natural, visita a los aleros con pinturas rupestres.'),
        ('Cerro Uritorco', '12:45', -30.8461, -64.4933, 'Llegada a la cumbre en Capilla del Monte.'),
        ('La Cumbrecita', '13:30', -31.8994, -64.7733, 'Punto de partida, pueblo peatonal, inicio hacia cascada Escondida.'),
        ('Cerro Champaquí', '14:00', -31.9894, -64.9350, 'Cumbre del cerro más alto de Córdoba (2790 msnm).'),
        ('Quebrada del Condorito', '16:20', -31.6219, -64.7672, 'Balcón Norte, mirador principal para avistaje de cóndores.'),
        ('Pueblo Escondido', '17:00', -32.5519, -64.8430, 'Llegada a las ruinas de la antigua mina en Cerro Áspero.')
    ]

    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()

        # Asegurar que la tabla exista
        cursor.execute(schema)

        # Limpiar datos previos si se desea ejecutar múltiples veces (opcional, aquí no limpiamos para sumar o podemos vaciar)
        # cursor.execute('DELETE FROM checkpoints;')

        # Insertar los datos
        cursor.executemany('''
            INSERT INTO checkpoints (nombre, hora, latitud, longitud, descripcion)
            VALUES (?, ?, ?, ?, ?)
        ''', datos_prueba)

        conn.commit()
        print(f"Éxito: Se han insertado {len(datos_prueba)} checkpoints de prueba de Córdoba.")
        print(f"Base de datos actualizada en: {db_path}")

    except sqlite3.Error as e:
        print(f"Error al operar con la base de datos: {e}")
    finally:
        if conn:
            conn.close()

if __name__ == '__main__':
    popular_bd()
