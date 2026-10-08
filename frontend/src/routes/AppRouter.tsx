import { BrowserRouter, Routes, Route } from "react-router-dom";

import MainLayout from "../components/layout/MainLayout";
import { AuthProvider } from "../context/AuthContext";

import HomePage from "../pages/Home/HomePage";
import HospitalsPage from "../pages/Hospitals/HospitalsPage";
import BloodBanksPage from "../pages/BloodBanks/BloodBanksPage";
import PharmaciesPage from "../pages/Pharmacies/PharmaciesPage";
import AmbulancesPage from "../pages/Ambulances/AmbulancesPage";
import LoginPage from "../pages/Login/LoginPage";
import ProviderFacilityPage from "../pages/Provider/ProviderFacilityPage";
import AdminVerificationPage from "../pages/Admin/AdminVerificationPage";
import NotFoundPage from "../pages/NotFound/NotFoundPage";
import { ProtectedRoute } from "../components/common/ProtectedRoute";

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
                            path="/login"
                            element={<LoginPage />}
                        />

                        {/* Protected Provider Route */}
                        <Route
                            path="/provider/create-facility"
                            element={
                                <ProtectedRoute allowedRoles={["PROVIDER"]}>
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