"use client";

import { useCallback, useEffect, useState } from "react";
import { AreaRestrita } from "@/components/AreaRestrita";
import { api, mensagemDe } from "@/lib/api";
import { mascararDecimal, paraDecimalApi, percentualParaFracao } from "@/lib/conversao";
import { formatarDataHora, formatarNumero, formatarPercentual } from "@/lib/formatar";
import type { TaxasVigentes } from "@/lib/tipos";

const CAMPO = "mt-1 w-full rounded border px-3 py-2";
const BOTAO = "rounded bg-blue-700 px-4 py-2 font-medium text-white hover:bg-blue-800 disabled:opacity-50";

const CASAS_TAXA = 2;
const DIGITOS_TAXA = 4;   // até 99,99% (o backend limita a 20%)
const CASAS_CAMBIO = 4;
const DIGITOS_CAMBIO = 8; // até 9.999,9999

type Mensagem = { tipo: "ok" | "erro"; texto: string };

export default function PaginaTaxas() {
  return (
    <AreaRestrita perfil="TESOURARIA">
      <Taxas />
    </AreaRestrita>
  );
}

function Taxas() {
  const [vigentes, setVigentes] = useState<TaxasVigentes | null>(null);
  const [percentual, setPercentual] = useState("");
  const [cambioManual, setCambioManual] = useState("");
  const [mensagem, setMensagem] = useState<Mensagem | null>(null);
  const [processando, setProcessando] = useState(false);

  const carregar = useCallback(async () => {
    try {
      setVigentes(await api.taxasVigentes());
    } catch (e) {
      setMensagem({ tipo: "erro", texto: mensagemDe(e) });
    }
  }, []);

  useEffect(() => {
    carregar();
  }, [carregar]);

  async function executar(acao: () => Promise<unknown>, sucesso: string): Promise<boolean> {
    setProcessando(true);
    setMensagem(null);
    try {
      await acao();
      setMensagem({ tipo: "ok", texto: sucesso });
      await carregar();
      return true;
    } catch (e) {
      setMensagem({ tipo: "erro", texto: mensagemDe(e) });
      return false;
    } finally {
      setProcessando(false);
    }
  }

  async function cadastrarTaxaBase(evento: React.FormEvent) {
    evento.preventDefault();
    const fracao = percentualParaFracao(percentual);
    if (!fracao) {
      setMensagem({ tipo: "erro", texto: "Informe a taxa em % ao mês, por exemplo 1,25" });
      return;
    }
    if (await executar(() => api.cadastrarTaxaBase(fracao), `Taxa base de ${percentual}% a.m. vigente a partir de agora.`)) {
      setPercentual("");
    }
  }

  async function cadastrarCambio(evento: React.FormEvent) {
    evento.preventDefault();
    const valor = paraDecimalApi(cambioManual, 8);
    if (!valor) {
      setMensagem({ tipo: "erro", texto: "Informe o câmbio em reais por dólar, por exemplo 5,4321" });
      return;
    }
    if (await executar(() => api.cadastrarCambio(valor), "Câmbio manual registrado.")) {
      setCambioManual("");
    }
  }

  const taxa = vigentes?.taxaBase;
  const cambio = vigentes?.cambioUsd;

  return (
    <div className="space-y-4">
      <h1 className="text-lg font-semibold">Taxas (Tesouraria)</h1>

      {mensagem && (
        <p className={`rounded p-2 text-sm ${mensagem.tipo === "ok" ? "bg-green-50 text-green-800" : "bg-red-50 text-red-800"}`}>
          {mensagem.texto}
        </p>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="space-y-4 rounded-lg border bg-white p-5">
          <h2 className="font-semibold">Taxa base</h2>
          {taxa && (
            <div>
              <p className="text-2xl font-semibold">{formatarPercentual(taxa.taxaMensal)} a.m.</p>
              <p className="text-sm text-slate-500">
                Vigente desde {formatarDataHora(taxa.vigenteDesde)} · por {taxa.criadoPor}
              </p>
            </div>
          )}
          <form onSubmit={cadastrarTaxaBase} className="flex items-end gap-3">
            <label className="flex-1 text-sm">
              Nova taxa (% ao mês)
              <input
                value={percentual}
                onChange={(e) => setPercentual(mascararDecimal(e.target.value, CASAS_TAXA, DIGITOS_TAXA))}
                inputMode="numeric"
                placeholder="0,00"
                className={CAMPO}
              />
            </label>
            <button disabled={processando} className={BOTAO}>
              Cadastrar
            </button>
          </form>
          <p className="text-xs text-slate-500">
            Cada cadastro cria uma nova vigência; a anterior continua no histórico e as cotações já feitas mantêm a taxa da época.
          </p>
        </section>

        <section className="space-y-4 rounded-lg border bg-white p-5">
          <h2 className="font-semibold">Câmbio USD/BRL</h2>
          {cambio && (
            <div>
              <p className="text-2xl font-semibold">R$ {formatarNumero(cambio.brlPorUnidade)} por US$</p>
              <p className="text-sm text-slate-500">
                Vigente desde {formatarDataHora(cambio.vigenteDesde)} · {cambio.fonte === "PROVEDOR" ? "provedor" : "manual"} · por{" "}
                {cambio.criadoPor}
              </p>
              {cambio.desatualizado && (
                <p className="mt-2 rounded bg-amber-50 p-2 text-sm text-amber-800">
                  Câmbio com mais de 24h: cotações em USD estão bloqueadas até atualizar.
                </p>
              )}
            </div>
          )}
          <button
            onClick={() => executar(() => api.atualizarCambio(), "Câmbio atualizado pelo provedor.")}
            disabled={processando}
            className={BOTAO}
          >
            Buscar no provedor
          </button>
          <form onSubmit={cadastrarCambio} className="flex items-end gap-3">
            <label className="flex-1 text-sm">
              Ou informe manualmente (R$ por US$)
              <input
                value={cambioManual}
                onChange={(e) => setCambioManual(mascararDecimal(e.target.value, CASAS_CAMBIO, DIGITOS_CAMBIO))}
                inputMode="numeric"
                placeholder="0,0000"
                className={CAMPO}
              />
            </label>
            <button disabled={processando} className={BOTAO}>
              Cadastrar
            </button>
          </form>
        </section>
      </div>
    </div>
  );
}