import { Outlet } from "react-router-dom";
import Navbar from "./Navbar";

export default function MainLayout() {
    return (
        <div
            style={{
                minHeight: "100vh",
                display: "flex",
                flexDirection: "column",
            }}
        >
            <Navbar />

            <main
                style={{
                    flex: 1,
                    padding: "1.5rem",
                }}
            >
                <Outlet />
            </main>

            {/* Footer will be added later */}
        </div>
    );
}