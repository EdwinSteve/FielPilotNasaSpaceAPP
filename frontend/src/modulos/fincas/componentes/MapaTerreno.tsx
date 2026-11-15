"use client";

// Mapa de Leaflet para segmentar el terreno: el productor toca cada esquina sobre la foto satelital y
// el contorno se dibuja en vivo. Solo corre en el navegador (Leaflet usa window): impórtelo con
// next/dynamic y ssr: false, como hace FormularioTerreno.
import "leaflet/dist/leaflet.css";
import { useEffect } from "react";
import { CircleMarker, LayersControl, MapContainer, Polygon, Polyline, TileLayer, useMap, useMapEvents } from "react-leaflet";
import type { Vertice } from "../tipos";

/** Centro de Colombia: lo que se ve antes de elegir municipio o ubicarse. */
export const CENTRO_COLOMBIA: Vertice = { lat: 4.6, lon: -74.1 };
const VERDE = "#2f7d32";
const ROJO = "#c62828";

export interface Enfoque {
  centro: Vertice;
  zoom: number;
  /** Cambia el número para volver a enfocar el mismo punto. */
  clave: number;
}

interface PropsEditor {
  vertices: Vertice[];
  alCambiar?: (vertices: Vertice[]) => void;
  /** Otros terrenos del productor, de referencia (no editables). */
  otros?: { id: string; vertices: Vertice[] }[];
  enfoque?: Enfoque;
  /** El contorno tiene un problema (lados cruzados...): se pinta en rojo. */
  invalido?: boolean;
  alto?: string;
}

function Clics({ alCambiar, vertices }: { alCambiar?: (v: Vertice[]) => void; vertices: Vertice[] }) {
  useMapEvents({
    click(e) {
      alCambiar?.([...vertices, { lat: e.latlng.lat, lon: e.latlng.lng }]);
    },
  });
  return null;
}

function Enfocar({ enfoque }: { enfoque?: Enfoque }) {
  const mapa = useMap();
  useEffect(() => {
    if (enfoque) mapa.flyTo([enfoque.centro.lat, enfoque.centro.lon], enfoque.zoom, { duration: 1.2 });
  }, [mapa, enfoque]);
  return null;
}

/** Al abrir un terreno guardado, encuadra su contorno. */
function Encuadrar({ vertices, activo }: { vertices: Vertice[]; activo: boolean }) {
  const mapa = useMap();
  useEffect(() => {
    if (activo && vertices.length >= 3) {
      mapa.fitBounds(vertices.map((v) => [v.lat, v.lon] as [number, number]), { padding: [24, 24], maxZoom: 18 });
    }
    // Solo al montar: después el productor mueve el mapa a su gusto.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mapa, activo]);
  return null;
}

export function MapaTerreno({ vertices, alCambiar, otros = [], enfoque, invalido = false, alto = "h-[420px]" }: PropsEditor) {
  const editable = Boolean(alCambiar);
  const color = invalido ? ROJO : VERDE;
  const puntos = vertices.map((v) => [v.lat, v.lon] as [number, number]);
  return (
    <div className={`relative w-full overflow-hidden rounded-2xl border border-borde ${alto}`}>
      <MapContainer
        center={[CENTRO_COLOMBIA.lat, CENTRO_COLOMBIA.lon]}
        zoom={6}
        className={`h-full w-full ${editable ? "cursor-crosshair" : ""}`}
        scrollWheelZoom
        doubleClickZoom={!editable}
      >
        <LayersControl position="topright">
          <LayersControl.BaseLayer checked name="Satélite">
            <TileLayer
              attribution="Imágenes &copy; Esri, Maxar, Earthstar Geographics"
              url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
              maxZoom={19}
            />
          </LayersControl.BaseLayer>
          <LayersControl.BaseLayer name="Calles">
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              maxZoom={19}
            />
          </LayersControl.BaseLayer>
          <LayersControl.Overlay checked name="Nombres de lugares">
            <TileLayer
              url="https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}"
              maxZoom={19}
            />
          </LayersControl.Overlay>
        </LayersControl>

        {otros.map((o) => (
          <Polygon
            key={o.id}
            positions={o.vertices.map((v) => [v.lat, v.lon] as [number, number])}
            pathOptions={{ color: "#ffffff", weight: 2, dashArray: "6 4", fillOpacity: 0.12 }}
          />
        ))}

        {puntos.length >= 3 && <Polygon positions={puntos} pathOptions={{ color, weight: 3, fillOpacity: 0.25 }} />}
        {puntos.length === 2 && <Polyline positions={puntos} pathOptions={{ color, weight: 3 }} />}
        {editable &&
          puntos.map((p, i) => (
            <CircleMarker
              key={`${i}-${p[0]}-${p[1]}`}
              center={p}
              radius={i === 0 ? 8 : 6}
              pathOptions={{ color: "#ffffff", weight: 2, fillColor: i === 0 ? "#f9a825" : color, fillOpacity: 1 }}
            />
          ))}

        {editable && <Clics alCambiar={alCambiar} vertices={vertices} />}
        <Enfocar enfoque={enfoque} />
        <Encuadrar vertices={vertices} activo={!editable} />
      </MapContainer>
    </div>
  );
}
