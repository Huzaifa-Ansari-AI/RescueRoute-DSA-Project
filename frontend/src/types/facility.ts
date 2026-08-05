export interface Facility {
  id: number;
  name: string;
  type: "Hospital" | "Blood Bank" | "Pharmacy" | "Ambulance";
  latitude: number;
  longitude: number;
  address: string;
}