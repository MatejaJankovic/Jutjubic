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

*(Biće dodato)*

---

## E2E testovi

*(Biće dodato)*
