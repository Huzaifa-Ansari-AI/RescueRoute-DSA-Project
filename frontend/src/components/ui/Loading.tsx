interface LoadingProps {
  message?: string;
  size?: "small" | "medium" | "large";
}

export default function Loading({
  message = "Loading emergency facilities...",
  size = "medium",
}: LoadingProps) {
  const spinnerSize = size === "small" ? "24px" : size === "large" ? "48px" : "36px";

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        padding: "40px 20px",
        minHeight: "200px",
        width: "100%",
        textAlign: "center",
      }}
      role="status"
      aria-live="polite"
    >
      <div
        style={{
          width: spinnerSize,
          height: spinnerSize,
          border: "4px solid #e2e8f0",
          borderTopColor: "#ef4444",
          borderRadius: "50%",
          animation: "spin 0.8s linear infinite",
        }}
      />
      <p
        style={{
          marginTop: "16px",
          color: "#475569",
          fontSize: "15px",
          fontWeight: 500,
        }}
      >
        {message}
      </p>

      <style>{`
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
}
