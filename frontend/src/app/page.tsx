"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { ErroApi } from "@/lib/api";
import { useSessao } from "@/lib/sessao";

export default function Login() {
  const { entrar } = useSessao();
  const router = useRouter();
  const [usuario, setUsuario] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro(null);
    setEnviando(true);
    try {
      const atual = await entrar(usuario, senha);
      router.push(atual.perfis.includes("OPERADOR") ? "/painel" : "/taxas");
    } catch (e) {
      setErro(e instanceof ErroApi && e.status === 401 ? "Usuário ou senha inválidos" : (e as Error).message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="mx-auto mt-16 grid max-w-4xl overflow-hidden rounded-2xl border bg-white shadow-lg md:grid-cols-2">
      <aside className="hidden flex-col justify-between bg-gradient-to-br from-slate-900 via-blue-950 to-blue-800 p-10 text-white md:flex">
        <div className="flex items-center gap-3">
          <div className="grid h-10 w-10 place-items-center rounded-lg bg-white/10 text-lg font-bold">CE</div>
          <span className="text-lg font-semibold tracking-tight">Credit Engine</span>
        </div>
        <div className="space-y-3">
          <h2 className="text-2xl font-semibold leading-snug">Precificação e liquidação de recebíveis</h2>
          <p className="text-sm text-blue-100/80">
            Duplicatas e cheques com cálculo exato, cotação travada e liquidação idempotente, em BRL ou USD.
          </p>
        </div>
      </aside>

      <form onSubmit={enviar} className="space-y-5 p-10">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Entrar</h1>
          <p className="mt-1 text-sm text-slate-500">Use suas credenciais de operador ou tesouraria.</p>
        </div>

        <label className="block text-sm font-medium text-slate-700">
          Usuário
          <input
            value={usuario}
            onChange={(e) => setUsuario(e.target.value)}
            autoComplete="username"
            autoFocus
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
            required
          />
        </label>

        <label className="block text-sm font-medium text-slate-700">
          Senha
          <input
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            autoComplete="current-password"
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
            required
          />
        </label>

        {erro && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>}

        <button
          disabled={enviando}
          className="w-full rounded-lg bg-blue-700 py-2.5 font-medium text-white transition hover:bg-blue-800 disabled:opacity-50"
        >
          {enviando ? "Entrando..." : "Entrar"}
        </button>
      </form>
    </div>
  );
}