import type { Metadata } from "next";
import { Nunito, Nunito_Sans } from "next/font/google";
import "./globals.css";

const nunito = Nunito_Sans({ variable: "--font-nunito", subsets: ["latin"] });

// Títulos
const nunitoTitulo = Nunito({
  variable: "--font-nunito-titulo",
  subsets: ["latin"],
  weight: ["700", "800", "900"],
});

export const metadata: Metadata = {
  title: "SURCO",
  description: "Ciclos de cultivo adaptados al clima que viene, con datos de la NASA y el IDEAM",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es">
      <body className={`${nunito.variable} ${nunitoTitulo.variable} font-sans antialiased`}>{children}</body>
    </html>
  );
}
