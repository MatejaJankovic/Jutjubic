import { test, expect } from '@playwright/test';

/**
 * E2E testovi za tok prijave korisnika (Modul 1 – Autentifikacija)
 *
 * Pretpostavke:
 *  - Docker stack je pokrenut (frontend :4200, backend :8080, PostgreSQL)
 *  - Seed korisnik postoji: jankovicmatejabp@gmail.com / Test@12345
 */

// Podaci za seed korisnika (kreiran u data.sql)
const VALID_EMAIL = 'jankovicmatejabp@gmail.com';
const VALID_PASSWORD = 'Test@12345';

// E2E-1: Uspešna prijava sa validnim kredencijalima
test('E2E-1: Uspešna prijava preusmerava korisnika na početnu stranicu', async ({ page }) => {
  await page.goto('/login');

  // Proveravamo da se stranica za prijavu učitala
  await expect(page.locator('h1')).toHaveText('Prijava');

  // Unosimo kredencijale
  await page.fill('#email', VALID_EMAIL);
  await page.fill('#password', VALID_PASSWORD);

  // Klik na dugme za prijavu i čekanje na navigaciju
  await Promise.all([
    page.waitForURL('http://localhost:4200/', { timeout: 10_000 }),
    page.click('button[type="submit"]'),
  ]);

  // Nakon uspešne prijave, navbar prikazuje avatar dugme (inicijali korisnika)
  // umesto linka "Prijava"
  await expect(page.locator('.profile-btn')).toBeVisible();
  await expect(page.locator('.btn-signin')).not.toBeVisible();
});

// E2E-2: Pogrešna lozinka prikazuje grešku bez preusmeravanja
test('E2E-2: Pogrešna lozinka prikazuje poruku greške na stranici za prijavu', async ({ page }) => {
  await page.goto('/login');

  await page.fill('#email', VALID_EMAIL);
  await page.fill('#password', 'pogresna_lozinka_123');

  // Čekamo da API poziv završi (backend vraća 400)
  const responsePromise = page.waitForResponse(
    res => res.url().includes('/api/auth/login') && res.status() === 400
  );
  await page.click('button[type="submit"]');
  await responsePromise;

  // Poruka greške mora biti vidljiva
  await expect(page.locator('.alert-error')).toBeVisible();

  // Korisnik ostaje na stranici za prijavu (nije preusmeren)
  expect(page.url()).toContain('/login');

  // Avatar ne postoji (korisnik nije prijavljen)
  await expect(page.locator('.profile-btn')).not.toBeVisible();
});
