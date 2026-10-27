"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api, definirCredencial, limparCredencial, quandoPerderAutenticacao } from "./api";
import type { Usuario } from "./tipos";

const INATIVIDADE_MS = 15 * 60 * 1000;
const EVENTOS_ATIVIDADE = ["mousemove", "keydown", "click"] as const;

type Sessao = {
  usuario: Usuario | null;
  entrar: (usuario: string, senha: string) => Promise<Usuario>;
  sair: () => void;
  temPerfil: (perfil: string) => boolean;
};

const ContextoSessao = createContext<Sessao | null>(null);

export function ProvedorSessao({ children }: { children: React.ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);

  const sair = useCallback(() => {
    limparCredencial();
    setUsuario(null);
  }, []);

  const entrar = useCallback(async (nome: string, senha: string) => {
    definirCredencial(nome, senha);
    try {
      const atual = await api.usuarioAtual();
      setUsuario(atual);
      return atual;
    } catch (erro) {
      limparCredencial();
      throw erro;
    }
  }, []);

  useEffect(() => {
    quandoPerderAutenticacao(sair);
  }, [sair]);

  useEffect(() => {
    if (!usuario) return;
    let temporizador = setTimeout(sair, INATIVIDADE_MS);
    const reiniciar = () => {
      clearTimeout(temporizador);
      temporizador = setTimeout(sair, INATIVIDADE_MS);
    };
    EVENTOS_ATIVIDADE.forEach((evento) => window.addEventListener(evento, reiniciar));
    return () => {
      clearTimeout(temporizador);
      EVENTOS_ATIVIDADE.forEach((evento) => window.removeEventListener(evento, reiniciar));
    };
  }, [usuario, sair]);

  const valor = useMemo<Sessao>(
    () => ({
      usuario,
      entrar,
      sair,
      temPerfil: (perfil) => usuario?.perfis.includes(perfil) ?? false,
    }),
    [usuario, entrar, sair],
  );

  return <ContextoSessao.Provider value={valor}>{children}</ContextoSessao.Provider>;
}

export function useSessao() {
  const sessao = useContext(ContextoSessao);
  if (!sessao) throw new Error("useSessao precisa estar dentro de ProvedorSessao");
  return sessao;
}