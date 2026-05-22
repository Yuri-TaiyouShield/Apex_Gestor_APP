const shell = document.getElementById('shell');
const toast = document.getElementById('toast');
const screenTitle = document.getElementById('screenTitle');
const cartList = document.getElementById('cartList');
const cartTotal = document.getElementById('cartTotal');
const result = document.getElementById('admcalcResult');
const domainMenu = document.getElementById('domainMenu');
const apiBaseUrl = window.apexDesktop?.apiBaseUrl || 'http://localhost:8080';
const cart = [];
let activeFeature = null;

const groups = [
  { id: 'comercial', label: 'Comercial', icon: 'CO', features: ['clientes', 'enderecos', 'vendas', 'itens-venda'] },
  { id: 'estoqueFiscal', label: 'Estoque e Fiscal', icon: 'EF', features: ['produtos', 'categorias', 'nf-entrada', 'itens-nf'] },
  { id: 'financas', label: 'Finanças', icon: 'FI', features: ['despesas', 'tipos-despesa', 'formas-pagamento', 'pagamentos-venda', 'balanco', 'admcalc'] },
  { id: 'administracao', label: 'Administração', icon: 'AD', features: ['usuarios', 'perfis', 'menus'] }
];

const features = [
  { key: 'clientes', group: 'comercial', icon: 'CL', label: 'Clientes CRM', className: 'Cliente.java', endpoint: '/api/clientes', summary: 'Cadastro, consulta, edição, desativação e vínculo de endereços.', operations: ['Listar clientes', 'Criar cliente', 'Editar cadastro', 'Desativar cliente', 'Vincular endereço'], fields: ['idCliente', 'nomeRazao', 'telefone', 'cpfCnpj', 'status'] },
  { key: 'enderecos', group: 'comercial', icon: 'EN', label: 'Endereços', className: 'Endereco.java', endpoint: '/api/enderecos', summary: 'Base de entrega e cobrança com CEP, cidade, UF e logradouro.', operations: ['Listar endereços', 'Criar endereço', 'Editar endereço', 'Excluir endereço', 'Validar CEP'], fields: ['idEndereco', 'cep', 'cidade', 'logradouro', 'uf'] },
  { key: 'vendas', group: 'comercial', icon: 'VD', label: 'Vendas', className: 'Venda.java', endpoint: '/api/vendas', summary: 'Venda, cliente, operador, itens, pagamentos, desconto e cancelamento.', operations: ['Listar vendas', 'Realizar venda', 'Cancelar venda', 'Conferir desconto', 'Auditar baixa de estoque'], fields: ['idVenda', 'cliente', 'usuario', 'valorTotal', 'itens', 'pagamentos'] },
  { key: 'itens-venda', group: 'comercial', icon: 'IV', label: 'Itens de Venda', className: 'ProdutoVenda.java + ProdutoVendaId.java', endpoint: '/api/itensvenda', summary: 'Chave composta produto/venda, quantidade, custo e preço total.', operations: ['Listar itens', 'Buscar chave composta', 'Conferir quantidade', 'Validar preço total'], fields: ['produtoId', 'vendaId', 'quantidade', 'precoUnitario', 'precoTotal'] },
  { key: 'produtos', group: 'estoqueFiscal', icon: 'PR', label: 'Produtos', className: 'Produto.java', endpoint: '/api/produtos', summary: 'Cadastro, custo, venda, estoque mínimo, categoria e fornecedor.', operations: ['Listar produtos', 'Criar produto', 'Editar preço/custo', 'Desativar produto', 'Monitorar estoque'], fields: ['idProduto', 'descricao', 'custo', 'valorVenda', 'quantidadeEstoque', 'estoqueMinimo'] },
  { key: 'categorias', group: 'estoqueFiscal', icon: 'CT', label: 'Categorias', className: 'Categoria.java', endpoint: '/api/categorias', summary: 'Classificação comercial e fiscal do catálogo.', operations: ['Listar categorias', 'Criar categoria', 'Editar categoria', 'Desativar categoria'], fields: ['idCategoria', 'descricao', 'status'] },
  { key: 'nf-entrada', group: 'estoqueFiscal', icon: 'NF', label: 'NF de Entrada', className: 'NotaFiscalEntrada.java', endpoint: '/api/nfs/entrada', summary: 'Entrada fiscal com fornecedor, valor total, itens e atualização de estoque.', operations: ['Importar XML', 'Registrar entrada', 'Conferir totais', 'Atualizar custo médio', 'Gerar auditoria fiscal'], fields: ['idNfEntrada', 'numero', 'serie', 'fornecedor', 'valorTotal', 'itens'] },
  { key: 'itens-nf', group: 'estoqueFiscal', icon: 'IN', label: 'Itens da NF', className: 'ItemNotaFiscal.java', endpoint: '/api/nfs/entrada', summary: 'Itens importados da nota fiscal com produto, quantidade e custo unitário.', operations: ['Conferir itens', 'Vincular produto', 'Ajustar quantidade', 'Validar custo unitário'], fields: ['idItemNf', 'produto', 'quantidade', 'valorCustoUnitario'] },
  { key: 'despesas', group: 'financas', icon: 'DP', label: 'Gestão de Despesas', className: 'Despesa.java', endpoint: '/api/despesas', summary: 'Contas a pagar, vencimentos, pagamentos, tipos e cancelamentos.', operations: ['Listar despesas', 'Criar despesa', 'Cancelar despesa', 'Conferir vencimentos', 'Classificar por tipo'], fields: ['idDespesa', 'descricao', 'valor', 'dataVencimento', 'tipoDespesa', 'status'] },
  { key: 'tipos-despesa', group: 'financas', icon: 'TD', label: 'Tipos de Despesa', className: 'TipoDespesa.java', endpoint: '/api/despesas/tipos', summary: 'Taxonomia financeira para DRE, balanço e centros de custo.', operations: ['Listar tipos', 'Criar tipo', 'Ativar/desativar tipo', 'Padronizar centro de custo'], fields: ['idTipoDespesa', 'nome', 'status'] },
  { key: 'formas-pagamento', group: 'financas', icon: 'FP', label: 'Formas de Pagamento', className: 'FormaPagamento.java', endpoint: '/api/formaspagamento', summary: 'Meios de pagamento, tipo, status e regras de liquidação.', operations: ['Listar formas', 'Criar forma', 'Editar regra', 'Desativar forma', 'Vincular ao PDV'], fields: ['idFormaPagamento', 'nome', 'descricao', 'tipoPagamento', 'status'] },
  { key: 'pagamentos-venda', group: 'financas', icon: 'PV', label: 'Pagamentos de Venda', className: 'VendaPagamento.java + VendaPagamentoId.java', endpoint: '/api/pagamentosvenda', summary: 'Chave composta venda/forma de pagamento, valor pago e parcelas.', operations: ['Listar pagamentos', 'Buscar chave composta', 'Conferir parcelas', 'Conciliar valor pago'], fields: ['vendaId', 'formaPagamentoId', 'valorPago', 'numeroParcelas'] },
  { key: 'balanco', group: 'financas', icon: 'BL', label: 'Balanço e Relatórios', className: 'RelatorioFinanceiroDTO.java', endpoint: '/api/relatorios/financeiro?inicio=2026-05-01&fim=2026-05-31', summary: 'Receita, CMV, despesas, lucro líquido e saldo de caixa.', operations: ['Gerar balanço', 'Exportar DRE', 'Auditar CMV', 'Projetar caixa'], fields: ['totalVendas', 'totalCustosProdutos', 'totalDespesas', 'lucroLiquido', 'saldoCaixa'] },
  { key: 'admcalc', group: 'financas', icon: 'AC', label: 'AdmCalc Financeiro', className: 'AdmCalcController.java', endpoint: '/api/admcalc/calcular', summary: 'Cálculos trabalhistas, benefícios, férias, 13º e Fator R.', operations: ['Calcular folha', 'Simular Fator R', 'Exportar demonstrativo'], fields: ['salarioMensal', 'jornadaMensal', 'horasExtras', 'receitaBruta'] },
  { key: 'usuarios', group: 'administracao', icon: 'US', label: 'Usuários', className: 'Usuario.java', endpoint: '/api/usuarios', summary: 'Operadores, login, status e vínculo com perfil RBAC.', operations: ['Listar usuários', 'Criar usuário', 'Editar usuário', 'Desativar usuário', 'Buscar por login'], fields: ['idUsuario', 'nome', 'login', 'dataNascimento', 'perfil'] },
  { key: 'perfis', group: 'administracao', icon: 'PF', label: 'Perfis RBAC', className: 'Perfil.java', endpoint: '/api/perfis', summary: 'Papéis de acesso e governança de permissões por menus.', operations: ['Listar perfis', 'Criar perfil', 'Editar perfil', 'Desativar perfil', 'Vincular menus'], fields: ['idPerfil', 'nome', 'status', 'menus'] },
  { key: 'menus', group: 'administracao', icon: 'MN', label: 'Menus do Sistema', className: 'Menu.java', endpoint: '/api/menus', summary: 'Catálogo de menus, links, ícones, exibição e perfis autorizados.', operations: ['Listar menus', 'Criar menu', 'Editar menu', 'Desativar menu', 'Controlar exibição'], fields: ['idMenu', 'nome', 'link', 'icone', 'exibir', 'perfis'] }
];

const titles = {
  pdv: 'PDV de alta produtividade',
  admcalc: 'AdmCalc financeiro',
  relatorios: 'Relatórios desktop',
  domain: 'Domínio operacional'
};

function money(value) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function featureByKey(key) {
  return features.find((feature) => feature.key === key);
}

function setToast(message) {
  toast.textContent = message;
}

function activateView(view) {
  document.querySelectorAll('.view').forEach((item) => item.classList.toggle('active', item.id === `${view}View`));
  document.querySelectorAll('.nav-item[data-view]').forEach((button) => button.classList.toggle('active', button.dataset.view === view));
}

function setView(view) {
  activeFeature = null;
  activateView(view);
  document.querySelectorAll('.nested-item').forEach((button) => button.classList.remove('active'));
  screenTitle.textContent = titles[view] || 'Apex Gestor';
  setToast(`Tela ${screenTitle.textContent} carregada.`);
}

function setFeature(key) {
  const feature = featureByKey(key);
  if (!feature) return;
  if (feature.key === 'admcalc') {
    setView('admcalc');
    document.querySelectorAll('.nested-item').forEach((button) => button.classList.toggle('active', button.dataset.feature === key));
    return;
  }
  activeFeature = feature;
  activateView('domain');
  document.querySelectorAll('.nav-item[data-view]').forEach((button) => button.classList.remove('active'));
  document.querySelectorAll('.nested-item').forEach((button) => button.classList.toggle('active', button.dataset.feature === key));
  screenTitle.textContent = feature.label;
  renderDomain(feature);
  setToast(`${feature.label} carregado com operações de ${feature.className}.`);
}

function makeButton(className, text, onClick) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = className;
  button.textContent = text;
  button.addEventListener('click', onClick);
  return button;
}

function buildDomainMenu() {
  groups.forEach((group) => {
    const section = document.createElement('section');
    section.className = 'nav-group open';

    const toggle = document.createElement('button');
    toggle.type = 'button';
    toggle.className = 'nav-item group-toggle';
    const icon = document.createElement('span');
    icon.textContent = group.icon;
    const label = document.createElement('strong');
    label.textContent = group.label;
    const marker = document.createElement('em');
    marker.textContent = '−';
    toggle.append(icon, label, marker);
    toggle.addEventListener('click', () => {
      section.classList.toggle('open');
      marker.textContent = section.classList.contains('open') ? '−' : '+';
    });

    const nested = document.createElement('div');
    nested.className = 'nested-menu';
    group.features.map(featureByKey).filter(Boolean).forEach((feature) => {
      const item = document.createElement('button');
      item.type = 'button';
      item.className = 'nested-item';
      item.dataset.feature = feature.key;
      const itemIcon = document.createElement('span');
      itemIcon.textContent = feature.icon;
      const itemLabel = document.createElement('strong');
      itemLabel.textContent = feature.label;
      item.append(itemIcon, itemLabel);
      item.addEventListener('click', () => setFeature(feature.key));
      nested.appendChild(item);
    });

    section.append(toggle, nested);
    domainMenu.appendChild(section);
  });
}

function renderDomain(feature) {
  document.getElementById('domainIcon').textContent = feature.icon;
  document.getElementById('domainClass').textContent = feature.className;
  document.getElementById('domainTitle').textContent = feature.label;
  document.getElementById('domainSummary').textContent = feature.summary;
  document.getElementById('domainEndpoint').textContent = feature.endpoint;
  document.getElementById('domainSyncState').textContent = 'Ainda não sincronizado';
  document.getElementById('domainActionLog').textContent = 'Selecione uma operação para iniciar o fluxo.';

  const actions = document.getElementById('domainActions');
  actions.replaceChildren();
  feature.operations.forEach((operation) => {
    actions.appendChild(makeButton('action-button', operation, () => runDomainOperation(feature, operation)));
  });

  const fields = document.getElementById('domainFields');
  fields.replaceChildren();
  feature.fields.forEach((field) => {
    const chip = document.createElement('span');
    chip.textContent = field;
    fields.appendChild(chip);
  });
}

function runDomainOperation(feature, operation) {
  document.getElementById('domainActionLog').textContent = `${feature.label}: ${operation} preparado. Valide permissões, dados obrigatórios e auditoria antes de salvar.`;
  setToast(`${operation} iniciado em ${feature.label}.`);
}

async function syncDomainFeature() {
  if (!activeFeature) {
    setToast(`API configurada: ${apiBaseUrl}`);
    return;
  }
  const state = document.getElementById('domainSyncState');
  state.textContent = 'Sincronizando...';
  try {
    const response = await fetch(`${apiBaseUrl}${activeFeature.endpoint}`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const payload = await response.json();
    const count = Array.isArray(payload) ? payload.length : 1;
    state.textContent = `${count} registro(s) recebidos`;
    setToast(`${activeFeature.label} sincronizado com sucesso.`);
  } catch {
    state.textContent = 'API indisponível, operação local mantida';
    setToast(`${activeFeature.label}: API indisponível. O fluxo desktop continua local.`);
  }
}

function renderCart() {
  cartList.replaceChildren();
  if (cart.length === 0) {
    const empty = document.createElement('li');
    empty.className = 'empty';
    empty.textContent = 'Nenhum item adicionado.';
    cartList.appendChild(empty);
    cartTotal.textContent = money(0);
    return;
  }

  cart.forEach((item, index) => {
    const row = document.createElement('li');
    const copy = document.createElement('span');
    const name = document.createElement('strong');
    name.textContent = item.name;
    const price = document.createElement('small');
    price.textContent = money(item.price);
    copy.append(name, document.createElement('br'), price);
    const remove = makeButton('btn', 'Remover', () => {
      cart.splice(index, 1);
      renderCart();
      setToast('Item removido.');
    });
    row.append(copy, remove);
    cartList.appendChild(row);
  });

  cartTotal.textContent = money(cart.reduce((sum, item) => sum + item.price, 0));
}

function localAdmcalc(payload) {
  const salario = Math.max(0, Number(payload.salarioMensal || 0));
  const jornada = Math.max(1, Number(payload.jornadaMensal || 220));
  const salarioHora = salario / jornada;
  const periculosidade = payload.adicionalPericulosidade ? salario * 0.3 : 0;
  const insalubridade = 1412 * Math.max(0, Number(payload.percentualInsalubridade || 0));
  const receita = Math.max(0, Number(payload.receitaBruta || 0));
  const fatorR = receita === 0 ? 0 : (Math.max(0, Number(payload.folhaSalarios || 0)) + 6500) / receita;
  return {
    salarioHora,
    valorHorasExtras: salarioHora * (1 + Math.max(0, Number(payload.percentualHoraExtra || 0))) * Math.max(0, Number(payload.horasExtras || 0)),
    adicionalPericulosidade: periculosidade,
    descontoValeTransporte: Math.min(Math.max(0, Number(payload.custoPassagens || 0)), salario * 0.06),
    decimoTerceiroProporcional: ((salario + periculosidade + insalubridade) / 12) * 12,
    feriasComTerco: (salario + periculosidade + insalubridade) * 1.3333,
    fatorR,
    recomendacao: fatorR >= 0.28 ? 'Fator R saudável para avaliação tributária.' : 'Fator R baixo. Revisar folha, pró-labore e precificação.'
  };
}

async function calculateAdmcalc(event) {
  event.preventDefault();
  const button = document.getElementById('calculateAdmcalc');
  const form = new FormData(event.currentTarget);
  const payload = Object.fromEntries(form.entries());
  payload.adicionalPericulosidade = form.get('adicionalPericulosidade') === 'on';
  button.disabled = true;
  button.textContent = 'Calculando...';

  let data;
  try {
    const response = await fetch(`${apiBaseUrl}/api/admcalc/calcular`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    data = await response.json();
    setToast('AdmCalc calculado pela API Spring Boot.');
  } catch {
    data = localAdmcalc(payload);
    setToast('API indisponível. AdmCalc calculado localmente no desktop.');
  } finally {
    button.disabled = false;
    button.textContent = 'Calcular AdmCalc';
  }

  renderAdmcalcResult(data);
}

function renderAdmcalcResult(data) {
  const rows = [
    ['Salário-hora', money(Number(data.salarioHora))],
    ['Horas extras', money(Number(data.valorHorasExtras))],
    ['Periculosidade', money(Number(data.adicionalPericulosidade))],
    ['VT', money(Number(data.descontoValeTransporte))],
    ['13º proporcional', money(Number(data.decimoTerceiroProporcional))],
    ['Férias + 1/3', money(Number(data.feriasComTerco))],
    ['Fator R', `${(Number(data.fatorR) * 100).toFixed(2)}%`]
  ];
  const dl = document.createElement('dl');
  rows.forEach(([label, value]) => {
    const row = document.createElement('div');
    const dt = document.createElement('dt');
    const dd = document.createElement('dd');
    dt.textContent = label;
    dd.textContent = value;
    row.append(dt, dd);
    dl.appendChild(row);
  });
  const recommendation = document.createElement('p');
  recommendation.className = 'recommendation';
  recommendation.textContent = data.recomendacao;
  result.className = 'result-card';
  result.replaceChildren(dl, recommendation);
}

document.getElementById('platformLabel').textContent = `${window.apexDesktop?.platform || 'desktop'} | Electron ${window.apexDesktop?.versions?.electron || 'dev'}`;
document.getElementById('toggleSidebar').addEventListener('click', () => {
  shell.classList.toggle('collapsed');
  document.getElementById('toggleSidebar').textContent = shell.classList.contains('collapsed') ? 'Expandir menu' : 'Recolher menu';
});
document.getElementById('toggleTheme').addEventListener('click', () => {
  const isDark = document.documentElement.dataset.theme === 'dark';
  document.documentElement.dataset.theme = isDark ? 'light' : 'dark';
  document.getElementById('toggleTheme').textContent = isDark ? 'Dark Mode' : 'Light Mode';
});
document.getElementById('syncApi').addEventListener('click', syncDomainFeature);
document.getElementById('domainSync').addEventListener('click', syncDomainFeature);
document.getElementById('domainAudit').addEventListener('click', () => {
  if (activeFeature) runDomainOperation(activeFeature, 'Validar permissões e auditoria');
});
document.getElementById('clearCart').addEventListener('click', () => {
  cart.splice(0, cart.length);
  renderCart();
  setToast('Carrinho limpo.');
});
document.getElementById('finishSale').addEventListener('click', () => {
  if (cart.length === 0) {
    setToast('Adicione um produto antes de finalizar a venda.');
    return;
  }
  cart.splice(0, cart.length);
  renderCart();
  setToast('Venda finalizada. Estoque preparado para baixa.');
});
document.getElementById('admcalcForm').addEventListener('submit', calculateAdmcalc);
document.getElementById('exportReport').addEventListener('click', () => setToast('Relatório desktop preparado para exportação.'));
document.querySelectorAll('.nav-item[data-view]').forEach((button) => button.addEventListener('click', () => setView(button.dataset.view)));
document.querySelectorAll('.product-button').forEach((button) => {
  button.addEventListener('click', () => {
    cart.push({ name: button.dataset.product, price: Number(button.dataset.price) });
    renderCart();
    setToast(`${button.dataset.product} adicionado ao carrinho.`);
  });
});

buildDomainMenu();
renderCart();
