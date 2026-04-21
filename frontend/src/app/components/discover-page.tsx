import { useState } from "react";
import { Search, SlidersHorizontal } from "lucide-react";
import { MOVIES } from "./mock-data";
import { MovieCard } from "./movie-card";
import { BottomNav } from "./bottom-nav";
import { ImageWithFallback } from "./figma/ImageWithFallback";

const FILTER_GENRES = ["Todos", "Acao", "Drama", "Suspense", "Ficcao Cientifica", "Terror"];

export function DiscoverPage() {
  const [activeGenre, setActiveGenre] = useState("Todos");
  const featured = MOVIES[1];
  const filtered = activeGenre === "Todos" ? MOVIES : MOVIES.filter((m) => m.genre === activeGenre);

  return (
    <div className="min-h-screen bg-background pb-24">
      {/* Featured */}
      <div className="relative h-72">
        <ImageWithFallback src={featured.poster} alt={featured.title} className="w-full h-full object-cover" />
        <div className="absolute inset-0 bg-gradient-to-t from-background via-background/40 to-transparent" />
        <div className="absolute bottom-6 left-6 right-6">
          <span className="text-primary text-xs bg-primary/20 px-2 py-1 rounded-full">{featured.genre}</span>
          <h2 className="text-white mt-2">{featured.title}</h2>
          <p className="text-muted-foreground text-sm mt-1">IMDb {featured.imdb} · {featured.year}</p>
        </div>
      </div>

      {/* Search */}
      <div className="px-6 mt-4">
        <div className="flex items-center gap-3 bg-secondary rounded-xl px-4 py-3">
          <Search className="w-5 h-5 text-muted-foreground" />
          <input placeholder="Buscar filmes..." className="bg-transparent flex-1 text-white placeholder:text-muted-foreground outline-none text-sm" />
          <SlidersHorizontal className="w-5 h-5 text-muted-foreground" />
        </div>
      </div>

      {/* Genre filter */}
      <div className="flex gap-2 px-6 mt-4 overflow-x-auto no-scrollbar">
        {FILTER_GENRES.map((g) => (
          <button
            key={g}
            onClick={() => setActiveGenre(g)}
            className={`px-4 py-2 rounded-full text-sm whitespace-nowrap cursor-pointer transition-all ${
              activeGenre === g ? "bg-primary text-primary-foreground" : "bg-secondary text-muted-foreground"
            }`}
          >
            {g}
          </button>
        ))}
      </div>

      {/* Section */}
      <div className="px-6 mt-6">
        <h3 className="text-white mb-3">Aclamados pela Critica</h3>
        <div className="grid grid-cols-2 gap-4">
          {filtered.map((movie) => (
            <MovieCard key={movie.id} movie={movie} />
          ))}
        </div>
      </div>

      <BottomNav />
    </div>
  );
}
