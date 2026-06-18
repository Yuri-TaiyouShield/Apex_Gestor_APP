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
});

function groups(key: keyof typeof ENTITY_CONFIGS): string[] {
  return Array.from(new Set(ENTITY_CONFIGS[key].fields.map((field) => field.group || 'Dados')));
}

function columnKeys(key: keyof typeof ENTITY_CONFIGS): string[] {
  return ENTITY_CONFIGS[key].columns.map((column) => column.key);
}
