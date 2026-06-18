# Testovi – Jutjubic projekat

## Sadržaj

- [Unit testovi](#unit-testovi)
  - [AuthServiceTest](#authservicetest)
  - [WatchPartyServiceTest](#watchpartyservicetest)
  - [ETLServiceTest](#etlservicetest)
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

**Zašto je smislen:** Potvrđuje da se validacija lozinke dešava na prvom mestu, pre pristupa bazi.

---

#### Test 2 — `register_throwsWhenEmailAlreadyExists`

**Šta testira:** Sprečavanje duplih registracija sa istim emailom.

**Kako radi:**
1. Kreira `RegisterRequest` sa validnim podacima i poklapajućim lozinkama.
2. **Mokuje** `userRepository.existsByEmail(...)` da vrati `true` (email postoji).
3. Poziva `authService.register(request)`.
4. Proverava da je bačen `BadRequestException` sa porukom `"Email adresa je već u upotrebi"`.
5. Verifikuje da `userRepository.save(...)` **nikada nije pozvan**.

**Zašto je smislen:** Garantuje da se dupli nalog ne može kreirati čak i ako se zaobiđe frontend validacija.

---

#### Test 3 — `login_throwsBadRequestExceptionOnWrongCredentials`

**Šta testira:** Rukovanje pogrešnim kredencijalima pri prijavi.

**Kako radi:**
1. Kreira `LoginRequest` sa neispravnom lozinkom.
2. **Mokuje** `authenticationManager.authenticate(...)` da baci `BadCredentialsException`.
3. Poziva `authService.login(request)`.
4. Proverava da `AuthService` pretvara Spring Security izuzetak u domenski `BadRequestException` sa odgovarajućom porukom.

**Zašto je smislen:** Testira da se Spring Security izuzeci pravilno mapiraju u API odgovore razumljive klijentu.

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

**Zašto je smislen:** Pokriva centralnu funkcionalnost Watch Party-ja i garantuje jedinstvenost invite koda i ispravno inicijalno stanje sobe.

---

#### Test 5 — `joinRoom_throwsRuntimeExceptionWhenInviteCodeNotFound`

**Šta testira:** Rukovanje nevalidnim invite kodom pri pridruživanju sobi.

**Kako radi:**
1. **Mokuje** `watchPartyRepository.findByInviteCode("nepostojeci-kod")` da vrati `Optional.empty()`.
2. Poziva `watchPartyService.joinRoom("nepostojeci-kod", 1L)`.
3. Proverava da je bačen `RuntimeException` sa porukom `"Watch party not found"`.
4. Verifikuje da `repository.save(...)` **nikada nije pozvan**.

**Zašto je smislen:** Osigurava da korisnik ne može da se pridruži nepostojećoj sobi, a da pritom nema neželjenih efekata na bazu.

---

#### Test 6 — `switchVideo_throwsWhenUserIsNotCreator`

**Šta testira:** Autorizaciono pravilo – samo kreator sobe može menjati video.

**Kako radi:**
1. Kreira `WatchParty` objekat sa `creatorId = 10L`.
2. **Mokuje** `watchPartyRepository.findById(1L)` da vrati tu sobu.
3. Poziva `watchPartyService.switchVideo(1L, 5L, 99L)` – korisnik `99L` nije kreator.
4. Proverava da je bačen `RuntimeException` sa porukom `"Only the room owner can switch videos"`.
5. Verifikuje da `repository.save(...)` i `messagingTemplate.convertAndSend(...)` **nikada nisu pozvani** (nema neželjenog broadcast-a).

**Zašto je smislen:** Testira kritično sigurnosno pravilo i potvrđuje da se WebSocket poruka ne šalje u slučaju neautorizovanog pristupa.

---

### ETLServiceTest

**Lokacija:** `src/test/java/rs/ftn/isa/jutjubicbackend/service/ETLServiceTest.java`

**Klasa koja se testira:** `ETLService`

**Mokovane zavisnosti:**
- `VideoViewRepository` – repozitorijum pregleda videa
- `PopularVideoRepository` – repozitorijum popularnih videa
- `VideoRepository` – repozitorijum videa

---

#### Test 10 — `getLatestPopularVideos_returnsEmptyOptionalWhenNoPipelineRunYet`

**Šta testira:** Ponašanje kada pipeline još nije ni jednom pokrenut.

**Kako radi:**
1. **Mokuje** `popularVideoRepository.findLatest()` da vrati `Optional.empty()`.
2. Poziva `etlService.getLatestPopularVideos()`.
3. Proverava da je vraćen prazan `Optional`.
4. Verifikuje da je `findLatest()` pozvan tačno jednom.

**Zašto je smislen:** Pokriva edge case – sistem mora elegantno da odgovori pre prvog ETL pokretanja, bez izuzetaka.

---

#### Test 11 — `runETLPipeline_savesPopularVideoRecordEvenWithNoViews`

**Šta testira:** Robusnost ETL pipeline-a kada nema pregleda u poslednjih 7 dana.

**Kako radi:**
1. **Mokuje** `videoViewRepository.findViewsSince(...)` da vrati praznu listu.
2. **Mokuje** `popularVideoRepository.save(...)` da vrati prosleđeni objekat.
3. Poziva `etlService.runETLPipeline()`.
4. Proverava da nije bačen izuzetak (`assertDoesNotThrow`).
5. Verifikuje da je `videoViewRepository.findViewsSince(...)` pozvan (Extract faza izvršena).
6. Verifikuje da je `popularVideoRepository.save(...)` pozvan (Load faza izvršena, čak i sa praznim rezultatom).
7. Verifikuje da `videoRepository` **nije ni pozvan** (nije potreban kada nema pregleda).

**Zašto je smislen:** Potvrđuje da pipeline ne pada pri praznim podacima i da sve tri faze (Extract, Transform, Load) funkcionišu ispravno i u tom slučaju.

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

**Zašto je smislen:** Testira pravu integraciju: HTTP deserijalizacija → Controller → servisni poziv → serializacija odgovora.

---

#### Test IT-2 — `register_returns400WithValidationErrorsWhenBodyIsInvalid`

**Šta testira:** Spring `@Valid` anotacija i `GlobalExceptionHandler` vraćaju 400 sa detaljima greške validacije.

**Kako radi:**
1. Šalje `POST /api/auth/register` sa nevalidnim podacima (prazan email, kratko korisničko ime).
2. Spring MVC automatski poziva Hibernate Validator (jer controller ima `@Valid`).
3. `GlobalExceptionHandler.handleValidationExceptions()` hvata grešku.
4. Proverava da je HTTP status **400**, da JSON sadrži `error: "Validation Failed"` i mapu `errors`.

**Zašto je smislen:** Testira pravu integraciju: HTTP sloj → Bean Validation → GlobalExceptionHandler → strukturirani JSON odgovor sa poljem `errors`.

---

#### Test IT-3 — `register_returns400WithMessageWhenServiceThrowsBadRequest`

**Šta testira:** `BadRequestException` iz `AuthService` prolazi kroz `GlobalExceptionHandler` i vraća 400 sa poljem `message`.

**Kako radi:**
1. Šalje validan `POST /api/auth/register`.
2. Mock `authService.register(...)` baca `BadRequestException("Email adresa je već u upotrebi")`.
3. `GlobalExceptionHandler.handleBadRequestException()` hvata izuzetak.
4. Proverava da je HTTP status **400** i da JSON odgovor sadrži tačnu poruku.

**Zašto je smislen:** Testira pravu integraciju između controller-a i GlobalExceptionHandler-a: izuzetak iz servisa → handler → ErrorResponse JSON (sa poljem `message`, a ne `success`).

---

#### Test IT-4 — `login_returns200WithTokenWhenCredentialsAreValid`

**Šta testira:** Uspešna prijava integrisan sa rate limiterom vraća 200 sa JWT tokenom u JSON-u.

**Kako radi:**
1. Mock `rateLimiterService.isBlocked(...)` vraća `false` (IP nije blokiran).
2. Mock `authService.login(...)` vraća `AuthResponse` sa JWT tokenom.
3. Šalje `POST /api/auth/login` sa validnim podacima.
4. Proverava da je HTTP status **200** i da JSON odgovor sadrži `token`, `type: "Bearer"` i `email`.

**Zašto je smislen:** Testira pravu integraciju između `AuthController`, `LoginRateLimiterService` i formata odgovora: oba servisa moraju biti ispravno pozvana da bi se vratio 200.

---

#### Test IT-5 — `login_returns429WithRetryAfterHeaderWhenIpIsBlocked`

**Šta testira:** Blokiran IP od strane rate limitera → `RateLimitExceededException` → `GlobalExceptionHandler` → 429 sa `Retry-After` headerom.

**Kako radi:**
1. Mock `rateLimiterService.isBlocked(...)` vraća `true`.
2. Mock `rateLimiterService.getSecondsUntilReset(...)` vraća `42`.
3. Proverava da je HTTP status **429 Too Many Requests**.
4. Proverava da HTTP header `Retry-After` ima vrednost `"42"`.
5. Proverava da JSON sadrži `status: 429`.

**Zašto je smislen:** Testira niz interakcija: `LoginRateLimiterService` → Controller → `RateLimitExceededException` → `GlobalExceptionHandler` → HTTP header + status + JSON telo.

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

**Zašto je smislen:** Testira pravu integraciju security filtera i konfiguracije: `JwtAuthenticationFilter` → `SecurityConfig` pravila → 403 odgovor. Ovo je ključan test koji proverava da zaštićeni endpointi zaista jesu zaštićeni.

---

#### Test IT-7 — `getWatchParty_returns404WhenRoomDoesNotExist`

**Šta testira:** Autentifikovani GET zahtev za nepostojećom sobom prolazi kroz SecurityContext i vraća 404.

**Kako radi:**
1. `@WithMockUser` postavlja korisnika u SecurityContext (simulira JWT autentifikaciju).
2. Mock `watchPartyService.findById(999L)` vraća `Optional.empty()`.
3. Šalje `GET /api/watch-party/999`.
4. Controller poziva `findById()`, dobija prazan Optional i vraća `ResponseEntity.notFound()`.
5. Proverava da je HTTP status **404 Not Found**.

**Zašto je smislen:** Testira integraciju: Spring Security propušta autentifikovani zahtev → Controller poziva servis → Optional.empty() → 404 odgovor.

---

#### Test IT-8 — `getWatchParty_returns200WithDtoWhenRoomExists`

**Šta testira:** GET zahtev za postojećom sobom vraća 200 sa ispravno serijalizovanim `WatchPartyDTO` JSON-om.

**Kako radi:**
1. `@WithMockUser` autentifikuje korisnika.
2. Mock `watchPartyService.findById(1L)` vraća `Optional` sa `WatchParty` entitetom.
3. Šalje `GET /api/watch-party/1`.
4. Controller poziva `WatchPartyDTO.fromEntity()` i Jackson serijalizuje DTO.
5. Proverava da JSON sadrži `id: 1`, `inviteCode: "abc-123-def"` i `public: true` (ne `isPublic`).

**Zašto je smislen:** Testira integraciju serijalizacije: `WatchParty` entitet → `WatchPartyDTO.fromEntity()` → Jackson → JSON sa `public` (umesto `isPublic`, jer Lombok/Jackson skida prefiks `is` kod boolean gettera).

---

## E2E testovi

*(Biće dodato)*
