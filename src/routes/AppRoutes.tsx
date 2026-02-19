import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import Home from "../pages/Home";
import MainLayout from "@/layouts/MainLayout";
import RepairOrders from "@/pages/RepairOrders";
import Installments from "@/pages/Installments";

export default function AppRoutes() {
    return (
        <Router>
            <Routes>
                <Route element={<MainLayout />}>
                    <Route path="/" element={<Home />} />
                    <Route path="/repair-orders" element={<RepairOrders />} />
                    <Route path="/installments" element={<Installments />} />
                </Route>
            </Routes>
        </Router>
    );
}
