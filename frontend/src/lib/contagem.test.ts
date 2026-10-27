import { describe, expect, it } from "vitest";
import { formatarContagem, segundosRestantes } from "./contagem";

const EXPIRA_EM = "2026-10-27T15:05:52.110330500Z"; // formato real do Java (nanossegundos)
const EXPIRA_EM_MS = Date.parse("2026-10-27T15:05:52.110Z");

describe("segundosRestantes", () => {
  it("aceita o instante com nanossegundos vindo do Java", () => {
    expect(segundosRestantes(EXPIRA_EM, EXPIRA_EM_MS - 120_000)).toBe(120);
  });

  it("arredonda para cima a fração de segundo", () => {
    expect(segundosRestantes(EXPIRA_EM, EXPIRA_EM_MS - 1_500)).toBe(2);
  });

  it("nunca fica negativo depois de expirar", () => {
    expect(segundosRestantes(EXPIRA_EM, EXPIRA_EM_MS + 10_000)).toBe(0);
  });
});

describe("formatarContagem", () => {
  it("exibe minutos e segundos", () => {
    expect(formatarContagem(125)).toBe("2:05");
    expect(formatarContagem(0)).toBe("0:00");
  });
});