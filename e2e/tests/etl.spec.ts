import { test, expect, request } from '@playwright/test';

/**
 * E2E test za ETL pipeline za popularne videe (Modul 3)
 *
 * Pokriva kompletan tok: generisanje pregleda → pokretanje ETL pipeline-a →
 * upis u PopularVideo tabelu → prikaz "Top 3 najpopularnija videa" na frontend-u.
 *
 * Pretpostavke:
 *  - Docker stack je pokrenut (frontend :4200, backend :8080, PostgreSQL)
 *  - Seed korisnik postoji: jankovicmatejabp@gmail.com / Test@12345 (data.sql)
 *  - Postoji barem jedan video u bazi (video_id=1 iz data.sql)
 */

const VALID_EMAIL = 'jankovicmatejabp@gmail.com';
const VALID_PASSWORD = 'Test@12345';
const BACKEND_URL = 'http://localhost:8080';
const SEED_VIDEO_ID = 1;

/** Pribavlja JWT token direktno od backend API-ja i upisuje ga u localStorage,
 *  čime se simulira prijavljeni korisnik bez prolaska kroz login UI. */
async function loginProgrammatically(page: import('@playwright/test').Page) {
  const apiContext = await request.newContext({ baseURL: BACKEND_URL });
  const response = await apiContext.post('/api/auth/login', {
    data: { email: VALID_EMAIL, password: VALID_PASSWORD },
  });
  expect(response.ok()).toBeTruthy();
  const authResponse = await response.json();
  await apiContext.dispose();

  await page.goto('/');
  await page.waitForLoadState('networkidle');
  await page.evaluate(
    (ar) => {
      localStorage.setItem('auth_token', ar.token);
      localStorage.setItem('current_user', JSON.stringify(ar));
    },
    authResponse
  );
  await page.reload();
  await page.waitForLoadState('networkidle');
}

// E2E-5: ETL pipeline – od pregleda videa do prikaza top 3 na stranici "U trendu"
test('E2E-5: Pokretanje ETL pipeline-a prikazuje popularne videe prijavljenom korisniku', async ({ page }) => {
  const apiContext = await request.newContext({ baseURL: BACKEND_URL });

  // Korak 1: Generišemo preglede seed videa kako bi Extract faza imala podatke
  // iz poslednjih 7 dana (POST /api/videos/{id}/view upisuje VideoView red).
  for (let i = 0; i < 5; i++) {
    const viewResponse = await apiContext.post(`/api/videos/${SEED_VIDEO_ID}/view`);
    expect(viewResponse.status()).toBe(200);
  }

  // Korak 2: Ručno pokrećemo ETL pipeline (Extract → Transform → Load).
  const pipelineResponse = await apiContext.post('/api/etl/run-pipeline');
  expect(pipelineResponse.status()).toBe(200);

  // Korak 3: Verifikujemo da je pipeline upisao popularne videe (backend → DB).
  const popularResponse = await apiContext.get('/api/etl/popular-videos');
  expect(popularResponse.status()).toBe(200);
  const popularJson = await popularResponse.json();
  expect(popularJson.pipelineRunAt).toBeTruthy();
  expect(Array.isArray(popularJson.topVideos)).toBeTruthy();
  expect(popularJson.topVideos.length).toBeGreaterThan(0);
  expect(popularJson.topVideos.length).toBeLessThanOrEqual(3);
  await apiContext.dispose();

  // Korak 4: Prijavljujemo korisnika i otvaramo stranicu "U trendu".
  await loginProgrammatically(page);
  await page.goto('/trending');
  await page.waitForLoadState('networkidle');

  // Korak 5: Sekcija "Top 3 najpopularnija videa" mora biti vidljiva.
  const popularSection = page.locator('.popular-videos-section');
  await expect(popularSection).toBeVisible({ timeout: 15_000 });
  await expect(popularSection.locator('h2')).toContainText('Top 3 najpopularnija videa');

  // Vremenska oznaka pokretanja pipeline-a je prikazana.
  await expect(popularSection.locator('.popular-timestamp')).toBeVisible();

  // Korak 6: Prikazana je barem jedna kartica popularnog videa sa rangom i skorom.
  const popularCards = popularSection.locator('.popular-video-card');
  await expect(popularCards.first()).toBeVisible();
  const cardCount = await popularCards.count();
  expect(cardCount).toBeGreaterThan(0);
  expect(cardCount).toBeLessThanOrEqual(3);

  // Prvi video ima rang badge "1" i prikazan popularity score.
  await expect(popularCards.first().locator('.rank-badge')).toHaveText('1');
  await expect(popularCards.first().locator('.popularity-score')).toContainText('Popularnost');
});
