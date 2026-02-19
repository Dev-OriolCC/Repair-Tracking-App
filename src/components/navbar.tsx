import { Button } from "@/components/ui/button";
import { NavMenu } from "@/components/nav-menu";
import { NavigationSheet } from "@/components/navigation-sheet";

const Navbar = () => {
  return (
    <nav className="fixed inset-x-1 top-6 mx-auto h-16 max-w-(--breakpoint-xl)  border bg-background">
      <div className="mx-auto flex h-full items-center justify-between ">
        <h1>Workshop App</h1>

        {/* Desktop Menu */}
        <NavMenu className="hidden md:block" />

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
};

export default Navbar;
