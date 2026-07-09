import { Text } from 'react-native';
import { render, screen } from '@testing-library/react-native';

import { Card } from './Card';

describe('Card', () => {
  it('renderiza o conteúdo filho recebido', async () => {
    await render(
      <Card>
        <Text>Conteúdo do card</Text>
      </Card>,
    );

    expect(screen.getByText('Conteúdo do card')).toBeTruthy();
  });
});
