import React from "react";
import { Link } from "react-router-dom";

export const Footer: React.FC = () => {
  return (
    <footer
      style={{
        backgroundColor: "#0f172a",
        color: "#94a3b8",
        borderTop: "1px solid #1e293b",
        marginTop: "auto",
        fontSize: "13px",
      }}
    >
      <div
        style={{
          maxWidth: "1400px",
          margin: "0 auto",
          padding: "40px 24px 24px 24px",
        }}
      >
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
            gap: "32px",
            marginBottom: "32px",
          }}
        >
          {/* Column 1: Branding & Emergency Mission */}
          <div>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "8px",
                color: "#ffffff",
                fontSize: "18px",
                fontWeight: 700,
                marginBottom: "12px",
              }}
            >
              <span style={{ fontSize: "22px" }}>🚑</span>
              <span>RescueRoute</span>
            </div>
            <p style={{ lineHeight: 1.6, color: "#94a3b8", margin: "0 0 16px 0" }}>
              Production-grade emergency healthcare facility discovery, multi-criteria Top-K
              ranking, and OSRM road navigation for critical medical access.
            </p>
            <div
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "6px",
                padding: "6px 10px",
                backgroundColor: "#1e293b",
                border: "1px solid #334155",
                borderRadius: "6px",
                fontSize: "11px",
                color: "#e2e8f0",
              }}
            >
              <span>⚡</span>
              <span>O(N log K) Bounded Min-Heap Engine</span>
            </div>
          </div>

          {/* Column 2: Facility Categories */}
          <div>
            <h4
              style={{
                color: "#ffffff",
                fontSize: "14px",
                fontWeight: 600,
                marginBottom: "14px",
                letterSpacing: "0.2px",
              }}
            >
              Facility Services
            </h4>
            <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "flex", flexDirection: "column", gap: "8px" }}>
              <li>
                <Link to="/hospitals" style={{ color: "#94a3b8", textDecoration: "none", transition: "color 0.15s" }}>
                  🏥 Emergency Hospitals
                </Link>
              </li>
              <li>
                <Link to="/clinics" style={{ color: "#94a3b8", textDecoration: "none", transition: "color 0.15s" }}>
                  🩺 Outpatient & Specialized Clinics
                </Link>
              </li>
              <li>
                <Link to="/pharmacies" style={{ color: "#94a3b8", textDecoration: "none", transition: "color 0.15s" }}>
                  💊 24/7 Pharmacies
                </Link>
              </li>
              <li>
                <Link to="/blood-banks" style={{ color: "#94a3b8", textDecoration: "none", transition: "color 0.15s" }}>
                  🩸 Blood Banks & Transfusion Centers
                </Link>
              </li>
              <li>
                <Link to="/ambulances" style={{ color: "#94a3b8", textDecoration: "none", transition: "color 0.15s" }}>
                  🚑 Ambulance Stations
                </Link>
              </li>
            </ul>
          </div>

          {/* Column 3: Portals & Access */}
          <div>
            <h4
              style={{
                color: "#ffffff",
                fontSize: "14px",
                fontWeight: 600,
                marginBottom: "14px",
                letterSpacing: "0.2px",
              }}
            >
              Portals & Management
            </h4>
            <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "flex", flexDirection: "column", gap: "8px" }}>
              <li>
                <Link to="/login" style={{ color: "#94a3b8", textDecoration: "none" }}>
                  🔐 Portal Sign In
                </Link>
              </li>
              <li>
                <Link to="/register" style={{ color: "#94a3b8", textDecoration: "none" }}>
                  📝 Provider Registration
                </Link>
              </li>
              <li>
                <Link to="/provider/create-facility" style={{ color: "#94a3b8", textDecoration: "none" }}>
                  ➕ Submit Facility Listing
                </Link>
              </li>
              <li>
                <Link to="/admin/verifications" style={{ color: "#94a3b8", textDecoration: "none" }}>
                  🛡️ Admin Moderation Queue
                </Link>
              </li>
              <li>
                <Link to="/admin/facilities" style={{ color: "#94a3b8", textDecoration: "none" }}>
                  📋 Admin Facility Registry
                </Link>
              </li>
            </ul>
          </div>

          {/* Column 4: Emergency Notice & Attribution */}
          <div>
            <h4
              style={{
                color: "#ffffff",
                fontSize: "14px",
                fontWeight: 600,
                marginBottom: "14px",
                letterSpacing: "0.2px",
              }}
            >
              Emergency Disclaimer
            </h4>
            <div
              style={{
                padding: "12px",
                backgroundColor: "rgba(239, 68, 68, 0.1)",
                border: "1px solid rgba(239, 68, 68, 0.3)",
                borderRadius: "8px",
                color: "#fca5a5",
                fontSize: "12px",
                lineHeight: 1.5,
                marginBottom: "12px",
              }}
            >
              <strong>Immediate Danger?</strong> Dial <strong>1122</strong> or your local emergency hotline. RescueRoute is a discovery and routing tool and does not dispatch emergency response teams.
            </div>
            <p style={{ fontSize: "11px", color: "#64748b", margin: 0, lineHeight: 1.5 }}>
              Map data © <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noreferrer" style={{ color: "#94a3b8" }}>OpenStreetMap</a> contributors. Routing via <a href="http://project-osrm.org/" target="_blank" rel="noreferrer" style={{ color: "#94a3b8" }}>OSRM</a>.
            </p>
          </div>
        </div>

        {/* Bottom Bar: Copyright & System Status */}
        <div
          style={{
            paddingTop: "20px",
            borderTop: "1px solid #1e293b",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            flexWrap: "wrap",
            gap: "12px",
            fontSize: "12px",
            color: "#64748b",
          }}
        >
          <div>
            © {new Date().getFullYear()} RescueRoute. All rights reserved.
          </div>
          <div style={{ display: "flex", gap: "16px", alignItems: "center" }}>
            <span style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}>
              <span
                style={{
                  width: "8px",
                  height: "8px",
                  borderRadius: "50%",
                  backgroundColor: "#22c55e",
                  display: "inline-block",
                }}
              />
              System Operational
            </span>
            <span>•</span>
            <span>OSRM Road Engine Active</span>
            <span>•</span>
            <span>Neon PostgreSQL Connected</span>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
