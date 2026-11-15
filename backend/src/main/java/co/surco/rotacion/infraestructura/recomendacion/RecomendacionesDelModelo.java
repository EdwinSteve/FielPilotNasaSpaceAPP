package co.surco.rotacion.infraestructura.recomendacion;

import co.surco.recomendacion.ConsultaRecomendaciones;
import co.surco.recomendacion.ConsultaRecomendaciones.CultivoEvaluado;
import co.surco.recomendacion.ConsultaRecomendaciones.RecomendacionTerreno;
import co.surco.rotacion.dominio.Candidatos;
import co.surco.rotacion.dominio.Puertos.Recomendaciones;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Pide la recomendación al módulo recomendacion (por su API pública) y la traduce a candidatos. */
@Component
class RecomendacionesDelModelo implements Recomendaciones {

	private final ConsultaRecomendaciones recomendacion;

	RecomendacionesDelModelo(ConsultaRecomendaciones recomendacion) {
		this.recomendacion = recomendacion;
	}

	@Override
	public Candidatos calcular(UUID usuarioId, UUID terrenoId) {
		return candidatos(recomendacion.calcular(usuarioId, terrenoId));
	}

	static Candidatos candidatos(RecomendacionTerreno r) {
		var h = r.historico();
		var l = r.lluvias();
		return new Candidatos(r.id(), r.nombreTerreno(), r.areaHa(), r.riego(), r.motor(),
			r.cultivos().stream().map(RecomendacionesDelModelo::cultivo).toList(),
			new Candidatos.Historico(h.fuente(), h.fechas(), h.probExito(), h.riegoMmP50()),
			r.cultivos().stream().map(c -> new Candidatos.CultivoRanking(c.id(), c.nombre(), c.puntaje(), c.apto(),
				c.razonNoApto())).toList(),
			new Candidatos.Lluvias(l.fuente(), l.meses().stream()
				.map(m -> new Candidatos.LluviaMes(m.mes(), m.lluviaMm(), m.nivel(), m.nota())).toList()));
	}

	static Candidatos.Cultivo cultivo(CultivoEvaluado c) {
		return new Candidatos.Cultivo(c.id(), c.nombre(), c.familia(), c.grupo(), c.fijaNitrogeno(), c.fijacionNitrogenoKgHa(),
			c.cicloDias(), c.siembra(), c.cosecha(), c.puntaje(), c.probExito(), c.riegoMmP50(), c.apto());
	}
}
