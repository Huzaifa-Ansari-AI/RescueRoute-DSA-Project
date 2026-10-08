import React from "react";
import { Navigate, useLocation, Outlet } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import Loading from "../ui/Loading";
import type { UserRole } from "../../types/auth";

interface ProtectedRouteProps {
  allowedRoles?: UserRole[];
  children?: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  allowedRoles,
  children,
}) => {
  const { isAuthenticated, isLoading, user } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <Loading message="Verifying authentication session..." size="medium" />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return (
      <div
        style={{
          maxWidth: "600px",
          margin: "60px auto",
          padding: "32px",
          backgroundColor: "#ffffff",
          borderRadius: "12px",
          border: "1px solid #fee2e2",
          boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.05)",
          textAlign: "center",
        }}
      >
        <div style={{ fontSize: "48px", marginBottom: "16px" }}>🚫</div>
        <h2 style={{ color: "#991b1b", marginBottom: "8px", fontSize: "22px" }}>
          Access Forbidden
        </h2>
        <p style={{ color: "#64748b", fontSize: "15px", lineHeight: "1.6", marginBottom: "24px" }}>
          Your current account role (<strong>{user.role}</strong>) does not have permission to access this resource.
        </p>
        <a
          href="/"
          style={{
            display: "inline-block",
            padding: "10px 20px",
            backgroundColor: "#2563eb",
            color: "#ffffff",
            borderRadius: "8px",
            textDecoration: "none",
            fontWeight: 600,
            fontSize: "14px",
          }}
        >
          Return to Dashboard
        </a>
      </div>
    );
  }

  return children ? <>{children}</> : <Outlet />;
};

export default ProtectedRoute;
