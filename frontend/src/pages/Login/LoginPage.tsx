import React, { useState } from "react";
import { useNavigate, useLocation, Navigate } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import type { UserRole } from "../../types/auth";

type FormMode = "login" | "register";

export default function LoginPage() {
  const { isAuthenticated, login, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [mode, setMode] = useState<FormMode>("login");
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Form Fields
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [role, setRole] = useState<UserRole>("USER");
  const [showPassword, setShowPassword] = useState(false);

  // Field validation errors
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  // If already logged in, redirect to home or previous route
  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || "/";
  if (isAuthenticated) {
    return <Navigate to={from} replace />;
  }

  const validate = (): boolean => {
    const errors: Record<string, string> = {};

    if (!email.trim()) {
      errors.email = "Email address is required.";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors.email = "Please enter a valid email address.";
    }

    if (!password) {
      errors.password = "Password is required.";
    } else if (password.length < 8) {
      errors.password = "Password must be at least 8 characters.";
    }

    if (mode === "register") {
      if (!fullName.trim()) {
        errors.fullName = "Full name is required.";
      }
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    if (!validate()) {
      return;
    }

    setLoading(true);

    try {
      if (mode === "login") {
        await login({ email: email.trim(), password });
        navigate(from, { replace: true });
      } else {
        await register({
          email: email.trim(),
          password,
          fullName: fullName.trim(),
          role,
        });
        setSuccessMessage("Account created successfully! You are now signed in.");
        setTimeout(() => {
          navigate(from, { replace: true });
        }, 1200);
      }
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } }; message?: string };
      const message =
        error.response?.data?.message ||
        error.message ||
        (mode === "login"
          ? "Invalid email or password. Please try again."
          : "Registration failed. Please check your information and try again.");
      setErrorMessage(message);
    } finally {
      setLoading(false);
    }
  };

  const switchMode = (newMode: FormMode) => {
    setMode(newMode);
    setErrorMessage(null);
    setSuccessMessage(null);
    setValidationErrors({});
  };

  return (
    <div
      style={{
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        minHeight: "75vh",
        padding: "20px 16px",
      }}
    >
      <div
        style={{
          width: "100%",
          maxWidth: "460px",
          backgroundColor: "#ffffff",
          borderRadius: "16px",
          boxShadow: "0 10px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.05)",
          border: "1px solid #e2e8f0",
          overflow: "hidden",
        }}
      >
        {/* Header Tabs */}
        <div
          style={{
            display: "flex",
            borderBottom: "1px solid #e2e8f0",
            backgroundColor: "#f8fafc",
          }}
        >
          <button
            type="button"
            onClick={() => switchMode("login")}
            style={{
              flex: 1,
              padding: "16px",
              fontSize: "15px",
              fontWeight: 600,
              backgroundColor: mode === "login" ? "#ffffff" : "transparent",
              color: mode === "login" ? "#2563eb" : "#64748b",
              border: "none",
              borderBottom: mode === "login" ? "2px solid #2563eb" : "2px solid transparent",
              cursor: "pointer",
              transition: "all 0.2s ease",
            }}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => switchMode("register")}
            style={{
              flex: 1,
              padding: "16px",
              fontSize: "15px",
              fontWeight: 600,
              backgroundColor: mode === "register" ? "#ffffff" : "transparent",
              color: mode === "register" ? "#2563eb" : "#64748b",
              border: "none",
              borderBottom: mode === "register" ? "2px solid #2563eb" : "2px solid transparent",
              cursor: "pointer",
              transition: "all 0.2s ease",
            }}
          >
            Create Account
          </button>
        </div>

        {/* Form Body */}
        <div style={{ padding: "32px 28px" }}>
          <div style={{ marginBottom: "24px", textAlign: "center" }}>
            <h2
              style={{
                fontSize: "22px",
                fontWeight: 700,
                color: "#1e293b",
                marginBottom: "6px",
              }}
            >
              {mode === "login" ? "Welcome back" : "Join RescueRoute"}
            </h2>
            <p style={{ fontSize: "14px", color: "#64748b" }}>
              {mode === "login"
                ? "Sign in to access your verified healthcare route account"
                : "Register for emergency healthcare routing and services"}
            </p>
          </div>

          {/* Error Message Banner */}
          {errorMessage && (
            <div
              style={{
                marginBottom: "20px",
                padding: "12px 16px",
                backgroundColor: "#fef2f2",
                border: "1px solid #fecaca",
                borderRadius: "8px",
                color: "#991b1b",
                fontSize: "14px",
                display: "flex",
                alignItems: "center",
                gap: "8px",
              }}
            >
              <span>⚠️</span>
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Success Message Banner */}
          {successMessage && (
            <div
              style={{
                marginBottom: "20px",
                padding: "12px 16px",
                backgroundColor: "#f0fdf4",
                border: "1px solid #bbf7d0",
                borderRadius: "8px",
                color: "#166534",
                fontSize: "14px",
                display: "flex",
                alignItems: "center",
                gap: "8px",
              }}
            >
              <span>✓</span>
              <span>{successMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate>
            {/* Registration Full Name */}
            {mode === "register" && (
              <div style={{ marginBottom: "18px" }}>
                <label
                  htmlFor="full-name-input"
                  style={{
                    display: "block",
                    marginBottom: "6px",
                    fontSize: "13px",
                    fontWeight: 600,
                    color: "#334155",
                  }}
                >
                  Full Name *
                </label>
                <input
                  id="full-name-input"
                  type="text"
                  placeholder="e.g. Dr. Jane Doe"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  style={{
                    width: "100%",
                    padding: "10px 14px",
                    borderRadius: "8px",
                    border: `1px solid ${validationErrors.fullName ? "#ef4444" : "#cbd5e1"}`,
                    fontSize: "14px",
                    outline: "none",
                    color: "#0f172a",
                    backgroundColor: "#ffffff",
                    boxSizing: "border-box",
                  }}
                />
                {validationErrors.fullName && (
                  <span style={{ fontSize: "12px", color: "#dc2626", marginTop: "4px", display: "block" }}>
                    {validationErrors.fullName}
                  </span>
                )}
              </div>
            )}

            {/* Email Field */}
            <div style={{ marginBottom: "18px" }}>
              <label
                htmlFor="email-input"
                style={{
                  display: "block",
                  marginBottom: "6px",
                  fontSize: "13px",
                  fontWeight: 600,
                  color: "#334155",
                }}
              >
                Email Address *
              </label>
              <input
                id="email-input"
                type="email"
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                style={{
                  width: "100%",
                  padding: "10px 14px",
                  borderRadius: "8px",
                  border: `1px solid ${validationErrors.email ? "#ef4444" : "#cbd5e1"}`,
                  fontSize: "14px",
                  outline: "none",
                  color: "#0f172a",
                  backgroundColor: "#ffffff",
                  boxSizing: "border-box",
                }}
              />
              {validationErrors.email && (
                <span style={{ fontSize: "12px", color: "#dc2626", marginTop: "4px", display: "block" }}>
                  {validationErrors.email}
                </span>
              )}
            </div>


            {/* Registration Role Selection */}
            {mode === "register" && (
              <div style={{ marginBottom: "18px" }}>
                <label
                  htmlFor="role-select"
                  style={{
                    display: "block",
                    marginBottom: "6px",
                    fontSize: "13px",
                    fontWeight: 600,
                    color: "#334155",
                  }}
                >
                  Account Type *
                </label>
                <select
                  id="role-select"
                  value={role}
                  onChange={(e) => setRole(e.target.value as UserRole)}
                  style={{
                    width: "100%",
                    padding: "10px 14px",
                    borderRadius: "8px",
                    border: "1px solid #cbd5e1",
                    fontSize: "14px",
                    outline: "none",
                    color: "#0f172a",
                    backgroundColor: "#ffffff",
                    boxSizing: "border-box",
                  }}
                >
                  <option value="USER" style={{ color: "#0f172a", backgroundColor: "#ffffff" }}>
                    User (Emergency Seeker)
                  </option>
                  <option value="PROVIDER" style={{ color: "#0f172a", backgroundColor: "#ffffff" }}>
                    Healthcare Provider (Hospital/Facility Admin)
                  </option>
                </select>
                <span style={{ fontSize: "12px", color: "#64748b", marginTop: "4px", display: "block" }}>
                  Healthcare providers can manage facility verified details in future updates.
                </span>
              </div>
            )}

            {/* Password Field */}
            <div style={{ marginBottom: "24px" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "6px" }}>
                <label
                  htmlFor="password-input"
                  style={{
                    fontSize: "13px",
                    fontWeight: 600,
                    color: "#334155",
                  }}
                >
                  Password *
                </label>
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    background: "none",
                    border: "none",
                    fontSize: "12px",
                    color: "#2563eb",
                    cursor: "pointer",
                    padding: 0,
                  }}
                >
                  {showPassword ? "Hide" : "Show"}
                </button>
              </div>
              <input
                id="password-input"
                type={showPassword ? "text" : "password"}
                placeholder="At least 8 characters"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete={mode === "login" ? "current-password" : "new-password"}
                style={{
                  width: "100%",
                  padding: "10px 14px",
                  borderRadius: "8px",
                  border: `1px solid ${validationErrors.password ? "#ef4444" : "#cbd5e1"}`,
                  fontSize: "14px",
                  outline: "none",
                  color: "#0f172a",
                  backgroundColor: "#ffffff",
                  boxSizing: "border-box",
                }}
              />
              {validationErrors.password && (
                <span style={{ fontSize: "12px", color: "#dc2626", marginTop: "4px", display: "block" }}>
                  {validationErrors.password}
                </span>
              )}
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              style={{
                width: "100%",
                padding: "12px",
                borderRadius: "8px",
                border: "none",
                backgroundColor: loading ? "#93c5fd" : "#2563eb",
                color: "#ffffff",
                fontSize: "15px",
                fontWeight: 600,
                cursor: loading ? "not-allowed" : "pointer",
                transition: "background-color 0.2s ease",
              }}
            >
              {loading ? (
                <span>Processing...</span>
              ) : mode === "login" ? (
                "Sign In"
              ) : (
                "Create Account"
              )}
            </button>
          </form>

          {/* Footer note */}
          <div style={{ marginTop: "24px", textAlign: "center" }}>
            <p style={{ fontSize: "13px", color: "#64748b" }}>
              {mode === "login" ? (
                <>
                  Don&apos;t have an account?{" "}
                  <button
                    type="button"
                    onClick={() => switchMode("register")}
                    style={{
                      background: "none",
                      border: "none",
                      color: "#2563eb",
                      fontWeight: 600,
                      cursor: "pointer",
                      padding: 0,
                    }}
                  >
                    Register here
                  </button>
                </>
              ) : (
                <>
                  Already registered?{" "}
                  <button
                    type="button"
                    onClick={() => switchMode("login")}
                    style={{
                      background: "none",
                      border: "none",
                      color: "#2563eb",
                      fontWeight: 600,
                      cursor: "pointer",
                      padding: 0,
                    }}
                  >
                    Sign in here
                  </button>
                </>
              )}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}