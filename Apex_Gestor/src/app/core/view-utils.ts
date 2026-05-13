export function currency(value: unknown): string {
  const numberValue = Number(value ?? 0);
  return numberValue.toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  });
}

export function date(value: unknown): string {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('pt-BR').format(new Date(String(value)));
}

export function getNestedValue(item: Record<string, unknown>, key: string): unknown {
  return key.split('.').reduce<unknown>((current, segment) => {
    if (current && typeof current === 'object' && segment in current) {
      return (current as Record<string, unknown>)[segment];
    }
    return undefined;
  }, item);
}
