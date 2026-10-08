interface ErrorMessageProps {
  title?: string;
  message: string;
  onRetry?: () => void;
}

export default function ErrorMessage({
  title = "Failed to load facilities",
  message,
  onRetry,
}: ErrorMessageProps) {
  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        padding: "32px 24px",
        margin: "20px 0",
        borderRadius: "12px",
        backgroundColor: "#fef2f2",
        border: "1px solid #fecaca",
        textAlign: "center",
        width: "100%",
        boxSizing: "border-box",
      }}
      role="alert"
    >
      <div
        style={{
          width: "48px",
          height: "48px",
          borderRadius: "50%",
          backgroundColor: "#fee2e2",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          marginBottom: "12px",
          fontSize: "24px",
        }}
      >
        ⚠️
      </div>

      <h3
        style={{
          margin: "0 0 8px 0",
          color: "#991b1b",
          fontSize: "18px",
          fontWeight: 600,
        }}
      >
        {title}
      </h3>

      <p
        style={{
          margin: "0 0 16px 0",
          color: "#b91c1c",
          fontSize: "14px",
          maxWidth: "500px",
          lineHeight: 1.5,
        }}
      >
        {message}
      </p>

      {onRetry && (
        <button
          onClick={onRetry}
          style={{
            padding: "10px 20px",
            backgroundColor: "#dc2626",
            color: "#ffffff",
            border: "none",
            borderRadius: "8px",
            fontSize: "14px",
            fontWeight: 500,
            cursor: "pointer",
            transition: "background-color 0.2s ease",
          }}
          onMouseOver={(e) => (e.currentTarget.style.backgroundColor = "#b91c1c")}
          onMouseOut={(e) => (e.currentTarget.style.backgroundColor = "#dc2626")}
        >
          🔄 Retry Request
        </button>
      )}
    </div>
  );
}
