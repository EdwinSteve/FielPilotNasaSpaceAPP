"""Dónde vive cada cosa. Los datos pesados van fuera del repositorio versionado (data/planificador/)."""

import os
from dataclasses import dataclass
from pathlib import Path

CARPETA = Path(__file__).resolve().parents[1]  # ml/planificador
RAIZ_REPO = CARPETA.parents[1]


@dataclass(frozen=True)
class Rutas:
    configs: Path
    datos: Path
    catalogo_json: Path
    divipola_json: Path
    modelos: Path

    @classmethod
    def por_defecto(cls) -> "Rutas":
        datos = Path(os.environ.get("SURCO_PLANIFICADOR_DATOS", RAIZ_REPO / "data" / "planificador"))
        return cls(
            configs=CARPETA / "configs",
            datos=datos,
            catalogo_json=RAIZ_REPO / "data" / "catalogo_cultivos" / "datos" / "cultivos.json",
            divipola_json=RAIZ_REPO / "backend/src/main/resources/territorio/divipola.json",
            modelos=CARPETA / "modelos",
        )

    @property
    def crudo(self) -> Path:
        return self.datos / "crudo"

    @property
    def limpio(self) -> Path:
        return self.datos / "limpio"

    @property
    def pronosticos(self) -> Path:
        return self.datos / "pronosticos"

    @property
    def etiquetas(self) -> Path:
        return self.datos / "etiquetas"

    @property
    def corridas(self) -> Path:
        return self.datos / "corridas"

    @property
    def registros(self) -> Path:
        return self.datos / "registros"

    @property
    def puntos_csv(self) -> Path:
        return self.configs / "puntos.csv"
