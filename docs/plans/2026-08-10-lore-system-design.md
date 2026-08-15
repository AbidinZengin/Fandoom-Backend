# Lore Sistemi Tasarımı — Timeline / Taksonomi / Lokasyon

**Tarih:** 2026-08-10
**Durum:** Kullanıcı ile brainstorming sonucu doğrulandı, implementasyona hazır.
**Kapsam:** Westeros özelinde ama herhangi bir yapıma (Movie/Series) uyarlanabilir bir "lore" sistemi — yapıma özel taksonomi kategorileri (Haneler, Ejderha Türleri...), lokasyonlar (harita pin'leri) ve kronolojik olaylar (timeline).

## Context

Mevcut `group/` modülü (`Group{type: FACTION|SPECIES}`), yapıma özel taksonomi değerlerini (House Stark, Dothraki gibi) Character'a atamak için kullanılıyordu. Ama kategori **adı** (Faction/Species) Java enum'una hardcode edilmişti — yeni bir yapımda "Canavar Türleri" ya da "Klanlar" gibi farklı bir kategori gerektiğinde enum'a değer eklemek, kod değişikliği + deploy gerektiriyordu. Bu, projenin "modüller yapıma özel olarak uyarlanabilir olmalı" hedefiyle çelişiyordu.

Ayrıca kullanıcı, Westeros lore'u için bir zaman çizelgesi (events) ve bir harita (x/y koordinatlı lokasyonlar) istiyor; bunların hem birbirleriyle hem mevcut Character/Group yapısıyla ilişkili olması gerekiyor, ama hiçbiri diğerinin şemasına sızmamalı (modül bağımsızlığı ilkesi).

Bu doküman, GroupType enum'unu admin-tanımlı dinamik bir kategori sistemine dönüştürüp, Location ve Event'i aynı `lore/` modülü altında bu sisteme bağlayan tasarımı kayıt altına alır.

## Kesin kararlar (brainstorming'de doğrulandı)

1. **Tek birleşik `lore/` modülü.** Mevcut `group/` modülü tamamen buraya taşınır (`person/`'ın Person+Character+Cast'i tek modülde toplama emsaliyle tutarlı). `group/` paketi silinir.
2. **TaxonomyCategory (yeni):** admin-tanımlı, **yapıma özel** kategori kataloğu (`GroupType` enum'unun yerini alır). İki farklı yapımda aynı isimli kategori olsa bile bağımsız kayıtlardır — global/ortak havuz değil.
3. **Group (güncellendi):** `type` enum'u kaldırılır, yerine `TaxonomyCategory`'ye gerçek `@ManyToOne` FK (`categoryId`) gelir. Kendi `subjectType`/`subjectId` alanları da kaldırılır — hangi yapıma ait olduğu artık `category` üzerinden türetilir (tek doğruluk kaynağı, stale-veri riski önlenir). Kategoriye özel serbest alanlar (sigil, güç özelliği vb.) için `customFields` (MySQL `JSON` kolonu, Java'da düz `String`) eklenir — backend içeriği yorumlamaz/doğrulamaz, opak blob olarak saklar (`ContentBlock.col/row` felsefesiyle aynı).
4. **GroupAssignment (değişmiyor):** Group'u Character'a bağlamaya devam eder. `taggableType`'a `EVENT` **eklenmez** — Event'in grup/karakter katılımcılığı tamamen `EventParticipant` üzerinden çözülür (aynı ilişkiyi iki tabloda tutmamak için).
5. **Location (yeni, hafif):** `id, name, slug, imageUrl, description, customFields, subjectType, subjectId`. Hiyerarşi yok (düz liste — YAGNI). **REVİZE (2026-08-10, ikinci tur):** `x`/`y` başlangıçta çekirdek kolon olarak eklenmişti, sonra geri alındı — gerekçe: harita koordinatı yapıma özel bir SUNUM kararı (bazı yapımların haritası olmayabilir, 2D koordinat yerine bambaşka bir gösterim isteyebilir), tıpkı `GroupType`'ın hardcode edilmiş olması gibi bir genellik ihlaliydi. Bunun yerine:
   - `description` (TEXT) → gerçek çekirdek kolon (neredeyse her entity'de olan evrensel alan).
   - `customFields` (JSON, opak) → `x`/`y`/`scale` gibi **sunuma özgü, yapıma özel** alanlar buraya gider (Group.customFields'la aynı felsefe — backend içeriği doğrulamaz/yorumlamaz). Konvansiyon: Westeros için Fandoom frontend'indeki `WorldMap.data.js`'in `camera.x/y` ile aynı **0.0–1.0 fraksiyonel skala** (`{"x":0.42,"y":0.67,"scale":1.0}` gibi) — ama bu backend'de zorlanmaz, sadece bu yapım için admin'in uyması gereken bir sözleşme.
   - **Çoklu görsel desteklenmiyor** (tek `imageUrl` yeterli, YAGNI) — eğer ileride gerçekten gerekirse `customFields`'a DEĞİL, ayrı bir `LocationImage` çocuk tablosuna gider, çünkü görsel URL'lerinin Cloudinary temizliği (`ImageStorageService.deleteIfChanged`) için backend'in bunları GÖRMESİ gerekiyor — opak blob'a gömülürse yetim Cloudinary dosyaları birikir.
   - **"Bu yer hangi haneye ait" (house) customFields'a DEĞİL**, mevcut `GroupAssignment` mekanizmasına gider — `TaggableType` enum'una `LOCATION` eklendi (`CHARACTER, LOCATION`), böylece `POST /api/lore/groups/{groupId}/assignments` ile `taggableType=LOCATION` kullanılarak gerçek bir ilişki kurulur, opak veri değil.
6. **Event (yeni):** `id, name, description, orderIndex (kronolojik sıralama — TEK sıralama kaynağı), imageUrl, subjectType, subjectId, location (nullable @ManyToOne FK — aynı modül içi ilişki), pinned (gerçek boolean — EpisodeBlock.pinned emsaliyle tutarlı, filtrelenebilir olasılığı yüksek), customFields (JSON, opak)`. **REVİZE (2026-08-11):** Gerçek kullanım verisiyle (Westeros timeline seed'i) customFields ihtiyacı netleşti — görünen tarih metni (`date`: "ca. 12,000 BC"), alıntı (`quote`), ton (`tone`), birincil `locationId` dışındaki ek yer adları (`locations`: string[]) buraya gider. **Kritik kural: `sortValue` customFields'a KONMAZ** — kronolojik sıralama her zaman `orderIndex`'e (negatif olabilir, ör. `-12000`) gider; ikinci bir opak "sıralama kaynağı" eklemek `orderIndex` ile çelişip hangisinin geçerli olduğu belirsizleşen sessiz bir tutarsızlık riski yaratır. `pinned`, "sadece pinlenmiş olayları filtrele" ihtimaline karşı bilinçli olarak gerçek kolon yapıldı (customFields'a değil) — EpisodeBlock.pinned emsaliyle tutarlı.
7. **EventParticipant (yeni):** Event'e hem Character (cross-module) hem Group (aynı modül) katılımcı olarak atanabilir — `participantType(CHARACTER|GROUP) + participantId(Long, ID-only, GroupAssignment'ın taggableId deseniyle tutarlı)`.

## Entity/Repository/DTO/Mapper/Service tasarımı

Tüm entity'ler `common.entity.Auditable` extend eder, mevcut Lombok şablonunu (`@Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor/@Builder/@EqualsAndHashCode(onlyExplicitlyIncluded)/@ToString`) izler.

```java
// TaxonomyCategory — taxonomy_category tablosu
id, name, slug (uk), subjectType(enum MOVIE|SERIES), subjectId(Long)

// Group — content_group tablosu (mevcut tablo adı korunur)
id, name, slug (uk), imageUrl(nullable)
@ManyToOne category -> TaxonomyCategory (fk_group_category, nullable=false)
@Column(columnDefinition="json") customFields(String, nullable)

// GroupAssignment — group_assignment tablosu (değişmedi)
id, @ManyToOne group -> Group, taggableType(enum: CHARACTER), taggableId(Long)

// Location — location tablosu (REVİZE: x/y kaldırıldı, bkz. madde 5)
id, name, slug(uk), imageUrl(nullable), description(TEXT, nullable),
customFields(JSON, nullable — {"x":..,"y":..,"scale":..} gibi, opak),
subjectType(enum), subjectId(Long)

// Event — event tablosu
id, name, description(TEXT), orderIndex(int), imageUrl(nullable),
subjectType(enum), subjectId(Long)
@ManyToOne location -> Location (fk_event_location, nullable=true)
@OneToMany(mappedBy="event", cascade=ALL, orphanRemoval=true) participants

// EventParticipant — event_participant tablosu
id, @ManyToOne event -> Event (fk_event_participant_event),
participantType(enum: CHARACTER|GROUP), participantId(Long, ID-only)
uk: (event_id, participant_type, participant_id)
```

**Servis katmanı doğrulama kuralları:**
- `GroupService.addTo{Movie|Series}`: category var mı + **category'nin subject'i, hedef yapımla eşleşiyor mu** kontrolü (`InvalidReferenceException` — başka yapımın kategorisi yanlışlıkla bağlanamaz).
- `EventService`: `locationId` verilmişse Location var mı + aynı subject kontrolü.
- `EventParticipantService.assertParticipantExists`: `CHARACTER` → `characterService.existsById` (person modülünden inject, cross-module); `GROUP` → aynı modül içi `GroupRepository.existsById` (doğrudan).
- Slug üretimi her yerde `SlugGenerator.generateUnique` (mevcut `FranchiseServiceImpl` deseniyle aynı).

## Endpoint tasarımı

`/api/lore/**` prefix'i (CMS/auth gibi "bütünsel alt sistem" örüntüsü). GET public, POST/PUT/DELETE `EDITOR/MODERATOR/ADMIN`.

```
GET/POST   /api/{movies|series}/{id}/lore/categories
GET/PUT/DELETE /api/lore/categories/{id}
GET        /api/lore/categories/slug/{slug}

GET/POST   /api/{movies|series}/{id}/lore/groups?categoryId=
GET/PUT/DELETE /api/lore/groups/{id}
GET        /api/lore/groups/assignments?taggableType=&taggableId=
POST/DELETE /api/lore/groups/{groupId}/assignments[/{id}]

GET/POST   /api/{movies|series}/{id}/lore/locations
GET/PUT/DELETE /api/lore/locations/{id}

GET/POST   /api/{movies|series}/{id}/lore/events   (orderIndex sıralı)
GET/PUT/DELETE /api/lore/events/{id}
GET/POST/DELETE /api/lore/events/{eventId}/participants[/{id}]
```

`common/config/SecurityConfig` içindeki iki path listesine `/api/lore/**` eklenir (mevcut `/api/groups/**` girdisinin yerine).

## Migration planı (`lore_migration.sql`)

`contentblock_migration.sql` ile aynı desen:
1. `content_group`/`group_assignment` → `_old` olarak yeniden adlandır.
2. Uygulamayı `ddl-auto=update` ile başlatıp yeni şemayı (taxonomy_category, content_group, group_assignment, location, event, event_participant) kurdur, durdur.
3. `content_group_old`'daki her benzersiz `(subject_type, subject_id, type)` kombinasyonu için bir `TaxonomyCategory` satırı türet (FACTION→"Faction", SPECIES→"Species"); `content_group_old` verisini id'leri koruyarak yeni `content_group`'a taşı (`category_id` join ile bulunur); `group_assignment_old`'u aynen taşı (id/FK referansları değişmez).
4. `AUTO_INCREMENT` sayaçlarını `MAX(id)+1`'e çek, `GET /api/lore/groups/...` ile doğrula, sorun yoksa `_old` tabloları sil.

`location`/`event`/`event_participant` için taşınacak eski veri yok (Hibernate boş kurar).

## Test planı

Mevcut `BlogServiceImplTest`/`BlogControllerTest` deseniyle paralel:
- **Servis (JUnit+Mockito):** her servis için CRUD + doğrulama senaryoları (geçersiz `categoryId`, başka subject'e ait `locationId`, `customFields`'in opak persist edildiği, `EventParticipant` için `CHARACTER`/`GROUP` doğrulama dallanması).
- **Controller (`@SpringBootTest`+`MockMvc`, gerçek `SecurityConfig`):** her kaynak için GET public / yazma auth'suz→401 / EDITOR+→başarılı / USER→403.
- **Repository (`@DataJpaTest`, opsiyonel ama önerilir):** Event silme → EventParticipant cascade+orphanRemoval doğrulaması.

## Sonraki adım

Kullanıcı onayı ile implementasyona geçilecek — sırasıyla: entity/repository → migration script → dto/mapper → service → controller → SecurityConfig → testler.
