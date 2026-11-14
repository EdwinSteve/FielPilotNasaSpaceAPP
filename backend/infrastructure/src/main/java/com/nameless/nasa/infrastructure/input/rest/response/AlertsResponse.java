package com.nameless.nasa.infrastructure.input.rest.response;

import java.time.OffsetDateTime;
import java.util.List;

public record AlertsResponse(
        double lat, double lon, OffsetDateTime consultado, List<AlertResponse> alertas) {}
