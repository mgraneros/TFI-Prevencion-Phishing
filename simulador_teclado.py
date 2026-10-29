import time
import threading
from motor_dlp import escanear_texto, validar_luhn
from base_datos import inicializar_db, registrar_alerta

class TecladoSimulado:
    def __init__(self):
        self.buffer_texto = ""
        self.timer = None
        self.tiempo_espera = 0.3

    def procesar_buffer(self):
        print(f"\n[Motor DLP Background] Analizando buffer: '{self.buffer_texto}'")
        hallazgos = escanear_texto(self.buffer_texto)
        
        tarjetas_reales = [t for t in hallazgos["tarjetas_potenciales"] if validar_luhn(t)]
        
        hay_datos_sensibles = tarjetas_reales or hallazgos["cbu_detectados"] or hallazgos["dni_detectados"]
        hay_phishing = len(hallazgos.get("palabras_sospechosas", [])) > 0
        contexto = hallazgos.get("palabras_sospechosas", [])

        if hay_datos_sensibles:
            print(f"🚨 ¡ALERTA DE SEGURIDAD! Bloqueando envío.")
            
            if hallazgos["dni_detectados"]:
                print(f"   -> DNI detectado: {hallazgos['dni_detectados']}")
                nivel_riesgo = "RIESGO EXTREMO" if hay_phishing else "RIESGO MEDIO"
                registrar_alerta("DNI", hallazgos['dni_detectados'][0], nivel_riesgo, contexto)
                
            if hallazgos["cbu_detectados"]:
                print(f"   -> CBU detectado: {hallazgos['cbu_detectados']}")
                nivel_riesgo = "RIESGO EXTREMO" if hay_phishing else "RIESGO ALTO"
                registrar_alerta("CBU", hallazgos['cbu_detectados'][0], nivel_riesgo, contexto)
            
            if tarjetas_reales: 
                print(f"   -> Tarjeta válida detectada: {tarjetas_reales}")
                nivel_riesgo = "RIESGO EXTREMO" if hay_phishing else "RIESGO ALTO"
                if hallazgos["vencimientos_detectados"] or hallazgos["cvv_potenciales"]:
                    print(f"   -> ⚠️ RIESGO CRÍTICO: Combo de tarjeta completo detectado!")
                    nivel_riesgo = "RIESGO EXTREMO" if hay_phishing else "RIESGO CRÍTICO"
                
                registrar_alerta("TARJETA", tarjetas_reales[0], nivel_riesgo, contexto)
            
            if hay_phishing:
                print(f"   -> 🛑 RIESGO EXTREMO (INGENIERÍA SOCIAL): Posible manipulación telefónica.")
                print(f"      - Palabras clave detectadas: {contexto}")

    def on_key_pressed(self, caracter: str):
        self.buffer_texto += caracter
        print(caracter, end='', flush=True)
        
        if self.timer is not None:
            self.timer.cancel()
            
        self.timer = threading.Timer(self.tiempo_espera, self.procesar_buffer)
        self.timer.start()

    def on_clipboard_paste(self, texto_pegado: str):
        print(f"\n[Acción de Usuario: Pegar] -> '{texto_pegado}'")
        self.buffer_texto += texto_pegado
        self.procesar_buffer()

if __name__ == "__main__":
    inicializar_db() # Garantiza que la tabla exista antes de simular
    teclado = TecladoSimulado()
    
    print("--- SIMULACIÓN FINAL: MOTOR DLP + BASE DE DATOS ---")
    time.sleep(1) 
    
    texto_fraude = "Hola, me llamó el asesor por el bloqueo. Te paso urgente mi CBU 0140000000000000000000"
    teclado.on_clipboard_paste(texto_fraude)