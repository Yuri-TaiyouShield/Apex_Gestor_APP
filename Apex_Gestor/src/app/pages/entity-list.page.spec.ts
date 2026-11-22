import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { addIcons } from 'ionicons';
import {
  add,
  albumsOutline,
  banOutline,
  cashOutline,
  checkmarkCircleOutline,
  chevronBackOutline,
  chevronForwardOutline,
  cloudOfflineOutline,
  createOutline,
  cubeOutline,
  filterOutline,
  layersOutline,
  receiptOutline,
  syncOutline,
  warningOutline
} from 'ionicons/icons';
import { of, throwError } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { ApexRole } from '../core/models';
import { SessionService } from '../core/session.service';
import { EntityListPage } from './entity-list.page';

describe('EntityListPage', () => {
  let activeRoles: ApexRole[];
  let entityKey: string;
  let api: jasmine.SpyObj<ApexApiService>;
  let fixture: ComponentFixture<EntityListPage>;
  let component: EntityListPage;

  beforeAll(() => {
    addIcons({
      add,
      'albums-outline': albumsOutline,
      'ban-outline': banOutline,
      'cash-outline': cashOutline,
      'checkmark-circle-outline': checkmarkCircleOutline,
      'chevron-back-outline': chevronBackOutline,
      'chevron-forward-outline': chevronForwardOutline,
      'cloud-offline-outline': cloudOfflineOutline,
      'create-outline': createOutline,
      'cube-outline': cubeOutline,
      'filter-outline': filterOutline,
      'layers-outline': layersOutline,
      'receipt-outline': receiptOutline,
      'sync-outline': syncOutline,
      'warning-outline': warningOutline
    });
  });

  function createPage(entity: string, roles: ApexRole[], initialRows: Array<Record<string, unknown>> = []): void {
    entityKey = entity;
    activeRoles = roles;

    api = jasmine.createSpyObj<ApexApiService>('ApexApiService', [
      'list',
      'create',
      'update',
      'patch',
      'pagarDespesa',
      'listTiposDespesa',
      'listPlanoContas',
      'listCentrosCusto'
    ]);
    api.list.and.returnValue(of(initialRows));
    api.create.and.returnValue(of({ idDespesa: 90, descricao: 'Nova despesa', valor: 100, status: 1 }));
    api.update.and.returnValue(of({ idDespesa: 90, descricao: 'Atualizada', valor: 100, status: 1 }));
    api.patch.and.returnValue(of({ idDespesa: 90, status: 0 }));
    api.pagarDespesa.and.returnValue(of({ idDespesa: 90, descricao: 'Paga', valor: 100, dataPagamento: '2026-11-17', status: 1 }));
    api.listTiposDespesa.and.returnValue(of([]));
    api.listPlanoContas.and.returnValue(of([]));
    api.listCentrosCusto.and.returnValue(of([]));

    const sessionStub = {
      hasAnyRole: (required?: readonly ApexRole[]) => {
        if (!required || required.length === 0) {
          return true;
        }
        const held = new Set(activeRoles);
        return required.some((role) => held.has(role));
      }
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [EntityListPage],
      providers: [
        { provide: ApexApiService, useValue: api },
        { provide: SessionService, useValue: sessionStub },
        { provide: ActivatedRoute, useValue: { data: of({ entity: entityKey }) } }
      ]
    });

    fixture = TestBed.createComponent(EntityListPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('remove status e dataPagamento do payload de despesas e não envia tipoDespesa fictício', () => {
    createPage('expenses', ['ROLE_FINANCEIRO']);

    component.formModel = {
      descricao: 'Conta de Água',
      valor: '180.5',
      status: 0,
      dataPagamento: '2026-11-10',
      dataCompetencia: '2026-11-01',
      dataVencimento: ''
    };

    component.save();

    expect(api.create).toHaveBeenCalledTimes(1);
    const payload = api.create.calls.mostRecent().args[1] as Record<string, unknown>;
    expect(payload['descricao']).toBe('Conta de Água');
    expect(payload['valor']).toBe(180.5);
    expect(payload['dataCompetencia']).toBe('2026-11-01');
    expect('status' in payload).toBeFalse();
    expect('dataPagamento' in payload).toBeFalse();
    expect('tipoDespesa' in payload).toBeFalse();
    expect('dataVencimento' in payload).toBeFalse();
  });

  it('mantém offline=false e exibe mensagem do backend em erros HTTP 400/403, marcando offline apenas em falha de rede', () => {
    createPage('expenses', ['ROLE_FINANCEIRO']);

    api.create.and.returnValue(
      throwError(() => ({ status: 400, error: { detail: 'Plano de contas incompatível com a despesa.' } }))
    );
    component.formModel = { descricao: 'Energia', valor: 200 };
    component.save();

    expect(component.offline()).toBeFalse();
    expect(component.saveState()).toBe('error');
    expect(component.formMessage()).toBe('Plano de contas incompatível com a despesa.');

    api.create.and.returnValue(throwError(() => ({ status: 403 })));
    component.save();

    expect(component.offline()).toBeFalse();
    expect(component.formMessage()).toBe('Nao foi possivel concluir a operacao.');

    api.create.and.returnValue(throwError(() => ({ status: 0 })));
    component.save();

    expect(component.offline()).toBeTrue();
    expect(component.formMessage()).toContain('Não foi possível salvar com a API');
  });

  it('calcula Valor aberto apenas sobre despesas ativas e não pagas, com hint ativas em aberto', () => {
    createPage('expenses', ['ROLE_FINANCEIRO'], [
      { idDespesa: 1, descricao: 'Aberta 1', valor: 300, status: 1 },
      { idDespesa: 2, descricao: 'Paga', valor: 500, status: 1, dataPagamento: '2026-11-10' },
      { idDespesa: 3, descricao: 'Cancelada', valor: 900, status: 0 },
      { idDespesa: 4, descricao: 'Aberta 2', valor: 150.5, status: 1, dataPagamento: null }
    ]);

    const openMetric = component.metrics().find((m) => m.label === 'Valor aberto');
    expect(openMetric).toBeDefined();
    expect(openMetric?.value.replace(/\u00a0/g, ' ')).toBe('R$ 450,50');
    expect(openMetric?.hint).toBe('ativas em aberto');
  });

  it('permite ROLE_CONTADOR gerenciar expenseTypes mantendo bloqueio em expenses', () => {
    createPage('expenseTypes', ['ROLE_CONTADOR']);
    expect(component.canManageEntity()).toBeTrue();

    createPage('expenses', ['ROLE_CONTADOR']);
    expect(component.canManageEntity()).toBeFalse();
  });
});
