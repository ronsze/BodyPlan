# 부위별 볼륨 — 하루·이번 주

**작성일**: 2026-09-14

## 설계

### 요약

운동기록 탭에 부위별 총 볼륨 카드 둘을 더한다. 날짜별 기록 화면에는 **그날**의 부위별 볼륨, 캘린더 화면에는 **이번 주**의 부위별 볼륨이다.
볼륨은 기존 `WorkoutVolume`과 같이 무게 종목만 kg×횟수로 세고, 부위별로 나눈다. 셈은 `:core:domain`의 새 UseCase가 하고 카드는 `:core:ui:components`에 하나 두어 두 화면이 함께 쓴다.
홈의 2주 추이(단위 B)는 이 셈을 재사용하지만 이 계획의 범위가 아니다.

### 배경

사용자가 4단위(C 접힘 기본값, D 캘린더 접기, A 이 문서, B 홈 추이) 순서와 단위별 브랜치를 정했다. 카드 자리(하루→기록 화면, 주간→캘린더 화면)와 "무게 종목만 센다"는 전제도 사용자가 추천안을 받아들여 확정했다.

D가 캘린더 화면 레이아웃을 바꿨으므로 이 단위의 브랜치는 `feature/calendar-week-collapse`에서 분기한다 — `master`에서 분기하면 같은 파일을 두 번 고쳐 병합 충돌이 난다.

`SummarizeWorkoutVolumeUseCase`가 무게 볼륨 셈을 `private` 확장으로 갖고 있다. 부위별 셈과 홈 추이(B)가 같은 셈을 써야 하므로 도메인 모델의 public 확장으로 올린다.

### 수용 조건

- [ ] 날짜별 기록 화면 목록 맨 위에 「부위별 볼륨」 카드가 뜨고, 그날 무게 종목 기록이 있는 부위마다 `가슴 1,240kg` 꼴로 한 줄씩, 맨 아래에 `합계 3,400kg`이 뜬다. 부위 순서는 `BodyPart` 선언 순서다.
- [ ] 무게 종목 기록이 없는 부위(각도·시간·유산소만 한 부위)는 줄에 나오지 않는다. 무게 종목이 하나라도 있으면 합이 0kg이어도 나온다 — 맨몸 0kg 기록도 기록이다.
- [ ] 그날 무게 종목 기록이 하나도 없으면 카드 안에 `무게 기록이 없습니다`가 뜬다. 기록 자체가 없는 날도 같다.
- [ ] 기록을 추가·수정·삭제하면 카드 숫자가 따라 바뀐다(기존 `observeLog` 구독으로).
- [ ] 캘린더 화면의 캘린더 아래, 이달 요약 배너 위에 「이번 주 부위별 볼륨」 카드가 같은 형식으로 뜬다. 구간은 이번 주 월요일(`AnalysisScopeKey.weekStart(today)`)부터 오늘까지다. 다른 달을 보고 있어도 이번 주 카드는 그대로다.
- [ ] 캘린더 화면 카드는 이번 주에 무게 종목 기록이 없으면 `이번 주 무게 기록이 없습니다`가 뜬다.
- [ ] 조회 실패(캘린더): `observeEntriesInRange`가 실패하면 카드 안에 `불러오지 못했습니다`가 뜬다. 달 상태 조회와 독립이라 캘린더는 그대로 보이고, 화면의 기존 `다시 시도`(달 조회 실패 화면)와 별개로 카드 안 `다시 시도`로 재구독한다.
- [ ] 조회 실패(기록 화면): 별도 경로 없음 — 카드는 기존 `observeLog` 결과에서 파생되며, 그 실패는 기존 에러 화면이 덮는다.
- [ ] 저장 실패: 해당 없음 — 두 카드 모두 쓰지 않는다.
- [ ] 권한: 해당 없음.
- [ ] `SummarizeWorkoutVolumeUseCase`의 결과가 확장을 옮긴 뒤에도 같다 — 기존 테스트 6건 통과.

### 비목표

- 부위별 볼륨의 기간 선택 UI를 두지 않는다. 하루·이번 주 고정이다.
- 각도·시간·유산소 종목의 "볼륨"을 정의하지 않는다. 무게 종목만이다.
- 주 시작 요일 불일치(`BodyPlanCalendar` 일요일 시작 vs `AnalysisScopeKey.WEEK_START` 월요일)는 이번에 손대지 않는다 — 기존 「이번 주 분석」과 같은 월요일 기준을 따른다.
- 홈의 `MetricRow`·`ProgressFormat`을 공용화하지 않는다. 부호 있는 변화량 포맷이라 총량 표기와 다르다.
- 캘린더 접기(D)와 볼륨 카드의 상호작용을 두지 않는다. 접혀 있어도 카드는 보인다.
- `FakeWorkoutLogRepository`의 `observeLog`가 날짜를 무시하는 것은 고치지 않는다 — 이 단위의 테스트는 그 동작에 기대지 않는다.

### 데이터 계약

**조회 (신규 없음)**

| 심볼 | 시그니처 | 이 단위가 쓰는 범위 |
|---|---|---|
| `WorkoutLogRepository.observeLog` | `(date: LocalDate): Flow<WorkoutLog>` | 기존 구독 그대로 — `WorkoutLog.entries`를 부위별 셈에 넘긴다 |
| `WorkoutLogRepository.observeEntriesInRange` | `(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<WorkoutEntry>>>` | `AnalysisScopeKey.weekStart(today)` ~ `today` |

`WorkoutEntry`에서 쓰는 필드와 귀착지:

| 필드 | 귀착지 |
|---|---|
| `bodyPart` | `BodyPartVolume.bodyPart` → 줄의 이름·색 |
| `intensityType` | `WEIGHT`가 아니면 셈에서 제외(그 부위의 줄 표시 여부에도 안 셈) |
| `sets[].intensity.value` × `sets[].repeatCount` | `BodyPartVolume.weightVolumeKg` |
| `id`, `exerciseId`, `exerciseName` | **미사용** |

**신규 도메인 모델 `core/domain/model/BodyPartVolume.kt`**

```kotlin
/** 한 부위의 무게 볼륨. 무게 종목 기록이 있는 부위만 만들어진다. */
data class BodyPartVolume(val bodyPart: BodyPart, val weightVolumeKg: Int)
```

**기존 파일 `core/domain/model/WorkoutVolume.kt`에 확장 추가**

```kotlin
/** 무게 종목이 아니면 0. 각도는 곱해도 뜻이 없고 시간은 단위가 다르다. */
val WorkoutEntry.weightVolumeKg: Int
```

`SummarizeWorkoutVolumeUseCase`의 `private fun WorkoutEntry.weightVolume()`을 이것으로 바꾼다.

### 상태 계약

**UseCase `SummarizeBodyPartVolumeUseCase` (`:core:domain`, 순수)**

- `@Inject constructor()`
- `operator fun invoke(entries: List<WorkoutEntry>): List<BodyPartVolume>`
- `BodyPart.entries` 순서. `intensityType == WEIGHT`인 기록이 하나도 없는 부위는 담지 않는다. 있으면 합이 0이어도 담는다.
- 기간 셈은 호출부가 `entriesByDate.values.flatten()`으로 넘긴다 — 오버로드를 두지 않는다.

**UseCase `GetWeeklyBodyPartVolumeUseCase` (`:core:domain`)**

- 생성자 주입: `workoutLogRepository: WorkoutLogRepository`, `summarizeBodyPartVolume: SummarizeBodyPartVolumeUseCase`, `clock: Clock`
- `operator fun invoke(): Flow<List<BodyPartVolume>>`
- `today = LocalDate.now(clock)`를 호출 시 한 번 읽고 `observeEntriesInRange(AnalysisScopeKey.weekStart(today), today)`를 `map`한다 — `GetExerciseTrendUseCase`와 같은 짜임새.
- 저장소 실패는 삼키지 않고 Flow로 던진다.

**화면 `WorkoutLog` (`feature/workoutlog/impl/log`) — 기존 State에 필드 추가**

| 필드 | 타입 | 초기값 | 출처 |
|---|---|---|---|
| `bodyPartVolumes` | `List<BodyPartVolume>` | `emptyList()` | `observeLog` collect 시 `summarizeBodyPartVolume(log.entries)` |

- ViewModel 생성자에 `summarizeBodyPartVolume: SummarizeBodyPartVolumeUseCase` 추가.
- Intent·Effect 추가 없음. 실패 경로는 기존 `errorMessage`가 덮는다.

**화면 `WorkoutCalendar` (`feature/workoutlog/impl/calendar`) — 기존 State에 필드 추가**

| 필드 | 타입 | 초기값 | 출처 |
|---|---|---|---|
| `weeklyVolumes` | `List<BodyPartVolume>` | `emptyList()` | `getWeeklyBodyPartVolume()` collect |
| `weeklyVolumeErrorMessage` | `String?` | `null` | collect 실패 시 `"불러오지 못했습니다"`, 재구독 시작 시 `null` |

- Intent 추가: `ClickRetryWeeklyVolume` → 주간 볼륨만 재구독.
- ViewModel 생성자에 `getWeeklyBodyPartVolume: GetWeeklyBodyPartVolumeUseCase` 추가. `initializeData()`에서 달 구독과 별도 `Job`으로 구독한다. 달 이동(`ChangeMonth`)은 이 구독에 영향을 주지 않는다.
- 기존 `ClickRetry`(달 조회 실패)는 그대로. 주간 볼륨 실패는 달 조회를 막지 않고 카드 안에서만 보인다.
- Effect 추가 없음.

### 화면 구성

**공용 카드 `BodyPartVolumeCard` — 신규, `core/ui/components/BodyPartVolumeCard.kt`**

도메인 타입(`BodyPartVolume`, `BodyPart.label`·`color`)을 쓰므로 `core:ui:components`다.

```kotlin
@Composable
fun BodyPartVolumeCard(
    title: String,
    volumes: List<BodyPartVolume>,
    emptyText: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    onClickRetry: () -> Unit = {},
)
```

- 트리: `BodyPlanCard`(재사용, `core/designsystem/component/BodyPlanCard.kt` — `modifier`, `cornerRadius = 16.dp`, `contentPadding = 20.dp`, `content`) → 제목 `BaseText`(titleSmall, TextPrimary) → 상태별 본문:
  - `errorMessage != null`: `BaseText`(errorMessage, bodyMedium, TextSecondary) + `OutlinedActionButton(text = "다시 시도", onClick = onClickRetry)` (재사용, `core/designsystem/component/BodyPlanButtons.kt`)
  - `volumes.isEmpty()`: `BaseText`(emptyText, bodyMedium, TextTertiary)
  - 그 외: 부위 줄 반복 + `HorizontalDivider(color = Border)` + 합계 줄
- 부위 줄: `Row` → 8dp 색 점(`Box`, `CircleShape`, `bodyPart.color`) → `BaseText`(`bodyPart.label`, bodyMedium, TextPrimary) → `WeightSpacer()` → `BaseText`(볼륨 문자열, bodyMedium, TextPrimary)
- 합계 줄: `BaseText`(`"합계"`, bodyMedium, TextSecondary) → `WeightSpacer()` → `BaseText`(볼륨 문자열, titleSmall, TextPrimary)
- 볼륨 문자열: 같은 파일의 `internal fun volumeText(kg: Int): String` = `String.format(Locale.US, "%,dkg", kg)` → `1,240kg`
- 로딩: 해당 없음 — 값이 오기 전에는 빈 목록이라 emptyText가 잠시 보인다. 카드에 스피너를 두지 않는다.
- 권한: 해당 없음.

**날짜별 기록 화면 (`log/composable/WorkoutLogView.kt`)**

- `LogContent`의 `LazyColumn` 첫 `item`으로 `BodyPartVolumeCard(title = "부위별 볼륨", volumes = state.bodyPartVolumes, emptyText = "무게 기록이 없습니다")`. 그 아래가 기존 메모 카드.
- 로딩·에러: 기존 화면 분기(`LoadingContent`·`ErrorContent`) 그대로. 카드는 `LogContent` 안에만 있다.
- 빈: 카드 안 `무게 기록이 없습니다`, 기존 `기록이 없습니다`도 그대로.
- 권한: 해당 없음.

**캘린더 화면 (`calendar/composable/WorkoutCalendarView.kt`)**

- `BodyPlanCalendar` 다음, `MonthSummaryBanner` 앞에 `BodyPartVolumeCard(title = "이번 주 부위별 볼륨", volumes = state.weeklyVolumes, emptyText = "이번 주 무게 기록이 없습니다", errorMessage = state.weeklyVolumeErrorMessage, onClickRetry = uiEvents.onClickRetryWeeklyVolume)`.
- `WorkoutCalendarUiEvents`에 `onClickRetryWeeklyVolume: () -> Unit` 추가.
- 로딩: 해당 없음(카드 규칙과 같음). 에러: 카드 안. 빈: 카드 안. 권한: 해당 없음.

**카피 원문**: `부위별 볼륨` / `이번 주 부위별 볼륨` / `무게 기록이 없습니다` / `이번 주 무게 기록이 없습니다` / `합계` / `불러오지 못했습니다` / `다시 시도`

### 네비게이션

해당 없음 — 새 화면·NavKey 없음.

### 버린 안

- 둘 다 캘린더 화면에(선택한 날짜 + 그 주): 캘린더가 날짜를 누르면 이동하는 구조라 "선택" 상태가 없어 동작을 바꿔야 한다. 채팅에서 버림.
- 별도 「볼륨」 화면: 진입점이 늘고 종목 추이 화면과 역할이 겹친다. 채팅에서 버림.
- 하루 볼륨을 `WorkoutLogState`의 파생 getter로: State가 UseCase를 못 가지므로 셈이 State 안에 복제된다. ViewModel이 collect 시 계산해 필드로 둔다.
- `SummarizeBodyPartVolumeUseCase`에 `Map<LocalDate, List<WorkoutEntry>>` 오버로드: `flatten()` 한 줄이라 오버로드 값이 없다.
- 주간 볼륨 실패를 화면 전체 `errorMessage`로: 달 조회는 멀쩡한데 캘린더까지 가려진다. 카드 안에서만 보인다.
- 카드를 `core:designsystem`에: `BodyPart`·`BodyPartVolume`에 묶여 도메인 종속이다.

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/model/BodyPartVolume.kt` | 신규 | `BodyPartVolume` data class |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/model/WorkoutVolume.kt` | 수정 | `val WorkoutEntry.weightVolumeKg: Int` 확장 추가 |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/usecase/SummarizeWorkoutVolumeUseCase.kt` | 수정 | private 확장을 지우고 `weightVolumeKg` 사용 |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/usecase/SummarizeBodyPartVolumeUseCase.kt` | 신규 | 부위별 셈 |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/usecase/GetWeeklyBodyPartVolumeUseCase.kt` | 신규 | 이번 주 구독 |
| `core/ui/components/src/main/java/kr/sdbk/bodyplan/core/ui/components/BodyPartVolumeCard.kt` | 신규 | 카드 + `volumeText` + Preview(값 있음·빈·에러) |
| `feature/workoutlog/impl/.../log/WorkoutLogContracts.kt` | 수정 | State에 `bodyPartVolumes` |
| `feature/workoutlog/impl/.../log/WorkoutLogViewModel.kt` | 수정 | 생성자 주입, `observeLog` collect에서 셈 |
| `feature/workoutlog/impl/.../log/composable/WorkoutLogView.kt` | 수정 | 첫 item에 카드, Preview 상태에 볼륨 |
| `feature/workoutlog/impl/.../calendar/WorkoutCalendarContracts.kt` | 수정 | State에 `weeklyVolumes`·`weeklyVolumeErrorMessage`, Intent에 `ClickRetryWeeklyVolume` |
| `feature/workoutlog/impl/.../calendar/WorkoutCalendarViewModel.kt` | 수정 | 생성자 주입, 별도 Job 구독, 재시도 |
| `feature/workoutlog/impl/.../calendar/composable/WorkoutCalendarView.kt` | 수정 | UiEvents 확장, 카드 배치, Preview |
| `core/domain/src/test/.../usecase/SummarizeBodyPartVolumeUseCaseTest.kt` | 신규 | 테스트 단계(test-engineer) |
| `core/domain/src/test/.../usecase/GetWeeklyBodyPartVolumeUseCaseTest.kt` | 신규 | 테스트 단계 |
| `feature/workoutlog/impl/src/test/.../log/WorkoutLogViewModelTest.kt` | 수정 | 헬퍼에 UseCase 주입, 볼륨 케이스 — 테스트 단계 |
| `feature/workoutlog/impl/src/test/.../calendar/WorkoutCalendarViewModelTest.kt` | 수정 | 헬퍼에 UseCase 주입, 주간 볼륨·실패·재시도 케이스 — 테스트 단계 |

### 구현 순서

1. **도메인** — 만드는 것: `BodyPartVolume`, `WorkoutEntry.weightVolumeKg`, `SummarizeBodyPartVolumeUseCase`, `GetWeeklyBodyPartVolumeUseCase`. 쓰는 것: `WorkoutEntry`, `IntensityType`, `AnalysisScopeKey.weekStart`, `WorkoutLogRepository.observeEntriesInRange`. 검증: `./gradlew :core:domain:test`(기존 `SummarizeWorkoutVolumeUseCaseTest` 통과).
2. **공용 카드** — 만드는 것: `BodyPartVolumeCard`, `volumeText`. 쓰는 것: 1의 `BodyPartVolume`, `BodyPart.label`·`color`, `BodyPlanCard`, `OutlinedActionButton`. 검증: `:app:compileDebugKotlin`.
3. **기록 화면** — 쓰는 것: 1의 `SummarizeBodyPartVolumeUseCase`, 2의 카드. 검증: `:app:compileDebugKotlin`.
4. **캘린더 화면** — 쓰는 것: 1의 `GetWeeklyBodyPartVolumeUseCase`, 2의 카드. 검증: `:app:compileDebugKotlin`.
5. 리뷰(code-reviewer: 로직·아키텍처·재사용)와 테스트(test-engineer: 위 테스트 4파일)를 동시에 위임. 통과 후 커밋 하나.

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| `SummarizeWorkoutVolumeUseCase` 내부 셈 → 공용 확장 | 홈 진척 카드(`GetProgressSummaryUseCase`) — 기존 테스트로 확인 |
| `WorkoutLogViewModel` 생성자 | `WorkoutLogViewModelTest` 헬퍼 |
| `WorkoutCalendarViewModel` 생성자 | `WorkoutCalendarViewModelTest` 헬퍼 |
| `WorkoutCalendarView` 레이아웃 | D에서 넣은 캘린더 접기 토글이 카드 위에서 그대로 동작 |

자기 대조: 통과 (대조 16/16)
- 고침: 수용 조건(기록 화면 실패 경로를 "해당 없음"이 아니라 기존 경로로 명시), 상태 계약(`weeklyVolumeErrorMessage` 초기값·재구독 시 초기화)
