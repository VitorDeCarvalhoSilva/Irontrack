import { render, screen } from '@testing-library/react-native';

import { Input } from './Input';

describe('Input', () => {
  it('renderiza o label recebido', async () => {
    await render(<Input label="Carga (kg)" />);

    expect(screen.getByText('Carga (kg)')).toBeTruthy();
  });

  it('repassa keyboardType para o TextInput nativo', async () => {
    await render(<Input label="Carga (kg)" keyboardType="decimal-pad" />);

    expect(screen.getByLabelText('Carga (kg)').props.keyboardType).toBe('decimal-pad');
  });

  it('exibe errorMessage quando informado', async () => {
    await render(<Input label="E-mail" errorMessage="E-mail inválido" />);

    expect(screen.getByText('E-mail inválido')).toBeTruthy();
  });
});
