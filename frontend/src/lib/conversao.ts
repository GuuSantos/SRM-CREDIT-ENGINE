import type { DadosRecebivel, Moeda, TipoRecebivel } from "./tipos";

const DIGITOS_MAXIMOS_VALOR = 12; // 1.000.000.000,00 = limite da API

export type CamposRecebivel = {
  cedenteId: string;
  tipo: TipoRecebivel;
  numero: string;
  valorFace: string;
  vencimento: string;
  moedaPagamento: Moeda;
};

export const CAMPOS_VAZIOS: CamposRecebivel = {
  cedenteId: "",
  tipo: "DUPLICATA_MERCANTIL",
  numero: "",
  valorFace: "",
  vencimento: "",
  moedaPagamento: "BRL",
};

// "100.000,50" ou "100000.50" → "100000.50". Nunca passa por number (ponto flutuante).
export function paraDecimalApi(texto: string, casasMaximas: number): string | null {
  let normal = texto.trim().replace(/\s/g, "");
  if (normal.includes(",")) normal = normal.replace(/\./g, "").replace(",", ".");
  const padrao = new RegExp(`^\\d+(\\.\\d{1,${casasMaximas}})?$`);
  return padrao.test(normal) ? normal : null;
}

// "1,25" (% ao mês) → "0.0125" (fração, como a API espera). Desloca a vírgula no texto.
export function percentualParaFracao(texto: string): string | null {
  const normal = texto.trim().replace(",", ".");
  if (!/^\d+(\.\d+)?$/.test(normal)) return null;
  const [inteiro, fracao = ""] = normal.split(".");
  const digitos = inteiro.padStart(3, "0") + fracao;
  const corte = digitos.length - fracao.length - 2;
  return `${digitos.slice(0, corte)}.${digitos.slice(corte)}`.replace(/^0+(?=\d)/, "");
}

export function montarDados(campos: CamposRecebivel): DadosRecebivel | null {
  const valorFace = paraDecimalApi(campos.valorFace, 2);
  if (!campos.cedenteId || !campos.numero.trim() || !campos.vencimento || !valorFace) return null;
  return {
    cedenteId: Number(campos.cedenteId),
    tipo: campos.tipo,
    numero: campos.numero.trim(),
    valorFace,
    vencimento: campos.vencimento,
    moedaPagamento: campos.moedaPagamento,
  };
}

// Máscara por casas fixas: os dígitos entram pela direita, como em app de banco.
// mascararDecimal("125", 2, 4) → "1,25"   |   mascararDecimal("54321", 4, 8) → "5,4321"
export function mascararDecimal(texto: string, casas: number, digitosMaximos: number): string {
  const digitos = texto.replace(/\D/g, "").replace(/^0+/, "").slice(0, digitosMaximos);
  if (!digitos) return "";
  const completo = digitos.padStart(casas + 1, "0");
  const inteiro = completo.slice(0, -casas).replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return `${inteiro},${completo.slice(-casas)}`;
}

export function mascararMoeda(texto: string): string {
  return mascararDecimal(texto, 2, DIGITOS_MAXIMOS_VALOR);
}