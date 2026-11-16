import { ENTITY_CONFIGS } from './entity-config';
import { APEX_NAV_ITEMS } from './feature-menu.config';

describe('ERP operational configuration', () => {
  it('keeps LGPD as a transversal layer outside the operational menu', () => {
    const labels = APEX_NAV_ITEMS.map((item) => item.label.toLowerCase());

    expect(labels.some((label) => label.includes('lgpd'))).toBeFalse();
    expect(labels.some((label) => label.includes('privacidade'))).toBeFalse();
  });

  it('keeps cashier hidden from seller-only navigation permissions', () => {
    const cashier = APEX_NAV_ITEMS.find((item) => item.path === '/cashier');

    expect(cashier?.allowedRoles).toContain('ROLE_CAIXA');
    expect(cashier?.allowedRoles).not.toContain('ROLE_VENDEDOR');
  });

  it('uses operational tabs for product, customer and supplier forms', () => {
    expect(groups('products')).toEqual(jasmine.arrayContaining(['Basico', 'Estoque', 'Precos', 'Fiscal']));
    expect(groups('clients')).toEqual(jasmine.arrayContaining(['Basico', 'Endereco', 'Contatos', 'Fiscal']));
    expect(groups('suppliers')).toEqual(jasmine.arrayContaining(['Basico', 'Endereco', 'Contatos', 'Fiscal', 'Bancario']));
  });

  it('requires barcode and exposes visual IDs in operational grids', () => {
    expect(ENTITY_CONFIGS['products'].fields.find((field) => field.key === 'codigoBarras')?.required).toBeTrue();

    expect(columnKeys('products')).toContain('idProduto');
    expect(columnKeys('clients')).toContain('idCliente');
    expect(columnKeys('suppliers')).toContain('idFornecedor');
    expect(columnKeys('paymentMethods')).toContain('idFormaPagamento');
    expect(columnKeys('categories')).toContain('idCategoria');
    expect(columnKeys('productGroups')).toContain('idGrupoProduto');
  });

  it('exposes sales history to seller without exposing cashier or finance', () => {
    const history = APEX_NAV_ITEMS.find((item) => item.path === '/sales-history');
    const cashier = APEX_NAV_ITEMS.find((item) => item.path === '/cashier');
    const finance = APEX_NAV_ITEMS.find((item) => item.path === '/finance-operations');

    expect(history?.allowedRoles).toContain('ROLE_VENDEDOR');
    expect(cashier?.allowedRoles).not.toContain('ROLE_VENDEDOR');
    expect(finance?.allowedRoles).not.toContain('ROLE_VENDEDOR');
  });

  it('declares required fields for the critical registration forms', () => {
    expect(requiredFields('products')).toEqual(jasmine.arrayContaining([
      'descricao',
      'codigoBarras',
      'quantidadeEstoque',
      'estoqueMinimo',
      'custo',
      'valorVenda',
      'categoria.idCategoria',
      'fornecedor.idFornecedor'
    ]));
    expect(requiredFields('clients')).toEqual(jasmine.arrayContaining(['nomeRazao', 'tipoDocumento', 'cpfCnpj', 'telefone', 'endereco.cep']));
    expect(requiredFields('suppliers')).toEqual(jasmine.arrayContaining(['razaoSocial', 'cnpj', 'telefone', 'endereco.cep']));
    expect(requiredFields('expenses')).toEqual(['descricao', 'valor']);
    expect(requiredFields('users')).toEqual(jasmine.arrayContaining(['nome', 'login', 'senha']));
  });

  it('builds deactivation paths per entity, keeping expenses with cancel semantics', () => {
    expect(ENTITY_CONFIGS['products'].deactivatePath?.(7)).toBe('/api/produtos/7/desativar');
    expect(ENTITY_CONFIGS['clients'].deactivatePath?.(4)).toBe('/api/clientes/4/desativar');
    expect(ENTITY_CONFIGS['expenses'].deactivatePath?.(3)).toBe('/api/despesas/3/cancelar');
    expect(ENTITY_CONFIGS['expenseTypes'].deactivatePath).toBeUndefined();
  });

  it('keeps every entity searchable by its own identity key behind an operational endpoint', () => {
    for (const config of Object.values(ENTITY_CONFIGS)) {
      expect(config.searchKeys).toContain(config.idKey);
      expect(config.endpoint.startsWith('/api/')).toBeTrue();
      expect(config.fields.length).toBeGreaterThan(0);
      expect(config.columns.length).toBeGreaterThan(0);
    }
  });
});

function groups(key: keyof typeof ENTITY_CONFIGS): string[] {
  return Array.from(new Set(ENTITY_CONFIGS[key].fields.map((field) => field.group || 'Dados')));
}

function columnKeys(key: keyof typeof ENTITY_CONFIGS): string[] {
  return ENTITY_CONFIGS[key].columns.map((column) => column.key);
}

function requiredFields(key: keyof typeof ENTITY_CONFIGS): string[] {
  return ENTITY_CONFIGS[key].fields.filter((field) => field.required === true).map((field) => field.key);
}

