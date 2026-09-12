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
│   ├── entity/             # Series, Season, Episode, SeriesStatus, SeriesHeroBlock(+SeriesHeroBlockType,
│   │                       # RadiusToken, HeroFontFamily) — bkz. "Series Hero Editörü" bölümü
│   ├── repository/         # SeriesRepository, SeasonRepository, EpisodeRepository
│   ├── dto/                # Series/Season/Episode Request + Summary/Detail response (record),
│   │                       # SeriesHeroBlockRequest/Response
│   ├── mapper/             # SeriesMapper(uses SeasonMapper(uses EpisodeMapper)), SeriesHeroBlockMapper
│   ├── service/            # SeriesService(+getHeroBlocks/replaceHeroBlocks), SeasonService, EpisodeService (+ *ServiceImpl)
│   └── controller/         # SeriesController(+GET/PUT /{id}/hero-blocks), SeasonController, EpisodeController
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
├── user/                   # Hesap/auth — kuruldu (bkz. "Kimlik Doğrulama" bölümü)
│   ├── entity/             # User, Role (USER/EDITOR/MODERATOR/ADMIN enum)
│   ├── repository/         # UserRepository
│   ├── dto/                # Register/Login/AuthResponse, UserSummary/DetailResponse, UpdateRole/PremiumRequest, ResendVerificationRequest
│   ├── mapper/             # UserMapper (MapStruct)
│   ├── security/           # CustomUserDetails, PremiumGuard
│   ├── service/            # AuthService, UserService, CustomUserDetailsService, EmailService (+ *Impl)
│   ├── controller/         # AuthController (/api/auth), UserController (/api/users)
│   ├── exception/          # AuthExceptionHandler, EmailNotVerifiedException
│   └── config/             # AdminBootstrapRunner (ADMIN_BOOTSTRAP_* env'den ilk admin'i oluşturur)
├── security/               # JWT mekaniği — user/'dan kasıtlı ayrı (auth verisi değil, token altyapısı)
│   ├── entity/             # RevokedToken (logout sonrası blacklist)
│   ├── repository/         # RevokedTokenRepository
│   └── (root)              # JwtService, JwtAuthenticationFilter, AuthRateLimitFilter,
│                           # RestAuthenticationEntryPoint/RestAccessDeniedHandler (401/403 JSON),
│                           # RevokedTokenService(Impl), RevokedTokenCleanupScheduler
├── community/              # Kullanıcı üretimi içerik: Discussion/Theory/Fan Art
│   │                       # thread'leri + yorumlar + like/bookmark + kullanıcı
│   │                       # profili (UserProfile, account/'tan taşındı — bkz.
│   │                       # "Community Modülü" bölümü). Vote/Report/ModerationAction
│   │                       # henüz kodda YOK.
│   ├── entity/             # Thread(+ThreadSurface,ThreadStatus), ThreadTag, Comment(+CommentStatus),
│   │                       # ThreadLike, ThreadBookmark, CommentLike, TagFollow, UserProfile
│   ├── repository/         # ThreadRepository(+JpaSpecificationExecutor, native @Modifying sayaç sorguları),
│   │                       # ThreadTagRepository(+countByTagAndThread_Status, findTrending), CommentRepository,
│   │                       # ThreadLikeRepository, ThreadBookmarkRepository, CommentLikeRepository, TagFollowRepository,
│   │                       # UserProfileRepository(+findByUserIdIn toplu avatarUrl çözümü için)
│   ├── dto/                # Thread Request/PatchRequest/Summary/DetailResponse, Thread Like/BookmarkStatusResponse,
│   │                       # Comment Request/Response, CommentLikeStatusResponse, TagFollowStatusResponse,
│   │                       # FollowedTagResponse, TrendingTagResponse, UserProfileResponse, UpdateUserProfileRequest,
│   │                       # ProfileStats (record)
│   ├── mapper/             # ThreadMapper, CommentMapper, UserProfileMapper (MapStruct)
│   ├── specification/      # ThreadSpecificationBuilder (BlogSpecificationBuilder ile aynı desen)
│   ├── service/            # ThreadService, ThreadInteractionService, CommentService,
│   │                       # CommentInteractionService, CommunityFeedService, TagFollowService,
│   │                       # UserProfileService(+getAvatarUrlsByUserIds toplu) (+ *Impl)
│   └── controller/         # ThreadController, CommentController, CommunityFeedController, TagFollowController
│                           # (/api/community/threads/**, /api/community/feed, /api/community/tags/**) —
│                           # UserProfileService için ayrı bir controller YOK, account/'daki
│                           # AccountController/PublicAccountController buna delege eder (bkz. aşağı)
├── media/                  # Görsel yükleme — Cloudinary
│   ├── config/             # CloudinaryConfig (Cloudinary bean, CLOUDINARY_URL'den)
│   ├── dto/                # MediaUploadResponse(url, publicId)
│   ├── service/            # ImageStorageService interface + CloudinaryImageStorageService
│   └── controller/         # MediaController (/api/media/images)
├── content/                 # blog/BlogBlock + cms/HomeBlock'un ortak atası — bkz. "ContentBlock" bölümü
│   └── entity/             # ContentBlock (abstract, @Inheritance JOINED — id/orderIndex/col/row)
├── cms/                    # Sayfa/component içerik yönetimi — bkz. "CMS Modülü" bölümü
│   ├── entity/             # HomeBlock (ContentBlock'u extend eder), PageName, SectionName, ContentType (enum'lar)
│   ├── repository/         # HomeBlockRepository
│   ├── dto/                # HomeBlockRequest, HomeBlockResponse (record)
│   ├── mapper/             # HomeBlockMapper (MapStruct)
│   ├── service/            # HomeBlockService interface + HomeBlockServiceImpl
│   └── controller/         # CmsController (/api/cms)
├── blog/                   # Editoryal makale sayfaları (eski adı "content-drop")
│   ├── entity/             # Blog, BlogBlock(+BlogBlockType), BlogTag, BlogRelation, BlogStatus, SubjectType
│   ├── repository/         # BlogRepository, BlogTagRepository, BlogRelationRepository
│   ├── dto/                # BlogRequest, BlogSummary/DetailResponse, BlogBlockRequest/Response, BlogTagRequest/Response, ReplaceRelatedRequest
│   ├── mapper/             # BlogMapper (MapStruct)
│   ├── service/            # BlogService + BlogServiceImpl
│   └── controller/         # BlogController (/api/blogs)
├── tag/                    # Genel amaçlı etiketleme — Movie/Series/Person/Character için
│   ├── entity/             # Tag, TagAssignment, TaggableType (MOVIE/SERIES/PERSON/CHARACTER)
│   ├── repository/         # TagRepository, TagAssignmentRepository
│   ├── dto/                # TagRequest/Response, TagAssignmentRequest/Response
│   ├── mapper/             # TagMapper, TagAssignmentMapper
│   ├── service/            # TagService, TagAssignmentService (+ *Impl)
│   └── controller/         # TagController, TagAssignmentController (/api/tags)
├── lore/                   # Yapıma özel worldbuilding: dinamik taksonomi (Haneler,
│   │                       # Ejderha Türleri...) + Lokasyon (harita pin'i) + Event
│   │                       # (zaman çizelgesi). Eski `group/` modülünün (GroupType
│   │                       # sabit enum: FACTION/SPECIES) yerini aldı — kategori adı
│   │                       # artık admin-tanımlı bir veri (TaxonomyCategory), kod
│   │                       # değişikliği/deploy gerekmeden yeni yapımda "Klanlar" gibi
│   │                       # bambaşka bir kategori açılabilir. Tek birleşik modül
│   │                       # olarak tasarlandı (person/'ın Person+Character+Cast'ı
│   │                       # bundling emsaliyle tutarlı) — TaxonomyCategory/Location/
│   │                       # Event aynı modül içi gerçek @ManyToOne ile birbirine
│   │                       # bağlanır. Detaylı tasarım kararları: docs/plans/2026-08-10-lore-system-design.md
│   ├── entity/             # TaxonomyCategory(+SubjectType), Group(→TaxonomyCategory FK,+customFields JSON),
│   │                       # GroupAssignment(+TaggableType: CHARACTER|LOCATION), Location(+description,+customFields JSON —
│   │                       # x/y/scale gibi sunuma özgü alanlar bilinçli olarak çekirdek kolon değil, customFields'ta),
│   │                       # Event(+orderIndex[TEK sıralama kaynağı, sortValue YOK],+pinned[gerçek bool],
│   │                       # +customFields JSON[date/quote/tone/locations],→Location FK),
│   │                       # EventParticipant(+ParticipantType: CHARACTER|GROUP)
│   ├── repository/         # TaxonomyCategoryRepository, GroupRepository, GroupAssignmentRepository,
│   │                       # LocationRepository, EventRepository, EventParticipantRepository
│   ├── dto/                # TaxonomyCategory/Group/Location/Event/EventParticipant Request+Response (record)
│   ├── mapper/             # TaxonomyCategoryMapper, GroupMapper, GroupAssignmentMapper, LocationMapper,
│   │                       # EventMapper, EventParticipantMapper
│   ├── service/            # TaxonomyCategoryService, GroupService, GroupAssignmentService, LocationService,
│   │                       # EventService, EventParticipantService (+ *Impl)
│   └── controller/         # TaxonomyCategoryController, GroupController, GroupAssignmentController,
│                           # LocationController, EventController, EventParticipantController — hepsi
│                           # /api/lore/** altında (nested: /api/movies|series/{id}/lore/{categories|groups|locations|events})
├── common/                 # Modüller arası paylaşılan gerçekten jenerik kod
│   ├── entity/             # Auditable (@MappedSuperclass — createdAt/updatedAt)
│   ├── dto/                # PageResponse<T> (record — Page<T> sarmalayıcı)
│   ├── util/               # SlugGenerator (isimden slug üretimi, domain-agnostic)
│   ├── config/             # JpaAuditingConfig, SecurityConfig, CorsConfig, Jackson, vb.
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
- **İSTİSNA — Specification/Criteria tabanlı cross-module filtreleme, entity-seviyeli erişim gerektirir.** `blog/specification/BlogSpecificationBuilder`, blog hub'ın Format/Mood/Tema facet filtreleri için `tag/` modülünün `Tag`/`TagAssignment` entity'lerine **doğrudan** erişir (normalde yasak olan cross-module entity erişimi). Gerekçe: `Specification<T>`/JPA Criteria API, DTO veya service interface üzerinden ifade edilemez — gerçek JPA metamodeline (entity sınıflarına) ihtiyaç duyar, bu yüzden bilinçli kabul edilmiş dar kapsamlı bir istisnadır. Bu istisna yalnızca `BlogSpecificationBuilder` sınıfını kapsar; `blog/` modülünün geri kalanı (ör. `BlogQueryServiceImpl`) `tag/` modülüne yine yalnızca `TagAssignmentService` interface'i üzerinden (DTO döndüren) erişir. Format/Mood/Tema, `tag/` modülünün genişletilmiş hali (`Tag.type: TagType(FORMAT|MOOD|THEME)` + `TaggableType.BLOG`) üzerinden modellenir — ayrı bir Taxonomy modülü açılmadı.

### CMS Modülü (`cms/`)

Site içindeki editoryal/statik içeriği (logo, banner, tanıtım metni gibi görsel+metin bileşenleri) kod değiştirmeden, admin panelinden yönetmek için eklendi. Diğer modüllerle tutarlı feature-package yapısında, ama şu noktalarda kasıtlı farklı tasarım kararları var:

- **Amaç ve sınır**: `HomeBlock` (eski adı `PageContent`) kayıtları tamamen kendi tablosunda yaşar; `Franchise`/`Series`/`Movie` entity'lerine hiçbir FK veya JPA ilişkisiyle bağlı değildir. Bir entity'nin **kendi doğal verisi** olan görseller (ör. `Series.coverImageUrl`, `Franchise.bannerImageUrl`, `Movie.posterUrl`) CMS'e taşınmaz — onlar zaten ilgili modülün kendi entity'sinde yaşar ve o modülün kendi create/update akışıyla yönetilir. CMS yalnızca, herhangi bir entity'nin "kendi verisi" sayılmayan, admin'in serbestçe ekleyip çıkarabildiği/sıralayabildiği **editoryal bileşenler** içindir (ör. anasayfadaki "bu hafta öne çıkan" banner'ı, bir series detay sayfasına sonradan eklenen ekstra promo görseli). Bir görselin CMS'e mi yoksa entity'nin kendi alanına mı ait olduğu belirsizse: "bu, entity'nin var oluşuyla ilgili temel bir bilgi mi (→ entity'nin alanı) yoksa editoryal/geçici bir sunum kararı mı (→ CMS)" sorusuyla ayrılır.
- **`page` (enum `PageName`) + `entityId` (nullable `Long`) ikilisi**: `HOME`/`FRANCHISE_LIST`/`GLOBAL` gibi sabit, tekil sayfalarda `entityId` boştur. `SERIES_DETAIL` gibi "bir entity'ye özel, çoklu sayfa" durumlarında `entityId` o entity'nin (ör. `Series.id`) id'sini taşır — gerçek bir FK değildir, sadece "hangi sayfa örneği" sorusuna cevap veren düz bir referans numarasıdır. Yeni bir entity tipi için detay sayfası desteği gerektiğinde (`MOVIE_DETAIL`, `FRANCHISE_DETAIL` vb.) `PageName` enum'una tek satır eklemek yeterlidir, şema değişikliği gerekmez. Sorgu deseni: `GET /api/cms/pages/{pageName}?entityId=...` (sabit sayfalarda `entityId` parametresi verilmez, `null` eşleşir).
- **`pageName`/`sectionName` için `String` yerine Java `enum` tercih edildi** (tip güvenliği > esneklik trade-off'u bilinçli yapıldı): yazım hatasıyla sessizce boş sonuç dönmesi riskini ortadan kaldırır, bedeli yeni bir sayfa/bölüm türü eklemenin kod değişikliği+deploy gerektirmesidir.
- **`contentType` kasıtlı olarak sadece `IMAGE`/`TEXT`, `HTML` yok.** Serbest HTML izni stored-XSS riski taşır (admin API'si henüz yetkilendirmesiz olduğu için risk daha da büyük); zengin metin ihtiyacı çıkarsa önce bir sanitizer kütüphanesi (ör. OWASP Java HTML Sanitizer) eklenmeden `HTML` content type'ı açılmamalı.
- **`linkUrl`/`altText` ayrı sütunlar** (`contentValue`'ya gömülü JSON değil) — banner'ların tıklanabilir link ve erişilebilirlik metni ihtiyacını tip güvenli şekilde karşılar.
- **Pagination yok.** Diğer listeleme endpoint'lerinin aksine (`Movie`/`Series`/`Franchise` → `PageResponse<T>`), `GET /api/cms/pages/{pageName}` düz `List<T>` döner: bir sayfanın bileşen sayısı küçük ve sabittir, frontend zaten hepsine aynı anda ihtiyaç duyar (banner'ı görüp footer'ı "sonraki sayfada" çekmek UX'i bozar).
- **Yazma uçları (`POST`/`PUT`/`DELETE /api/cms`) artık `ADMIN` rolüyle korunuyor** — Spring Security/JWT kurulduktan sonra `SecurityConfig`'e eklendi (bkz. "Kimlik Doğrulama" bölümü). Sadece `GET /api/cms/**` public.
- **Cache katmanı henüz yok, planlandı ama uygulanmadı.** Hedef: çoklu instance'a güvenli, dağıtık (Redis-backed) `@Cacheable`/`@CacheEvict` — admin bir içeriği güncellediğinde tüm instance'larda anında görünür olması gerekiyor (TTL'li/CDN tipi "birazdan güncellenir" yaklaşımı bu proje için yeterli değil). Bu adım bilinçli olarak CMS'in temel CRUD'undan ayrı, kullanıcının Redis'e aşina olmadığı için adım adım ele alınacak.
- **Bileşik kart listeleri (ör. bir "adım" bileşeninin görsel+başlık+açıklama üçlüsü) `orderIndex`'i grup anahtarı olarak kullanır.** `HomeBlock` şeması bu amaçla değişmedi. Bunun yerine: bir kartın her alanı (görsel, başlık, açıklama) AYRI bir `HomeBlock` kaydıdır, hepsi AYNI `orderIndex`'i paylaşır, hangi alan olduğu `section`'dan anlaşılır (ör. `STEPPER_ITEM_IMAGE`/`STEPPER_ITEM_TITLE`/`STEPPER_ITEM_DESCRIPTION` — üçü `orderIndex=2` ise 3. kartın parçalarıdır). Frontend, `GET /api/cms/pages/{pageName}?entityId=...`'den dönen düz listeyi `orderIndex`'e göre gruplayıp `section`'ı alan adına eşleyerek yapılı nesnelere geri kurar (bkz. Fandoom frontend deposu, `shared/api/cms.js` → `groupBySection`). Bu deseni yeni bir bileşik liste için kullanacaksanız: her alan için ayrı, açıkça adlandırılmış (`<LİSTE>_ITEM_<ALAN>`) bir `SectionName` değeri ekleyin — tek bir section'ı birden fazla alan için "yeniden yorumlamayın", grup içindeki hangi kaydın hangi alana karşılık geldiği yalnızca section adından okunabilmeli.

### ContentBlock (`content/`)

`blog/BlogBlock` ve `cms/HomeBlock`'un ortak atası — `@Inheritance(strategy = InheritanceType.JOINED)` ile kurulu. Amaç: editöryel içerik birimlerini (sıra + CSS grid pozisyonu) tek yerden tutarlı kontrol edebilmek, gelecekte eklenecek tiplerin (ör. bir "Story" ailesi) kendi özel alanlarını mevcut tiplere nullable kolon olarak sızdırmadan taşıyabilmesi.

- **Base'te (`ContentBlock`) sadece gerçekten evrensel alanlar var**: `id`, `orderIndex`, `col`, `row` (CSS grid shorthand, ör. `"1 / 6"` — backend için opak, sadece saklanır/döner; DB sütunu `grid_row`, çünkü `ROW` MySQL 8.0.19+'da rezerve kelime). Tipe özel alanlar (`blockType`/`text`/`imageUrl`, `page`/`section`/`contentType`...) alt sınıflarda kalır — spekülatif olarak buraya taşınmaz.
- **Bilinçli olarak builder yok.** `ContentBlock` abstract, doğrudan inşa edilmiyor. `Auditable`'a `@SuperBuilder` eklemek denendi ama `Auditable`'ı extend eden TÜM diğer entity'lerin (`Franchise`, `Movie`, `Series`, `User`, `Tag`...) mevcut plain `@Builder`'ıyla static `builder()` dönüş tipi çakışması yaratıp tüm projenin derlemesini kırdı — bu yüzden geri alındı. Alt sınıflar (`BlogBlock`, `HomeBlock`, `SeriesHeroBlock`) kendi alanları için plain `@Builder`+`@AllArgsConstructor` kullanır; miras alınan `orderIndex`/`col`/`row`, `build()` sonrası setter ile atanır (bkz. `BlogServiceImpl.applyBlocks`, `HomeBlockServiceImpl`, `SeriesServiceImpl.replaceHeroBlocks`).
- **Aggregate ilişkiler korunur**: `BlogBlock`'un `Blog`'a, `SeriesHeroBlock`'un `Series`'e gerçek `@ManyToOne`/cascade+orphanRemoval ilişkisi (sırasıyla `fk_blog_block_blog`, `fk_series_hero_block_series`) JOINED inheritance'tan etkilenmedi, `@PrimaryKeyJoinColumn(name="id")` ile `content_block`'a bağlanıyor.
- **`col`/`row`'u KULLANMAYAN alt sınıflar da olabilir**: `col`/`row` yalnızca `HomeBlock`'ta kullanılır; diğer alt sınıflarda hep null kalır. `id`/`orderIndex` yine de ortak kalır. **`SeriesHeroBlock`** CSS grid yerine serbest kanvas konumlama (`x`/`y`/`width`/`height`, kendi alanları olarak) kullanır. **`BlogBlock`** (2026-08'de, kullanıcı kararıyla) bu serbest kanvas modelinden `series/SeasonBlock` ile BİREBİR aynı desene geçti: `sceneKey`'e göre gruplanan sıralı sahne blokları (`content`/`contentTr`) — ne col/row ne de x/y/width/height kullanır. KASITLI FARK: görsel blockType'ı `SeasonBlockType`/`EpisodeBlockType`'ın `MEDIA`'sı değil `IMAGE` kalır, alanlar da `mediaUrl`/`mediaAlt(Tr)` değil `imageUrl`/`imageAlt(Tr)` — blog'un görseli her zaman düz bir fotoğraf, `mediaCredit` YOK (blog'da fotoğraf kredisi gösterilmiyor). Migration: depo kökündeki `blogblock_seasonblock_migration.sql`.
- **Var olan veriyi taşıma**: `ddl-auto=update` mevcut `blog_block`/`page_content` verisini otomatik yeni şemaya taşımaz (tablo yeniden adlandırma/veri kopyalama yapmaz). Tek seferlik elle migration için depo kökündeki `contentblock_migration.sql`'e bakın.

### Series Hero Editörü (`series/`)

Series detay sayfasının en üstündeki Hero bileşeni (arka plan görseli+blur, logo, başlık, meta, sinopsis, fragman butonu, serbest metin kutuları) — admin panelinde serbestçe konumlanıp eklenip/çıkarılabilen blok listesi. Tasarım kararlarının tam gerekçesi: `docs/plans/2026-08-13-series-hero-editor-design.md`.

- **`SeriesHeroBlock`**, `blog/BlogBlock` ile birebir aynı desende: `ContentBlock`'u extend eder (`col`/`row` kullanılmaz, kendi `x`/`y`/`width`/`height`'i var), `Series`'e gerçek `@ManyToOne` (cascade ALL+orphanRemoval, `Series.heroBlocks`). Ayrı bir "SeriesHero" sarmalayıcı entity YOK — `Series`'in kendisi zaten sarmalayıcı (Blog'un kendisi `blocks`'un sahibi olduğu gibi). "Ekle/çıkar" = bu listede bulk-replace (`PUT /api/series/{id}/hero-blocks`, `BlogServiceImpl.applyBlocks` ile aynı mantık: `clearHeroBlocks()` + yeniden ekle + `orderIndex` ata).
- **`SeriesHeroBlockType`**: `IMAGE, LOGO, TITLE, META, SYNOPSIS, BUTTON, BOX`. **TITLE/META/SYNOPSIS/BUTTON hiçbir içerik alanı taşımaz** — render anında `Series`'in kendi alanlarından (`titleTr/En`, `synopsisTr/En`, `trailerUrl`) otomatik beslenir; Series'te ilgili alan boşsa (ör. `trailerUrl` null) FE o bloğu/rozeti otomatik gizler, blok DB'de konumlanmış halde dursa bile — admin ayrıca bir göster/gizle toggle'ı yönetmez. `IMAGE`/`LOGO` `imageUrl` taşır (`IMAGE` ayrıca `blurAmount`, px, arka plan blur şiddeti). `BOX` serbestçe `textTr`/`textEn`+`backgroundColor` (hex) taşır — admin'in HomeBlock/BlogBlock dışında serbestçe yazabildiği tek blok tipi.
- **`buttonStyle` alanı YOK** — Hero butonları her zaman glassmorfik, FE'nin sabit CSS'i (`backdrop-filter: blur(20px) saturate(200%) brightness(1.15)`) kullanılır; bu 20px, `IMAGE` bloğunun `blurAmount`'ından tamamen bağımsızdır, backend hiçbirini birbirine karıştırmaz.
- **`RadiusToken`** (`SM, MD, LG, PILL`) FE'nin `--radius-*` token adlarıyla birebir eşleşir — keyfi px değil. **`HeroFontFamily`**, Blog'un `BlockFontFamily`'sinden bilinçli bağımsız yeni bir enum (series/'in blog/'a bağımlı olmaması için); başlangıç değeri `COOPER_BT`.
- **IMDb rating canlı çekme (OMDb entegrasyonu) bilinçli olarak kapsam dışı bırakıldı.** Hero, mevcut admin-elle-girilen `Series.externalRating`/`imdbId`'yi olduğu gibi okur. İleride eklenirse ayrı bir cache katmanı/Redis gerekmez — `Series.externalRatingUpdatedAt` kolonu zaten "ne zaman tazelendi" bilgisini taşıyor, cache'in kendisi olarak kullanılabilir.

### Community Modülü (`community/`)

Kullanıcı üretimi içerik (UGC) — Discussion/Theory/Fan Art tarzı thread'ler + yorumlar. Diğer içerik modüllerinden (editoryal, yazarı `EDITOR/MODERATOR/ADMIN`) kasıtlı olarak farklı: burada **her girişli kullanıcı** yazabilir, yetki sadece sahiplik/moderasyon seviyesinde kontrol edilir. Faz 1 kapsamı bu; kişiselleştirme ve moderasyon araçları sonraki fazlara bırakıldı.

- **`Thread.surface`** (enum `DISCUSSION/THEORY/FAN_ART`) tek entity/tabloyu üç sekmeye böler — üç ayrı tablo yerine tek entity + filtre tercih edildi, şema tekrarını önlemek için.
- **Cross-module referanslar ID-only**: `authorId` (`user/`'a), `productionSlug` (`movie/`/`series/`'e) gerçek FK değil düz alan; `ThreadServiceImpl.create/update` bunu `MovieService`/`SeriesService.existsBySlug` ile doğrular, geçersizse `InvalidReferenceException`.
- **Sayaçlar (`likeCount`/`commentCount`/`bookmarkCount`) denormalize**, `ThreadRepository`'deki native `@Modifying UPDATE ... SET x = x+1` sorgularıyla artırılıp azaltılır — entity'nin in-memory alanı bilerek set edilmez (dirty-checking'in bulk update'i stale değerle ezmesini önlemek için); dönüş DTO'sundaki sayı `mevcutSayı±1` ile hesaplanır.
- **Hard-delete kuralına dar kapsamlı istisna**: proje genelinde "delete = hard delete" ama `Thread`/`Comment` soft-delete (`status=DELETED`). Gerekçe: `Comment.parent` zinciri kırılmasın (bir yoruma verilen yanıtlar anlamsız kalmasın) ve moderasyon izni DB'de tutulabilsin. Silinen yorumun `body`'si `CommentMapper`'da `"[silindi]"` olarak maskelenir, orijinal veri DB'de korunur. `ThreadStatus.HIDDEN` şimdilik yer tutucu — onu set eden bir endpoint yok.
- **Yorumlar 2 seviyeyle sabit**: kendi `parent`'ı dolu olan bir yoruma tekrar yanıt verilemez — kısıt DB'de değil `CommentServiceImpl.create`'de uygulanır. Üst seviye yorumlar listelenirken en fazla 3 yanıt önizlemesi (`REPLIES_PREVIEW_SIZE`) döner, tamamı için ayrı bir "yanıtları göster" ucu yok.
- **Like/Bookmark, `account/`'daki genel `SavedItem`/like mekanizmasından bilinçli olarak bağımsız**: `ThreadLike`/`ThreadBookmark`/`CommentLike` kendi tablolarında (`user_id`+`thread_id` unique constraint), toggle idempotent — zaten like'lı bir kaynağa tekrar `POST` atmak hata vermez, mevcut durumu aynen döner.
- **`ThreadTag`, `tag/` modülünün Tag'ından bağımsız yeni bir entity**: `tag/` editor-curated ve yazması rol kısıtlı, community'de ise kullanıcı kendi thread'ine serbestçe tag yazabilmeli — iki farklı yetki modeli aynı tabloda buluşturulmadı. Tag'ler `SlugGenerator.slugify` ile normalize edilir, thread başına en fazla 10 tag (`MAX_TAGS_PER_THREAD`).
- **`ThreadSpecificationBuilder`, `blog/BlogSpecificationBuilder` ile aynı desende**: her facet metodu (surface/productionSlug/tag→id listesi) seçilmemişse `null` döner, `ThreadServiceImpl.list` bunları `Specification.allOf` ile birleştirmeden önce eler.
- **`?tags=` çoklu değer VEYA (OR) mantığıyla çalışır**: `ThreadController.list`/`ThreadService.list` bir `List<String> tags` alır (`ThreadTagRepository.findByTagIn`), herhangi birine sahip thread'ler döner (hepsine birden sahip olma şartı yok). Tek tag göndermek (`?tags=theory`) eski `?tag=` davranışıyla aynı sonucu verir.
- **`CommunityFeedService` şu an `ThreadService.list`'e ince bir delege** — Faz 4'te kişiselleştirme (`UserInterestProfile`+`FeedRankingService`) buraya eklenecek diye bilerek ayrı bir servis olarak tutuluyor, `/threads` listeleme akışına dokunmadan genişleyebilsin diye.
- **Yazma yetkilendirmesi diğer içerik modüllerinden farklı**: `SecurityConfig`'te community yazma uçları (`POST/PATCH/DELETE /api/community/**`) sadece `authenticated()` — `EDITOR/MODERATOR/ADMIN` rol şartı YOK, çünkü bu editoryal değil kullanıcı üretimi içerik. Sahip/moderatör ayrımı path seviyesinde ifade edilemediği için gerçek kontrol (`assertOwnerOrModerator`) servis katmanında yapılır (moderatör her zaman geçer, aksi halde `authorId == userId` şartı aranır).
- **`hotScore`**, `community/HotScoreJob` tarafından 15 dakikada bir (`@Scheduled(fixedRate=900000)`, `RevokedTokenCleanupScheduler` ile aynı desen) tüm `PUBLISHED` thread'ler için yeniden hesaplanır: `(likeCount + 2*commentCount) / (ageHours+2)^1.5` — Reddit tarzı zaman-azalışlı basit bir formül, sayfalama yok (Faz 1 ölçeği için tüm thread'ler belleğe alınır). `qualityScore` hâlâ hiçbir yerde hesaplanmıyor.
- **Yazar zenginleştirmesi (username + avatarUrl)**: `ThreadSummaryResponse`/`ThreadDetailResponse`/`CommentResponse`, ham `authorId`'yi KORUYARAK yanına `AuthorSummary(id, username, avatarUrl)` ekler. `username` `UserService.getUsernamesByIds(Set<Long>)` ile, `avatarUrl` `UserProfileService.getAvatarUrlsByUserIds(Set<Long>)` ile toplu çözülür (`ThreadServiceImpl`/`CommentServiceImpl` sayfa başına ikisi için de tek sorgu, N+1 yok). Silinmiş kullanıcıda/hiç profil oluşturmamış kullanıcıda `username`/`avatarUrl` null gelir, kayıt silinmez.
- **`UserProfile` ailesi `account/`'tan buraya taşındı (döngüsel bağımlılık önleme)**: `account/UserProfileServiceImpl` zaten Faz 2'de `commentCount`/`theoryCount` için `community/ThreadService`+`CommentService`'i inject ediyordu (`account/ → community/`). avatarUrl zenginleştirmesi eklenince ters yönde (`community/ → account/UserProfileService`) bir bağımlılık daha gerekiyordu — ikisi birden döngüsel bağımlılık (Spring başlangıçta patlar) yaratacağından `UserProfile`/`UserProfileService`/`UserProfileMapper`/`UserProfileRepository`/`UserProfileResponse`/`UpdateUserProfileRequest`/`ProfileStats` community/'ye taşındı. `account/AccountController`/`PublicAccountController` artık `community.service.UserProfileService`'e (tek yönlü `account/ → community/`) delege eder; DB tablo adı (`user_profile`) değişmedi. `UserProfileServiceImpl`'in `ThreadService`/`CommentService` bağımlılığı artık aynı modül içi sibling servis çağrısı; `account/`'a kalan `ActivityLogService.countByUserIdAndType`/`UserLikeService.countByUserId` (interface üzerinden, cross-module) ile `ProfileStats` hesaplanmaya devam eder.
- **`isLiked`/`isBookmarked` (Thread'de ikisi de, Comment'te sadece `isLiked`)**: `ThreadLikeRepository`/`ThreadBookmarkRepository`/`CommentLikeRepository`'deki toplu `findXIdsByUserIdAndXIdIn` sorgularıyla, listeleme sayfası başına tek sorgu şeklinde çözülür. Anonim istekte (`viewerId=null`, GET uçlarında `@AuthenticationPrincipal` null gelebilir) hiç sorgu atılmaz, hepsi `false`.
- **`ThreadSummaryResponse.excerpt`**: `ThreadMapper.buildExcerpt` ile `body`'den türetilir (160 karakter, kelime ortasından kesmez, son boşluğa geri sarar + `"..."`). `ThreadDetailResponse` zaten tam `body`'yi döndürdüğü için ayrıca excerpt taşımaz.
- **Yorum silme idempotent sayaç azaltma**: `CommentServiceImpl.delete`, yalnızca yorum henüz `DELETED` değilse durumu değiştirir VE `ThreadRepository.decrementCommentCount` çağırır — zaten silinmiş bir yoruma tekrar `DELETE` atmak `commentCount`'u fazladan düşürmez.
- **`community/CommunityRateLimitFilter`**: `security/AuthRateLimitFilter` ile aynı in-memory `ConcurrentHashMap` deseni ama IP değil userId bazlı — `POST /api/community/threads` (10/saat), `POST /api/community/threads/*/comments` (30/saat). `JwtAuthenticationFilter`'dan SONRA zincire eklenir (`SecurityConfig`) ki `SecurityContextHolder`'da authentication çözülmüş olsun; anonim istekte devre dışı kalır (o istek zaten yetkilendirmede 401'e düşer).
- **Faz 1 kapsam dışı (hâlâ)**: Vote, Report, ModerationAction hiç kodda yok; `qualityScore` hesaplanmıyor.
- **Faz 3 — Tag sistemi**: `TagFollow`, `ThreadLike`/`ThreadBookmark` ile birebir aynı desende (surrogate id + `(user_id, tag)` unique constraint), `account/`'ın genel `SavedItem`/`UserFollow` mekanizmasından bilinçli olarak bağımsız (`ThreadTag`'ın `tag/` modülünden bağımsız olma gerekçesiyle aynı). `TagFollowServiceImpl.follow/unfollow` idempotent, tag `SlugGenerator.slugify` ile normalize edilir. **Trending tags job/cache YOK** — `ThreadTagRepository.findTrending` her istekte on-the-fly JPQL `GROUP BY` (bilinçli basit çözüm, `production/` modülündeki trade-off'a benzer; ölçek sorunu çıkarsa saatlik bir `TrendingTagsJob`+cache'e geçilebilir). `GET /api/community/tags/{tag}/threads`, yeni bir servis metodu YAZMADAN `ThreadService.list(null, null, tag, sort, viewerId, pageable)`'a ince bir delege — mevcut `?tag=` filtresiyle aynı mantık, sadece daha temiz URL.
- **Pre-existing bağımsız düzeltme**: `MovieService`/`SeriesService` interface'lerinde `existsBySlug(String)` yoktu (repository'de vardı ama servise hiç açılmamıştı) — `ThreadServiceImpl.validateProductionSlug` bunu çağırdığı için community modülü hiç derlenmiyordu; bu turda `existsById` ile simetrik şekilde eklendi (`MovieServiceImpl`/`SeriesServiceImpl`'de doğrudan repository'ye delege).

### SOLID uygulaması

- **Single Responsibility** — bir sınıf tek nedenle değişir: DTO mapping (`mapper/`), validasyon, iş kuralı (`service`) ayrı sınıflarda tutulur.
- **Open/Closed** — yeni davranış, mevcut sınıfı değiştirmek yerine yeni implementasyon/strategy eklenerek karşılanır.
- **Liskov Substitution** — interface implementasyonları, interface'in taahhüt ettiği davranışı bozmaz.
- **Interface Segregation** — her modülün `service`/`repository` interface'i yalnızca o modülün gerçekten kullandığı metodları içerir; şişkin "god interface" oluşturulmaz.
- **Dependency Inversion** — controller'lar ve modüller arası çağrılar her zaman **interface** üzerinden yapılır (constructor injection), concrete implementasyona doğrudan bağımlılık kurulmaz. Her modülün `service` paketinde interface + `*ServiceImpl` ayrımı olur.
- Entity'ler doğrudan controller'dan döndürülmez; her zaman DTO'ya map edilir.

## Kimlik Doğrulama

**JWT tabanlı stateless auth kuruldu** (Spring Security + `io.jsonwebtoken` / jjwt `0.12.6`). Session/cookie tabanlı auth kullanılmıyor.

- **`User`/`Role`** (`user/entity/`): `User` — username/email (unique), bcrypt `password`, `role` (`Role` enum: `USER, EDITOR, MODERATOR, ADMIN`, `getAuthority()` → `"ROLE_"+name()`), `premiumExpiresAt`, `emailVerified` + doğrulama token'ı alanları. Tablo adı `app_user`.
- **Auth uçları** — `user/controller/AuthController` (`/api/auth`): `POST /register`, `POST /login`, `POST /logout`, `GET /me`, `GET /verify-email`, `POST /resend-verification`. E-posta doğrulama akışı `EmailService`/`EmailNotVerifiedException` ile tam kurulu.
- **JWT üretimi/doğrulaması** — `security/JwtService`: HMAC-SHA imza, key `${jwt.secret}` (env `JWT_SECRET`, fallback yok — eksikse açılış patlar, `DB_PASSWORD` ile aynı konvansiyon), süre `${jwt.expiration-ms:3600000}` (varsayılan 1 saat). Token'da `jti` (UUID) taşınır.
- **Token blacklist (logout)** — `security/entity/RevokedToken` + `RevokedTokenRepository`: logout'ta token'ın `jti`'si kaydedilir; `JwtAuthenticationFilter` her istekte `existsByJti()` ile kontrol eder. `RevokedTokenCleanupScheduler` süresi dolmuş kayıtları temizler.
- **Rate limiting** — `security/AuthRateLimitFilter`, `JwtAuthenticationFilter`'dan önce zincire eklenir (brute-force login denemelerine karşı).
- **Yetkilendirme, `common/config/SecurityConfig`'te path bazlı `authorizeHttpRequests` ile merkezi yapılır** (controller'larda `@PreAuthorize` kullanılmıyor, `@EnableMethodSecurity` açık ama şu an path kuralları yeterli görülmüş):
  - `permitAll`: `OPTIONS /**`, `/error`, `POST /api/auth/{register,login,resend-verification}`, `GET /api/auth/verify-email`, ve **GET** metoduyla `franchises/genres/movies/series/seasons/episodes/people/characters/cast/productions/cms/blogs` (`/**`).
  - `hasRole("ADMIN")`: `/api/cms/**` (GET hariç tüm metodlar — GET zaten yukarıda public), `/api/users/**` (tüm metodlar).
  - `hasAnyRole("EDITOR","MODERATOR","ADMIN")`: içerik yazma uçları (franchise/genre/movie/series/season/episode/people/character/cast/media, blogs — GET hariç).
  - Listede **olmayan** her şey (ör. `/api/tags/**` — henüz path kuralına eklenmemiş) `anyRequest().authenticated()`'a düşer, yani sadece login yeterli, rol şartı yok. Yeni bir modül yazma ucu eklerken bunu unutmayın — path'i açıkça `SecurityConfig`'e eklemezseniz varsayılan sadece "giriş yapılmış olsun" olur, rol kısıtı olmaz.
  - 401/403 yanıtları JSON: `security/RestAuthenticationEntryPoint` / `RestAccessDeniedHandler`.
- **İlk admin** — `user/config/AdminBootstrapRunner`: `ADMIN_BOOTSTRAP_USERNAME/PASSWORD/EMAIL` env'leri setliyse ve DB'de hiç `ADMIN` yoksa açılışta otomatik oluşturur.
- **Test** — ayrı bir `AuthController`/`SecurityConfig` birim testi yok; rol bazlı yetkilendirme `blog` modülünün `BlogControllerTest`'inde (`@AutoConfigureMockMvc`, gerçek `SecurityConfig` + `@WithMockUser(roles=...)`) entegrasyon testiyle doğrulanıyor. Yeni bir modülün yazma uçlarını test ederken bu deseni örnek alın.

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

Depo git ile takip ediliyor (`main` branch). Secrets (`application.properties` içindeki düz metin şifreler) hiçbir zaman commit edilmedi — hassas değerler ortam değişkeninden okunur (bkz. "Konfigürasyon & Secrets").
