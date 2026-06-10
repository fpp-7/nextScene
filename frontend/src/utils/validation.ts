export function validateEmail(email: string): string | null {
  if (!email.trim()) return 'Email é obrigatório';
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!regex.test(email)) return 'Email inválido';
  return null;
}

export function validatePassword(password: string): string | null {
  if (!password) return 'Senha é obrigatória';
  if (password.length < 6) return 'Senha deve ter pelo menos 6 caracteres';
  return null;
}

export function validateName(name: string): string | null {
  if (!name.trim()) return 'Nome é obrigatório';
  if (name.trim().length < 2) return 'Nome deve ter pelo menos 2 caracteres';
  return null;
}
