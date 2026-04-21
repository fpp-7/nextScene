import { useState } from "react";
import { useNavigate } from "react-router";
import { Clapperboard, Mail, Lock } from "lucide-react";

export function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center px-6">
      <div className="w-full max-w-sm flex flex-col items-center gap-8">
        {/* Logo */}
        <div className="flex flex-col items-center gap-3">
          <div className="w-14 h-14 bg-primary rounded-xl flex items-center justify-center">
            <Clapperboard className="w-7 h-7 text-primary-foreground" />
          </div>
          <h1 className="text-white">NextScene</h1>
          <p className="text-muted-foreground text-sm">Descubra seu proximo filme favorito</p>
        </div>

        {/* Form */}
        <div className="w-full flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm text-white/80">Email</label>
            <div className="flex items-center gap-3 bg-input-background rounded-xl px-4 py-3">
              <Mail className="w-5 h-5 text-muted-foreground" />
              <input
                type="email"
                placeholder="seu@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="bg-transparent flex-1 text-white placeholder:text-muted-foreground outline-none"
              />
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-sm text-white/80">Senha</label>
            <div className="flex items-center gap-3 bg-input-background rounded-xl px-4 py-3">
              <Lock className="w-5 h-5 text-muted-foreground" />
              <input
                type="password"
                placeholder="********"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="bg-transparent flex-1 text-white placeholder:text-muted-foreground outline-none"
              />
            </div>
          </div>

          <button
            onClick={() => navigate("/onboarding/genres")}
            className="w-full bg-primary text-primary-foreground py-3.5 rounded-xl mt-2 cursor-pointer hover:opacity-90 transition-opacity"
          >
            Entrar
          </button>

          <p className="text-center text-muted-foreground text-sm mt-2">
            Nao tem uma conta?{" "}
            <span onClick={() => navigate("/register")} className="text-primary underline cursor-pointer">
              Cadastre-se
            </span>
          </p>
        </div>
      </div>
    </div>
  );
}
