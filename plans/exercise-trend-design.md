# 종목별 성과 추이

**작성일**: 2026-09-10

## 설계

### 요약

`:feature:workoutlog`에 종목 추이 화면을 더한다. 운동 캘린더 상단바의 아이콘 액션으로 들어간다.
부위를 고르고 그 부위에서 **기록이 있는 종목**을 고르면, 최근 8주를 주 단위로 묶어 그 종목의 추이를 꺾은선으로 그린다.
지표는 종목의 강도 축을 따른다 — 무게는 최고중량·총볼륨 두 줄, 각도는 총 횟수, 시간은 총 시간이다. 넷 다 사용자가 정했다.

### 배경

세트별 무게·횟수를 다 받아 두고도 종목 하나가 어떻게 가고 있는지 보여주는 곳이 없다. 홈 요약은 몸 전체의 흐름을 한 줄로 줄이므로, "벤치프레스가 늘고 있나"에는 답하지 않는다.

`SummarizeWorkoutVolumeUseCase`와 `WorkoutLogRepository.observeEntriesInRange`는 홈 요약을 만들며 이 화면을 겨냥해 이미 만들어 두었다 — 주 버킷과 총볼륨은 그대로 쓰고, 최고중량·총 횟수·총 시간만 새로 센다.

꺾은선 차트는 리포에 하나뿐인데(`InbodyTrendChart`) `:feature:my:impl` 안에 갇혀 있고 `InbodyMeasurement`에 묶여 있다. 공용으로 올려 두 화면이 함께 쓴다.

`BodyPlanTopBar`는 오른쪽에 글자 액션 하나만 받는데 운동 캘린더는 그 자리를 「운동 종목 관리」로 이미 쓴다. 아이콘 액션 슬롯을 더한다 — 사용자가 정했다.

### 수용 조건

- [ ] 운동 캘린더 상단바에 아이콘이 생기고, 누르면 종목 추이 화면이 열린다. 기존 「운동 종목 관리」 글자 액션은 그대로 남는다.
- [ ] 부위를 고르면 최근 8주에 기록이 있는 그 부위의 종목만 칩으로 뜬다. 기록이 없는 종목은 뜨지 않는다.
- [ ] 종목 관리에서 지운 종목도 최근 8주에 기록이 있으면 칩에 뜬다 — 기록이 이름·부위·축을 스냅샷으로 들고 있다.
- [ ] 무게 종목을 고르면 「최고중량」·「총볼륨」 두 줄이 그려지고 범례에 마지막 주의 값이 `123kg` 꼴로 뜬다.
- [ ] 각도 종목을 고르면 「총 횟수」 한 줄이 `12회` 꼴로, 시간 종목을 고르면 「총 시간」 한 줄이 `30분` 꼴로 뜬다.
- [ ] 차트 아래 좌우에 첫 주와 마지막 주의 시작 날짜가 `2026.7.20` 꼴로 뜬다.
- [ ] 기록이 한 주에만 있으면 선 없이 점 하나가 찍힌다.
- [ ] 빈 결과: 부위를 고르기 전에는 `부위를 먼저 선택하세요`, 고른 부위에 기록이 없으면 `이 부위에는 최근 기록이 없습니다`, 종목을 고르기 전에는 `종목을 선택하세요`가 뜬다.
- [ ] 조회 실패: `observeEntriesInRange`가 실패하면 `불러오지 못했습니다`와 `다시 시도`가 뜨고, `다시 시도`로 재구독된다.
- [ ] 저장 실패: 해당 없음 — 이 화면은 아무것도 쓰지 않는다.
- [ ] 권한: 해당 없음.
- [ ] 인바디 화면의 기존 차트가 공용 차트로 바뀐 뒤에도 보이는 것이 같다 — 카드 제목 `변화`, 부제 `사진에서 읽은 추정값입니다.`, 세 줄(체중·골격근량·체지방량), 좌우 날짜, 범례.

### 비목표

- 기간을 고르는 UI를 두지 않는다. 최근 8주 고정이다.
- 눈금 숫자·격자선·축을 그리지 않는다. 기존 차트와 같이 줄마다 자기 최소·최대로 편다 — 최고중량(수십)과 총볼륨(수천)을 한 축에 두면 볼륨 줄이 눌린다.
- 점을 눌러 그 주의 값을 보는 상호작용을 넣지 않는다.
- 여러 종목을 겹쳐 견주지 않는다. 한 번에 한 종목이다.
- 1RM 추정을 넣지 않는다 — 추정식을 고르는 판단이 따로 필요하다.
- `entryedit`·`exercisemanage`에 흩어진 `IntensityType` 라벨·단위 표기를 이번에 합치지 않는다. 이 화면이 쓰는 것은 지표 라벨·단위라 그 둘과 문구가 다르다.
- `BodyPlanCalendar`가 일요일 시작으로 그리는 것과 `AnalysisScopeKey.WEEK_START`가 월요일인 불일치는 이번에 손대지 않는다. 이 화면은 `AnalysisScopeKey.weekStart`만 쓴다.

### 데이터 계약

**조회 (신규 없음)**

| 심볼 | 시그니처 | 이 화면이 쓰는 범위 |
|---|---|---|
| `WorkoutLogRepository.observeEntriesInRange` | `(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<WorkoutEntry>>>` | `AnalysisScopeKey.weekStart(today).minusWeeks(7)` ~ `today` |

`WorkoutEntry`에서 쓰는 필드와 귀착지:

| 필드 | 귀착지 |
|---|---|
| `exerciseId` | `RecordedExercise.id` → 종목 칩의 키, 선택 판정 |
| `exerciseName` | `RecordedExercise.name` → 칩 문구, 차트 카드 제목 |
| `bodyPart` | `RecordedExercise.bodyPart` → 부위 탭으로 거르는 기준 |
| `intensityType` | `RecordedExercise.intensityType` → 그릴 지표를 정한다 |
| `sets[].intensity.value` | `MAX_WEIGHT`(최대), `TOTAL_VOLUME`(×횟수 합), `TOTAL_MINUTES`(합) |
| `sets[].repeatCount` | `TOTAL_VOLUME`(무게×횟수), `TOTAL_REPS`(합) |
| `id` | **미사용** — 추이는 기록 하나를 가리키지 않는다 |

**신규 도메인 모델 `core/domain/model/ExerciseTrend.kt`**

```kotlin
enum class ExerciseTrendMetric { MAX_WEIGHT, TOTAL_VOLUME, TOTAL_REPS, TOTAL_MINUTES }

data class ExerciseTrendPoint(val weekStart: LocalDate, val values: Map<ExerciseTrendMetric, Int>)

data class RecordedExercise(
    val id: Long,
    val name: String,
    val bodyPart: BodyPart,
    val intensityType: IntensityType,
)

data class ExerciseTrend(
    val exercise: RecordedExercise,
    val points: List<ExerciseTrendPoint>,
)

data class ExerciseTrendResult(
    val exercises: List<RecordedExercise> = emptyList(),
    val trend: ExerciseTrend? = null,
)
```

- 강도 축이 그릴 지표를 정한다. 같은 파일에 확장을 둔다 — 축이 늘면 여기만 고친다.
  `val IntensityType.trendMetrics: List<ExerciseTrendMetric>` = `WEIGHT` → `[MAX_WEIGHT, TOTAL_VOLUME]`, `ANGLE` → `[TOTAL_REPS]`, `DURATION` → `[TOTAL_MINUTES]`.
- `ExerciseTrendPoint.values`는 그 주에 기록이 없으면 빈 맵이다 — 주는 늘 8개가 다 들어가고, 값이 없는 주가 차트에서 끊긴 점이 된다.
- 값이 `Int`인 것은 `Intensity.value`가 `Int`이기 때문이다. 차트에 넘길 때 `Double?`로 올린다.
- `RecordedExercise`는 `Exercise`와 다르다 — `Exercise`는 지금 등록된 종목이고 이것은 기록에 박힌 스냅샷이다. 지운 종목도 담기므로 `isDeleted`가 없다.

### 상태 계약

**UseCase `SummarizeExerciseTrendUseCase` (`:core:domain`, 순수)**

- `operator fun invoke(entriesByDate: Map<LocalDate, List<WorkoutEntry>>, exerciseId: Long, weekStarts: List<LocalDate>): ExerciseTrend?`
- 그 종목의 기록이 하나도 없으면 `null`.
- `RecordedExercise`는 가장 최근 기록의 스냅샷에서 만든다 — 이름을 고친 종목은 최근 이름으로 보인다.
- 주 버킷은 `AnalysisScopeKey.weekStart(date)`로 가른다. `weekStarts`에 없는 주의 기록은 버린다.
- 지표 계산(그 주의 그 종목 기록 전체를 모아):
  - `MAX_WEIGHT` = `sets.maxOf { it.intensity.value }`
  - `TOTAL_VOLUME` = `sets.sumOf { it.intensity.value * it.repeatCount }`
  - `TOTAL_REPS` = `sets.sumOf { it.repeatCount }`
  - `TOTAL_MINUTES` = `sets.sumOf { it.intensity.value }`
- 그 축의 `trendMetrics`에 없는 지표는 담지 않는다.
- 저장소를 구독하지 않고 받는다 — `SummarizeWorkoutVolumeUseCase`·`GetWeightTrendUseCase`와 같은 짜임새다.

**UseCase `GetExerciseTrendUseCase` (`:core:domain`)**

- 생성자 주입: `workoutLogRepository: WorkoutLogRepository`, `summarizeExerciseTrend: SummarizeExerciseTrendUseCase`, `clock: Clock`
- `operator fun invoke(exerciseId: Long?): Flow<ExerciseTrendResult>`
- `today = LocalDate.now(clock)`를 진입 시 한 번만 읽는다(`GetMonthlyDayStatusUseCase`와 같은 판단).
- `weekStarts` = `AnalysisScopeKey.weekStart(today)`부터 뒤로 7주, 오래된 것부터 8개.
- `observeEntriesInRange(weekStarts.first(), today)`를 구독해:
  - `exercises` = 기록에서 `exerciseId`로 묶어 만든 `RecordedExercise` 목록. 이름·부위·축은 그 종목의 **가장 최근** 기록을 따른다. 정렬은 이름 오름차순.
  - `trend` = `exerciseId`가 `null`이면 `null`, 아니면 `summarizeExerciseTrend(...)`.
- 실패는 삼키지 않고 위로 던진다 — 화면이 잡는다.
- `exerciseId`가 바뀌면 화면이 다시 부른다(구독을 갈아끼운다). 같은 구간을 다시 읽지만 8주치라 감내한다 — 선택마다 스트림을 더 여는 것보다 낫다.

**화면: 종목 추이 (`ExerciseTrendState` / `ExerciseTrendIntent` / `ExerciseTrendEffect`)**

| 필드 | 타입 | 초기값 | 출처 |
|---|---|---|---|
| `selectedBodyPart` | `BodyPart?` | `null` | `SelectBodyPart` |
| `exercises` | `List<RecordedExercise>` | `emptyList()` | `ExerciseTrendResult.exercises` |
| `selectedExerciseId` | `Long?` | `null` | `SelectExercise` |
| `trend` | `ExerciseTrend?` | `null` | `ExerciseTrendResult.trend` |
| `isLoading` | `Boolean` | `false` | 구독 시작 시 `true`, 첫 방출·실패에 `false` |
| `errorMessage` | `String?` | `null` | 구독 실패 시 `LOAD_ERROR` |

화면이 그리는 종목 칩은 `exercises`를 `selectedBodyPart`로 거른 것이다 — 상태에 따로 담지 않는다.

Intent:

- `data class SelectBodyPart(val bodyPart: BodyPart)` — 같은 부위면 무시한다. 다르면 `selectedBodyPart`를 바꾸고 `selectedExerciseId = null`, `trend = null`로 되돌린다(`ExerciseManageViewModel.selectBodyPart`와 같은 짜임새). 구독은 다시 열지 않는다 — 종목 목록은 부위와 무관하게 같은 스트림에서 온다.
- `data class SelectExercise(val id: Long)` — 같은 종목이면 무시. 다르면 `selectedExerciseId`를 바꾸고 구독을 갈아끼운다.
- `data object ClickBack` — `GoBack`.
- `data object ClickRetry` — 지금 선택으로 구독을 다시 연다.

Effect:

- `data object GoBack : ExerciseTrendEffect`

초기 로드는 `initializeData()`에서 `invoke(null)` 구독을 연다.

### 화면 구성

**종목 추이 (`ExerciseTrendViewImpl`)**

```
Column(fillMaxSize + background(Background))
├ BodyPlanTopBar(title = "종목 추이", onBack = ...)     (재사용)
├ BodyPartTabRow(selected, onSelect, fillMaxWidth)      (재사용)
└ Box(weight 1f)
   ├ errorMessage != null → ErrorContent                (신규 — 이 화면 private)
   ├ isLoading && trend == null && exercises.isEmpty() → LoadingContent (신규 — private)
   ├ selectedBodyPart == null → CenterText("부위를 먼저 선택하세요")     (신규 — private)
   ├ 거른 종목이 빔 → CenterText("이 부위에는 최근 기록이 없습니다")      (신규 — private)
   └ else → Column(verticalScroll, padding 16.dp, spacedBy 12.dp)
        ├ LazyRow { items(거른 종목) { ItemChip(...) } }  (재사용)
        ├ trend == null → CenterText("종목을 선택하세요")
        └ trend != null → ExerciseTrendCard(trend)       (신규)
```

재사용하는 심볼(전부 파일을 열어 시그니처를 확인함):

| 심볼 | 시그니처 | 판정 |
|---|---|---|
| `BodyPlanTopBar` | `(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actionText: String? = null, onClickAction: () -> Unit = {})` | **확장** — 아이콘 액션 슬롯을 더한다(아래) |
| `BodyPartTabRow` | `(selected: BodyPart?, onSelect: (BodyPart) -> Unit, modifier: Modifier = Modifier)` | 재사용 — `selected`가 널 허용이라 초기 미선택을 그대로 표현한다 |
| `ItemChip` | `(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)` | 재사용 — 종목처럼 글자가 긴 항목용이라고 KDoc에 적힌 것 |
| `BodyPlanCard` | `(modifier: Modifier = Modifier, cornerRadius: Dp = 16.dp, contentPadding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit)` | 재사용 |
| `BaseText` | `(text: String, modifier: Modifier = Modifier, color: Color, style: TextStyle, textAlign, maxLines, overflow)` | 재사용 |
| `VerticalSpacer` | `(space: Dp)` | 재사용 |
| `OutlinedActionButton` | `(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)` | 재사용 — `다시 시도` |
| `BodyPlanIcon` | `(painter: Painter, contentDescription: String?, boxSize: Dp, iconSize: Dp, tint: Color, modifier: Modifier = Modifier)` | 재사용 — 상단바 아이콘 |
| `PartChest`·`PartLeg`·`PartCardio`·`TextPrimary`·`TextTertiary`·`Background` | 색 상수 | 재사용 — 차트 선 색은 기존 차트와 같이 `Part*` 팔레트를 쓴다 |

**`BodyPlanTopBar` 확장 (`:core:designsystem`)**

파라미터 두 개를 뒤에 더한다. 기본값이 있어 기존 호출부는 그대로다.

```kotlin
actionIcon: Painter? = null,
onClickActionIcon: () -> Unit = {},
```

아이콘은 글자 액션 **왼쪽**에 그린다. `BodyPlanIcon(boxSize = 24.dp, iconSize = 20.dp, tint = TextPrimary)`로 뒤로가기와 같은 규격을 쓰고, 오른쪽 묶음은 `Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically)`로 감싼다.

아이콘 drawable `ic_chart_line.xml`을 `:core:designsystem`에 더하고 `BodyPlanIcons.ChartLine`을 늘린다. 규격은 기존 아이콘과 같다 — `24dp`, `viewportWidth/Height 24`, `strokeWidth 2`, `strokeLineCap/Join round`, 채우기 없이 선만.

**공용 차트 `TrendChart` (`:core:ui:components`, 신규)**

`InbodyTrendChart`의 그리기를 그대로 옮기고 도메인에 묶인 것만 호출부로 뺀다.

```kotlin
data class TrendChartLine(val label: String, val color: Color, val values: List<Double?>)

@Composable
fun TrendChart(
    lines: List<TrendChartLine>,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
    valueText: (Double) -> String = ::defaultValueText,
)
```

- 카드 껍데기(`BodyPlanCard`·제목·부제)는 넣지 않는다 — 두 호출부의 문구가 다르다. 차트는 Canvas + 좌우 라벨 + 범례만 그린다.
- 그리기 규약은 기존 그대로 옮긴다: 줄마다 자기 최소·최대로 편다, 값이 전부 같으면 가운데(`0.5f`), 점이 하나면 선 없이 점만, `usableHeight = size.height - POINT_RADIUS * 2`, `LINE_WIDTH = 3f`, `POINT_RADIUS = 5f`, `DOT_SIZE = 8.dp`, `CHART_HEIGHT = 160.dp`.
- `values`가 전부 `null`인 줄은 호출부가 걸러 넘긴다 — `lines`가 비면 아무것도 그리지 않는다.
- `valueText`는 범례에 적을 값의 표기다. 기본은 기존 `format`과 같다(정수면 정수, 아니면 `%.1f`, `Locale.US`).

**신규 컴포저블 `ExerciseTrendCard`** — `exercisetrend/composable/ExerciseTrendCard.kt`

```kotlin
@Composable
internal fun ExerciseTrendCard(trend: ExerciseTrend, modifier: Modifier = Modifier)
```

`BodyPlanCard` 안에 제목(종목 이름)·부제·`TrendChart`를 둔다. 지표별 선 색은 `MAX_WEIGHT` → `PartChest`, `TOTAL_VOLUME` → `PartLeg`, `TOTAL_REPS` → `PartShoulder`, `TOTAL_MINUTES` → `PartCardio`.

**신규 표기 `exercisetrend/ExerciseTrendFormat.kt`**

- `internal val ExerciseTrendMetric.label: String` — 아래 카피 표.
- `internal val ExerciseTrendMetric.unit: String` — `MAX_WEIGHT`·`TOTAL_VOLUME` → `kg`, `TOTAL_REPS` → `회`, `TOTAL_MINUTES` → `분`.
- `internal fun weekLabel(weekStart: LocalDate): String` — `DateTimeFormatter.ofPattern("yyyy.M.d")`. 기존 차트의 날짜 표기와 같다.

카피 원문:

| 자리 | 문구 |
|---|---|
| 상단바 제목 | `종목 추이` |
| 캘린더 상단바 아이콘 `contentDescription` | `종목 추이` |
| 차트 카드 부제 | `최근 8주를 주 단위로 묶었습니다.` |
| 라벨 `MAX_WEIGHT` | `최고중량` |
| 라벨 `TOTAL_VOLUME` | `총볼륨` |
| 라벨 `TOTAL_REPS` | `총 횟수` |
| 라벨 `TOTAL_MINUTES` | `총 시간` |
| 부위 미선택 | `부위를 먼저 선택하세요` |
| 부위에 기록 없음 | `이 부위에는 최근 기록이 없습니다` |
| 종목 미선택 | `종목을 선택하세요` |
| 조회 실패 | `불러오지 못했습니다` |
| 재시도 버튼 | `다시 시도` |
| 범례 값 없음 | `-` |

화면별 상태:

- 로딩: 첫 구독에서 화면 가운데 `CircularProgressIndicator`. 종목을 바꿔 다시 구독할 때는 이전 차트를 그대로 두고 스피너를 덮지 않는다.
- 빈: 위 세 가지 안내 문구.
- 에러: 화면 가운데 `불러오지 못했습니다` + `다시 시도`.
- 권한: 해당 없음.

시안 노드 id: 해당 없음(Figma 시안 없음).

### 네비게이션

- `:feature:workoutlog:api`에 `@Serializable data object ExerciseTrendNavKey : BodyPlanNavKey()`와 `fun BodyPlanNavigator.navigateToExerciseTrend() = navigate(ExerciseTrendNavKey)`. 종목은 화면 안에서 고르므로 인자가 없다.
- `WorkoutLogNavGraph.kt`에 `entry<ExerciseTrendNavKey>`를 더한다. `ExerciseTrendEvents(goBack = navigator::goBack)`, ViewModel은 NavKey를 받지 않으므로 `hiltViewModel()`.
- `WorkoutCalendarEvents`에 `goToExerciseTrend: () -> Unit`을, `WorkoutCalendarUiEvents`에 `onClickExerciseTrend: () -> Unit`을, `WorkoutCalendarIntent`에 `ClickExerciseTrend`를, `WorkoutCalendarEffect`에 `NavigateToExerciseTrend`를 더한다.
- 복귀: 뒤로 가기로 운동 캘린더로 돌아간다.

### 버린 안

- **홈의 「무게 볼륨」 행을 눌러 들어가기**: 홈이 `:feature:workoutlog:api`를 의존하게 되고, 홈 요약 계획이 「카드는 눌리지 않는다」를 비목표로 못박은 것과 어긋난다. 사용자가 상단바를 골랐다.
- **종목 관리 화면에서 종목을 눌러 들어가기**: 종목 관리는 이름·삭제를 다루는 화면이라 역할이 섞인다. 사용자가 상단바를 골랐다.
- **상단바 액션을 「종목」 하나로 묶고 그 화면에서 갈라지기**: 상단바를 안 고쳐도 되지만 기존 진입 경로가 바뀌어 쓰던 사람이 헤맨다. 사용자가 아이콘 슬롯 추가를 골랐다.
- **본문 아래 분석 버튼 행에 붙이기**: 제일 싸지만 AI 글과 숫자 그래프가 같은 줄에 섞인다. 사용자가 아이콘 슬롯 추가를 골랐다.
- **부위별 등록 종목을 전부 보여주기**: 기록이 없는 종목을 고르면 빈 화면이 뜨고, 지운 종목의 과거 기록은 볼 길이 없다. 사용자가 「기록이 있는 종목만」을 골랐다.
- **최고중량만 / 총볼륨만 그리기**: 무게를 올리면 볼륨이 줄기도 해 하나만 보면 잘못 읽는다. 사용자가 둘 다를 골랐다.
- **비무게 종목을 목록에서 감추기**: 종목을 고르는 화면에서 절반이 사라진다. 사용자가 축마다 맞는 지표를 골랐다.
- **`SummarizeWorkoutVolumeUseCase`를 고쳐 최고중량·총 횟수·총 시간까지 내게 하기**: 그 UseCase는 기간 전체를 한 값으로 줄이는 것이고 이 화면은 주마다 종목 하나를 본다. 계약이 달라 새 UseCase를 둔다.
- **DAO에 종목별 조회를 새로 뚫기**: `WorkoutEntry`가 `exerciseId`를 스냅샷으로 들고 있어 이미 있는 기간 조회로 거를 수 있다. 8주치라 읽는 비용도 작다.
- **차트에 공유 축 옵션을 두기**: 최고중량(수십)과 총볼륨(수천)을 한 축에 두면 볼륨 줄만 보인다. 줄별 정규화를 그대로 쓴다.

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/ui/components/src/main/java/kr/sdbk/bodyplan/core/ui/components/TrendChart.kt` | 신규 | `TrendChartLine`·`TrendChart`·`defaultValueText` + Preview |
| `feature/my/impl/src/main/java/kr/sdbk/bodyplan/feature/my/impl/inbody/composable/InbodyTrendChart.kt` | 수정 | 그리기를 `TrendChart`에 넘기고 카드·문구·`InbodyMeasurement` 매핑만 남긴다 |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/model/ExerciseTrend.kt` | 신규 | `ExerciseTrendMetric`·`ExerciseTrendPoint`·`RecordedExercise`·`ExerciseTrend`·`ExerciseTrendResult`·`IntensityType.trendMetrics` |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/usecase/SummarizeExerciseTrendUseCase.kt` | 신규 | 주 버킷·지표 집계(순수) |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/usecase/GetExerciseTrendUseCase.kt` | 신규 | 구간 계산 + 구독 + 종목 목록 |
| `core/domain/src/test/java/kr/sdbk/bodyplan/core/domain/usecase/SummarizeExerciseTrendUseCaseTest.kt` | 신규 | 축별 지표·주 버킷·빈 주·지운 종목 |
| `core/domain/src/test/java/kr/sdbk/bodyplan/core/domain/usecase/GetExerciseTrendUseCaseTest.kt` | 신규 | 8주 구간·종목 목록·선택 없음·실패 전파 |
| `core/designsystem/src/main/res/drawable/ic_chart_line.xml` | 신규 | 상단바 아이콘 |
| `core/designsystem/src/main/java/kr/sdbk/bodyplan/core/designsystem/component/BodyPlanIcons.kt` | 수정 | `ChartLine` 추가 |
| `core/designsystem/src/main/java/kr/sdbk/bodyplan/core/designsystem/component/BodyPlanTopBar.kt` | 수정 | `actionIcon`·`onClickActionIcon` 추가, 오른쪽 묶음 `Row`로 |
| `feature/workoutlog/api/src/main/java/kr/sdbk/bodyplan/feature/workoutlog/api/WorkoutLogNavKey.kt` | 수정 | `ExerciseTrendNavKey`·`navigateToExerciseTrend()` |
| `feature/workoutlog/impl/src/main/java/.../WorkoutLogNavGraph.kt` | 수정 | `entry<ExerciseTrendNavKey>` 추가, 캘린더 events에 `goToExerciseTrend` |
| `feature/workoutlog/impl/src/main/java/.../calendar/WorkoutCalendarContracts.kt` | 수정 | `ClickExerciseTrend` Intent, `NavigateToExerciseTrend` Effect |
| `feature/workoutlog/impl/src/main/java/.../calendar/WorkoutCalendarViewModel.kt` | 수정 | 새 Intent 처리 |
| `feature/workoutlog/impl/src/main/java/.../calendar/composable/WorkoutCalendarView.kt` | 수정 | `Events`·`UiEvents` 확장, 상단바에 아이콘 액션 |
| `feature/workoutlog/impl/src/main/java/.../exercisetrend/ExerciseTrendContracts.kt` | 신규 | State·Intent·Effect |
| `feature/workoutlog/impl/src/main/java/.../exercisetrend/ExerciseTrendViewModel.kt` | 신규 | 구독·선택·재시도 |
| `feature/workoutlog/impl/src/main/java/.../exercisetrend/ExerciseTrendFormat.kt` | 신규 | 지표 라벨·단위·주 표기 |
| `feature/workoutlog/impl/src/main/java/.../exercisetrend/composable/ExerciseTrendView.kt` | 신규 | `Events`·`UiEvents`·`View`·`ViewImpl` + Preview |
| `feature/workoutlog/impl/src/main/java/.../exercisetrend/composable/ExerciseTrendCard.kt` | 신규 | 카드 + `TrendChart` 조립 + Preview |
| `feature/workoutlog/impl/src/test/java/.../fake/FakeWorkoutLogRepository.kt` | 수정 | `observeEntriesInRange`가 생성자로 받은 기록을 실제로 내도록(지금은 `entriesByDate` 고정값) |
| `feature/workoutlog/impl/src/test/java/.../exercisetrend/ExerciseTrendViewModelTest.kt` | 신규 | 초기 로드·부위 선택·종목 선택·재시도·실패 |
| `feature/workoutlog/impl/src/test/java/.../calendar/WorkoutCalendarViewModelTest.kt` | 수정 | `ClickExerciseTrend` → `NavigateToExerciseTrend` 케이스 추가 |

### 구현 순서

**단위 1 — 공용 차트**
- 쓰는 것: `InbodyTrendChart`의 그리기(기존), `BodyPlanCard`·`BaseText`·`VerticalSpacer`·`WeightSpacer`(기존)
- 만드는 것: `TrendChartLine`, `TrendChart`, `defaultValueText`. `InbodyTrendChart`가 그것을 쓰도록 고친다
- 검증: `./gradlew :app:compileDebugKotlin` + `:feature:my:impl:testDebugUnitTest` + 인바디 화면 Preview 렌더 비교
- 커밋: `꺾은선 차트를 공용으로 올림`

**단위 2 — 도메인**
- 쓰는 것: `WorkoutLogRepository.observeEntriesInRange`(기존), `AnalysisScopeKey.weekStart`(기존), `Clock`(기존 `DataModule` 제공)
- 만드는 것: `ExerciseTrend.kt` 모델 일습, `SummarizeExerciseTrendUseCase`, `GetExerciseTrendUseCase`, 두 테스트
- 검증: `./gradlew :core:domain:test`
- 커밋: `종목별 주간 추이를 내는 계층을 만듦`

**단위 3 — 상단바 슬롯과 아이콘**
- 쓰는 것: `BodyPlanIcon`(기존)
- 만드는 것: `ic_chart_line.xml`, `BodyPlanIcons.ChartLine`, `BodyPlanTopBar`의 아이콘 액션 슬롯
- 검증: `./gradlew :app:compileDebugKotlin` + `BodyPlanTopBarPreview` 렌더
- 커밋: 단위 4와 함께 한 커밋

**단위 4 — 화면과 진입**
- 쓰는 것: 단위 1의 `TrendChart`, 단위 2의 `GetExerciseTrendUseCase`·모델, 단위 3의 상단바 슬롯
- 만드는 것: `ExerciseTrendNavKey`·navigate 확장, `exercisetrend` 패키지 일습, navGraph entry, 캘린더의 Intent·Effect·Events·상단바 아이콘, 테스트 2개
- 검증: `./gradlew :app:assembleDebug` + `:feature:workoutlog:impl:testDebugUnitTest` + Preview 렌더
- 커밋: `종목별 성과 추이 화면을 더함`

### 회귀 대상

| 고치는 것 | 확인할 기존 호출부 |
|---|---|
| `BodyPlanTopBar` 파라미터 추가 | 이 상단바를 쓰는 화면 전부 — 운동 캘린더·운동 일지·기록 편집·종목 관리·분석·마이의 프로필/인바디/체중/AI 토큰·홈. 기본값이 있어 컴파일은 유지되나 오른쪽 정렬이 `Row`로 바뀌므로 글자 액션만 있는 화면의 모양이 그대로인지 본다 |
| `InbodyTrendChart` 내부 교체 | 인바디 화면(`InbodyView`의 `beforeResult` 슬롯) — 점 0개·1개·여러 개에서 보이는 것이 같은지 |
| `BodyPlanIcons` 확장 | `MainTabItem`·`SectionRow`·`BodyPlanTopBar`의 기존 아이콘 — 기존 프로퍼티를 건드리지 않는지 |
| `WorkoutCalendarEvents` 생성자 확장 | `WorkoutLogNavGraph`의 `entry<WorkoutCalendarNavKey>`, `WorkoutCalendarView.kt`의 `previewUiEvents` |
| `WorkoutCalendarUiEvents` 생성자 확장 | 같은 파일의 `rememberUiEvents`·`previewUiEvents` |
| `FakeWorkoutLogRepository`의 `observeEntriesInRange` 동작 변경 | `feature/workoutlog/impl` 테스트 전체 — `WorkoutLogViewModelTest`·`WorkoutCalendarViewModelTest`·`WorkoutEntryEditViewModelTest`가 이 페이크를 함께 쓴다 |

자기 대조: 통과 (대조 17/17)
- 고침: 수용 조건, 상태 계약, 화면 구성, 파일별 작업, 회귀 대상
