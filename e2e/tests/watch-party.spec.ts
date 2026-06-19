import { test, expect, request } from '@playwright/test';

/**
 * E2E test za tok kreiranja Watch Party sobe (Modul 2 – Watch Party)
 *
 * Pretpostavke:
 *  - Docker stack je pokrenut (frontend :4200, backend :8080)
 *  - Seed korisnik postoji i ima enabled: true
 *  - Postoji barem jedan video u bazi
 */

const VALID_EMAIL = 'jankovicmatejabp@gmail.com';
const VALID_PASSWORD = 'Test@12345';

/** Pribavlja JWT token direktno od backend API-ja i upisuje ga u localStorage,
 *  čime se simulira prijavljeni korisnik bez prolaska kroz login UI.
 *
 *  Angular AuthService čita sledeće ključeve iz localStorage:
 *   - 'auth_token'    → string (JWT)
 *   - 'current_user'  → JSON string (puna AuthResponse struktura)
 */
async function loginProgrammatically(page: import('@playwright/test').Page) {
  // Pozivamo backend API direktno (zaobilazimo UI)
  const apiContext = await request.newContext({ baseURL: 'http://localhost:8080' });
  const response = await apiContext.post('/api/auth/login', {
    data: { email: VALID_EMAIL, password: VALID_PASSWORD },
  });
  expect(response.ok()).toBeTruthy();
  // body je puna AuthResponse: {token, type, id, email, username, firstName, lastName, role}
  const authResponse = await response.json();
  await apiContext.dispose();

  // Angular app treba biti učitana pre nego što možemo da pišemo u localStorage
  await page.goto('/');
  await page.waitForLoadState('networkidle');

  // Postavljamo auth stanje onako kako ga Angular AuthService očekuje
  await page.evaluate(
    (ar) => {
      localStorage.setItem('auth_token', ar.token);
      localStorage.setItem('current_user', JSON.stringify(ar));
    },
    authResponse
  );

  // Osvežavamo stranicu da bi Angular preuzeo stanje iz localStorage-a
  await page.reload();
  await page.waitForLoadState('networkidle');
}

// E2E-4: Kreiranje Watch Party sobe – kompletni korisnički tok
test('E2E-4: Prijavljen korisnik može da kreira Watch Party sobu i dobija jedinstveni invite link', async ({ page }) => {
  // Korak 1: Programatska prijava (bez UI login toka)
  await loginProgrammatically(page);

  // Korak 2: Navigacija na stranicu za Watch Party
  await page.goto('/watch-party');

  // Čekamo da se učita barem jedna kartica sa videom
  const createBtn = page.locator('.create-party-btn').first();
  await expect(createBtn).toBeVisible({ timeout: 15_000 });

  // Korak 3: Klik na "Kreiraj Watch Party" za prvi dostupan video
  // Backend: POST /api/watch-party/create → 200 OK sa WatchPartyDTO
  const responsePromise = page.waitForResponse(
    res => res.url().includes('/api/watch-party/create'),
    { timeout: 15_000 }
  );
  await createBtn.click();
  const createResponse = await responsePromise;
  expect(createResponse.status()).toBe(200);

  // Korak 4: Proveravamo da je URL promenjen na /watch-party/room/:id
  await page.waitForURL(/\/watch-party\/room\/[a-f0-9-]+/, { timeout: 10_000 });
  expect(page.url()).toMatch(/\/watch-party\/room\/[a-f0-9-]+/);

  // Korak 5: Proveravamo da je invite link vidljiv i sadrži UUID sobe
  const inviteInput = page.locator('.invite-input');
  await expect(inviteInput).toBeVisible({ timeout: 10_000 });

  const inviteValue = await inviteInput.inputValue();
  expect(inviteValue).toBeTruthy();
  // Invite link treba da sadrži UUID koji odgovara UUID formatu
  expect(inviteValue).toMatch(/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}/i);

  // Korak 6: Proveravamo da sidebar prikazuje ulogu "Kreator"
  const roleRow = page.locator('.info-row').filter({ hasText: 'Uloga' });
  await expect(roleRow).toContainText('Kreator');
});
