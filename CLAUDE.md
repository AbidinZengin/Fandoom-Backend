# CLAUDE.md

Bu dosya, bu depoda çalışırken Claude Code'a rehberlik eder.

## Proje

**Fandoom Backend** — fandom/wiki tarzı bir içerik platformunun REST API backend'i.

Kapsam (mevcut yol haritası):
- Fandom/Wiki içerik yönetimi (makale/sayfa oluşturma, düzenleme)
- Kullanıcı üyelik & profil (kayıt, giriş, roller)
- Yorum / tartışma sistemi
- Medya / dosya yükleme

Frontend ayrı bir projede; bu depo yalnızca backend'i kapsar.

## Stack

- **Java 21**, **Spring Boot 4.1.0**
- Spring Data JPA, Spring Web MVC, Spring Web Services
- **MySQL** (`mysql-connector-j`)
- Lombok
- Maven (`mvnw` / `mvnw.cmd`)

## Build & Çalıştırma

```bash
./mvnw spring-boot:run     # uygulamayı çalıştır
./mvnw test                 # testleri çalıştır
./mvnw clean package        # derle ve paketle
```

## Konfigürasyon & Secrets

Hassas değerler (`spring.datasource.password` vb.) `application.properties` içine **düz metin yazılmaz** — ortam değişkeninden okunur (`${DB_PASSWORD}` gibi). Yerel geliştirmede çalıştırmadan önce ortam değişkenlerini set edin (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) — IDE run configuration üzerinden veya terminalde `export`/`$env:` ile.

Cloudinary kimlik bilgileri de aynı şekilde `CLOUDINARY_URL` ortam değişkeninden okunur (`cloudinary://api_key:api_secret@cloud_name` formatı) — `media/config/CloudinaryConfig` bunu `new Cloudinary()` ile otomatik okur, kodda hiçbir yerde hardcode edilmez. Set edilmezse uygulama açılır ama görsel yükleme/silme çağrıları hata verir.

## Mimari Konvansiyonlar

Genel mimari: **RESTful API**, mümkün olduğunca **SOLID**'e sadık, ölçeklenebilir ve modüler. Temel ilke: **bağımsızlık** — bir modüle dokunmak yalnızca o modülün kendi paketinde değişiklik gerektirmeli, başka hiçbir yeri etkilememeli.

### Paket yapısı — özellik bazlı (feature-based)

Her domain kendi paketinde, kendi mini-katmanlarıyla birlikte izole yaşar:

```
com.example.fandoom_backend
├── franchise/              # Üst düzey gruplama (ör. bir evren/marka)
│   ├── entity/             # Franchise
│   ├── repository/
│   ├── dto/                # FranchiseRequest, FranchiseSummaryResponse, FranchiseDetailResponse (record)
│   ├── mapper/             # FranchiseMapper (MapStruct)
│   ├── service/            # FranchiseService interface + FranchiseServiceImpl
│   └── controller/         # FranchiseController
├── genre/
│   ├── entity/             # Genre
│   ├── repository/
│   ├── dto/                # GenreRequest, GenreResponse (record)
│   ├── mapper/             # GenreMapper (MapStruct)
│   ├── service/            # GenreService interface + GenreServiceImpl
│   └── controller/         # GenreController
├── movie/
│   ├── entity/             # Movie — franchiseId/genreIds sadece ID (cross-module)
│   ├── repository/
│   ├── dto/                # MovieRequest, MovieSummaryResponse, MovieDetailResponse (record)
│   ├── mapper/             # MovieMapper (MapStruct)
│   ├── service/            # MovieService interface + MovieServiceImpl
│   └── controller/         # MovieController
├── series/                 # Series + Season + Episode — TEK modül (aggregate)
│   ├── entity/             # Series, Season, Episode, SeriesStatus
│   ├── repository/         # SeriesRepository, SeasonRepository, EpisodeRepository
│   ├── dto/                # Series/Season/Episode Request + Summary/Detail response (record)
│   ├── mapper/             # SeriesMapper(uses SeasonMapper(uses EpisodeMapper))
│   ├── service/            # SeriesService, SeasonService, EpisodeService (+ *ServiceImpl)
│   └── controller/         # SeriesController, SeasonController, EpisodeController
├── person/                 # Person (gerçek insan) + Character (kurgusal, global) + Cast (credit)
│   ├── entity/             # Person, Character, Cast, SubjectType(MOVIE/SERIES)
│   ├── repository/
│   ├── dto/                # Person/Character Request+Response, CastRequest, CastResponse (nested)
│   ├── mapper/             # PersonMapper, CharacterMapper, CastMapper(uses ikisini de)
│   ├── service/            # PersonService, CharacterService, CastService (+ *ServiceImpl)
│   └── controller/         # PersonController, CharacterController, CastController
├── production/             # Movie+Series orkestrasyon (entity/repository YOK, salt-okunur)
│   ├── dto/                # ProductionSummaryResponse(type: MOVIE/SERIES), ProductionType
│   ├── service/            # ProductionService — MovieService+SeriesService inject eder
│   └── controller/         # ProductionController (/api/productions — birleşik feed)
├── user/                   # Henüz yok — hesap/auth modülü ileride eklenecek
│   ├── controller/
│   ├── service/           # UserService interface + UserServiceImpl
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── mapper/
├── community/              # Henüz yok — TODO: "comment" fikri Community
│   │                       # özelliğine genişledi (Blog/Discussion/Theory/
│   │                       # Fan Art + Vote/Tag/Report/ModerationAction).
│   │                       # Taslak tasarım Fandoom frontend deposunda
│   │                       # (docs_dev/, learned-rules/SKILL.md "Topluluk"
│   │                       # bölümü) hazır — buraya henüz kod yazılmadı.
│   │                       # Bağımlılık: authorId için user/ modülü önce
│   │                       # kurulmalı (Thread/Post/Vote/Report authorId
│   │                       # taşıyor). franchiseId zaten var olan
│   │                       # franchise/ modülüne cross-module ID referansı.
│   └── ... (aynı iç yapı, ilk adım: Category — bağımsız, sıfır
│       cross-module referans, en düşük efor)
├── media/                  # Görsel yükleme — Cloudinary
│   ├── config/             # CloudinaryConfig (Cloudinary bean, CLOUDINARY_URL'den)
│   ├── dto/                # MediaUploadResponse(url, publicId)
│   ├── service/            # ImageStorageService interface + CloudinaryImageStorageService
│   └── controller/         # MediaController (/api/media/images)
├── cms/                    # Sayfa/component içerik yönetimi — bkz. "CMS Modülü" bölümü
│   ├── entity/             # PageContent, PageName, SectionName, ContentType (enum'lar)
│   ├── repository/         # PageContentRepository
│   ├── dto/                # PageContentRequest, PageContentResponse (record)
│   ├── mapper/             # PageContentMapper (MapStruct)
│   ├── service/            # PageContentService interface + PageContentServiceImpl
│   └── controller/         # CmsController (/api/cms)
├── common/                 # Modüller arası paylaşılan gerçekten jenerik kod
│   ├── entity/             # Auditable (@MappedSuperclass — createdAt/updatedAt)
│   ├── dto/                # PageResponse<T> (record — Page<T> sarmalayıcı)
│   ├── util/               # SlugGenerator (isimden slug üretimi, domain-agnostic)
│   ├── config/             # JpaAuditingConfig, Security, CORS, Jackson, vb.
│   └── exception/          # ResourceNotFoundException, InvalidReferenceException, DuplicateResourceException, ApiErrorResponse, GlobalExceptionHandler
└── FandoomBackendApplication.java
```

### Bağımsızlık kuralları

- **Bir modül başka bir modülün `entity`/`repository` paketine asla doğrudan erişmez.** İhtiyaç varsa, ilgili modülün `service` **interface**'i inject edilir (ör. `CommentService`, `WikiArticle`'a yorum sayısını `WikiService` üzerinden değil, kendi repository'sinden okur; `wiki` modülü yorum sayısı istiyorsa `CommentService` interface'ini çağırır).
- Entity'ler kendi modülünün dışına asla sızmaz; modüller arası veri alışverişi sadece **DTO** ile olur.
- `common/` paketine yalnızca gerçekten tüm modüllerin ihtiyaç duyduğu, domain'e özgü olmayan kod girer (generic exception, güvenlik config). Bir özelliğe özel kod asla `common/`'a konmaz.
- **Cross-module referanslar = sadece ID.** `Movie`/`Series` gibi entity'ler başka modüllere (`Franchise`, `Genre`) JPA `@ManyToOne`/`@ManyToMany` ile bağlanmaz; sadece düz `Long franchiseId` / `Set<Long> genreIds` alanı taşırlar. Bu kural DTO katmanında da aynen geçerlidir — `MovieDetailResponse`/`SeriesDetailResponse` de `franchiseId`/`genreIds`'i ham ID olarak taşır, "franchise adını göster" gibi zenginleştirmeler ileride eklenecek servis-orkestrasyon katmanının işidir. **İstisna**: bir modülün kendi aggregate'i içindeki ilişkiler (ör. `series` modülünde Series→Season→Episode) gerçek JPA ilişkisi ve MapStruct nested-DTO kompozisyonuyla kurulur, çünkü bu ilişki modül sınırını aşmaz.
- **DTO'lar Java `record`**, entity→DTO dönüşümü her modülün kendi `mapper/` paketindeki MapStruct arayüzleriyle (`@Mapper(componentModel = "spring")`) yapılır; aggregate içi kompozisyon `@Mapper(uses = ...)` ile zincirlenir (ör. `SeriesMapper` → `SeasonMapper` → `EpisodeMapper`).
- **Listeleme endpoint'leri pagination'lı**: gerçek listeleme olan yerlerde (`Movie`, `Series`, `Franchise`) repository `Page<T>` döner, controller katmanında ortak `common/dto/PageResponse<T>` ile sarmalanır. Sabit/küçük iç listeler (bir sezonun bölümleri gibi) pagination'sız düz `List<T>` kullanır.
- **Cross-module referans doğrulaması servis katmanında yapılır.** Movie/Series servisleri create/update sırasında `franchiseId`/`genreIds`'i, `FranchiseService`/`GenreService` **interface**'ini inject ederek doğrular (`existsById`, `assertAllExist`) — geçersiz ID `InvalidReferenceException` (400) fırlatır. Bu, entity/repository seviyesinde DB FK'sı olmayan cross-module referansların uygulama seviyesinde tutarlılığını sağlar.
- **Slug'lar otomatik üretilir**, client göndermez. `common/util/SlugGenerator.generateUnique(name, existsBySlugPredicate)` — Türkçe karakter normalizasyonu + kebab-case + çakışma durumunda `-2`, `-3` gibi sayısal sonek. İsim değişmediği sürece güncellemede slug korunur (URL kararlılığı için).
- **Hata yönetimi ortak**: `common/exception/GlobalExceptionHandler` (`@RestControllerAdvice`) tüm modüllerde `ResourceNotFoundException`→404, `InvalidReferenceException`→400, `DuplicateResourceException`→409, `MethodArgumentNotValidException` (Bean Validation, `@Valid`)→400 (alan hatalarıyla), `DataIntegrityViolationException`→409 (DB kısıt ihlali için son çare) şeklinde `ApiErrorResponse` formatına çevirir. Modüle özel exception sınıfı gerekmedikçe bu üç genel exception kullanılır.
- **Delete = hard delete.** Soft-delete/is-active alanı yok; basitlik tercih edildi. Series silme, JPA cascade+orphanRemoval sayesinde season/episode'ları da siler.
- **Polimorfik cross-module credit (Cast) deseni**: `person/` modülündeki `Cast` entity'si, Movie veya Series'e `subjectType`(enum) + `subjectId`(Long) çiftiyle bağlanır — hangi tabloya referans verdiğini `subjectType` belirler, gerçek FK yoktur (cross-module ID-only kuralının bir uzantısı). Buna karşılık `Cast.person`/`Cast.character`, `Person`/`Character` ile aynı modül içinde olduğu için gerçek `@ManyToOne` ilişkisidir ve `CastResponse`'da nested (`PersonSummaryResponse`, `CharacterResponse`) olarak map edilir. Movie/Series servislerine cast eklerken `CastServiceImpl`, `MovieService`/`SeriesService` interface'lerini inject ederek `subjectId`'nin var olduğunu doğrular. Gelecekte crew (yazar/yönetmen) veya awards gibi benzer ihtiyaçlar aynı `subjectType`+`subjectId` desenini tekrar kullanabilir.
- **Görsel yükleme `media/` modülü üzerinden, iki adımlı akış**: Client önce `POST /api/media/images` (multipart) ile görseli yükler, dönen `url`'i alır, sonra ilgili entity'nin create/update isteğinde (`posterUrl` vb. alanlarda) bu URL'i gönderir. Entity'ler `media/`'ye bağımlı değildir — sadece döndürülen düz `String` URL'e bağımlıdır (bağımsızlık ilkesiyle uyumlu). `ImageStorageService` interface'i (`CloudinaryImageStorageService` implementasyonu) DIP'e uygun kurulmuştur; provider değişirse tek yer değişir.
- **Görsel alanı olan tüm entity'lerin (`Franchise`, `Movie`, `Series`, `Season`, `Episode`, `Person`, `Character`) servisleri `ImageStorageService`'i inject eder.** `update()` metodunda her görsel alan için `deleteIfChanged(eskiUrl, yeniUrl)` çağrılır (Cloudinary 5GB free tier'ı korumak için eski görsel otomatik silinir); `delete()` metodunda entity'nin tüm görselleri, DB kaydı silinmeden önce temizlenir.
- **`production/` modülü, cross-module zenginleştirme/orkestrasyon katmanının ilk örneği.** `ProductionService`, `MovieService`+`SeriesService` interface'lerini inject edip iki ayrı kaynağı `releaseDate`/`firstAirDate`'e göre bellek içinde birleştirir. **Bilinçli trade-off**: gerçek bir DB-seviyeli `UNION` değil — her sayfa isteğinde her iki kaynaktan da `(page+1)*size` kadar kayıt çekilip birleştirilir/sıralanır, bu yüzden derin sayfalarda (`page` büyüdükçe) maliyet artar. Küçük/orta ölçekli bir katalog için yeterli; ölçek sorunu çıkarsa native SQL `UNION` sorgusuna veya materialized bir feed tablosuna geçilmeli.

### CMS Modülü (`cms/`)

Site içindeki editoryal/statik içeriği (logo, banner, tanıtım metni gibi görsel+metin bileşenleri) kod değiştirmeden, admin panelinden yönetmek için eklendi. Diğer modüllerle tutarlı feature-package yapısında, ama şu noktalarda kasıtlı farklı tasarım kararları var:

- **Amaç ve sınır**: `PageContent` kayıtları tamamen kendi tablosunda yaşar; `Franchise`/`Series`/`Movie` entity'lerine hiçbir FK veya JPA ilişkisiyle bağlı değildir. Bir entity'nin **kendi doğal verisi** olan görseller (ör. `Series.coverImageUrl`, `Franchise.bannerImageUrl`, `Movie.posterUrl`) CMS'e taşınmaz — onlar zaten ilgili modülün kendi entity'sinde yaşar ve o modülün kendi create/update akışıyla yönetilir. CMS yalnızca, herhangi bir entity'nin "kendi verisi" sayılmayan, admin'in serbestçe ekleyip çıkarabildiği/sıralayabildiği **editoryal bileşenler** içindir (ör. anasayfadaki "bu hafta öne çıkan" banner'ı, bir series detay sayfasına sonradan eklenen ekstra promo görseli). Bir görselin CMS'e mi yoksa entity'nin kendi alanına mı ait olduğu belirsizse: "bu, entity'nin var oluşuyla ilgili temel bir bilgi mi (→ entity'nin alanı) yoksa editoryal/geçici bir sunum kararı mı (→ CMS)" sorusuyla ayrılır.
- **`page` (enum `PageName`) + `entityId` (nullable `Long`) ikilisi**: `HOME`/`FRANCHISE_LIST`/`GLOBAL` gibi sabit, tekil sayfalarda `entityId` boştur. `SERIES_DETAIL` gibi "bir entity'ye özel, çoklu sayfa" durumlarında `entityId` o entity'nin (ör. `Series.id`) id'sini taşır — gerçek bir FK değildir, sadece "hangi sayfa örneği" sorusuna cevap veren düz bir referans numarasıdır. Yeni bir entity tipi için detay sayfası desteği gerektiğinde (`MOVIE_DETAIL`, `FRANCHISE_DETAIL` vb.) `PageName` enum'una tek satır eklemek yeterlidir, şema değişikliği gerekmez. Sorgu deseni: `GET /api/cms/pages/{pageName}?entityId=...` (sabit sayfalarda `entityId` parametresi verilmez, `null` eşleşir).
- **`pageName`/`sectionName` için `String` yerine Java `enum` tercih edildi** (tip güvenliği > esneklik trade-off'u bilinçli yapıldı): yazım hatasıyla sessizce boş sonuç dönmesi riskini ortadan kaldırır, bedeli yeni bir sayfa/bölüm türü eklemenin kod değişikliği+deploy gerektirmesidir.
- **`contentType` kasıtlı olarak sadece `IMAGE`/`TEXT`, `HTML` yok.** Serbest HTML izni stored-XSS riski taşır (admin API'si henüz yetkilendirmesiz olduğu için risk daha da büyük); zengin metin ihtiyacı çıkarsa önce bir sanitizer kütüphanesi (ör. OWASP Java HTML Sanitizer) eklenmeden `HTML` content type'ı açılmamalı.
- **`linkUrl`/`altText` ayrı sütunlar** (`contentValue`'ya gömülü JSON değil) — banner'ların tıklanabilir link ve erişilebilirlik metni ihtiyacını tip güvenli şekilde karşılar.
- **Pagination yok.** Diğer listeleme endpoint'lerinin aksine (`Movie`/`Series`/`Franchise` → `PageResponse<T>`), `GET /api/cms/pages/{pageName}` düz `List<T>` döner: bir sayfanın bileşen sayısı küçük ve sabittir, frontend zaten hepsine aynı anda ihtiyaç duyar (banner'ı görüp footer'ı "sonraki sayfada" çekmek UX'i bozar).
- **Yazma uçları (`POST`/`PUT`/`DELETE /api/cms`) henüz yetkilendirmeden korunmuyor** — projede henüz Spring Security hiç kurulmadı (bkz. "Kimlik Doğrulama" bölümü). `CmsController` üzerinde bunu hatırlatan bir yorum var. Production'a çıkmadan önce mutlaka en az bir yetkilendirme katmanı (API-key veya tam JWT) eklenmeli; aksi halde sitenin görünen yüzü (logo, banner) herkese açık şekilde değiştirilebilir durumda kalır.
- **Cache katmanı henüz yok, planlandı ama uygulanmadı.** Hedef: çoklu instance'a güvenli, dağıtık (Redis-backed) `@Cacheable`/`@CacheEvict` — admin bir içeriği güncellediğinde tüm instance'larda anında görünür olması gerekiyor (TTL'li/CDN tipi "birazdan güncellenir" yaklaşımı bu proje için yeterli değil). Bu adım bilinçli olarak CMS'in temel CRUD'undan ayrı, kullanıcının Redis'e aşina olmadığı için adım adım ele alınacak.
- **Bileşik kart listeleri (ör. bir "adım" bileşeninin görsel+başlık+açıklama üçlüsü) `orderIndex`'i grup anahtarı olarak kullanır.** `PageContent` şeması değişmedi — yeni sütun yok. Bunun yerine: bir kartın her alanı (görsel, başlık, açıklama) AYRI bir `PageContent` kaydıdır, hepsi AYNI `orderIndex`'i paylaşır, hangi alan olduğu `section`'dan anlaşılır (ör. `STEPPER_ITEM_IMAGE`/`STEPPER_ITEM_TITLE`/`STEPPER_ITEM_DESCRIPTION` — üçü `orderIndex=2` ise 3. kartın parçalarıdır). Frontend, `GET /api/cms/pages/{pageName}?entityId=...`'den dönen düz listeyi `orderIndex`'e göre gruplayıp `section`'ı alan adına eşleyerek yapılı nesnelere geri kurar (bkz. Fandoom frontend deposu, `shared/api/cms.js` → `groupBySection`). Bu deseni yeni bir bileşik liste için kullanacaksanız: her alan için ayrı, açıkça adlandırılmış (`<LİSTE>_ITEM_<ALAN>`) bir `SectionName` değeri ekleyin — tek bir section'ı birden fazla alan için "yeniden yorumlamayın", grup içindeki hangi kaydın hangi alana karşılık geldiği yalnızca section adından okunabilmeli.

### SOLID uygulaması

- **Single Responsibility** — bir sınıf tek nedenle değişir: DTO mapping (`mapper/`), validasyon, iş kuralı (`service`) ayrı sınıflarda tutulur.
- **Open/Closed** — yeni davranış, mevcut sınıfı değiştirmek yerine yeni implementasyon/strategy eklenerek karşılanır.
- **Liskov Substitution** — interface implementasyonları, interface'in taahhüt ettiği davranışı bozmaz.
- **Interface Segregation** — her modülün `service`/`repository` interface'i yalnızca o modülün gerçekten kullandığı metodları içerir; şişkin "god interface" oluşturulmaz.
- **Dependency Inversion** — controller'lar ve modüller arası çağrılar her zaman **interface** üzerinden yapılır (constructor injection), concrete implementasyona doğrudan bağımlılık kurulmaz. Her modülün `service` paketinde interface + `*ServiceImpl` ayrımı olur.
- Entity'ler doğrudan controller'dan döndürülmez; her zaman DTO'ya map edilir.

## Kimlik Doğrulama

**JWT tabanlı stateless auth** (Spring Security). Session/cookie tabanlı auth kullanılmayacak. Henüz `spring-boot-starter-security` bağımlılığı eklenmedi — implementasyon aşamasında `spring-boot-engineer` agent'ı ile birlikte kurulacak.

## Kurulu Agent'lar — Ne Zaman Kullanılır

| Agent | Ne zaman |
|---|---|
| `backend-architect` | Yeni servis/endpoint grubu tasarımı, modül sınırlarının belirlenmesi, büyük mimari kararlar |
| `spring-boot-engineer` | Spring Boot'a özgü implementasyon, Spring Security/JWT kurulumu, config detayları |
| `database-architect` | Şema tasarımı, entity ilişkileri, migration planlama, index/performans kararları |
| `security-auditor` | Auth/yetkilendirme değişikliklerinden sonra güvenlik denetimi |
| `api-documenter` | Endpoint'ler stabilleştikçe OpenAPI/Swagger dokümantasyonu |
| `debugger` | Hata, stack trace, beklenmeyen davranış analizi |

## Kurulu Skill

- **`senior-backend`** — API tasarım desenleri, backend güvenlik pratikleri ve DB optimizasyonu için referans dokümanlar (`references/`) içerir. Yardımcı scriptler (`scripts/`) Node/Python ekosistemine göre yazıldığı için doğrudan çalıştırılabilir değil, referans/ilham amaçlı kullanılmalı — bu depoda Java/Maven eşdeğerleri tercih edilir.

## Test Stratejisi

- Servis katmanı için birim testler (JUnit + Mockito).
- Repository/entity için `@DataJpaTest`.
- Controller/API için `@SpringBootTest` + `MockMvc` veya `WebTestClient`.

## Git

Bu depo henüz bir git deposu değil (`git init` yapılmadı). Secrets fix'i (`application.properties`) bu nedenle hiçbir zaman commit edilmedi.
