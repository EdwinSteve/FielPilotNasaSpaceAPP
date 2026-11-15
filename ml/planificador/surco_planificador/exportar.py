"""Etapa `exportar`: el bundle que corre en CUALQUIER computador, sin tarjeta gráfica.

- model.onnx: el conjunto de redes en una sola gráfica (opset 18), probabilidades ya aplicadas.
- Paridad: ONNX Runtime y PyTorch deben coincidir en 1e-4 sobre muestras de prueba.
- INT8: se cuantizan solo MatMul/Gemm (las Conv1d cuantizadas fallan o se vuelven más lentas en
  ONNX Runtime CPU). Se guarda model_int8.onnx SOLO si es >= 1,3× más rápido en esta CPU y no
  mueve el rendimiento p50 más de 0,01; si no, el fp32 ya corre en milisegundos.
"""

import hashlib
import json
import logging
import time
from collections.abc import Sequence
from pathlib import Path
from typing import Any

import numpy as np
import onnxruntime as ort
import torch

from surco_planificador.caracteristicas import (
    CANALES_ANOMALIA,
    CANALES_CLIM,
    CANALES_HISTORIA,
    ENTRADAS,
    ESTATICO,
    PRIOR,
    SALIDA,
    SALIDAS_RED,
    Normalizacion,
)
from surco_planificador.catalogo import CARACTERISTICAS_CULTIVO, FichaCultivo, vector_ficha

log = logging.getLogger(__name__)
OPSET = 18
TOLERANCIA = 1e-4


def exportar_onnx(modelo: torch.nn.Module, muestra: tuple[torch.Tensor, ...], ruta: Path) -> None:
    """Exportador torch.export (dynamo) con ejes dinámicos; si falla, el de TorchScript (obsoleto)."""
    modelo = modelo.float().cpu().eval()
    x = tuple(t.float().cpu() for t in muestra)
    lote = torch.export.Dim("lote", min=1, max=8192)
    miembros = torch.export.Dim("miembros", min=2, max=128)
    cultivos = torch.export.Dim("cultivos", min=2, max=512)
    try:
        torch.onnx.export(
            modelo, x, str(ruta), input_names=list(ENTRADAS), output_names=[SALIDA], opset_version=OPSET,
            dynamic_shapes=({0: lote}, {0: lote, 1: miembros}, {0: lote}, {0: lote}, {0: cultivos},
                            {0: lote, 1: cultivos}),
            dynamo=True,
        )  # fmt: skip
        return
    except Exception as e:  # versiones de torch sin el exportador nuevo estable
        log.warning("exportador dynamo falló (%s); se usa el de TorchScript", e)
    torch.onnx.export(
        modelo, x, str(ruta), input_names=list(ENTRADAS), output_names=[SALIDA],
        dynamic_axes={"historia": {0: "lote"}, "pronostico": {0: "lote", 1: "miembros"},
                      "clim": {0: "lote"}, "estatico": {0: "lote"}, "cultivos": {0: "cultivos"},
                      "prior": {0: "lote", 1: "cultivos"}, SALIDA: {0: "lote", 1: "cultivos"}},
        opset_version=OPSET, dynamo=False,
    )  # fmt: skip


def _sesion(ruta: Path) -> ort.InferenceSession:
    opciones = ort.SessionOptions()
    opciones.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
    return ort.InferenceSession(str(ruta), opciones, providers=["CPUExecutionProvider"])


def correr(ruta: Path, entradas: Sequence[np.ndarray]) -> np.ndarray:
    return _sesion(ruta).run(None, dict(zip(ENTRADAS, entradas, strict=True)))[0]


def paridad(modelo: torch.nn.Module, muestra: tuple[torch.Tensor, ...], ruta: Path) -> float:
    modelo = modelo.float().cpu().eval()
    x = tuple(t.float().cpu() for t in muestra)
    with torch.no_grad():
        esperado = modelo(*x).numpy()
    return float(np.abs(esperado - correr(ruta, [t.numpy() for t in x])).max())


def latencia_ms(ruta: Path, entradas: Sequence[np.ndarray], repeticiones: int = 30) -> float:
    sesion = _sesion(ruta)
    feed = dict(zip(ENTRADAS, entradas, strict=True))
    sesion.run(None, feed)
    t0 = time.perf_counter()
    for _ in range(repeticiones):
        sesion.run(None, feed)
    return (time.perf_counter() - t0) / repeticiones * 1000


def probar_int8(ruta: Path, entradas_lote1: Sequence[np.ndarray], entradas_ref: Sequence[np.ndarray]
                ) -> dict[str, Any]:
    from onnxruntime.quantization import QuantType, quantize_dynamic

    ruta8 = ruta.with_name("model_int8.onnx")
    quantize_dynamic(str(ruta), str(ruta8), op_types_to_quantize=["MatMul", "Gemm"], weight_type=QuantType.QInt8)
    fp32, int8 = latencia_ms(ruta, entradas_lote1), latencia_ms(ruta8, entradas_lote1)
    dif = float(np.abs(correr(ruta, entradas_ref)[..., 1] - correr(ruta8, entradas_ref)[..., 1]).max())
    aceptado = fp32 / int8 >= 1.3 and dif <= 0.01
    if not aceptado:
        ruta8.unlink()
    return {"latencia_fp32_ms": round(fp32, 2), "latencia_int8_ms": round(int8, 2),
            "dif_max_rend_p50": round(dif, 5), "int8_aceptado": aceptado}  # fmt: skip


def _sha(ruta: Path) -> str:
    return hashlib.sha256(ruta.read_bytes()).hexdigest()


def escribir_bundle(carpeta: Path, conjunto: torch.nn.Module, muestra: tuple[torch.Tensor, ...],
                    norm: Normalizacion, fichas: Sequence[FichaCultivo], fuera: dict[str, str],
                    metricas: dict[str, Any], correccion: Path, datos: dict[str, str]) -> dict[str, Any]:
    carpeta.mkdir(parents=True, exist_ok=True)
    ruta = carpeta / "model.onnx"
    exportar_onnx(conjunto, muestra, ruta)
    dif = paridad(conjunto, muestra, ruta)
    if dif > TOLERANCIA:
        raise RuntimeError(f"falló la paridad ONNX: diferencia máxima {dif:.2e}")
    x = [t.float().cpu().numpy() for t in muestra]
    lote1 = [a if nombre == "cultivos" else a[:1] for nombre, a in zip(ENTRADAS, x, strict=True)]
    cpu = probar_int8(ruta, lote1, x)
    norm.guardar(carpeta / "normalizacion.json")
    (carpeta / "cultivos.json").write_text(json.dumps({
        "caracteristicas": list(CARACTERISTICAS_CULTIVO),
        "cultivos": [{"id": f.id, "nombre": f.nombre, "ciclo_dias": f.ciclo_dias, "ky": f.ky,
                      "ky_metodo": f.ky_metodo, "balance_hidrico": f.balance_hidrico,
                      "vector": vector_ficha(f).tolist()} for f in fichas],
        "fuera_del_modelo": fuera,
    }, indent=1, ensure_ascii=False), encoding="utf-8", newline="\n")  # fmt: skip
    (carpeta / "correccion.npz").write_bytes(correccion.read_bytes())
    (carpeta / "metricas.json").write_text(json.dumps({**metricas, "cpu": cpu}, indent=1), encoding="utf-8")
    (carpeta / "MODELO.md").write_text(ficha_modelo(metricas, fichas, dif, cpu), encoding="utf-8", newline="\n")
    manifiesto = {
        "archivos": {p.name: _sha(p) for p in sorted(carpeta.iterdir()) if p.name != "manifiesto.json"},
        "entradas": {"historia": list(CANALES_HISTORIA), "pronostico": list(CANALES_ANOMALIA),
                     "clim": list(CANALES_CLIM), "estatico": list(ESTATICO), "prior": list(PRIOR),
                     "cultivos": [f.id for f in fichas]},
        "salidas": list(SALIDAS_RED), "datos": datos, "paridad_onnx": dif,
    }  # fmt: skip
    (carpeta / "manifiesto.json").write_text(json.dumps(manifiesto, indent=1), encoding="utf-8")
    return manifiesto


def _tabla(filas: dict[str, dict[str, float]]) -> str:
    claves = list(next(iter(filas.values())))
    out = ["| Modelo | " + " | ".join(claves) + " |", "|---" * (len(claves) + 1) + "|"]
    return "\n".join(out + [f"| {n} | " + " | ".join(str(v[k]) for k in claves) + " |" for n, v in filas.items()])


def ficha_modelo(m: dict[str, Any], fichas: Sequence[FichaCultivo], dif: float, cpu: dict[str, Any]) -> str:
    veredicto = ("**Adoptado:** le gana a B0 y a Bf en pinball y NDCG@3." if m.get("adoptado")
                 else "**No adoptado:** no le gana a B0 y a Bf; el backend sirve Bf.")  # fmt: skip
    return f"""# PlanificadorEstacionalNet

{veredicto}

- **Tarea:** rendimiento relativo en secano y con riego (p10/p50/p90), P(rendimiento >= 0,6),
  riego necesario y días de estrés térmico para {len(fichas)} cultivos del catálogo surco_agro.
- **Entradas:** 365 días de NASA POWER antes de sembrar, el pronóstico ECMWF SEAS5 corregido
  (conjunto de miembros, 175 días), la climatología sin fuga, el suelo (SoilGrids) y el ENSO.
- **Etiquetas:** oráculo FAO-56 / FAO-33 / trapecios del catálogo sobre el clima que ocurrió.
- **No está calibrado contra cosechas reales.** Las fichas del catálogo no las ha validado un agrónomo.
- **Paridad ONNX:** {dif:.2e}. **CPU:** {cpu["latencia_fp32_ms"]} ms por lote (fp32);
  INT8 {"aceptado" if cpu["int8_aceptado"] else "descartado"} ({cpu["latencia_int8_ms"]} ms).

## Prueba ({m.get("anios_prueba", "")}, solo pronósticos operativos)

{_tabla(m["prueba"]) if m.get("prueba") else "Sin correr."}

## Prueba espacial (Huila nunca visto)

{_tabla(m["reserva"]) if m.get("reserva") else "Sin correr."}
"""
