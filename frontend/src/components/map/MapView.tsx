import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
} from "react-leaflet";

import L from "leaflet";

import type { Facility } from "../../types/facility";

type Props = {
  facilities: Facility[];
};

const center: [number, number] = [24.8607, 67.0011];

const createIcon = (icon: string) =>
  new L.Icon({
    iconUrl: icon,
    iconSize: [35, 35],
    iconAnchor: [17, 35],
    popupAnchor: [0, -35],
  });

const icons = {
  Hospital: createIcon("/icons/hospital.png"),
  "Blood Bank": createIcon("/icons/blood-bank.png"),
  Pharmacy: createIcon("/icons/pharmacy.png"),
  Ambulance: createIcon("/icons/ambulance.png"),
};

export default function MapView({ facilities }: Props) {
  return (
    <MapContainer
      center={center}
      zoom={13}
      style={{ height: "600px", width: "100%" }}
    >
      <TileLayer
        attribution="&copy; OpenStreetMap contributors"
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {facilities.map((facility) => (
        <Marker
          key={facility.id}
          position={[facility.latitude, facility.longitude]}
          icon={icons[facility.type]}
        >
          <Popup>
            <h3>{facility.name}</h3>

            <strong>{facility.type}</strong>

            <br />

            {facility.address}
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}