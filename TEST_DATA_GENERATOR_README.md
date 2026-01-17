# 🧪 Test Data Generator

## Implementirano

Sistem za generisanje 5000 test videa sa random koordinatama širom Evrope za testiranje tile-based učitavanja na mapi.

**Backend komponente:**
- `TestDataGenerator` - Service za generisanje test podataka
- `AdminController` - REST API endpoint-i (/api/admin/*)
- Batch insert optimizacija (100 videa po batch-u)

**Karakteristike:**
- Geografski raspon: Evropa (35°N-70°N, 10°W-40°E)
- 45 evropskih gradova (Paris, London, Berlin, Novi Sad...)
- Realistična view count distribucija (70%/20%/10%)
- Random datumi (zadnjih 12 meseci)
- Vreme generisanja: ~2-5 minuta za 5000 videa

## Prerequisites

1. **Backend pokrenut:**
```bash
cd jutjubic-backend
./mvnw spring-boot:run
```

2. **Placeholder fajlovi:**
```
uploads/videos/test-video.mp4
uploads/thumbnails/test-thumbnail.jpg
```

Kopiraj bilo koji postojeći video/thumbnail ili kreiraj dummy fajlove.

## Korišćenje - Swagger

### 1. Otvori Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### 2. Pronađi Admin sekciju
Skroluj do **Admin** grupe endpoint-a

### 3. Generiši test videe
- Klikni `POST /api/admin/generate-test-videos`
- Klikni **Try it out**
- Parametar `count`: 5000 (default) ili custom broj
- Klikni **Execute**

**Response:**
```json
{
  "message": "Successfully generated 5000 test videos...",
  "count": 5000
}
```

### 4. Obriši test videe (opciono - na kraju testiranja)
- Klikni `DELETE /api/admin/delete-test-videos`
- Klikni **Try it out**
- Klikni **Execute**

## Testiranje

### 1. Proveri u bazi (opciono)
```sql
SELECT COUNT(*) FROM videos WHERE latitude IS NOT NULL;
```

### 2. Testiraj na mapi (frontend)
```
http://localhost:4200/map
```
- Zoom out - vidi celu Evropu
- Pomeri mapu - tile-based učitavanje
- Zoom in/out - različit broj videa po zoom nivou

### 3. Performance test
- Zoom < 6: Max 50 videa
- Zoom 6-10: Max 200 videa
- Zoom >= 11: Svi videi u region-u

## Napomena

⚠️ Admin endpoint-i su otvoreni za development. Pre produkcije zaštiti sa:
```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

