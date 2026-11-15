"""python -m surco_planificador [etapas...] [--corrida nombre] [--forzar etapa ...]"""

import argparse
import logging
import sys
from datetime import datetime

from surco_planificador.rutas import Rutas
from surco_planificador.tuberia import ETAPAS, TODO, Tuberia


def main() -> None:
    ap = argparse.ArgumentParser(prog="surco_planificador", description=__doc__)
    ap.add_argument("etapas", nargs="*", default=["todo"], help=f"todo, o algunas de: {', '.join(ETAPAS)}")
    ap.add_argument("--corrida", default="principal")
    ap.add_argument("--forzar", nargs="*", default=[], help="etapas que se rehacen aunque exista su salida")
    args = ap.parse_args()
    etapas = TODO if args.etapas == ["todo"] else tuple(args.etapas)
    desconocidas = [e for e in etapas if e not in ETAPAS]
    if desconocidas:
        ap.error(f"etapas desconocidas: {desconocidas}")
    rutas = Rutas.por_defecto()
    rutas.registros.mkdir(parents=True, exist_ok=True)
    archivo = rutas.registros / f"{datetime.now().astimezone():%Y%m%d-%H%M%S}.log"
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s",
                        handlers=[logging.StreamHandler(sys.stdout), logging.FileHandler(archivo, encoding="utf-8")])  # fmt: skip
    Tuberia(rutas, args.corrida, frozenset(args.forzar)).correr(etapas)


if __name__ == "__main__":
    main()
