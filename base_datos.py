import sqlite3
import datetime

# Conexión a la base de datos local
DB_NAME = "dlp_logs.db"

def inicializar_db():
    """Crea la tabla de auditoría si no existe."""
    conexion = sqlite3.connect(DB_NAME)
    cursor = conexion.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS logs_seguridad (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            fecha_hora TEXT,
            tipo_dato TEXT,
            dato_ofuscado TEXT,
            nivel_riesgo TEXT,
            contexto_phishing TEXT
        )
    ''')
    conexion.commit()
    conexion.close()

def ofuscar_dato(dato: str, tipo: str) -> str:
    """Enmascara el dato sensible dejando solo los últimos dígitos para referencia."""
    if len(dato) <= 4:
        return "***"
    
    if tipo == "TARJETA":
        return f"****-****-****-{dato[-4:]}"
    elif tipo == "CBU":
        return f"{'*' * 18}{dato[-4:]}"
    elif tipo == "DNI":
        return f"{'*' * (len(dato)-3)}{dato[-3:]}"
    return "***"

def registrar_alerta(tipo_dato: str, dato_crudo: str, nivel_riesgo: str, contexto: list = None):
    """Ofusca y guarda el evento de seguridad en la base de datos."""
    dato_seguro = ofuscar_dato(dato_crudo, tipo_dato)
    fecha_actual = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    palabras_contexto = ",".join(contexto) if contexto else "Ninguno"
    
    conexion = sqlite3.connect(DB_NAME)
    cursor = conexion.cursor()
    cursor.execute('''
        INSERT INTO logs_seguridad (fecha_hora, tipo_dato, dato_ofuscado, nivel_riesgo, contexto_phishing)
        VALUES (?, ?, ?, ?, ?)
    ''', (fecha_actual, tipo_dato, dato_seguro, nivel_riesgo, palabras_contexto))
    conexion.commit()
    conexion.close()
    
    print(f"[DB LOG] Registrado {tipo_dato} ({nivel_riesgo}) ofuscado como: {dato_seguro}")

# Prueba rápida e inicialización aislada
if __name__ == "__main__":
    inicializar_db()
    print("Base de datos inicializada correctamente.")
    
    # Simulamos el guardado de un par de alertas
    registrar_alerta("TARJETA", "4242424242424242", "RIESGO CRÍTICO", ["urgente", "asesor"])
    registrar_alerta("CBU", "0140000000000000000000", "RIESGO ALTO")