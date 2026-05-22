const shell = document.getElementById('shell');
const toast = document.getElementById('toast');
const screenTitle = document.getElementById('screenTitle');
const cartList = document.getElementById('cartList');
const cartTotal = document.getElementById('cartTotal');
const result = document.getElementById('admcalcResult');
const cart = [];

const titles = {
  pdv: 'PDV de alta produtividade',
  admcalc: 'AdmCalc financeiro',
  relatorios: 'Relatórios desktop'
};

function money(value) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function setToast(message) {
  toast.textContent = message;
}

function setView(view) {
  document.querySelectorAll('.view').forEach((item) => item.classList.toggle('active', item.id === `${view}View`));
  document.querySelectorAll('.nav-item').forEach((button) => button.classList.toggle('active', button.dataset.view === view));
  screenTitle.textContent = titles[view] || 'Apex Gestor';
  setToast(`Tela ${screenTitle.textContent} carregada.`);
}

function renderCart() {
  cartList.innerHTML = '';
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
    row.innerHTML = `<span><strong>${item.name}</strong><br>${money(item.price)}</span>`;
    const remove = document.createElement('button');
    remove.type = 'button';
    remove.className = 'btn';
    remove.textContent = 'Remover';
    remove.addEventListener('click', () => {
      cart.splice(index, 1);
      renderCart();
      setToast('Item removido.');
    });
    row.appendChild(remove);
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
    const response = await fetch(`${window.apexDesktop.apiBaseUrl}/api/admcalc/calcular`, {
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

document.getElementById('platformLabel').textContent = `${window.apexDesktop.platform} | Electron ${window.apexDesktop.versions.electron}`;
document.getElementById('toggleSidebar').addEventListener('click', () => {
  shell.classList.toggle('collapsed');
  document.getElementById('toggleSidebar').textContent = shell.classList.contains('collapsed') ? 'Expandir menu' : 'Recolher menu';
});
document.getElementById('toggleTheme').addEventListener('click', () => {
  const isDark = document.documentElement.dataset.theme === 'dark';
  document.documentElement.dataset.theme = isDark ? 'light' : 'dark';
  document.getElementById('toggleTheme').textContent = isDark ? 'Dark Mode' : 'Light Mode';
});
document.getElementById('syncApi').addEventListener('click', () => setToast(`API configurada: ${window.apexDesktop.apiBaseUrl}`));
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
document.querySelectorAll('.nav-item').forEach((button) => button.addEventListener('click', () => setView(button.dataset.view)));
document.querySelectorAll('.product-button').forEach((button) => {
  button.addEventListener('click', () => {
    cart.push({ name: button.dataset.product, price: Number(button.dataset.price) });
    renderCart();
    setToast(`${button.dataset.product} adicionado ao carrinho.`);
  });
});

renderCart();
