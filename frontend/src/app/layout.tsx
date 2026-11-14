import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "SURCO",
  description: "Rotaciones de cultivo resilientes con datos de la NASA",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  );
}
