"""Pruebas del modelo (casos del informe "Proyecto de Aula - Modelo de Programación Lineal").

Ejecutar:  pip install -r requirements-dev.txt   y luego   pytest
"""
from fastapi.testclient import TestClient

from main import app

client = TestClient(app)
URL = "/api/v1/optimizar-operacion"


def resolver(total=300, admins=4, capacidad=100, objetivo="B"):
    return client.post(URL, json={"total_solicitudes": total, "admins_disponibles": admins,
                                  "capacidad_por_admin": capacidad, "objetivo": objetivo})


def test_alternativa_b_optimo_en_300_y_3():
    r = resolver().json()
    assert (r["solicitudesAtendidas"], r["adminsAsignados"], r["adminsLiberados"]) == (300, 3, 1)
    assert r["z"] == 297 and r["rendimientoNeto"] == 297
    assert r["optimosAlternativos"] is False
    assert r["status"] == "Optimal"


def test_vertices_coinciden_con_el_informe():
    v = resolver().json()["vertices"]
    assert [p["etiqueta"] for p in v] == ["O", "A", "B", "C"]
    assert [(p["x"], p["y"]) for p in v] == [(0, 0), (0, 4), (300, 4), (300, 3)]
    assert [p["zB"] for p in v] == [0, -4, 296, 297]
    assert [p["optimo"] for p in v] == [False, False, False, True]


def test_alternativa_a_tiene_optimos_alternativos_y_elige_menos_admins():
    r = resolver(objetivo="A").json()
    assert r["z"] == 300 and r["adminsAsignados"] == 3
    assert r["optimosAlternativos"] is True


def test_demanda_no_multiplo_redondea_admins_hacia_arriba():
    r = resolver(total=250).json()
    assert (r["solicitudesAtendidas"], r["adminsAsignados"]) == (250, 3)
    assert r["z"] == 247


def test_empate_entre_admins_prefiere_menos_personal():
    # 301 solicitudes: 3 admins -> Z=297 y 4 admins -> Z=297 (empate)
    r = resolver(total=301).json()
    assert r["adminsAsignados"] == 3 and r["solicitudesAtendidas"] == 300


def test_capacidad_insuficiente_usa_todos_los_admins():
    r = resolver(capacidad=50).json()
    assert (r["adminsAsignados"], r["solicitudesAtendidas"], r["adminsLiberados"]) == (4, 200, 0)
    assert "quedarían sin responder" in r["recomendacion"]


def test_sin_demanda_no_se_asigna_a_nadie():
    r = resolver(total=0).json()
    assert (r["adminsAsignados"], r["solicitudesAtendidas"]) == (0, 0)


def test_sin_admins_disponibles():
    r = resolver(admins=0).json()
    assert (r["adminsAsignados"], r["solicitudesAtendidas"]) == (0, 0)
    assert "No hay administradores disponibles" in r["recomendacion"]


def test_valores_grandes_no_se_truncan():
    r = resolver(total=1_000_000, admins=10_000, capacidad=100).json()
    assert (r["solicitudesAtendidas"], r["adminsAsignados"]) == (1_000_000, 10_000)


def test_parametros_invalidos_devuelven_422_con_mensaje_en_espanol():
    r = resolver(capacidad=0)
    assert r.status_code == 422
    assert r.text == "La capacidad por admin debe estar entre 1 y 100000."
    assert resolver(objetivo="C").status_code == 422
    assert resolver(total=-1).status_code == 422


def test_health():
    assert client.get("/health").json() == {"status": "ok"}
