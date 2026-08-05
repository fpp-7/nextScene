import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import { ErrorMessage } from '../ErrorMessage';

describe('ErrorMessage', () => {
  it('mostra a mensagem recebida', async () => {
    const { getByText } = await render(
      <ErrorMessage message="Sua sessão expirou. Entre novamente." />
    );

    expect(getByText('Sua sessão expirou. Entre novamente.')).toBeTruthy();
  });

  it('tem mensagem padrão quando nenhuma é informada', async () => {
    const { getByText } = await render(<ErrorMessage />);

    expect(getByText('Algo deu errado. Tente novamente.')).toBeTruthy();
  });

  /**
   * Nem toda falha é recuperável. Oferecer "Tentar novamente" onde não há o que
   * repetir só gera frustração, então o botão depende de haver um `onRetry`.
   */
  it('só oferece nova tentativa quando há o que tentar', async () => {
    const semRetry = await render(<ErrorMessage message="Erro" />);
    expect(semRetry.queryByText('Tentar novamente')).toBeNull();

    const comRetry = await render(<ErrorMessage message="Erro" onRetry={jest.fn()} />);
    expect(comRetry.getByText('Tentar novamente')).toBeTruthy();
  });

  it('chama onRetry ao tocar no botão', async () => {
    const onRetry = jest.fn();
    const { getByText } = await render(<ErrorMessage message="Erro" onRetry={onRetry} />);

    fireEvent.press(getByText('Tentar novamente'));

    expect(onRetry).toHaveBeenCalledTimes(1);
  });
});
