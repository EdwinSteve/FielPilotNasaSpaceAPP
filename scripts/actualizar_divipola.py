"""Descarga la DIVIPOLA del DANE (todos los departamentos y municipios de Colombia) y genera el JSON
que empaqueta el módulo territorio de la API (backend/src/main/resources/territorio/divipola.json).

    python3 scripts/actualizar_divipola.py                    # descarga de datos.gov.co
    python3 scripts/actualizar_divipola.py --desde mpios.json # usa una descarga previa

Fuente: datos abiertos del DANE en datos.gov.co, conjunto gdxc-w37w (DIVIPOLA, municipios).
Incluye Bogotá D.C., las áreas no municipalizadas y las islas.
"""
import argparse
import json
import unicodedata
import urllib.request
from datetime import date
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
DESTINOS = [RAIZ / "backend/src/main/resources/territorio/divipola.json"]
URL = "https://www.datos.gov.co/resource/gdxc-w37w.json?$limit=5000"
FUENTE = "DANE, DIVIPOLA (datos.gov.co: gdxc-w37w)"
MINUSCULAS = {"de", "del", "la", "las", "los", "el", "y", "e"}
# Nombres que la regla general no escribe bien
ESPECIALES = {"BOGOTÁ, D.C.": "Bogotá, D.C."}


def nombre_propio(texto: str) -> str:
    """'SAN JOSÉ DE CÚCUTA' -> 'San José de Cúcuta'; 'MIRITÍ - PARANÁ' -> 'Mirití - Paraná'."""
    texto = texto.strip()
    if texto in ESPECIALES:
        return ESPECIALES[texto]
    palabras = texto.lower().split()
    return " ".join(p if i and p in MINUSCULAS else p[:1].upper() + p[1:] for i, p in enumerate(palabras))


def orden(texto: str) -> str:
    """Clave de orden alfabético sin tildes: 'Ábrego' va antes de 'Abriaquí' como en un directorio."""
    return unicodedata.normalize("NFKD", texto).encode("ascii", "ignore").decode().lower()


def coordenada(texto: str | None) -> float | None:
    """El DANE usa coma decimal y a veces separador de miles ('-75,581,775')."""
    if not texto:
        return None
    entero, _, decimales = texto.partition(",")
    return round(float(f"{entero}.{decimales.replace(',', '')}"), 6)


def construir(filas: list[dict]) -> dict:
    departamentos: dict[str, dict] = {}
    for f in filas:
        dep = departamentos.setdefault(
            f["cod_dpto"], {"codigo": f["cod_dpto"], "nombre": nombre_propio(f["dpto"]), "municipios": []}
        )
        dep["municipios"].append(
            {
                "codigo": f["cod_mpio"],
                "nombre": nombre_propio(f["nom_mpio"]),
                "tipo": f.get("tipo_municipio", "Municipio"),
                "lat": coordenada(f.get("latitud")),
                "lon": coordenada(f.get("longitud")),
            }
        )
    for dep in departamentos.values():
        dep["municipios"].sort(key=lambda m: orden(m["nombre"]))
    return {
        "fuente": FUENTE,
        "descargado": date.today().isoformat(),
        "departamentos": sorted(departamentos.values(), key=lambda d: orden(d["nombre"])),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--desde", type=Path, help="JSON ya descargado de gdxc-w37w, en lugar de descargarlo")
    args = parser.parse_args()

    if args.desde:
        filas = json.loads(args.desde.read_text(encoding="utf-8"))
    else:
        with urllib.request.urlopen(URL, timeout=120) as respuesta:
            filas = json.load(respuesta)

    datos = construir(filas)
    total = sum(len(d["municipios"]) for d in datos["departamentos"])
    if len(datos["departamentos"]) < 33 or total < 1100:
        raise SystemExit(f"Descarga incompleta: {len(datos['departamentos'])} departamentos, {total} municipios")

    texto = json.dumps(datos, ensure_ascii=False, separators=(",", ":")) + "\n"
    for destino in DESTINOS:
        destino.parent.mkdir(parents=True, exist_ok=True)
        destino.write_text(texto, encoding="utf-8")
        print("Guardado en", destino.relative_to(RAIZ))
    print(f"{len(datos['departamentos'])} departamentos, {total} municipios")


if __name__ == "__main__":
    main()
