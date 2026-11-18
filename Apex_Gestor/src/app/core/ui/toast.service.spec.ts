import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ToastService);
  });

  afterEach(() => {
    service.clear();
  });

  it('emite toasts semânticos e limita fila visível a no máximo 4 itens', () => {
    service.info('Info 1');
    service.success('Sucesso 2', 'Operação');
    service.warning('Atenção 3');
    service.danger('Erro 4');
    service.info('Info 5');

    const items = service.items();
    expect(items.length).toBe(4);
    expect(items.map((item) => item.message)).toEqual(['Sucesso 2', 'Atenção 3', 'Erro 4', 'Info 5']);
    expect(items[0].tone).toBe('success');
    expect(items[0].title).toBe('Operação');
  });

  it('limpa timers de toasts descartados ao ultrapassar o limite da fila', fakeAsync(() => {
    service.show({ message: '1', durationMs: 500 });
    service.show({ message: '2', durationMs: 2000 });
    service.show({ message: '3', durationMs: 2000 });
    service.show({ message: '4', durationMs: 2000 });
    service.show({ message: '5', durationMs: 2000 });

    tick(600);
    expect(service.items().map((item) => item.message)).toEqual(['2', '3', '4', '5']);

    tick(1500);
    expect(service.items()).toEqual([]);
  }));

  it('remove toast automaticamente após durationMs expirar', fakeAsync(() => {
    service.show({ message: 'Temporário', tone: 'info', durationMs: 1200 });
    expect(service.items().length).toBe(1);

    tick(1199);
    expect(service.items().length).toBe(1);

    tick(1);
    expect(service.items().length).toBe(0);
  }));

  it('permite encerramento manual por id e limpeza global com clear()', () => {
    const id1 = service.info('Primeiro');
    const id2 = service.warning('Segundo');

    service.dismiss(id1);
    expect(service.items().map((item) => item.id)).toEqual([id2]);

    service.clear();
    expect(service.items()).toEqual([]);
  });
});
