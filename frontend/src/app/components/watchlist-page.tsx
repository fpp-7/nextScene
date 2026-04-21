import { Bookmark, Trash2 } from "lucide-react";
import { MOVIES } from "./mock-data";
import { BottomNav } from "./bottom-nav";
import { MovieCard } from "./movie-card";

export function WatchlistPage() {
  const saved = [MOVIES[0], MOVIES[2], MOVIES[4], MOVIES[7]];

  return (
    <div className="min-h-screen bg-background pb-24 pt-12 px-6">
      <div className="flex items-center gap-2 mb-6">
        <Bookmark className="w-5 h-5 text-primary" />
        <h1 className="text-white">Watchlist</h1>
      </div>

      <p className="text-muted-foreground text-sm mb-6">{saved.length} filmes salvos</p>

      <div className="grid grid-cols-2 gap-4">
        {saved.map((movie) => (
          <MovieCard key={movie.id} movie={movie} />
        ))}
      </div>

      <BottomNav />
    </div>
  );
}
