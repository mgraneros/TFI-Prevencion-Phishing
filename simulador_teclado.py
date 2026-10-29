import time
import threading
from motor_dlp import escanear_texto, validar_luhn

class TecladoSimulado:
    def __init__(self):
        self.buffer_texto = ""
        self.timer = None
        self.tiempo_espera = 0.3

    def procesar_buffer(self):
        print(f"\n[Motor DLP Background] Analizando buffer: '{self.buffer_texto}'")
        hallazgos = escanear_texto(self.buffer_texto)
        
        tarjetas_reales = [t for t in hallazgos["tarjetas_potenciales"] if validar_luhn(t)]
        
        if tarjetas_reales or hallazgos["cbu_detectados"] or hallazgos["dni_detectados"]:
            print(f"🚨 ¡ALERTA DE SEGURIDAD! Bloqueando envío.")
            if hallazgos["dni_detectados"]: print(f"   -> DNI detectado: {hallazgos['dni_detectados']}")
            if hallazgos["cbu_detectados"]: print(f"   -> CBU detectado: {hallazgos['cbu_detectados']}")
            if tarjetas_reales: print(f"   -> Tarjeta válida detectada: {tarjetas_reales}")

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
    teclado = TecladoSimulado()
    
    print("--- 1. SIMULACIÓN DE TIPEO ---")
    for letra in "Mi DNI es ":
        teclado.on_key_pressed(letra)
        time.sleep(0.1)
        
    time.sleep(0.4) 
    
    for letra in "35123456":
        teclado.on_key_pressed(letra)
        time.sleep(0.1)
        
    time.sleep(0.5) 

    print("\n--- 2. SIMULACIÓN DE PORTAPAPELES ---")
    time.sleep(1) 
    teclado.on_clipboard_paste(" y mi tarjeta es 4242424242424242")