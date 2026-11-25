export type StatusKey = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'INTERRUPTED';

/** Restricted semantic palette: orange = to do, blue = in progress, brand green = done, red = interrupted. */
export const STATUS_META: Record<StatusKey, { label: string; color: string; soft: string; text: string }> = {
  TODO: { label: 'A Fazer', color: '#e8590c', soft: '#fff1e6', text: '#b94309' },
  IN_PROGRESS: { label: 'Em Andamento', color: '#1c7ed6', soft: '#e7f3fd', text: '#1864ab' },
  DONE: { label: 'Concluído', color: '#1d720a', soft: '#ebf6e8', text: '#17600a' },
  INTERRUPTED: { label: 'Interrompido', color: '#e03131', soft: '#ffecec', text: '#b02525' },
};

export function statusMeta(status: string) {
  return (
    STATUS_META[status as StatusKey] ?? { label: status, color: '#868e96', soft: '#f1f3f5', text: '#495057' }
  );
}
