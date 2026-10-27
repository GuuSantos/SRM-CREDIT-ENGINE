import { describe, expect, it } from "vitest";
import { formatarMoeda, formatarPercentual, somarDias } from "./formatar";

describe("formatarMoeda", () => {
  it("formata em pt-BR", () => {
    expect(formatarMoeda("92859.94")).toBe("R$\u00a092.859,94");
  });

  it("não perde precisão em valores que um number não representa", () => {
    // 9007199254740993 > 2^53: como number, viraria ...992
    expect(formatarMoeda("9007199254740993.01")).toBe("R$\u00a09.007.199.254.740.993,01");
  });
});

describe("formatarPercentual", () => {
  it("exibe a fração da API como percentual", () => {
    expect(formatarPercentual("0.0125")).toBe("1,25%");
  });
});

describe("somarDias", () => {
  it("conta dias corridos atravessando meses de 31 dias e a virada do ano", () => {
    expect(somarDias("2026-10-27", 90)).toBe("2027-01-25");
    expect(somarDias("2026-10-27", 92)).toBe("2027-01-27");
  });
});