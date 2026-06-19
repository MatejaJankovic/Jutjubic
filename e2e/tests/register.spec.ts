import { test, expect } from '@playwright/test';

/**
 * E2E test za tok registracije korisnika (Modul 1 – Autentifikacija)
 *
 * Pretpostavke:
 *  - Docker stack je pokrenut (frontend :4200, backend :8080)
 */

// E2E-3: Kompletni tok registracije – validacija forme i uspešna registracija
test('E2E-3: Registracija prikazuje validacione greške za prazna polja, zatim uspešnu poruku za validne podatke', async ({ page }) => {
  await page.goto('/register');

  await expect(page.locator('h1')).toHaveText('Registracija');

  // --- Korak 1: Pokušaj slanja prazne forme ---
  // Angular forma markira sva polja kao "touched" na klik submit-a i prikazuje greške
  await page.click('button[type="submit"]');

  // Validacione greške moraju biti vidljive za obavezna polja
  const errorMessages = page.locator('.error-message');
  await expect(errorMessages.first()).toBeVisible();

  // Proveravamo da nije došlo do navigacije – i dalje smo na /register
  expect(page.url()).toContain('/register');

  // --- Korak 2: Unos validnih podataka i uspešna registracija ---
  // Koristimo timestamp kako bismo izbegli duplikat email-a između pokretanja
  const uniqueEmail = `e2e_test_${Date.now()}@example.com`;

  await page.fill('#firstName', 'E2E');
  await page.fill('#lastName', 'Test');
  await page.fill('#email', uniqueEmail);
  await page.fill('#username', `e2euser${Date.now()}`);
  await page.fill('#address', 'Test adresa, Novi Sad');
  await page.fill('#password', 'Test@12345');
  await page.fill('#confirmPassword', 'Test@12345');

  // Čekamo odgovor backend-a (201 Created)
  const responsePromise = page.waitForResponse(
    res => res.url().includes('/api/auth/register') && res.status() === 201
  );
  await page.click('button[type="submit"]');
  await responsePromise;

  // Poruka o uspešnoj registraciji mora biti vidljiva
  await expect(page.locator('.alert-success')).toBeVisible();
  await expect(page.locator('.alert-success')).toContainText('Registracija uspešna');
});
