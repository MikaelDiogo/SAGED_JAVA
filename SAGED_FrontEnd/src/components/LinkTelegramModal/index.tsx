import { useState } from 'react';
import { Modal, Stack, Text, PinInput, Button, Group, List, ThemeIcon, Alert } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconBrandTelegram, IconCheck, IconAlertCircle } from '@tabler/icons-react';
import { isAxiosError } from 'axios';
import { api } from '../../services/api';

interface LinkTelegramModalProps {
  opened: boolean;
  onClose: () => void;
}

/** Vincula o Telegram do usuário logado usando o código de 6 dígitos gerado pelo botão "Sou técnico" do bot. */
export function LinkTelegramModal({ opened, onClose }: LinkTelegramModalProps) {
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const close = () => {
    setCode('');
    setError(null);
    onClose();
  };

  const submit = async () => {
    setLoading(true);
    setError(null);
    try {
      await api.post('/telegram/technicians/link', { code });
      notifications.show({
        title: 'Telegram vinculado',
        message: 'Abra o bot do SAGED e toque em Área do Técnico.',
        color: 'green',
        icon: <IconCheck size={16} />,
      });
      close();
    } catch (e) {
      const status = isAxiosError(e) ? e.response?.status : undefined;
      if (status === 400) setError('Código inválido ou vencido. Toque em "Gerar novo código" no bot e tente de novo.');
      else if (status === 409) setError('Esta conta ou este Telegram já está vinculado.');
      else setError('Não foi possível vincular agora. Tente novamente em instantes.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      opened={opened}
      onClose={close}
      centered
      radius="md"
      title={
        <Group gap={8}>
          <ThemeIcon color="blue" variant="light" radius="xl"><IconBrandTelegram size={16} /></ThemeIcon>
          <Text fw={700}>Vincular Telegram</Text>
        </Group>
      }
    >
      <Stack gap="md">
        <List type="ordered" size="sm" spacing={6}>
          <List.Item>No Telegram, abra o bot do SAGED.</List.Item>
          <List.Item>Toque no botão <b>Sou técnico</b>.</List.Item>
          <List.Item>Digite abaixo o código de 6 dígitos que o bot mostrar.</List.Item>
        </List>
        <Group justify="center">
          <PinInput length={6} type="number" size="lg" value={code} onChange={setCode} oneTimeCode aria-label="Código de vínculo" />
        </Group>
        {error && (
          <Alert color="red" variant="light" icon={<IconAlertCircle size={16} />}>{error}</Alert>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={close}>Cancelar</Button>
          <Button color="green.8" onClick={submit} loading={loading} disabled={code.length !== 6}>
            Vincular
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
