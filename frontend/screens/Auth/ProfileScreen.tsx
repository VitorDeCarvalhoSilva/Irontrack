import { zodResolver } from '@hookform/resolvers/zod';
import * as Haptics from 'expo-haptics';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { ScrollView, Switch, Text, View } from 'react-native';

import { Button } from '../../components/common/Button';
import { Card } from '../../components/common/Card';
import { Input } from '../../components/common/Input';
import { Modal } from '../../components/common/Modal';
import { Header } from '../../components/layout/Header';
import { Screen } from '../../components/layout/Screen';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { usersService } from '../../services/usersService';
import type { AppStackScreenProps } from '../../navigation/types';
import {
  changePasswordSchema,
  deleteAccountSchema,
  profileSchema,
  type ChangePasswordFormValues,
  type DeleteAccountFormValues,
  type ProfileFormValues,
} from './ProfileScreen.schema';

function ProfileInfoSection() {
  const { user, updateUser } = useAuth();
  const { showToast } = useToast();

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { name: user?.name ?? '', email: user?.email ?? '' },
  });

  async function onSubmit(values: ProfileFormValues) {
    try {
      const updated = await usersService.updateMe(values);
      updateUser(updated);
      void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      showToast(
        'success',
        updated.emailVerifiedAt === null
          ? 'Perfil atualizado. Verifique seu novo e-mail para confirmá-lo.'
          : 'Perfil atualizado com sucesso.',
      );
    } catch (error) {
      void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
      showToast(
        'error',
        error instanceof Error ? error.message : 'Não foi possível atualizar o perfil.',
      );
    }
  }

  return (
    <Card className="gap-4">
      <Text className="font-oswald-medium text-display-md uppercase text-primary">Meus dados</Text>

      <Controller
        control={control}
        name="name"
        render={({ field: { onChange, onBlur, value } }) => (
          <Input
            label="Nome"
            icon="account"
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            errorMessage={errors.name?.message}
            accessibilityLabel="Nome"
          />
        )}
      />

      <Controller
        control={control}
        name="email"
        render={({ field: { onChange, onBlur, value } }) => (
          <Input
            label="E-mail"
            icon="email"
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            errorMessage={errors.email?.message}
            autoCapitalize="none"
            keyboardType="email-address"
            accessibilityLabel="E-mail"
          />
        )}
      />
      <Text className="-mt-2 text-body-sm text-muted">Trocar o e-mail exige nova verificação.</Text>

      <Button label="Salvar alterações" onPress={handleSubmit(onSubmit)} disabled={isSubmitting} />
    </Card>
  );
}

function ChangePasswordSection() {
  const { logout } = useAuth();
  const { showToast } = useToast();

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '', confirmNewPassword: '' },
  });

  async function onSubmit(values: ChangePasswordFormValues) {
    try {
      await usersService.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      // 03_CONTRATOS_API.md §2.9 — troca de senha revoga todos os refresh_tokens
      // ativos, inclusive o desta sessão: o logout local é consequência direta,
      // não uma escolha de UX independente.
      await logout();
    } catch (error) {
      void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
      showToast(
        'error',
        error instanceof Error ? error.message : 'Não foi possível trocar a senha.',
      );
    }
  }

  return (
    <Card className="gap-4">
      <Text className="font-oswald-medium text-display-md uppercase text-primary">
        Trocar senha
      </Text>

      <Controller
        control={control}
        name="currentPassword"
        render={({ field: { onChange, onBlur, value } }) => (
          <Input
            label="Senha atual"
            icon="lock"
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            errorMessage={errors.currentPassword?.message}
            secureTextEntry
            accessibilityLabel="Senha atual"
          />
        )}
      />

      <Controller
        control={control}
        name="newPassword"
        render={({ field: { onChange, onBlur, value } }) => (
          <Input
            label="Nova senha"
            icon="lock-reset"
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            errorMessage={errors.newPassword?.message}
            secureTextEntry
            accessibilityLabel="Nova senha"
          />
        )}
      />

      <Controller
        control={control}
        name="confirmNewPassword"
        render={({ field: { onChange, onBlur, value } }) => (
          <Input
            label="Confirmar nova senha"
            icon="lock-reset"
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            errorMessage={errors.confirmNewPassword?.message}
            secureTextEntry
            accessibilityLabel="Confirmar nova senha"
          />
        )}
      />

      <Button label="Trocar senha" onPress={handleSubmit(onSubmit)} disabled={isSubmitting} />
    </Card>
  );
}

/**
 * UI estática (08_ROADMAP_FRONTEND.md §C.1, tarefa 4): a ativação real via
 * expo-notifications (permissão + Expo Push Token +
 * POST /users/me/push-subscription) só acontece na Sprint 5
 * (04_FRONTEND_UI_COMPONENTES.md §E.3) — este bloco não chama nenhum
 * endpoint, é só o esqueleto visual.
 */
function PushPreferencesSection() {
  const [enabled, setEnabled] = useState(false);

  return (
    <Card className="gap-3">
      <Text className="font-oswald-medium text-display-md uppercase text-primary">
        Lembretes de treino
      </Text>
      <View className="flex-row items-center justify-between">
        <View className="flex-row items-center gap-2">
          <Text className="text-body-md text-primary">Ativar notificações push</Text>
        </View>
        <Switch
          value={enabled}
          onValueChange={setEnabled}
          accessibilityLabel="Ativar notificações push"
        />
      </View>
      <Text className="text-body-sm text-muted">
        Em breve: escolha dos dias e horário do lembrete. Esta preferência ainda não é enviada ao
        servidor nesta versão.
      </Text>
    </Card>
  );
}

function DeleteAccountSection() {
  const { logout } = useAuth();
  const { showToast } = useToast();
  const [modalVisible, setModalVisible] = useState(false);
  const [scheduledFor, setScheduledFor] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<DeleteAccountFormValues>({
    resolver: zodResolver(deleteAccountSchema),
    defaultValues: { password: '' },
  });

  async function onSubmit(values: DeleteAccountFormValues) {
    try {
      const result = await usersService.deleteAccount(values);
      setScheduledFor(result.deletionScheduledFor);
      reset();
      setModalVisible(false);
      void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      // 03_CONTRATOS_API.md §2.10 — a exclusão também revoga todos os
      // refresh_tokens; a conta fica bloqueada de login durante a carência.
      await logout();
    } catch (error) {
      void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
      showToast(
        'error',
        error instanceof Error ? error.message : 'Não foi possível excluir a conta.',
      );
    }
  }

  return (
    // Ação destrutiva isolada com space.xl acima (15_DESIGN_SYSTEM_UI_UX.md §I.1/04 §C.2).
    <Card className="mt-xl gap-3">
      <Text className="font-oswald-medium text-display-md uppercase text-accent">
        Excluir conta
      </Text>
      <Text className="text-body-md text-secondary">
        Sua conta entra em um período de carência de 30 dias antes da exclusão definitiva. Você pode
        cancelar fazendo login novamente dentro desse prazo.
      </Text>
      <Button
        label="Excluir conta"
        icon="trash-can"
        variant="destructive"
        onPress={() => {
          void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
          setModalVisible(true);
        }}
      />

      <Modal
        visible={modalVisible}
        animationType="slide"
        title="Confirmar exclusão"
        onClose={() => setModalVisible(false)}
      >
        <View className="gap-4">
          <Text className="text-body-md text-secondary">
            Digite sua senha atual para confirmar a exclusão da conta.
          </Text>

          <Controller
            control={control}
            name="password"
            render={({ field: { onChange, onBlur, value } }) => (
              <Input
                label="Senha atual"
                icon="lock"
                value={value}
                onChangeText={onChange}
                onBlur={onBlur}
                errorMessage={errors.password?.message}
                secureTextEntry
                accessibilityLabel="Senha atual"
              />
            )}
          />

          <Button
            label="Excluir conta permanentemente"
            icon="trash-can"
            variant="destructive"
            onPress={handleSubmit(onSubmit)}
            disabled={isSubmitting}
          />
        </View>
      </Modal>

      {scheduledFor ? (
        <Text className="text-body-md text-secondary">
          Exclusão agendada para {new Date(scheduledFor).toLocaleDateString('pt-BR')}.
        </Text>
      ) : null}
    </Card>
  );
}

export function ProfileScreen({ navigation }: AppStackScreenProps<'Profile'>) {
  const { logout } = useAuth();

  return (
    <Screen>
      <Header title="Perfil" onBackPress={() => navigation.goBack()} />
      <ScrollView className="flex-1" contentContainerClassName="gap-4 p-md">
        <ProfileInfoSection />
        <ChangePasswordSection />
        <PushPreferencesSection />
        <DeleteAccountSection />
        <Button label="Sair" icon="logout" variant="secondary" onPress={() => void logout()} />
      </ScrollView>
    </Screen>
  );
}
