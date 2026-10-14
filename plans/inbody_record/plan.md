# 인바디 기록과 사진별 분석 — 구현 계획

**스펙**: [spec.md](spec.md) / **설계**: [design.md](design.md)

## 적용 규칙

- `.claude/rules/code-style.md` `표기`: 이름은 줄이지 않는다(`viewModel`). 주석은 "왜"만 적는다. 선언 KDoc 첫 문장은 역할까지만 쓴다. `.kt` 편집 후 훅이 `spotlessApply`를 실행한다.
- `.claude/rules/code-style.md` `MVI`: 화면 하나는 패키지 하나(`<화면>Contracts.kt`, `<화면>ViewModel.kt`, `composable/`). ViewModel은 `BaseViewModel<S, I, E>`를 상속하고 `handleIntent`만 공개한다. `<화면>View`는 구독·`UiEvents` 조립·effect 처리만 하고, UI는 `<화면>ViewImpl`에만 둔다. 화면 밖 이벤트는 `<화면>Events`, 안 이벤트는 `<화면>UiEvents`다. effect 수집은 `CollectEffect`.
- `.claude/rules/code-style.md` `Compose`: `Modifier`는 첫 optional 파라미터. 색·타이포는 `MaterialTheme`과 `core:designsystem` 값만 쓴다. `@Preview`는 `private`이고 이름은 `<대상>Preview`.
- `.claude/rules/code-style.md` `배치`: `ui → domain ← data` 한 방향이고 `domain`은 Android에 의존하지 않는다. Entity는 `data` 밖으로 내보내지 않는다. 새 화면은 `:feature:<이름>:impl`의 화면 패키지에, NavKey와 navigate 함수는 `:feature:<이름>:api`에 둔다. 여러 화면이 함께 쓰는 컴포넌트는 도메인 종속이면 `core:ui:components`, 아니면 `core:designsystem`이다.
- `CLAUDE.md` `아키텍처`: 다른 feature는 상대의 `api`만 의존한다. 의존 버전은 `gradle/libs.versions.toml`에서만 관리한다. 모듈 `build.gradle.kts`에는 컨벤션 플러그인, `namespace`, 고유 의존성만 적는다.
- `CLAUDE.md` `프로젝트`: 컴파일은 `./gradlew :app:compileDebugKotlin`, 테스트는 Android 모듈 `./gradlew :<모듈>:testDebugUnitTest`, JVM 모듈 `./gradlew :core:domain:test`.
- 테스트 문서는 없다. 선례를 따른다: 함수명은 백틱 한글 문장(`AnalyzeInbodyUseCaseTest`), 대역은 테스트 소스의 `Fake*` 클래스(`feature/my/impl/src/test/.../fake/`), 코루틴은 `runTest`와 `MainDispatcherRule`.
- 색은 앱 토큰을 유지한다(사용자 결정 2026-10-14). 시안의 인디고는 `Accent`, 연보라는 `AccentSurface`, 연보라 흰 배경은 `Background`로 대응한다.

## 구조 결정

| 결정 | 근거 |
|---|---|
| 기록은 새 테이블 `inbody_record`로 두고, 분석 결과는 기존 `analysis_result`(종류 `INBODY`, `scopeKey` = 기록 번호 문자열)를 그대로 쓴다 | 결과 저장·요약·묶음·측정값 컬럼·`AnalysisResult` 모델·`AnalysisResultContent`·알림·백업 경로가 이미 있다. 기록 번호를 `scopeKey`에 두면 작업 열쇠(`AnalysisWork.nameOf`)가 기록마다 달라져 설계의 "기록마다 독립 작업"이 기존 `ExistingWorkPolicy.KEEP`으로 해결된다 |
| 결과 교체는 `AnalysisResultDao`의 `@Transaction` 메서드(해당 열쇠 삭제 후 삽입)로 한다 | `RoutineDao`·`WorkoutEntryDao`에 `@Transaction` DAO 메서드 선례가 있다 |
| 결과의 `imageFileName` 컬럼은 남기되 인바디에서는 쓰지 않는다(이전 후 `NULL`) | 컬럼 삭제는 테이블 재생성이 필요하고 식단·운동 결과도 같은 컬럼을 `NULL`로 쓴다. 지우는 이득이 없다 |
| 분석 상태는 저장하지 않고 `ObserveInbodyEntriesUseCase`가 기록·결과·작업 상태를 합쳐 정한다 | `design` 데이터 모델 `기록 상태`. 작업 상태는 `AnalysisRunner`가 이미 낸다 |
| 순수 계산(측정 순서, 가장 최근 측정, 기간, 반올림, 변화량, 축)은 `core:domain`의 모델·UseCase로 둔다 | `core:domain`이 순수 JVM이라 `:core:domain:test`로 검증된다. 선례: `GetProgressSummaryUseCase`가 같은 식으로 계산을 맡는다 |
| 옛 인바디 이력 변환은 `core:local`의 순수 함수 `LegacyInbodyConverter` 하나로 두고 DB 이전과 옛 백업 복원이 함께 쓴다 | 변환 규칙이 둘이 되면 어긋난다. Entity가 `core:local`에 있어 `core:data`로 못 올린다 |
| 인바디 메인은 `AnalysisScreen`을 쓰지 않고 `BodyPlanTopBar` + 스크롤 `Column`으로 독립 구성한다 | `AnalysisScreen`은 하단 고정 버튼과 단일 `result`를 전제하고 식단·운동 분석 화면이 함께 쓴다. 시안의 메인에는 하단 고정 버튼이 없고 기록별 버튼이 있다 |
| 화면 셋이 함께 쓰는 것(안내 상자, 날짜·수치 표기, 상태 문구)은 `feature/my/impl/.../inbodycommon/`에 둔다 | 한 feature 안의 공유물이라 `core`로 올리지 않는다. `code-style.md`의 "화면 하나는 패키지 하나"를 지키려고 화면 패키지 밖에 둔다 |
| 사진 추가 화면은 `inbodyadd/`, 기록 상세 화면은 `inbodydetail/`, 메인은 기존 `inbody/`를 다시 쓴다 | `MyNavGraph.kt`가 화면마다 패키지를 가지는 선례(`aitoken`, `backup`, `profile`, `weight`) |
| 측정일 선택은 `BodyPlanCalendar`를 `Dialog`에 담아 쓴다 | `ComparisonDatePickerDialog`(workoutlog)가 같은 방식이다. feature끼리 `impl`을 못 가져다 쓰므로 인바디 쪽에 같은 모양으로 새로 만든다 |
| 촬영은 `ActivityResultContracts.TakePicture`와 `FileProvider` 임시 파일을 쓴다 | 매니페스트에 `CAMERA` 권한이 없고 인텐트 방식은 권한이 필요 없다. 촬영 파일은 사진 보관소로 복사한 뒤 지운다 |
| `Type.kt`에 두 단계를 더한다: `headlineMedium` 24sp Bold, `displaySmall` 32sp ExtraBold | 사용자 결정 2026-10-14. 추가만 하는 변경이라 기존 화면은 그대로다 |
| 지표 카드는 단일 지표 전용 `MetricTrendCard`를 `feature/my/impl`에 새로 만든다. `TrendChart`는 건드리지 않는다 | `TrendChart`는 눈금 숫자를 일부러 그리지 않는 설계이고 시안과 정반대다. 사용처(`ExerciseTrendCard`)를 지키려고 분기를 늘리지 않는다 |

## 구현 계약

### 저장소·모델 (`core:local`)

- `InbodyRecordEntity` 신규 (표 `inbody_record`): `id: Long`(자동 증가), `imageFileName: String`, `measuredOnEpochDay: Long`, `createdAtMillis: Long`, `failureReason: String?`(기본값 `null`).
- `InbodyRecordDao` 신규: `observeAll(): Flow<List<InbodyRecordEntity>>`, `observe(id)`, `getById(id)`, `insert(entity): Long`, `updateFailureReason(id, reason: String?)`, 백업용 `getAll()`·`insertAll`·`deleteAll`.
- `AnalysisResultDao`: `@Transaction suspend fun replace(entity: AnalysisResultEntity)` 추가. `@Query("DELETE FROM analysis_result WHERE kind = :kind AND scopeKey = :scopeKey") suspend fun deleteByScope(...)`도 둔다.
- `BodyPlanDatabase`: `version = 13` → `14`, 엔티티에 `InbodyRecordEntity` 추가, `inbodyRecordDao()` 추가. 스키마 파일 `schemas/…/14.json`은 빌드가 만든다.
- `MIGRATION_13_14`: `inbody_record` 표를 만들고 `LegacyInbodyConverter`로 옛 인바디 결과를 기록으로 옮긴 뒤 결과의 `scopeKey`를 기록 번호로, `imageFileName`을 `NULL`로 바꾼다. 한 트랜잭션이다. 처리 실패는 마이그레이션 실패(앱이 열리지 않음)가 되므로 변환은 예외를 던지지 않는 입력(빈 목록 가능)만 받는다.
- `LegacyInbodyConverter`(순수 함수): 입력은 인바디 결과 목록(`scopeKey`가 빈 문자열인 것만 대상)과 시간대, 출력은 `(기록 엔티티 목록, 바뀐 결과 목록)`이다. 기록 번호는 생성 시각 오름차순(같으면 결과 번호 오름차순)으로 1부터 매긴다. 측정일은 결과 생성 시각을 입력 시간대의 달력 날짜로 바꾼 값, 추가 시각은 결과 생성 시각이다. 사진 파일 이름은 기록으로 옮긴다.
- `BodyPlanSnapshot`: `inbodyRecords: List<InbodyRecordEntity> = emptyList()` 추가. `FORMAT_VERSION`은 그대로 1이고 `dbVersion` 14로 새 구조를 알아본다.
- `SnapshotStore.export`: 기록을 담는다. `import`: `snapshot.dbVersion < 14`이면 `LegacyInbodyConverter`로 변환한 기록과 바뀐 결과로 바꿔 넣는다. 기록 테이블도 비운 뒤 넣는다. `MIN_IMPORTABLE_DB_VERSION`은 그대로 10이다.

### 도메인 (`core:domain`)

| 항목 | before | after |
|---|---|---|
| 모델 `InbodyRecord` | 없음 | `id: Long`, `imagePath: String`, `measuredOn: LocalDate`, `createdAtMillis: Long`, `failureReason: String?` |
| 모델 `InbodyEntry` | 없음 | `record: InbodyRecord`, `result: AnalysisResult?`, `run: AnalysisRun?`. 파생 속성 `status: InbodyEntryStatus`(`NOT_ANALYZED`, `RUNNING`, `FAILED`, `ANALYZED`; 설계 `기록 상태` 순서)와 `measurement: InbodyMeasurement?` |
| 모델 `InbodyRecordWithResult` | 없음 | `record`, `result: AnalysisResult?`. 저장소가 내는 값 |
| `InbodyRecordRepository` | 없음 | `observeRecords(): Flow<List<InbodyRecordWithResult>>`, `observeRecord(id)`, `getRecords(): List<InbodyRecordWithResult>`, `insert(imageFileName, measuredOn): Long`, `setFailureReason(id, reason: String?)` |
| `AnalysisResultRepository` | `save(...)` 만 있음, 인바디는 쌓았다 | `replace(kind, scopeKey, content, measurement)` 추가(열쇠당 결과 하나로 교체). `save`는 식단·운동이 그대로 쓴다 |
| `AnalysisRunner` | `start`, `observe(kind, scopeKey)` | `observeAll(kind): Flow<List<AnalysisRun>>` 추가 |
| `AnalysisRunRequest` | `sourceUri: String?`을 가짐 | `sourceUri` 삭제. 인바디 요청은 `scopeKey`가 기록 번호다 |
| `AnalyzeInbodyUseCase` | `invoke(sourceUri, onStage)`: 사진 복사, 호출, 결과 저장(쌓기), 프로필 반영(항상) | `invoke(recordId, onStage)`: 기록의 사진으로 호출, `replace`, 프로필 반영은 설계 D9 조건에서만. 사진 복사·삭제는 하지 않는다 |
| `AddInbodyRecordUseCase` | 없음 | `invoke(sourceUri, measuredOn): Long` — 사진 복사, 측정일 보정(D29), 기록 저장, 저장 실패 시 복사본 삭제 |
| `RequestInbodyAnalysisUseCase` | 없음 | `invoke(recordId)` — AI 연결 확인(없으면 `AiCredentialMissingException`), 실패 사유 지움(D6), `AnalysisRunner.start` |
| `ObserveInbodyEntriesUseCase` | 없음 | `invoke(): Flow<List<InbodyEntry>>` — 기록·결과·작업 상태를 합치고 목록은 측정 순서 내림차순(D1) |
| `InbodyTimeline`(순수 함수 모음) | 없음 | 측정 순서 정렬, 가장 최근 측정(D2), 이전 측정(D4), `isMeasured` |
| `BuildInbodyTrendUseCase` | 없음 | `invoke(entries, period, today): InbodyTrend` — 설계 D11~D25, D32 |
| `BuildInbodyDetailUseCase` | 없음 | `invoke(entries, recordId): InbodyDetail?` — 설계 D27, D28, D31 |
| `GetProgressSummaryUseCase` | `observeHistory(INBODY)`로 최신 두 값의 차 | `InbodyRecordRepository.observeRecords()`로 설계 D33 |

- `InbodyTrend`·`InbodyDetail`·`InbodyPeriod`(`ONE_MONTH`, `THREE_MONTHS`, `SIX_MONTHS`, `ALL`)·`InbodyMetricKind`(`WEIGHT`, `SKELETAL_MUSCLE`, `BODY_FAT_PERCENT`)는 화면이 그대로 그리는 값이다. 표시값은 소수 첫째 자리 수를 십분의 일 단위 정수로 들고(`Int`), 화면이 `/ 10.0`로 적는다. 비교와 변화량 계산이 정수라 부동소수 오차가 없다.
- 계산은 `BigDecimal`(`HALF_UP`)로 한다. `Double`을 `toString()`으로 십진 문자열로 바꾸어 읽는다(설계 D17).
- 설계 처리 흐름의 실패 처리는 다음 결과가 된다. 사진 복사 실패와 기록 저장 실패는 `AddInbodyRecordUseCase`가 던지는 예외(화면이 오류 문구로 바꿈), AI 연결 없음은 `AiCredentialMissingException`, 분석 실패는 `AnalysisRunState.FAILED`와 기록의 `failureReason`이다.

### 데이터 (`core:data`)

- `InbodyRecordRepositoryImpl` 신규. `AnalysisResultRepositoryImpl.replace`는 `AnalysisResultDao.replace`를 부른다.
- `AnalysisRunnerImpl`: 작업 요청에 태그 `analysis:<종류>`를 붙이고 `observeAll(kind)`를 `getWorkInfosByTagFlow`로 낸다. `KEY_SOURCE_URI` 입력을 삭제한다.
- `AnalysisWorker`: 인바디는 `KEY_SCOPE`를 기록 번호로 읽어 `AnalyzeInbodyUseCase(recordId)`를 부른다. 실패(취소 제외)에서 `InbodyRecordRepository.setFailureReason(recordId, reason)`을 한 뒤 기존대로 알림과 `Result.failure`를 낸다. 취소는 그대로 다시 던지고 아무 것도 저장하지 않는다(D8).
- `BackupRepositoryImpl.exportTo`: 사진 목록을 `snapshot.inbodyRecords`의 `imageFileName`에서 모은다.
- `DataModule`에 `InbodyRecordRepository` 바인딩을 더한다.

### 화면 진입점 (`feature:my`)

- `:feature:my:api`: `InbodyAddNavKey`(data object), `InbodyDetailNavKey(recordId: Long)`, `BodyPlanNavigator.navigateToInbodyAdd()`, `navigateToInbodyDetail(recordId)`. 기존 `InbodyNavKey`·`navigateToInbody()`는 그대로다(알림 딥링크가 쓴다).
- `MyNavGraph.kt`: `entry<InbodyAddNavKey>`와 `entry<InbodyDetailNavKey>`를 더한다.
- 복귀 경로: 사진 추가 → 메인(`goBack`), 상세 → 메인(`goBack`), 메인 → 사진 추가·상세, AI 연결 안내는 기존 `goToAiToken`.
- `:app`: 매니페스트에 `FileProvider`(`${applicationId}.fileprovider`)와 `res/xml/file_paths.xml`(캐시 하위 `camera/`)를 더한다.

### 공용 시그니처 (`core:designsystem`)

- `Type.kt`: `Typography`에 `headlineMedium = style(24, FontWeight.Bold)`, `displaySmall = style(32, FontWeight.ExtraBold)` 추가, KDoc의 단계 설명 갱신.

## 화면 구성

| 요소 | 판정 | 대상 | 근거 |
|---|---|---|---|
| 메인·사진 추가·상세의 상단바 | 재사용 | `BodyPlanTopBar(title, onBack, actionText, onClickAction)` | 뒤로·제목·오른쪽 글자 동작이 이미 있다. 메인의 「사진 추가」가 `actionText` |
| 기간 선택 | 재사용 | `PillChip(text, selected, onClick)` 넷을 `Row`로 | 하나만 고르는 알약 칩이다. 시안의 트랙 안 세그먼트 모양과는 다르며 토큰 유지 방침으로 칩 모양을 따른다 |
| 조회 범위·측정 요약·안내 문구·섹션 제목 | 재사용 | `BaseText` + `TextTertiary`·`titleSmall`·`labelSmall` | 글자만 놓는 요소 |
| 지표 카드 | 신규 | `inbody/composable/MetricTrendCard`(`BodyPlanCard` 위에 값 헤더, 변화 `Badge`, 단일 지표 꺾은선, 눈금 셋, 날짜 셋, 마지막 점 강조) | `TrendChart` 확장은 눈금 없음 설계와 부딪친다. 시안의 값 헤더는 `displaySmall`과 `bodySmall` 단위 |
| 변화 칩·「최근 측정」 표시 | 재사용 | `Badge(text)` | 시안은 알약 모양이고 `Badge`는 모서리 6dp 사각이다. 모양 차이는 받아들인다 |
| 「인바디 사진 추가」 버튼 | 재사용 | `PrimaryButton(text, onClick)` | 높이 52dp·채움·흰 글자. 앞의 "+"는 글자로 붙인다 |
| 기록 카드 | 신규 | `inbody/composable/InbodyRecordCard`(`BodyPlanCard` 안에 썸네일, 날짜, 상태 문구, 값 셋 또는 진행 단계·실패 사유, 「상세 보기」, 「새로 분석하기」) | 현 `HistoryList` 항목은 날짜와 요약 한 줄뿐이다 |
| 「새로 분석하기」 보조 채움 버튼 | 신규 | `inbodycommon/SecondaryFilledButton(text, onClick, enabled)` | `OutlinedActionButton`은 테두리형이라 모양이 다르다. 연보라 채움은 공용에 없다 |
| 분석 중 표시 | 신규 | `InbodyRecordCard` 안의 진행 줄(진행 단계 문구 + 진행 바) | `AnalysisScreen`의 `stageMessage`는 한 줄 글자뿐이다 |
| 사진 촬영·앨범 선택 | 신규 | `inbodyadd/composable/PhotoSourceButtons` | 현 `PhotoSlot`은 앨범 하나만 연다. 촬영 경로는 코드에 없다 |
| 사진 미리보기 카드(「사진 바꾸기」 포함) | 신규 | `inbodyadd/composable/PhotoPreviewCard`(`BodyPlanCard` + `BaseImage`) | `PhotoSlot`은 `private`이고 캡션 줄이 없다. 이 화면과 함께 지운다 |
| 측정일 입력 칸과 선택창 | 신규 | `inbodyadd/composable/MeasuredDateField`, `MeasuredDatePickerDialog`(`BodyPlanCalendar` + `Dialog`) | 선례 `ComparisonDatePickerDialog`. 오늘 이후 날짜는 눌러도 고르지 않는다 |
| 안내 상자 | 신규 | `inbodycommon/NoticeBox(title, body)` | `InfoBanner`는 아이콘이 필수이고 시안의 상자는 아이콘이 없다. 사진 추가와 상세 두 곳이 쓴다 |
| 측정값 상세 카드 | 신규 | `inbodydetail/composable/InbodyMeasurementCard`(`BodyPlanCard` 안에 이전 측정 표기, 지표 행 셋, 체지방량·제지방량) | 시안 전용 구성 |
| 날짜 머리글 | 신규 | `inbodydetail/composable/InbodyDetailHeader`(`headlineMedium` 날짜) | |
| 결과지 카드와 「결과지 확대 보기」 | 신규 | `inbodydetail/composable/ResultImageCard`, `ImageViewerDialog`(전체 화면, 두 손가락 확대) | 확대는 새 동작이다 |
| 분석 내용 | 재사용 | `AnalysisResultContent(result)` (`core:ui:components`) | 요약과 세부 묶음을 카드로 각각 그린다(P27). 시안의 요약 한 상자는 그 요약 카드로 대신한다 |
| 「새로 분석하기」 전체 폭 버튼 | 재사용 | `PrimaryButton` | 새로고침 아이콘은 `BodyPlanIcons`에 없어 글자만 쓴다 |
| 분석 안 된 안내·AI 연결 안내 | 재사용 | `AiTokenRequiredDialog`(`core:ui:components`) | E1이 기존 안내를 쓴다 |

## 테스트 케이스

TDD: 적용 — 기간·축·반올림·변화량·측정 순서·변환·분석 흐름이 순수 로직이라 단위 테스트로 쓸 수 있다.

### 측정 순서와 상태 (`InbodyTimelineTest`, `InbodyEntryStatusTest` — `:core:domain`)

- [D1][P22][A1] `목록은 측정일 내림차순이고 같은 날은 나중에 추가한 기록이 위에 온다` — 기록 셋(9.01 #1, 9.01 #2, 9.08 #3) — 순서 #3, #2, #1
- [D2][P23] `가장 최근 측정은 측정 기록 중 측정 순서가 가장 나중인 기록이다` — 측정 기록 둘과 분석 전 기록(측정일 가장 늦음) — 분석 전 기록이 아닌 측정 기록
- [D3] `측정 기록이 없으면 가장 최근 측정은 없다` — 분석 전 기록만 — `null`
- [V13] `측정일이 같고 번호만 다른 두 기록에서 가장 최근 측정은 번호가 큰 기록이다` — 같은 날 #1, #2 — #2
- [D4][V12] `이전 측정은 사이에 낀 분석 전 기록을 건너뛴다` — 측정 #1, 분석 전 #2, 측정 #3 — #3의 이전은 #1
- [D4][A12][P26] `같은 측정일 두 기록에서 나중 기록의 이전 측정은 먼저 추가한 기록이다` — 같은 날 #1, #2 — #2의 이전은 #1
- [D4][E11] `앞에 측정 기록이 없으면 이전 측정은 없다` — 첫 기록 — `null`
- [D2][E3] `키만 읽힌 결과도 측정 기록이다` — 키만 있는 결과 — 측정 기록으로 센다
- [상태] `진행 중이면 분석 중이고 그 다음 실패 사유, 결과, 분석 전 순서로 정해진다` — 네 조합 — 위 순서 우선
- [D6][D31][E2] `실패 사유가 있고 이전 결과도 있으면 분석 실패 상태이며 이전 결과를 함께 가진다` — 사유 + 결과 — `FAILED`, 결과 유지

### 지표·그래프 (`BuildInbodyTrendUseCaseTest` — `:core:domain`)

- [D12][V5][P15] `1개월 전 시작일은 달에 같은 일자가 없으면 그 달 마지막 날이다` — 오늘 3.31(평년)·3.31(윤년)·8.31 6개월 — 2.28, 2.29, 2.28 또는 2.29(해당 해)
- [D13] `전체의 시작일은 측정 기록 중 가장 이른 측정일이고 측정 기록이 없으면 오늘이다` — 측정 있음·없음 — 각각
- [D14][P16][A2] `기간 시작일과 끝일에 측정한 기록은 그래프에 포함된다` — 시작일 당일 기록 — 포함
- [P14][A13] `기간을 3개월로 바꾸면 범위와 포함 기록이 바뀐다` — 2개월 전 기록 — 1개월에는 없고 3개월에는 있다
- [D15][D17][V10] `표시값은 십진 반올림이라 20.45는 20.5이고 20.449는 20.4다` — 체중 20.45, 20.449 — 205, 204(십분의 일 단위)
- [D16][V9] `체중이 0이거나 없거나 체지방량이 없으면 체지방률 값이 없다` — 세 조합 — 값 없음
- [D16][P20] `체지방률은 체지방량을 체중으로 나눈 백분율의 소수 첫째 자리 반올림이다` — 15.4kg, 72.4kg — 21.3
- [P20][A3] `체지방률 20.43과 20.38은 같은 20.4이고 같은 높이이며 변화는 변화 없음이다` — 두 기록 — 표시값 같음, 세로 위치 같음, 변화 0
- [D18][D19][A11] `최근 값은 가장 나중 기록의 표시값이고 변화량은 최근 값에서 첫 값을 뺀 값이다` — 체중 70.0, 69.0, 68.4 — 최근 684, 변화 −16
- [D19][V11][E14] `표시값이 같으면 변화량은 0이다` — 20.4, 20.4 — 0
- [D20][E6] `측정이 한 번뿐이면 점 하나와 최근 값만 있고 변화량은 없다` — 한 기록 — 변화량 `null`
- [D20][P21][V23] `한 지표의 값이 하나도 없으면 그 카드만 빠진다` — 체중만 있는 기록 — 체중 카드만
- [E5] `기록은 있으나 기간 안에 측정 기록이 없으면 카드가 없다` — 2개월 전 기록, 1개월 — 카드 없음, 기록 있음
- [E4] `기록이 하나도 없으면 빈 상태다` — 빈 목록 — 빈 상태
- [D21][V7] `시작일과 끝일이 같으면 점의 가로 위치는 0.5이고 날짜 셋은 같은 날이다` — 전체, 오늘 하나 — 0.5, 같은 날 셋
- [D21][V8] `같은 측정일의 두 점은 같은 가로 위치이고 최근 값은 나중 기록이다` — 같은 날 둘 — 같은 위치, 나중 값
- [D22][V6] `가운데 날짜는 일수 차이의 절반을 내린 날이다` — 9.01~9.30 — 9.15
- [D23][V1][D25] `표시값 범위가 0이면 축 길이는 1.0이고 점은 가운데다` — 값이 모두 70.0 — 길이 1.0, 위치 0.5
- [D23][V2] `표시값 범위가 0.2이면 축 길이는 1.0이다` — 70.0~70.2 — 1.0
- [D23][V3] `표시값 범위가 10.0이면 축 길이는 12.0이다` — 60.0~70.0 — 12.0
- [D23][V4] `축의 최솟값은 내리고 최댓값은 올려 소수 첫째 자리로 맞춘다` — 범위 1.6(68.4~70.0) — 68.2, 70.2
- [D24][D37] `세로 눈금은 최솟값·중간·최댓값 셋이고 점의 세로 위치는 축에서의 비율이다` — 위 입력 — 눈금 68.2, 69.2, 70.2
- [D32][V25] `키만 읽힌 결과는 측정 횟수와 그래프에 들어가지 않는다` — 키만 있는 기록 — 횟수·점 없음
- [D5][A10] `분석 대기 안내의 수는 결과가 없는 분석 전·분석 중·분석 실패 기록의 수다` — 사진만 추가 1, 이전 결과 있는 재분석 중 1 — 1
- [P16][P24] `분석 전 기록은 그래프에 쓰이지 않는다` — 분석 전 기록 — 점 없음
- [보여 줄 정보] `측정 요약은 기간 안 가장 최근 측정일과 측정 횟수다` — 기록 셋 — `9.30`, 3회

### 기록 상세 (`BuildInbodyDetailUseCaseTest` — `:core:domain`)

- [D27][V24] `제지방량은 체중과 체지방량의 표시값 차이다` — 68.44, 15.06 — 53.3
- [D27] `체중이나 체지방량이 없으면 제지방량이 없다` — 체지방량 없음 — `null`
- [D28][P25] `이전 대비 변화량은 표시값 차이이고 이전 값을 함께 낸다` — 이전 69.0, 지금 68.4 — −6, 이전 69.0
- [D28][E11] `이전 측정에 그 지표 값이 없으면 그 지표의 변화량만 없다` — 이전에 체지방률 없음 — 체지방률 변화 `null`
- [P26][E11] `이전 측정이 없으면 비교 정보 전체가 없다` — 첫 기록 — 이전 `null`

### 기록 추가·분석 요청 (`AddInbodyRecordUseCaseTest`, `RequestInbodyAnalysisUseCaseTest`, `AnalyzeInbodyUseCaseTest` — `:core:domain`)

- [P1][P2] `사진과 측정일로 기록이 만들어진다` — 사진 uri, 측정일 — 복사된 파일 이름과 측정일로 저장
- [P3][A8] `측정일이 오늘이면 그대로 저장한다` — 오늘 — 오늘
- [P3][D29][V21] `측정일이 오늘보다 뒤이면 오늘로 바꿔 저장한다` — 내일 — 오늘
- [P4] `같은 측정일에 기록을 둘 만들 수 있다` — 같은 날 두 번 — 기록 둘
- [E7][V20] `기록 저장이 실패하면 복사한 사진을 지운다` — 저장소가 던짐 — 사진 삭제됨, 예외 전달
- [E7] `사진 복사가 실패하면 기록을 만들지 않는다` — 복사가 던짐 — 기록 없음
- [P13][E1] `AI 연결이 없으면 요청하지 않고 연결 없음 예외를 던진다` — 연결 없음 — 작업 시작 없음
- [D6] `분석을 요청하면 그 기록의 실패 사유를 지우고 기록 번호를 열쇠로 작업을 시작한다` — 사유 있는 기록 — 사유 `null`, 열쇠 = 기록 번호
- [P10][D7] `성공하면 그 기록의 결과를 교체한다` — 이전 결과 있음 — 결과 하나, 새 값
- [D9][A5][P12] `측정일이 가장 늦은 기록이면 읽힌 키와 체중을 프로필에 반영한다` — 가장 늦은 기록 — 프로필 갱신(반올림 정수)
- [D9][A4] `측정일이 가장 늦지 않은 기록을 분석하면 프로필을 바꾸지 않는다` — 더 늦은 기록 있음 — 프로필 그대로
- [D9] `읽히지 않은 값은 프로필을 그대로 둔다` — 체중만 읽힘 — 키 유지
- [D9][E3] `읽힌 값이 하나도 없으면 프로필을 바꾸지 않는다` — 빈 측정 — 프로필 그대로
- [D10][V17] `두 기록을 차례로 분석해도 최종 프로필은 가장 최근 측정 기록의 값이다` — 늦은 기록 먼저, 이른 기록 나중 — 늦은 기록 값
- [P11][A6] `분석이 실패하면 이전 결과가 그대로 남는다` — AI가 던짐 — 결과 변화 없음
- [D7][V15] `결과 교체 저장이 실패하면 이전 결과가 그대로 남는다` — 저장소가 던짐 — 이전 결과
- [구현] `AI 호출에는 기록의 사진 경로를 쓰고 단계가 차례로 보고된다` — 정상 — 사진 경로, 단계 순서

### 목록·메인 화면 (`ObserveInbodyEntriesUseCaseTest`, `InbodyViewModelTest` — `:core:domain`, `:feature:my:impl`)

- [P7][A7] `기록마다 작업 상태가 따로이고 한 기록이 분석 중이어도 다른 기록은 분석을 요청할 수 있다` — 한 기록 `RUNNING` — 다른 기록 요청 가능
- [P9] `분석 중인 기록의 요청은 무시된다` — 같은 기록 — 요청 안 감
- [P8] `분석된 기록도 다시 분석할 수 있다` — 분석됨 — 요청 감
- [P14] `조회 기간의 처음 값은 1개월이고 고르면 바뀐다` — 선택 — 상태
- [P13][E1] `AI 연결이 없으면 AI 연결 안내가 열리고 요청하지 않는다` — 연결 없음 — 다이얼로그 상태
- [P24][E2] `실패한 기록은 실패 사유를 보이고 이전 결과가 있으면 값도 함께 보인다` — 실패 + 결과 — 상태 값
- [P24] `분석 중인 기록은 이전 결과가 있어도 값 대신 진행 단계를 보인다` — `RUNNING` + 결과 — 진행 단계 값, 값 숨김
- [D26][P23] `가장 최근 측정 기록에만 최근 측정 표시가 붙는다` — 기록 셋 — 하나만

### 사진 추가 (`InbodyAddViewModelTest` — `:feature:my:impl`)

- [P5][P6] `사진만 추가를 누르면 기록만 만들고 분석은 요청하지 않으며 화면을 닫는다` — 사진 고름 — 추가 호출, 요청 없음, 닫기 effect
- [P5] `추가하고 분석 요청을 누르면 기록을 만든 뒤 분석을 요청한다` — 사진 고름 — 추가 후 요청
- [E1][P13][V22] `AI 연결이 없으면 추가하고 분석 요청은 기록도 사진 복사도 만들지 않고 선택을 유지한다` — 연결 없음 — 추가 호출 없음, 안내 다이얼로그, 선택 유지
- [P13] `사진만 추가는 AI 연결이 없어도 동작한다` — 연결 없음 — 추가됨
- [E15][D30] `사진을 고르기 전에는 두 버튼이 눌리지 않는다` — 초기 상태 — 비활성
- [E7] `저장이 실패하면 오류 문구가 보이고 고른 사진과 측정일이 남는다` — 추가 예외 — 오류, 선택 유지
- [E16][D38] `기록을 만든 뒤 분석 요청이 실패하면 기록은 남고 오류를 알린 뒤 닫는다` — 요청 예외 — 기록 남음, 오류, 닫기
- [P2][P3] `측정일의 처음 값은 오늘이고 오늘 이후 날짜는 고를 수 없다` — 내일 고르기 — 상태 변화 없음

### 기록 상세 (`InbodyDetailViewModelTest` — `:feature:my:impl`)

- [P25][D31] `결과가 있으면 상태와 상관없이 측정값과 분석 내용을 보인다` — 분석 중 + 결과 — 값 있음
- [D31][P9] `분석 중이면 새로 분석하기가 눌리지 않는다` — `RUNNING` — 비활성
- [E12] `분석 전 기록은 값 없음과 분석 요청 안내를 보인다` — 분석 전 — 상태 값
- [P8] `새로 분석하기를 누르면 그 기록의 분석을 요청한다` — 분석됨 — 요청 감
- [E1] `AI 연결이 없으면 AI 연결 안내가 열린다` — 연결 없음 — 다이얼로그

### 진행 요약·마이 홈 (`GetProgressSummaryUseCaseTest`, `MyViewModelTest`)

- [D33][V26][P28] `골격근량 변화는 그 지표 값이 있는 마지막 두 기록의 차이다` — 값 있음, 없음, 있음 — 첫째와 셋째의 차
- [D33] `값이 있는 기록이 둘 미만이면 변화가 없다` — 하나 — `null`
- [D34][P28] `프로필 카드의 인바디 값은 가장 최근 측정 기록의 값이다` — 측정일 늦은 기록을 먼저 분석함 — 측정일 가장 늦은 기록 값

### 저장·변환 (`LegacyInbodyConverterTest` — `:core:local`, `InbodyRecordMapperTest` — `:core:data`)

- [P30][V18] `옛 인바디 결과 셋은 기록 셋이 되고 번호는 생성 시각 순서로 1부터 매겨진다` — 결과 셋 — 기록 셋, 번호 1~3
- [P30][D35] `측정일은 결과 생성 시각의 주어진 시간대 날짜이고 추가 시각은 생성 시각이다` — 자정 직전·직후 시각, 두 시간대 — 날짜 각각
- [V18] `변환된 결과는 요약·묶음·값·시각이 같고 열쇠가 기록 번호이며 사진 파일 이름은 비워진다` — 결과 — 필드 비교
- [V18] `이미 기록 번호가 열쇠인 결과는 다시 변환하지 않는다` — 변환 두 번 — 기록 중복 없음
- [V18] `인바디가 아닌 결과는 건드리지 않는다` — 식단 결과 — 그대로
- [V19] `옛 스냅샷(dbVersion 13)은 변환해 기록을 만들고 새 스냅샷(dbVersion 14)은 그대로 들인다` — 두 스냅샷 — 각각
- [구현] `기록 엔티티와 도메인 모델이 서로 바뀌고 측정일은 epochDay와 날짜로 오간다` — 기록 — 왕복 동일

### 수동 확인 (확인 주체: 사용자)

- [시안] 수동 확인: 메인 화면(기간 칩, 지표 카드 셋, 사진 기록 목록)이 시안과 맞는지, 높이와 간격이 과하지 않은지 본다.
- [P1][E8][E9][E15] 수동 확인: 사진 추가에서 앨범 선택과 촬영 모두 미리보기가 나오는지, 선택을 취소해도 상태가 그대로인지, 촬영을 취소하면 아무 것도 바뀌지 않는지 본다.
- [P25][P27] 수동 확인: 상세에서 결과지 확대(두 손가락 확대)가 되는지, 분석 내용의 요약과 세부 묶음이 각각 보이는지 본다.
- [E10] 수동 확인: 분석 중에 화면을 나갔다 돌아오면 진행 상태가 보이고, 끝나면 알림이 오며 알림을 누르면 메인이 열리는지 본다.
- [A9][P30] 수동 확인: 이전 빌드로 인바디를 분석해 둔 기기에 새 빌드를 덮어 설치했을 때 기록과 그래프가 그대로 보이고 측정일이 분석한 날인지 본다.
- [P29] 수동 확인: 백업 후 복원했을 때 사진과 측정일과 분석 결과가 돌아오는지 본다.
- [E13] 수동 확인: 기록의 사진 파일을 지운 뒤 사진 자리에 자리표시가 보이는지 본다.
- [D23] 수동 확인: 세로축 눈금이 시안(체중 68.0~71.0 등)과 비슷한 느낌인지 보고 여유 비율을 정한다.
- [E4][E5][E11][E12] 수동 확인: 시안에 없는 안내 문구(빈 상태, 기간 안 측정 없음, 이전 기록 없음, 분석 전 안내)와 오류·진행 단계 문구가 어색하지 않은지 본다.

### 리뷰 확인

- [D8][V14] 리뷰 확인: `AnalysisWorker`가 `CancellationException`에서 `setFailureReason`과 알림을 하기 전에 다시 던지는지.
- [P9][V16] 리뷰 확인: 인바디 분석 요청이 기록 번호를 작업 열쇠로 쓰고 `ExistingWorkPolicy.KEEP`을 유지하는지.
- [D7][V15] 리뷰 확인: 결과 교체가 `AnalysisResultDao`의 `@Transaction` 한 번으로 이뤄지는지.
- [P29] 리뷰 확인: `exportTo`의 사진 목록이 `inbodyRecords`에서 오고, 옛 `analysisResults.imageFileName`에 기대지 않는지.
- [구현] 리뷰 확인: `Type.kt` 추가가 기존 화면의 글자를 바꾸지 않는지, 새 화면이 색을 하드코딩하지 않는지.

## 작업 순서

### 1단계 — 저장소와 변환 (`core:local`)

| 경로 | 신규·수정 | 할 일 |
|---|---|---|
| `core/local/.../entity/InbodyRecordEntity.kt` | 신규 | 엔티티 |
| `core/local/.../dao/InbodyRecordDao.kt` | 신규 | DAO |
| `core/local/.../dao/AnalysisResultDao.kt` | 수정 | `deleteByScope`, `@Transaction replace` |
| `core/local/.../migration/LegacyInbodyConverter.kt` | 신규 | 순수 변환 함수 |
| `core/local/.../migration/Migrations.kt` | 수정 | `MIGRATION_13_14` |
| `core/local/.../BodyPlanDatabase.kt` | 수정 | 버전 14, 엔티티·DAO 등록 |
| `core/local/.../di/LocalModule.kt` | 수정 | DAO 제공, 마이그레이션 등록 |
| `core/local/.../snapshot/BodyPlanSnapshot.kt`, `SnapshotStore.kt` | 수정 | 기록 담기, 옛 스냅샷 변환 |
| `core/local/src/test/.../LegacyInbodyConverterTest.kt` | 신규 | 변환 테스트. `core:local`에 `src/test`를 처음 만든다. 컨벤션 플러그인이 junit을 주는지 확인하고 부족하면 `libs.versions.toml` 별칭으로 더한다 |

검증: `./gradlew :core:local:testDebugUnitTest`. 커밋: 이 단계 끝.

### 2단계 — 도메인

| 경로 | 신규·수정 | 할 일 |
|---|---|---|
| `core/domain/.../model/InbodyRecord.kt`, `InbodyEntry.kt`, `InbodyTrend.kt`, `InbodyDetail.kt` | 신규 | 모델과 열거형 |
| `core/domain/.../repository/InbodyRecordRepository.kt` | 신규 | 인터페이스 |
| `core/domain/.../repository/AnalysisResultRepository.kt` | 수정 | `replace` |
| `core/domain/.../repository/AnalysisRunner.kt` | 수정 | `observeAll` |
| `core/domain/.../model/AnalysisRun.kt` | 수정 | `AnalysisRunRequest.sourceUri` 삭제 |
| `core/domain/.../usecase/InbodyTimeline.kt`, `BuildInbodyTrendUseCase.kt`, `BuildInbodyDetailUseCase.kt` | 신규 | 순수 계산 |
| `core/domain/.../usecase/AddInbodyRecordUseCase.kt`, `RequestInbodyAnalysisUseCase.kt`, `ObserveInbodyEntriesUseCase.kt` | 신규 | 흐름 |
| `core/domain/.../usecase/AnalyzeInbodyUseCase.kt` | 수정 | 기록 번호 기반으로 교체 |
| `core/domain/.../usecase/GetProgressSummaryUseCase.kt` | 수정 | 설계 D33 |
| `core/domain/src/test/...` | 신규·수정 | 위 테스트 케이스. 기존 `AnalyzeInbodyUseCaseTest`·`GetProgressSummaryUseCaseTest`와 식단·운동 분석 테스트의 인터페이스 대역(`AnalysisResultRepository`·`AnalysisRunner`)을 새 메서드에 맞춘다 |
| 다른 모듈의 대역 | 수정 | `feature/dietlog/impl`, `feature/workoutlog/impl`, `feature/my/impl`의 `FakeAnalysisResultRepository`·`FakeAnalysisRunner`, `HomeViewModelTest` 쪽 대역에 새 메서드를 더한다(컴파일이 깨지지 않게) |

검증: `./gradlew :core:domain:test`. 커밋: 이 단계 끝.

### 3단계 — 데이터

| 경로 | 신규·수정 | 할 일 |
|---|---|---|
| `core/data/.../repository/InbodyRecordRepositoryImpl.kt` | 신규 | 구현 |
| `core/data/.../mapper/InbodyRecordMapper.kt` | 신규 | Entity ↔ 도메인 |
| `core/data/.../repository/AnalysisResultRepositoryImpl.kt` | 수정 | `replace` |
| `core/data/.../work/AnalysisRunnerImpl.kt`, `AnalysisWork.kt`, `AnalysisWorker.kt` | 수정 | 태그·`observeAll`, 입력에서 사진 삭제, 인바디 기록 번호, 실패 사유 저장 |
| `core/data/.../repository/BackupRepositoryImpl.kt` | 수정 | 사진 목록을 기록에서 |
| `core/data/.../di/DataModule.kt` | 수정 | 바인딩 |
| `core/data/src/test/.../mapper/InbodyRecordMapperTest.kt` | 신규 | 매퍼 테스트 |

검증: `./gradlew :core:data:testDebugUnitTest`. 커밋: 이 단계 끝.

### 4단계 — 화면

| 경로 | 신규·수정 | 할 일 |
|---|---|---|
| `core/designsystem/.../theme/Type.kt` | 수정 | 두 단계 추가 |
| `feature/my/api/.../MyNavKey.kt` | 수정 | 두 NavKey와 navigate 함수 |
| `feature/my/impl/.../MyNavGraph.kt` | 수정 | 엔트리 둘 |
| `feature/my/impl/.../inbodycommon/` (`NoticeBox`, `SecondaryFilledButton`, `InbodyFormat`) | 신규 | 공유 조각 |
| `feature/my/impl/.../inbody/` (`InbodyContracts`, `InbodyViewModel`, `composable/InbodyView`, `MetricTrendCard`, `InbodyRecordCard`) | 교체 | 메인 화면. `InbodyTrendChart.kt`는 삭제 |
| `feature/my/impl/.../inbodyadd/` (`InbodyAddContracts`, `InbodyAddViewModel`, `composable/…`) | 신규 | 사진 추가 화면 |
| `feature/my/impl/.../inbodydetail/` (`InbodyDetailContracts`, `InbodyDetailViewModel`, `composable/…`) | 신규 | 기록 상세 화면 |
| `feature/my/impl/.../home/MyViewModel.kt` | 수정 | 설계 D34 |
| `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/file_paths.xml` | 수정·신규 | `FileProvider` |
| `feature/my/impl/src/test/...` | 신규·수정 | 위 ViewModel 테스트. 기존 `InbodyViewModelTest`·`MyViewModelTest`를 새 구조에 맞춰 다시 쓴다 |

검증: `./gradlew :feature:my:impl:testDebugUnitTest`, `./gradlew :app:compileDebugKotlin`. 커밋: 이 단계 끝.

### 5단계 — 전체 확인

`./gradlew :core:domain:test :core:data:testDebugUnitTest :core:local:testDebugUnitTest :feature:my:impl:testDebugUnitTest :feature:dietlog:impl:testDebugUnitTest :feature:workoutlog:impl:testDebugUnitTest :feature:home:impl:testDebugUnitTest :app:assembleDebug`를 돌려 인터페이스 변경이 다른 모듈을 깨지 않았는지 본다. 이어서 위 `수동 확인`을 사용자에게 넘긴다. 이 단계는 코드 변경이 없고 필요하면 앞 단계로 되돌아가 고친다.

## 회귀 대상

| 고치는 것 | 확인할 기존 동작 |
|---|---|
| `AnalysisResultRepository`(`replace` 추가) | 구현체 `AnalysisResultRepositoryImpl`, 대역 `FakeAnalysisResultRepository`(`feature/my`·`dietlog`·`workoutlog`), 도메인 테스트의 인라인 대역(`AnalyzeInbodyUseCaseTest`·`AnalyzeWorkoutUseCaseTest`·`AnalyzeDietUseCaseTest`·`GetProgressSummaryUseCaseTest`), `HomeViewModelTest`의 대역을 모두 새 메서드에 맞춘다 |
| `AnalysisRunner`(`observeAll` 추가) | 구현체 `AnalysisRunnerImpl`, 대역 `FakeAnalysisRunner`(`feature/my`·`dietlog`·`workoutlog`) |
| `AnalysisRunRequest.sourceUri` 삭제 | 식단·운동 기간 분석 요청이 이 필드를 쓰지 않는지(`DietAnalysisViewModel`·`WorkoutAnalysisViewModel`), `AnalysisRunnerImpl`·`AnalysisWorker`의 입력 키 |
| `AnalysisWorker`의 인바디 입력 변경 | 식단·운동 기간 분석이 같은 워커로 정상 동작하는지(실패 사유·알림 포함) |
| `BodyPlanDatabase` 버전 14와 마이그레이션 | 이전 버전(13)에서 올린 기기의 모든 표, 식단·운동 기록과 식단 사진 |
| `BodyPlanSnapshot`·`SnapshotStore` | 옛 백업(dbVersion 10~13) 복원이 인바디 외 표에서 그대로 되는지, 백업 파일의 사진 디렉터리 |
| `GetProgressSummaryUseCase` | 홈의 진행 요약에서 체중·운동 지표가 그대로이고 골격근량·체지방량 변화만 설계 D33으로 바뀌는지 |
| `MyViewModel`·`ProfileSummaryCard` | 마이 홈 프로필 카드의 체지방률·골격근량 표시와 프로필 일치 판정(`profileFromInbody`) |
| `Type.kt` | `MaterialTheme.typography` 기존 아홉 단계의 값이 그대로인지 |
| `AnalysisDeepLink`(`AnalysisKind.INBODY → InbodyNavKey`) | 인바디 분석 완료 알림을 누르면 새 메인이 열리는지 |
| 삭제하는 `InbodyTrendChart.kt`·`PhotoSlot`·`HistoryList` | 다른 곳에서 참조하지 않는지(grep으로 확인했을 때 `InbodyView.kt` 안에서만 쓰임) |

## 고려한 대안

- 기록과 결과를 새 테이블 둘로 나누는 안: 결과 저장·요약·알림·백업·식단/운동과 공유하는 `AnalysisResult` 경로를 모두 다시 만들어야 하고, 이전 때 결과 행도 옮겨야 한다. 기존 결과 테이블에 기록 번호를 열쇠로 붙이는 안을 택했다.
- 결과의 `imageFileName` 컬럼을 지우는 안: Room은 컬럼 삭제에 테이블 재생성이 필요하고 다른 분석 종류도 같은 컬럼을 쓴다. 남기고 인바디에서 `NULL`로 둔다.
- `TrendChart`에 눈금·날짜 라벨·범례 끄기 파라미터를 더하는 안: 눈금 숫자를 그리지 않는다는 기존 설계와 부딪치고 분기가 커진다. 단일 지표 전용 컴포넌트를 `feature/my/impl`에 새로 만든다. 신체 변화 화면이 목표가 아니라 `core`로 올리지 않는다.
- 인바디 메인을 `AnalysisScreen` 안에 억지로 끼우는 안: 하단 고정 버튼과 단일 결과 전제 때문에 시안의 기록별 버튼 구조를 담을 수 없고 식단·운동 화면이 함께 쓰는 껍데기를 건드리게 된다. 독립 화면으로 만든다.
- 변화 칩을 알약 모양의 새 컴포넌트로 만드는 안: 시안과 더 가깝지만 `Badge`로 충분하고 모양 하나 때문에 공용 컴포넌트를 늘리게 된다. `Badge`를 쓰고 모서리 차이를 받아들인다.
- 촬영에 `CAMERA` 권한과 CameraX를 쓰는 안: 권한 흐름과 의존이 늘어난다. 인텐트 방식 `TakePicture`가 권한 없이 동작한다.
- 앱 팔레트를 시안 인디고로 바꾸는 안: 사용자가 토큰 유지(A)를 골랐다.

---
자기 대조(plan): 미해결 있음
- 고침: 구현 계약, 화면 구성, 테스트 케이스, 회귀 대상
- 미해결 구현 계약: `core:local`에 테스트 소스 세트가 처음 생긴다 — 컨벤션 플러그인이 junit을 주는지 확인되지 않음(`core:data`가 같은 컨벤션이라 줄 가능성이 높음). 1단계에서 확인하고 부족하면 `libs.versions.toml` 별칭으로 더한다
- 미해결 화면 구성: 시안의 「최근 측정」과 변화 칩은 알약 모양이고 `Badge`는 사각이다 — 시안과 모양이 다름(받아들이기로 했으나 사용자 확인 필요)
