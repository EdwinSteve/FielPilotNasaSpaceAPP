import type { Metadata } from "next";
import { Inter } from "next/font/google";
import { NOMBRE_APP } from "@/compartido/ui/Marca";
import "./globals.css";

// Tipografía del diseño de Figma en toda la app
const inter = Inter({ subsets: ["latin"], variable: "--font-inter" });

export const metadata: Metadata = {
  title: NOMBRE_APP,
  description: "Ciclos de cultivo adaptados al clima que viene, con datos de la NASA y el IDEAM",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es" className={inter.variable}>
      <body className="font-sans antialiased">{children}</body>
    </html>
  );
}
