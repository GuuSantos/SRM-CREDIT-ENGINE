# SPEC — SRM Credit Engine

Premissas, decisões de precisão e critérios de aceite definidos **antes** do código. Cada premissa é uma escolha consciente diante de uma ambiguidade do enunciado; as alternativas descartadas e cortes de escopo estão detalhados no `DECISIONS.md`.

## 1. Premissas

| Tema | Premissa adotada | Alternativa descartada |
|---|---|---|
| Prazo | Dias corridos entre a data da cotação e o vencimento ÷ 30, com expoente fracionário: `VP = Face / (1 + taxa base + spread)^(dias/30)`. Vencimento hoje ou no passado é rejeitado. | Meses inteiros (45 dias viraria 1 ou 2 meses); dias úteis/252 (exige calendário de feriados e conversão de taxa mensal → dia útil). |
| Taxa base | Cadastro com vigência (nova taxa = novo registro), mantido pela Tesouraria. Valor inicial 1,00% a.m. Armazenada como **fração** (`0.01`), nunca percentual; valor `≥ 0`. | Valor fixo em configuração; integração com CDI/Selic. |
| Spread | Fixo por tipo de recebível, definido em cada Strategy: Duplicata Mercantil 1,5% a.m., Cheque Pré-datado 2,5% a.m. | Spread por cedente/rating (sem modelo de risco no enunciado). |
| Moedas | Títulos sempre em BRL; pagamento em BRL ou USD. Câmbio expresso em **BRL por 1 USD**; `USD = BRL ÷ taxa`. Deságio sempre em BRL. | Títulos em outras moedas. |
| Câmbio: fonte | Cadastro com vigência. Alimentado por um provedor mockado, consultado por um job a cada 1h e pelo botão "Atualizar taxa" da Tesouraria; o cadastro manual é a contingência. **O provedor nunca é chamado na simulação, cotação ou liquidação** — elas só leem o cadastro. | Consultar o provedor na hora da cotação (colocaria uma dependência externa no caminho crítico). |
| Câmbio: qual taxa | A taxa vigente no momento da **cotação**. Taxa com mais de 24h é recusada nesse momento (verificação só na cotação). | Taxa da data da operação ou do vencimento (forward, inexistente hoje). |
| Recebível | O título (duplicata ou cheque) de um cedente, identificado pela chave natural **cedente + tipo + número do título** (`UNIQUE`). Entra pelo próprio painel: ao cotar, o recebível é cadastrado se ainda não existir. Status `PENDENTE` → `LIQUIDADO`. Um recebível tem N cotações e no máximo uma liquidação. | Cadastro prévio em tela separada; importação em lote (cortada, ver `DECISIONS.md`). |
| Fluxo | Simulação em tempo real (`POST /simulacoes`, não grava) → cotação (`POST /cotacoes`) travada por **2 minutos**, congelando taxas, câmbio, prazo e valores → liquidação (`PUT /cotacoes/{id}/liquidacao`, sem corpo) usando exatamente os valores cotados. Cotação expirada é recusada; a tela oferece cotar novamente. O **backend** decide a expiração; o front só exibe a contagem. | Liquidar recalculando na hora (o operador veria um valor e liquidaria outro). |
| Idempotência | Chave natural = id da cotação, garantida por `UNIQUE (cotacao_id)` na liquidação; o `PUT` comunica a idempotência pela própria semântica HTTP. 1ª chamada → `201`; repetição → `200` com a mesma liquidação. Duas cotações do mesmo recebível liquidadas em paralelo → optimistic locking (`version`) → `409`. | Header `Idempotency-Key` (redundante: a cotação já é a identidade da operação, e a proteção real é a UNIQUE). |
| Auditoria | A liquidação é o registro de auditoria: snapshot de valores, taxa base, spread, câmbio (valor e referência ao registro), prazo, usuário e timestamps. UPDATE/DELETE bloqueados por trigger no banco. | Tabela de auditoria separada (duplicaria dados); apenas "não ter endpoint" (frágil a SQL direto). |
| Acesso | HTTP Basic com dois papéis: `OPERADOR` (painel, cotação, liquidação, extrato) e `TESOURARIA` (taxas). Só quem criou a cotação pode liquidá-la. Ids de cotação são UUID. No front, a credencial fica só em memória (nunca `localStorage`), com logout e expiração por inatividade. Fora do ambiente local, exige HTTPS. | JWT ou sessão com cookie httpOnly (adequados à produção; custo fora do foco do desafio); grupos/mesas de operadores. |
| Datas | `timestamptz` em UTC no banco; exibição e cálculo de prazo pela data em America/Sao_Paulo. O extrato filtra pela data da liquidação: o período informado é convertido em intervalo UTC na aplicação (`>= início AND < fim`), preservando o uso do índice. | Converter o fuso dentro da consulta (`AT TIME ZONE` na coluna impede o índice). |
| Erros | Problem Details (RFC 9457): `400` validação · `401` sem credencial · `403` papel/dono · `404` inexistente · `409` já liquidado/concorrência · `422` cotação expirada ou câmbio vencido · `503` provedor indisponível (só no "Atualizar taxa"). Nunca `200` com erro. | Envelope de erro próprio. |

**Golden cases:** as premissas de aferição do enunciado (meses inteiros, 1% a.m., HALF_EVEN no final, BRL arredondado antes de converter) são compatíveis com as premissas acima: o motor recebe o prazo em meses (decimal), e 90/60 dias resultam exatamente em 3/2 meses.

### Liquidação: ordem de processamento

1. Autenticação e papel `OPERADOR`.
2. A cotação existe e pertence ao usuário (senão `404`/`403`).
3. **Já existe liquidação para esta cotação?** → `200` com ela. Vem antes da expiração: um retry aos 2:01 de uma liquidação feita aos 1:59 devolve a liquidação, não `422`.
4. A cotação expirou? → `422`.
5. Em uma transação: **INSERT da liquidação primeiro** (a `UNIQUE (cotacao_id)` serializa chamadas da mesma cotação) e depois `UPDATE recebivel ... WHERE version = ?` (cotações diferentes do mesmo recebível → `409`).
6. Violação da UNIQUE no passo 5 → a liquidação existente é lida em **nova transação** (no PostgreSQL a transação original fica abortada após o erro) e devolvida com `200`.

## 2. Precisão numérica

- **Aplicação:** `BigDecimal` em todo o domínio, construído a partir de `String` (nunca de `double`). Cálculos intermediários com `MathContext.DECIMAL128` (34 dígitos significativos).
- **Potência:** `(1+i)^n` é calculada separando o expoente: a parte inteira com `BigDecimal.pow(int)` (exata) e só a parte fracionária com biblioteca de precisão arbitrária (`big-math`). Golden cases (prazo inteiro) nunca passam por `exp/ln`. `Math.pow` é proibido (usa `double`).
- **Arredondamento:** `HALF_EVEN`, 2 casas, **apenas no resultado final**. Em cross-currency, o valor presente em BRL é arredondado primeiro e então convertido e arredondado em USD — o BRL gravado é exatamente o que gerou o USD.
- **Banco (PostgreSQL):** valores monetários `NUMERIC(19,2)`; taxas e câmbio `NUMERIC(19,8)`.
- **API e front:** valores e taxas trafegam no JSON como **string** (`"92859.94"`), porque `number` em JavaScript é ponto flutuante binário. O front só formata para exibição; **todo cálculo, inclusive o da simulação, é do backend**.
- **Deságio:** `Face − VP em BRL` (ambos já com 2 casas), em BRL.

## 3. Critérios de aceite

Números de desempenho são **linha de base local** (`docker compose` em máquina de desenvolvimento), medidos com um script k6 versionado; o resultado medido é registrado no README.

**Usabilidade**
- U1. Simulação sem botão "calcular": a API é chamada após 300 ms sem digitação (debounce). *Teste de componente com timers falsos; latência coberta por D1.*
- U2. Erro de validação aparece junto ao campo e desabilita "Cotar"; erros da API exibem o `detail` em português. *Teste de componente.*
- U3. Contagem regressiva da cotação visível; ao expirar, "Liquidar" é bloqueado e aparece "Cotar novamente". *Teste de componente com timers falsos; a recusa real é do backend (C5).*
- U4. "Liquidar" desabilita no primeiro clique. *Teste de componente; a unicidade da liquidação é garantida no backend (C3).*
- U5. Valores formatados por moeda em pt-BR; a tela mostra o câmbio usado e sua vigência; com câmbio vencido, o painel avisa ao abrir: "câmbio desatualizado, acione a Tesouraria". *Teste de componente com o golden case C3.*

**Segurança**
- S1. Sem credencial → `401`; `OPERADOR` em cadastro de taxa → `403`; `TESOURARIA` → `201`; só `/actuator/health` é público. Senhas com BCrypt, nenhuma no repositório, `{noop}` proibido. *Teste de integração por endpoint × papel.*
- S2. Só o usuário que criou a cotação pode liquidá-la (outro operador → `403`). *Teste de integração com dois operadores.*
- S3. Entrada validada no servidor: valor `> 0` e `≤ R$ 1.000.000.000,00`; vencimento futuro e em até 5 anos; tipo e moeda restritos aos valores conhecidos; taxas `≥ 0`. *Teste de integração por regra.*
- S4. SQL sempre parametrizado (`#{}` no MyBatis; o build falha se encontrar `${`); a ordenação do extrato usa lista fixa de colunas (`<choose>`); `' OR 1=1--` no filtro de cedente → `400`. Erro `500` devolve Problem Details genérico com `traceId`, sem stack trace ou SQL; o header `Authorization` nunca é logado. *Verificação no build + testes de integração.*
- S5. `criado_por` vem do usuário autenticado, nunca do payload; UPDATE/DELETE em `liquidacao` falha mesmo executado direto no banco. *Teste de integração (Testcontainers) via JDBC.*

**Desempenho**
- D1. Simulação: p95 ≤ 100 ms a 50 req/s por 60 s. *k6.*
- D2. Cotação + liquidação: p95 ≤ 300 ms a 20 req/s por 60 s, 0% de `5xx`. *k6.*
- D3. Extrato com 100 mil liquidações (período + cedente + moeda, página de 50): p95 ≤ 500 ms, com plano de execução usando índice. *k6 + `EXPLAIN ANALYZE` documentado.*
- D4. Paginação limitada: `size > 100` → `400`. *Teste de integração.*
- D5. O provedor de câmbio fica fora do caminho crítico; na atualização, timeout de 2 s com retry; provedor fora → `503` no "Atualizar taxa", e as cotações seguem com a taxa vigente enquanto tiver menos de 24h. *Teste de integração com o provedor mockado atrasado/fora.*

**Corretude**
- C1. Golden cases C1–C3 ao centavo; a liquidação usa a taxa congelada na cotação mesmo que uma nova taxa seja cadastrada depois. *Teste unitário parametrizado + teste de integração.*
- C2. Casos de borda: 45 dias (expoente 1,5), valor no limite do HALF_EVEN, VP nunca maior que o valor de face. Nenhum `float`/`double` no domínio, nem `Math.pow`, `new BigDecimal(double)`, `BigDecimal.valueOf(double)` ou `Double.parseDouble`. *Testes unitários das strategies + regra ArchUnit.*
- C3. 10 liquidações simultâneas da mesma cotação geram 1 registro; as demais recebem `200` com a mesma liquidação, sem `409` nem `5xx`. *Teste de integração concorrente.*
- C4. Duas cotações do mesmo recebível liquidadas ao mesmo tempo → 1 sucesso e 1 `409`; falha no meio da liquidação desfaz tudo (rollback). *Testes de integração.*
- C5. Cotação expirada (120 s + 1 s) e não liquidada → `422` sem gravar nada; cotação já liquidada, repetida após expirar → `200`; câmbio com mais de 24h → `422` na cotação. *Teste com `Clock` injetado.*

**Observabilidade**
- O1. Log estruturado em JSON com `traceId` em toda requisição; métricas expostas: contador de liquidações (por moeda e resultado) e histograma de latência do motor de precificação. *Teste que executa uma liquidação e verifica as métricas e os campos do log.*

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
