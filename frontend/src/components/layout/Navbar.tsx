import React from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const getRoleBadgeStyle = (role?: string) => {
    switch (role) {
      case "ADMIN":
        return { bg: "#f3e8ff", text: "#7e22ce", border: "#d8b4fe" };
      case "PROVIDER":
        return { bg: "#fffbeb", text: "#b45309", border: "#fde68a" };
      case "USER":
      default:
        return { bg: "#eff6ff", text: "#1d4ed8", border: "#bfdbfe" };
    }
  };

  const navLinks = [
    { label: "Find Facilities", path: "/" },
    { label: "Hospitals", path: "/hospitals" },
    { label: "Pharmacies", path: "/pharmacies" },
    { label: "Blood Banks", path: "/blood-banks" },
    { label: "Ambulances", path: "/ambulances" },
  ];

  return (
    <header
      style={{
        backgroundColor: "#ffffff",
        borderBottom: "1px solid #e2e8f0",
        boxShadow: "0 1px 4px rgba(0, 0, 0, 0.04)",
        position: "sticky",
        top: 0,
        zIndex: 1000,
      }}
    >
      <div
        style={{
          maxWidth: "1400px",
          margin: "0 auto",
          padding: "12px 24px",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          flexWrap: "wrap",
          gap: "16px",
        }}
      >
        {/* Brand Logo */}
        <Link
          to="/"
          style={{
            display: "flex",
            alignItems: "center",
            gap: "8px",
            textDecoration: "none",
            color: "#0f172a",
          }}
        >
          <span style={{ fontSize: "24px" }}>🚑</span>
          <span style={{ fontSize: "20px", fontWeight: 700, letterSpacing: "-0.5px" }}>
            RescueRoute
          </span>
        </Link>

        {/* Navigation Categories */}
        <nav style={{ display: "flex", alignItems: "center", gap: "16px", flexWrap: "wrap" }}>
          {navLinks.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                style={{
                  fontSize: "14px",
                  fontWeight: isActive ? 600 : 500,
                  color: isActive ? "#2563eb" : "#475569",
                  textDecoration: "none",
                  padding: "4px 8px",
                  borderRadius: "6px",
                  backgroundColor: isActive ? "#eff6ff" : "transparent",
                  transition: "all 0.15s ease",
                }}
              >
                {item.label}
              </Link>
            );
          })}

          {isAuthenticated && user?.role === "PROVIDER" && (
            <Link
              to="/provider/create-facility"
              style={{
                fontSize: "13px",
                fontWeight: location.pathname === "/provider/create-facility" ? 700 : 600,
                color: "#15803d",
                textDecoration: "none",
                padding: "4px 10px",
                borderRadius: "6px",
                backgroundColor: location.pathname === "/provider/create-facility" ? "#dcfce7" : "#f0fdf4",
                border: "1px solid #bbf7d0",
              }}
            >
              + Add Facility
            </Link>
          )}

          {isAuthenticated && user?.role === "ADMIN" && (
            <Link
              to="/admin/verifications"
              style={{
                fontSize: "13px",
                fontWeight: location.pathname === "/admin/verifications" ? 700 : 600,
                color: "#7e22ce",
                textDecoration: "none",
                padding: "4px 10px",
                borderRadius: "6px",
                backgroundColor: location.pathname === "/admin/verifications" ? "#f3e8ff" : "#faf5ff",
                border: "1px solid #e9d5ff",
              }}
            >
              🛡️ Moderation Queue
            </Link>
          )}
        </nav>

        {/* Auth Actions / User Profile */}
        <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
          {isAuthenticated && user ? (
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <div style={{ textAlign: "right" }}>
                <div style={{ fontSize: "13px", fontWeight: 600, color: "#0f172a" }}>
                  {user.fullName}
                </div>
                <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "2px" }}>
                  <span
                    style={{
                      fontSize: "10px",
                      fontWeight: 700,
                      padding: "1px 6px",
                      borderRadius: "4px",
                      backgroundColor: getRoleBadgeStyle(user.role).bg,
                      color: getRoleBadgeStyle(user.role).text,
                      border: `1px solid ${getRoleBadgeStyle(user.role).border}`,
                    }}
                  >
                    {user.role}
                  </span>
                </div>
              </div>

              <button
                type="button"
                onClick={handleLogout}
                style={{
                  padding: "6px 12px",
                  backgroundColor: "#f8fafc",
                  color: "#dc2626",
                  border: "1px solid #fecaca",
                  borderRadius: "6px",
                  fontSize: "12px",
                  fontWeight: 600,
                  cursor: "pointer",
                  transition: "background-color 0.15s",
                }}
                onMouseOver={(e) => {
                  e.currentTarget.style.backgroundColor = "#fef2f2";
                }}
                onMouseOut={(e) => {
                  e.currentTarget.style.backgroundColor = "#f8fafc";
                }}
              >
                Sign Out
              </button>
            </div>
          ) : (
            <Link
              to="/login"
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "6px",
                padding: "8px 16px",
                backgroundColor: "#2563eb",
                color: "#ffffff",
                borderRadius: "8px",
                fontSize: "13px",
                fontWeight: 600,
                textDecoration: "none",
                boxShadow: "0 2px 6px rgba(37, 99, 235, 0.2)",
                transition: "background-color 0.15s",
              }}
              onMouseOver={(e) => {
                e.currentTarget.style.backgroundColor = "#1d4ed8";
              }}
              onMouseOut={(e) => {
                e.currentTarget.style.backgroundColor = "#2563eb";
              }}
            >
              <span>👤</span>
              <span>Sign In</span>
            </Link>
          )}
        </div>
      </div>
    </header>
  );
};

export default Navbar;
