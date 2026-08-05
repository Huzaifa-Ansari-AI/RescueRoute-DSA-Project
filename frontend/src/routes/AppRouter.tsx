import { BrowserRouter, Routes, Route } from "react-router-dom";

import MainLayout from "../components/layout/MainLayout";

import HomePage from "../pages/Home/HomePage";
import HospitalsPage from "../pages/Hospitals/HospitalsPage";
import BloodBanksPage from "../pages/BloodBanks/BloodBanksPage";
import PharmaciesPage from "../pages/Pharmacies/PharmaciesPage";
import AmbulancesPage from "../pages/Ambulances/AmbulancesPage";
import LoginPage from "../pages/Login/LoginPage";
import NotFoundPage from "../pages/NotFound/NotFoundPage";

export default function AppRouter() {
    return (
        <BrowserRouter>
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

                    <Route
                        path="*"
                        element={<NotFoundPage />}
                    />
                </Route>
            </Routes>
        </BrowserRouter>
    );
}