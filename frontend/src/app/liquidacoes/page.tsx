"use client";

import { useCallback, useEffect, useState } from "react";
import { AreaRestrita } from "@/components/AreaRestrita";
import { api, mensagemDe } from "@/lib/api";
import { formatarDataHora, formatarMoeda, formatarNumero, hojeEmSaoPaulo } from "@/lib/formatar";
import type { Cedente, PaginaExtrato, TipoRecebivel } from "@/lib/tipos";

const TAMANHO_PAGINA = 20;
const CAMPO = "mt-1 w-full rounded border px-3 py-2";
const NOME_TIPO: Record<TipoRecebivel, string> = {
  DUPLICATA_MERCANTIL: "Duplicata",
  CHEQUE_PRE_DATADO: "Cheque",
};

type Filtro = { dataInicio: string; dataFim: string; cedenteCnpj: string; moeda: string };

function parametrosDe(filtro: Filtro, pagina: number): Record<string, string> {
  const parametros: Record<string, string> = {
    dataInicio: filtro.dataInicio,
    dataFim: filtro.dataFim,
    pagina: String(pagina),
    tamanho: String(TAMANHO_PAGINA),
  };
  if (filtro.cedenteCnpj) parametros.cedenteCnpj = filtro.cedenteCnpj;
  if (filtro.moeda) parametros.moeda = filtro.moeda;
  return parametros;
}

export default function PaginaLiquidacoes() {
  return (
    <AreaRestrita perfil="OPERADOR">
      <Liquidacoes />
    </AreaRestrita>
  );
}

function Liquidacoes() {
  const hoje = hojeEmSaoPaulo();
  const [cedentes, setCedentes] = useState<Cedente[]>([]);
  const [rascunho, setRascunho] = useState<Filtro>({ dataInicio: hoje, dataFim: hoje, cedenteCnpj: "", moeda: "" });
  const [filtroAplicado, setFiltroAplicado] = useState<Filtro>(rascunho);
  const [resultado, setResultado] = useState<PaginaExtrato | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  const buscar = useCallback(async (filtro: Filtro, pagina: number) => {
    setCarregando(true);
    setErro(null);
    try {
      setResultado(await api.extrato(parametrosDe(filtro, pagina)));
      setFiltroAplicado(filtro);
    } catch (e) {
      setErro(mensagemDe(e));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    api.cedentes().then(setCedentes).catch((e) => setErro(mensagemDe(e)));
    buscar(rascunho, 0);
    // carga inicial: só uma vez, ao abrir a tela
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function alterar(campo: keyof Filtro, valor: string) {
    setRascunho({ ...rascunho, [campo]: valor });
  }

  function filtrar(evento: React.FormEvent) {
    evento.preventDefault();
    buscar(rascunho, 0);
  }

  const pagina = resultado?.pagina ?? 0;
  const totalPaginas = resultado?.totalPaginas ?? 0;

  return (
    <div className="space-y-4">
      <h1 className="text-lg font-semibold">Liquidações</h1>

      <form onSubmit={filtrar} className="grid gap-4 rounded-lg border bg-white p-4 sm:grid-cols-5">
        <label className="text-sm">
          De
          <input type="date" value={rascunho.dataInicio} onChange={(e) => alterar("dataInicio", e.target.value)} className={CAMPO} required />
        </label>
        <label className="text-sm">
          Até
          <input type="date" value={rascunho.dataFim} onChange={(e) => alterar("dataFim", e.target.value)} className={CAMPO} required />
        </label>
        <label className="text-sm">
          Cedente
          <select value={rascunho.cedenteCnpj} onChange={(e) => alterar("cedenteCnpj", e.target.value)} className={CAMPO}>
            <option value="">Todos</option>
            {cedentes.map((c) => (
              <option key={c.id} value={c.cnpj}>
                {c.razaoSocial}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm">
          Moeda
          <select value={rascunho.moeda} onChange={(e) => alterar("moeda", e.target.value)} className={CAMPO}>
            <option value="">Todas</option>
            <option value="BRL">BRL</option>
            <option value="USD">USD</option>
          </select>
        </label>
        <button disabled={carregando} className="self-end rounded bg-blue-700 py-2 font-medium text-white hover:bg-blue-800 disabled:opacity-50">
          {carregando ? "Buscando..." : "Filtrar"}
        </button>
      </form>

      {erro && <p className="rounded bg-red-50 p-2 text-sm text-red-800">{erro}</p>}

      <div className="overflow-x-auto rounded-lg border bg-white">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-600">
            <tr>
              <th className="px-3 py-2">Data/hora</th>
              <th className="px-3 py-2">Título</th>
              <th className="px-3 py-2">Cedente</th>
              <th className="px-3 py-2">Tipo</th>
              <th className="px-3 py-2 text-right">Face</th>
              <th className="px-3 py-2 text-right">Valor presente</th>
              <th className="px-3 py-2 text-right">Deságio</th>
              <th className="px-3 py-2 text-right">Câmbio</th>
              <th className="px-3 py-2 text-right">Valor pago</th>
              <th className="px-3 py-2">Usuário</th>
            </tr>
          </thead>
          <tbody>
            {resultado?.itens.map((item) => (
              <tr key={item.id} className="border-t">
                <td className="whitespace-nowrap px-3 py-2">{formatarDataHora(item.liquidadoEm)}</td>
                <td className="whitespace-nowrap px-3 py-2">{item.numeroTitulo}</td>
                <td className="whitespace-nowrap px-3 py-2">{item.cedenteRazaoSocial}</td>
                <td className="px-3 py-2">{NOME_TIPO[item.tipo]}</td>
                <td className="whitespace-nowrap px-3 py-2 text-right">{formatarMoeda(item.valorFace)}</td>
                <td className="whitespace-nowrap px-3 py-2 text-right">{formatarMoeda(item.valorPresenteBrl)}</td>
                <td className="whitespace-nowrap px-3 py-2 text-right">{formatarMoeda(item.desagio)}</td>
                <td className="whitespace-nowrap px-3 py-2 text-right">
                  {item.cambioBrlPorUsd ? formatarNumero(item.cambioBrlPorUsd) : "—"}
                </td>
                <td className="whitespace-nowrap px-3 py-2 text-right font-medium">
                  {formatarMoeda(item.valorPagamento, item.moedaPagamento)}
                </td>
                <td className="px-3 py-2">{item.liquidadoPor}</td>
              </tr>
            ))}
            {resultado && resultado.itens.length === 0 && (
              <tr>
                <td colSpan={10} className="px-3 py-6 text-center text-slate-500">
                  Nenhuma liquidação no período.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {resultado && resultado.totalItens > 0 && (
        <div className="flex items-center justify-between text-sm text-slate-600">
          <span>
            Página {pagina + 1} de {totalPaginas} · {resultado.totalItens} liquidações
          </span>
          <div className="space-x-2">
            <button
              onClick={() => buscar(filtroAplicado, pagina - 1)}
              disabled={carregando || pagina === 0}
              className="rounded border px-3 py-1 disabled:opacity-40"
            >
              ‹ Anterior
            </button>
            <button
              onClick={() => buscar(filtroAplicado, pagina + 1)}
              disabled={carregando || pagina + 1 >= totalPaginas}
              className="rounded border px-3 py-1 disabled:opacity-40"
            >
              Próxima ›
            </button>
          </div>
        </div>
      )}
    </div>
  );
}