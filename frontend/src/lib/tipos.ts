export type Moeda = "BRL" | "USD";
export type TipoRecebivel = "DUPLICATA_MERCANTIL" | "CHEQUE_PRE_DATADO";

export type Usuario = {
    usuario: string; 
    perfis: string[]
};

export type Cedente = {
    id: number;
    cnpj: string;
    razaoSocial: string
};

export type DadosRecebivel = {
    cedenteId: number;
    tipo: TipoRecebivel;
    numero: string;
    valorFace: string;
    vencimento: string; // AAAA-MM-DD
    moedaPagamento: Moeda;
};

export type Simulacao = {
  prazoDias: number;
  taxaBaseMensal: string;
  spreadMensal: string;
  moedaPagamento: Moeda;
  cambioBrlPorUsd: string | null;
  cambioVigenteDesde: string | null;
  cambioDesatualizado: boolean;
  valorPresenteBrl: string;
  desagio: string;
  valorPagamento: string;
};

export type Cotacao = {
  id: string;
  recebivelId: number;
  prazoDias: number;
  taxaBaseMensal: string;
  spreadMensal: string;
  moedaPagamento: Moeda;
  cambioBrlPorUsd: string | null;
  valorPresenteBrl: string;
  desagio: string;
  valorPagamento: string;
  expiraEm: string; // instante UTC
};

export type ItemExtrato = {
  id: number;
  cotacaoId: string;
  cedenteCnpj: string;
  cedenteRazaoSocial: string;
  numeroTitulo: string;
  tipo: TipoRecebivel;
  valorFace: string;
  valorPresenteBrl: string;
  desagio: string;
  moedaPagamento: Moeda;
  valorPagamento: string;
  cambioBrlPorUsd: string | null;
  taxaBaseMensal: string;
  spreadMensal: string;
  prazoDias: number;
  liquidadoPor: string;
  liquidadoEm: string;
};

export type PaginaExtrato = {
  itens: ItemExtrato[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
};

export type Problema = {
  status: number;
  title?: string;
  detail?: string;
  codigo?: string;
  campos?: { campo: string; mensagem: string }[];
  traceId?: string;
};

export type TaxaBase = { id: number; taxaMensal: string; vigenteDesde: string; criadoPor: string };

export type Cambio = {
  id: number;
  moeda: Moeda;
  brlPorUnidade: string;
  vigenteDesde: string;
  fonte: "PROVEDOR" | "MANUAL";
  criadoPor: string;
  desatualizado: boolean;
};

export type TaxasVigentes = { taxaBase: TaxaBase; cambioUsd: Cambio };