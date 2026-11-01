import { lazy } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import MainLayout from "../components/layout/MainLayout";
import { AuthProvider } from "../context/AuthContext";
import { ProtectedRoute } from "../components/common/ProtectedRoute";

// Code-split route components for optimal LCP and lazy loading
const HomePage = lazy(() => import("../pages/Home/HomePage"));
const HospitalsPage = lazy(() => import("../pages/Hospitals/HospitalsPage"));
const ClinicsPage = lazy(() => import("../pages/Clinics/ClinicsPage"));
const BloodBanksPage = lazy(() => import("../pages/BloodBanks/BloodBanksPage"));
const PharmaciesPage = lazy(() => import("../pages/Pharmacies/PharmaciesPage"));
const AmbulancesPage = lazy(() => import("../pages/Ambulances/AmbulancesPage"));
const LoginPage = lazy(() => import("../pages/Login/LoginPage"));
const ProviderFacilityPage = lazy(() => import("../pages/Provider/ProviderFacilityPage"));
const AdminVerificationPage = lazy(() => import("../pages/Admin/AdminVerificationPage"));
const AdminFacilityManagementPage = lazy(() => import("../pages/Admin/AdminFacilityManagementPage"));
const NotFoundPage = lazy(() => import("../pages/NotFound/NotFoundPage"));

export default function AppRouter() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Routes>
                    <Route element={<MainLayout />}>
                        <Route path="/" element={<HomePage />} />

                        <Route
                            path="/hospitals"
                            element={<HospitalsPage />}
                        />

                        <Route
                            path="/clinics"
                            element={<ClinicsPage />}
                        />

                        <Route
                            path="/blood-banks"
                            element={<BloodBanksPage />}
                        />

                        <Route
                            path="/pharmacies"
                            element={<PharmaciesPage />}
                        />

                        <Route
                            path="/ambulances"
                            element={<AmbulancesPage />}
                        />

                        <Route
                            path="/external-discovery"
                            element={<Navigate to="/" replace />}
                        />

                        <Route
                            path="/login"
                            element={<LoginPage />}
                        />

                        {/* Protected Provider Route */}
                        <Route
                            path="/provider/create-facility"
                            element={
                                <ProtectedRoute allowedRoles={["PROVIDER", "ADMIN"]}>
                                    <ProviderFacilityPage />
                                </ProtectedRoute>
                            }
                        />

                        {/* Protected Admin Verification Dashboard */}
                        <Route
                            path="/admin/verifications"
                            element={
                                <ProtectedRoute allowedRoles={["ADMIN"]}>
                                    <AdminVerificationPage />
                                </ProtectedRoute>
                            }
                        />

                        {/* Protected Admin Facility Management */}
                        <Route
                            path="/admin/facilities"
                            element={
                                <ProtectedRoute allowedRoles={["ADMIN"]}>
                                    <AdminFacilityManagementPage />
                                </ProtectedRoute>
                            }
                        />

                        <Route
                            path="*"
                            element={<NotFoundPage />}
                        />
                    </Route>
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}