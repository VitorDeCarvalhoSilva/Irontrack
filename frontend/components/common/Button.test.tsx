import { fireEvent, render, screen } from '@testing-library/react-native';

import { Button } from './Button';

describe('Button', () => {
  it('renderiza o label recebido', async () => {
    await render(<Button label="Salvar" onPress={() => {}} />);

    expect(screen.getByText('Salvar')).toBeTruthy();
  });

  it('dispara onPress ao ser tocado', async () => {
    const onPress = jest.fn();
    await render(<Button label="Confirmar" onPress={onPress} />);

    await fireEvent.press(screen.getByRole('button'));

    expect(onPress).toHaveBeenCalledTimes(1);
  });

  it('marca accessibilityState.disabled quando disabled=true', async () => {
    await render(<Button label="Indisponível" onPress={() => {}} disabled />);

    expect(screen.getByRole('button').props.accessibilityState.disabled).toBe(true);
  });
});
