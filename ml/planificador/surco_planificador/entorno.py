"""Etapa `entorno`: comprueba que la GPU sirve antes de horas de trabajo."""

import logging
import platform
import sys

log = logging.getLogger(__name__)


def revisar() -> bool:
    import torch

    log.info("Python %s en %s", sys.version.split()[0], platform.platform())
    log.info("torch %s, CUDA %s", torch.__version__, torch.version.cuda)
    if not torch.cuda.is_available():
        log.error("CUDA no disponible: revise el driver NVIDIA (570 o más nuevo para la serie RTX 50)")
        return False
    mayor, menor = torch.cuda.get_device_capability(0)
    if f"sm_{mayor}{menor}" not in torch.cuda.get_arch_list():
        log.error("esta build de torch no trae kernels para sm_%d%d: instale la de CUDA 12.8 (cu128)", mayor, menor)
        return False
    gb = torch.cuda.get_device_properties(0).total_memory / 1024**3
    log.info("GPU %s, sm_%d%d, %.1f GB, bf16 %s", torch.cuda.get_device_name(0), mayor, menor, gb,
             torch.cuda.is_bf16_supported())  # fmt: skip
    x = torch.randn(1024, 1024, device="cuda", dtype=torch.bfloat16)
    log.info("matmul bf16 OK: %.3f", float((x @ x).float().abs().mean()))
    return True
