# 🎬 NextScene — Mobile App

Aplicativo mobile de recomendação de filmes construído com **React Native** e **Expo**.

O NextScene utiliza inteligência artificial para recomendar filmes personalizados com base nos gostos do usuário.

---

## 📱 Telas

| Tela | Descrição |
|------|-----------|
| **Login** | Autenticação com email e senha |
| **Cadastro** | Criação de nova conta |
| **Onboarding — Gêneros** | Seleção de gêneros favoritos (curtir/excluir) |
| **Onboarding — Avaliação** | Avaliação inicial de filmes para calibrar recomendações |
| **Descobrir** | Feed principal com destaque, busca e filtro por gênero |
| **Para Você** | Recomendações personalizadas pela IA |
| **Watchlist** | Filmes salvos para assistir depois |
| **Perfil** | Estatísticas do usuário e configurações |
| **Detalhes do Filme** | Sinopse, elenco, avaliações e ações |

---

## 🛠️ Tech Stack

- **Framework:** [React Native](https://reactnative.dev/) com [Expo](https://expo.dev/)
- **Navegação:** [React Navigation](https://reactnavigation.org/) (Stack + Bottom Tabs)
- **Ícones:** [Lucide React Native](https://lucide.dev/)
- **Linguagem:** TypeScript

---

## 🚀 Como Rodar

### Pré-requisitos

- [Node.js](https://nodejs.org/) (v18+)
- [Expo Go](https://expo.dev/go) instalado no celular (para teste físico)
- Ou um emulador Android/iOS configurado

### Instalação

```bash
npm install
```

Configure para onde o app aponta:

```bash
cp .env.example .env
```

`EXPO_PUBLIC_API_URL` precisa ser alcançável **pelo aparelho**. No navegador,
`http://localhost:8080/api` funciona; no celular, `localhost` significa o próprio
celular — use o IP da sua máquina na LAN.

### Executar

```bash
npm start
```

- **📱 Celular físico:** escaneie o QR code com o Expo Go
- **🤖 Android:** pressione `a` (requer emulador)
- **🍎 iOS:** pressione `i` (requer macOS + simulador)
- **🌐 Navegador:** `npm run web`

O backend precisa estar no ar — veja o [README da raiz](../README.md).

### Testes

```bash
npm test && npm run typecheck
```

Cobrem os serviços e o cliente HTTP. Ainda não há testes de componente: o
`react-test-renderer` não resolve com React 19 neste projeto.

---

## 📂 Estrutura do Projeto

```
src/
├── components/         # Componentes reutilizáveis
│   ├── MovieCard.tsx       # Card de filme
│   ├── ImageFallback.tsx   # Imagem com placeholder
│   ├── EmptyState.tsx
│   ├── ErrorMessage.tsx
│   └── Loading*.tsx
├── contexts/           # Estado global
│   ├── AuthContext.tsx     # Sessão, login, logout por expiração
│   └── WatchlistContext.tsx
├── data/
│   └── genres.ts           # Gêneros oferecidos na interface
├── navigation/
│   ├── AppNavigator.tsx    # Stack + Bottom Tabs
│   └── types.ts
├── screens/            # Uma por tela do app
├── services/           # Camada de API
│   ├── api.ts              # Cliente axios, token, interceptor de 401
│   ├── storage.ts          # SecureStore em nativo, localStorage na web
│   ├── errors.ts           # Erros HTTP em linguagem de usuário
│   └── *Service.ts         # Um por domínio
├── theme/
│   └── colors.ts
└── utils/
    ├── layout.ts           # Colunas e largura de card responsivas
    └── validation.ts
```

---

## 🎨 Design System

O app utiliza um tema **dark** com a seguinte paleta:

| Token | Cor | Uso |
|-------|-----|-----|
| `primary` | `#d4a017` (Dourado) | Ações principais, destaques |
| `background` | `#0a0a0a` | Fundo do app |
| `secondary` | `#1e1e1e` | Cards, inputs, chips |
| `mutedForeground` | `#888888` | Textos secundários |
| `destructive` | `#d4183d` | Ações destrutivas |

---

## 🔗 Integração com Backend

O app consome a API real — não há mais dados mock. Pontos que valem conhecer:

**Sessão.** O token JWT fica no `SecureStore` em nativo e no `localStorage` na
web (`services/storage.ts`). Na web ele **não fica criptografado** — é o que a
plataforma permite sem um backend de sessão por cookie.

**Expiração.** O token dura 30 minutos. Quando a API responde 401, o interceptor
em `services/api.ts` encerra a sessão e devolve o usuário ao login, preservando
a flag de onboarding para não repeti-lo.

**Erros.** `services/errors.ts` traduz status HTTP e falhas de rede em mensagens
de usuário, em vez de exibir "Request failed with status code 401".

---

## 📄 Licença

Este projeto é privado e faz parte do ecossistema **NextScene**.
