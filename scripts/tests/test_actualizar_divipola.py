from scripts.actualizar_divipola import construir, coordenada, nombre_propio


def test_nombre_propio_respeta_conectores_y_tildes():
    assert nombre_propio("SAN JOSÉ DE CÚCUTA") == "San José de Cúcuta"
    assert nombre_propio("MIRITÍ - PARANÁ") == "Mirití - Paraná"
    assert nombre_propio("EL CARMEN DE VIBORAL") == "El Carmen de Viboral"


def test_nombre_propio_casos_especiales():
    assert nombre_propio("BOGOTÁ, D.C.") == "Bogotá, D.C."


def test_coordenada_con_coma_decimal_y_separador_de_miles():
    assert coordenada("6,246631") == 6.246631
    assert coordenada("-75,581,775") == -75.581775
    assert coordenada(None) is None


def test_construir_agrupa_y_ordena_sin_tildes():
    filas = [
        {"cod_dpto": "41", "dpto": "HUILA", "cod_mpio": "41001", "nom_mpio": "NEIVA", "latitud": "2,9", "longitud": "-75,2"},
        {"cod_dpto": "05", "dpto": "ANTIOQUIA", "cod_mpio": "05002", "nom_mpio": "ÁBREGO", "latitud": "1", "longitud": "1"},
        {"cod_dpto": "05", "dpto": "ANTIOQUIA", "cod_mpio": "05001", "nom_mpio": "MEDELLÍN", "latitud": "6", "longitud": "-75"},
    ]
    datos = construir(filas)
    assert [d["codigo"] for d in datos["departamentos"]] == ["05", "41"]
    assert [m["nombre"] for m in datos["departamentos"][0]["municipios"]] == ["Ábrego", "Medellín"]
    assert datos["departamentos"][1]["municipios"][0]["tipo"] == "Municipio"
