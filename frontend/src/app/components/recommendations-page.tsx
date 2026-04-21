import { Sparkles, RefreshCw } from "lucide-react";
import { MOVIES } from "./mock-data";
import { MovieCard } from "./movie-card";
import { BottomNav } from "./bottom-nav";

export function RecommendationsPage() {
  const aiPicks = [MOVIES[0], MOVIES[3], MOVIES[4], MOVIES[2]];
  const similar = [MOVIES[5], MOVIES[7], MOVIES[6], MOVIES[8]];

  return (
    <div className="min-h-screen bg-background pb-24 pt-12 px-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-primary" />
          <h1 className="text-white">Para Voce</h1>
        </div>
        <button className="p-2 bg-secondary rounded-full cursor-pointer">
          <RefreshCw className="w-4 h-4 text-muted-foreground" />
        </button>
      </div>

      {/* Match info */}
      <div className="bg-gradient-to-r from-primary/10 to-primary/5 border border-primary/20 rounded-xl p-4 mb-6">
        <p className="text-primary text-sm">Baseado no seu perfil</p>
        <p className="text-muted-foreground text-xs mt-1">
          Usuarios com gostos parecidos adoraram estes filmes
        </p>
      </div>

      {/* AI picks */}
      <div className="mb-6">
        <h3 className="text-white mb-3">IA Recomenda</h3>
        <div className="grid grid-cols-2 gap-4">
          {aiPicks.map((movie) => (
            <MovieCard key={movie.id} movie={movie} />
          ))}
        </div>
      </div>

      {/* Similar users */}
      <div>
        <h3 className="text-white mb-3">Usuarios Parecidos Curtiram</h3>
        <div className="grid grid-cols-2 gap-4">
          {similar.map((movie) => (
            <MovieCard key={movie.id} movie={movie} />
          ))}
        </div>
      </div>

      <BottomNav />
    </div>
  );
}
