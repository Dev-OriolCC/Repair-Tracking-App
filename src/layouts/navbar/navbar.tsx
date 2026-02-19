import { Button } from "@/components/ui/button";
import { SidebarTrigger } from "@/components/ui/sidebar";
import { NavigationSheet } from "@/components/navigation-sheet";

type NavBarProps = {
    title?: string;
};

export default function NavBar({ title = "Workshop App" }: NavBarProps) {
    return (
        <nav className="sticky top-0 z-10 flex h-16 w-full items-center border-b bg-background px-4">
            <div className="flex h-full w-full items-center justify-between">
                {/* Left: Sidebar toggle + Page title */}
                <div className="flex items-center gap-3">
                    <SidebarTrigger />
                    <h2 className="text-lg font-semibold">{title}</h2>
                </div>

                {/* Right: Username button + Mobile Menu */}
                <div className="flex items-center gap-3">
                    <Button className="rounded-full">Username</Button>

                    {/* Mobile Menu */}
                    <div className="md:hidden">
                        <NavigationSheet />
                    </div>
                </div>
            </div>
        </nav>
    );
}