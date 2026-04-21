import { useNavigate, useParams } from "react-router";
import { ArrowLeft, Star, Bookmark, ThumbsUp, ThumbsDown, Share2 } from "lucide-react";
import { MOVIES } from "./mock-data";
import { ImageWithFallback } from "./figma/ImageWithFallback";
import { useState } from "react";

export function MovieDetailsPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const movie = MOVIES.find((m) => m.id === Number(id)) || MOVIES[0];
  const [saved, setSaved] = useState(false);

  return (
    <div className="min-h-screen bg-background">
      {/* Hero */}
      <div className="relative h-96">
        <ImageWithFallback src={movie.poster} alt={movie.title} className="w-full h-full object-cover" />
        <div className="absolute inset-0 bg-gradient-to-t from-background via-background/50 to-transparent" />
        <button onClick={() => navigate(-1)} className="absolute top-12 left-4 bg-black/50 rounded-full p-2 cursor-pointer">
          <ArrowLeft className="w-5 h-5 text-white" />
        </button>
        <button onClick={() => setSaved(!saved)} className="absolute top-12 right-4 bg-black/50 rounded-full p-2 cursor-pointer">
          <Bookmark className={`w-5 h-5 ${saved ? "text-primary fill-primary" : "text-white"}`} />
        </button>
      </div>

      {/* Content */}
      <div className="px-6 -mt-16 relative z-10">
        <span className="text-primary text-xs bg-primary/20 px-3 py-1 rounded-full">{movie.genre}</span>
        <h1 className="text-white mt-3">{movie.title}</h1>
        <p className="text-muted-foreground text-sm mt-1">{movie.year}</p>

        {/* Ratings */}
        <div className="flex items-center gap-6 mt-4">
          <div className="flex items-center gap-2">
            <Star className="w-5 h-5 text-primary fill-primary" />
            <div>
              <p className="text-white text-sm">{movie.imdb}</p>
              <p className="text-muted-foreground text-[10px]">IMDb</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Star className="w-5 h-5 text-primary fill-primary" />
            <div>
              <p className="text-white text-sm">{movie.rating}</p>
              <p className="text-muted-foreground text-[10px]">NextScene</p>
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex gap-3 mt-6">
          <button className="flex-1 bg-primary text-primary-foreground py-3 rounded-xl cursor-pointer flex items-center justify-center gap-2">
            <ThumbsUp className="w-4 h-4" /> Curti
          </button>
          <button className="flex-1 bg-secondary text-muted-foreground py-3 rounded-xl cursor-pointer flex items-center justify-center gap-2">
            <ThumbsDown className="w-4 h-4" /> Nao Curti
          </button>
          <button className="bg-secondary text-muted-foreground p-3 rounded-xl cursor-pointer">
            <Share2 className="w-4 h-4" />
          </button>
        </div>

        {/* Synopsis */}
        <div className="mt-6">
          <h3 className="text-white mb-2">Sinopse</h3>
          <p className="text-muted-foreground text-sm leading-relaxed">{movie.synopsis}</p>
        </div>

        {/* Cast */}
        <div className="mt-6 pb-10">
          <h3 className="text-white mb-3">Elenco</h3>
          <div className="flex gap-3 overflow-x-auto no-scrollbar">
            {movie.cast.map((actor) => (
              <div key={actor} className="bg-secondary rounded-xl px-4 py-2 whitespace-nowrap">
                <p className="text-white text-sm">{actor}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
