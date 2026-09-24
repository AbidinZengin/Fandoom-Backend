# Trivia — API Sözleşmesi (FE referansı)

Kaynak: gerçek kod (`trivia/controller|service|dto|entity|mapper`, `common/config/SecurityConfig`, `common/exception/GlobalExceptionHandler`, `trivia_tags_migration.sql`). Kod ile bu metin çeliştiğinde KOD esastır.

Yapım sayfalarındaki (film/dizi) "Biliyor muydunuz? / Kamera Arkası" hap bilgileri. Movie ve Series'e polimorfik bağlanır: `itemType` + `itemId`, gerçek FK yoktur.

---

## 0. Genel kurallar

- Base path: `/api/trivia`. Auth: `Authorization: Bearer <JWT>`.
- **Okuma (GET) public**, anonim çağrılır. **Yazma (POST/PUT/DELETE)** `EDITOR`, `MODERATOR` veya `ADMIN` rolü ister. Token yoksa/geçersizse `401`, rol yetmiyorsa `403` (ikisi de JSON).
- Tarih alanları: zaman dilimsiz ISO-8601 (`"2026-09-24T16:37:11.482913"`).
- JSON alan adları birebir kodla aynıdır; boolean `isSpoiler` **`is` öneki kalır**.
- **`itemType` küçük harf**: `"movie"` | `"series"`. İstekte (query ve JSON gövde) büyük/küçük harf fark etmez, yanıtta her zaman küçük harf.
- **Dil**: `Accept-Language` başlığı (`tr` | `en`, yoksa/başkasıysa `en`). `title` ve `content` seçilen dile göre çözülür; seçilen dilin alanı boşsa diğer dile düşer. `titleTr` / `contentTr` her zaman ham döner.
- Hata gövdesi (tüm modüllerle ortak):

```json
{
  "timestamp": "2026-09-24T16:40:02.118",
  "status": 400,
  "error": "Bad Request",
  "message": "Geçersiz movie id: 999",
  "path": "/api/trivia",
  "fieldErrors": null
}
```

Bean Validation hatasında `message` genel, `fieldErrors` alan bazlı mesaj listesidir.

---

## 1. Trivia nesnesi (yanıt)

```json
{
  "id": 12,
  "itemId": 45,
  "itemType": "movie",
  "title": "Ürdün'de çekim",
  "titleTr": "Ürdün'de çekim",
  "content": "Desert scenes were shot in Jordan.",
  "contentTr": "Çöl sahneleri Ürdün'de çekildi.",
  "imageUrl": "https://res.cloudinary.com/<cloud>/image/upload/....webp",
  "tag": "BEHIND_THE_SCENES",
  "tags": ["BEHIND_THE_SCENES", "Cinematography"],
  "isSpoiler": false,
  "sourceUrl": "https://example.com/makale",
  "createdBy": 7,
  "createdAt": "2026-09-24T16:37:11.482913",
  "updatedAt": "2026-09-24T16:37:11.482913"
}
```

| Alan | Tip | Not |
|---|---|---|
| `id`, `itemId`, `createdBy` | number | `createdBy` null olabilir (ekleyen kullanıcının id'si) |
| `itemType` | `"movie"` \| `"series"` | |
| `title` | string \| null | Dile göre çözülür; iki dil de boşsa `null` |
| `titleTr` | string \| null | Ham |
| `content` | string | Dile göre çözülür, her zaman dolu |
| `contentTr` | string \| null | Ham |
| `imageUrl` | string \| null | Cloudinary URL'i |
| `tags` | string[] | En az 1, en fazla 5. Sıra korunur. **Hazır değer ya da serbest metin** (aşağıya bak) |
| `tag` | string | `tags[0]` — birincil etiket, ikon için (spec uyumluluğu) |
| `isSpoiler` | boolean | `true` ise FE blur uygular |
| `sourceUrl` | string \| null | Kaynak linki |

### 1.1 Etiketler

Hazır (predefined) değerler — FE bunlara özel ikon çizer:

| Değer | Anlam |
|---|---|
| `BEHIND_THE_SCENES` | Kamera arkası |
| `EASTER_EGG` | Gönderme / sürpriz |
| `CASTING` | Oyuncu seçimi |
| `GOOF` | Çekim hatası |
| `LORE` | Evren / hikaye detayı |

**Bunların dışında serbest metin etiket de gelir** (ör. `"Cinematography"`, `"Müzik"`). FE, tanımadığı bir değer için **varsayılan bir ikon** göstermelidir. Hazır değerler her zaman BÜYÜK_HARF_ALT_ÇİZGİ olarak döner; serbest etiketler kullanıcının yazdığı gibi (büyük/küçük harf korunarak) döner.

---

## 2. `GET /api/trivia` — liste

Auth: yok (public).

| Query | Tip | Zorunlu | Not |
|---|---|---|---|
| `itemId` | number | evet | |
| `itemType` | `movie` \| `series` | evet | Büyük/küçük harf fark etmez |
| `limit` | number | hayır | ≥ 1. Verilmezse en fazla **100** kayıt döner; 100'den büyük değerler 100'e kısılır |
| `random` | boolean | hayır (default `false`) | `true` → DB seviyesinde rastgele sıra (her istekte farklı). `false` → yeniden eskiye (`createdAt DESC`) |

Yanıt `200`:

```json
{ "data": [ { /* Trivia nesnesi */ } ] }
```

Kayıt yoksa `{ "data": [] }`. Sayfalama yoktur; karusel için `limit=5&random=true` önerilir. Yanıt cache'lenmez.

Hatalar:

| Kod | Durum |
|---|---|
| 400 | `itemId`/`itemType` eksik ya da tipi hatalı, `itemType` `movie`/`series` dışında, `limit < 1` |

> Not: `itemId` bir yapıma ait olmasa bile hata dönmez, sadece boş liste gelir (GET yapım varlığını doğrulamaz).

---

## 3. `POST /api/trivia` — oluştur

Auth: `EDITOR` / `MODERATOR` / `ADMIN`. Başarılı yanıt `201` + Trivia nesnesi. `createdBy` token'daki kullanıcıdan otomatik atanır.

İstek gövdesi:

```json
{
  "itemId": 45,
  "itemType": "movie",
  "title": "Shot in Jordan",
  "titleTr": "Ürdün'de çekim",
  "content": "Desert scenes were shot in Jordan.",
  "contentTr": "Çöl sahneleri Ürdün'de çekildi.",
  "imageUrl": "https://res.cloudinary.com/.../image/upload/....webp",
  "tags": ["BEHIND_THE_SCENES", "Cinematography"],
  "isSpoiler": false,
  "sourceUrl": "https://example.com/makale"
}
```

| Alan | Zorunlu | Kural |
|---|---|---|
| `itemId` | evet | Var olan bir film/dizi id'si |
| `itemType` | evet | `movie` \| `series` |
| `title` / `titleTr` | hayır | ≤ 255. Boş/whitespace → `null` |
| `content` | **evet** | Boş olamaz, ≤ 2000 |
| `contentTr` | hayır | ≤ 2000. Boş/whitespace → `null` |
| `imageUrl` | hayır | ≤ 500. Önce `POST /api/media/images` ile yükle, dönen `url`'i buraya koy |
| `tags` | **`tag` ile birlikte en az biri** | ≤ 5 öğe, her biri ≤ 30 karakter, `<` `>` içeremez |
| `tag` | (yukarıya bak) | Tek etiket (spec uyumluluğu); `tags` ile birleştirilir, `tag` başa gelir |
| `isSpoiler` | hayır | Gönderilmezse `false` |
| `sourceUrl` | hayır | ≤ 500, `http://` veya `https://` ile başlamalı |

Etiket kuralları: her etiketin boşlukları sadeleştirilir; hazır bir değere denk geliyorsa (büyük/küçük harf, boşluk, tire fark etmez: `"behind the scenes"` → `BEHIND_THE_SCENES`) kanonik ada çevrilir; aynı etiket iki kez (büyük/küçük harf duyarsız) gelirse tekilleştirilir. `tag` + `tags` birleşiminin tekilleştirilmiş hali 1–5 arası olmalı.

Hatalar:

| Kod | Durum |
|---|---|
| 400 | Doğrulama hatası (`fieldErrors` dolu), geçersiz `itemType`, **`itemId` var olmayan yapım** (`"Geçersiz movie id: 999"`), hiç etiket yok (`"En az bir tag gerekli"`), 5'ten fazla etiket (`"En fazla 5 tag eklenebilir"`) |
| 401 | Token yok/geçersiz |
| 403 | Rol yetersiz |

---

## 4. `PUT /api/trivia/{id}` — güncelle

Auth: `EDITOR` / `MODERATOR` / `ADMIN`. **Tam değiştirme** (PATCH değil): gövde POST ile aynıdır ve gönderilmeyen opsiyonel alanlar temizlenir (`title`, `titleTr`, `contentTr`, `imageUrl`, `sourceUrl` → `null`, `isSpoiler` → `false`, etiketler gövdedekilerle değişir). Yanıt `200` + güncel Trivia nesnesi.

- `itemId`/`itemType` değişirse yeni yapımın varlığı yeniden doğrulanır; değişmezse doğrulama atlanır.
- `imageUrl` değişirse (ya da `null` gönderilirse) eski görsel Cloudinary'den otomatik silinir.
- `createdBy` değişmez.

Hatalar: POST'takilere ek olarak **404** (`id` yok).

---

## 5. `DELETE /api/trivia/{id}` — sil

Auth: `EDITOR` / `MODERATOR` / `ADMIN`. Yanıt `204`, gövde yok. Hard delete; trivia'nın görseli Cloudinary'den de silinir.

Hatalar: `404` (`id` yok), `401`, `403`.

---

## 6. FE notları

- Karusel: `GET /api/trivia?itemId=45&itemType=movie&limit=5&random=true`.
- `isSpoiler: true` kayıtlarda içeriği blur'la, tıklayınca aç.
- `imageUrl` yoksa görselsiz kart çiz; `title` yoksa yalnız `content` göster.
- İkon: `tag` (ya da `tags[0]`) hazır değerlerden biriyse ona özel ikon, değilse varsayılan ikon. Tüm etiketleri rozet olarak `tags` ile gösterebilirsin.
- Türkçe arayüzde `Accept-Language: tr` gönder; `title`/`content` otomatik Türkçe gelir (yoksa İngilizceye düşer). Admin formunda ham değerler için `titleTr`/`contentTr`'yi kullan.

## 7. Sapmalar ve varsayımlar

- Bu API projenin diğer uçlarından iki noktada farklıdır (FE spec'i gereği): `itemType` küçük harf, liste `{"data": [...]}` zarfıyla döner (diğer listeler düz dizi).
- Yazma yetkisi spec'teki "adminler" yerine projedeki içerik yazma rolleridir (`EDITOR`/`MODERATOR`/`ADMIN`).
- `createdBy` bir kullanıcı FK'sı değil, düz id'dir; kullanıcı silinse de kayıt kalır.
- Tablo: `trivia` (+ `trivia_tags`). Eski `production_trivia` adı/`tag` sütunu için `trivia_tags_migration.sql`.
