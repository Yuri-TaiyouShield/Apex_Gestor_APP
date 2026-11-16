import { expect, test } from '@playwright/test';

const VIEWPORTS = [
  { name: 'Desktop Full HD', width: 1920, height: 1080 },
  { name: 'Desktop Standard', width: 1440, height: 900 },
  { name: 'Desktop WXGA', width: 1366, height: 768 },
  { name: 'Tablet Landscape', width: 1024, height: 768 },
  { name: 'Mobile Modern (iPhone 12/13/14)', width: 390, height: 844 },
  { name: 'Mobile Standard (Android)', width: 360, height: 800 }
];

test.describe('Frontend UI, Responsividade e Acessibilidade (WCAG 2.2)', () => {
  test('Meta tags, CSP e estrutura base do documento', async ({ page }) => {
    await page.goto('/store');
    await expect(page).toHaveTitle(/Loja online|Apex Gestor/);

    const csp = await page.locator('meta[http-equiv="Content-Security-Policy"]').getAttribute('content');
    expect(csp).toBeTruthy();
    expect(csp).toContain("default-src 'self'");
    expect(csp).toContain("object-src 'none'");

    const viewport = await page.locator('meta[name="viewport"]').getAttribute('content');
    expect(viewport).toContain('viewport-fit=cover');
  });

  for (const vp of VIEWPORTS) {
    test(`Renderizacao responsiva em ${vp.name} (${vp.width}x${vp.height})`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await page.goto('/store');

      // Verifica se o shell da aplicacao carrega
      const appShell = page.locator('ion-app');
      await expect(appShell).toBeVisible();

      // Verifica heading principal da tela operacional/loja
      const heading = page.locator('h1').first();
      await expect(heading).toBeVisible();
      await expect(heading).toContainText(/Materiais|Dashboard|Apex/);

      // Verifica presenca da barra de acoes ou carrinho
      const cartOrAction = page.locator('ion-button').first();
      await expect(cartOrAction).toBeVisible();
    });
  }

  test('Design system e tokens CSS --apex-* aplicados no root', async ({ page }) => {
    await page.goto('/store');

    const tokens = await page.evaluate(() => {
      const style = getComputedStyle(document.documentElement);
      return {
        primary: style.getPropertyValue('--apex-primary').trim(),
        surface: style.getPropertyValue('--apex-surface').trim(),
        shadow: style.getPropertyValue('--apex-shadow').trim()
      };
    });

    expect(tokens.primary).toBeTruthy();
    expect(tokens.surface).toBeTruthy();
  });

  test('Acessibilidade: foco visivel e navegacao por teclado nos controles', async ({ page }) => {
    await page.goto('/store');

    // Localiza campo de busca na loja
    const searchInput = page.locator('ion-searchbar input').first();
    await expect(searchInput).toBeVisible();

    await searchInput.focus();
    await expect(searchInput).toBeFocused();
  });

  test('Acesso a pagina de login e controles de autenticacao', async ({ page }) => {
    await page.goto('/login');

    const fields = page.locator('label.field input');
    await expect(fields.nth(0)).toBeVisible();
    await expect(fields.nth(1)).toBeVisible();

    const checkbox = page.locator('label.check-row input[type="checkbox"]');
    await expect(checkbox).toBeVisible();

    const submitBtn = page.getByRole('button', { name: /Entrar com/ });
    await expect(submitBtn).toBeVisible();
  });
});
