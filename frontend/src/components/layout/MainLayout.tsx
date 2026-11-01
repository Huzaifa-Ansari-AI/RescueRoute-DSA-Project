import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import Navbar from "./Navbar";
import Footer from "./Footer";
import Loading from "../ui/Loading";

export default function MainLayout() {
    return (
        <div
            style={{
                minHeight: "100vh",
                display: "flex",
                flexDirection: "column",
                backgroundColor: "#f8fafc",
            }}
        >
            <Navbar />

            <main
                style={{
                    flex: 1,
                    display: "flex",
                    flexDirection: "column",
                }}
            >
                <Suspense fallback={<Loading message="Loading RescueRoute..." size="large" />}>
                    <Outlet />
                </Suspense>
            </main>

            <Footer />
        </div>
    );
}
