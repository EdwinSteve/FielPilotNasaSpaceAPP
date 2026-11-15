package co.surco.fincas.aplicacion;

import co.surco.fincas.dominio.Coordenada;
import co.surco.fincas.dominio.ErroresFincas;
import co.surco.fincas.dominio.Poligono;
import co.surco.fincas.dominio.Puertos.RepositorioTerrenos;
import co.surco.fincas.dominio.Puertos.ValidadorMunicipios;
import co.surco.fincas.dominio.Terreno;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Guarda el terreno que el productor dibujó en el mapa. */
@Service
@Transactional
public class RegistrarTerreno {

	private final RepositorioTerrenos terrenos;
	private final ValidadorMunicipios municipios;
	private final Clock reloj;

	public RegistrarTerreno(RepositorioTerrenos terrenos, ValidadorMunicipios municipios, Clock reloj) {
		this.terrenos = terrenos;
		this.municipios = municipios;
		this.reloj = reloj;
	}

	public record Solicitud(String nombre, String municipio, boolean riego, List<Coordenada> vertices) {
	}

	public Terreno ejecutar(UUID usuarioId, Solicitud s) {
		Poligono poligono = Poligono.de(s.vertices());
		if (!municipios.existe(s.municipio())) {
			throw ErroresFincas.municipioInexistente();
		}
		if (terrenos.contarDeUsuario(usuarioId) >= ErroresFincas.MAXIMO_TERRENOS) {
			throw ErroresFincas.limiteTerrenos();
		}
		Terreno terreno = Terreno.nuevo(usuarioId, s.nombre(), s.municipio(), s.riego(), poligono, reloj.instant());
		terrenos.guardar(terreno);
		return terreno;
	}
}
