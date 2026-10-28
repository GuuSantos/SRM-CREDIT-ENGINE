# AI_USAGE.md — Como usei IA neste desafio

## 1. Como trabalhei

Usei o Claude no chat, sem agente com acesso ao repositório, para explicar conceitos, propor opções com os prós e contras de cada uma, escrever o código que eu digitava ou colava e revisar o que eu escrevi. Para revisar os documentos mais importantes (o SPEC, os critérios de aceite e o code review), cruzei as respostas com mais duas IAs: um segundo chat do Claude (Opus, esforço alto, sem o contexto da conversa principal) e o DeepSeek.

Algumas regras que segui do começo ao fim:

- **Spec antes de código.** O `SPEC.md` foi escrito, revisado por outras IAs e mesclado antes da primeira linha de backend. Ele virou o contrato que eu usava para conferir o que a IA propunha depois.
- **A IA não tinha acesso ao repositório.** Usei o chat, e não um agente, de propósito: queria ver cada linha antes de ela entrar no código. Eu digitava o núcleo (motor de precificação, strategies, taxas, cotação e liquidação) e colava o repetitivo (configuração, SQL, testes e boa parte do front).
- **Revisão às cegas.** Para cruzar com outra IA, eu mandava só o enunciado e as regras, **sem** a resposta da primeira. Depois pedia a comparação com postura crítica. Mostrar a resposta antes faz a segunda IA concordar com ela.
- **Nada entra sem verificação:** compilador, testes, golden cases, execução real no navegador ou conta conferida por mim no console.
- **Diário de decisões.** Mantive um registro cronológico de decisões, erros da IA e sugestões rejeitadas. Este documento foi montado a partir dele.

## 2. Specs e prompts estratégicos

**O próprio `SPEC.md`** foi o principal "prompt": premissas, precisão numérica e critérios de aceite fechados antes do código. Cada etapa da implementação começava conferindo o que o SPEC dizia.

**Critérios de aceite comparados entre três fontes.** O mesmo pedido foi para três modelos, sem mostrar a lista dos outros:

> Com base no enunciado e nas decisões já tomadas (abaixo), proponha critérios de aceite para usabilidade, segurança e desempenho. No máximo 5 por categoria. Para cada critério, diga **como ele seria verificado** (qual teste ou medição).

**Code review do Anexo A às cegas.** O mesmo pedido foi para as três IAs, cada uma sem ver a resposta das outras:

> Você é revisor sênior de um sistema financeiro. Revise o PR abaixo. [regras de negócio: fórmula, taxas, half-even, arredondar o BRL antes de converter, ACID, idempotência, auditoria imutável]. Entregue os problemas em ordem de severidade, com impacto em produção e correção, explique o critério de priorização e como evitar que um PR assim chegue à `main`.

As três chegaram a conclusões parecidas. Levei as diferenças para o Claude da conversa principal, que tinha todo o histórico do projeto, para consolidar uma versão; cada ajuste só entrou depois da minha aprovação, e algumas severidades eu mudei por conta própria (ver a seção 4).

**Modo de trabalho**, combinado com o Claude no início: *"Eu mesmo quero ver o que você está gerando, escrever, entender e prosseguir com as aprovações."* Por isso a IA explicava cada peça antes de eu digitá-la, e eu perguntava o que não entendia.

## 3. Casos concretos em que a IA errou, e como o processo detectou

| # | O erro | Quem errou | Como detectei |
|---|---|---|---|
| 1 | **Ordem errada na liquidação.** No primeiro SPEC, um retry às 2:01 de uma liquidação feita às 1:59 devolveria `422 expirada` em vez de `200` com a liquidação existente. A idempotência quebrava justamente no retry | Claude | **Revisão cruzada às cegas** do SPEC, antes de existir código |
| 2 | **Dois critérios contraditórios.** Com o `UPDATE` do recebível antes do `INSERT` da liquidação, 10 chamadas simultâneas da mesma cotação poderiam receber `409`, mas outro critério exigia `200`. A correção foi inverter a ordem e ler a liquidação vencedora numa nova transação (no PostgreSQL a transação fica abortada depois do erro) | Claude | **Revisão cruzada às cegas.** Depois, os testes de concorrência confirmaram o desenho corrigido |
| 3 | **Comportamento de uma versão antiga do framework.** O endpoint de perfil do usuário devolvia `FACTOR_PASSWORD` como se fosse um perfil: o Spring Security 7 passou a incluir uma *authority* de fator de autenticação, e o código assumia o comportamento da versão 6 | Claude | **Teste de contrato** que verificava o conteúdo da resposta, não só o status `200` |
| 4 | **Configuração de CORS ignorada.** O bean tinha um nome diferente do que o Spring Security procura, e o navegador bloqueava o front. Os testes passavam porque o MockMvc não é um navegador | Claude | **Uso real no navegador** (console mostrando o bloqueio do *preflight*). Acrescentei um teste que simula o *preflight* para não voltar |
| 5 | **`${}` num comentário do mapper.** O SPEC exige que o build falhe se aparecer `${` num mapper MyBatis; a IA escreveu um comentário "só `#{}`, nunca `${}`" e o teste falhou no primeiro uso | Claude | **Guarda automatizada** que o próprio SPEC pedia |
| 6 | **Ponto flutuante numa conta de apoio.** Ao calcular valores de exemplo, a IA passou um número como `float` para um tipo decimal e o deságio saiu `4690.42999999999417…` em vez de `4.690,43` | Claude | **Leitura da saída.** É o mesmo motivo de o projeto construir `BigDecimal` sempre a partir de `String` |
| 7 | **Código citado com o comentário alterado no review.** A IA "melhorou" o comentário do Anexo A (`// taxa base mensal` virou `// "1% a.m."`), e generalizou "paga ~2,5% do valor" para todos os prazos, quando isso só vale para 3 meses | Claude | **Revisão cruzada às cegas** do REVIEW.md |
| 8 | **Recomendações que contradiziam o enunciado.** Usar prazo em meses inteiros "para aferição" (o enunciado diz que as premissas dos golden cases são independentes do SPEC); dizer que `DECIMAL128` poderia errar na 2ª casa decimal (com 34 dígitos significativos o erro fica muito abaixo do centavo); assumir Spring Boot 3 | DeepSeek | **Leitura cruzada com o enunciado** e com a conta de ordem de grandeza |
| 9 | **Erros que o compilador e o Hibernate pegaram:** entidade sem `@Id`, asserção do AssertJ que não compila, previsão errada da escala de um `BigDecimal` vindo do cache do Hibernate | Claude | **Compilador, `ddl-auto=validate` e testes** |

Um caso que **não** classifiquei como alucinação: duas IAs colocaram o provedor de câmbio no caminho da cotação. O rascunho de decisões que eu enviei para elas analisarem (a base do `SPEC.md`) dizia que o câmbio vinha de um provedor mockado, mas não dizia **quando** ele era chamado, e as duas preencheram a lacuna com uma premissa plausível. A falha estava no meu documento. A correção foi deixar explícito no `SPEC.md` que o provedor só é chamado na atualização da taxa. Isso virou um argumento de resiliência: se o provedor cai no meio de uma liquidação, nada acontece, porque ele não está no caminho crítico.

## 4. Sugestões que rejeitei

- **Stack TypeScript/NestJS** (DeepSeek): genérica, ignorava a vaga (Java, MyBatis).
- **Operador digitar a taxa na operação:** risco de erro e de fraude; quem opera não define o preço que usa.
- **Tabela de auditoria separada:** duplicaria dados que a liquidação já guarda como snapshot imutável.
- **Severidade proposta pela IA para "exceção derruba o processo"** (Alta): questionei se não havia risco de loop. Há: o retry do cliente derruba o processo de novo a cada reinício, e a queda mata liquidações de outros usuários no meio. Subiu para bloqueante.

## 5. O que não deleguei, e por quê

- **As premissas de negócio:** dias corridos ÷ 30, taxa base com vigência, cotação travada por 2 minutos ("não faz sentido simular um valor e efetivar outro"), só o criador liquida, auditoria diferente de log, rota `PUT /cotacoes/{id}/liquidacao`. A IA trazia as opções; a escolha e a defesa são minhas.
- **As perguntas de negócio que mudaram o modelo.** Eu sabia que o mesmo número de duplicata pode existir em cedentes diferentes, e perguntei como o modelo estava tratando isso. A chave cedente + tipo + número resolvia a duplicata, mas a discussão mostrou que no cheque o número é do banco do emitente, não do cedente, e pode se repetir. O cheque passou a ser identificado pelo CMC7.
- **O núcleo cobrado na defesa:** digitei e entendi o motor, as strategies, as taxas, a cotação e a liquidação com idempotência e concorrência.
- **A priorização do escopo.** Já no fim do dia, em vez de entregar do jeito que estava, revisei o `SPEC.md` atrás de critérios ainda não atendidos e implementei os que cabiam: observabilidade e CI. O restante virou corte justificado no `DECISIONS.md`.
- **A revisão antes de cada commit**, os testes rodando antes de cada push e a decisão de parar a revisão cruzada quando ela deixou de trazer ganho.

O que tive **mais apoio da IA**, e digo com transparência: o MyBatis do extrato, o front (Next.js), o CI, a observabilidade e os testes automatizados. MyBatis, CI e observabilidade eu não conhecia antes deste desafio; front e testes eu já tinha visto em projetos, mas nunca tinha usado de forma ativa. Nessas partes, colei a maior parte do código e verifiquei rodando: os testes do extrato (filtros, fuso e paginação), o CI verde no primeiro PR, os golden cases reproduzidos ponta a ponta pela tela (prints em `docs/img/`) e cada tela testada no navegador.

## 6. Reflexão

A IA facilita muito entender o que está sendo feito, como e por quê, e enxergar todas as opções. Mas, conforme o projeto cresceu, entender o processo deixou de ser opcional: sem isso eu não conseguiria escolher entre as opções nem perceber quando uma resposta estava errada.

Tenho consciência de que a IA tem grande parte da responsabilidade por eu ter chegado a itens de nível sênior. Eu não sabia o que era CI nem observabilidade, e nunca tinha usado MyBatis; escolhi usá-lo porque ele está na descrição da vaga e quis aprender aplicando. Para entender o domínio, pesquisei bastante com outras IAs, desde o que é um spread até como funcionam métricas de negócio. Muita coisa tem paralelo com o meu trabalho com ERP, mas hoje não programo dessa forma no dia a dia.

Mesmo assim, saio com a sensação de ter trabalhado num nível pleno de verdade, em arquitetura, documentação, testes e padronização de versionamento, e com clareza de que preciso continuar estudando e praticando diariamente.

**O que eu levo:**

- Revisão às cegas acha erro de verdade; revisão com a resposta à vista só confirma.
- Quase todos os erros da IA foram pegos por algo que **eu tinha pedido antes**: um teste de contrato, uma guarda no build, um golden case, uma segunda opinião. A verificação precisa existir antes do erro.
