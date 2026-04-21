import { User, Settings, Star, Film, Heart, LogOut, ChevronRight } from "lucide-react";
import { BottomNav } from "./bottom-nav";
import { useNavigate } from "react-router";

export function ProfilePage() {
  const navigate = useNavigate();

  const stats = [
    { label: "Avaliados", value: 47, icon: Star },
    { label: "Assistidos", value: 62, icon: Film },
    { label: "Favoritos", value: 15, icon: Heart },
  ];

  const menuItems = [
    { label: "Editar Perfil", icon: User },
    { label: "Preferencias de Genero", icon: Settings },
    { label: "Configuracoes", icon: Settings },
  ];

  return (
    <div className="min-h-screen bg-background pb-24 pt-12 px-6">
      {/* Avatar */}
      <div className="flex flex-col items-center gap-3 mb-8">
        <div className="w-20 h-20 rounded-full bg-primary/20 border-2 border-primary flex items-center justify-center">
          <User className="w-8 h-8 text-primary" />
        </div>
        <div className="text-center">
          <h2 className="text-white">Usuario NextScene</h2>
          <p className="text-muted-foreground text-sm">usuario@email.com</p>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-3 mb-8">
        {stats.map((s) => (
          <div key={s.label} className="bg-secondary rounded-xl p-4 flex flex-col items-center gap-1">
            <s.icon className="w-5 h-5 text-primary" />
            <p className="text-white">{s.value}</p>
            <p className="text-muted-foreground text-xs">{s.label}</p>
          </div>
        ))}
      </div>

      {/* Menu */}
      <div className="flex flex-col gap-2">
        {menuItems.map((item) => (
          <button key={item.label} className="flex items-center gap-3 bg-secondary rounded-xl px-4 py-3.5 cursor-pointer w-full text-left">
            <item.icon className="w-5 h-5 text-muted-foreground" />
            <span className="text-white flex-1 text-sm">{item.label}</span>
            <ChevronRight className="w-4 h-4 text-muted-foreground" />
          </button>
        ))}

        <button
          onClick={() => navigate("/")}
          className="flex items-center gap-3 bg-secondary rounded-xl px-4 py-3.5 cursor-pointer w-full text-left mt-4"
        >
          <LogOut className="w-5 h-5 text-red-400" />
          <span className="text-red-400 flex-1 text-sm">Sair</span>
        </button>
      </div>

      <BottomNav />
    </div>
  );
}
