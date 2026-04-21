import { useState } from "react";
import { useNavigate } from "react-router";
import { Star, Check, ThumbsUp, ThumbsDown } from "lucide-react";
import { MOVIES } from "./mock-data";
import { ImageWithFallback } from "./figma/ImageWithFallback";

export function OnboardingColdStart() {
  const navigate = useNavigate();
  const [ratings, setRatings] = useState<Record<number, "like" | "dislike" | "seen">>({});

  const setRating = (id: number, type: "like" | "dislike" | "seen") => {
    setRatings((r) => ({ ...r, [id]: r[id] === type ? undefined! : type }));
  };

  return (
    <div className="min-h-screen bg-background flex flex-col px-6 py-10">
      <div className="flex flex-col gap-2 mb-2">
        <p className="text-primary text-sm">Etapa 2 de 2</p>
        <h1 className="text-white">Avalie Filmes</h1>
        <p className="text-muted-foreground text-sm">Avalie alguns filmes para calibrar suas recomendacoes</p>
      </div>

      <div className="flex gap-2 mb-6">
        <div className="flex-1 h-1 rounded-full bg-primary" />
        <div className="flex-1 h-1 rounded-full bg-primary" />
      </div>

      <div className="grid grid-cols-2 gap-4 flex-1">
        {MOVIES.slice(0, 8).map((movie) => (
          <div key={movie.id} className="flex flex-col gap-2">
            <div className="relative aspect-[2/3] rounded-xl overflow-hidden">
              <ImageWithFallback src={movie.poster} alt={movie.title} className="w-full h-full object-cover" />
              <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/90 to-transparent p-3">
                <p className="text-white text-xs truncate">{movie.title}</p>
                <div className="flex items-center gap-1">
                  <Star className="w-3 h-3 text-primary fill-primary" />
                  <span className="text-primary text-xs">{movie.rating}</span>
                </div>
              </div>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => setRating(movie.id, "like")}
                className={`flex-1 py-1.5 rounded-lg flex items-center justify-center cursor-pointer transition-all ${
                  ratings[movie.id] === "like" ? "bg-primary/20 text-primary" : "bg-secondary text-muted-foreground"
                }`}
              >
                <ThumbsUp className="w-4 h-4" />
              </button>
              <button
                onClick={() => setRating(movie.id, "dislike")}
                className={`flex-1 py-1.5 rounded-lg flex items-center justify-center cursor-pointer transition-all ${
                  ratings[movie.id] === "dislike" ? "bg-red-500/20 text-red-400" : "bg-secondary text-muted-foreground"
                }`}
              >
                <ThumbsDown className="w-4 h-4" />
              </button>
              <button
                onClick={() => setRating(movie.id, "seen")}
                className={`flex-1 py-1.5 rounded-lg flex items-center justify-center cursor-pointer transition-all ${
                  ratings[movie.id] === "seen" ? "bg-white/20 text-white" : "bg-secondary text-muted-foreground"
                }`}
              >
                <Check className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))}
      </div>

      <button
        onClick={() => navigate("/discover")}
        className="w-full bg-primary text-primary-foreground py-3.5 rounded-xl mt-6 cursor-pointer hover:opacity-90 transition-opacity"
      >
        Comecar a Explorar
      </button>
    </div>
  );
}
