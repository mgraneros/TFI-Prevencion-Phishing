import re

# --- 1. EXPRESIONES REGULARES (Filtro previo rápido) ---
REGEX_CBU = re.compile(r'\b\d{22}\b')
REGEX_DNI = re.compile(r'\b[1-9]\d{6,7}\b')
REGEX_TARJETA = re.compile(r'\b\d{13,19}\b')

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
    """Escanea el buffer de texto en busca de datos sensibles."""
    resultados = {
        "cbu_detectados": REGEX_CBU.findall(texto),
        "dni_detectados": REGEX_DNI.findall(texto),
        "tarjetas_potenciales": REGEX_TARJETA.findall(texto)
    }
    return resultados