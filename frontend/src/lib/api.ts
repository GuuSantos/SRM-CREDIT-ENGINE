import { Cedente, Cotacao, DadosRecebivel, PaginaExtrato, Problema, Simulacao, Usuario } from "./tipos";

const URL_API = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

let credencial: string | null = null;
let aoPerderAutenticacao: (() => void) | null = null;

export function definirCredencial(usuario: string, senha: string){
    const bytes = new TextEncoder().encode(`${usuario}:${senha}`)
    credencial = "Basic "+ btoa(String.fromCharCode(...bytes));
}

export function limparCredencial(){
    credencial = null;
}

export function quandoPerderAutenticacao(acao: () => void){
    aoPerderAutenticacao = acao;
}

export class ErroApi extends Error {
  readonly problema: Problema;

  constructor(problema: Problema) {
    super(problema.detail ?? problema.title ?? `Erro ${problema.status}`);
    this.problema = problema;
  }

  get status() {
    return this.problema.status;
  }

  get codigo() {
    return this.problema.codigo;
  }
}

async function requisicao<T>(metodo: string, caminho: string, corpo?: unknown): Promise<{ status: number; dados: T }> {
  const headers: Record<string, string> = {};
  if (credencial) headers.Authorization = credencial;
  if (corpo !== undefined) headers["Content-Type"] = "application/json";

  let resposta: Response;
  try {
    resposta = await fetch(URL_API + caminho, {
      method: metodo,
      headers,
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });
  } catch {
    throw new ErroApi({ status: 0, detail: "Não foi possível conectar ao servidor" });
  }

  const texto = await resposta.text();
  const json = texto ? JSON.parse(texto) : null;

  if (!resposta.ok) {
    if (resposta.status === 401 && credencial && aoPerderAutenticacao) aoPerderAutenticacao();
    throw new ErroApi({ ...(json ?? {}), status: resposta.status });
  }
  return { status: resposta.status, dados: json as T };
}

export const api = {
  usuarioAtual: () => requisicao<Usuario>("GET", "/usuarios/eu").then((r) => r.dados),
  cedentes: () => requisicao<Cedente[]>("GET", "/cedentes").then((r) => r.dados),
  simular: (dados: DadosRecebivel) => requisicao<Simulacao>("POST", "/simulacoes", dados).then((r) => r.dados),
  cotar: (dados: DadosRecebivel) => requisicao<Cotacao>("POST", "/cotacoes", dados).then((r) => r.dados),
  liquidar: (cotacaoId: string) =>
    requisicao<unknown>("PUT", `/cotacoes/${encodeURIComponent(cotacaoId)}/liquidacao`).then((r) => ({
      liquidacao: r.dados,
      nova: r.status === 201,
    })),
  extrato: (filtro: Record<string, string>) =>
    requisicao<PaginaExtrato>("GET", `/liquidacoes?${new URLSearchParams(filtro)}`).then((r) => r.dados),
};

export function mensagemDe(erro: unknown): string {
  if (erro instanceof ErroApi) {
    const campos = erro.problema.campos?.map((c) => `${c.campo}: ${c.mensagem}`).join("; ");
    return campos ? `${erro.message} (${campos})` : erro.message;
  }
  return "Erro inesperado";
}

