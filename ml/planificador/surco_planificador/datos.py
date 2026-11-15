"""El conjunto limpio en disco (data/planificador/limpio/): un arreglo por tipo, todos los puntos alineados."""

from dataclasses import dataclass, fields
from datetime import date, timedelta
from pathlib import Path

import numpy as np
import numpy.typing as npt

F32 = npt.NDArray[np.float32]


@dataclass(frozen=True)
class DatosLimpios:
    ids: npt.NDArray[np.str_]  # [P]
    departamentos: npt.NDArray[np.str_]  # [P]
    lat: F32  # [P]
    lon: F32  # [P]
    elevacion: F32  # [P]
    dia0: date
    clima: F32  # [P, D, len(CANALES_HISTORIA)], 0 donde no es válido
    valido: npt.NDArray[np.bool_]  # [P, D]
    sitio: F32  # [P, len(SITIO)]
    perfil_suelo: F32  # [P, capas, propiedades]
    oni_meses: npt.NDArray[np.int32]  # año * 12 + mes - 1
    oni_valores: F32
    anios_clim: npt.NDArray[np.int32]  # [Y] años de etiqueta
    clim: F32  # [P, Y, 366, len(SALIDAS)]

    @property
    def dias(self) -> int:
        return int(self.clima.shape[1])

    def dia(self, indice: int) -> date:
        return self.dia0 + timedelta(days=indice)

    def indice(self, fecha: date) -> int:
        return (fecha - self.dia0).days

    def mapa_oni(self) -> dict[tuple[int, int], float]:
        return {(int(m) // 12, int(m) % 12 + 1): float(v)
                for m, v in zip(self.oni_meses, self.oni_valores, strict=True)}  # fmt: skip

    def guardar(self, carpeta: Path) -> None:
        carpeta.mkdir(parents=True, exist_ok=True)
        arreglos = {f.name: getattr(self, f.name) for f in fields(self) if f.name not in ("dia0", "clim")}
        np.savez(carpeta / "datos.npz", dia0=np.array(self.dia0.toordinal()), **arreglos)
        np.save(carpeta / "climatologia.npy", self.clim)

    @classmethod
    def cargar(cls, carpeta: Path) -> "DatosLimpios":
        with np.load(carpeta / "datos.npz") as d:
            valores = {k: d[k] for k in d.files if k != "dia0"}
            dia0 = date.fromordinal(int(d["dia0"]))
        return cls(dia0=dia0, clim=np.load(carpeta / "climatologia.npy"), **valores)
