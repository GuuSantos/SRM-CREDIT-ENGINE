import type { Metadata } from "next";
import { Navegacao } from "@/components/Navegacao";
import { ProvedorSessao } from "@/lib/sessao";
import "./globals.css";

export const metadata: Metadata = {
  title: "SRM Credit Engine",
  description: "Precificação, cotação e liquidação de recebíveis",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <body className="min-h-screen bg-slate-50 text-slate-900 antialiased">
        <ProvedorSessao>
          <Navegacao />
          <main className="mx-auto max-w-screen-2xl px-6 py-6">{children}</main>
        </ProvedorSessao>
      </body>
    </html>
  );
}