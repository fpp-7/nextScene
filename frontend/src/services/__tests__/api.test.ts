/**
 * Testes do cliente HTTP.
 *
 * O interceptor de 401 é a peça que faltava quando a sessão expirava: o app
 * seguia "logado" e todas as telas mostravam erro sem explicação. Vale travar
 * o comportamento dele.
 */

type Handler = {
  fulfilled?: (value: any) => any;
  rejected?: (error: any) => any;
};

/** Reimporta o módulo do zero, já que ele guarda o token em estado interno. */
function loadApi() {
  let api: typeof import('../api');
  jest.isolateModules(() => {
    api = require('../api');
  });
  return api!;
}

function requestHandler(client: any): Handler {
  return client.interceptors.request.handlers[0];
}

function responseHandler(client: any): Handler {
  return client.interceptors.response.handlers[0];
}

describe('configuração', () => {
  it('usa EXPO_PUBLIC_API_URL quando definida', () => {
    const original = process.env.EXPO_PUBLIC_API_URL;
    process.env.EXPO_PUBLIC_API_URL = 'https://api.exemplo.com/api';

    const { apiClient } = loadApi();

    expect(apiClient.defaults.baseURL).toBe('https://api.exemplo.com/api');
    process.env.EXPO_PUBLIC_API_URL = original;
  });

  it('cai para localhost quando a variável não está definida', () => {
    const original = process.env.EXPO_PUBLIC_API_URL;
    delete process.env.EXPO_PUBLIC_API_URL;

    const { apiClient } = loadApi();

    // Regressão: a URL era um túnel localtunnel fixo no código, que expira.
    expect(apiClient.defaults.baseURL).toBe('http://localhost:8080/api');
    expect(apiClient.defaults.baseURL).not.toContain('loca.lt');

    process.env.EXPO_PUBLIC_API_URL = original;
  });

  it('define um timeout', () => {
    const { apiClient } = loadApi();
    expect(apiClient.defaults.timeout).toBeGreaterThan(0);
  });
});

describe('token de autenticação', () => {
  it('anexa o header Authorization quando há token', () => {
    const { apiClient, setAuthToken } = loadApi();
    setAuthToken('token-abc');

    const config = requestHandler(apiClient).fulfilled!({ headers: {} });

    expect(config.headers.Authorization).toBe('Bearer token-abc');
  });

  it('não anexa header quando não há token', () => {
    const { apiClient } = loadApi();

    const config = requestHandler(apiClient).fulfilled!({ headers: {} });

    expect(config.headers.Authorization).toBeUndefined();
  });

  it('setAuthToken(null) limpa o token', () => {
    const { setAuthToken, getAuthToken } = loadApi();

    setAuthToken('token-abc');
    expect(getAuthToken()).toBe('token-abc');

    setAuthToken(null);
    expect(getAuthToken()).toBeNull();
  });
});

describe('interceptor de 401', () => {
  it('dispara o handler quando havia sessão ativa', async () => {
    const { apiClient, setAuthToken, setUnauthorizedHandler } = loadApi();
    const onUnauthorized = jest.fn();
    setAuthToken('token-expirado');
    setUnauthorizedHandler(onUnauthorized);

    await expect(
      responseHandler(apiClient).rejected!({ response: { status: 401 } })
    ).rejects.toBeDefined();

    expect(onUnauthorized).toHaveBeenCalledTimes(1);
  });

  it('limpa o token ao deslogar por expiração', async () => {
    const { apiClient, setAuthToken, getAuthToken, setUnauthorizedHandler } = loadApi();
    setAuthToken('token-expirado');
    setUnauthorizedHandler(jest.fn());

    await responseHandler(apiClient).rejected!({ response: { status: 401 } }).catch(() => {});

    expect(getAuthToken()).toBeNull();
  });

  it('não dispara no 401 do próprio login', async () => {
    // Sem sessão ativa, um 401 é credencial errada — a tela de login precisa
    // tratar o erro, não ser expulsa por ele.
    const { apiClient, setUnauthorizedHandler } = loadApi();
    const onUnauthorized = jest.fn();
    setUnauthorizedHandler(onUnauthorized);

    await responseHandler(apiClient).rejected!({ response: { status: 401 } }).catch(() => {});

    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it('ignora outros códigos de erro', async () => {
    const { apiClient, setAuthToken, setUnauthorizedHandler } = loadApi();
    const onUnauthorized = jest.fn();
    setAuthToken('token-valido');
    setUnauthorizedHandler(onUnauthorized);

    for (const status of [400, 403, 404, 500]) {
      await responseHandler(apiClient).rejected!({ response: { status } }).catch(() => {});
    }

    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it('não quebra quando não há handler registrado', async () => {
    const { apiClient, setAuthToken } = loadApi();
    setAuthToken('token-expirado');

    await expect(
      responseHandler(apiClient).rejected!({ response: { status: 401 } })
    ).rejects.toBeDefined();
  });

  it('erro de rede sem response é repassado', async () => {
    const { apiClient, setAuthToken, setUnauthorizedHandler } = loadApi();
    const onUnauthorized = jest.fn();
    setAuthToken('token-valido');
    setUnauthorizedHandler(onUnauthorized);

    await expect(
      responseHandler(apiClient).rejected!({ message: 'Network Error' })
    ).rejects.toBeDefined();

    expect(onUnauthorized).not.toHaveBeenCalled();
  });
});
