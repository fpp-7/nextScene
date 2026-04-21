import { useNavigate, useLocation } from "react-router";
import { Compass, Sparkles, Bookmark, User } from "lucide-react";

const tabs = [
  { path: "/discover", icon: Compass, label: "Descobrir" },
  { path: "/recommendations", icon: Sparkles, label: "Para Voce" },
  { path: "/watchlist", icon: Bookmark, label: "Watchlist" },
  { path: "/profile", icon: User, label: "Perfil" },
];

export function BottomNav() {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-card/95 backdrop-blur-md border-t border-border px-4 pb-5 pt-2">
      <div className="flex justify-around max-w-md mx-auto">
        {tabs.map((tab) => {
          const active = location.pathname.startsWith(tab.path);
          return (
            <button
              key={tab.path}
              onClick={() => navigate(tab.path)}
              className={`flex flex-col items-center gap-1 cursor-pointer transition-colors ${
                active ? "text-primary" : "text-muted-foreground"
              }`}
            >
              <tab.icon className="w-5 h-5" />
              <span className="text-[10px]">{tab.label}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
