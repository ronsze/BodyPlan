# 홈 화면 진척 요약

**작성일**: 2026-09-10

## 설계

### 요약

`:feature:home` 모듈을 새로 만들고 하단 탭 맨 앞에 홈을 둔다.
홈은 최근 7일과 그 앞 7일을 견줘 체중·무게 볼륨·운동한 날이 어느 쪽으로 움직였는지 보이고, 인바디 최근 두 번의 골격근량·체지방량 차이를 따로 보인다.
개선·악화 판정은 목표(`UserProfile.goals`·`targetWeightKg`)가 있으면 목표 기준으로, 없으면 고정 기준으로 한다 — 사용자가 정했다.

### 배경

기록은 쌓이는데 되먹임이 없다. 운동일지·식단일지·마이 어디에도 "지금 나아지고 있는가"에 답하는 자리가 없고, 세트별 무게·횟수를 다 받아 두고도 집계해 보여주는 화면이 없다.
홈 화면 자체가 없어(탭은 운동 일지·식단 일지·마이 셋뿐) 요약을 얹을 자리를 새로 만든다 — 사용자가 「홈 탭 신설」을 골랐다.

기간 범위로 세트까지 읽는 조회가 지금 없다. `WorkoutEntryDao.observeInRange`는 `DISTINCT dateEpochDay, bodyPart`만 내므로 볼륨을 셀 수 없어 조회를 새로 더한다.

### 수용 조건

- [ ] 앱을 열면 하단 탭 맨 앞이 「홈」이고, 홈이 첫 화면으로 뜬다. 기존 세 탭은 순서를 유지한 채 뒤로 밀린다.
- [ ] 최근 7일과 그 앞 7일에 체중 기록이 모두 있으면 「체중」 행에 두 구간 평균의 차이가 `%+.1fkg`로 뜬다.
- [ ] 최근 7일과 그 앞 7일에 무게 종목 기록이 있으면 「무게 볼륨」 행에 두 구간 볼륨의 차이가 `%+,dkg`로 뜬다.
- [ ] 「운동한 날」 행에 최근 7일의 기록된 날 수에서 그 앞 7일의 날 수를 뺀 값이 `%+d일`로 뜬다.
- [ ] 감량 목표(`Goal.DIET`)면 체중 감소가 개선 색으로, 증량 목표(`Goal.MUSCLE_GAIN`)면 체중 증가가 개선 색으로 뜬다.
- [ ] 목표가 하나도 없으면 「체중」 행은 값만 뜨고 개선·악화 색이 붙지 않는다.
- [ ] 측정값이 있는 인바디 결과가 2건 이상이면 「골격근량」·「체지방량」 행에 최근 두 건의 차이가 `%+.1fkg`로 뜨고, 골격근 증가·체지방 감소가 개선 색으로 뜬다.
- [ ] 판정된 지표 중 개선이 더 많으면 헤드라인이 `잘 가고 있어요`, 악화가 더 많으면 `흐름이 처졌어요`, 같으면 `큰 변화가 없어요`로 뜬다.
- [ ] 빈 결과: 판정된 지표가 하나도 없으면 헤드라인이 `아직 견줄 기록이 부족해요`가 되고, 값이 없는 행은 `-`로 뜬다.
- [ ] 조회 실패: 구독 중 하나라도 실패하면 `불러오지 못했습니다`와 `다시 시도`가 뜨고, `다시 시도`로 재구독된다.
- [ ] 저장 실패: 해당 없음 — 홈은 아무것도 쓰지 않는다.
- [ ] 권한: 해당 없음.

### 비목표

- 홈에서 다른 화면으로 들어가는 경로를 만들지 않는다. 카드는 눌리지 않는다.
- 각도(`ANGLE`)·시간(`DURATION`) 종목을 볼륨에 넣지 않는다. 각도×횟수는 뜻이 없고 시간은 단위가 달라, 이번 요약은 무게 볼륨과 운동한 날만 센다.
- 그래프·스파크라인을 그리지 않는다. 숫자와 방향 표시만 둔다.
- 식단 기록을 지표에 넣지 않는다 — 사진 유무뿐이라 개선·악화를 판정할 수 없다.
- AI 분석 결과(`AnalysisContent`)의 글을 홈에 옮기지 않는다. 인바디는 측정 숫자만 쓴다.
- 종목별 성과 추이 화면은 이번에 만들지 않는다. `SummarizeWorkoutVolumeUseCase`를 그 화면이 재사용할 수 있게 순수 함수로 두는 데까지만 한다.
- `feature/my/impl/weight/WeightFormat.kt`를 공용으로 올리지 않는다 — 홈이 쓰는 표기는 `%+.1fkg` 하나뿐이라 홈 모듈에 따로 둔다.
- `MyView`의 기존 카드·섹션을 손대지 않는다.

### 데이터 계약

**조회 `WorkoutEntryDao` (수정, 스키마 변경 없음)**

- `@Transaction @Query("SELECT * FROM workout_entry WHERE dateEpochDay BETWEEN :from AND :to")`
  `fun observeWithSetsInRange(from: Long, to: Long): Flow<List<WorkoutEntryWithSets>>`

기존 `observeInRange`(`DISTINCT dateEpochDay, bodyPart`)는 캘린더가 계속 쓰므로 그대로 둔다.

**매핑 `WorkoutMapper.kt` (수정)**

- `internal fun List<WorkoutEntryWithSets>.toEntriesByDate(): Map<LocalDate, List<WorkoutEntry>>`
  — `groupBy { LocalDate.ofEpochDay(it.entry.dateEpochDay) }` + 기존 `WorkoutEntryWithSets.toDomain()`. 세트 정렬은 `toDomain()`이 `setNumber`로 이미 한다.

| `WorkoutEntryWithSets` 필드 | 귀착지 |
|---|---|
| `entry.dateEpochDay` | `Map`의 키 `LocalDate` → 운동한 날 수 |
| `entry.intensityType` | `WorkoutEntry.intensityType` → 볼륨 합산 대상 판정(`WEIGHT`만) |
| `entry.id`·`exerciseId`·`exerciseName`·`bodyPart` | `WorkoutEntry`의 같은 필드. 홈 요약에서는 **미사용** — 기존 `toDomain()`을 그대로 쓰므로 함께 실려 온다 |
| `entry.createdAtMillis` | **미사용** |
| `sets[].repeatCount` | `WorkoutSet.repeatCount` → 볼륨 = `intensity.value * repeatCount` |
| `sets[].intensityValue` | `WorkoutSet.intensity.value` → 볼륨 |
| `sets[].setNumber` | `toDomain()`의 정렬 키. 홈 요약에서는 **미사용** |
| `sets[].id`·`entryId` | **미사용** |

**기존 조회 재사용 (신규 없음)**

| 심볼 | 시그니처 | 홈이 쓰는 범위 |
|---|---|---|
| `WeightLogRepository.observeRecordsInRange` | `(from: LocalDate, to: LocalDate): Flow<List<WeightRecord>>` | `today.minusDays(13)` ~ `today` |
| `UserProfileRepository.observeProfile` | `(): Flow<UserProfile>` | 전체 |
| `AnalysisResultRepository.observeHistory` | `(kind: AnalysisKind): Flow<List<AnalysisResult>>` | `AnalysisKind.INBODY`. 최신순 전체를 받아 `measurement != null`인 앞의 2건만 쓴다 |

`AnalysisResult`에서 쓰는 필드는 `measurement`(`InbodyMeasurement`)뿐이다. `id`·`kind`·`scopeKey`·`content`·`createdAtMillis`·`imagePath`는 **미사용**.
`InbodyMeasurement`에서 쓰는 필드는 `skeletalMuscleKg`·`bodyFatKg`뿐이다. `weightKg`·`heightCm`은 **미사용** — 체중은 사람이 손수 넣은 `WeightRecord`만 쓴다(추정값과 섞지 않는다는 기존 판단).

**신규 도메인 모델 `core/domain/model/ProgressSummary.kt`**

```kotlin
enum class ProgressMetricKey { WEIGHT, WORKOUT_VOLUME, WORKOUT_DAYS, SKELETAL_MUSCLE, BODY_FAT }
enum class ProgressDirection { IMPROVING, WORSENING, STEADY, UNKNOWN }
enum class ProgressHeadline { IMPROVING, WORSENING, STEADY, NOT_ENOUGH_DATA }

data class ProgressMetric(
    val key: ProgressMetricKey,
    val changeValue: Double?,
    val direction: ProgressDirection,
)

data class ProgressSummary(
    val headline: ProgressHeadline = ProgressHeadline.NOT_ENOUGH_DATA,
    val recentMetrics: List<ProgressMetric> = emptyList(),
    val bodyCompositionMetrics: List<ProgressMetric> = emptyList(),
)
```

- `changeValue`가 `null`이면 견줄 구간이 비었다는 뜻이다 — `0.0`이 아니다(`WeightTrend`와 같은 판단).
- `changeValue == null`이면 `direction`은 반드시 `UNKNOWN`이다.
- `WORKOUT_VOLUME`·`WORKOUT_DAYS`도 `Double`에 담는다. 표기 시 정수로 내린다 — 필드를 키마다 나누면 화면이 키별로 분기해야 한다.

**신규 도메인 모델 `core/domain/model/WorkoutVolume.kt`**

```kotlin
data class WorkoutVolume(val weightVolumeKg: Int, val workoutDays: Int)
```

### 상태 계약

**UseCase `SummarizeWorkoutVolumeUseCase` (`:core:domain`, 순수)**

- `operator fun invoke(entriesByDate: Map<LocalDate, List<WorkoutEntry>>): WorkoutVolume`
- `weightVolumeKg` = 모든 날짜의 모든 기록 중 `intensityType == IntensityType.WEIGHT`인 세트의 `intensity.value * repeatCount` 합.
- `workoutDays` = 값이 빈 목록이 아닌 날짜 수.
- 저장소를 구독하지 않고 이미 읽은 값을 받는다 — `GetWeightTrendUseCase`와 같은 짜임새이며, 종목별 성과 추이 화면이 같은 함수를 다시 쓸 수 있게 하려는 것이다.

**UseCase `GetProgressSummaryUseCase` (`:core:domain`)**

- 생성자 주입: `weightLogRepository: WeightLogRepository`, `workoutLogRepository: WorkoutLogRepository`, `userProfileRepository: UserProfileRepository`, `analysisResultRepository: AnalysisResultRepository`, `getWeightTrend: GetWeightTrendUseCase`, `summarizeWorkoutVolume: SummarizeWorkoutVolumeUseCase`, `clock: Clock`
- `operator fun invoke(): Flow<ProgressSummary>`
- `today = LocalDate.now(clock)`를 한 번만 읽고 모든 판정이 그 값을 쓴다(`GetMonthlyDayStatusUseCase`와 같은 판단).
- 구간: 최근 = `today.minusDays(6)`~`today`, 그 앞 = `today.minusDays(13)`~`today.minusDays(7)`.
- `combine`으로 네 스트림을 묶는다. 어느 하나가 던지면 그대로 위로 던진다 — 화면이 잡는다.

지표별 산출:

| 키 | `changeValue` | `UNKNOWN`이 되는 조건 |
|---|---|---|
| `WEIGHT` | `getWeightTrend(records, today).weeklyAverageChangeKg` | 두 구간 중 한쪽이라도 체중 기록이 없어 `null`일 때 |
| `WORKOUT_VOLUME` | 최근 구간 `weightVolumeKg` − 앞 구간 `weightVolumeKg` (`Double`로 올림) | 두 구간 모두 무게 종목 기록이 0건일 때 |
| `WORKOUT_DAYS` | 최근 구간 `workoutDays` − 앞 구간 `workoutDays` (`Double`로 올림) | 두 구간 모두 기록이 0일일 때 |
| `SKELETAL_MUSCLE` | 최신 `skeletalMuscleKg` − 그 이전 `skeletalMuscleKg` | 그 값이 있는 인바디 결과가 2건 미만일 때 |
| `BODY_FAT` | 최신 `bodyFatKg` − 그 이전 `bodyFatKg` | 그 값이 있는 인바디 결과가 2건 미만일 때 |

`recentMetrics`는 `WEIGHT`, `WORKOUT_VOLUME`, `WORKOUT_DAYS` 순서, `bodyCompositionMetrics`는 `SKELETAL_MUSCLE`, `BODY_FAT` 순서로 늘 5개를 다 담는다 — 값이 없는 행도 `-`로 보여야 하므로 빼지 않는다.

방향 판정:

- `changeValue == null` → `UNKNOWN`.
- `changeValue`의 절대값이 그 키의 문턱 미만 → `STEADY`. 문턱은 `WEIGHT`·`SKELETAL_MUSCLE`·`BODY_FAT` = `0.1`, `WORKOUT_VOLUME` = `1.0`, `WORKOUT_DAYS` = `1.0`.
- `WORKOUT_VOLUME`·`WORKOUT_DAYS`: 늘면 `IMPROVING`, 줄면 `WORSENING`.
- `SKELETAL_MUSCLE`: 늘면 `IMPROVING`. `BODY_FAT`: 줄면 `IMPROVING`.
- `WEIGHT`: 목표에 따라 갈린다 — 사용자가 「목표 있으면 목표 기준, 없으면 고정 기준」을 골랐고, 체중은 고정 기준으로 방향을 정할 수 없어 목표가 없으면 판정하지 않는다.

| 조건 | 판정 |
|---|---|
| `goals`에 `Goal.TARGET_WEIGHT`가 있고 `targetWeightKg != null`이고 최근 구간에 체중 기록이 있음 | 최근 평균이 목표에 가까워졌으면 `IMPROVING`, 멀어졌으면 `WORSENING` |
| `goals`에 `Goal.DIET`가 있음 | 줄면 `IMPROVING` |
| `goals`에 `Goal.MUSCLE_GAIN`이 있음 | 늘면 `IMPROVING` |
| 위 어느 것도 아님(`goals`가 비었거나 `Goal.TARGET_STRENGTH`만 있음) | `STEADY` — 값은 보이되 개선·악화 색을 붙이지 않는다 |

`DIET`와 `MUSCLE_GAIN`을 함께 골랐으면 `TARGET_WEIGHT` → `DIET` → `MUSCLE_GAIN` 순으로 먼저 걸리는 것을 쓴다.

헤드라인:

- 다섯 지표 전부 `UNKNOWN` → `NOT_ENOUGH_DATA`.
- `IMPROVING` 개수 > `WORSENING` 개수 → `IMPROVING`. 반대면 `WORSENING`. 같으면 `STEADY`.

**화면: 홈 (`HomeState` / `HomeIntent` / `HomeEffect`)**

| 필드 | 타입 | 초기값 | 출처 |
|---|---|---|---|
| `summary` | `ProgressSummary?` | `null` | `GetProgressSummaryUseCase` |
| `isLoading` | `Boolean` | `false` | 구독 시작 시 `true`, 첫 방출·실패에 `false` |
| `errorMessage` | `String?` | `null` | 구독 실패 시 `LOAD_ERROR` |

- `HomeIntent`: `data object ClickRetry` — 구독을 취소하고 다시 연다(`WorkoutLogViewModel.observeLog`와 같은 짜임새).
- `HomeEffect`: 멤버 없는 sealed interface. 홈은 이동도 토스트도 없다 — `BaseViewModel<S, I, E>`가 타입을 요구하므로 선언만 둔다.
- 초기 로드는 `initializeData()`에서 구독을 연다.
- 실패: `catch`로 받아 `isLoading = false`, `errorMessage = LOAD_ERROR`. 문구 상수 `LOAD_ERROR = "불러오지 못했습니다"`.

### 화면 구성

**홈 (`HomeViewImpl`)**

```
Column(fillMaxSize + background(Background))
├ BodyPlanTopBar(title = "홈")                       (재사용, onBack 없음)
└ Box(weight 1f)
   ├ errorMessage != null → ErrorContent             (신규 — 홈 모듈 내 private)
   ├ isLoading && summary == null → LoadingContent   (신규 — 홈 모듈 내 private)
   └ else → Column(verticalScroll, padding 16.dp, spacedBy 12.dp)
        ├ ProgressCard(...)                          (신규)
        └ BodyCompositionCard(...)                   (신규)
```

재사용하는 심볼(전부 파일을 열어 시그니처를 확인함):

| 심볼 | 시그니처 | 판정 |
|---|---|---|
| `BodyPlanTopBar` | `(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actionText: String? = null, onClickAction: () -> Unit = {})` | 재사용 — `title`만 넘긴다 |
| `BodyPlanCard` | `(modifier: Modifier = Modifier, cornerRadius: Dp = 16.dp, contentPadding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit)` | 재사용 |
| `BaseText` | `(text: String, modifier: Modifier = Modifier, color: Color, style: TextStyle, textAlign, maxLines, overflow)` — `text`·`style`·`color`·`modifier`만 쓴다 | 재사용 |
| `VerticalSpacer` | `(space: Dp)` | 재사용 |
| `RowScope.WeightSpacer` | `(weight: Float = 1f)` | 재사용 |
| `OutlinedActionButton` | `(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)` | 재사용 — `다시 시도` |
| `CircularProgressIndicator` | Material3 기본 | 재사용 — 로딩 |
| `Accent`·`Danger`·`TextPrimary`·`TextSecondary`·`TextTertiary`·`Background` | 색 상수 | 재사용 |
| `BodyPlanIcons` | `object` — `Dumbbell`·`ForkKnifeCrossed`·`User` 등 `Painter` 프로퍼티 | 확장 — `House`를 더한다(아래) |

**신규 컴포저블**

```kotlin
// home/composable/ProgressCard.kt
@Composable
internal fun ProgressCard(
    headline: ProgressHeadline,
    metrics: List<ProgressMetric>,
    modifier: Modifier = Modifier,
)

// home/composable/BodyCompositionCard.kt
@Composable
internal fun BodyCompositionCard(metrics: List<ProgressMetric>, modifier: Modifier = Modifier)

// home/composable/MetricRow.kt — 두 카드가 함께 쓰는 라벨-값 행
@Composable
internal fun MetricRow(metric: ProgressMetric, modifier: Modifier = Modifier)
```

`MetricRow`의 표기는 기존 관용구를 그대로 따른다 — `Row(fillMaxWidth) { 라벨(bodyMedium, TextTertiary); WeightSpacer(); 값(bodyMedium, 판정색) }`.

**신규 표기 함수 `home/ProgressFormat.kt`**

- `internal fun changeText(metric: ProgressMetric): String` — `null`이면 `"-"`. `WEIGHT`·`SKELETAL_MUSCLE`·`BODY_FAT`는 `"%+.1fkg"`, `WORKOUT_VOLUME`은 `"%+,dkg"`, `WORKOUT_DAYS`는 `"%+d일"`. `Locale.US`를 쓴다(`WeightFormat.kt`와 같은 판단).
- `internal fun ProgressMetricKey.label: String` — 아래 카피 표의 라벨.
- 판정 색: `IMPROVING` → `Accent`, `WORSENING` → `Danger`, `STEADY`·`UNKNOWN` → `TextPrimary`. 상승·하락 전용 색이 디자인시스템에 없어 기존 `Accent`/`Danger`를 쓴다.

카피 원문:

| 자리 | 문구 |
|---|---|
| 상단바 | `홈` |
| 하단 탭 라벨 | `홈` |
| 진척 카드 제목 | `지금 흐름` |
| 진척 카드 부제 | `최근 7일과 그 앞 7일을 견준 결과입니다.` |
| 헤드라인 `IMPROVING` | `잘 가고 있어요` |
| 헤드라인 `WORSENING` | `흐름이 처졌어요` |
| 헤드라인 `STEADY` | `큰 변화가 없어요` |
| 헤드라인 `NOT_ENOUGH_DATA` | `아직 견줄 기록이 부족해요` |
| 헤드라인 `NOT_ENOUGH_DATA` 보조 문구 | `운동과 체중을 며칠 기록하면 흐름을 보여드려요.` |
| 체성분 카드 제목 | `체성분` |
| 체성분 카드 부제 | `최근 두 번의 인바디 차이입니다.` |
| 체성분 값 없음 문구 | `인바디를 두 번 이상 분석하면 변화를 보여드려요.` |
| 라벨 `WEIGHT` | `체중` |
| 라벨 `WORKOUT_VOLUME` | `무게 볼륨` |
| 라벨 `WORKOUT_DAYS` | `운동한 날` |
| 라벨 `SKELETAL_MUSCLE` | `골격근량` |
| 라벨 `BODY_FAT` | `체지방량` |
| 값 없음 | `-` |
| 조회 실패 | `불러오지 못했습니다` |
| 재시도 버튼 | `다시 시도` |

화면별 상태:

- 로딩: `summary == null`인 동안 화면 가운데 `CircularProgressIndicator`.
- 빈: 카드는 늘 보이고, 값이 없는 행은 `-`. 헤드라인은 `아직 견줄 기록이 부족해요` + 보조 문구. 체성분 지표가 둘 다 `UNKNOWN`이면 행 대신 안내 문구 한 줄.
- 에러: 화면 가운데 `불러오지 못했습니다` + `다시 시도`.
- 권한: 해당 없음.

시안 노드 id: 해당 없음(Figma 시안 없음).

**아이콘**

- `core/designsystem/src/main/res/drawable/ic_house.xml` 신규. 기존 아이콘과 같은 규격 — `24dp`, `viewportWidth/Height 24`, `strokeWidth 2`, `strokeLineCap/Join round`, 채우기 없이 선만.
- `BodyPlanIcons`에 `val House: Painter @Composable get() = painterResource(R.drawable.ic_house)` 추가.

### 네비게이션

- `:feature:home:api`에 `@Serializable data object HomeNavKey : BodyPlanNavKey()`와 `fun BodyPlanNavigator.navigateToHome() = navigate(HomeNavKey)`.
- `:feature:home:impl`의 `fun BodyPlanEntryProviderScope.homeNavGraph(navigator: BodyPlanNavigator)`가 `entry<HomeNavKey> { HomeView(viewModel = hiltViewModel()) }`를 등록한다.
- 홈은 다른 화면으로 나가지 않으므로 `HomeEvents`를 두지 않는다 — `navigator`는 시그니처에만 받고 쓰지 않는다. navGraph 확장 함수의 모양을 다른 feature와 맞추기 위한 것이다.
- `app/.../BodyPlanMainScreen.kt`의 `entryProvider`에 `homeNavGraph(navigator)`를 더하고, `MainTab` enum 맨 앞에 `HOME("홈", HomeNavKey)`를 넣는다. 백스택의 뿌리는 `MainTab.entries.first().navKey`라 홈이 첫 화면이 된다.
- 복귀: 홈은 탭 뿌리라 뒤로 가기로 나가는 화면이 없다.

### 버린 안

- **마이 탭 상단에 요약 카드**: 새 모듈 없이 가장 싸지만 마이는 설정성 화면이라 매일 여는 자리가 아니다. 사용자가 「홈 탭 신설」을 골랐다.
- **운동 일지 탭 캘린더 위에 카드**: 노출은 제일 좋으나 체중·체성분 지표가 운동 탭에 섞여 화면의 역할이 흐려진다. 사용자가 「홈 탭 신설」을 골랐다.
- **판정 없이 변화량만 보이기**: 틀릴 일이 없지만 "지금 나아지고 있나"에 답하지 않는다. 사용자가 「목표 있으면 목표 기준, 없으면 고정 기준」을 골랐다.
- **목표를 보지 않고 늘 고정 기준으로 판정**: 증량 중인 사용자에게 체중 증가를 악화로 보이는 오판이 생긴다. 사용자가 혼합안을 골랐다.
- **`observeLog(date)`를 14번 `combine`해 기간을 만들기**: Flow를 14개 열게 되고 화면이 열릴 때마다 늘어난다. 범위 조회를 새로 만든다.
- **인바디 측정값 전용 표를 새로 두기**: `analysis_result`에 이미 컬럼 4개로 실려 있고 홈은 최근 2건만 본다. 표를 늘릴 이유가 없다.
- **체중을 인바디 측정값과 섞어 한 줄로 내기**: 손으로 넣은 값과 사진에서 읽은 추정값이 섞이면 하루 차이가 실제 변화인지 읽기 오차인지 구분되지 않는다(`WeightRecord`의 기존 판단).

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/local/src/main/java/kr/sdbk/bodyplan/core/local/dao/WorkoutEntryDao.kt` | 수정 | `observeWithSetsInRange(from, to)` 추가 |
| `core/data/src/main/java/kr/sdbk/bodyplan/core/data/mapper/WorkoutMapper.kt` | 수정 | `List<WorkoutEntryWithSets>.toEntriesByDate()` 추가 |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/repository/WorkoutLogRepository.kt` | 수정 | `observeEntriesInRange(from, to): Flow<Map<LocalDate, List<WorkoutEntry>>>` 추가 |
| `core/data/src/main/java/kr/sdbk/bodyplan/core/data/repository/WorkoutLogRepositoryImpl.kt` | 수정 | `observeEntriesInRange` 구현 |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/model/WorkoutVolume.kt` | 신규 | `WorkoutVolume(weightVolumeKg, workoutDays)` |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/model/ProgressSummary.kt` | 신규 | `ProgressMetricKey`·`ProgressDirection`·`ProgressHeadline`·`ProgressMetric`·`ProgressSummary` |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/usecase/SummarizeWorkoutVolumeUseCase.kt` | 신규 | 순수 집계 |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/usecase/GetProgressSummaryUseCase.kt` | 신규 | 네 스트림 `combine` + 지표·방향·헤드라인 산출 |
| `core/data/src/test/java/kr/sdbk/bodyplan/core/data/repository/fake/FakeWorkoutEntryDao.kt` | 수정 | `observeWithSetsInRange` 구현 |
| `core/data/src/test/java/kr/sdbk/bodyplan/core/data/repository/WorkoutLogRepositoryImplTest.kt` | 수정 | 범위 조회·날짜별 묶음·세트 정렬 케이스 추가 |
| `core/domain/src/test/java/kr/sdbk/bodyplan/core/domain/usecase/SummarizeWorkoutVolumeUseCaseTest.kt` | 신규 | 무게 종목만 합산·각도/시간 제외·빈 날 제외 |
| `core/domain/src/test/java/kr/sdbk/bodyplan/core/domain/usecase/GetProgressSummaryUseCaseTest.kt` | 신규 | 지표별 값·목표별 방향·헤드라인·빈 결과 |
| `core/domain/src/test/java/kr/sdbk/bodyplan/core/domain/usecase/AnalyzeWorkoutUseCaseTest.kt` | 수정 | 내부 가짜 `WorkoutLogRepository`에 `observeEntriesInRange` 추가(컴파일 유지) |
| `core/domain/src/test/java/kr/sdbk/bodyplan/core/domain/usecase/GetMonthlyDayStatusUseCaseTest.kt` | 수정 | 같은 이유로 가짜 구현 보강 |
| `core/designsystem/src/main/res/drawable/ic_house.xml` | 신규 | 홈 탭 아이콘 |
| `core/designsystem/src/main/java/kr/sdbk/bodyplan/core/designsystem/component/BodyPlanIcon.kt` | 수정 | `BodyPlanIcons.House` 추가 |
| `settings.gradle.kts` | 수정 | `include(":feature:home:api")`·`include(":feature:home:impl")` |
| `feature/home/api/build.gradle.kts` | 신규 | `bodyplan.android.feature.api` + `namespace` |
| `feature/home/api/src/main/java/kr/sdbk/bodyplan/feature/home/api/HomeNavKey.kt` | 신규 | `HomeNavKey`·`navigateToHome()` |
| `feature/home/impl/build.gradle.kts` | 신규 | `bodyplan.android.feature.impl` + `namespace` |
| `feature/home/impl/src/main/java/kr/sdbk/bodyplan/feature/home/impl/HomeNavGraph.kt` | 신규 | `homeNavGraph(navigator)` |
| `feature/home/impl/src/main/java/.../home/HomeContracts.kt` | 신규 | `HomeState`·`HomeIntent`·`HomeEffect` |
| `feature/home/impl/src/main/java/.../home/HomeViewModel.kt` | 신규 | 구독·재시도·`LOAD_ERROR` |
| `feature/home/impl/src/main/java/.../home/ProgressFormat.kt` | 신규 | `changeText`·`label`·판정 색 |
| `feature/home/impl/src/main/java/.../home/composable/HomeView.kt` | 신규 | `HomeView`·`HomeViewImpl`·`HomeUiEvents`·로딩/에러 + Preview |
| `feature/home/impl/src/main/java/.../home/composable/ProgressCard.kt` | 신규 | 헤드라인 + 지표 3행 + Preview |
| `feature/home/impl/src/main/java/.../home/composable/BodyCompositionCard.kt` | 신규 | 체성분 2행 또는 안내 문구 + Preview |
| `feature/home/impl/src/main/java/.../home/composable/MetricRow.kt` | 신규 | 라벨-값 행 |
| `feature/home/impl/src/test/java/.../MainDispatcherRule.kt` | 신규 | 다른 feature의 것과 같은 내용(`UnconfinedTestDispatcher`) |
| `feature/home/impl/src/test/java/.../home/HomeViewModelTest.kt` | 신규 | 첫 로드·실패·재시도 |
| `app/src/main/java/kr/sdbk/bodyplan/navigation/BodyPlanMainScreen.kt` | 수정 | `homeNavGraph(navigator)` 등록, `MainTab`에 `HOME` 맨 앞 추가, 아이콘 매핑 |

`app/build.gradle.kts`는 고치지 않는다 — `AndroidApplicationConventionPlugin`이 `:feature:*:impl`을 경로로 전부 붙인다.

### 구현 순서

**단위 1 — 기간 조회 (local·data·domain 계약)**
- 쓰는 것: `WorkoutEntryWithSets`, `WorkoutEntryWithSets.toDomain()`(기존)
- 만드는 것: `WorkoutEntryDao.observeWithSetsInRange`, `toEntriesByDate`, `WorkoutLogRepository.observeEntriesInRange`, 구현, `FakeWorkoutEntryDao` 보강, 가짜 저장소 2곳 보강
- 검증: `./gradlew :core:data:testDebugUnitTest :core:domain:test`
- 커밋: `운동 기록을 기간으로 읽는 조회를 더함`

**단위 2 — 집계 UseCase**
- 쓰는 것: 단위 1의 `observeEntriesInRange`, 기존 `GetWeightTrendUseCase`·`WeightLogRepository`·`UserProfileRepository`·`AnalysisResultRepository`·`Clock`
- 만드는 것: `WorkoutVolume`, `ProgressSummary` 계열 모델, `SummarizeWorkoutVolumeUseCase`, `GetProgressSummaryUseCase`, 두 UseCase 테스트
- 검증: `./gradlew :core:domain:test`
- 커밋: `최근 흐름을 지표로 내는 계층을 만듦`

**단위 3 — 홈 모듈과 탭**
- 쓰는 것: 단위 2의 `GetProgressSummaryUseCase`·`ProgressSummary`
- 만드는 것: `ic_house.xml`, `BodyPlanIcons.House`, `:feature:home:api`·`:feature:home:impl` 전체, `MainTab.HOME`, `homeNavGraph` 등록, `HomeViewModelTest`
- 검증: `./gradlew :app:compileDebugKotlin` + `./gradlew :feature:home:impl:testDebugUnitTest` + Preview 렌더 확인
- 커밋: `홈 탭을 더하고 최근 흐름을 보여줌`

### 회귀 대상

| 고치는 것 | 확인할 기존 호출부 |
|---|---|
| `WorkoutLogRepository` 인터페이스 확장 | 구현체 4곳: `WorkoutLogRepositoryImpl`, `feature/workoutlog/impl` 테스트의 `FakeWorkoutLogRepository`, `AnalyzeWorkoutUseCaseTest` 내부 가짜, `GetMonthlyDayStatusUseCaseTest` 내부 가짜 |
| `WorkoutEntryDao` 확장 | `FakeWorkoutEntryDao`(`:core:data` 테스트). 기존 `observeInRange`를 쓰는 `GetMonthlyDayStatusUseCase`가 그대로인지 |
| `BodyPlanIcons` 확장 | `BodyPlanMainScreen`의 `MainTab.icon`, `SectionRow`의 `ChevronRight` — 기존 프로퍼티를 건드리지 않는지 |
| `MainTab` 맨 앞에 탭 추가 | 백스택 뿌리(`MainTab.entries.first().navKey`), 온보딩 완료 후 이동, 알림으로 들어오는 `startNavKey` 처리 — 셋 다 `first()`를 쓰므로 홈으로 바뀐다 |
| 탭 4개로 늘어남 | `MainBottomBar`의 `Modifier.weight(1f)` 배분 — 좁은 화면에서 라벨이 줄바꿈되는지 |
| `settings.gradle.kts`에 모듈 추가 | `AndroidApplicationConventionPlugin`이 새 impl을 자동으로 붙이는지(`:app` 빌드로 확인) |

자기 대조: 통과 (대조 17/17)
- 고침: 수용 조건, 데이터 계약, 상태 계약, 화면 구성, 파일별 작업, 회귀 대상
