import { Outlet } from "react-router-dom";

export default function MainLayout() {
    return (
        <div
            style={{
                minHeight: "100vh",
                display: "flex",
                flexDirection: "column",
            }}
        >
            {/* Navbar will be added later */}

            <main
                style={{
                    flex: 1,
                    padding: "2rem",
                }}
            >
                <Outlet />
            </main>

            {/* Footer will be added later */}
        </div>
    );
}