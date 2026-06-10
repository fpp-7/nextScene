import { Movie } from '../types';

export const GENRES = [
  "Acao", "Aventura", "Animacao", "Comedia", "Crime", "Documentario",
  "Drama", "Fantasia", "Ficcao Cientifica", "Guerra", "Historia",
  "Horror", "Misterio", "Musical", "Romance", "Suspense", "Terror", "Western"
];

export const MOVIES: Movie[] = [
  { id: 1, title: "O Poderoso Chefao", year: 2022, genre: "Crime", rating: 9.2, imdb: 9.2, poster: "https://images.unsplash.com/photo-1580741569354-08feedd159f9?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", synopsis: "Um imperio do crime, uma familia dividida. A saga epica dos Corleone continua.", cast: ["Marlon Brando", "Al Pacino", "James Caan"] },
  { id: 2, title: "Interestelar", year: 2014, genre: "Ficcao Cientifica", rating: 8.7, imdb: 8.7, poster: "https://images.unsplash.com/photo-1653045474061-075ba29db54f?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", synopsis: "Uma jornada atraves do espaco-tempo para salvar a humanidade da extincao.", cast: ["Matthew McConaughey", "Anne Hathaway", "Jessica Chastain"] },
  { id: 3, title: "Clube da Luta", year: 1999, genre: "Drama", rating: 8.8, imdb: 8.8, poster: "https://images.unsplash.com/photo-1762115445557-967c1504ffe9?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", synopsis: "Um homem insatisfeito encontra libertacao atraves da violencia e do caos.", cast: ["Brad Pitt", "Edward Norton", "Helena Bonham Carter"] },
  { id: 4, title: "A Origem", year: 2010, genre: "Suspense", rating: 8.8, imdb: 8.8, poster: "https://images.unsplash.com/photo-1550556808-ec6ada57caff?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", synopsis: "Um ladrao especializado em extrair segredos do subconsciente durante o sonho.", cast: ["Leonardo DiCaprio", "Joseph Gordon-Levitt", "Elliot Page"] },
  { id: 5, title: "Matrix", year: 1999, genre: "Acao", rating: 8.7, imdb: 8.7, poster: "https://images.unsplash.com/photo-1653849532026-745a05f0b882?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", synopsis: "A realidade e uma simulacao. Neo precisa decidir entre a pilula azul e a vermelha.", cast: ["Keanu Reeves", "Laurence Fishburne", "Carrie-Anne Moss"] },
  { id: 6, title: "O Iluminado", year: 1980, genre: "Terror", rating: 8.4, imdb: 8.4, poster: "https://images.unsplash.com/photo-1760577315790-3a0ed3f42496?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", synopsis: "Um escritor e sua familia ficam isolados em um hotel assombrado durante o inverno.", cast: ["Jack Nicholson", "Shelley Duvall", "Danny Lloyd"] },
  { id: 7, title: "Orgulho e Preconceito", year: 2005, genre: "Romance", rating: 7.8, imdb: 7.8, poster: "https://images.unsplash.com/photo-1627964464837-6328f5931576?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", synopsis: "Uma historia de amor atemporal entre Elizabeth Bennet e Mr. Darcy.", cast: ["Keira Knightley", "Matthew Macfadyen", "Judi Dench"] },
  { id: 8, title: "Indiana Jones", year: 1981, genre: "Aventura", rating: 8.4, imdb: 8.4, poster: "https://images.unsplash.com/photo-1758523957586-0e9592d880f9?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", synopsis: "Um arqueólogo aventureiro busca reliquias antigas pelo mundo.", cast: ["Harrison Ford", "Karen Allen", "Paul Freeman"] },
  { id: 9, title: "Planeta Terra", year: 2006, genre: "Documentario", rating: 9.4, imdb: 9.4, poster: "https://images.unsplash.com/photo-1759521528494-fd6ceb6049e3?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", synopsis: "Uma jornada visual impressionante pela diversidade natural do nosso planeta.", cast: ["David Attenborough"] },
];
