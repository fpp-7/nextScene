# NextScene — Revisão Técnica Completa
**Data:** 2026-07-27 · **Escopo:** backend (Spring Boot 4), frontend (Expo/RN), recommendation-engine (FastAPI), infra (Docker Compose)

---

## Situação atual

O diagnóstico abaixo foi escrito antes das correções e está preservado como
registro. Todos os itens do plano da §6 foram implementados.

**Cobertura de testes: 111 testes, todos passando.**

| Projeto | Testes | Como rodar |
|---|---|---|
| Backend | 68 (integração com Postgres real via Testcontainers) | `cd backend && ./mvnw verify` |
| Engine | 31 (pytest) | `cd recommendation-engine && python -m pytest tests/ -q` |
| Frontend | 12 (jest) + checagem de tipos | `cd frontend && npm test && npm run typecheck` |

CI em `.github/workflows/ci.yml`, com um job dedicado a impedir que segredos
voltem a ser versionados.

### Validação de ponta a ponta

A pilha foi executada com `docker compose up` e um fluxo real percorrido:
cadastro → catálogo paginado → busca → onboarding com avaliações em lote →
watchlist → recomendações. 13 verificações, todas passando.

O contrato entre backend e motor foi confirmado nos logs dos dois lados: o
engine registrou `POST /api/v1/recommend/history 200 OK` interpretando
`4 avaliações (2 curtidas, 1 rejeitadas)`, e o backend não registrou nenhuma
queda para o fallback. O job do TMDB enriqueceu os primeiros lotes de filmes.

Subir a pilha revelou dois defeitos que nenhum teste pegava, ambos corrigidos:

- **`/actuator/health` não existia.** O endpoint estava configurado no
  `application.yml` e usado como healthcheck do compose, mas a dependência
  `spring-boot-starter-actuator` nunca foi declarada. O container ficava
  permanentemente "unhealthy".
- **Rota inexistente virava 500.** O handler genérico capturava
  `NoResourceFoundException` e a transformava em erro interno, escondendo URLs
  erradas — foi o que mascarou o problema acima.

### O que continua pendente

- **Rotacionar a chave do TMDB.** Ela está no histórico do Git. Nada no código resolve isso.
- **Re-treinar o modelo com os ratings do app.** O motor já recebe o histórico real
  (§3.1 resolvido), mas o SVD colaborativo só pontua usuários vistos no treino —
  hoje o sinal é content-based com penalização por rejeição.
- **Redis.** Cache e rate limiter são por instância; com mais de um nó do backend,
  o limite multiplica e cada nó tem seu próprio cache.
- **Testes de componente no app.** O `react-test-renderer` ainda não resolve com
  React 19 neste projeto; os testes cobrem serviços e cliente HTTP, não telas.
- **Endpoint de avaliação individual.** A tela de detalhes ainda busca todas as
  avaliações do usuário para encontrar uma.

---

## 0. Resumo honesto

O projeto tem uma arquitetura bem desenhada **no papel** (3 serviços, separação limpa de camadas, migrations, JWT, modelo híbrido CF+CB). Mas o que está no repositório hoje **não sobe** e, mesmo se subisse, a funcionalidade central — recomendação personalizada — **não funciona de verdade**: o motor de ML nunca vê os dados dos usuários reais do app.

São três problemas de natureza diferente, e vale não confundi-los:

1. **Bugs que impedem a aplicação de rodar** (§1) — 5 itens, todos com correção pequena.
2. **Um erro arquitetural central** (§3.1) — a ponte entre o app e o motor de ML não existe. Isso não é bug, é design faltando.
3. **Dívida de segurança/escala** (§2, §4) — aceitável para TCC, bloqueante para produção.

---

## 1. BLOQUEADORES — por que a API não funciona

### 1.1 🔴 Dois beans `corsConfigurationSource` → a aplicação não inicia

`SecurityConfig.java:41` e `CorsConfig.java:15` declaram **beans com o mesmo nome**. Desde o Spring Boot 2.1, `spring.main.allow-bean-definition-overriding` é `false` por padrão, e não há override no `application.yml`. Resultado: `BeanDefinitionOverrideException` no startup — o contexto nem sobe. Este é o suspeito nº 1 do "a API não responde".

**Correção:** apagar `CorsConfig.java` inteiro (o `SecurityConfig` já registra CORS), e mover a lista de origens do `CorsConfig` para dentro do `SecurityConfig` — ela é a versão correta (ver §2.3).

### 1.2 🔴 Frontend aponta para um túnel morto

`frontend/src/services/api.ts:3`:
```ts
const BASE_URL = 'https://slick-parrots-rule.loca.lt/api';
```
URL de `localtunnel`, efêmera por natureza — expira ao reiniciar o túnel. Hardcoded, sem fallback e sem configuração por ambiente. Mesmo com o backend no ar, o app fala com um host inexistente e todo erro vira `Network Error`.

**Correção:** `app.config.ts` + `process.env.EXPO_PUBLIC_API_URL`, com default `http://<IP-da-LAN>:8080/api` para dev.

### 1.3 🔴 `LazyInitializationException` em dois endpoints

`open-in-view: false` (correto) + `@ManyToOne(fetch = LAZY)` + método de serviço **sem `@Transactional`**:

- `WatchListService.getWatchlist():24` → acessa `wl.getMovie()` fora de transação → **500 em `GET /api/watchlist`**
- `RatingService.getMyRatings():55` → acessa `r.getMovie().getMovieId()` → **500 em `GET /api/ratings/me`**

Como `MovieDetailsScreen` chama `getMyRatings()` a cada abertura de filme, o erro aparece em toda a navegação (mascarado por um `catch` silencioso na linha 50).

**Correção:** `@Transactional(readOnly = true)` nos dois métodos, ou melhor, `@EntityGraph({"movie"})` / `JOIN FETCH` nas queries do repositório.

### 1.4 🔴 A imagem do engine não builda fora da sua máquina

`recommendation-engine/Dockerfile:16-17` copia `data/` e `models_saved/`, mas ambos estão no `.gitignore`:
```
data/
models_saved/*.joblib
```
Em qualquer clone limpo (CI, outro dev, servidor) o `docker compose build` falha. E como o backend tem `depends_on: recommendation-engine: condition: service_healthy`, **o backend nunca sobe junto**. Localmente funciona porque os arquivos existem no seu disco — o que esconde o problema.

**Correção:** publicar os `.joblib` via Git LFS ou baixá-los no `entrypoint`; e o dataset MovieLens deve ser baixado por script no build. Enquanto isso, trocar a dependência para `service_started` para o backend não ficar refém do engine.

### 1.5 🔴 `MovieImporter` apaga dados de usuário no startup

`MovieImporter.java:111-113`, dentro de um `CommandLineRunner`:
```java
jdbcTemplate.update("DELETE FROM watch_list");
jdbcTemplate.update("DELETE FROM rating");
jdbcTemplate.update("DELETE FROM movie");
```
Dispara sempre que `count <= 15` **ou** existir qualquer poster com "unsplash" — condição que o seed `V2__seed_movies.sql` cria. Ou seja: V2 insere 9 filmes com posters do Unsplash, o importer detecta e apaga **todas as avaliações e watchlists de todos os usuários**. Em produção com múltiplas instâncias, dois pods sobem e fazem isso concorrentemente.

**Correção:** remover o V2 (ele conflita com o importer), tornar a importação idempotente com `INSERT ... ON CONFLICT (movie_id) DO NOTHING`, e nunca deletar tabelas de usuário. Idealmente mover a carga do catálogo para fora do ciclo de vida da aplicação (job/migration).

---

## 2. SEGURANÇA

| # | Severidade | Problema |
|---|---|---|
| 2.1 | 🔴 Crítico | **Chave TMDB real commitada.** `.env` está versionado (`git ls-files` confirma) com `TMDB_API_KEY=033f0556…`, e a mesma chave está hardcoded como default em `application.yml:31`. Está no histórico do Git — **rotacione a chave**, remover o arquivo agora não resolve. |
| 2.2 | 🔴 Crítico | **Segredo JWT default e fraco.** `application.yml:27` traz `nextscene-dev-secret-…` como fallback. Se a env não for setada em produção, qualquer pessoa com acesso ao repo forja tokens de qualquer usuário. Falhe o boot se `JWT_SECRET` estiver ausente. |
| 2.3 | 🟠 Alto | **CORS permissivo + credenciais.** `SecurityConfig:43-46` combina `allowedOriginPatterns("*")` com `allowCredentials(true)` — qualquer site pode chamar a API autenticada. Use a lista restrita que já existe no `CorsConfig` (que é a correta) ou uma allowlist por env. |
| 2.4 | 🟠 Alto | **Engine sem autenticação nenhuma**, publicado em `0.0.0.0:8000` pelo compose. Qualquer um na rede consulta recomendações e a busca. Deve ficar em rede interna (remover o `ports:`) e/ou exigir um token de serviço. |
| 2.5 | 🟠 Alto | **Sem rate limiting em `/api/auth/login`.** Força bruta livre, sem lockout, sem captcha, sem backoff. E `@Size(min = 6)` é a única política de senha. |
| 2.6 | 🟠 Alto | **`/actuator/**` liberado** (`SecurityConfig:31`). Se o starter de actuator entrar com endpoints sensíveis expostos (`env`, `heapdump`, `configprops`), vaza segredo e memória. |
| 2.7 | 🟡 Médio | **Logout é no-op.** Token válido por 30 min continua funcionando após o logout — não há blocklist nem refresh token rotativo. |
| 2.8 | 🟡 Médio | **Enumeração de usuários.** `register` retorna "Email já cadastrado", permitindo mapear quem tem conta. O `login` acertou ao usar mensagem genérica. |
| 2.9 | 🟡 Médio | **`GlobalExceptionHandler:28-32` engole exceções sem logar.** Todo erro inesperado vira 500 mudo — depuração às cegas. E o engine faz o oposto (`main.py:92`, `detail=str(e)`), vazando stack interno ao cliente. |
| 2.10 | 🟡 Médio | **Semântica HTTP errada.** `IllegalArgumentException` → 400 para tudo: "Filme não encontrado" deveria ser 404, "Credenciais inválidas" deveria ser 401. O app não consegue distinguir sessão expirada de erro de request. |
| 2.11 | 🟢 Baixo | Sem verificação de e-mail, sem fluxo de recuperação de senha, sem TLS obrigatório (`http://` no CORS e no BASE_URL). |

---

## 3. ARQUITETURA

### 3.1 🔴 O motor de ML está desconectado dos usuários reais — o erro central

`RecommendationService.java:31-34`:
```java
var engineResponse = engineClient.getRecommendations(
        user.getInteractionCount() > 0 ? user.getInteractionCount() : 1, 20);
```

O `user_id` enviado ao engine é o **contador de interações** do usuário. As consequências, em ordem:

- O engine é treinado sobre usuários do **MovieLens**. O `interactionCount` não identifica ninguém — é um número que por acaso cai no range de IDs do dataset.
- Todos os usuários com o mesmo contador recebem **exatamente as mesmas recomendações**.
- Como as avaliações do onboarding nunca são persistidas (§3.2), `interactionCount` fica em 0 para quase todo mundo → **todos recebem as recomendações do usuário MovieLens nº 1**.
- Ninguém percebe, porque o `catch (Exception e)` da linha 44 cai silenciosamente no fallback "top 20 por rating" — e como o `rating` de todo o catálogo importado é `0.0` (`MovieImporter:129`), o fallback devolve uma ordem arbitrária.

Não existe caminho no código pelo qual uma avaliação feita no app influencie uma recomendação. **A funcionalidade que dá nome ao produto é decorativa.**

**O que falta desenhar:** as avaliações do Postgres precisam chegar ao engine. Duas opções viáveis:
- **(a) Engine stateless:** o backend envia o histórico do usuário no corpo da requisição; o engine pontua sob demanda. Simples, sem estado compartilhado, funciona já.
- **(b) Engine com estado:** um job periódico exporta `rating` → re-treina o SVD com IDs do app + MovieLens num espaço unificado. Mais correto a longo prazo, bem mais trabalhoso.

Para o escopo atual, (a) é a escolha certa.

### 3.2 🔴 As avaliações do onboarding são descartadas

`ratingService.ts:28` → `POST /recommendations/cold-start`, que **apenas retorna sugestões** e não grava nada. E o retorno é ignorado (`OnboardingColdStartScreen.tsx:53`). O usuário avalia 8 filmes, e nada disso existe depois. É o motivo direto de `interactionCount = 0`.

**Correção:** o onboarding deve fazer `POST /api/ratings` para cada avaliação (ou um endpoint em lote), e *depois* pedir as recomendações.

### 3.3 🟠 Identidade truncada: UUID → long

`UserResponse.java:19`, `RatingService.java:49`, `WatchListService.java:28`:
```java
user.getId().getMostSignificantBits() & Long.MAX_VALUE
```
As entidades usam UUID; os DTOs jogam metade dos bits fora para caber num `number` do TypeScript. Isso é **perda de identidade**: o ID retornado não pode ser usado para buscar o registro de volta, e há risco real de colisão. O `WatchlistItemResponse.id` e o `RatingResponse.id` são números sem significado nenhum.

**Correção:** serializar UUID como `string` e mudar `User.id: string` no frontend. O truncamento não resolve nenhum problema que `string` não resolva melhor.

### 3.4 🟠 Enriquecimento TMDB dentro do caminho de leitura

`MovieService.enrichMovieFromTmdb()` faz chamada HTTP externa **e grava no banco** (`linha 133`), sendo invocado por `getMovieById`, `getFeaturedMovie`, `findEntityByMovieId` (usado em rating e watchlist) e em loop pelo `RecommendationService`.

Problemas empilhados: um `GET` muta estado; o `save()` roda fora de transação; duas requisições simultâneas para o mesmo filme disparam duas chamadas TMDB e duas escritas concorrentes; e a latência do TMDB entra direto na latência da sua API (ver §4.2).

**Correção:** mover o enriquecimento para um job assíncrono/agendado, ou `@Async` com cache. O request do usuário nunca deveria esperar por uma API de terceiros.

### 3.5 🟡 Outros pontos de arquitetura

- **Dois clientes HTTP** sem padrão: Feign (engine) e `RestTemplate` (TMDB). Sem circuit breaker, sem retry, e o Feign está com **timeouts default** (10s conexão / 60s leitura) — um engine lento trava threads do Tomcat.
- **Sem versionamento de API** (`/api/...`, não `/api/v1/...`) — o engine, ironicamente, tem.
- **Sem OpenAPI/springdoc**: o contrato entre backend e app só existe na cabeça de quem escreveu.
- **Sem testes**: apenas o `contextLoads()` gerado (que, aliás, teria pegado o bug §1.1 se rodasse com o contexto real). Os diretórios `tests/` do engine estão vazios. Sem CI.
- **`RecommendationResponse` divide a lista ao meio** (`RecommendationService:39-41`) e chama uma metade de "Escolhas da IA" e a outra de "Usuários Similares". São a mesma lista cortada arbitrariamente — a UI afirma ao usuário algo que não é verdade.
- **Frontend com `USE_MOCK` e `mock-data.ts` ainda em produção** em 5 services — código morto que confunde e infla o bundle.

---

## 4. ESCALABILIDADE E PERFORMANCE

### 4.1 🔴 Engine: CPU-bound dentro de `async def`

`main.py:72` e `:115` declaram os endpoints como `async def` mas executam pandas/numpy/sklearn síncronos. No FastAPI, `async def` roda **no event loop** — cada recomendação bloqueia o servidor inteiro. Com uvicorn em worker único (`Dockerfile:21`), a capacidade real é **uma requisição por vez**.

**Correção:** trocar para `def` (o FastAPI move para o threadpool) e subir com `--workers N`. Ganho imediato, mudança de uma palavra.

### 4.2 🔴 Backend: até 60s por requisição de recomendação

`RecommendationService.mapEngineResults()` chama `enrichMovieFromTmdb()` em loop, sequencialmente, para até 20 filmes. Com `SimpleClientHttpRequestFactory` (2s connect + 3s read, **sem pool de conexões**), o pior caso é ~100s; o `timeout: 10000` do axios corta em 10s e o usuário vê "Erro ao carregar recomendações" sem entender por quê. Sem cache, isso se repete a cada refresh, e o rate limit do TMDB chega rápido.

**Correção:** cache (Redis ou `@Cacheable` com Caffeine) + enriquecimento assíncrono + `HttpComponentsClientHttpRequestFactory` com pool.

### 4.3 🟠 Queries que não escalam

- `findByGenresContainingIgnoreCase` e `findByTitleContainingIgnoreCase` geram `LIKE '%termo%'` — o índice B-tree `idx_movie_genres` é **inútil** para isso. Full scan em ~87k filmes a cada busca e a cada troca de gênero. Precisa de `pg_trgm` + índice GIN, ou normalizar gêneros numa tabela própria.
- `findTopByOrderByRatingDesc()` (filme em destaque) ordena sem índice em `rating`.
- **Sem paginação em lugar nenhum:** `PageRequest.of(0, 30)` fixo. O usuário nunca vê o 31º filme do catálogo. Não há "carregar mais".
- `JwtAuthFilter:46` faz `findById` no banco **a cada requisição**, e `AppUser.genresPreference` é `@ElementCollection(fetch = EAGER)` → 2 queries mínimas por request autenticada. O nome e o e-mail já estão no token; a busca no banco só é necessária para operações que precisam da entidade.

### 4.4 🟠 Engine: memória e custo por requisição

- `ContentBasedModel._similarity_cache` (`content_based.py:31`) é um `dict` **sem limite** num objeto que vive todo o ciclo do processo. Cada entrada é um vetor de tamanho `n_movies`. Crescimento monotônico → vazamento de memória.
- `hybrid.recommend()` faz `ratings[ratings["userId"] == user_id]` — varredura completa do DataFrame de ratings a cada requisição.
- Todo o dataset fica em RAM por worker; multiplicar workers multiplica a memória.
- `requirements.txt` inclui jupyter, matplotlib, seaborn, plotly e pytest — tudo isso entra na imagem de produção. Imagem provavelmente >2GB sem necessidade. Separe `requirements-dev.txt`.

### 4.5 🟡 Infra

- Nenhum healthcheck para o backend no compose; sem `depends_on` reverso, sem readiness.
- Sem limites de recursos (`mem_limit`, `cpus`) — o engine pode consumir a máquina.
- Sem tuning do pool HikariCP, sem `graceful shutdown`.
- Postgres sem backup nem política de retenção definida.

---

## 5. USABILIDADE E QUALIDADE PERCEBIDA

### 5.1 🔴 O catálogo aparece sem pôster e sem nota

Esta é, visualmente, a pior. `MovieService.getMovies()` (linha 49) e `searchMovies()` **não chamam** `enrichMovieFromTmdb()`. Como o `MovieImporter` grava `poster_url = "https://placehold.co/600x900/…?text=Titulo"` e `rating = 0.0` para os 87k filmes, a tela Descobrir — a primeira que o usuário vê — mostra uma grade de **retângulos cinza com texto e nota 0.0**. O onboarding usa a mesma lista, então a primeira impressão do app é um catálogo quebrado.

Só `getMovieById` e `getFeaturedMovie` enriquecem — ou seja, o filme só ganha cara depois de aberto.

**Correção:** pré-popular pôsteres em batch (job offline sobre o `tmdb_id`, que já está no banco), não sob demanda.

### 5.2 🔴 Sessão expira em 30 minutos, silenciosamente

`JWT_EXPIRATION=1800000` (30 min), sem refresh token, e o interceptor de 401 em `api.ts:34` está **literalmente vazio** — só um comentário. Depois de 30 minutos, o app continua "logado", todas as telas mostram "Erro ao carregar", e o usuário não tem como saber que precisa relogar. Alguns lugares (`MovieDetailsScreen:50`) engolem o erro sem mostrar nada.

**Correção:** interceptor que dispara logout + tela de login no 401, e refresh token para não expulsar o usuário a cada 30 min.

### 5.3 🟠 Filtro "Historia" nunca retorna nada

`MovieService.GENRE_TRANSLATION:38` mapeia `historia → History`, mas o MovieLens **não tem o gênero History**. O chip existe na UI, o usuário clica, recebe "Nenhum filme encontrado". Mesmo caso para gêneros do dataset que a UI nunca oferece: `Children`, `Film-Noir`, `IMAX`.

Além disso, `MovieResponse.genre` devolve a string inteira (`"Action, Adventure, Sci-Fi"`) e o `MovieDetailsScreen:163` renderiza tudo dentro de **um único chip**.

### 5.4 🟠 Busca com race condition

`DiscoverScreen.handleSearch():90` faz debounce mas **não cancela a requisição anterior**. Digitando rápido, a resposta de "mat" pode chegar depois da de "matrix" e sobrescrever o resultado. Também: o `error` não é limpo ao iniciar nova busca, então um erro antigo persiste na tela.

### 5.5 🟠 Listas sem virtualização

`DiscoverScreen:188` e `RecommendationsScreen:100` renderizam com `ScrollView` + `.map()`. Com 30 itens (e mais, quando houver paginação) todos os cards são montados de uma vez — travamento perceptível em aparelho modesto. `FlatList` resolve.

### 5.6 🟡 Ações que mentem para o usuário

- **Watchlist:** `WatchlistContext:85` faz rollback otimista mas **engole o erro**. O ícone volta ao normal sem nenhuma mensagem — o usuário acha que salvou.
- **Avaliação:** `MovieDetailsScreen:86` — `catch { /* handle error silently */ }`. Mesmo problema.
- **"Assistir Trailer"** abre uma **busca no YouTube**, não um trailer. A API do TMDB já retorna `videos` — dá para acertar.
- **Toggle de tema escuro** existe e não faz nada além de um alerta.
- **"Limpar Cache"** desloga o usuário e pede para "reiniciar o aplicativo" em vez de resetar o estado.
- **`ImageFallback`** existe, mas `OnboardingColdStartScreen:85` usa `<Image>` puro — pôster quebrado fica em branco.

### 5.7 🟡 Acessibilidade e responsividade

- Nenhum `accessibilityLabel` / `accessibilityRole` nos botões de ícone (voltar, bookmark, like/dislike) — inutilizável com leitor de tela.
- `Dimensions.get('window')` no escopo do módulo (`DiscoverScreen:19`, `MovieDetailsScreen:16`) é capturado uma vez: layout não reage a rotação, split-screen ou dobráveis.
- Contraste de `colors.mutedForeground` sobre `colors.background` merece verificação contra WCAG AA.
- Nenhum estado de "sem conexão"; o componente `EmptyState` existe mas o Discover usa `<Text>` cru.

---

## 6. Plano de ação — todos os itens concluídos

### Fase 1 — Fazer subir
1. ✅ `CorsConfig.java` apagado (§1.1). Causa confirmada rodando o teste com o arquivo restaurado: `BeanDefinitionOverrideException`. Coberto por `BackendApplicationTests`.
2. ✅ `@Transactional(readOnly = true)` + `@EntityGraph` em `getWatchlist` e `getMyRatings` (§1.3). Coberto por `WatchlistAndRatingsIntegrationTest`.
3. ✅ `BASE_URL` via `EXPO_PUBLIC_API_URL` (§1.2). Coberto por `api.test.ts`.
4. ✅ `DELETE FROM` removidos; inserção idempotente; migration V4 limpa o seed V2 (§1.5).
5. ✅ Interceptor 401 → logout, preservando a flag de onboarding (§5.2).

### Fase 2 — Fazer funcionar de verdade
6. ✅ `POST /api/ratings/batch` grava as avaliações do onboarding (§3.2).
7. ✅ `POST /api/v1/recommend/history` recebe o histórico do Postgres (§3.1).
8. ✅ `TmdbEnrichmentJob` agendado; TMDB saiu do caminho da requisição (§5.1, §3.4, §4.2).
9. ✅ `async def` → `def` + `WORKERS=2`; cache de similaridade com teto (§4.1, §4.4).

### Fase 3 — Segurança
10. ⚠️ `.env` destrackeado e `.gitignore` criado — **a chave ainda precisa ser rotacionada** (§2.1).
11. ✅ `JWT_SECRET` sem valor padrão (a aplicação não sobe sem ele); CORS restrito e sem credenciais (§2.2, §2.3).
12. ✅ Engine sem porta publicada; rate limit por IP no login (§2.4, §2.5). Actuator reduzido a `health,info` (§2.6).

### Fase 4 — Sustentar
13. ✅ UUID como string ponta a ponta (§3.3). Coberto por `AuthIntegrationTest`.
14. ✅ Paginação real + índices GIN com `pg_trgm` (§4.3). Coberto por `MoviesAndMigrationsIntegrationTest`.
15. ✅ Cache com Caffeine, timeouts no Feign, pool HTTP e circuit breaker com fallback (§4.2, §3.5).
16. ✅ 108 testes + CI em quatro jobs (§3.5).
17. ✅ `FlatList`, acessibilidade, erros visíveis, race condition na busca (§5.4, §5.5, §5.6, §5.7).

---

## 7. O que está bem feito

Vale registrar, porque não é pouco:

- Separação de camadas no backend é limpa e consistente (controller → service → repository → DTO).
- Migrations com Flyway e `ddl-auto: validate` — decisão madura, muita gente usa `update` e se arrepende.
- `Dockerfile` do backend é multi-stage e roda como usuário não-root.
- Senhas com BCrypt, `open-in-view: false`, sessão stateless — fundamentos certos.
- O `parseCsvLine` do `MovieImporter` trata aspas e vírgulas corretamente, incluindo aspas escapadas.
- O design do modelo híbrido com pesos adaptativos por estágio do usuário é uma boa ideia, bem documentada no código.
- O frontend tem uma identidade visual coerente e componentes de estado (`EmptyState`, `ErrorMessage`, `LoadingSpinner`, `ImageFallback`) — a estrutura para uma boa UX existe, falta ligá-la.
