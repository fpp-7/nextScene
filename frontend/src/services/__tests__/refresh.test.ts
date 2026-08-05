/**
 * Renovação automática da sessão.
 *
 * O access token dura 30 minutos. Sem renovação, o usuário era devolvido à tela
 * de login a cada meia hora de uso.
 */
import axios from 'axios';

jest.mock('axios', () => {
  const actual = jest.requireActual('axios');
  return { ...actual, default: { ...actual.default, post: jest.fn() }, post: jest.fn() };
});

type Handler = { fulfilled?: (v: any) => any; rejected?: (e: any) => any };

function loadApi() {
  let api: typeof import('../api');
  jest.isolateModules(() => {
    api = require('../api');
  });
  return api!;
}

const responseHandler = (client: any): Handler => client.interceptors.response.handlers[0];

function unauthorized(url = '/watchlist') {
  return { response: { status: 401 }, config: { url, headers: {} } };
}

beforeEach(() => {
  (axios.post as jest.Mock).mockReset();
});

describe('renovação automática', () => {
  it('renova e repete a requisição que falhou', async () => {
    const api = loadApi();
    api.setAuthToken('vencido');
    api.setRefreshToken('refresh-valido');

    (axios.post as jest.Mock).mockResolvedValue({
      data: { token: 'novo-access', refreshToken: 'novo-refresh' },
    });

    // O replay usa o próprio apiClient; interceptamos o adapter para não sair
    // pela rede e conferir que a repetição carrega o token novo.
    const replayed: any[] = [];
    api.apiClient.defaults.adapter = async (config: any) => {
      replayed.push(config);
      return { data: 'ok', status: 200, statusText: 'OK', headers: {}, config };
    };

    const result: any = await responseHandler(api.apiClient).rejected!(unauthorized());

    expect(result.data).toBe('ok');
    expect(replayed[0].headers.Authorization).toBe('Bearer novo-access');
    expect(api.getAuthToken()).toBe('novo-access');
    expect(api.getRefreshToken()).toBe('novo-refresh');
  });

  it('avisa para persistir os tokens renovados', async () => {
    const api = loadApi();
    api.setAuthToken('vencido');
    api.setRefreshToken('refresh-valido');

    const onRenewed = jest.fn();
    api.setTokensRenewedHandler(onRenewed);
    (axios.post as jest.Mock).mockResolvedValue({
      data: { token: 'novo-access', refreshToken: 'novo-refresh' },
    });
    api.apiClient.defaults.adapter = async (config: any) => ({
      data: 'ok', status: 200, statusText: 'OK', headers: {}, config,
    });

    await responseHandler(api.apiClient).rejected!(unauthorized());

    expect(onRenewed).toHaveBeenCalledWith('novo-access', 'novo-refresh');
  });

  /**
   * O refresh token é rotacionado e vale uma única vez. Se três chamadas
   * paralelas renovassem em paralelo, a segunda seria recusada e o backend
   * trataria a repetição como roubo, derrubando a sessão inteira.
   */
  it('faz uma única renovação para várias requisições simultâneas', async () => {
    const api = loadApi();
    api.setAuthToken('vencido');
    api.setRefreshToken('refresh-valido');

    (axios.post as jest.Mock).mockImplementation(
      () => new Promise((resolve) =>
        setTimeout(() => resolve({ data: { token: 'novo', refreshToken: 'r2' } }), 10))
    );
    api.apiClient.defaults.adapter = async (config: any) => ({
      data: 'ok', status: 200, statusText: 'OK', headers: {}, config,
    });

    const handler = responseHandler(api.apiClient).rejected!;
    await Promise.all([handler(unauthorized('/a')), handler(unauthorized('/b')), handler(unauthorized('/c'))]);

    expect(axios.post).toHaveBeenCalledTimes(1);
  });

  it('desloga quando a renovação é recusada', async () => {
    const api = loadApi();
    api.setAuthToken('vencido');
    api.setRefreshToken('refresh-invalido');

    const onUnauthorized = jest.fn();
    api.setUnauthorizedHandler(onUnauthorized);
    (axios.post as jest.Mock).mockRejectedValue(new Error('401'));

    await expect(responseHandler(api.apiClient).rejected!(unauthorized())).rejects.toBeDefined();

    expect(onUnauthorized).toHaveBeenCalledTimes(1);
    expect(api.getAuthToken()).toBeNull();
    expect(api.getRefreshToken()).toBeNull();
  });

  it('não tenta renovar sem refresh token', async () => {
    const api = loadApi();
    api.setAuthToken('vencido');
    const onUnauthorized = jest.fn();
    api.setUnauthorizedHandler(onUnauthorized);

    await expect(responseHandler(api.apiClient).rejected!(unauthorized())).rejects.toBeDefined();

    expect(axios.post).not.toHaveBeenCalled();
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
  });

  it('não entra em laço se a repetição também falhar', async () => {
    const api = loadApi();
    api.setAuthToken('vencido');
    api.setRefreshToken('refresh-valido');

    const jaRepetida = { response: { status: 401 }, config: { url: '/x', headers: {}, _retried: true } };

    await expect(responseHandler(api.apiClient).rejected!(jaRepetida)).rejects.toBeDefined();

    expect(axios.post).not.toHaveBeenCalled();
  });

  it('não tenta renovar quando quem falhou foi o próprio refresh', async () => {
    const api = loadApi();
    api.setAuthToken('vencido');
    api.setRefreshToken('refresh-valido');

    await expect(
      responseHandler(api.apiClient).rejected!(unauthorized('/auth/refresh'))
    ).rejects.toBeDefined();

    expect(axios.post).not.toHaveBeenCalled();
  });
});
