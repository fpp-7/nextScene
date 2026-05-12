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
# Instalar dependências
npm install

# Iniciar o servidor de desenvolvimento
npx expo start
```

### Executar no dispositivo

- **📱 Celular físico:** Escaneie o QR code com o Expo Go
- **🤖 Android:** Pressione `a` no terminal (requer emulador)
- **🍎 iOS:** Pressione `i` no terminal (requer macOS + simulador)

---

## 📂 Estrutura do Projeto

```
src/
├── components/         # Componentes reutilizáveis
│   └── MovieCard.tsx       # Card de filme para grids
├── data/               # Dados e tipos
│   └── mock-data.ts        # Dados mock de filmes e gêneros
├── navigation/         # Configuração de navegação
│   ├── AppNavigator.tsx    # Stack + Bottom Tabs
│   └── types.ts            # Tipos de rotas
├── screens/            # Telas do app
│   ├── LoginScreen.tsx
│   ├── RegisterScreen.tsx
│   ├── OnboardingGenresScreen.tsx
│   ├── OnboardingColdStartScreen.tsx
│   ├── DiscoverScreen.tsx
│   ├── RecommendationsScreen.tsx
│   ├── WatchlistScreen.tsx
│   ├── ProfileScreen.tsx
│   └── MovieDetailsScreen.tsx
└── theme/              # Design system
    └── colors.ts           # Paleta de cores
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

Atualmente o app utiliza dados mock. Para conectar com o backend:

1. Substituir os imports de `mock-data.ts` por chamadas à API
2. Configurar o base URL da API em um arquivo de configuração
3. Implementar autenticação real com JWT/tokens

---

## 📄 Licença

Este projeto é privado e faz parte do ecossistema **NextScene**.
