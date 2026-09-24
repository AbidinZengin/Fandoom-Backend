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
│   ├── entity/             # Movie(+MovieStatus) — franchiseId/genreIds/producerIds/directorIds/writerIds sadece ID
│   │                       # (cross-module), bkz. "Movie Yaşam Döngüsü" bölümü
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
├── person/                 # Person (gerçek insan) + Character (kurgusal, yapıma özel: subjectType+subjectId) + Cast (Person↔Character köprüsü)
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
│                           # + Portal ailesi (bkz. "Portal" bölümü): entity Portal/PortalProduction/
│                           # PortalMembership(+PortalStatus, PortalPostingPolicy, PortalProductionType),
│                           # PortalService/PortalQueryService/PortalMembershipService/PortalAdminService (+ *Impl),
│                           # PortalController/PortalMembershipController (/api/community/portals/**, /api/me/portals),
│                           # PortalAdminController (/api/admin/community/portals)
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
│   ├── entity/             # Tag(+TagType MOOD/CONTENT_WARNING), TagAssignment, TaggableType (MOVIE/SERIES/PERSON/CHARACTER/BLOG)
│   ├── repository/         # TagRepository, TagAssignmentRepository
│   ├── dto/                # TagRequest/Response, TagAssignmentRequest/Response
│   ├── mapper/             # TagMapper, TagAssignmentMapper
│   ├── service/            # TagService, TagAssignmentService (+ *Impl)
│   └── controller/         # TagController, TagAssignmentController (/api/tags)
├── trivia/                 # Movie/Series "Biliyor muydunuz?" hap bilgileri — bkz. "Trivia Modülü" bölümü
│   ├── entity/             # Trivia, TriviaItemType(MOVIE/SERIES, JSON'da küçük harf), TriviaTag
│   ├── repository/ dto/ mapper/ service/ controller/   # TriviaController (/api/trivia)
│   └── config/             # TriviaItemTypeConverter (?itemType=movie küçük harf query)
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
- **Polimorfik cross-module credit (Cast) deseni**: Movie/Series'e `subjectType`(enum) + `subjectId`(Long) ile bağlanan şey `Cast` DEĞİL `Character`'dir — karakter yapıma özeldir (global değil), hangi tabloya referans verdiğini `subjectType` belirler, gerçek FK yoktur (cross-module ID-only kuralının bir uzantısı). `Cast` yalnızca `Person`↔`Character` köprüsüdür (`billingOrder` ile); `Cast.person`/`Cast.character` aynı modül içinde olduğu için gerçek `@ManyToOne`'dır ve `CastResponse`'da nested (`PersonSummaryResponse`, `CharacterResponse`) map edilir. Cast listesi `Character.subjectType/subjectId` üzerinden çekilir (`findByCharacter_SubjectTypeAndCharacter_SubjectId...`). `CastServiceImpl.addToMovie/addToSeries`, `MovieService`/`SeriesService` interface'leriyle yapımın varlığını doğrular ve karakterin o yapıma ait olduğunu (`character.subject == hedef`) kontrol eder. Bu yüzden Movie'de `castIds` YOKTUR — oyuncu kadrosu zaten Cast'ten gelir. Yönetmen/senarist gibi yalın künye ilişkileri ise Cast/Character kullanmaz, `producerIds` gibi düz `Set<Long>` (bkz. "Movie Yaşam Döngüsü").
- **Görsel yükleme `media/` modülü üzerinden, iki adımlı akış**: Client önce `POST /api/media/images` (multipart) ile görseli yükler, dönen `url`'i alır, sonra ilgili entity'nin create/update isteğinde (`posterUrl` vb. alanlarda) bu URL'i gönderir. Entity'ler `media/`'ye bağımlı değildir — sadece döndürülen düz `String` URL'e bağımlıdır (bağımsızlık ilkesiyle uyumlu). `ImageStorageService` interface'i (`CloudinaryImageStorageService` implementasyonu) DIP'e uygun kurulmuştur; provider değişirse tek yer değişir.
- **Görsel alanı olan tüm entity'lerin (`Franchise`, `Movie`, `Series`, `Season`, `Episode`, `Person`, `Character`) servisleri `ImageStorageService`'i inject eder.** `update()` metodunda her görsel alan için `deleteIfChanged(eskiUrl, yeniUrl)` çağrılır (Cloudinary 5GB free tier'ı korumak için eski görsel otomatik silinir); `delete()` metodunda entity'nin tüm görselleri, DB kaydı silinmeden önce temizlenir.
- **`production/` modülü, cross-module zenginleştirme/orkestrasyon katmanının ilk örneği.** `ProductionService`, `MovieService`+`SeriesService` interface'lerini inject edip iki ayrı kaynağı `releaseDate`/`firstAirDate`'e göre bellek içinde birleştirir. **Bilinçli trade-off**: gerçek bir DB-seviyeli `UNION` değil — her sayfa isteğinde her iki kaynaktan da `(page+1)*size` kadar kayıt çekilip birleştirilir/sıralanır, bu yüzden derin sayfalarda (`page` büyüdükçe) maliyet artar. Küçük/orta ölçekli bir katalog için yeterli; ölçek sorunu çıkarsa native SQL `UNION` sorgusuna veya materialized bir feed tablosuna geçilmeli.
- **İSTİSNA — Specification/Criteria tabanlı cross-module filtreleme, entity-seviyeli erişim gerektirir.** `blog/specification/BlogSpecificationBuilder`, blog hub'ın Format/Mood/Tema facet filtreleri için `tag/` modülünün `Tag`/`TagAssignment` entity'lerine **doğrudan** erişir (normalde yasak olan cross-module entity erişimi). Gerekçe: `Specification<T>`/JPA Criteria API, DTO veya service interface üzerinden ifade edilemez — gerçek JPA metamodeline (entity sınıflarına) ihtiyaç duyar, bu yüzden bilinçli kabul edilmiş dar kapsamlı bir istisnadır. Bu istisna yalnızca `BlogSpecificationBuilder` sınıfını kapsar; `blog/` modülünün geri kalanı (ör. `BlogQueryServiceImpl`) `tag/` modülüne yine yalnızca `TagAssignmentService` interface'i üzerinden (DTO döndüren) erişir. Mood facet'i, `tag/` modülünün genişletilmiş hali (`Tag.type: TagType(MOOD|CONTENT_WARNING)` + `TaggableType.BLOG`) üzerinden modellenir — ayrı bir Taxonomy modülü açılmadı. (Format `Blog.format` enum'una taşındı, THEME kaldırıldı: sıradan `type=null` tag'ler üstlendi.)

### Movie Yaşam Döngüsü & Künye (`movie/`)

Duyurulmuş/çekimdeki filmlerin de tartışılabilmesi için `Movie`'ye eklendi. Migration: depo kökündeki `movie_lifecycle_migration.sql` (idempotent, yeni build'in İLK açılışından ÖNCE çalıştırılmalı).

- **`status`** (`MovieStatus`: `ANNOUNCED, FILMING, POST_PRODUCTION, RELEASED, CANCELLED`): DB'de NULLABLE. Client göndermezse `MovieServiceImpl.resolveStatus`: update'te mevcut değer korunur, yoksa `releaseDate` geçmiş/bugün → `RELEASED`, gelecek/boş → `ANNOUNCED` (migration'daki backfill aynı kuralı uygular). Kolon migration'da `VARCHAR(20)` açılır — Hibernate'e bırakılırsa MySQL native `enum(...)` üretebilir ve yeni değer eklemek `ddl-auto=update` ile ALTER edilmez. "Çıkışına kaç gün kaldı" sayacı FE'de `releaseDate`'ten hesaplanır, backend'de karşılığı yok. `MovieSummaryResponse` de `status` taşır.
- **`budget` / `boxOffice`**: `Long`, birim USD (TMDB ile aynı), `null` = bilinmiyor. Servis `0`'ı `null`'a çevirir (TMDB "bilinmiyor" için 0 döner). BigDecimal bilinçli seçilmedi — kuruş hassasiyeti sahte olurdu.
- **`tagline` / `taglineTr`**: `title`/`titleTr` ile aynı desen; detay yanıtındaki `tagline` `LocalizedTextResolver` ile dile göre çözülür, `taglineTr` ham döner.
- **`directorIds` / `writerIds`**: `producerIds` ile birebir aynı desen (`@ElementCollection Set<Long>`, `movie_directors`/`movie_writers`, `(movie_id, person_id)` unique, DB FK yok, servis `PersonService.assertAllExist` ile doğrular; update'te `null` = değişmedi). `(person_id, movie_id)` indeksi "bu yönetmenin filmleri" yönü için. Tek `movie_crew(role)` tablosu bilinçli seçilmedi — yeni roller (görüntü yönetmeni, besteci) gerekirse `Cast` benzeri ayrı bir entity gerekir, `ElementCollection` olmaz. Person silinince bu tablolardaki kalıntı ID'ler temizlenmez (producerIds'teki mevcut davranışla aynı).
- **İçerik uyarıları (`contentWarnings`) Movie'de alan DEĞİL**: `tag/` modülünde `TagType.CONTENT_WARNING` olarak modellenir (bkz. blog Mood facet'i emsali). Editör tag'i açar, `TagAssignment` ile Movie/Series/... 'e atar; FE `GET /api/tags/assignments?taggableType=MOVIE&taggableId=` ile okur ve `tagType == CONTENT_WARNING` olanları süzer. **Bilinçli olarak Movie detay yanıtına gömülü değil**: `MovieDetailResponse` `@Cacheable` (1 saat) ve movie/ modülü `tag/` entity'sine dokunamaz (yalnız `TagAssignmentService` interface'i); gömmek `tag/` yazmalarının Movie cache'ini evict etmesini gerektirirdi (blog hub'ın `blog:hub` düz-string evict'i gibi). Gerekirse `MovieService` içinde `TagAssignmentService.listForTarget` ile zenginleştirilir. `tag.type` kolonu da varchar olmalı: `tag_content_warning_migration.sql`.
- **Doğrulama sınırı**: bu alanlar eklenirken Spring context (`@SpringBootTest`) testleri DB olmadığı için çalıştırılamadı; entity eşlemesi ve iki migration script'i gerçek bir MySQL'de denenmedi.

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

### Trivia Modülü (`trivia/`)

- **Polimorfik, `person/Character` deseniyle aynı**: `itemType`(MOVIE/SERIES)+`itemId` düz alanlar, gerçek FK yok; `TriviaServiceImpl` create/update'te `MovieService`/`SeriesService.existsById` ile doğrular (geçersiz → 400). `createdBy` de `user/`'a düz `Long` (FK yok — cross-module ID-only kuralı).
- **API sözleşmesi FE spec'inden geldiği için projenin geri kalanından iki fark**: (1) `itemType` JSON/query'de küçük harf (`movie`/`series`; girişte büyük/küçük fark etmez), DB'de enum adı; (2) `GET /api/trivia` liste `{"data":[...]}` zarfıyla döner (`TriviaListResponse`), diğer listeler düz `List<T>`. `tag` uppercase (`BEHIND_THE_SCENES, EASTER_EGG, CASTING, GOOF, LORE`).
- **Çok dilli metin + görsel**: `Movie.title/titleTr` deseni — `title`/`content` ana (EN) alan (`content` zorunlu, `title` opsiyonel), `titleTr`/`contentTr` opsiyonel. Yanıtta `title`/`content` `LocalizedTextResolver` ile istek diline göre çözülür (boşsa diğer dile düşer), `titleTr`/`contentTr` ham döner. Boş/whitespace string'ler `null`'a çevrilir. `imageUrl` (opsiyonel, ≤500) `media/` yüklemesinden dönen düz URL'dir; `TriviaServiceImpl` `ImageStorageService` inject eder (update'te `deleteIfChanged`, delete'te `delete` — bkz. görsel kuralı).
- **`random=true`**: native `ORDER BY RAND() LIMIT n` (MySQL'e özgü). `(item_id, item_type, created_at)` indeksi sorguyu bir yapımın küçük kümesine daraltır, RAND yalnız onun üzerinde çalışır. `limit` yoksa üst sınır 100 (`MAX_LIMIT`); random dışı sıralama `createdAt DESC, id ASC`.
- **Cache YOK (bilinçli)**: `random` sonuçları zaten cache'lenemez, sıralı okuma da ucuz indeks taraması.
- **Yetki**: GET public; POST/PUT/DELETE `EDITOR/MODERATOR/ADMIN` (içerik yazma uçlarıyla aynı, `SecurityConfig`).
- **Doğrulama sınırı**: `random` native sorgusu ve entity eşlemesi gerçek MySQL'de denenmedi (servis birim testi Mockito ile).

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
- **`hotScore`**, `community/HotScoreJob` tarafından 15 dakikada bir (`@Scheduled(fixedDelay=900000)`) tüm `PUBLISHED` thread'ler için yeniden hesaplanır: `(likeCount + 2*commentCount) / (ageHours+2)^1.5` (formülün TEK kaynağı `ThreadRepository.recalculateHotScores` native SQL'i). **Hiçbir thread Java'ya çekilmez**: hesap+yazma DB'de, PK aralığı pencereleriyle (`community.hot-score.window-size`, varsayılan 10.000 id; `[fromId, toId)` başına tek `UPDATE`), her pencere `HotScoreRangeUpdater`'da KENDİ kısa transaction'ında (ayrı bean: self-invocation `@Transactional`'ı atlar). Pencere çok büyük verilirse tek statement'a dönüşür ama tüm satırları aynı anda kilitler. `UPDATE` yalnızca `hot_score` kolonunu yazar — entity yazsaydık Hibernate tüm kolonları yazıp eşzamanlı like/yorum sayaçlarını ezebilir, `updatedAt` de her çalışmada değişirdi. `:now` parametre olarak verilir (SQL `NOW()` DB saat dilimini kullanır, `created_at` JVM'inkini). Native SQL MySQL'e özgü (`POW`, `TIMESTAMPDIFF`). Pencere hatasında çalışma bırakılır, sonraki çalışma baştan hesaplar. Maliyet notu: `hot_score` 3 indekste (`idx_thread_status_*_hot`) olduğundan tam yeniden yazım indeks bakımı yüzünden yavaştır (500k satırda ölçüm: bkz. 2026-09-19 raporu, ~78 sn). `qualityScore` hâlâ hiçbir yerde hesaplanmıyor. Çoklu instance'ta job her instance'ta çalışır (dağıtık kilit yok).
- **Yazar zenginleştirmesi (username + avatarUrl)**: `ThreadSummaryResponse`/`ThreadDetailResponse`/`CommentResponse`, ham `authorId`'yi KORUYARAK yanına `AuthorSummary(id, username, avatarUrl)` ekler. `username` `UserService.getUsernamesByIds(Set<Long>)` ile, `avatarUrl` `UserProfileService.getAvatarUrlsByUserIds(Set<Long>)` ile toplu çözülür (`ThreadServiceImpl`/`CommentServiceImpl` sayfa başına ikisi için de tek sorgu, N+1 yok). Silinmiş kullanıcıda/hiç profil oluşturmamış kullanıcıda `username`/`avatarUrl` null gelir, kayıt silinmez.
- **`UserProfile` ailesi `account/`'tan buraya taşındı (döngüsel bağımlılık önleme)**: `account/UserProfileServiceImpl` zaten Faz 2'de `commentCount`/`theoryCount` için `community/ThreadService`+`CommentService`'i inject ediyordu (`account/ → community/`). avatarUrl zenginleştirmesi eklenince ters yönde (`community/ → account/UserProfileService`) bir bağımlılık daha gerekiyordu — ikisi birden döngüsel bağımlılık (Spring başlangıçta patlar) yaratacağından `UserProfile`/`UserProfileService`/`UserProfileMapper`/`UserProfileRepository`/`UserProfileResponse`/`UpdateUserProfileRequest`/`ProfileStats` community/'ye taşındı. `account/AccountController`/`PublicAccountController` artık `community.service.UserProfileService`'e (tek yönlü `account/ → community/`) delege eder; DB tablo adı (`user_profile`) değişmedi. **Taşıma tek başına yeterli değildi**: `UserProfileServiceImpl`'in `commentCount`/`theoryCount` için `ThreadService`/`CommentService` **interface**'lerini inject etmesi (artık aynı modül içi olsalar bile) `ThreadServiceImpl`/`CommentServiceImpl`'in avatarUrl için `UserProfileService`'e bağımlı olmasıyla birleşince yine döngüsel bean referansı yaratıyordu (`UserProfileServiceImpl → ThreadServiceImpl → UserProfileServiceImpl`) — modül sınırı aşılmış olsa da üç servis birbirine dairesel bağlanmış oluyordu. Çözüm: `UserProfileServiceImpl`, `ThreadService`/`CommentService` yerine doğrudan `ThreadRepository.countByAuthorIdAndSurfaceAndStatus`/`CommentRepository.countByAuthorIdAndStatus`'u çağırır (aynı modül içi repository erişimi serbest, cross-module kuralı sadece başka modülün repository'sine erişimi yasaklar). Bu düzeltme sonrası `ThreadService`/`CommentService` interface'lerindeki artık kullanılmayan `countByAuthorIdAndSurface`/`countByAuthorId` metotları da kaldırıldı. `account/`'a kalan `ActivityLogService.countByUserIdAndType`/`UserLikeService.countByUserId` (interface üzerinden, cross-module, gerçek leaf servisler) ile `ProfileStats`'ın diğer alanları hesaplanmaya devam eder. **Kişi takibi (2026-09) eklenince `UserProfileServiceImpl`'e bir cross-module bağımlılık daha girdi: `account/UserFollowService`** (`followerCount`/`followingCount`/`isFollowing` için, `itemType=USER`). Bu da döngü YARATMAZ: `UserFollowServiceImpl` yalnızca `UserFollowRepository`+`ItemReferenceValidator`'a bağımlı, `ItemReferenceValidator` da `Movie`/`Series`/`BlogService` + (kişi takibi doğrulaması için eklenen) `user/UserService`'e bağımlı — hiçbiri `community/`'ye dokunmuyor, yani zincir tek yönlü kalıyor (`community/UserProfileServiceImpl → account/UserFollowService → {Movie,Series,Blog,User}Service`, geri dönüş yok).
- **`isLiked`/`isBookmarked` (Thread'de ikisi de, Comment'te sadece `isLiked`)**: `ThreadLikeRepository`/`ThreadBookmarkRepository`/`CommentLikeRepository`'deki toplu `findXIdsByUserIdAndXIdIn` sorgularıyla, listeleme sayfası başına tek sorgu şeklinde çözülür. Anonim istekte (`viewerId=null`, GET uçlarında `@AuthenticationPrincipal` null gelebilir) hiç sorgu atılmaz, hepsi `false`.
- **`ThreadSummaryResponse.excerpt`**: `ThreadMapper.buildExcerpt` ile `body`'den türetilir (160 karakter, kelime ortasından kesmez, son boşluğa geri sarar + `"..."`). `ThreadDetailResponse` zaten tam `body`'yi döndürdüğü için ayrıca excerpt taşımaz.
- **Yorum silme idempotent sayaç azaltma**: `CommentServiceImpl.delete`, yalnızca yorum henüz `DELETED` değilse durumu değiştirir VE (sadece `subjectType==THREAD` ise) `ThreadRepository.decrementCommentCount` çağırır — zaten silinmiş bir yoruma tekrar `DELETE` atmak `commentCount`'u fazladan düşürmez.
- **`Comment` polimorfik (THREAD/BLOG/SEASON/EPISODE)**: `Comment.thread` (`@ManyToOne`) FK'si kaldırıldı, yerine `person/Cast`'teki subjectType+subjectId deseninin aynısı geldi — `CommentSubjectType(THREAD/BLOG/SEASON/EPISODE)` + `subjectId` (gerçek FK yok). `CommentServiceImpl.validateSubject`, `BlogService`/`SeasonService`/`EpisodeService` **interface**'lerini (`existsById`) inject ederek BLOG/SEASON/EPISODE'u doğrular; THREAD için `ThreadRepository.findById`+`status==PUBLISHED` kontrolü aynen korunur (DELETED bir thread'e yorum eklenemesin diye). `SeasonService.existsById` bu iş için eklendi (`EpisodeService`/`BlogService`'te zaten vardı). **Migration script yazılmadı** — community/ verisi henüz production'da yok, `ddl-auto=update` yeni şemayı sıfırdan kurar. **Yorum sayısı Blog/Season/Episode'da denormalize edilmez** (Thread'in aksine) — o modüllerin kendi yorum sayısı ihtiyacı olursa `PageResponse.totalElements`'e güvenilir, ekstra bir sayaç kolonu açılmadı. **Merkezi uçlar** (`GET/POST /api/community/comments`, query/body'de `subjectType`+`subjectId`) eklendi ve `SecurityConfig`+`CommunityRateLimitFilter`'a (aynı comment-create bucket, 30/saat) işlendi — path'in blog/series modüllerinin kendi controller'ları altında değil `community/` altında merkezi kalmasının nedeni, blog/series'in blanket `EDITOR/MODERATOR/ADMIN` yazma kuralına yorumun (herkes yazabilir) nested bir carve-out olarak sızma riskini önlemek. **Eski THREAD-özel uçlar** (`GET/POST /api/community/threads/{slug}/comments`) AYNEN kalır — `CommentServiceImpl.listForThread`/`create`, slug'ı `ThreadRepository.findBySlugAndStatus` ile id'ye çözüp yeni `listForSubject`/`createForSubject`'e delege eder. `CommentResponse.threadId` geriye uyumluluk için korunur, `subjectType==THREAD` iken `subjectId`'yle aynı değeri taşır, diğer türlerde `null` döner (yeni entegrasyonlar `subjectType`/`subjectId`'yi kullanmalı).
- **`community/CommunityRateLimitFilter`**: `security/AuthRateLimitFilter` ile aynı in-memory `ConcurrentHashMap` deseni ama IP değil userId bazlı — `POST /api/community/threads` (10/saat), `POST /api/community/threads/*/comments` (30/saat). `JwtAuthenticationFilter`'dan SONRA zincire eklenir (`SecurityConfig`) ki `SecurityContextHolder`'da authentication çözülmüş olsun; anonim istekte devre dışı kalır (o istek zaten yetkilendirmede 401'e düşer).
- **Faz 1 kapsam dışı (hâlâ)**: Vote, Report, ModerationAction hiç kodda yok; `qualityScore` hesaplanmıyor.
- **Faz 3 — Tag sistemi**: `TagFollow`, `ThreadLike`/`ThreadBookmark` ile birebir aynı desende (surrogate id + `(user_id, tag)` unique constraint), `account/`'ın genel `SavedItem`/`UserFollow` mekanizmasından bilinçli olarak bağımsız (`ThreadTag`'ın `tag/` modülünden bağımsız olma gerekçesiyle aynı). `TagFollowServiceImpl.follow/unfollow` idempotent, tag `SlugGenerator.slugify` ile normalize edilir. **Trending tags job/cache YOK** — `ThreadTagRepository.findTrending` her istekte on-the-fly JPQL `GROUP BY` (bilinçli basit çözüm, `production/` modülündeki trade-off'a benzer; ölçek sorunu çıkarsa saatlik bir `TrendingTagsJob`+cache'e geçilebilir). `GET /api/community/tags/{tag}/threads`, yeni bir servis metodu YAZMADAN `ThreadService.list(null, null, tag, sort, viewerId, pageable)`'a ince bir delege — mevcut `?tag=` filtresiyle aynı mantık, sadece daha temiz URL.
- **Pre-existing bağımsız düzeltme**: `MovieService`/`SeriesService` interface'lerinde `existsBySlug(String)` yoktu (repository'de vardı ama servise hiç açılmamıştı) — `ThreadServiceImpl.validateProductionSlug` bunu çağırdığı için community modülü hiç derlenmiyordu; bu turda `existsById` ile simetrik şekilde eklendi (`MovieServiceImpl`/`SeriesServiceImpl`'de doğrudan repository'ye delege).
- **Thread medyası (çoklu görsel + video)**: `ThreadMedia` (id, thread FK, `type` IMAGE|VIDEO, `url` ≤500, `position` 0..n) — `ThreadRequest`/`ThreadPatchRequest.media` (≤6, sıra = liste sırası; PATCH'te null=değişmedi, `[]`=hepsi silinir, dolu=tamamen değiştir), response'larda `media: [{type,url,position}]`. Tüm iş `ThreadMediaService`'te (ThreadServiceImpl ince delege): doğrulama, kalıcılık, storage temizliği, toplu okuma (`getMediaByThread`, sayfa başına tek IN sorgusu). **`imageUrl` geriye uyumluluk**: her medya yazımında `thread.imageUrl` = ilk IMAGE'ın url'ine (yoksa null) eşitlenir; media satırı olmayan eski thread'lerde response'a tek elemanlı IMAGE olarak sentezlenir. Request'te deprecated `imageUrl` yalnızca `media` yoksa/boşsa `media[IMAGE]` gibi işlenir (PATCH'te boş string = medya temizle). **URL doğrulaması** `media/MediaUrlValidator` üzerinden (`https://res.cloudinary.com/<cloud>/{image|video}/upload/...`, query/fragment/userinfo/`..` reddedilir, type↔path eşleşmeli, `CLOUDINARY_URL` yoksa fail-closed). **Storage temizliği**: çıkarılan medya transaction COMMIT'inden sonra silinir (rollback dosyayı kaybettirmesin), hata loglanıp yutulur; yalnızca `fandoom/community/` klasöründeki (upload'da `folder=community`) ve başka thread'in kullanmadığı URL'ler silinir — aksi halde kullanıcı başka birinin/film posterinin URL'ini koyup çıkararak Cloudinary'den silebilirdi. Thread soft-delete'te medya SİLİNMEZ (moderasyon kaydı, `Comment` ile aynı gerekçe). `thread_media(thread_id, position)` üzerinde UNIQUE bilinçli yok (sil+ekle aynı transaction'da IDENTITY insert'i delete'ten önce çalıştırır). Cache: mevcut `@CacheEvict(allEntries)` create/update/delete'te medyayı da kapsar. **Video yükleme**: `POST/DELETE /api/media/videos` (`VideoStorageService`, mp4/webm/quicktime ≤50MB, destroy `resource_type=video`); görsel ≤10MB. Multipart limiti 100MB.

### Portal (`community/` içinde)

Community thread'lerini "oda"lara (ör. bir evren/yapım etrafında toplanan topluluk alanı) bölen katman. Ayrı modül DEĞİL, `community/` içinde (Thread'in doğrudan `portalId`'si var, aynı aggregate sınırı). Tam API sözleşmesi: `docs/portal-api-contract.md`; devam/karar geçmişi: `docs/plans/2026-09-21-portal-devam-notlari.md`; canlı geçiş: depo kökündeki `portal_migration.sql`.

- **Entity'ler**: `Portal` (slug, `nameTr/En`, `descriptionTr/En`, banner/icon URL, `accentColor` (opsiyonel `#RRGGBB`), `status` `PortalStatus(ACTIVE|ARCHIVED|HIDDEN)`, `postingPolicy` `PortalPostingPolicy(OPEN|STAFF_ONLY)`, `sortOrder`, denormalize `memberCount`/`threadCount`), `PortalProduction` (portal↔yapım eşlemesi; `productionSlug` düz alan, gerçek FK yok, bir yapım en fazla bir portala bağlı), `PortalMembership` (userId+portalId unique, join idempotent). `Thread.portalId` **zorunlu** (NOT NULL, `fk_thread_portal`): her thread tek bir portala aittir, `ThreadRequest.portalSlug` zorunlu.
- **`accentColor`**: nullable, yalnız `^#[0-9A-Fa-f]{6}$` doğrulanır (PATCH'te `""` = kaldır, null = değişmedi); Summary/Detail/Admin yanıtlarında döner, `PortalRefResponse`'ta YOK. Gradyan/sis/okunabilirlik (color-mix) tamamen FE'nin işi — backend CSS/gradyan string'i saklamaz. Banner/ikon boyutlandırması da FE'de (Cloudinary URL dönüşümü).
- **Statü semantiği**: `HIDDEN` public tarafta "yok" sayılır — portal ve içindeki thread/yorum/like/bookmark uçları 404, portalın slug/adı hiçbir yanıtta sızmaz (`getRefsByIds` HIDDEN için ref dönmez, `findTrending`/`countByTagAndThread_Status` HIDDEN portal thread'lerini dışlar); moderatör/admin serbest. `ARCHIVED` **yazma kapalı**: yeni thread, thread PATCH (moderatör hariç), yorum ekleme, like/bookmark ekleme, yeni üyelik, thread taşıma → 400 (`ThreadRepository.isInArchivedPortal`); serbest kalanlar DELETE, unlike/unbookmark, portaldan ayrılma. `STAFF_ONLY` portala yalnız MODERATOR/ADMIN thread yazar (403).
- **Thread taşıma**: `PATCH /threads/{id}` + `portalSlug` yalnız MODERATOR/ADMIN (sahip için mevcut slug dışı 403). Taşıma thread satırını `SELECT ... FOR UPDATE` ile kilitler ve iki portalın `threadCount`'unu tek transaction'da, **portal id sırasıyla** günceller (karşılıklı taşımada deadlock önleme). `applyDelete` de thread'i kilitli okur (eşzamanlı taşıma+silmede sayaç kayması).
- **Sayaç kilit sırası (deadlock)**: create'te önce `incrementThreadCount` (portal satırı X-kilit), SONRA thread INSERT — ters sırada FK'nın S-kilidi + UPDATE S→X yükseltmesi deadlock üretirdi. Silmede sayaç yalnız `markDeletedIfPublished` gerçekten bir satır değiştirdiyse düşer (çift düşüm yok).
- **Thread slug'ı saf-sayısal olmaz**: `ThreadServiceImpl.threadSlug` sonucu yalnız rakamsa `-t` ekler (`/threads/{segment}`'te rakam-only segment her zaman id sayılır). `common/SlugGenerator`'a konmadı — Movie/Series slug'ları ("1917") etkilenmesin diye kural Thread'e özgü.
- **Uçlar**: public GET `/api/community/portals[/{slug}[/feed]]`; AUTH `POST/DELETE /api/community/portals/{slug}/join`, `GET /api/me/portals`, feed `?scope=joined`; ADMIN `/api/admin/community/portals` (liste HIDDEN dahil, POST, `PATCH /{slug}`, `POST /{slug}/archive|unarchive`; silme ucu YOK — kaldırmak = ARCHIVED/HIDDEN). `SecurityConfig`'te `/api/community/portals/**` GET permitAll, join yazma `authenticated()`, admin yolları `hasRole("ADMIN")`.
- **Sıralama**: dizin `sort=trending` (varsayılan; skor = `3*yeniThread(7g) + yeniYorum(7g)`, on-the-fly, cache/job yok — ölçek sorunu çıkarsa denormalize `activity_7d` + job'a geç), `members`, `new`, `alpha`; trending eşitliğinde `sortOrder→id`. `/api/me/portals?sort=activity` aynı skoru kullanır. Portal uçlarında Redis cache YOK (kullanıcıya özel `isMember` + dil).
- **Rate limit** (`CommunityRateLimitFilter`, userId bazlı): portal join+leave AYNI bucket, 60/saat. Thread create 10/saat, yorum 30/saat (id ve slug varyantları aynı bucket).
- **Cache**: anonim thread listeleri `productionSlug` verildiğinde CACHE'LENMEZ (rastgele slug ile sınırsız key çeşitliliği); portal + tag filtreli listeler mevcut kurallarla cache'lenir. Yanıttaki `portal.name` istek diline göre çözüldüğünden `ThreadCacheKeys` her key'e dil ekler.
- **Migration**: `portal_migration.sql` idempotent, yeni build'in İLK açılışından ÖNCE çalıştırılmalı (yoksa Hibernate `portal_id`'yi NOT NULL ekleyip mevcut satırları bozar, indeks/FK adları çakışıp kopya açılır). Mevcut thread'ler `production_slug` bir portala bağlıysa o portala, değilse `genel-sohbet`'e backfill edilir; sayaçlar yeniden hesaplanır.
- **Bilinen varsayımlar**: taşımada `productionSlug` tutarlılığı kontrol edilmez (yalnız create'te portalın yapımı olmalı); trending skorunda çoklu instance/dağıtık cache yok.

### Veri çekme & performans kuralları

- **Tüm `@ManyToOne` `LAZY`** (`@OneToOne` yok) — yeni ilişki eklerken de böyle kalmalı.
- **Liste sayfalarında N+1 yasak**: sayfadaki id'ler toplanıp tek `IN` sorgusuyla çözülür (`ThreadServiceImpl.mapSummaries`, `CommentServiceImpl.enrich`, `BlogQueryServiceImpl.toFilterableSummaries`; cross-module için `TagAssignmentService.listForTargets`, `FranchiseService.getByIds`). Tekil detay okumalarında `@EntityGraph` (`findWithSeasonsById`, `findWithEpisodesById`, `findWithBlocksBySlugAndStatus`) — tek bag koleksiyon JOIN FETCH, diğerleri `hibernate.default_batch_fetch_size=50` ile toplu gelir (birden çok `List` aynı grafta fetch edilemez: MultipleBagFetchException).
- **Koleksiyon JOIN FETCH + `Pageable` birlikte KULLANILMAZ** (HHH000104 in-memory paging). Gerekirse iki aşama: önce ana kayıtlar sayfalanır, sonra `IN (ids)` ile koleksiyon çekilir.
- **Yüksek trafikli listeler keyset (cursor) sayfalama sunar**: `GET /api/community/feed/cursor`, `/threads/{slug}/comments/cursor`, `/comments/cursor` → `KeysetPageResponse(content, hasNext, nextCursor)`; `common/util/KeysetCursor` (opak Base64 `değer|id`) + `common/specification/KeysetSpecification`. COUNT/OFFSET yok, `size+1` satır çekilip `hasNext` türetilir, sıralama daima `(alan DESC, id ASC)` — tie-breaker `id DESC` OLAMAZ: InnoDB ikincil indeksler PK'yı örtük `ASC` ekler, ters yön (`x DESC, id DESC`) indeksi sıralamada kullandırmaz ve FILESORT'a düşer (EXPLAIN ile doğrulandı). Eski `Pageable` uçları geriye uyumluluk için duruyor. Movie/Series/Production feed'i kendi tarih-bazlı cursor'ını (`CursorPageResponse`) kullanmaya devam eder.
- **Composite index kuralı**: `WHERE` eşitlik sütunları önce, `ORDER BY` sütunu sona (`status, surface, created_at DESC`). Araya sorguda olmayan bir sütun girmemeli (Comment'teki eski `status` örneği filesort'a yol açıyordu). NOT: `ddl-auto=update` eski/yeniden adlandırılan indeksleri DROP etmez — mevcut DB'de elle temizlenmeli (`idx_thread_author`, `idx_comment_subject_status_created`, `idx_comment_parent`, `idx_comment_author`, `idx_blog_relation_source`, `idx_event_subject`, `idx_character_subject`).

### Redis Cache (`common/config/RedisConfig`, `CacheErrorConfig`)

- **Tek global TTL (1 saat), çoklu TTL yok.** Değerler DTO (record) olarak JSON saklanır (`GenericJacksonJsonRedisSerializer` — `GenericJackson2JsonRedisSerializer`'ın Jackson 3 halefi; Jackson 2 SDR 4.x'te `@Deprecated`), key'ler `StringRedisSerializer`, allow-list'li default typing. Entity asla cache'e girmez. `transactionAware()`: `@CacheEvict` transaction COMMIT'inden sonra çalışır. Redis erişilemezse `CacheErrorConfig` hatayı loglayıp DB'ye düşer (500 değil). **`sync = true` okuma yolu** için bu tek başına yetmez (Redis hatası loader'dan önce fırlar ve loader'ın 404'ü de 500'e çevrilirdi): `RedisConfig.FailSafeRedisCacheManager` her cache'i `FailSafeCache` ile sarar — Redis hatasında loader doğrudan çalışır, iş exception'ı (404) özgün haliyle döner, loader zaten bittiyse tekrar çalışmaz (`CacheNotFoundPropagationTest`, `FailSafeCacheTest`).
- **Her `@Cacheable` `sync = true`** (stampede) — `CacheContractTest` bunu zorlar. `sync=true` ile `unless` ve çoklu cacheName birlikte KULLANILAMAZ; koşul `condition` ile verilir. Not: `RedisCache` sync kilidi JVM-yerelidir (dağıtık değil).
- **Yazma = `@CacheEvict(allEntries = true)`** (aynı kayıt id/slug/liste key'leriyle ayrı girdilerde durur; tek key silmek bayat bırakır). Cache adı sabitleri her modülün kendisinde (`MovieCacheNames`, `SeriesCacheNames`, `ThreadCacheNames`, `BlogCacheNames`). Cache'li: Movie/Series okumaları, Thread `list`/`listByCursor`(ilk sayfa)/`getBySlug`, Blog `list`/`getBySlug`/`findRelatedForProduction`/hub (`findFilterable`, `getFacets`). Cache'lenmeyenler (bilinçli): `search` (sınırsız key), yorumlar, `BlogService.getById` (admin + yan etki), `exists*`.
- **Kullanıcıya özel veri paylaşılan cache'e girmez**: Thread okumaları yalnızca `viewerId == null` iken cache'lenir (isLiked/isBookmarked). Derin sayfalar (`pageNumber >= 5`) ve cursor'lu sayfalar cache'lenmez (sınırsız key çeşitliliği). Thread key'leri `ThreadCacheKeys` ile normalize edilir (`sort`/`tags`).
- **Yan etkili okumaya `@Cacheable` KONMAZ**: `BlogServiceImpl.getBySlug` viewCount artırır + aktivite kaydeder; yalnızca "detayı yükle" kısmı ayrı bean'de (`BlogDetailCache`, self-invocation proxy'yi atlar) cache'lenir, yan etkiler her istekte çalışır. Dönen `viewCount` cache'in dolduğu andaki değerdir.
- **Cross-module bayatlama**: blog hub (`blog:hub`) mood/franchise verisinden beslenir → `tag/` ve `franchise/` yazmaları hub cache'ini düz string adıyla temizler (blog/'a paket bağımlılığı olmasın diye). Diğer türetilmiş alanlar TTL (1 saat) ile sınırlı bayatlar: blog `findRelatedForProduction` (movie/series slug değişimi), `viewCount` sayaçları (liste/hub'da).
- **Yeni bir cache'li metod eklerken**: `sync = true`, kullanıcıya özel alan varsa `condition`, DTO döndür, ilgili TÜM yazma metodlarına evict ekle (Season/Episode → Series örneği gibi gömülü veri kaynaklarını da unutma).

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
  - Listede **olmayan** her şey (path kuralı yazılmamış yeni bir yol; `/api/tags/**` artık kurallı: GET public, yazma EDITOR/MODERATOR/ADMIN) `anyRequest().authenticated()`'a düşer, yani sadece login yeterli, rol şartı yok. Yeni bir modül yazma ucu eklerken bunu unutmayın — path'i açıkça `SecurityConfig`'e eklemezseniz varsayılan sadece "giriş yapılmış olsun" olur, rol kısıtı olmaz.
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
