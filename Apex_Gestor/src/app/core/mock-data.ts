import { Cliente, Despesa, FormaPagamento, Produto, RelatorioFinanceiro, TipoDespesa, Usuario } from './models';

export const MOCK_PRODUCTS: Produto[] = [
  {
    idProduto: 1,
    descricao: 'Cimento CP II 50kg',
    codigoBarras: '7891000000010',
    marca: 'Votoran',
    unidadeMedida: 'SC',
    quantidadeEstoque: 128,
    estoqueMinimo: 40,
    valorVenda: 42.9,
    custo: 31.5,
    status: 1,
    categoria: { idCategoria: 1, descricao: 'Construção', status: 1 }
  },
  {
    idProduto: 2,
    descricao: 'Furadeira de Impacto 650W',
    codigoBarras: '7891000000027',
    marca: 'Bosch',
    unidadeMedida: 'UN',
    quantidadeEstoque: 18,
    estoqueMinimo: 6,
    valorVenda: 329.9,
    custo: 226,
    status: 1,
    categoria: { idCategoria: 2, descricao: 'Ferramentas', status: 1 }
  },
  {
    idProduto: 3,
    descricao: 'Tinta Acrílica Premium 18L',
    codigoBarras: '7891000000034',
    marca: 'Coral',
    unidadeMedida: 'GL',
    quantidadeEstoque: 22,
    estoqueMinimo: 12,
    valorVenda: 289,
    custo: 198,
    status: 1,
    categoria: { idCategoria: 3, descricao: 'Pintura', status: 1 }
  }
];

export const MOCK_CLIENTS: Cliente[] = [
  { idCliente: 1, nomeRazao: 'João Silva', telefone: '(11) 99999-1010', cpfCnpj: '12345678909', status: 1 },
  { idCliente: 2, nomeRazao: 'Maria Santos', telefone: '(11) 98888-2020', cpfCnpj: '98765432100', status: 1 }
];

export const MOCK_USERS: Usuario[] = [
  { idUsuario: 1, nome: 'Yuri Alcantara', login: 'yuri', status: 1, perfil: { nome: 'Gerente' } },
  { idUsuario: 2, nome: 'Caixa Loja 01', login: 'caixa01', status: 1, perfil: { nome: 'Vendedor' } }
];

export const MOCK_PAYMENT_METHODS: FormaPagamento[] = [
  { idFormaPagamento: 1, nome: 'Dinheiro', descricao: 'Pagamento em espécie', tipoPagamento: 1, status: 1 },
  { idFormaPagamento: 2, nome: 'PIX', descricao: 'Pagamento instantâneo', tipoPagamento: 2, status: 1 },
  { idFormaPagamento: 3, nome: 'Cartão de Crédito', descricao: 'Crédito parcelado', tipoPagamento: 3, status: 1 }
];

export const MOCK_EXPENSE_TYPES: TipoDespesa[] = [
  { idTipoDespesa: 1, nome: 'Aluguel', status: 1 },
  { idTipoDespesa: 2, nome: 'Folha de pagamento', status: 1 }
];

export const MOCK_EXPENSES: Despesa[] = [
  { idDespesa: 1, descricao: 'Aluguel da loja', valor: 4200, dataVencimento: '2026-05-10', status: 1, tipoDespesa: MOCK_EXPENSE_TYPES[0] },
  { idDespesa: 2, descricao: 'Campanha de ofertas', valor: 1350, dataVencimento: '2026-05-18', status: 1, tipoDespesa: MOCK_EXPENSE_TYPES[1] }
];

export const MOCK_REPORT: RelatorioFinanceiro = {
  totalVendas: 89640,
  totalCustosProdutos: 52120,
  totalDespesas: 17850,
  lucroLiquido: 19670,
  saldoCaixa: 128300
};
