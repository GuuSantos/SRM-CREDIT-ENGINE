import { formatarMoeda, formatarNumero, formatarPercentual } from "@/lib/formatar";
import type { Simulacao } from "@/lib/tipos";

export type Precificacao = Pick<
  Simulacao,
  | "prazoDias"
  | "taxaBaseMensal"
  | "spreadMensal"
  | "moedaPagamento"
  | "cambioBrlPorUsd"
  | "valorPresenteBrl"
  | "desagio"
  | "valorPagamento"
>;

export function ResumoPrecificacao({ precificacao: p }: { precificacao: Precificacao }) {
  return (
    <dl className="grid grid-cols-2 gap-y-2 text-sm">
      <dt className="text-slate-500">Prazo</dt>
      <dd className="text-right">{p.prazoDias} dias</dd>
      <dt className="text-slate-500">Taxa base</dt>
      <dd className="text-right">{formatarPercentual(p.taxaBaseMensal)} a.m.</dd>
      <dt className="text-slate-500">Spread</dt>
      <dd className="text-right">{formatarPercentual(p.spreadMensal)} a.m.</dd>
      <dt className="text-slate-500">Valor presente</dt>
      <dd className="text-right">{formatarMoeda(p.valorPresenteBrl, "BRL")}</dd>
      <dt className="text-slate-500">Deságio</dt>
      <dd className="text-right">{formatarMoeda(p.desagio, "BRL")}</dd>
      {p.moedaPagamento === "USD" && (
        <>
          <dt className="text-slate-500">Câmbio</dt>
          <dd className="text-right">R$ {formatarNumero(p.cambioBrlPorUsd)} por US$</dd>
        </>
      )}
      <dt className="pt-2 font-semibold">Valor a pagar</dt>
      <dd className="pt-2 text-right text-lg font-semibold">{formatarMoeda(p.valorPagamento, p.moedaPagamento)}</dd>
    </dl>
  );
}