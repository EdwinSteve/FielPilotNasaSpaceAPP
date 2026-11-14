package com.nameless.nasa.infrastructure.input.rest.controller;

import com.nameless.nasa.domain.model.DroughtCalculator;
import com.nameless.nasa.domain.model.enums.AlertMessages;
import com.nameless.nasa.domain.model.enums.DroughtMessages;
import com.nameless.nasa.infrastructure.input.rest.response.AlertResponse;
import com.nameless.nasa.infrastructure.input.rest.response.AlertsResponse;
import com.nameless.nasa.infrastructure.output.client.IdeamAlertsClient;
import com.nameless.nasa.infrastructure.output.client.NasaPowerClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/alerts")
public class AlertsController {
    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(AlertsController.class);

    private final IdeamAlertsClient ideam;
    private final NasaPowerClient power;

    AlertsController(IdeamAlertsClient ideam, NasaPowerClient power) {
        this.ideam = ideam;
        this.power = power;
    }

    @GetMapping("/flood")
    public AlertsResponse vigentes(@RequestParam double lat, @RequestParam double lon) {
        if (lat < -4.5 || lat > 13.5 || lon < -82 || lon > -66) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La ubicación está fuera de Colombia.");
        }

        Optional<IdeamAlertsClient.Subzona> conAlerta;
        boolean enColombia = true;
        try {
            conAlerta = ideam.subzonaEn(lat, lon);
            if (conAlerta.isEmpty()) {
                enColombia = ideam.estaEnColombia(lat, lon);
            }
        } catch (RestClientException | IllegalStateException e) {
            log.error("Fallo consultando al IDEAM", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No pudimos consultar al IDEAM. Intenta de nuevo en unos minutos.", e);
        }

        if (!enColombia) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La ubicación está fuera de Colombia.");
        }

        OffsetDateTime ahora = OffsetDateTime.now(COLOMBIA);
        if (conAlerta.isEmpty()) {
            // La capa de alertas solo trae subzonas con alerta vigente: no aparecer = sin alerta.
            AlertResponse sinAlerta = new AlertResponse("HIDROLOGICA", AlertMessages.SIN_ALERTA.name(), null,
                    AlertMessages.SIN_ALERTA.message(), "IDEAM");
            return new AlertsResponse(lat, lon, ahora, List.of(sinAlerta));
        }

        IdeamAlertsClient.Subzona subzona = conAlerta.get();
        AlertMessages nivel;
        try {
            nivel = AlertMessages.desdeIdeam(subzona.nivel());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "El IDEAM envió un nivel de alerta que no reconocemos.", e);
        }

        AlertResponse alerta = new AlertResponse(
                "HIDROLOGICA", nivel.name(), subzona.nombre(), nivel.message(), "IDEAM");
        return new AlertsResponse(lat, lon, ahora, List.of(alerta));
    }

    /** Indicador de sequía calculado con lluvia de NASA POWER. No es una alerta oficial. */
    @GetMapping("/drought")
    public AlertsResponse sequia(@RequestParam double lat, @RequestParam double lon) {
        if (lat < -4.5 || lat > 13.5 || lon < -82 || lon > -66) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La ubicación está fuera de Colombia.");
        }

        DroughtCalculator.Resultado resultado;
        try {
            NasaPowerClient.Lluvia lluvia = power.lluviaEn(lat, lon);
            resultado = DroughtCalculator.calcular(lluvia.diaria(), lluvia.normalPorMes());
        } catch (RestClientException | IllegalStateException e) {
            log.error("Fallo consultando a NASA POWER", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No pudimos consultar los datos de lluvia de la NASA. Intenta de nuevo en unos minutos.", e);
        }

        DroughtMessages nivel = DroughtMessages.desdePorcentaje(resultado.porcentaje());
        AlertResponse alerta = new AlertResponse(
                "SEQUIA", nivel.name(), "Tu ubicación", nivel.message(), "NASA POWER");
        return new AlertsResponse(lat, lon, OffsetDateTime.now(COLOMBIA), List.of(alerta));
    }
}
