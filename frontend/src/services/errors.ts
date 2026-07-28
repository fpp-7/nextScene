/**
 * Converte erros de rede em mensagens que fazem sentido para quem está usando o
 * app.
 *
 * Sem isto, o axios entrega "Request failed with status code 403" direto na
 * tela — texto que não diz ao usuário o que houve nem o que fazer.
 */
export function messageFor(error: any, fallback = 'Algo deu errado. Tente novamente.'): string {
  // Mensagem vinda do backend tem prioridade: é a mais específica.
  const fromServer = error?.response?.data?.error;
  if (typeof fromServer === 'string' && fromServer.trim()) {
    return fromServer;
  }

  const status = error?.response?.status;
  if (status) {
    switch (status) {
      case 401:
        return 'Sua sessão expirou. Entre novamente.';
      case 403:
        return 'Você não tem permissão para isso.';
      case 404:
        return 'Não encontramos o que você procurava.';
      case 429:
        return 'Muitas tentativas. Aguarde alguns minutos.';
      default:
        return status >= 500
          ? 'O servidor está com problemas. Tente de novo em instantes.'
          : fallback;
    }
  }

  // Sem `response`: a requisição não chegou ao servidor.
  if (error?.code === 'ECONNABORTED') {
    return 'A conexão demorou demais. Verifique sua internet.';
  }
  if (error?.message === 'Network Error') {
    return 'Não foi possível conectar ao servidor. Verifique sua internet.';
  }

  return fallback;
}
