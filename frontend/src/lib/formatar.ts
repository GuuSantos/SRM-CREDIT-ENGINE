import type { Moeda } from "./tipos";

const FUSO = "America/Sao_Paulo";

// Intl aceita o decimal como string e formata sem converter para float
export function formatarMoeda(valor: string | null | undefined, moeda: Moeda = "BRL"): string {
  if (valor == null) return "—";
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: moeda }).format(
    valor as Intl.StringNumericLiteral,
  );
}

export function formatarPercentual(fracao: string | null | undefined): string {
  if (fracao == null) return "—";
  return new Intl.NumberFormat("pt-BR", {
    style: "percent",
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  }).format(fracao as Intl.StringNumericLiteral);
}

export function formatarNumero(valor: string | null | undefined, casas = 4): string {
  if (valor == null) return "—";
  return new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: casas }).format(
    valor as Intl.StringNumericLiteral,
  );
}

export function formatarDataHora(instante: string): string {
  return new Date(normalizarInstante(instante)).toLocaleString("pt-BR", { timeZone: FUSO });
}

export function hojeEmSaoPaulo(): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: FUSO }).format(new Date()); // AAAA-MM-DD
}

// O Java manda até 9 casas nos segundos (…52.110330500Z); o Date do JS só garante 3
export function normalizarInstante(instante: string): string {
  return instante.replace(/(\.\d{3})\d+/, "$1");
}