import { SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "./app-sidebar";
import { Outlet, useLocation } from "react-router-dom";
import NavBar from "./navbar/navbar";

const pageTitles: Record<string, string> = {
    "/": "Home",
    "/repair-orders": "Repair Orders",
    "/installments": "Installments",
};

export default function MainLayout() {
    const { pathname } = useLocation();
    const title = pageTitles[pathname] ?? "Workshop App";

    return (
        <SidebarProvider>
            <AppSidebar />
            <main className="flex flex-1 flex-col min-h-svh w-full overflow-auto">
                <NavBar title={title} />
                <div className="flex-1 p-6">
                    <Outlet />
                </div>
            </main>
        </SidebarProvider>
    );
}