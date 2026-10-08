type SearchBarProps = {
  value: string;
  onChange: (value: string) => void;
};

export default function SearchBar({
  value,
  onChange,
}: SearchBarProps) {
  return (
    <input
      type="text"
      placeholder="Search hospitals, pharmacies..."
      value={value}
      onChange={(e) => onChange(e.target.value)}
      style={{
        width: "100%",
        padding: "12px 16px",
        fontSize: "15px",
        borderRadius: "8px",
        border: "1px solid #cbd5e1",
        backgroundColor: "#ffffff",
        color: "#0f172a",
        marginBottom: "16px",
        outline: "none",
        boxSizing: "border-box",
        boxShadow: "0 1px 3px rgba(0,0,0,0.04)",
      }}
    />
  );
}