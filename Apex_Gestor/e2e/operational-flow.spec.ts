import { expect, Page, test } from '@playwright/test';

const API = process.env.APEX_E2E_API_URL || 'http://127.0.0.1:8080';
const PASSWORD = 'Apex@2026';
const BARCODE = '7890000001001';

async function login(page: Page, login: string): Promise<void> {
  await page.goto('/login');
  const fields = page.locator('label.field input');
  await fields.nth(0).fill(login);
  await fields.nth(1).fill(PASSWORD);
  await page.locator('label.check-row input[type="checkbox"]').check();
  await page.getByRole('button', { name: 'Entrar com segurança' }).click();
  await expect(page).toHaveURL(/\/$/);
  await expect(page.locator('h1:visible')).toBeVisible();
}

async function token(page: Page): Promise<string> {
  return page.evaluate(() => sessionStorage.getItem('apex.accessToken') || '');
}

function menuLink(page: Page, name: string) {
  return page.locator('ion-menu').getByRole('link', { name, exact: true });
}

async function stock(page: Page): Promise<number> {
  const response = await page.request.get(`${API}/api/produtos`, {
    headers: { Authorization: `Bearer ${await token(page)}` }
  });
  expect(response.ok()).toBeTruthy();
  const products = await response.json();
  return products.find((item: { codigoBarras?: string }) => item.codigoBarras === BARCODE).quantidadeEstoque;
}

test.describe.serial('homologação operacional v1', () => {
  test('Vendedor → Caixa, pagamento misto, estoque, histórico, RBAC e LGPD transversal', async ({ page }) => {
    await login(page, 'v5_vendedor');
    await expect(page.getByText('Privacidade ativa')).toBeVisible();
    await expect(page.getByText('Privacidade LGPD', { exact: true })).toHaveCount(0);
    await expect(page.locator('h1:visible')).toContainText('Dashboard do Vendedor');
    await expect(page.getByText('Meta do mes')).toBeVisible();
    await expect(menuLink(page, 'Caixa')).toHaveCount(0);
    await expect(menuLink(page, 'Financeiro')).toHaveCount(0);

    const navigationCount = await page.evaluate(() => performance.getEntriesByType('navigation').length);
    const stockBefore = await stock(page);
    await page.locator('ion-router-outlet').getByRole('link', { name: 'Abrir PDV', exact: true }).click();
    await expect(page).toHaveURL(/\/pos$/);
    expect(await page.evaluate(() => performance.getEntriesByType('navigation').length)).toBe(navigationCount);

    await page.locator('ion-searchbar input').fill(BARCODE);
    await page.getByRole('button', { name: /Cimento HML 50kg/ }).click();
    await page.getByRole('button', { name: 'Adicionar unidade' }).click();
    await expect(page.locator('.cart-line ion-badge')).toHaveText('2');
    await expect(page.locator('.checkout-panel .total')).toContainText('R$');
    await page.getByRole('button', { name: 'Enviar para o Caixa' }).click();
    const sent = page.locator('.checkout-panel .state-line');
    await expect(sent).toContainText('AGUARDANDO_CAIXA');
    const saleId = Number((await sent.textContent())?.match(/#(\d+)/)?.[1]);
    expect(saleId).toBeGreaterThan(0);

    await page.goto('/cashier');
    await expect(page).toHaveURL(/\/login\?redirect=%2Fcashier&reason=forbidden/);

    await login(page, 'v5_caixa');
    await expect(menuLink(page, 'Financeiro')).toHaveCount(0);
    await menuLink(page, 'Produtos / Estoque').click();
    await expect(page).toHaveURL(/\/products$/);
    await expect(page.getByRole('button', { name: /Novo/ })).toBeDisabled();
    await login(page, 'v5_caixa');
    await menuLink(page, 'Caixa').click();
    await expect(page).toHaveURL(/\/cashier$/);

    const open = page.getByRole('button', { name: 'Abrir caixa' });
    if (await open.isVisible()) {
      await open.click();
      await expect(page.getByText('Caixa aberto.')).toBeVisible();
    }
    await page.getByRole('button', { name: new RegExp(`#${saleId}\\b`) }).click();
    const paymentValues = page.locator('.payment-row ion-input[label="Valor"] input');
    await paymentValues.nth(0).fill('20');
    await paymentValues.nth(0).blur();
    await page.getByRole('button', { name: 'Adicionar forma' }).click();
    await expect(page.locator('.payment-row')).toHaveCount(2);
    await expect(page.locator('.payment-row').nth(1)).toContainText('PIX');
    await expect(page.getByText('Restante').locator('..')).toContainText('R$ 0,00');
    await page.getByRole('button', { name: 'Confirmar recebimento' }).click();
    await expect(page.getByText(/Venda .* finalizada/)).toBeVisible();

    expect(await stock(page)).toBe(stockBefore - 2);

    await login(page, 'v5_vendedor');
    await menuLink(page, 'Histórico de Vendas').click();
    await page.locator('ion-searchbar input').fill(String(saleId));
    await page.getByRole('button', { name: 'Buscar' }).click();
    const historyId = page.locator('.history-page').getByRole('cell', { name: new RegExp(`^#${saleId}\\b`) });
    await expect(historyId).toBeVisible();
    await expect(historyId.locator('..')).toContainText('FINALIZADA');

    await login(page, 'v5_gerente');
    await menuLink(page, 'Histórico de Vendas').click();
    await page.locator('ion-searchbar input').fill(String(saleId));
    await page.getByRole('button', { name: 'Buscar' }).click();
    await page.getByRole('button', { name: 'Detalhes' }).first().click();
    page.once('dialog', (dialog) => dialog.dismiss());
    await page.getByRole('button', { name: /Devolução parcial/ }).click();
    await expect(page.getByText('Motivo obrigatório.')).toBeVisible();
    await menuLink(page, 'Financeiro').click();
    await expect(page.locator('h1:visible')).toContainText('Contas e conciliação');
    await expect(page.locator('.finance-page').getByRole('cell', { name: `#${saleId}`, exact: true })).toBeVisible();

    await login(page, 'v5_sysadmin');
    await expect(menuLink(page, 'Administração')).toBeVisible();
    await menuLink(page, 'Administração').click();
    await expect(page).toHaveURL(/\/admin-settings$/);
    await expect(page.getByText('Privacidade ativa')).toBeVisible();
    await expect(page.getByText('Privacidade LGPD', { exact: true })).toHaveCount(0);
  });
});
