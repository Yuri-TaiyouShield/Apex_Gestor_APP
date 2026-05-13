import { EntityConfig } from './models';

export const ENTITY_CONFIGS: Record<string, EntityConfig> = {
  products: {
    key: 'products',
    title: 'Catálogo de Produtos',
    subtitle: 'Produtos com estoque, preço de venda, fornecedor e categoria.',
    endpoint: '/api/produtos',
    idKey: 'idProduto',
    searchKeys: ['descricao', 'codigoBarras', 'marca'],
    columns: [
      { key: 'codigoBarras', label: 'Código' },
      { key: 'descricao', label: 'Produto' },
      { key: 'marca', label: 'Marca' },
      { key: 'quantidadeEstoque', label: 'Estoque' },
      { key: 'valorVenda', label: 'Venda', format: 'currency' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'descricao', label: 'Descrição', type: 'text', required: true },
      { key: 'codigoBarras', label: 'Código de barras', type: 'text' },
      { key: 'marca', label: 'Marca', type: 'text' },
      { key: 'quantidadeEstoque', label: 'Estoque', type: 'number', required: true },
      { key: 'estoqueMinimo', label: 'Estoque mínimo', type: 'number' },
      { key: 'valorVenda', label: 'Valor de venda', type: 'number', required: true },
      { key: 'custo', label: 'Custo', type: 'number' }
    ],
    deactivatePath: (id) => `/api/produtos/${id}/desativar`
  },
  clients: {
    key: 'clients',
    title: 'Clientes CRM',
    subtitle: 'Base B2C e clientes de balcão com histórico de relacionamento.',
    endpoint: '/api/clientes',
    idKey: 'idCliente',
    searchKeys: ['nomeRazao', 'telefone', 'cpfCnpj'],
    columns: [
      { key: 'nomeRazao', label: 'Nome/Razão' },
      { key: 'telefone', label: 'Telefone' },
      { key: 'cpfCnpj', label: 'CPF/CNPJ' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'nomeRazao', label: 'Nome/Razão social', type: 'text', required: true },
      { key: 'telefone', label: 'Telefone', type: 'text' },
      { key: 'cpfCnpj', label: 'CPF/CNPJ', type: 'text' }
    ],
    deactivatePath: (id) => `/api/clientes/${id}/desativar`
  },
  suppliers: {
    key: 'suppliers',
    title: 'Fornecedores',
    subtitle: 'Cadastro operacional para compras, XML de NF e reposição.',
    endpoint: '/api/fornecedores',
    idKey: 'idFornecedor',
    searchKeys: ['razaoSocial', 'nomeFantasia', 'cnpj'],
    columns: [
      { key: 'razaoSocial', label: 'Razão social' },
      { key: 'nomeFantasia', label: 'Fantasia' },
      { key: 'cnpj', label: 'CNPJ' },
      { key: 'telefone', label: 'Telefone' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'razaoSocial', label: 'Razão social', type: 'text', required: true },
      { key: 'nomeFantasia', label: 'Nome fantasia', type: 'text' },
      { key: 'cnpj', label: 'CNPJ', type: 'text', required: true },
      { key: 'telefone', label: 'Telefone', type: 'text' }
    ],
    deactivatePath: (id) => `/api/fornecedores/${id}/desativar`
  },
  users: {
    key: 'users',
    title: 'Funcionários e RBAC',
    subtitle: 'Usuários internos com perfis de vendedor, gerente e administrador.',
    endpoint: '/api/usuarios',
    idKey: 'idUsuario',
    searchKeys: ['nome', 'login'],
    columns: [
      { key: 'nome', label: 'Nome' },
      { key: 'login', label: 'Login' },
      { key: 'perfil.nome', label: 'Perfil', format: 'nested' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'nome', label: 'Nome', type: 'text', required: true },
      { key: 'login', label: 'Login', type: 'text', required: true },
      { key: 'senha', label: 'Senha inicial', type: 'text', required: true },
      { key: 'dataNascimento', label: 'Data de nascimento', type: 'date' }
    ],
    deactivatePath: (id) => `/api/usuarios/${id}/desativar`
  },
  expenses: {
    key: 'expenses',
    title: 'Contas a Pagar',
    subtitle: 'Despesas, vencimentos e fluxo diário de caixa.',
    endpoint: '/api/despesas',
    idKey: 'idDespesa',
    searchKeys: ['descricao'],
    columns: [
      { key: 'descricao', label: 'Descrição' },
      { key: 'valor', label: 'Valor', format: 'currency' },
      { key: 'dataVencimento', label: 'Vencimento', format: 'date' },
      { key: 'tipoDespesa.nome', label: 'Tipo', format: 'nested' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'descricao', label: 'Descrição', type: 'text', required: true },
      { key: 'valor', label: 'Valor', type: 'number', required: true },
      { key: 'dataVencimento', label: 'Vencimento', type: 'date' }
    ],
    deactivatePath: (id) => `/api/despesas/${id}/cancelar`
  },
  expenseTypes: {
    key: 'expenseTypes',
    title: 'Tipos de Despesa',
    subtitle: 'Classificações usadas em DRE, fluxo de caixa e relatórios.',
    endpoint: '/api/despesas/tipos',
    idKey: 'idTipoDespesa',
    searchKeys: ['nome'],
    columns: [
      { key: 'nome', label: 'Nome' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'nome', label: 'Nome', type: 'text', required: true }
    ]
  }
};
