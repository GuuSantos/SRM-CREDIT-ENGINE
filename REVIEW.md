# REVIEW.md — Code review do endpoint de liquidação (Anexo A)

> Revisão do Anexo A
>
> **Veredito: não aprovar.** O endpoint movimenta dinheiro e hoje pode pagar o valor errado, pagar duas vezes,
> registrar operação pela metade respondendo sucesso e expor o banco inteiro a SQL injection.
>
> As notas **"Neste repositório"** descrevem como a minha implementação (Java/Spring Boot/Next.js) trata cada ponto.

## Priorização

A ordem de importância é **impacto de negócio em produção**, não a facilidade da correção:

1. **Bloqueante/Crítico:** perda financeira, comprometimento do banco e sem dúvidas diversos registros inconsistentes já nos usos da aplicação, ou queda do serviço que qualquer cliente consegue provocar. Não pode ir para produção.
2. **Alta:** comprometer a auditoria, o controle de acesso ou a confiança no valor pago. Corrigir o mais rápido possível.
3. **Média:** erro em cenários específicos ou dificuldade de operar o sistema.
4. **Baixa:** design e manutenção. Não quebra hoje, mas torna o próximo bug mais provável.

Dentro dos bloqueantes, ponho a injeção primeiro porque o dano não tem limite: dá para apagar, alterar e forjar liquidações, inclusive escondendo os outros erros. O erro de fórmula vem logo atrás porque atinge **100% das operações** sem dar nenhum sinal. Os dois são bloqueantes, e a ordem entre eles é discutível. A duplicidade (item 4) é a mais cara por ocorrência, mas depende de retry ou concorrência para aparecer.

| # | Severidade | Problema | Impacto em uma frase |
|---|---|---|---|
| 1 | Bloqueante | SQL injection nas três queries | Qualquer cliente lê, altera ou apaga o banco, inclusive liquidando a carteira inteira |
| 2 | Bloqueante | Taxas em unidade errada na fórmula | Paga uma fração do valor justo (2,5% em 3 meses), que cai com o prazo |
| 3 | Bloqueante | Sem transação + erro engolido + `200 OK` | Liquidação "pela metade" respondida como sucesso |
| 4 | Bloqueante | Sem idempotência nem controle de concorrência | Duplo clique ou retry geram pagamento em dobro |
| 5 | Bloqueante | `number` (ponto flutuante) e `toFixed` para dinheiro | Centavos errados e arredondamento fora da política |
| 6 | Bloqueante | Exceções fora do `try` em handler `async` | Um ID inexistente derruba o processo, e o retry o mantém caindo |
| 7 | Alta | Câmbio sem registro, sem trava e sem validação | Não dá para auditar nem reproduzir o valor pago em USD |
| 8 | Alta | Sem autenticação/autorização visível | Qualquer um liquida qualquer título; a auditoria não tem o "quem" |
| 9 | Alta | Registro de auditoria incompleto e mutável | Não prova o que foi pago nem por quê, e nada impede alteração |
| 10 | Alta | Validação de entrada ausente | Moeda inventada gravada, tipo novo precificado errado, título já liquidado aceito |
| 11 | Média | Prazo (`term`) sem unidade e estático | Prazo errado conforme a data da liquidação |
| 12 | Média | Taxa base fixa no código | Mudança de taxa exige deploy e não deixa histórico |
| 13 | Média | Semântica HTTP e contrato de erro | O cliente não distingue sucesso, regra violada e falha |
| 14 | Baixa | Regra de negócio no controller, sem Strategy, `SELECT *` | Cada tipo novo mexe no endpoint de liquidação |
| 15 | Baixa | Sem logs, métricas ou testes | O erro engolido do item 3 nunca seria descoberto |

---

## Bloqueantes

### 1. SQL injection nas três queries

```ts
`SELECT * FROM receivables WHERE id = ${receivableId}`
`INSERT INTO settlements (receivable_id, amount, currency)
 VALUES (${receivableId}, ${finalAmount.toFixed(2)}, '${currency}')`
`UPDATE receivables SET status = 'SETTLED' WHERE id = ${receivableId}`
```

`receivableId` e `currency` vêm direto do `req.body` e são **colados no texto do SQL**. O usuário deixa de mandar um dado e passa a mandar código.

**Impacto:**

- `receivableId = "0 OR 1=1"`: o `UPDATE` vira `... WHERE id = 0 OR 1=1` e **marca a carteira inteira como `SETTLED`**. É o pior caso, porque bloqueia a operação real e mascara o estado de todos os títulos.
- `currency = "BRL'); DELETE FROM settlements; --"`: executa um segundo comando. Se funciona ou não depende de o driver aceitar múltiplos comandos, mas segurança não pode depender de configuração de driver.
- O `SELECT` vira canal de leitura de dados de cedentes (`UNION SELECT ...`), com implicação de sigilo e LGPD.

Qualquer cliente da API explora isso com um `curl`; não precisa de tela.

**Correção:**

- Queries **parametrizadas** sempre (`WHERE id = $1`, valores passados à parte).
- Validação do formato da entrada antes de chegar ao banco.
- Usuário de banco com privilégio mínimo.
- Prevenção: regra de lint ou SAST que falha o build com interpolação de string em SQL.

*Neste repositório:* `#{}` no MyBatis e JPA parametrizado. Um teste falha o build se aparecer `${` num mapper, e `' OR 1=1--` no filtro de cedente devolve `400`.

### 2. Taxas em unidade errada: a fórmula paga uma fração do valor

```ts
const BASE_RATE = 1.0; // taxa base mensal
const spread = receivable.type === "DUPLICATA" ? 1.5 : 2.5;
const presentValue =
  receivable.face_value / Math.pow(1 + BASE_RATE + spread, receivable.term);
```

O comentário diz "taxa base mensal" e a regra de negócio define 1% a.m. de taxa base e 1,5% a.m. de spread para duplicata. Assumindo essa intenção, as taxas foram escritas **em percentual** (`1.0`, `1.5`) mas usadas como **fração**. A fórmula calcula `(1 + 1 + 1,5)^n = 3,5^n` em vez de `(1 + 0,01 + 0,015)^n = 1,025^n`.

**Impacto:** no golden case C1 (R$ 100.000,00, 3 meses), o correto é **R$ 92.859,94**; este código calcula **R$ 2.332,36**. A proporção paga é `(1,025 / 3,5)^n`:

| Prazo | Paga, do valor justo |
|---|---|
| 1 mês | ~29% |
| 3 meses | ~2,5% |
| 6 meses | ~0,06% |

O erro atinge **todas** as operações sem exceção nem log: o sistema "funciona" e paga errado. Para o cedente é um prejuízo enorme; para o fundo, é risco jurídico e reputacional.

**Correção:**

- Taxas armazenadas e trafegadas como **fração** (`0.01`, `0.015`), com nome explícito (`taxaMensal`).
- Golden cases num teste automatizado, que teria pegado o erro no primeiro build.

*Neste repositório:* taxas em fração com `NUMERIC(19,8)` e teto de 20% a.m. na validação da tesouraria. A tela recebe percentual e converte para fração.

### 3. Sem transação, erro engolido e resposta de sucesso

```ts
try {
  await db.query(`INSERT INTO settlements ...`);
  await db.query(`UPDATE receivables SET status = 'SETTLED' ...`);
} catch (e) {
  // se falhar aqui, o insert já rodou, então segue o jogo
}
res.status(200).json({ ok: true, amount: finalAmount.toFixed(2) });
```

| Cenário | Estado final | O que o cliente recebe |
|---|---|---|
| `INSERT` falha | Nenhuma liquidação | `200 ok: true`. O cliente acredita que pagou. |
| `INSERT` ok, `UPDATE` falha | Liquidação gravada, título ainda "em aberto": **pode ser liquidado de novo** | `200 ok: true` |
| Ambos ok | Correto | `200 ok: true` |

**Impacto:** é exatamente a "liquidação pela metade" que ACID existe para impedir, somada a um `200` com erro. A segunda linha da tabela é outro caminho para a duplicidade do item 4. O `catch` vazio apaga a única evidência do problema.

**Correção:**

- `INSERT` e `UPDATE` na **mesma transação** (`BEGIN … COMMIT`, `ROLLBACK` em qualquer falha).
- Nunca engolir exceção: deixar propagar para um tratamento global que responda `5xx` ou `4xx` com corpo padronizado e registre log com identificador de rastreio.
- A resposta só sai depois do `COMMIT`.

*Neste repositório:* `TransactionTemplate` envolvendo INSERT + UPDATE. No conflito, a perdedora sofre rollback completo, e há teste que confirma que ela não deixa registro.

### 4. Sem idempotência nem controle de concorrência

Nada impede que a mesma liquidação seja processada duas vezes. O código não verifica se o título já está liquidado, não há chave de idempotência e não há restrição no banco. Em USD a janela de corrida é maior, porque existe uma chamada de rede (`fxService`) entre a leitura e a escrita.

**Impacto:** duplo clique, retry automático do cliente após timeout ou dois operadores ao mesmo tempo geram **duas linhas em `settlements` para o mesmo título**, ou seja, pagamento em dobro. Os itens 6 e 7 agravam isso: uma requisição que trava leva o cliente ao timeout, e o retry gera a duplicidade. Cada chamada isolada parece correta, por isso o defeito é difícil de notar.

**Correção:** defesa em camadas.

- **No banco:** no máximo uma liquidação por título, com `UNIQUE (receivable_id)` (ou índice único parcial, se houver cancelamento). É a única garantia que vale para qualquer caminho de código.
- **Idempotência:** uma chave que identifique a operação, com `UNIQUE`. A repetição devolve a liquidação existente (`200`) em vez de criar outra.
- **Concorrência:** transição de estado condicional dentro da transação (`UPDATE ... WHERE id = $1 AND status = 'PENDING'`, exigindo 1 linha afetada) ou optimistic locking com `version`. Quem perde recebe `409`.
- **Pagamento:** se outro processo emite o pagamento a partir de `settlements`, ele também precisa ser idempotente por título.

*Neste repositório:* a operação é identificada pela **cotação** (`UNIQUE (cotacao_id)`), papel que o `receivableId` faz no Anexo. A disputa entre cotações do mesmo título é resolvida por `@Version` no recebível. Testes: 10 chamadas simultâneas da mesma cotação geram 1 registro; duas cotações do mesmo título geram 1 sucesso + 1 `409`.

### 5. Ponto flutuante e arredondamento fora da política

`face_value / Math.pow(...)`, `presentValue / rate` e `toFixed(2)` usam `number` (ponto flutuante binário).

**Impacto:**

- Erros de representação: `0.1 + 0.2 = 0.30000000000000004`, e `(1.005).toFixed(2)` dá `"1.00"`. Em volume, centavos somem ou aparecem e a conciliação não fecha.
- `toFixed` arredonda empates **para cima** (`(0.125).toFixed(2)` dá `"0.13"`), mas a política exigida é **half-even** (`0.12`).
- Em cross-currency, converte o valor **não arredondado**, quando a regra é arredondar o BRL primeiro (golden case C3).
- Acima de 2^53, `number` perde até os inteiros.
- Drivers de Postgres costumam devolver `NUMERIC` como **string**. A divisão converte por acaso; um `+` concatenaria.

**Correção:**

- Tipo decimal de precisão arbitrária em todo o cálculo (`decimal.js` em TS; `BigDecimal` em Java), construído a partir de string.
- `HALF_EVEN` com 2 casas **só no resultado final**.
- `NUMERIC` no banco e valores trafegando como string no JSON.

*Neste repositório:* `BigDecimal` com `DECIMAL128`, potência fracionária com biblioteca de precisão arbitrária e golden cases ao centavo. No front, nenhum valor vira `number`.

### 6. Exceções fora do `try` em handler `async`

`receivable` pode ser `null` (ID inexistente), e então `receivable.type` lança `TypeError`. A chamada ao `fxService` também pode falhar. As duas estão **fora do `try`**.

**Impacto:** o Express 4 não captura rejeição de handler `async`. O resultado depende do ambiente:

- **Sem handler global de `unhandledRejection` (padrão do Node 15+):** o **processo cai**. Um único ID inexistente vira negação de serviço trivial.
- **Com handler global:** a requisição fica **pendurada** até o timeout do cliente, que faz retry e alimenta a duplicidade do item 4.

Por que é bloqueante e não só "indisponibilidade":

- **Loop de queda.** O orquestrador reinicia o processo, o cliente repete a mesma requisição por retry automático e o processo cai de novo. Com várias instâncias atrás de um balanceador, os retries espalham a mesma requisição e derrubam uma a uma. Uma única "requisição envenenada" mantém o serviço fora do ar.
- **Dano colateral.** Quando o processo cai, as liquidações **de outros usuários** em andamento morrem junto. Como não há transação (item 3), uma delas pode morrer entre o `INSERT` e o `UPDATE` e deixar uma liquidação pela metade, que ninguém pediu.

**Correção:**

- `404` explícito para recebível inexistente.
- Wrapper para handlers `async` (ou Express 5).
- Tratamento de erro central que responde com corpo padronizado, sem stack trace.

*Neste repositório:* toda exceção chega ao `TratadorGlobalDeErros` (Problem Details), e o `500` traz só `traceId`.

---

## Alta

### 7. Câmbio: sem registro, sem trava e sem validação

**Impacto:**

- A taxa usada **não é gravada** na liquidação: impossível auditar ou reproduzir o valor pago em USD.
- A taxa é buscada **no momento da liquidação**, não travada na cotação: o operador vê um valor e o sistema paga outro.
- Sem timeout: se o provedor travar, a requisição trava junto, e com ela o pool de conexões, em cascata.
- Sem validação: taxa `0`, `null` ou expirada vira `Infinity`/`NaN`. O `INSERT` falha, o erro é engolido (item 3) e a resposta é `200`.
- A convenção da taxa (BRL por USD ou o inverso) não está explícita. Invertida, o erro é de ~29× com câmbio de 5,43.

**Correção:**

- Congelar a taxa (valor + referência ao registro de vigência) na cotação e copiá-la para a liquidação.
- Rejeitar taxa inválida ou mais velha que a janela aceita (`422`).
- Provedor fora do caminho crítico, com timeout e retry na atualização.
- Nome que carrega a convenção (`brlPorUsd`).

*Neste repositório:* snapshot do câmbio na cotação e na liquidação, `422` para câmbio com mais de 24h, provedor com timeout de 2 s + retry, e `503` só na atualização da taxa.

### 8. Sem autenticação nem autorização visível

**Impacto:** pelo trecho revisado, qualquer cliente que alcance a API liquida qualquer título. Não existe segregação de funções (quem define a taxa ≠ quem liquida), e a auditoria não registra **quem** fez a operação. Classifico como Alta porque a autenticação pode existir numa camada que o trecho não mostra (gateway, middleware global). **Se não existir em nenhuma camada, sobe para bloqueante.**

**Correção:**

- Autenticação obrigatória e perfis: operador liquida, tesouraria mantém taxas.
- Usuário da liquidação obtido da sessão, nunca do corpo da requisição.

*Neste repositório:* HTTP Basic com perfis `OPERADOR`/`TESOURARIA`. Só o criador da cotação liquida, e `liquidado_por` vem do usuário autenticado.

### 9. Registro de auditoria incompleto e mutável

`settlements` guarda só `receivable_id`, `amount` e `currency`.

**Impacto:** não há como provar **por que** aquele valor foi pago (valor de face, taxa base, spread, prazo, câmbio), nem **quando** e **por quem**. Nada impede `UPDATE`/`DELETE` posterior, e o requisito é registro imutável.

**Correção:**

- A liquidação como snapshot autossuficiente: valores, taxas, câmbio e sua referência, prazo, usuário e timestamp.
- Bloquear alteração no próprio banco (trigger ou permissões), não só "não ter endpoint".
- Estorno é nova operação compensatória.

*Neste repositório:* snapshot completo e trigger que rejeita UPDATE/DELETE em `liquidacao`, testado via JDBC.

### 10. Validação de entrada ausente

**Impacto:**

- `currency = "EUR"`, `"usd"` ou ausente: não converte, mas grava `EUR`, `usd` ou `undefined` com o valor **em BRL**.
- Tipo desconhecido cai no `else` e é precificado silenciosamente com o spread do cheque.
- Título já liquidado ou vencido é liquidado normalmente.
- Fica a questão de negócio: a moeda de pagamento deveria vir do request ou do contrato com o cedente?

**Correção:**

- Validar presença e formato da entrada (`400`), existência do recurso (`404`) e estado do título (`409`/`422`).
- Lista fechada de moedas e tipos. Tipo sem regra é erro, nunca padrão silencioso.

---

## Média

### 11. Prazo `term` sem unidade e estático

`receivable.term` não diz se é dia ou mês, e é um número guardado no título.

**Impacto:** se for dia, a potência explode e o valor vai a zero. Se for fixo na originação, o prazo não diminui com o tempo: um título liquidado perto do vencimento sofre o mesmo deságio de um liquidado no início. Somado ao item 2, o erro se compõe.

**Correção:** guardar o **vencimento** e calcular o prazo na data da operação, com unidade e convenção explícitas.

*Neste repositório:* dias corridos ÷ 30, com a data de hoje em America/Sao_Paulo.

### 12. Taxa base fixa no código

**Impacto:** mudar a taxa exige deploy. Não há histórico de qual taxa valia quando, nem como provar a taxa de uma operação passada.

**Correção:** taxa como registro com vigência, mantida por perfil próprio e copiada para a cotação e a liquidação.

### 13. Semântica HTTP e contrato de erro

**Impacto:** um `POST` que cria recurso responde `200 { ok: true }` mesmo em falha, sem o `id` da liquidação. O cliente não distingue "criado", "já existia", "regra violada" e "erro do servidor", e por isso não sabe se pode repetir com segurança.

**Correção:**

- `201` na criação, com o `id`; `200` na repetição idempotente.
- `400`/`404`/`409`/`422` para entrada e regras; `5xx` para falhas.
- Corpo de erro padronizado (RFC 9457) com código estável.

---

## Baixa

### 14. Regra de negócio no controller

O ternário do spread, a fórmula e o câmbio estão no handler HTTP, com números mágicos e `SELECT *`.

**Impacto:** cada novo tipo de recebível altera o endpoint de liquidação, que é o que mais exige cuidado. A regra não é testável isoladamente, e o `SELECT *` quebra em silêncio quando o schema muda.

**Correção:** camadas (controller → serviço → persistência), padrão **Strategy** por tipo de recebível com o spread dentro da estratégia, e colunas explícitas na consulta.

### 15. Sem logs, métricas ou testes

**Impacto:** com o `catch` vazio, nenhuma falha deixa rastro. Sem teste, os itens 2 e 5 chegam à produção.

**Correção:** log estruturado com `traceId`, métrica de liquidações por resultado e os testes da seção abaixo.

---

## Testes que eu exigiria para aprovar

- [ ] **Golden cases:** C1, C2 e C3 ao centavo.
- [ ] **Idempotência:** a mesma operação 2× resulta em 1 registro e na mesma resposta.
- [ ] **Concorrência:** N requisições paralelas para o mesmo título resultam em exatamente 1 liquidação, e as demais recebem `409` ou `200` idempotente.
- [ ] **Atomicidade:** falha injetada no `UPDATE` deixa nenhuma liquidação gravada, o título inalterado e resposta `5xx`.
- [ ] **Câmbio:** taxa `0`, negativa, ausente ou vencida não grava nada e devolve erro.
- [ ] **Segurança:** payloads de injection devolvem `400`, sem efeito no banco.
- [ ] **Estados:** recebível inexistente → `404`; já liquidado → `409`; tipo desconhecido → erro.

---

## Prevenção: como evitar que um PR assim chegue à `main`

O problema não é a IA ter gerado o código, e sim o processo ter aceitado sem verificação.

**Antes do merge (a máquina pega antes do humano):**

1. **Golden cases no CI:** o build quebra se o motor não bater os valores de referência ao centavo. Isso pega os itens 2 e 5 no primeiro push.
2. **Regras automáticas:** lint ou SAST que proíbe interpolação de string em SQL, `catch` vazio e ponto flutuante em código monetário.
3. **Restrições no banco:** `UNIQUE` para idempotência, `CHECK` de domínio e trigger de imutabilidade. O banco recusa o que o código esquecer.
4. **Checklist de PR** só para código que move dinheiro: transação, idempotência, concorrência, decimal, auditoria, quem pode chamar.
5. **Código gerado por IA passa pelos mesmos portões** que o escrito à mão, e o autor do PR responde por ele.

**Em produção (detectar rápido o que escapou):**

6. **Alerta de negócio:** mais de uma liquidação por título, ou divergência entre total liquidado e total pago.
7. **Conciliação automática** entre liquidações, títulos e pagamentos.
8. **Feature flag** para fluxos financeiros novos, que permite desligar sem deploy.

**Se este código já estiver em produção** (o cenário do Anexo B): desligar o endpoint por flag ou revert até a correção, auditar os logs de acesso atrás de payloads de injection e conferir, no banco, títulos com mais de uma liquidação.
