from typing import List
from fastapi import FastAPI
from pydantic import BaseModel, Field
import pulp

app = FastAPI(title="MediStock - API Optimización por Farmacia")


class FarmaciaInput(BaseModel):
    farmacia_id: int
    farmacia_nombre: str
    total_solicitudes: int = Field(..., ge=0)
    admins_disponibles: int = Field(..., ge=0)


class OptimizacionRequest(BaseModel):
    modo: str = "BD"  # "BD" o "SIMULACION"
    capacidad_por_admin: int = Field(default=100, ge=1)
    objetivo: str = Field(default="B")  # "A" -> Z = X | "B" -> Z = X - Y
    farmacias: List[FarmaciaInput] = Field(default_factory=list)


@app.post("/api/v1/optimizar-operacion")
def optimizar_por_farmacia(req: OptimizacionRequest):
    objetivo_norm = (req.objetivo or "B").strip().upper()
    if objetivo_norm not in ("A", "B"):
        objetivo_norm = "B"

    problem = pulp.LpProblem("Optimizacion_MultiFarmacia_MediStock", pulp.LpMaximize)

    x_vars = {}
    y_vars = {}

    # 1. Variables y restricciones por cada farmacia
    for f in req.farmacias:
        fid = f.farmacia_id
        x_vars[fid] = pulp.LpVariable(
            f"X_farm_{fid}", lowBound=0, upBound=f.total_solicitudes, cat="Continuous"
        )
        y_vars[fid] = pulp.LpVariable(
            f"Y_farm_{fid}", lowBound=0, upBound=f.admins_disponibles, cat="Integer"
        )

        # Restricciones propias de cada farmacia
        problem += (
            x_vars[fid] <= req.capacidad_por_admin * y_vars[fid],
            f"Capacidad_Farm_{fid}",
        )
        problem += x_vars[fid] <= f.total_solicitudes, f"Demanda_Farm_{fid}"
        problem += y_vars[fid] <= f.admins_disponibles, f"Personal_Farm_{fid}"

    # 2. Función Objetivo según selección ("A": Z = X | "B": Z = X - Y)
    if objetivo_norm == "A":
        problem += (
            pulp.lpSum([x_vars[f.farmacia_id] for f in req.farmacias]),
            "Volumen_Total_A",
        )
    else:
        problem += (
            pulp.lpSum([x_vars[f.farmacia_id] - y_vars[f.farmacia_id] for f in req.farmacias]),
            "Rendimiento_Neto_Total_B",
        )

    # 3. Resolver con CBC
    problem.solve(pulp.PULP_CBC_CMD(msg=False))

    resultados_farmacias = []
    total_solicitudes_req = 0
    total_admins_disp = 0
    total_atendidas = 0
    total_admins_asignados = 0
    total_admins_liberados = 0

    for f in req.farmacias:
        fid = f.farmacia_id
        x_opt = int(round(float(pulp.value(x_vars[fid]) or 0.0)))
        y_opt = int(round(float(pulp.value(y_vars[fid]) or 0.0)))
        z_opt = x_opt - y_opt
        liberados = max(0, f.admins_disponibles - y_opt)
        sin_atender = max(0, f.total_solicitudes - x_opt)

        total_solicitudes_req += f.total_solicitudes
        total_admins_disp += f.admins_disponibles
        total_atendidas += x_opt
        total_admins_asignados += y_opt
        total_admins_liberados += liberados

        resultados_farmacias.append(
            {
                "farmaciaId": fid,
                "farmaciaNombre": f.farmacia_nombre,
                "solicitudesTotales": f.total_solicitudes,
                "solicitudesAtendidas": x_opt,
                "solicitudesSinAtender": sin_atender,
                "adminsDisponibles": f.admins_disponibles,
                "adminsAsignados": y_opt,
                "adminsLiberados": liberados,
                "rendimientoNeto": z_opt,
            }
        )

    z_a = float(total_atendidas)
    z_b = float(total_atendidas - total_admins_asignados)
    z_seleccionado = z_a if objetivo_norm == "A" else z_b

    optimos_alternativos = (
        objetivo_norm == "A" and total_admins_liberados > 0 and total_atendidas > 0
    )

    # 4. Vértices de referencia de la región factible global
    cap_max_total = req.capacidad_por_admin * total_admins_disp
    x_cap_max = min(float(total_solicitudes_req), float(cap_max_total))
    admins_necesarios = (
        min(
            total_admins_disp,
            (total_solicitudes_req + req.capacidad_por_admin - 1)
            // req.capacidad_por_admin,
        )
        if req.capacidad_por_admin > 0 and total_solicitudes_req > 0
        else 0
    )

    candidatos_vertices = [
        ("Origen (0, 0)", 0.0, 0.0),
        (f"Vértice A (0, {total_admins_disp})", 0.0, float(total_admins_disp)),
        (
            f"Vértice B ({int(x_cap_max)}, {total_admins_disp})",
            x_cap_max,
            float(total_admins_disp),
        ),
        (
            f"Vértice C ({total_atendidas}, {admins_necesarios})",
            float(total_atendidas),
            float(admins_necesarios),
        ),
    ]

    vertices = []
    for etiqueta, vx, vy in candidatos_vertices:
        es_optimo = (
            int(round(vx)) == total_atendidas
            and int(round(vy)) == total_admins_asignados
        )
        vertices.append(
            {
                "etiqueta": etiqueta,
                "x": round(vx, 2),
                "y": round(vy, 2),
                "zA": round(vx, 2),
                "zB": round(vx - vy, 2),
                "optimo": es_optimo,
            }
        )

    if total_atendidas == 0 or total_admins_asignados == 0:
        recomendacion = (
            "No hay solicitudes activas para atender o no se asignaron administradores."
        )
    elif objetivo_norm == "B":
        recomendacion = (
            f"Con la Opción B (Rendimiento Neto Z = X - Y), se recomienda asignar "
            f"{total_admins_asignados} de {total_admins_disp} administradores para atender "
            f"{total_atendidas} solicitudes y liberar {total_admins_liberados} administrador(es)."
        )
    else:
        recomendacion = (
            f"Con la Opción A (Volumen Z = X), se atienden {total_atendidas} solicitudes "
            f"con {total_admins_asignados} administrador(es) asignado(s); quedan "
            f"{total_admins_liberados} disponible(s)."
        )

    fuente_texto = (
        "Simulación (PuLP + CBC)"
        if (req.modo or "").upper() == "SIMULACION"
        else "Base de Datos (PuLP + CBC)"
    )

    return {
        "status": pulp.LpStatus[problem.status],
        "solicitudesAtendidas": total_atendidas,
        "adminsAsignados": total_admins_asignados,
        "adminsLiberados": total_admins_liberados,
        "rendimientoNeto": round(z_b, 2),
        "z": round(z_seleccionado, 2),
        "recomendacion": recomendacion,
        "totalSolicitudes": total_solicitudes_req,
        "adminsDisponibles": total_admins_disp,
        "capacidadPorAdmin": req.capacidad_por_admin,
        "objetivo": objetivo_norm,
        "optimosAlternativos": optimos_alternativos,
        "vertices": vertices,
        "fuente": fuente_texto,
        "datosPorDefecto": False,
        "detallePorFarmacia": resultados_farmacias,
    }