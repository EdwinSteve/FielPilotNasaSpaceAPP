"""Vigila las reglas del monolito modular: el dominio no depende de frameworks
y ningún módulo importa las capas internas de otro."""
import ast
from pathlib import Path

MODULES = Path(__file__).resolve().parents[1] / "app" / "modules"
PROHIBIDO_EN_DOMINIO = ("fastapi", "sqlalchemy", "pydantic", "httpx")


def _imports(path: Path) -> list[str]:
    tree = ast.parse(path.read_text(encoding="utf-8"))
    out: list[str] = []
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            out += [a.name for a in node.names]
        elif isinstance(node, ast.ImportFrom) and node.module:
            out.append(node.module)
    return out


def test_dominio_es_python_puro() -> None:
    for f in MODULES.glob("*/domain/*.py"):
        for imp in _imports(f):
            assert not imp.startswith(PROHIBIDO_EN_DOMINIO), f"{f}: importa {imp}"


def test_modulos_solo_usan_la_api_publica_de_otros() -> None:
    for f in MODULES.glob("*/**/*.py"):
        propio = f.relative_to(MODULES).parts[0]
        for imp in _imports(f):
            partes = imp.split(".")
            if partes[:2] == ["app", "modules"] and len(partes) > 3 and partes[2] != propio:
                assert False, f"{f}: importa {imp}; use app.modules.{partes[2]}"
