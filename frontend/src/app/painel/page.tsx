"use client";

import { useEffect, useMemo, useState } from "react";
import { AreaRestrita } from "@/components/AreaRestrita";
import { FormularioRecebivel } from "@/components/FormularioRecebivel";
import { ResumoPrecificacao } from "@/components/ResumoPrecificacao";
import { api, ErroApi, mensagemDe } from "@/lib/api";
import { formatarContagem, segundosRestantes } from "@/lib/contagem";
import { CAMPOS_VAZIOS, montarDados, type CamposRecebivel } from "@/lib/conversao";
import { formatarMoeda } from "@/lib/formatar";
import type { Cedente, Cotacao, Simulacao } from "@/lib/tipos";
import { useAgora } from "@/lib/useAgora";

type Etapa = "edicao" | "cotada" | "liquidada";

const ESPERA_SIMULACAO_MS = 400;
const BOTAO = "w-full rounded bg-blue-700 py-2 font-medium text-white hover:bg-blue-800 disabled:opacity-50";

export default function PaginaPainel() {
  return (
    <AreaRestrita perfil="OPERADOR">
      <Painel />
    </AreaRestrita>
  );
}

function Painel() {
  const [cedentes, setCedentes] = useState<Cedente[]>([]);
  const [campos, setCampos] = useState<CamposRecebivel>(CAMPOS_VAZIOS);
  const [etapa, setEtapa] = useState<Etapa>("edicao");
  const [simulacao, setSimulacao] = useState<Simulacao | null>(null);
  const [cotacao, setCotacao] = useState<Cotacao | null>(null);
  const [liquidacaoNova, setLiquidacaoNova] = useState<boolean | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [processando, setProcessando] = useState(false);
  const agora = useAgora();

  const dados = useMemo(() => montarDados(campos), [campos]);
  const simulacaoExibida = dados ? simulacao : null;

  useEffect(() => {
    api.cedentes().then(setCedentes).catch((e) => setErro(mensagemDe(e)));
  }, []);

  // Simulação em tempo real: só chama a API quando o usuário para de digitar (debounce)
  useEffect(() => {
    if (etapa !== "edicao" || !dados) return;
    let descartada = false;
    const temporizador = setTimeout(() => {
      api
        .simular(dados)
        .then((resultado) => {
          if (descartada) return;
          setSimulacao(resultado);
          setErro(null);
        })
        .catch((e) => {
          if (descartada) return;
          setSimulacao(null);
          setErro(mensagemDe(e));
        });
    }, ESPERA_SIMULACAO_MS);
    return () => {
      descartada = true;
      clearTimeout(temporizador);
    };
  }, [dados, etapa]);

  async function cotar() {
    if (!dados) return;
    setProcessando(true);
    setErro(null);
    try {
      setCotacao(await api.cotar(dados));
      setEtapa("cotada");
    } catch (e) {
      setErro(mensagemDe(e));
    } finally {
      setProcessando(false);
    }
  }

  async function liquidar() {
    if (!cotacao) return;
    setProcessando(true);
    setErro(null);
    try {
      const resultado = await api.liquidar(cotacao.id);
      setLiquidacaoNova(resultado.nova);
      setEtapa("liquidada");
    } catch (e) {
      const expirou = e instanceof ErroApi && e.codigo === "COTACAO_EXPIRADA";
      setErro(expirou ? "A cotação expirou. Cote novamente." : mensagemDe(e));
    } finally {
      setProcessando(false);
    }
  }

  function voltarParaEdicao() {
    setCotacao(null);
    setLiquidacaoNova(null);
    setErro(null);
    setEtapa("edicao");
  }

  function novaOperacao() {
    setCampos(CAMPOS_VAZIOS);
    setSimulacao(null);
    voltarParaEdicao();
  }

  const restantes = cotacao ? segundosRestantes(cotacao.expiraEm, agora) : 0;
  const expirada = etapa === "cotada" && restantes === 0;
  const resultado = etapa === "edicao" ? simulacaoExibida : cotacao;

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <section className="rounded-lg border bg-white p-5">
        <h1 className="mb-4 text-lg font-semibold">Recebível</h1>
        <FormularioRecebivel
          campos={campos}
          cedentes={cedentes}
          bloqueado={etapa !== "edicao"}
          aoMudar={setCampos}
        />
      </section>

      <section className="space-y-4 rounded-lg border bg-white p-5">
        <h2 className="text-lg font-semibold">{etapa === "edicao" ? "Simulação" : "Cotação"}</h2>

        {resultado ? (
          <ResumoPrecificacao precificacao={resultado} />
        ) : (
          <p className="text-sm text-slate-500">Preencha todos os campos para simular.</p>
        )}

        {etapa === "edicao" && simulacaoExibida?.cambioDesatualizado && (
          <p className="rounded bg-amber-50 p-2 text-sm text-amber-800">
            Câmbio com mais de 24h: a cotação será recusada até a Tesouraria atualizar a taxa.
          </p>
        )}

        {erro && <p className="rounded bg-red-50 p-2 text-sm text-red-800">{erro}</p>}

        {etapa === "edicao" && (
          <button onClick={cotar} disabled={!simulacaoExibida || processando} className={BOTAO}>
            Cotar (trava por 2 min)
          </button>
        )}

        {etapa === "cotada" && cotacao && (
          <div className="space-y-3">
            <p className={expirada ? "font-medium text-red-700" : "text-slate-700"}>
              {expirada ? "Cotação expirada" : `Válida por ${formatarContagem(restantes)}`}
            </p>
            {expirada ? (
              <button onClick={cotar} disabled={processando} className={BOTAO}>
                Cotar novamente
              </button>
            ) : (
              <button onClick={liquidar} disabled={processando} className={BOTAO}>
                Liquidar {formatarMoeda(cotacao.valorPagamento, cotacao.moedaPagamento)}
              </button>
            )}
            <button onClick={voltarParaEdicao} className="w-full text-sm text-slate-600 hover:underline">
              Cancelar e editar
            </button>
          </div>
        )}

        {etapa === "liquidada" && (
          <div className="space-y-3">
            <p className="rounded bg-green-50 p-2 text-sm text-green-800">
              {liquidacaoNova ? "Liquidação registrada." : "Esta cotação já estava liquidada (nenhum novo registro)."}
            </p>
            <button onClick={novaOperacao} className={BOTAO}>
              Nova operação
            </button>
          </div>
        )}
      </section>
    </div>
  );
}