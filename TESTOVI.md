# Testovi - Jutjubic projekat

## Sadržaj

- [Unit testovi](#unit-testovi)
- [Integracioni testovi](#integracioni-testovi)
- [E2E testovi](#e2e-testovi)

---

## Unit testovi

Unit testovi testiraju izolovanu logiku jedne klase bez pokretanja Spring konteksta ili baze podataka. Sve spoljne zavisnosti (repozitorijumi, servisi, WebSocket) se **mokuju** pomoću Mockito biblioteke. Testovi se pokreću komandom:

```bash
./mvnw test -Dtest="AuthServiceTest,WatchPartyServiceTest,ETLServiceTest"
```

---

### AuthServiceTest

**Lokacija:** `src/test/java/rs/ftn/isa/jutjubicbackend/service/AuthServiceTest.java`

**Klasa koja se testira:** `AuthService`

**Mokovane zavisnosti:**
- `UserRepository` – repozitorijum korisnika (baza podataka)
- `AuthenticationManager` – Spring Security autentifikacija
- `PasswordEncoder`, `JwtTokenProvider`, `EmailService`, `ActiveUsersMetrics`

---

#### Test 1 — `register_throwsWhenPasswordsDontMatch`

**Šta testira:** Logiku provere podudarnosti lozinki pri registraciji.

**Kako radi:**
1. Kreira `RegisterRequest` sa različitim vrednostima `password` i `confirmPassword`.
2. Poziva `authService.register(request)`.
3. Proverava da li je bačen `BadRequestException` sa porukom `"Lozinke se ne poklapaju"`.
4. Verifikuje da `UserRepository` **nije ni pozvan** (greška je detektovana pre bilo kakvog DB poziva).

Potvrđuje da se validacija lozinke dešava na prvom mestu, pre pristupa bazi.

---

#### Test 2 — `register_throwsWhenEmailAlreadyExists`

**Šta testira:** Sprečavanje duplih registracija sa istim emailom.

**Kako radi:**
1. Kreira `RegisterRequest` sa validnim podacima i poklapajućim lozinkama.
2. **Mokuje** `userRepository.existsByEmail(...)` da vrati `true` (email postoji).
3. Poziva `authService.register(request)`.
4. Proverava da je bačen `BadRequestException` sa porukom `"Email adresa je već u upotrebi"`.
5. Verifikuje da `userRepository.save(...)` **nikada nije pozvan**.

Garantuje da se dupli nalog ne može kreirati čak i ako se zaobiđe frontend validacija.

---

#### Test 3 — `login_throwsBadRequestExceptionOnWrongCredentials`

**Šta testira:** Rukovanje pogrešnim kredencijalima pri prijavi.

**Kako radi:**
1. Kreira `LoginRequest` sa neispravnom lozinkom.
2. **Mokuje** `authenticationManager.authenticate(...)` da baci `BadCredentialsException`.
3. Poziva `authService.login(request)`.
4. Proverava da `AuthService` pretvara Spring Security izuzetak u domenski `BadRequestException` sa odgovarajućom porukom.

Testira da se Spring Security izuzeci pravilno mapiraju u API odgovore razumljive klijentu.

---

### WatchPartyServiceTest

**Lokacija:** `src/test/java/rs/ftn/isa/jutjubicbackend/service/WatchPartyServiceTest.java`

**Klasa koja se testira:** `WatchPartyService`

**Mokovane zavisnosti:**
- `WatchPartyRepository` – repozitorijum soba (baza podataka)
- `SimpMessagingTemplate` – WebSocket broadcasting (STOMP)

---

#### Test 4 — `createRoom_savesRoomWithUUIDInviteCodeAndAddsCreatorAsParticipant`

**Šta testira:** Kreiranje Watch Party sobe – generisanje invite koda i inicijalno dodavanje kreatora.

**Kako radi:**
1. **Mokuje** `watchPartyRepository.existsByInviteCode(...)` da vrati `false` (kod je jedinstven).
2. **Mokuje** `watchPartyRepository.save(...)` da vrati prosleđeni objekat (simulacija persist-a).
3. Poziva `watchPartyService.createRoom(42L, 1L, true)`.
4. Proverava da `inviteCode` nije `null` i da je u UUID formatu (`xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`).
5. Proverava da skup `participantIds` sadrži ID kreatora (`42L`).
6. Verifikuje da je `repository.save(...)` pozvan tačno jednom.

Pokriva centralnu funkcionalnost Watch Party-ja i garantuje jedinstvenost invite koda i ispravno inicijalno stanje sobe.

---

#### Test 5 — `joinRoom_throwsRuntimeExceptionWhenInviteCodeNotFound`

**Šta testira:** Rukovanje nevalidnim invite kodom pri pridruživanju sobi.

**Kako radi:**
1. **Mokuje** `watchPartyRepository.findByInviteCode("nepostojeci-kod")` da vrati `Optional.empty()`.
2. Poziva `watchPartyService.joinRoom("nepostojeci-kod", 1L)`.
3. Proverava da je bačen `RuntimeException` sa porukom `"Watch party not found"`.
4. Verifikuje da `repository.save(...)` **nikada nije pozvan**.

Osigurava da korisnik ne može da se pridruži nepostojećoj sobi, a da pritom nema neželjenih efekata na bazu.

---

#### Test 6 — `switchVideo_throwsWhenUserIsNotCreator`

**Šta testira:** Autorizaciono pravilo – samo kreator sobe može menjati video.

**Kako radi:**
1. Kreira `WatchParty` objekat sa `creatorId = 10L`.
2. **Mokuje** `watchPartyRepository.findById(1L)` da vrati tu sobu.
3. Poziva `watchPartyService.switchVideo(1L, 5L, 99L)` – korisnik `99L` nije kreator.
4. Proverava da je bačen `RuntimeException` sa porukom `"Only the room owner can switch videos"`.
5. Verifikuje da `repository.save(...)` i `messagingTemplate.convertAndSend(...)` **nikada nisu pozvani** (nema neželjenog broadcast-a).

Testira kritično sigurnosno pravilo i potvrđuje da se WebSocket poruka ne šalje u slučaju neautorizovanog pristupa.

---

### ETLServiceTest

**Lokacija:** `src/test/java/rs/ftn/isa/jutjubicbackend/service/ETLServiceTest.java`

**Klasa koja se testira:** `ETLService`

**Mokovane zavisnosti:**
- `VideoViewRepository` – repozitorijum pregleda videa
- `PopularVideoRepository` – repozitorijum popularnih videa
- `VideoRepository` – repozitorijum videa

---

#### Test 7 — `getLatestPopularVideos_returnsEmptyOptionalWhenNoPipelineRunYet`

**Šta testira:** Ponašanje kada pipeline još nije ni jednom pokrenut.

**Kako radi:**
1. **Mokuje** `popularVideoRepository.findLatest()` da vrati `Optional.empty()`.
2. Poziva `etlService.getLatestPopularVideos()`.
3. Proverava da je vraćen prazan `Optional`.
4. Verifikuje da je `findLatest()` pozvan tačno jednom.

Pokriva edge case – sistem mora elegantno da odgovori pre prvog ETL pokretanja, bez izuzetaka.

---

#### Test 8 — `runETLPipeline_savesPopularVideoRecordEvenWithNoViews`

**Šta testira:** Robusnost ETL pipeline-a kada nema pregleda u poslednjih 7 dana.

**Kako radi:**
1. **Mokuje** `videoViewRepository.findViewsSince(...)` da vrati praznu listu.
2. **Mokuje** `popularVideoRepository.save(...)` da vrati prosleđeni objekat.
3. Poziva `etlService.runETLPipeline()`.
4. Proverava da nije bačen izuzetak (`assertDoesNotThrow`).
5. Verifikuje da je `videoViewRepository.findViewsSince(...)` pozvan (Extract faza izvršena).
6. Verifikuje da je `popularVideoRepository.save(...)` pozvan (Load faza izvršena, čak i sa praznim rezultatom).
7. Verifikuje da `videoRepository` **nije ni pozvan** (nije potreban kada nema pregleda).

Potvrđuje da pipeline ne pada pri praznim podacima i da sve tri faze (Extract, Transform, Load) funkcionišu ispravno i u tom slučaju.

---

## Integracioni testovi

Integracioni testovi koriste `@WebMvcTest` koji pokreće **pravi Spring MVC kontekst** bez pune baze podataka. Testiraju se stvarne interakcije između slojeva:

- **Spring MVC** – rutiranje HTTP zahteva, deserijalizacija JSON tela zahteva
- **Bean Validation (`@Valid`)** – Hibernate Validator proverava `RegisterRequest` polja
- **Spring Security filter chain** – `JwtAuthenticationFilter` + pravila iz `SecurityConfig`
- **GlobalExceptionHandler** – prevodi izuzetke iz servisa u strukturirani HTTP odgovor
- **Jackson serijalizacija** – konverzija Java objekata u JSON (npr. `isPublic` → `public`)

Mokuju se samo: `AuthService`, `WatchPartyService`, `VideoService` (zahtevaju bazu), `JwtTokenProvider` (zahteva JWT secret iz konfiguracije), `UserDetailsService` (zahteva bazu korisnika).

Pokretanje integrationih testova:
```bash
./mvnw test -Dtest="AuthControllerIntegrationTest,WatchPartyControllerIntegrationTest"
```

---

### AuthControllerIntegrationTest

**Lokacija:** `src/test/java/rs/ftn/isa/jutjubicbackend/controller/AuthControllerIntegrationTest.java`

**Kontroler koji se testira:** `AuthController`

**Mokovane zavisnosti:**
- `AuthService` – servis koji zahteva bazu i email server
- `LoginRateLimiterService` – in-memory rate limiter (stanje koje ne sme da utiče na testove)
- `JwtTokenProvider` – zahteva JWT secret key iz konfiguracionih fajlova
- `UserDetailsService` – zahteva bazu korisnika

---

#### Test IT-1 — `register_returns201WithSuccessMessage`

**Šta testira:** Uspešan HTTP zahtev za registraciju prolazi kroz ceo MVC pipeline i vraća 201.

**Kako radi:**
1. Šalje `POST /api/auth/register` sa validnim JSON telom.
2. Mock `authService.register(...)` ne radi ništa (simulacija uspešnog poziva).
3. Proverava da je HTTP status **201 Created**.
4. Proverava da JSON odgovor sadrži `success: true` i odgovarajuću poruku.

Testira pravu integraciju: HTTP deserijalizacija → Controller → servisni poziv → serializacija odgovora.

---

#### Test IT-2 — `register_returns400WithValidationErrorsWhenBodyIsInvalid`

**Šta testira:** Spring `@Valid` anotacija i `GlobalExceptionHandler` vraćaju 400 sa detaljima greške validacije.

**Kako radi:**
1. Šalje `POST /api/auth/register` sa nevalidnim podacima (prazan email, kratko korisničko ime).
2. Spring MVC automatski poziva Hibernate Validator (jer controller ima `@Valid`).
3. `GlobalExceptionHandler.handleValidationExceptions()` hvata grešku.
4. Proverava da je HTTP status **400**, da JSON sadrži `error: "Validation Failed"` i mapu `errors`.

Testira pravu integraciju: HTTP sloj → Bean Validation → GlobalExceptionHandler → strukturirani JSON odgovor sa poljem `errors`.

---

#### Test IT-3 — `register_returns400WithMessageWhenServiceThrowsBadRequest`

**Šta testira:** `BadRequestException` iz `AuthService` prolazi kroz `GlobalExceptionHandler` i vraća 400 sa poljem `message`.

**Kako radi:**
1. Šalje validan `POST /api/auth/register`.
2. Mock `authService.register(...)` baca `BadRequestException("Email adresa je već u upotrebi")`.
3. `GlobalExceptionHandler.handleBadRequestException()` hvata izuzetak.
4. Proverava da je HTTP status **400** i da JSON odgovor sadrži tačnu poruku.

Testira pravu integraciju između controller-a i GlobalExceptionHandler-a: izuzetak iz servisa → handler → ErrorResponse JSON (sa poljem `message`, a ne `success`).

---

#### Test IT-4 — `login_returns200WithTokenWhenCredentialsAreValid`

**Šta testira:** Uspešna prijava integrisan sa rate limiterom vraća 200 sa JWT tokenom u JSON-u.

**Kako radi:**
1. Mock `rateLimiterService.isBlocked(...)` vraća `false` (IP nije blokiran).
2. Mock `authService.login(...)` vraća `AuthResponse` sa JWT tokenom.
3. Šalje `POST /api/auth/login` sa validnim podacima.
4. Proverava da je HTTP status **200** i da JSON odgovor sadrži `token`, `type: "Bearer"` i `email`.

Testira pravu integraciju između `AuthController`, `LoginRateLimiterService` i formata odgovora: oba servisa moraju biti ispravno pozvana da bi se vratio 200.

---

#### Test IT-5 — `login_returns429WithRetryAfterHeaderWhenIpIsBlocked`

**Šta testira:** Blokiran IP od strane rate limitera → `RateLimitExceededException` → `GlobalExceptionHandler` → 429 sa `Retry-After` headerom.

**Kako radi:**
1. Mock `rateLimiterService.isBlocked(...)` vraća `true`.
2. Mock `rateLimiterService.getSecondsUntilReset(...)` vraća `42`.
3. Proverava da je HTTP status **429 Too Many Requests**.
4. Proverava da HTTP header `Retry-After` ima vrednost `"42"`.
5. Proverava da JSON sadrži `status: 429`.

Testira niz interakcija: `LoginRateLimiterService` → Controller → `RateLimitExceededException` → `GlobalExceptionHandler` → HTTP header + status + JSON telo.

---

### WatchPartyControllerIntegrationTest

**Lokacija:** `src/test/java/rs/ftn/isa/jutjubicbackend/controller/WatchPartyControllerIntegrationTest.java`

**Kontroler koji se testira:** `WatchPartyController`

**Mokovane zavisnosti:**
- `WatchPartyService` – servis koji zahteva bazu
- `VideoService` – servis koji zahteva bazu
- `JwtTokenProvider` – zahteva JWT konfiguraciju
- `UserDetailsService` – zahteva bazu korisnika

---

#### Test IT-6 — `createWatchParty_returns403WhenRequestHasNoJwtToken`

**Šta testira:** Spring Security sa realnom `SecurityConfig` konfiguracijom odbija neautentifikovani POST zahtev sa 403.

**Kako radi:**
1. Šalje `POST /api/watch-party/create` **bez** `Authorization: Bearer ...` headera.
2. `JwtAuthenticationFilter` ne pronalazi token – SecurityContext ostaje prazan.
3. Spring Security vidi da endpoint zahteva autentifikaciju (`.anyRequest().authenticated()`).
4. Proverava da je HTTP status **403 Forbidden**.

Testira pravu integraciju security filtera i konfiguracije: `JwtAuthenticationFilter` → `SecurityConfig` pravila → 403 odgovor. Ovo je ključan test koji proverava da zaštićeni endpointi zaista jesu zaštićeni.

---

#### Test IT-7 — `getWatchParty_returns404WhenRoomDoesNotExist`

**Šta testira:** Autentifikovani GET zahtev za nepostojećom sobom prolazi kroz SecurityContext i vraća 404.

**Kako radi:**
1. `@WithMockUser` postavlja korisnika u SecurityContext (simulira JWT autentifikaciju).
2. Mock `watchPartyService.findById(999L)` vraća `Optional.empty()`.
3. Šalje `GET /api/watch-party/999`.
4. Controller poziva `findById()`, dobija prazan Optional i vraća `ResponseEntity.notFound()`.
5. Proverava da je HTTP status **404 Not Found**.

Testira integraciju: Spring Security propušta autentifikovani zahtev → Controller poziva servis → Optional.empty() → 404 odgovor.

---

#### Test IT-8 — `getWatchParty_returns200WithDtoWhenRoomExists`

**Šta testira:** GET zahtev za postojećom sobom vraća 200 sa ispravno serijalizovanim `WatchPartyDTO` JSON-om.

**Kako radi:**
1. `@WithMockUser` autentifikuje korisnika.
2. Mock `watchPartyService.findById(1L)` vraća `Optional` sa `WatchParty` entitetom.
3. Šalje `GET /api/watch-party/1`.
4. Controller poziva `WatchPartyDTO.fromEntity()` i Jackson serijalizuje DTO.
5. Proverava da JSON sadrži `id: 1`, `inviteCode: "abc-123-def"` i `public: true` (ne `isPublic`).

Testira integraciju serijalizacije: `WatchParty` entitet → `WatchPartyDTO.fromEntity()` → Jackson → JSON sa `public` (umesto `isPublic`, jer Lombok/Jackson skida prefiks `is` kod boolean gettera).

---

## E2E testovi

E2E (end-to-end) testovi pokreću pravi Chromium pretraživač i testiraju **kompletne korisničke tokove** — od Angular frontend-a, kroz HTTP zahteve, sve do Spring Boot backend-a i PostgreSQL baze. Za razliku od integracioni testova koji testiraju samo HTTP sloj, E2E testovi potvrđuju da **ceo sistem radi zajedno** iz perspektive korisnika.

**Alat:** [Playwright](https://playwright.dev/) (TypeScript), `@playwright/test` v1.49  
**Pretraživač:** Chromium (headless)  
**Lokacija testova:** `e2e/tests/`  
**Konfiguracija:** `e2e/playwright.config.ts` (baseURL: `http://localhost:4200`)

Pokretanje E2E testova (Docker stack mora biti pokrenut):
```bash
cd e2e
npx playwright test
```

**Napomena:** Testovi E2E-1, E2E-2 i E2E-3 ne zahtevaju prethodno prijavljivanje. Testovi E2E-4 i E2E-5 koriste programatsku prijavu direktnim API pozivom kako bi izbegli ponavljanje UI toka prijave — JWT token se upisuje u `localStorage` onako kako to Angular `AuthService` očekuje.

---

### E2E-1 — Uspešna prijava preusmerava korisnika na početnu stranicu

**Fajl:** `e2e/tests/login.spec.ts`

**Šta testira:** Kompletan tok prijave korisnika – unos kredencijala u formu, klik na dugme, navigacija na početnu stranicu i prikaz avatara umesto linka za prijavu.

**Kako radi:**
1. Otvara stranicu `/login` i proverava naslov `Prijava`.
2. Popunjava polja `#email` i `#password` sa kredencijalima seed korisnika (`jankovicmatejabp@gmail.com` / `Test@12345`).
3. Klik na `button[type="submit"]`, čeka navigaciju na `http://localhost:4200/`.
4. Proverava da je `.profile-btn` (avatar korisnika) vidljiv.
5. Proverava da `.btn-signin` (link "Prijava" u navbar-u) **nije** vidljiv.

Potvrđuje da Angular router ispravno preusmerava, da JWT token pristiže i čuva se u `localStorage`, i da navbar odražava stanje autentifikacije.

---

### E2E-2 — Pogrešna lozinka prikazuje poruku greške

**Fajl:** `e2e/tests/login.spec.ts`

**Šta testira:** Negativan scenario prijave – pogrešna lozinka ne sme da preusmerava korisnika i mora prikazati grešku.

**Kako radi:**
1. Otvara `/login`, popunjava `#email` sa tačnom adresom ali `#password` sa pogrešnom lozinkom.
2. Čeka HTTP odgovor od backend-a (`/api/auth/login`) sa statusom 400.
3. Proverava da je `.alert-error` vidljiv.
4. Proverava da URL i dalje sadrži `/login` (nije došlo do preusmeravanja).
5. Proverava da `.profile-btn` **nije** vidljiv (korisnik nije prijavljen).

Potvrđuje da Angular prikazuje serversku grešku korisniku i da ne dolazi do lažne autentifikacije.

---

### E2E-3 — Registracija validira formu i prikazuje poruku uspeha

**Fajl:** `e2e/tests/register.spec.ts`

**Šta testira:** Dvostruki scenario – najpre Angular validacija prazne forme (klijentska strana), potom uspešna registracija sa validnim podacima.

**Kako radi:**
1. Otvara `/register` i proverava naslov `Registracija`.
2. **Korak 1 – prazna forma:** klik na submit bez popunjavanja polja; proverava da `.error-message` elementi postanu vidljivi i da URL ostaje `/register`.
3. **Korak 2 – validni podaci:** popunjava sva polja (`#firstName`, `#lastName`, `#email` sa jedinstvenim timestamp emailom, `#username`, `#address`, `#password`, `#confirmPassword`).
4. Čeka HTTP odgovor `/api/auth/register` sa statusom 201.
5. Proverava da je `.alert-success` vidljiv i da sadrži tekst `Registracija uspešna`.

Pokriva i klijentsku validaciju (Angular Reactive Forms) i serverski uspešan odgovor u jednom toku.

---

### E2E-4 — Prijavljen korisnik kreira Watch Party sobu

**Fajl:** `e2e/tests/watch-party.spec.ts`

**Šta testira:** Kompletan tok kreiranja Watch Party sobe — prijava, navigacija, izbor videa, kreiranje sobe i provera invite linka.

**Kako radi:**
1. **Programatska prijava:** direktan API poziv na `/api/auth/login`, JWT token se upisuje u `localStorage['auth_token']`, a puna `AuthResponse` u `localStorage['current_user']`. Stranica se osveži da bi Angular preuzeo stanje.
2. Navigacija na `/watch-party`; čeka da `.create-party-btn` bude vidljiv (lista videa učitana).
3. Klik na prvo dugme "Kreiraj Watch Party"; čeka HTTP odgovor `POST /api/watch-party/create` sa statusom 200.
4. Proverava da URL prati obrazac `/watch-party/room/<uuid>`.
5. Proverava da `.invite-input` (readonly polje) sadrži UUID invite kod.
6. Proverava da `.info-row` sa labelom `Uloga` prikazuje `Kreator`.

Potvrđuje integraciju: Angular → JWT interceptor → Spring Security → WatchPartyService → kreiranje sobe u bazi → WebSocket priprema → navigacija → prikaz sobe.

---

### E2E-5 — ETL pipeline prikazuje popularne videe prijavljenom korisniku

**Fajl:** `e2e/tests/etl.spec.ts`

**Šta testira:** Kompletan tok ETL modula — od generisanja pregleda videa, preko pokretanja pipeline-a, do prikaza sekcije „Top 3 najpopularnija videa" na stranici „U trendu".

**Kako radi:**
1. **Generisanje pregleda:** 5× direktan API poziv `POST /api/videos/1/view` upisuje `VideoView` redove (podaci za Extract fazu iz poslednjih 7 dana).
2. **Pokretanje pipeline-a:** `POST /api/etl/run-pipeline` — proverava status 200 (Extract → Transform → Load).
3. **Backend verifikacija:** `GET /api/etl/popular-videos` vraća 200; proverava da `pipelineRunAt` postoji i da `topVideos` ima između 1 i 3 elementa.
4. **Programatska prijava** i navigacija na `/trending`.
5. Proverava da je `.popular-videos-section` vidljiva i da sadrži naslov „Top 3 najpopularnija videa" i vremensku oznaku (`.popular-timestamp`).
6. Proverava da je prikazana barem jedna `.popular-video-card` (max 3), sa rang badge-om „1" i tekstom „Popularnost" (popularity score).

Potvrđuje integraciju celog sistema: zabeleženi pregledi → `ETLService` (Extract/Transform/Load) → `PopularVideo` tabela → `GET /api/etl/popular-videos` → Angular `TrendingComponent` → prikaz top 3 videa korisniku.
