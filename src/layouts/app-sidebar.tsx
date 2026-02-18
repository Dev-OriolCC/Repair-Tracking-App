import { SidebarContent, SidebarFooter, SidebarGroup, SidebarHeader } from "@/components/ui/sidebar";
import { Sidebar } from "lucide-react";

export function AppSidebar() {
    return (

        <Sidebar >
            <SidebarHeader />
            <SidebarContent>
                <p>Testing</p>
                <SidebarGroup />
                <SidebarGroup />
            </SidebarContent>
            <SidebarFooter />
        </Sidebar>

    )
}