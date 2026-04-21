import { useNavigate } from "react-router";
import { Star, Bookmark } from "lucide-react";
import { ImageWithFallback } from "./figma/ImageWithFallback";

interface MovieCardProps {
  movie: { id: number; title: string; year: number; genre: string; rating: number; poster: string };
  size?: "sm" | "lg";
}

export function MovieCard({ movie, size = "sm" }: MovieCardProps) {
  const navigate = useNavigate();

  return (
    <button
      onClick={() => navigate(`/movie/${movie.id}`)}
      className={`relative rounded-xl overflow-hidden cursor-pointer group ${size === "lg" ? "aspect-[2/3]" : "aspect-[2/3]"}`}
    >
      <ImageWithFallback src={movie.poster} alt={movie.title} className="w-full h-full object-cover" />
      <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/20 to-transparent" />
      <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
        <div className="bg-black/60 rounded-full p-1.5">
          <Bookmark className="w-4 h-4 text-white" />
        </div>
      </div>
      <div className="absolute bottom-0 left-0 right-0 p-3">
        <p className="text-white text-sm truncate">{movie.title}</p>
        <div className="flex items-center gap-2 mt-1">
          <div className="flex items-center gap-1">
            <Star className="w-3 h-3 text-primary fill-primary" />
            <span className="text-primary text-xs">{movie.rating}</span>
          </div>
          <span className="text-muted-foreground text-xs">{movie.year}</span>
        </div>
      </div>
    </button>
  );
}
