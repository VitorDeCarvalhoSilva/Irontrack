import { Text } from 'react-native';
import { fireEvent, render, screen } from '@testing-library/react-native';

import { Modal } from './Modal';

describe('Modal', () => {
  it('renderiza título e conteúdo quando visible=true', async () => {
    await render(
      <Modal visible title="Confirmação" onClose={() => {}}>
        <Text>Deseja continuar?</Text>
      </Modal>,
    );

    expect(screen.getByText('Confirmação')).toBeTruthy();
    expect(screen.getByText('Deseja continuar?')).toBeTruthy();
  });

  it('dispara onClose ao tocar no botão de fechar', async () => {
    const onClose = jest.fn();
    await render(
      <Modal visible title="Confirmação" onClose={onClose}>
        <Text>Deseja continuar?</Text>
      </Modal>,
    );

    await fireEvent.press(screen.getByLabelText('Fechar'));

    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
