import { EntityConfig } from './models';

export const ENTITY_CONFIGS: Record<string, EntityConfig> = {
  products: {
    key: 'products',
    title: 'Produtos / Estoque',
    subtitle: 'Cadastro do produto, saldo, estoque minimo, custo, preco, fornecedor e categoria.',
    endpoint: '/api/produtos',
    idKey: 'idProduto',
    searchKeys: ['idProduto', 'descricao', 'codigoInterno', 'codigoBarras', 'marca'],
    columns: [
      { key: 'idProduto', label: 'ID' },
      { key: 'codigoBarras', label: 'Codigo' },
      { key: 'descricao', label: 'Produto' },
      { key: 'marca', label: 'Marca' },
      { key: 'categoria.descricao', label: 'Categoria', format: 'nested' },
      { key: 'fornecedor.razaoSocial', label: 'Fornecedor', format: 'nested' },
      { key: 'quantidadeEstoque', label: 'Estoque' },
      { key: 'valorVenda', label: 'Venda', format: 'currency' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'descricao', label: 'Descricao do produto', type: 'text', required: true, group: 'Basico' },
      { key: 'codigoInterno', label: 'Codigo interno', type: 'text', group: 'Basico' },
      { key: 'codigoOriginal', label: 'Codigo original', type: 'text', group: 'Basico' },
      { key: 'codigoBarras', label: 'Codigo de barras', type: 'text', required: true, group: 'Basico' },
      { key: 'marca', label: 'Marca', type: 'text', group: 'Basico' },
      { key: 'unidadeMedida', label: 'Unidade de medida', type: 'text', placeholder: 'UN, CX, KG...', group: 'Basico' },
      { key: 'embalagem', label: 'Embalagem', type: 'text', group: 'Basico' },
      { key: 'quantidadeEstoque', label: 'Estoque atual', type: 'number', required: true, group: 'Estoque' },
      { key: 'estoqueMinimo', label: 'Estoque minimo', type: 'number', required: true, group: 'Estoque' },
      { key: 'localizacao', label: 'Localizacao', type: 'text', group: 'Estoque' },
      { key: 'custo', label: 'Custo', type: 'number', required: true, group: 'Precos' },
      { key: 'valorVenda', label: 'Valor de venda', type: 'number', required: true, group: 'Precos' },
      { key: 'margem', label: 'Margem %', type: 'number', group: 'Precos' },
      { key: 'categoria.idCategoria', label: 'ID da categoria', type: 'number', required: true, group: 'Fornecedores' },
      { key: 'fornecedor.idFornecedor', label: 'ID do fornecedor', type: 'number', required: true, group: 'Fornecedores' },
      { key: 'grupo', label: 'Grupo', type: 'text', group: 'Fornecedores' },
      { key: 'subgrupo', label: 'Subgrupo', type: 'text', group: 'Fornecedores' },
      { key: 'situacaoFiscal', label: 'Situacao fiscal', type: 'text', group: 'Fiscal' },
      { key: 'ncm', label: 'NCM', type: 'text', group: 'Fiscal' },
      { key: 'cfop', label: 'CFOP', type: 'text', group: 'Fiscal' },
      { key: 'icms', label: 'ICMS %', type: 'number', group: 'Fiscal' },
      { key: 'ipi', label: 'IPI %', type: 'number', group: 'Fiscal' },
      { key: 'pis', label: 'PIS %', type: 'number', group: 'Fiscal' },
      { key: 'cofins', label: 'COFINS %', type: 'number', group: 'Fiscal' },
      { key: 'dataAquisicao', label: 'Data de aquisicao', type: 'date', group: 'Historico' },
      { key: 'observacoes', label: 'Observacoes', type: 'text', group: 'Historico' }
    ],
    deactivatePath: (id) => `/api/produtos/${id}/desativar`
  },
  clients: {
    key: 'clients',
    title: 'Clientes CRM',
    subtitle: 'Dados gerais, contatos, enderecos, historico de compras, financeiro e observacoes.',
    endpoint: '/api/clientes',
    idKey: 'idCliente',
    searchKeys: ['idCliente', 'nomeRazao', 'telefone', 'cpfCnpj'],
    columns: [
      { key: 'idCliente', label: 'ID' },
      { key: 'nomeRazao', label: 'Nome/Razao' },
      { key: 'telefone', label: 'Telefone' },
      { key: 'cpfCnpj', label: 'CPF/CNPJ' },
      { key: 'enderecos.0.cidade', label: 'Cidade', format: 'nested' },
      { key: 'enderecos.0.uf', label: 'UF', format: 'nested' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'nomeRazao', label: 'Nome/Razao social', type: 'text', required: true, group: 'Basico' },
      { key: 'nomeFantasia', label: 'Nome fantasia', type: 'text', group: 'Basico' },
      { key: 'tipoDocumento', label: 'Tipo de documento', type: 'number', required: true, placeholder: '1 CPF, 2 CNPJ', group: 'Basico' },
      { key: 'cpfCnpj', label: 'CPF/CNPJ', type: 'text', required: true, group: 'Basico' },
      { key: 'rgIe', label: 'RG/IE', type: 'text', group: 'Fiscal' },
      { key: 'situacaoFiscal', label: 'Situacao fiscal', type: 'text', group: 'Fiscal' },
      { key: 'telefone', label: 'Telefone', type: 'text', required: true, group: 'Contatos' },
      { key: 'celular', label: 'Celular', type: 'text', group: 'Contatos' },
      { key: 'email', label: 'E-mail', type: 'text', group: 'Contatos' },
      { key: 'endereco.cep', label: 'CEP', type: 'text', required: true, group: 'Endereco' },
      { key: 'endereco.uf', label: 'UF', type: 'text', group: 'Endereco' },
      { key: 'endereco.estado', label: 'Estado', type: 'text', group: 'Endereco' },
      { key: 'endereco.cidade', label: 'Cidade', type: 'text', group: 'Endereco' },
      { key: 'endereco.bairro', label: 'Bairro', type: 'text', group: 'Endereco' },
      { key: 'endereco.logradouro', label: 'Logradouro', type: 'text', group: 'Endereco' },
      { key: 'endereco.numero', label: 'Numero', type: 'text', group: 'Endereco' },
      { key: 'endereco.complemento', label: 'Complemento', type: 'text', group: 'Endereco' },
      { key: 'observacoes', label: 'Observacoes', type: 'text', group: 'Comercial' }
    ],
    deactivatePath: (id) => `/api/clientes/${id}/desativar`
  },
  suppliers: {
    key: 'suppliers',
    title: 'Fornecedores',
    subtitle: 'Cadastro operacional para compras, XML de NF e reposicao.',
    endpoint: '/api/fornecedores',
    idKey: 'idFornecedor',
    searchKeys: ['idFornecedor', 'razaoSocial', 'nomeFantasia', 'cnpj'],
    columns: [
      { key: 'idFornecedor', label: 'ID' },
      { key: 'razaoSocial', label: 'Razao social' },
      { key: 'nomeFantasia', label: 'Fantasia' },
      { key: 'cnpj', label: 'CNPJ' },
      { key: 'telefone', label: 'Telefone' },
      { key: 'endereco.cidade', label: 'Cidade', format: 'nested' },
      { key: 'endereco.uf', label: 'UF', format: 'nested' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'razaoSocial', label: 'Razao social', type: 'text', required: true, group: 'Basico' },
      { key: 'nomeFantasia', label: 'Nome fantasia', type: 'text', required: true, group: 'Basico' },
      { key: 'cnpj', label: 'CNPJ', type: 'text', required: true, group: 'Fiscal' },
      { key: 'inscricaoEstadual', label: 'Inscricao estadual', type: 'text', group: 'Fiscal' },
      { key: 'situacaoFiscal', label: 'Situacao fiscal', type: 'text', group: 'Fiscal' },
      { key: 'telefone', label: 'Telefone', type: 'text', required: true, group: 'Contatos' },
      { key: 'celular', label: 'Celular', type: 'text', group: 'Contatos' },
      { key: 'email', label: 'E-mail', type: 'text', group: 'Contatos' },
      { key: 'endereco.cep', label: 'CEP', type: 'text', required: true, group: 'Endereco' },
      { key: 'endereco.uf', label: 'UF', type: 'text', group: 'Endereco' },
      { key: 'endereco.estado', label: 'Estado', type: 'text', group: 'Endereco' },
      { key: 'endereco.cidade', label: 'Cidade', type: 'text', group: 'Endereco' },
      { key: 'endereco.bairro', label: 'Bairro', type: 'text', group: 'Endereco' },
      { key: 'endereco.logradouro', label: 'Logradouro', type: 'text', group: 'Endereco' },
      { key: 'endereco.numero', label: 'Numero', type: 'text', group: 'Endereco' },
      { key: 'endereco.complemento', label: 'Complemento', type: 'text', group: 'Endereco' },
      { key: 'condicoesPagamento', label: 'Condicoes de pagamento', type: 'text', group: 'Comercial' },
      { key: 'dadosBancarios', label: 'Dados bancarios', type: 'text', group: 'Bancario' },
      { key: 'observacoes', label: 'Observacoes', type: 'text', group: 'Historico' }
    ],
    deactivatePath: (id) => `/api/fornecedores/${id}/desativar`
  },
  users: {
    key: 'users',
    title: 'Funcionarios / Ponto',
    subtitle: 'Usuarios internos, perfis RBAC e base para automacao de jornada.',
    endpoint: '/api/usuarios',
    idKey: 'idUsuario',
    searchKeys: ['idUsuario', 'nome', 'login'],
    columns: [
      { key: 'idUsuario', label: 'ID' },
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
    title: 'Financeiro',
    subtitle: 'Despesas, vencimentos, contas e fluxo diario de caixa.',
    endpoint: '/api/despesas',
    idKey: 'idDespesa',
    searchKeys: ['idDespesa', 'descricao'],
    columns: [
      { key: 'idDespesa', label: 'ID' },
      { key: 'descricao', label: 'Descricao' },
      { key: 'valor', label: 'Valor', format: 'currency' },
      { key: 'dataVencimento', label: 'Vencimento', format: 'date' },
      { key: 'tipoDespesa.nome', label: 'Tipo', format: 'nested' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'descricao', label: 'Descricao', type: 'text', required: true },
      { key: 'valor', label: 'Valor', type: 'number', required: true },
      { key: 'dataVencimento', label: 'Vencimento', type: 'date' }
    ],
    deactivatePath: (id) => `/api/despesas/${id}/cancelar`
  },
  expenseTypes: {
    key: 'expenseTypes',
    title: 'Parametros Financeiros',
    subtitle: 'Classificacoes usadas em DRE, fluxo de caixa e relatorios.',
    endpoint: '/api/despesas/tipos',
    idKey: 'idTipoDespesa',
    searchKeys: ['idTipoDespesa', 'nome'],
    columns: [
      { key: 'idTipoDespesa', label: 'ID' },
      { key: 'nome', label: 'Nome' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'nome', label: 'Nome', type: 'text', required: true }
    ]
  },
  paymentMethods: {
    key: 'paymentMethods',
    title: 'Formas de Pagamento',
    subtitle: 'Parametros do PDV e caixa para recebimentos, parcelamentos e conciliacao.',
    endpoint: '/api/formaspagamento',
    idKey: 'idFormaPagamento',
    searchKeys: ['idFormaPagamento', 'nome', 'descricao'],
    columns: [
      { key: 'idFormaPagamento', label: 'ID' },
      { key: 'nome', label: 'Nome' },
      { key: 'descricao', label: 'Descricao' },
      { key: 'tipoPagamento', label: 'Tipo' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'nome', label: 'Nome', type: 'text', required: true },
      { key: 'descricao', label: 'Descricao', type: 'text', required: true },
      { key: 'tipoPagamento', label: 'Tipo', type: 'number', required: true }
    ],
    deactivatePath: (id) => `/api/formaspagamento/${id}/desativar`
  },
  categories: {
    key: 'categories',
    title: 'Categorias de Produtos',
    subtitle: 'Classificacao principal do catalogo para busca, relatorio e reposicao.',
    endpoint: '/api/categorias',
    idKey: 'idCategoria',
    searchKeys: ['idCategoria', 'descricao'],
    columns: [
      { key: 'idCategoria', label: 'ID' },
      { key: 'descricao', label: 'Descricao' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'descricao', label: 'Descricao', type: 'text', required: true }
    ],
    deactivatePath: (id) => `/api/categorias/${id}/desativar`
  },
  productGroups: {
    key: 'productGroups',
    title: 'Grupos de Produtos',
    subtitle: 'Agrupamentos comerciais e operacionais para evolucao do estoque e BI.',
    endpoint: '/api/grupos-produtos',
    idKey: 'idGrupoProduto',
    searchKeys: ['idGrupoProduto', 'nome', 'descricao'],
    columns: [
      { key: 'idGrupoProduto', label: 'ID' },
      { key: 'nome', label: 'Nome' },
      { key: 'descricao', label: 'Descricao' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'nome', label: 'Nome', type: 'text', required: true },
      { key: 'descricao', label: 'Descricao', type: 'text' }
    ],
    deactivatePath: (id) => `/api/grupos-produtos/${id}/desativar`
  },
  menus: {
    key: 'menus',
    title: 'Menus',
    subtitle: 'Cadastro administrativo de rotas exibidas e icones do sistema.',
    endpoint: '/api/menus',
    idKey: 'idMenu',
    searchKeys: ['idMenu', 'nome', 'link', 'icone'],
    columns: [
      { key: 'idMenu', label: 'ID' },
      { key: 'nome', label: 'Nome' },
      { key: 'link', label: 'Link' },
      { key: 'icone', label: 'Icone' },
      { key: 'exibir', label: 'Exibir', format: 'status' }
    ],
    fields: [
      { key: 'nome', label: 'Nome', type: 'text', required: true },
      { key: 'link', label: 'Link', type: 'text', required: true },
      { key: 'icone', label: 'Icone', type: 'text' },
      { key: 'exibir', label: 'Exibir', type: 'number' }
    ],
    deactivatePath: (id) => `/api/menus/${id}/desativar`
  },
  profiles: {
    key: 'profiles',
    title: 'Perfis e Permissoes',
    subtitle: 'Perfis de acesso usados pelo RBAC e pelo vinculo de menus.',
    endpoint: '/api/perfis',
    idKey: 'idPerfil',
    searchKeys: ['idPerfil', 'nome'],
    columns: [
      { key: 'idPerfil', label: 'ID' },
      { key: 'nome', label: 'Nome' },
      { key: 'status', label: 'Status', format: 'status' }
    ],
    fields: [
      { key: 'nome', label: 'Nome', type: 'text', required: true }
    ],
    deactivatePath: (id) => `/api/perfis/${id}/desativar`
  }
};
