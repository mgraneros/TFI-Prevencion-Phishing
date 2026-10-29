import re

# --- 1. EXPRESIONES REGULARES Y DICCIONARIOS ---
REGEX_CBU = re.compile(r'\b\d{22}\b')
REGEX_DNI = re.compile(r'\b[1-9]\d{6,7}\b')
REGEX_TARJETA = re.compile(r'\b\d{13,19}\b')
REGEX_VENCIMIENTO = re.compile(r'\b(0[1-9]|1[0-2])[-/]([0-9]{2}|[0-9]{4})\b')
REGEX_CVV = re.compile(r'\b\d{3,4}\b')

# NUEVO: Diccionario de palabras de alerta temprana (Phishing)
PALABRAS_CLAVE_PHISHING = [
    "urgente", "bloqueo", "soporte", "transferi", "transferí",
    "validar", "cuenta", "suspension", "suspendida", "asesor", "fraude"
]

# --- 2. LÓGICA MATEMÁTICA (Validación profunda) ---
def validar_luhn(texto: str) -> bool:
    digitos = [int(c) for c in texto if c.isdigit()]
    if not (13 <= len(digitos) <= 19):
        return False
    digitos.reverse()
    suma_total = 0
    for i, digito in enumerate(digitos):
        if i % 2 == 1:
            digito *= 2
            if digito > 9:
                digito -= 9
        suma_total += digito
    return suma_total % 10 == 0

# --- 3. MOTOR DLP (Orquestador) ---
def escanear_texto(texto: str) -> dict:
    """Escanea el buffer de texto en busca de datos sensibles y contexto de fraude."""
    
    # Normalizamos el texto a minúsculas para buscar las palabras clave sin problemas de mayúsculas
    texto_lower = texto.lower()
    palabras_detectadas = [palabra for palabra in PALABRAS_CLAVE_PHISHING if palabra in texto_lower]
    
    resultados = {
        "cbu_detectados": REGEX_CBU.findall(texto),
        "dni_detectados": REGEX_DNI.findall(texto),
        "tarjetas_potenciales": REGEX_TARJETA.findall(texto),
        "vencimientos_detectados": [f"{m[0]}/{m[1]}" for m in REGEX_VENCIMIENTO.findall(texto)],
        "cvv_potenciales": REGEX_CVV.findall(texto),
        "palabras_sospechosas": palabras_detectadas
    }
    return resultados