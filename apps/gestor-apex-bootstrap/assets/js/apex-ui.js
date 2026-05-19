(function () {
  const html = document.documentElement;
  const viewTitle = document.getElementById("viewTitle");
  const toastElement = document.getElementById("apexToast");
  const toastMessage = document.getElementById("toastMessage");
  const toast = bootstrap.Toast.getOrCreateInstance(toastElement, { delay: 3200 });
  const saleModal = bootstrap.Modal.getOrCreateInstance(document.getElementById("saleCompleteModal"));
  const cartList = document.getElementById("cartList");
  const cartTotal = document.getElementById("cartTotal");
  const cart = [];

  const viewLabels = {
    normalView: "Dashboard gerencial",
    pdvView: "PDV focado",
    errorView: "Central de pendências"
  };

  function formatCurrency(value) {
    return value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
  }

  function showToast(message) {
    toastMessage.textContent = message;
    toast.show();
  }

  function setView(targetId) {
    document.querySelectorAll(".apex-view").forEach((view) => {
      view.classList.toggle("active", view.id === targetId);
    });

    document.querySelectorAll("[data-section-target]").forEach((button) => {
      const isActive = button.dataset.sectionTarget === targetId;
      if (button.classList.contains("apex-nav-item")) {
        button.classList.toggle("active", isActive);
        button.setAttribute("aria-pressed", String(isActive));
      }
    });

    viewTitle.textContent = viewLabels[targetId] || "Gestor Apex";
    document.getElementById("mainContent").focus({ preventScroll: true });
  }

  function updateThemeButton(theme) {
    const button = document.getElementById("themeToggle");
    const icon = button.querySelector("i");
    const label = button.querySelector("span");
    const isDark = theme === "dark";

    icon.className = isDark ? "bi bi-sun" : "bi bi-moon-stars";
    label.textContent = isDark ? "Modo claro" : "Modo escuro";
    button.setAttribute("aria-pressed", String(isDark));
  }

  function setTheme(theme) {
    html.dataset.bsTheme = theme;
    localStorage.setItem("apex-theme", theme);
    updateThemeButton(theme);
  }

  function renderCart() {
    cartList.innerHTML = "";

    if (!cart.length) {
      const empty = document.createElement("li");
      empty.className = "empty";
      empty.textContent = "Nenhum item adicionado.";
      cartList.appendChild(empty);
      cartTotal.textContent = formatCurrency(0);
      return;
    }

    cart.forEach((item, index) => {
      const row = document.createElement("li");
      const description = document.createElement("span");
      const remove = document.createElement("button");

      description.innerHTML = `<strong>${item.name}</strong><br><small>${formatCurrency(item.price)}</small>`;
      remove.type = "button";
      remove.className = "btn btn-sm btn-apex-ghost";
      remove.setAttribute("aria-label", `Remover ${item.name}`);
      remove.innerHTML = '<i class="bi bi-x-lg" aria-hidden="true"></i>';
      remove.addEventListener("click", () => {
        cart.splice(index, 1);
        renderCart();
        showToast("Item removido da venda.");
      });

      row.append(description, remove);
      cartList.appendChild(row);
    });

    const total = cart.reduce((sum, item) => sum + item.price, 0);
    cartTotal.textContent = formatCurrency(total);
  }

  function addProduct(button) {
    cart.push({
      name: button.dataset.product,
      price: Number(button.dataset.price)
    });
    renderCart();
    showToast(`${button.dataset.product} adicionado ao carrinho.`);
  }

  function simulateRetry(button) {
    const original = button.innerHTML;
    button.disabled = true;
    button.innerHTML = '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span> Verificando...';

    window.setTimeout(() => {
      button.disabled = false;
      button.innerHTML = original;
      showToast("Conexão restabelecida. Dados prontos para sincronização.");
    }, 900);
  }

  function filterRows(input, selector) {
    const term = input.value.trim().toLowerCase();
    document.querySelectorAll(selector).forEach((row) => {
      row.hidden = !row.textContent.toLowerCase().includes(term);
    });
  }

  document.querySelectorAll("[data-section-target]").forEach((button) => {
    button.addEventListener("click", () => setView(button.dataset.sectionTarget));
  });

  document.getElementById("themeToggle").addEventListener("click", () => {
    setTheme(html.dataset.bsTheme === "dark" ? "light" : "dark");
  });

  document.querySelectorAll("[data-range]").forEach((button) => {
    button.addEventListener("click", () => {
      document.querySelectorAll("[data-range]").forEach((item) => item.classList.remove("active"));
      button.classList.add("active");
      showToast(`Dashboard atualizado para o período: ${button.dataset.range}.`);
    });
  });

  document.querySelectorAll(".apex-product-button").forEach((button) => {
    button.addEventListener("click", () => addProduct(button));
  });

  document.querySelectorAll("[data-payment]").forEach((button) => {
    button.addEventListener("click", () => {
      document.querySelectorAll("[data-payment]").forEach((item) => item.classList.remove("active"));
      button.classList.add("active");
      showToast(`Forma de pagamento selecionada: ${button.dataset.payment}.`);
    });
  });

  document.getElementById("clearCartButton").addEventListener("click", () => {
    cart.splice(0, cart.length);
    renderCart();
    showToast("Carrinho limpo. Pronto para uma nova venda.");
  });

  document.getElementById("finishSaleButton").addEventListener("click", () => {
    if (!cart.length) {
      showToast("Adicione ao menos um produto antes de finalizar a venda.");
      return;
    }

    cart.splice(0, cart.length);
    renderCart();
    saleModal.show();
  });

  document.getElementById("retryConnectionButton").addEventListener("click", (event) => simulateRetry(event.currentTarget));
  document.getElementById("stockSearch").addEventListener("input", (event) => filterRows(event.currentTarget, "#stockTable tr"));
  document.getElementById("pdvSearch").addEventListener("input", (event) => {
    const term = event.currentTarget.value.trim().toLowerCase();
    document.querySelectorAll(".apex-product-button").forEach((button) => {
      button.hidden = !button.textContent.toLowerCase().includes(term);
    });
  });

  document.addEventListener("click", (event) => {
    const button = event.target.closest("[data-demo-action]");
    if (!button) return;

    const action = button.dataset.demoAction;
    if (action === "toast") showToast(button.dataset.toastMessage || "Ação executada.");
    if (action === "purchase") showToast("Pedido de compra criado em rascunho para revisão do gerente.");
    if (action === "permission") showToast("Solicitação enviada. O gerente receberá uma notificação para aprovar.");
    if (action === "retry") simulateRetry(button);
  });

  setTheme(localStorage.getItem("apex-theme") || "light");
  renderCart();
})();
