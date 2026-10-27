"use client";

import { mascararMoeda, CamposRecebivel } from "@/lib/conversao";
import type { Cedente, Moeda, TipoRecebivel } from "@/lib/tipos";
import { hojeEmSaoPaulo, somarDias } from "@/lib/formatar";

const CAMPO = "mt-1 w-full rounded border px-3 py-2 disabled:bg-slate-100";
const ATALHOS_PRAZO = [30, 60, 90, 120];

type Props = {
  campos: CamposRecebivel;
  cedentes: Cedente[];
  bloqueado: boolean;
  aoMudar: (campos: CamposRecebivel) => void;
};

export function FormularioRecebivel({ campos, cedentes, bloqueado, aoMudar }: Props) {
  function alterar<K extends keyof CamposRecebivel>(campo: K, valor: CamposRecebivel[K]) {
    aoMudar({ ...campos, [campo]: valor });
  }

  const cheque = campos.tipo === "CHEQUE_PRE_DATADO";

  return (
    <fieldset disabled={bloqueado} className="grid gap-4 sm:grid-cols-2">
      <label className="text-sm sm:col-span-2">
        Cedente
        <select value={campos.cedenteId} onChange={(e) => alterar("cedenteId", e.target.value)} className={CAMPO}>
          <option value="">Selecione...</option>
          {cedentes.map((c) => (
            <option key={c.id} value={c.id}>
              {c.razaoSocial} — {c.cnpj}
            </option>
          ))}
        </select>
      </label>

      <label className="text-sm">
        Tipo
        <select
          value={campos.tipo}
          onChange={(e) => alterar("tipo", e.target.value as TipoRecebivel)}
          className={CAMPO}
        >
          <option value="DUPLICATA_MERCANTIL">Duplicata mercantil</option>
          <option value="CHEQUE_PRE_DATADO">Cheque pré-datado</option>
        </select>
      </label>

      <label className="text-sm">
        {cheque ? "CMC7 do cheque" : "Número da duplicata"}
        <input
          value={campos.numero}
          onChange={(e) => alterar("numero", e.target.value)}
          maxLength={30}
          className={CAMPO}
        />
      </label>

      <label className="text-sm">
        Valor de face (R$)
        <input
            value={campos.valorFace}
            onChange={(e) => alterar("valorFace", mascararMoeda(e.target.value))}
            inputMode="numeric"
            placeholder="0,00"
            className={CAMPO}
        />
      </label>

        <div className="text-sm">
        <label>
            Vencimento
            <input
            type="date"
            value={campos.vencimento}
            onChange={(e) => alterar("vencimento", e.target.value)}
            className={CAMPO}
            />
        </label>
        <div className="mt-2 flex gap-2">
            {ATALHOS_PRAZO.map((dias) => (
            <button
                key={dias}
                type="button"
                onClick={() => alterar("vencimento", somarDias(hojeEmSaoPaulo(), dias))}
                className="rounded border px-2 py-0.5 text-xs text-slate-600 hover:bg-slate-100"
            >
                +{dias}d
            </button>
            ))}
        </div>
        </div>

      <label className="text-sm sm:col-span-2">
        Moeda de pagamento
        <select
          value={campos.moedaPagamento}
          onChange={(e) => alterar("moedaPagamento", e.target.value as Moeda)}
          className={CAMPO}
        >
          <option value="BRL">Real (BRL)</option>
          <option value="USD">Dólar (USD)</option>
        </select>
      </label>
    </fieldset>
  );
}