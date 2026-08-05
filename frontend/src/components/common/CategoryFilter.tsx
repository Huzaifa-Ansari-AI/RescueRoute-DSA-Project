type Props = {
  value: string;
  onChange: (value: string) => void;
};

export default function CategoryFilter({
  value,
  onChange,
}: Props) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      style={{
        width: "100%",
        padding: "12px",
        marginBottom: "20px",
        borderRadius: "8px",
        fontSize: "16px",
      }}
    >
      <option value="All">All Facilities</option>
      <option value="Hospital">Hospital</option>
      <option value="Blood Bank">Blood Bank</option>
      <option value="Pharmacy">Pharmacy</option>
      <option value="Ambulance">Ambulance</option>
    </select>
  );
}