import { describe, expect, it } from "vitest";
import { CAMPOS_VAZIOS, mascararMoeda, montarDados, paraDecimalApi, percentualParaFracao } from "./conversao";

describe("mascararMoeda", () => {
  it("entra pelos centavos e agrupa os milhares", () => {
    expect(mascararMoeda("1")).toBe("0,01");
    expect(mascararMoeda("10000000")).toBe("100.000,00");
  });

  it("ignora letras e símbolos", () => {
    expect(mascararMoeda("R$ 1.234,5a6")).toBe("1.234,56");
  });

  it("limita a 12 dígitos", () => {
    expect(mascararMoeda("1234567890123")).toBe("1.234.567.890,12");
  });

  it("fica vazio sem dígitos", () => {
    expect(mascararMoeda("abc")).toBe("");
  });
});

describe("paraDecimalApi", () => {
  it("converte o formato brasileiro para o decimal da API", () => {
    expect(paraDecimalApi("100.000,50", 2)).toBe("100000.50");
    expect(paraDecimalApi("100000.50", 2)).toBe("100000.50");
  });

  it("recusa casas decimais demais e texto inválido", () => {
    expect(paraDecimalApi("1,234", 2)).toBeNull();
    expect(paraDecimalApi("abc", 2)).toBeNull();
  });
});

describe("percentualParaFracao", () => {
  it("desloca a vírgula sem passar por ponto flutuante", () => {
    expect(percentualParaFracao("1")).toBe("0.01");
    expect(percentualParaFracao("1,25")).toBe("0.0125");
    expect(percentualParaFracao("12.5")).toBe("0.125");
    expect(percentualParaFracao("100")).toBe("1.00");
  });

  it("recusa texto inválido", () => {
    expect(percentualParaFracao("um")).toBeNull();
  });
});

describe("montarDados", () => {
  it("só monta a requisição com todos os campos preenchidos", () => {
    expect(montarDados(CAMPOS_VAZIOS)).toBeNull();
    expect(
      montarDados({ ...CAMPOS_VAZIOS, cedenteId: "1", numero: " DM-1 ", valorFace: "100.000,00", vencimento: "2027-01-25" }),
    ).toEqual({
      cedenteId: 1,
      tipo: "DUPLICATA_MERCANTIL",
      numero: "DM-1",
      valorFace: "100000.00",
      vencimento: "2027-01-25",
      moedaPagamento: "BRL",
    });
  });
});