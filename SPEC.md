# SPEC — SRM Credit Engine

Premissas, decisões de precisão e critérios de aceite definidos **antes** do código. Cada premissa é uma escolha consciente diante de uma ambiguidade do enunciado; as alternativas descartadas e cortes de escopo estão detalhados no `DECISIONS.md`.

## 1. Premissas

| Tema | Premissa adotada | Alternativa descartada |
|---|---|---|
| Prazo | Dias corridos entre a data da cotação e o vencimento ÷ 30, com expoente fracionário: `VP = Face / (1 + taxa base + spread)^(dias/30)`. Vencimento hoje ou no passado é rejeitado. | Meses inteiros (45 dias viraria 1 ou 2 meses); dias úteis/252 (exige calendário de feriados e conversão de taxa mensal → dia útil). |
| Taxa base | Cadastro com vigência (nova taxa = novo registro), mantido pela Tesouraria. Valor inicial 1,00% a.m. Armazenada como **fração** (`0.01`), nunca percentual. | Valor fixo em configuração; integração com CDI/Selic. |
| Spread | Fixo por tipo de recebível, definido em cada Strategy: Duplicata Mercantil 1,5% a.m., Cheque Pré-datado 2,5% a.m. | Spread por cedente/rating (sem modelo de risco no enunciado). |
| Moedas | Títulos sempre em BRL; pagamento em BRL ou USD. Câmbio expresso em **BRL por 1 USD**; `USD = BRL ÷ taxa`. Deságio sempre em BRL. | Títulos em outras moedas. |
| Câmbio: fonte | Cadastro com vigência. Alimentado por um provedor mockado, consultado por um job a cada 1h e pelo botão "Atualizar taxa" da Tesouraria; o cadastro manual é a contingência. **O provedor nunca é chamado na simulação, cotação ou liquidação** — elas só leem o cadastro. | Consultar o provedor na hora da cotação (colocaria uma dependência externa no caminho crítico). |
| Câmbio: qual taxa | A taxa vigente no momento da **cotação**. Taxa com mais de 24h é recusada nesse momento (verificação só na cotação). | Taxa da data da operação ou do vencimento (forward, inexistente hoje). |
| Fluxo | Simulação em tempo real (não grava) → cotação travada por **2 minutos** (congela taxas, câmbio, prazo e valores) → liquidação recebe só o `cotacaoId` e usa exatamente os valores cotados. Cotação expirada é recusada; a tela oferece cotar novamente. | Liquidar recalculando na hora (o operador veria um valor e liquidaria outro). |
| Recebível | Cadastrado antes da precificação (um a um ou em lote), com status `PENDENTE` → `LIQUIDADO`, vinculado a um cedente. | Recebível nascer junto com a cotação (não haveria "o mesmo recebível" a proteger). |
| Idempotência | Chave natural = `cotacaoId`, garantida por `UNIQUE (cotacao_id)` na liquidação. 1ª chamada → `201`; repetição → `200` com a mesma liquidação. Duas cotações do mesmo recebível liquidadas em paralelo → optimistic locking (`version`) → `409`. | Header `Idempotency-Key` (redundante: o corpo só tem `cotacaoId`, e a proteção real é a UNIQUE). |
| Auditoria | A liquidação é o registro de auditoria: snapshot de valores, taxa base, spread, câmbio (valor e referência ao registro), prazo, usuário e timestamps. UPDATE/DELETE bloqueados por trigger no banco. | Tabela de auditoria separada (duplicaria dados); apenas "não ter endpoint" (frágil a SQL direto). |
| Acesso | HTTP Basic com dois papéis: `OPERADOR` (recebíveis, simulação, cotação, liquidação, extrato) e `TESOURARIA` (taxas). Só quem criou a cotação pode liquidá-la. Identificadores de cotação são UUID. | JWT completo; grupos/mesas de operadores. |
| Datas | `timestamptz` em UTC no banco; exibição e cálculo de prazo pela data em America/Sao_Paulo; extrato filtra pela data da liquidação. | — |
| Erros | Problem Details (RFC 9457): `400` validação · `403` papel/dono · `404` inexistente · `409` já liquidado/concorrência · `422` cotação expirada ou câmbio vencido · `503` provedor indisponível (só no "Atualizar taxa"). Nunca `200` com erro. | Envelope de erro próprio. |

**Golden cases:** as premissas de aferição do enunciado (meses inteiros, 1% a.m., HALF_EVEN no final, BRL arredondado antes de converter) são compatíveis com as premissas acima: o motor recebe o prazo em meses (decimal), e 90/60 dias resultam exatamente em 3/2 meses.

## 2. Precisão numérica

- **Aplicação:** `BigDecimal` em todo o domínio, construído a partir de `String` (nunca de `double`). Cálculos intermediários com `MathContext.DECIMAL128` (34 dígitos significativos). Potência com expoente fracionário via biblioteca de precisão arbitrária (`BigDecimal.pow` só aceita inteiro; `Math.pow` usa `double`).
- **Arredondamento:** `HALF_EVEN`, 2 casas, **apenas no resultado final**. Em cross-currency, o valor presente em BRL é arredondado primeiro e então convertido e arredondado em USD — o BRL gravado é exatamente o que gerou o USD.
- **Banco (PostgreSQL):** valores monetários `NUMERIC(19,2)`; taxas e câmbio `NUMERIC(19,8)`.
- **Deságio:** `Face − VP em BRL` (ambos já com 2 casas), em BRL.

## 3. Critérios de aceite

Números de desempenho são **linha de base local** (`docker compose` em máquina de desenvolvimento), medidos com um script k6 versionado; o resultado medido é registrado no README.

**Usabilidade**
- U1. Simulação sem botão "calcular": a API é chamada após 300 ms sem digitação (debounce). *Teste de componente com timers falsos; latência coberta por D1.*
- U2. Erro de validação aparece junto ao campo e desabilita "Cotar"; erros da API exibem o `detail` em português. *Teste de componente.*
- U3. Contagem regressiva da cotação visível; ao expirar, "Liquidar" é bloqueado e aparece "Cotar novamente". *Teste de componente com timers falsos.*
- U4. "Liquidar" desabilita no primeiro clique. *Teste de componente; a unicidade da liquidação é garantida no backend (C3).*
- U5. Valores formatados por moeda em pt-BR; a tela mostra o câmbio usado e sua vigência; com câmbio vencido, o painel avisa ao abrir: "câmbio desatualizado, acione a Tesouraria". *Teste de componente com o golden case C3.*

**Segurança**
- S1. Sem credencial → `401`; `OPERADOR` em cadastro de taxa → `403`; `TESOURARIA` → `201`; só `/actuator/health` é público. Senhas com BCrypt, nenhuma no repositório, `{noop}` proibido. *Teste de integração por endpoint × papel.*
- S2. A liquidação aceita só `cotacaoId` (campo extra → `400`) e só pelo usuário que criou a cotação (outro → `403`). *Teste de integração com payload adulterado e com outro operador.*
- S3. Entrada validada no servidor: valor `> 0` e `≤ R$ 1.000.000.000,00`; vencimento futuro e em até 5 anos; tipo e moeda restritos aos valores conhecidos. *Teste de integração por regra.*
- S4. SQL sempre parametrizado (`#{}` no MyBatis; o build falha se encontrar `${`); `' OR 1=1--` no filtro do extrato retorna 0 linhas. Erro `500` devolve Problem Details genérico com `traceId`, sem stack trace ou SQL; o header `Authorization` nunca é logado. *Verificação no build + testes de integração.*
- S5. `criado_por` vem do usuário autenticado, nunca do payload; UPDATE/DELETE em `liquidacao` falha mesmo executado direto no banco. *Teste de integração (Testcontainers) via JDBC.*

**Desempenho**
- D1. Simulação: p95 ≤ 100 ms a 50 req/s por 60 s. *k6.*
- D2. Cotação + liquidação: p95 ≤ 300 ms a 20 req/s por 60 s, 0% de `5xx`. *k6.*
- D3. Extrato com 100 mil liquidações (período + cedente + moeda, página de 50): p95 ≤ 500 ms, com plano de execução usando índice. *k6 + `EXPLAIN ANALYZE` documentado.*
- D4. Paginação limitada: `size > 100` → `400`. *Teste de integração.*
- D5. O provedor de câmbio fica fora do caminho crítico; na atualização, timeout de 2 s com retry; provedor fora → `503` no "Atualizar taxa", e as cotações seguem com a taxa vigente enquanto tiver menos de 24h. *Teste de integração com o provedor mockado atrasado/fora.*

**Corretude**
- C1. Golden cases C1–C3 ao centavo; a liquidação usa a taxa congelada na cotação mesmo que uma nova taxa seja cadastrada depois. *Teste unitário parametrizado + teste de integração.*
- C2. Casos de borda: 45 dias (expoente 1,5), valor no limite do HALF_EVEN, VP nunca maior que o valor de face; nenhum `float`/`double` no domínio. *Testes unitários das strategies + regra ArchUnit.*
- C3. 10 liquidações simultâneas com o mesmo `cotacaoId` geram 1 registro; as demais recebem `200` com a mesma liquidação, sem `5xx`. *Teste de integração concorrente.*
- C4. Duas cotações do mesmo recebível liquidadas ao mesmo tempo → 1 sucesso e 1 `409`; falha no meio da liquidação desfaz tudo (rollback). *Testes de integração.*
- C5. Cotação expirada (120 s + 1 s) → `422` sem gravar nada; câmbio com mais de 24h → `422` na cotação. *Teste com `Clock` injetado.*

## 4. Perguntas ao negócio

1. O prazo deve contar dias corridos ou úteis? Vencimento em dia não útil é prorrogado?
2. Qual a referência da taxa base (CDI, custo de captação do fundo)? Quem define e com que frequência muda?
3. O spread deve variar por cedente ou sacado (rating), além do tipo de recebível?
4. Qual a fonte oficial de câmbio (PTAX, cotação da mesa)? 24h é uma janela de validade aceitável?
5. 2 minutos de validade da cotação atendem ao fluxo da mesa?
6. A mesa trabalha por operador ou por equipe? Um operador pode liquidar a cotação de outro?
7. Quais são os limites operacionais (valor máximo por recebível, prazo máximo)?
8. Títulos em USD ou outras moedas estão no horizonte?
9. Como um estorno é tratado hoje? (Assumido: nova operação compensatória, nunca alteração da liquidação.)
