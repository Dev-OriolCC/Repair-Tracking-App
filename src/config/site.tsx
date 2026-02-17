import { DollarSign, Gauge, House, type LucideIcon, MessagesSquare, Wrench } from "lucide-react";

export type SiteConfig = typeof siteConfig;
export type Navigation = {
  icon: LucideIcon;
  name: string;
  href: string;
};

export const siteConfig = {
  title: "Workshop App",
  description: "Software to manage Repair Orders & Installment Payments.",
};

export const navigations: Navigation[] = [
  {
    icon: House,
    name: "Home",
    href: "/",
  },
  {
    icon: Wrench,
    name: "Repair Orders",
    href: "/repair-orders",
  },
  {
    icon: DollarSign,
    name: "Installments",
    href: "/installments",
  },
];
