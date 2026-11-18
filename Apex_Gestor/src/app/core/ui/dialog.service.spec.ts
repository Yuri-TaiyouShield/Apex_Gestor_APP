import { TestBed } from '@angular/core/testing';

import { DialogService } from './dialog.service';

describe('DialogService', () => {
  let service: DialogService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DialogService);
  });

  it('resolve confirm() com true ao aceitar e false ao cancelar', async () => {
    const acceptedPromise = service.confirm({
      title: 'Confirmar estorno',
      message: 'Deseja cancelar o pedido #102?',
      tone: 'danger'
    });
    expect(service.active()?.kind).toBe('confirm');

    service.accept();
    await expectAsync(acceptedPromise).toBeResolvedTo(true);
    expect(service.active()).toBeNull();

    const cancelledPromise = service.confirm({
      title: 'Sair',
      message: 'Descartar alterações?'
    });
    service.cancel();
    await expectAsync(cancelledPromise).toBeResolvedTo(false);
    expect(service.active()).toBeNull();
  });

  it('valida campo obrigatório em prompt(), remove espaços nas pontas e resolve null ao cancelar', async () => {
    const promptPromise = service.prompt({
      title: 'Motivo do cancelamento',
      label: 'Motivo',
      required: true,
      requiredMessage: 'Informe o motivo.'
    });

    service.updatePromptValue('   ');
    service.accept();
    const afterInvalid = service.active();
    expect(afterInvalid?.kind).toBe('prompt');
    if (afterInvalid?.kind === 'prompt') {
      expect(afterInvalid.validationError).toBe('Informe o motivo.');
    }

    service.updatePromptValue('  Cliente desistiu  ');
    service.accept();
    await expectAsync(promptPromise).toBeResolvedTo('Cliente desistiu');

    const cancelledPrompt = service.prompt({
      title: 'Observação',
      label: 'Texto',
      required: false
    });
    service.cancel();
    await expectAsync(cancelledPrompt).toBeResolvedTo(null);
  });

  it('cancela diálogo anterior pendente ao abrir um novo', async () => {
    const first = service.confirm({ title: 'Primeiro', message: 'A' });
    const second = service.confirm({ title: 'Segundo', message: 'B' });

    await expectAsync(first).toBeResolvedTo(false);
    service.accept();
    await expectAsync(second).toBeResolvedTo(true);
  });
});
