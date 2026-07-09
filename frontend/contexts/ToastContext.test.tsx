import { act, fireEvent, render } from '@testing-library/react-native';
import { Button as RNButton, Text } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { useToast } from '../hooks/useToast';
import { ToastProvider } from './ToastContext';

const TEST_SAFE_AREA_METRICS = {
  frame: { x: 0, y: 0, width: 390, height: 844 },
  insets: { top: 47, left: 0, right: 0, bottom: 34 },
};

function Trigger() {
  const { showToast } = useToast();
  return (
    <>
      <RNButton
        title="Disparar sucesso"
        onPress={() => showToast('success', 'Perfil atualizado com sucesso.')}
      />
      <RNButton title="Disparar erro" onPress={() => showToast('error', 'Algo deu errado.')} />
    </>
  );
}

describe('ToastContext / Toast', () => {
  it('useToast lança erro fora de um ToastProvider', async () => {
    function Broken() {
      useToast();
      return <Text>nunca renderiza</Text>;
    }

    await expect(render(<Broken />)).rejects.toThrow(
      'useToast precisa ser usado dentro de um ToastProvider.',
    );
  });

  it('exibe o toast disparado, permite dispensar por toque, e mostra o próximo da fila em seguida', async () => {
    const { getByText, queryByText, getByLabelText } = await render(
      <SafeAreaProvider initialMetrics={TEST_SAFE_AREA_METRICS}>
        <ToastProvider>
          <Trigger />
        </ToastProvider>
      </SafeAreaProvider>,
    );

    await fireEvent.press(getByText('Disparar sucesso'));
    await fireEvent.press(getByText('Disparar erro'));

    // Fila de 1 item visível por vez (15_DESIGN_SYSTEM_UI_UX.md §F.1) — o
    // segundo toast espera o primeiro ser dispensado.
    expect(getByText('Perfil atualizado com sucesso.')).toBeTruthy();
    expect(queryByText('Algo deu errado.')).toBeNull();

    jest.useFakeTimers();
    fireEvent.press(getByLabelText('Dispensar'));
    await act(async () => {
      jest.advanceTimersByTime(500);
    });
    jest.useRealTimers();

    expect(queryByText('Perfil atualizado com sucesso.')).toBeNull();
    expect(getByText('Algo deu errado.')).toBeTruthy();
  });
});
