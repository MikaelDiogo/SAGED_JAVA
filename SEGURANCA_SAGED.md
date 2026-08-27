# Segurança — Módulo SAGED (BCM Prefeitura de Crateús)

> Documento de requisitos de segurança para o módulo SAGED. Serve como guia
> para o Claude Code implementar as proteções necessárias, do básico ao
> avançado. Leia este arquivo inteiro antes de implementar qualquer controle.
>
> **Regra geral:** segurança no SAGED é responsabilidade do backend. O
> front-end apenas melhora a experiência (esconde botões, etc.), mas nunca
> substitui uma verificação de servidor. Toda decisão de acesso é tomada no
> backend, a partir do token JWT emitido pelo Keycloak.

---

## 0. Princípios que guiam tudo

1. **Defense in depth** — várias camadas independentes; se uma falha, outra
   segura.
2. **Least privilege** — cada usuário/role/serviço tem só o mínimo de acesso
   necessário.
3. **Fail secure** — na dúvida, negar. Erro de autorização = 403, nunca
   "deixa passar por garantia".
4. **Nunca confie na entrada** — todo dado externo (body, query, header,
   webhook) é hostil até ser validado.
5. **Nunca confie no front-end** — a UI esconder um botão não protege o
   endpoint por trás dele.
6. **Auditar tudo que é sensível** — quem fez, o quê, quando.
7. **LGPD por padrão** — dado pessoal é tratado com o menor escopo e a maior
   proteção possível.

---

## 1. Autenticação (quem é o usuário)

- Toda a autenticação vem do **Keycloak** via **JWT (OAuth2/OIDC)**. O SAGED
  é um **resource server** — ele só valida tokens, nunca cria login/senha
  próprio.
- Configurar o backend como resource server validando o token contra o
  issuer do realm `bcm-sdk`:
  ```yaml
  spring:
    security:
      oauth2:
        resourceserver:
          jwt:
            issuer-uri: http://localhost:8180/realms/bcm-sdk
  ```
- **Validar a assinatura do token** contra a chave pública do Keycloak
  (JWKS) — nunca aceitar `alg: none`, nunca decodificar sem verificar
  assinatura.
- **Validar claims obrigatórios** em todo request: `exp` (expiração), `iss`
  (issuer correto), `aud`/`azp` (destinatário esperado). Rejeitar token
  expirado, de issuer errado, ou sem assinatura válida.
- **Nunca** aceitar autenticação por usuário/senha em endpoints de negócio.
- **Nunca** logar o token completo, nem em nível DEBUG.

## 2. Autorização (o que o usuário pode fazer)

### 2.1 Autorização por role (RBAC)

- Roles do realm (`SAGED_ADMIN_GERAL`, `SAGED_ADMIN_SETOR`,
  `SAGED_TECNICO_LIDER`, `SAGED_TECNICO`) chegam no token em
  `realm_access.roles` e são convertidas para `ROLE_*` no Spring.
- **Todo endpoint de negócio** deve ter `@PreAuthorize` explícito. Nenhum
  endpoint fica "aberto por padrão".
  ```java
  @PreAuthorize("hasRole('SAGED_ADMIN_GERAL')")
  ```
- Endpoints administrativos (gerenciar técnicos, criar especialidade,
  autorizar Telegram) restritos às roles corretas conforme a matriz UAT.
- Configurar o `JwtAuthenticationConverter` para mapear `realm_access.roles`
  → `ROLE_*` (o Dev Host já faz isso; garantir que o módulo respeite o mesmo
  padrão).

### 2.2 Autorização por dado (multi-tenant / ABAC)

Isto é o ponto mais crítico e mais fácil de errar. Ter a role certa **não
basta** — o usuário só pode ver os dados da sua unidade/especialidade.

- **Row-level filtering obrigatório no backend.** As queries devem filtrar
  por `org_unit_id` (claim do token) e, para técnicos, por `specialty_codes`.
- Exemplos de regra:
  - `SAGED_ADMIN_GERAL` → sem filtro (vê tudo).
  - `SAGED_ADMIN_SETOR` → `WHERE department_id = :orgUnitId` (do token).
  - `SAGED_TECNICO_LIDER` → fila do setor (`department_id = :orgUnitId`).
  - `SAGED_TECNICO` → `WHERE specialty_id IN (:specialtyCodes)` do token.
- **Nunca** aceitar `department_id`/`specialty_id` vindo do body/query como
  fonte da verdade para *restringir* visão — sempre derivar do token. (O
  body pode informar destino de uma criação, mas a permissão para criar ali
  vem do token, não do body.)
- Cuidado com **IDOR** (Insecure Direct Object Reference): antes de retornar
  ou alterar `GET/PATCH /demands/{id}`, verificar que aquele `id` pertence à
  unidade/especialidade que o usuário tem direito de ver. Não basta o objeto
  existir — ele precisa ser *daquele* usuário.
- Escrever teste automatizado que confirme: usuário da unidade A recebe
  **403/404** ao tentar acessar demanda da unidade B.

### 2.3 Matriz de autorização (fonte da verdade)

| Ação | ADMIN_GERAL | ADMIN_SETOR | TECNICO_LIDER | TECNICO |
|---|---|---|---|---|
| Ver todas as secretarias | sim | não | não | não |
| Ver quadro da unidade | sim | sim | sim | só própria specialty |
| Criar demanda em outra unidade | sim | não | não | não |
| Ver relatórios de gestão | sim | sim | sim | não |
| Gerenciar técnicos | sim | não | não | não |
| Autorizar Telegram | sim | sim | não | não |

## 3. Validação e sanitização de entrada

- **Validar todo DTO de entrada** com Bean Validation (`@NotNull`,
  `@Size`, `@Pattern`, `@Email`, etc.) e `@Valid` nos controllers.
- **Whitelist, não blacklist** — aceitar só o formato esperado, rejeitar o
  resto.
- **Tamanho máximo** em todo campo de texto (title, description,
  justification) para evitar payloads gigantes.
- **Enums validados** — `status` só pode ser um dos 4 valores válidos
  (`A_FAZER`, `EM_ANDAMENTO`, `CONCLUIDO`, `INTERROMPIDO`); rejeitar
  qualquer outro.
- **Transições de status válidas** — a regra de negócio deve recusar
  transições ilegais (ex.: `CONCLUIDO` → `A_FAZER` se não permitido).
  `INTERROMPIDO` exige justificativa não-vazia.
- **Mass assignment / over-posting** — nunca fazer bind direto da entidade
  JPA a partir do request. Usar DTOs; o cliente nunca deve conseguir setar
  `id`, `protocol`, `createdBy`, `orgId`, `version`, `lifecycleStatus` ou
  qualquer campo de auditoria pelo body.
- **Paginação obrigatória** em `GET /demands` e listagens — limite máximo de
  page size (ex.: 100) para evitar dump de tabela inteira.

## 4. Proteção contra injeção

- **SQL Injection** — usar exclusivamente JPA/consultas parametrizadas ou
  `@Query` com parâmetros nomeados. **Nunca** concatenar string em query.
  Nada de SQL montado com `+ userInput +`.
- **Ordenação/filtros dinâmicos** — se aceitar `sort`/`filter` do cliente,
  validar contra uma whitelist de campos permitidos (não passar o valor cru
  para a query).
- **Log injection** — sanitizar/escapar dados de usuário antes de logar
  (remover quebras de linha) para evitar forjar entradas de log.
- **JSON deserialization** — não desabilitar proteções do Jackson; evitar
  desserialização polimórfica com tipos vindos do cliente.

## 5. Segurança de dados e LGPD

- **Classificação de sensibilidade** — a coluna `sensitivity`
  (`PUBLIC`/`INTERNAL`/`SENSITIVE`/`CONFIDENTIAL`) deve ser respeitada; dados
  `SENSITIVE`/`CONFIDENTIAL` exigem cuidado extra em log, resposta e
  exportação.
- **Minimização** — só coletar e retornar os dados pessoais estritamente
  necessários. Não retornar campos internos/sensíveis em endpoints que não
  precisam deles (usar DTOs de resposta enxutos, nunca serializar a entidade
  crua).
- **Soft delete** — nunca `DELETE` físico; usar `lifecycle_status = DELETED`.
  Registros "apagados" não devem aparecer em consultas normais.
- **Dados pessoais em logs** — proibido logar telefone, nome completo,
  identificadores pessoais em nível INFO. Mascarar quando necessário
  (ex.: `+55 85 ****-1234`).
- **Direito do titular (LGPD)** — prever, na modelagem, como localizar e
  exportar/anonimizar os dados de uma pessoa (por `requester_user_id`,
  `telegram` etc.) se solicitado.
- **Criptografia**:
  - Em trânsito: TLS obrigatório em produção (a Seplati cuida do TLS no
    gateway; o módulo não deve assumir HTTP em prod).
  - Em repouso: segredos e, se aplicável, campos altamente sensíveis
    protegidos; nunca guardar segredo em texto plano no banco/código.
- **Retenção** — histórico e auditoria mantidos; dados operacionais seguem
  a política de retenção acordada com a secretaria (documentar o prazo).

## 6. Segredos e configuração

- **Nenhum segredo no código nem no Git** — tokens (Telegram, client
  secret), senhas de banco, etc. só via variável de ambiente / secret
  manager.
- `.gitignore` cobrindo `.env`, arquivos de credencial, dumps.
- **Sem segredo de produção no repositório** — nem em teste, nem em exemplo,
  nem comentado.
- Variáveis esperadas documentadas na nota técnica (ex.:
  `SAGED_TELEGRAM_BOT_TOKEN`, `SAGED_TELEGRAM_WEBHOOK_SECRET`), com valores
  reais só no ambiente.
- **Rotação** — assumir que segredos podem ser rotacionados; não hardcodar
  em lugar nenhum.

## 7. Segurança do webhook do Telegram

Este é um ponto de entrada externo e não autenticado por JWT de usuário —
requer proteção específica.

- **Segredo do webhook** — validar o header secreto do Telegram
  (`X-Telegram-Bot-Api-Secret-Token`) contra `SAGED_TELEGRAM_WEBHOOK_SECRET`
  em toda requisição. Requisição sem o secret correto = **401/403**,
  descartada.
- **Comparação em tempo constante** ao checar o secret (evitar timing
  attack) — usar `MessageDigest.isEqual` em vez de `equals` de string.
- **Idempotência** — usar a tabela `bot_processed_messages` (constraint
  única em `provider + external_message_id`) para nunca processar a mesma
  mensagem duas vezes (Telegram reenvia em caso de timeout).
- **Autorização de remetente** — só processar mensagens de chats/telefones
  previamente autorizados (`telegram_demand_requesters` /
  `telegram_requester_authorizations`). Mensagem de origem desconhecida é
  ignorada.
- **Rate limiting** específico no webhook para evitar flood.
- **Validar o payload** do Telegram como qualquer entrada hostil (tamanho,
  formato, campos esperados).
- **Nunca** confiar em dados do payload para decidir permissão sem cruzar
  com a base de autorizados.

## 8. Proteções de transporte e cabeçalhos HTTP

- **CORS restritivo** — permitir só as origens conhecidas (o front do SAGED
  em dev: `http://localhost:*` conforme o client Keycloak; em prod, o
  domínio real). Nunca `Access-Control-Allow-Origin: *` em endpoints
  autenticados.
- **Security headers** (via Spring Security ou gateway):
  - `Strict-Transport-Security` (HSTS) em produção
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY` (ou CSP `frame-ancestors`)
  - `Content-Security-Policy` adequada
  - `Referrer-Policy: no-referrer`
- **Desabilitar** exposição de versão do servidor / stack no header/erro.
- **CSRF** — para API stateless com Bearer token, CSRF pode ser desabilitado
  com segurança (não há cookie de sessão); documentar essa decisão. Se algum
  fluxo usar cookie, aí CSRF volta a ser obrigatório.

## 9. Tratamento de erros e vazamento de informação

- **Respostas de erro no formato Problem Details (RFC 7807)**, sem stack
  trace, sem SQL, sem detalhe interno.
- **Mensagens genéricas** para o cliente; detalhe técnico só no log interno.
- **404 vs 403** — decidir política: para recursos que o usuário não pode
  ver, retornar `404` (não revela existência) costuma ser mais seguro que
  `403` em alguns casos. Ser consistente.
- **Nunca** ecoar de volta dados sensíveis num erro de validação.
- Sanitizar exceções para não vazar caminho de arquivo, versão de
  biblioteca, nome de tabela, etc.

## 10. Rate limiting e disponibilidade

- **Rate limiting** por usuário/IP em endpoints sensíveis (login-adjacente,
  criação de demanda, webhook).
- **Limites de payload** (tamanho máximo de body).
- **Timeouts** em chamadas externas (ex.: API do Telegram) para não
  travar threads.
- **Paginação** obrigatória (já citada) também é proteção contra
  exaustão de recursos.
- Proteção contra **enumeração** — não permitir varrer IDs sequenciais para
  mapear dados (UUIDs ajudam; combinar com checagem de ownership).

## 11. Auditoria e observabilidade de segurança

- **Colunas de auditoria** (`created_by`, `updated_by`, `created_at`,
  `updated_at`) preenchidas automaticamente a partir do JWT — nunca do body.
- **Trilha de auditoria de negócio** em `demand_history` (quem mudou status,
  quando, justificativa). Imutável — só insere, nunca edita/apaga.
- **Logar eventos de segurança**: falha de autenticação, 403, webhook
  rejeitado, transição de status inválida, tentativa de acesso cross-tenant.
- **X-Correlation-Id** propagado em todo fluxo para rastreabilidade
  ponta a ponta.
- Logs **sem dados pessoais/segredos**; com identificador de usuário
  (sub/username), não com o conteúdo sensível.
- Alertar (ou ao menos logar em nível WARN) padrões suspeitos: muitos 403 do
  mesmo usuário, muitos webhooks rejeitados.

## 12. Concorrência e integridade

- **Optimistic locking** — a coluna `version` (JPA `@Version`) deve impedir
  updates concorrentes que sobrescrevam dados; tratar
  `OptimisticLockException` com resposta `409 Conflict`.
- **Transações** — operações de negócio + gravação de outbox na **mesma
  transação** (garantia de consistência do padrão outbox).
- **Unicidade** — protocolo (`YYYY-DEP-TEC-NNNNN`) gerado com garantia de
  não-duplicação (constraint única + geração transacional/lock).

## 13. Dependências e cadeia de suprimentos

- **Manter dependências atualizadas** — sem versões com CVE conhecido.
- Rodar **verificação de vulnerabilidades** (ex.: OWASP Dependency-Check,
  `gradle dependencyCheckAnalyze`, ou equivalente) no build.
- Não introduzir dependência desnecessária; menos superfície de ataque.
- Fixar versões (evitar ranges abertos) para builds reproduzíveis.

## 14. Testes de segurança (obrigatórios na entrega)

- **Matriz de autorização** (JUnit 5 + `spring-security-test`): para cada
  endpoint, testar cada role — quem recebe `200` e quem recebe `403`.
  ```java
  mockMvc.perform(get("/api/v1/saged/demands/reports/x")
          .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SAGED_TECNICO"))))
      .andExpect(status().isForbidden());
  ```
- **Teste de isolamento de tenant** — usuário da unidade A não acessa dado
  da unidade B.
- **Teste de IDOR** — acesso direto a `id` de outro escopo é negado.
- **Teste de validação** — payload inválido/malicioso é rejeitado com erro
  tratado.
- **Teste do webhook** — sem secret → rejeitado; mensagem duplicada → não
  reprocessada; remetente não autorizado → ignorado.
- **Teste de mass assignment** — tentar setar `protocol`/`createdBy`/`id`
  pelo body não tem efeito.

## 15. Checklist de segurança para a entrega

- [ ] Todo endpoint de negócio tem `@PreAuthorize` explícito
- [ ] JWT validado (assinatura, issuer, expiração, audience)
- [ ] Filtragem por `org_unit_id` / `specialty_codes` aplicada nas queries
- [ ] Proteção contra IDOR (ownership check em acesso por id)
- [ ] DTOs de entrada e saída (sem expor entidade crua; sem mass assignment)
- [ ] Bean Validation em todos os inputs + limites de tamanho
- [ ] Enums e transições de status validados
- [ ] Paginação com limite máximo em todas as listagens
- [ ] Queries 100% parametrizadas (zero concatenação de SQL)
- [ ] Soft delete (`lifecycle_status`), sem DELETE físico
- [ ] Dados pessoais fora dos logs; segredos fora do código e do Git
- [ ] Webhook Telegram: secret validado (tempo constante) + idempotência +
      remetente autorizado + rate limit
- [ ] CORS restrito às origens conhecidas
- [ ] Security headers configurados
- [ ] Erros no formato Problem Details, sem vazar stack/SQL/interno
- [ ] Rate limiting nos pontos sensíveis
- [ ] Optimistic locking (`@Version`) tratando 409
- [ ] Outbox na mesma transação do negócio
- [ ] Auditoria (`created_by`/`updated_by` do token; `demand_history`
      imutável)
- [ ] `X-Correlation-Id` propagado e logado
- [ ] Dependências sem CVE conhecido (scan no build)
- [ ] Testes automatizados de autorização, isolamento de tenant, IDOR,
      validação e webhook passando

---

## 16. Instrução sugerida para o Claude Code

> Leia `SEGURANCA_SAGED.md` inteiro. Ele descreve os requisitos de segurança
> do módulo SAGED, do básico ao avançado. Antes de implementar, faça um
> diagnóstico do estado atual do código: para cada item do checklist da
> seção 15, diga se já está implementado, parcialmente, ou ausente. Depois
> proponha um plano de implementação priorizado (crítico → importante →
> desejável). Só então comece a implementar, começando pelos itens críticos:
> `@PreAuthorize` em todos os endpoints, validação de JWT, filtragem por
> tenant (`org_unit_id`/`specialty_codes`), proteção contra IDOR, e
> segurança do webhook do Telegram. Escreva testes automatizados para cada
> proteção implementada.
