import { normalizarInstante } from "./formatar";

export function segundosRestantes(expiraEm: string, agoraMs: number): number {
  const restanteMs = Date.parse(normalizarInstante(expiraEm)) - agoraMs;
  return Math.max(0, Math.ceil(restanteMs / 1000));
}

export function formatarContagem(segundos: number): string {
  const minutos = Math.floor(segundos / 60);
  const resto = segundos % 60;
  return `${minutos}:${String(resto).padStart(2, "0")}`;
}