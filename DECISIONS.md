# DECISIONS.md — O que cortei, simplifiquei e por quê

As premissas de negócio (prazo, taxa, câmbio, cotação travada, idempotência) estão no `SPEC.md`. Este documento registra **o que deliberadamente ficou de fora ou mais simples**, por que, e o que eu faria numa próxima etapa. Também lista onde a entrega diverge do que o próprio `SPEC.md` prometeu.

## 1. Nível-alvo

Mirei **Pleno completo** mais os itens de **Sênior** com melhor relação custo/benefício:

| Item de Sênior | Situação |
|---|---|
| Optimistic locking com teste do conflito | ✅ `@Version` no recebível; duas cotações do mesmo título → 1 sucesso + 1 `409` |
| Observabilidade (log estruturado + métricas de negócio) | ✅ `traceId` por requisição, log JSON (ECS), contador de liquidações e tempo do motor com p95 |
| Resiliência na integração de câmbio | ✅ timeout de 2 s + 3 tentativas; provedor fora do caminho crítico |
| CI rodando testes e linter | ✅ GitHub Actions: backend (todos os testes) e frontend (lint, Vitest, build) |
| Diagrama C4 | ❌ cortado (ver 3.6); o README tem o diagrama ER |

## 2. Decisões de design que não são óbvias

| Decisão | Por quê | Alternativa descartada |
|---|---|---|
| **JPA nas escritas, MyBatis só no extrato** | Escrita com regra precisa de `@Version`, entidade com comportamento e validação de schema; o extrato é leitura analítica com filtros opcionais, onde quero o SQL explícito para conferir o plano de execução. MyBatis também é tecnologia da vaga: eu não tinha usado antes e quis aprender aplicando-a no lugar em que ela faz sentido | Só JPA (filtro opcional vira `:x is null or ...`); só MyBatis (perderia o lock otimista pronto) |
| **Tipo de recebível como enum + `CHECK`**, sem tabela | A regra de cada tipo mora no código (Strategy): tipo novo exige classe de qualquer forma, e uma tabela com spread duplicaria a fonte da verdade | Tabela `tipo_recebivel` — passa a valer se os spreads precisarem mudar sem deploy e com histórico |
| **Sem `UNIQUE (recebivel_id)` na liquidação** | Uma unique por recebível pegaria a disputa entre cotações antes do optimistic lock, que é o mecanismo pedido e testado. A proteção continua dupla: status `PENDENTE` + `version`. (No `REVIEW.md` eu recomendo a unique para o Anexo A, porque lá não existe `version`.) | Evolução: acrescentar a unique como terceira barreira e tratar a violação vinda de outra cotação como `409` |
| **Cheque identificado pelo CMC7** | O número do cheque é do banco do emitente, não do cedente: dois cheques de bancos diferentes podem ter o mesmo número. O CMC7 (banco + agência + conta + número) é único | Chave cedente + tipo + número simples |
| **Só o criador da cotação liquida** | Evita um operador efetivar o preço que outro cotou. Como a cotação expira em 2 min e o título segue `PENDENTE`, nada fica preso | Liquidação por equipe/mesa (virou pergunta ao negócio nº 6) |
| **DTOs como `record` com fábrica `de(...)`**, sem MapStruct | Poucos DTOs, mapeamento trivial; menos uma dependência e um annotation processor | MapStruct (previsto no planejamento inicial) |
| **Sem Lombok** | Records cobrem os DTOs; entidades são poucas | Lombok |
| **Spring Boot 4.0.9, não 4.1.x** | O starter do MyBatis ainda não suporta a 4.1 | Última versão |
| **Next.js usado só no navegador** | A credencial existe só em memória no navegador e todo cálculo é do backend; renderização no servidor não traria ganho | SSR / Server Components |

## 3. Cortes deliberados

### 3.1 Negócio

- **Dias úteis e feriados (base 252).** Exige calendário de feriados mantido até o maior vencimento, convenção de conversão da taxa mensal para dia útil e regra de prorrogação de vencimento em dia não útil. Um ano de feriados faltando vira preço errado silencioso. Fiquei com dias corridos ÷ 30 (pergunta ao negócio nº 1).
- **Taxa base de mercado (CDI/Selic) e câmbio oficial (PTAX).** Dependências externas e conversão de taxa anual para mensal. O provedor de câmbio é um mock atrás da interface `ProvedorCambio`; a PTAX entraria como outra implementação, sem mudar o resto do sistema.
- **Câmbio a termo (forward) para a data de vencimento.** É a taxa negociada hoje para uma data futura, que embute a diferença de juros entre real e dólar no período. Exigiria uma fonte de mercado (contratos a termo/futuro de dólar) e uma decisão de negócio sobre quem assume o risco cambial até o vencimento. Usei a taxa vigente no momento da cotação.
- **Cadastro e importação de recebíveis em lote.** O painel cadastra o título na primeira cotação. Importação por arquivo seria a evolução natural para a mesa, com a cotação continuando individual.
- **Estorno.** Assumido como nova operação compensatória (pergunta ao negócio nº 9); não implementado. A liquidação é imutável por trigger.
- **Cessão parcial e pagamento parcelado.** Um título é cedido inteiro, uma vez; parcelas de duplicata são títulos distintos.

### 3.2 Segurança

- **HTTP Basic em vez de JWT/OIDC.** Suficiente para demonstrar segregação de funções (`OPERADOR` × `TESOURARIA`) e testável com `curl`. Limitações aceitas:
  - a senha trafega a cada requisição (exige HTTPS fora do ambiente local);
  - não há sessão revogável no servidor;
  - a credencial fica só em memória no front, então recarregar a página pede login de novo.

  Em produção: OIDC ou BFF com cookie `httpOnly`.
- **Usuários em memória**, com senhas por variável de ambiente e sem valor padrão. Não há cadastro de usuários no escopo.
- **Swagger público.** O `SPEC.md` diz que só `/actuator/health` seria público; deixei também a documentação OpenAPI aberta para o avaliador explorar a API. Os endpoints em si continuam exigindo login.

### 3.3 Resiliência

- **Timeout e retry escritos à mão, sem Resilience4j e sem circuit breaker.** O objetivo do desafio é mostrar o comportamento, não a biblioteca. Com o provedor fora do caminho crítico (ele só alimenta o cadastro de câmbio), um circuit breaker traria pouco.
- **Limitação conhecida:** depois do timeout, a tarefa do `supplyAsync` continua executando no `ForkJoinPool` comum, porque uma chamada bloqueante não é cancelável. Aceitável no mock; com um provedor HTTP real, o timeout seria do próprio cliente HTTP (connect/read timeout, que aborta o socket) e o executor seria dedicado.

### 3.4 Extrato

- **Paginação por `OFFSET`.** Atende às primeiras páginas. Para páginas profundas, a evolução é keyset (`WHERE liquidado_em < :ultimo`).
- **Totais por moeda** no extrato e **histórico de vigências** na tela de taxas: sem endpoint, ficaram de fora.

### 3.5 Desempenho

- **Medição com k6 e massa de 100 mil liquidações (critérios D1–D3 do `SPEC.md`) não executadas.** Priorizei observabilidade e CI, que dão garantia contínua, sobre uma medição pontual. O que existe hoje: índice composto `(cedente_id, moeda_pagamento, liquidado_em DESC)` desenhado para o filtro completo do extrato e o timer `precificacao.motor` com p95. A evolução é um script de massa (`generate_series`), o `EXPLAIN ANALYZE` documentado e o script k6 versionado.

### 3.6 Documentação e testes

- **Diagrama C4.** O README tem o diagrama ER e a descrição das camadas, suficientes para um monólito com front separado.
- **Testes de componente do front.** O `SPEC.md` previa testes de componente com timers falsos (U1–U5). Em vez de renderizar componentes, extraí a lógica que pode errar em silêncio (conversão de valores, máscara, percentual → fração, formatação sem ponto flutuante, contagem regressiva, soma de dias) para funções puras e testei essas. O comportamento de tela foi verificado manualmente, inclusive os três golden cases ponta a ponta (prints em `docs/img/`).
- **Testes end-to-end (Playwright).** Cortados pelo custo de configuração.
- **ArchUnit para proibir `double` no domínio.** Cortado; a garantia vem dos golden cases e dos testes de borda no CI.
- **Anexo B (post-mortem do incidente).** É exercício de Staff/Tech Lead. O `REVIEW.md` só indica a contenção imediata, caso o código já esteja em produção.

### 3.7 Operação

- **Painéis de métricas e logs (Prometheus/Grafana, Loki/ELK).** O sistema **expõe** as métricas em `/actuator/metrics` e o log sai em JSON; coletar e visualizar é papel da plataforma. As métricas ficam na mesma porta da API, atrás do perfil `OPERADOR`; em produção iriam para uma porta de gestão separada.
- **Massa de dados para demonstração.** O banco sobe só com os 3 cedentes e a taxa base; o câmbio vem do job na subida.

## 4. Onde a entrega diverge do `SPEC.md`

| `SPEC.md` | Entregue | Motivo |
|---|---|---|
| U2: erro de validação junto ao campo | Mensagem única com a lista de campos (`campos` do Problem Details) | Custo de front; a API já devolve o campo, então a evolução é só de tela |
| U5: mostrar a vigência do câmbio no painel e avisar ao abrir | O painel mostra a taxa usada e avisa na simulação quando o câmbio tem mais de 24h; a vigência aparece na tela de taxas | Simplificação de tela |
| D1–D3: medição com k6 | Não medido | Ver 3.5 |
| C2: regra ArchUnit | Não implementada | Ver 3.6 |
| U1–U4: testes de componente | Testes das funções puras | Ver 3.6 |
| S1: só `/actuator/health` público | Health e Swagger públicos | Ver 3.2 |

## 5. Limitações conhecidas

- A contagem regressiva usa o relógio do navegador, que pode divergir do servidor. Ela é informativa; quem decide a expiração é o backend (`422 COTACAO_EXPIRADA`, tratado na tela).
- O scanner de dependências da IDE aponta CVE em biblioteca **transitiva** do Spring Boot 4.0.9. Não forcei versões às cegas; em produção, Dependabot/Renovate e atualização de patch antes do deploy.
- `POSTGRES_PASSWORD` só vale na criação do volume do banco; trocá-la depois exige recriar o volume (`docker compose down -v`).
- O mock de câmbio devolve um valor fixo (`5.4321`, o câmbio do golden case C3), configurável por variável de ambiente.

## 6. Próximos passos, em ordem

1. Massa de 100 mil liquidações, `EXPLAIN ANALYZE` do extrato e script k6 (fecha D1–D3).
2. `UNIQUE (recebivel_id)` como terceira barreira contra liquidação duplicada.
3. OIDC no lugar do HTTP Basic, com sessão revogável.
4. PTAX do Banco Central como implementação real de `ProvedorCambio`.
5. Importação de recebíveis em lote.
6. Dias úteis, quando o negócio confirmar a convenção.
