"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSessao } from "@/lib/sessao";

const LINKS = [
  { href: "/painel", rotulo: "Painel", perfil: "OPERADOR" },
  { href: "/liquidacoes", rotulo: "Liquidações", perfil: "OPERADOR" },
  { href: "/taxas", rotulo: "Taxas", perfil: "TESOURARIA" },
];

export function Navegacao() {
  const { usuario, temPerfil, sair } = useSessao();
  const caminho = usePathname();
  if (!usuario) return null;

  return (
    <header className="border-b bg-white">
      <nav className="mx-auto flex max-w-6xl items-center gap-6 px-4 py-3">
        <span className="flex items-center gap-2 font-semibold text-slate-900">
        <span className="grid h-7 w-7 place-items-center rounded-md bg-blue-800 text-xs font-bold text-white">CE</span>
        Credit Engine
        </span>
        {LINKS.filter((link) => temPerfil(link.perfil)).map((link) => (
          <Link
            key={link.href}
            href={link.href}
            className={caminho === link.href ? "font-medium text-blue-700" : "text-slate-600 hover:text-slate-900"}
          >
            {link.rotulo}
          </Link>
        ))}
        <span className="ml-auto text-sm text-slate-500">{usuario.usuario}</span>
        <button onClick={sair} className="text-sm text-slate-600 hover:text-red-700">
          Sair
        </button>
      </nav>
    </header>
  );
}