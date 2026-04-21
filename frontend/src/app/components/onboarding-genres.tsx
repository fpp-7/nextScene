import { useState } from "react";
import { useNavigate } from "react-router";
import { ArrowRight, Heart, X } from "lucide-react";
import { GENRES } from "./mock-data";

export function OnboardingGenres() {
  const navigate = useNavigate();
  const [liked, setLiked] = useState<string[]>([]);
  const [disliked, setDisliked] = useState<string[]>([]);

  const toggle = (genre: string, list: "liked" | "disliked") => {
    if (list === "liked") {
      setDisliked((d) => d.filter((g) => g !== genre));
      setLiked((l) => l.includes(genre) ? l.filter((g) => g !== genre) : [...l, genre]);
    } else {
      setLiked((l) => l.filter((g) => g !== genre));
      setDisliked((d) => d.includes(genre) ? d.filter((g) => g !== genre) : [...d, genre]);
    }
  };

  const getState = (genre: string) => {
    if (liked.includes(genre)) return "liked";
    if (disliked.includes(genre)) return "disliked";
    return "neutral";
  };

  return (
    <div className="min-h-screen bg-background flex flex-col px-6 py-10">
      {/* Header */}
      <div className="flex flex-col gap-2 mb-2">
        <p className="text-primary text-sm">Etapa 1 de 2</p>
        <h1 className="text-white">Seus Generos</h1>
        <p className="text-muted-foreground text-sm">
          Toque uma vez para <span className="text-primary">curtir</span>, duas vezes para <span className="text-red-400">excluir</span>
        </p>
      </div>

      {/* Progress */}
      <div className="flex gap-2 mb-6">
        <div className="flex-1 h-1 rounded-full bg-primary" />
        <div className="flex-1 h-1 rounded-full bg-secondary" />
      </div>

      {/* Genre grid */}
      <div className="flex flex-wrap gap-3 flex-1">
        {GENRES.map((genre) => {
          const state = getState(genre);
          return (
            <button
              key={genre}
              onClick={() => {
                if (state === "neutral") toggle(genre, "liked");
                else if (state === "liked") toggle(genre, "disliked");
                else { setDisliked((d) => d.filter((g) => g !== genre)); }
              }}
              className={`px-4 py-2.5 rounded-full border cursor-pointer flex items-center gap-2 transition-all ${
                state === "liked"
                  ? "border-primary bg-primary/20 text-primary"
                  : state === "disliked"
                  ? "border-red-500/50 bg-red-500/10 text-red-400"
                  : "border-border bg-secondary text-white/70"
              }`}
            >
              {state === "liked" && <Heart className="w-3.5 h-3.5 fill-primary" />}
              {state === "disliked" && <X className="w-3.5 h-3.5" />}
              {genre}
            </button>
          );
        })}
      </div>

      {/* CTA */}
      <button
        onClick={() => navigate("/onboarding/coldstart")}
        className="w-full bg-primary text-primary-foreground py-3.5 rounded-xl mt-6 cursor-pointer hover:opacity-90 transition-opacity flex items-center justify-center gap-2"
      >
        Continuar <ArrowRight className="w-5 h-5" />
      </button>
    </div>
  );
}
