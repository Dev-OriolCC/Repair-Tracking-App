import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"
import { AppSidebar } from "./app-sidebar";
import { Outlet } from "react-router-dom";

// type MainLayoutProps = {
//     children?: React.ReactNode
// }

export default function MainLayout() {
    return (
        <SidebarProvider>
            <AppSidebar />
            {/* Navbar */}
            <main>
                <SidebarTrigger />
                <Outlet />            
            </main>
            {/* footer */}
        </SidebarProvider>
    )
}