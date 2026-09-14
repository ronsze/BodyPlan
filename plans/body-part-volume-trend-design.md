# 홈 부위별 볼륨 추이 — 최근 2주

**작성일**: 2026-09-14

## 설계

### 요약

홈 탭에 「부위별 볼륨 변화」 카드를 더한다. 최근 7일과 그 앞 7일의 부위별 무게 볼륨을 견줘 부위마다 `최근 볼륨 · 증감`을 한 줄씩 보이고, 늘어난 부위와 줄어든 부위를 묶은 한 줄 메시지를 카드 맨 위에 둔다.
셈은 단위 A의 `SummarizeBodyPartVolumeUseCase`를 재사용하고, 구독은 홈이 이미 열어 둔 `GetProgressSummaryUseCase`의 14일 `observeEntriesInRange`를 그대로 쓴다 — 새 구독을 열지 않는다.

### 배경

사용자가 구간(최근 7일 vs 앞 7일, 홈 진척 카드와 같은 기준)과 메시지 형식(카드 하나에 한 줄, 늘어난·줄어든 부위를 묶어서)을 추천안으로 확정했다. 부위별 행은 `부위 · 최근 7일 볼륨 · 증감(+1,240kg / -300kg / 변화 없음)`이고 두 구간 모두 무게 기록이 없는 부위는 뺀다.

"나머지도 문제없으면 다 진행"이라는 지시에 따라 이 계획은 승인 대기 없이 착수한다. 브랜치는 `feature/body-part-volume`(단위 A)에서 분기한다 — A의 도메인 심볼을 쓴다.

### 수용 조건

- [ ] 홈의 「지금 흐름」·「몸 구성」 카드 아래에 「부위별 볼륨 변화」 카드가 뜬다. 부제는 `최근 7일과 그 앞 7일을 견준 결과입니다.`
- [ ] 두 구간 중 한쪽에라도 무게 종목 기록이 있는 부위마다 `가슴  1,240kg  +300kg` 꼴 한 줄이 `BodyPart` 선언 순서로 뜬다. 증감은 `+300kg`(늘음, 강조색) / `-300kg`(줄음, 위험색) / `변화 없음`(같음, 기본색)이다.
- [ ] 메시지: 늘어난 부위만 있으면 `가슴·등 볼륨이 늘었어요`, 줄어든 부위만 있으면 `하체 볼륨이 줄었어요. 이번 주에 챙겨보세요`, 둘 다면 `가슴·등 볼륨이 늘고 하체 볼륨이 줄었어요`(리뷰 뒤 고침 — `{부위}는`은 받침 있는 부위에서 `등는`이 되어 조사를 뺐다), 전부 같으면 `모든 부위가 지난주와 비슷해요`. 부위 나열은 선언 순서, `·`로 잇는다.
- [ ] 빈 결과: 두 구간 모두 무게 기록이 없으면 행 없이 `최근 2주 무게 기록이 없습니다`만 뜬다.
- [ ] 조회 실패: 별도 경로 없음 — `GetProgressSummaryUseCase` 실패는 기존 홈 에러 화면(`불러오지 못했습니다` + `다시 시도`)이 덮는다.
- [ ] 저장 실패·권한: 해당 없음.
- [ ] 기존 「지금 흐름」 헤드라인·지표 3개와 「몸 구성」은 바뀌지 않는다 — `GetProgressSummaryUseCaseTest`·`HomeViewModelTest` 기존 케이스 통과.

### 비목표

- 헤드라인(`ProgressHeadline`) 판정에 부위별 추이를 넣지 않는다. 기존 다섯 축 그대로다.
- 부위별 추이를 8주 꺾은선으로 그리지 않는다 — 종목 추이 화면이 있다.
- 증감의 문턱을 두지 않는다. 볼륨은 정수라 1kg 차이도 변화다(`COUNT_THRESHOLD`와 같은 판단).
- `MetricRow`·`changeText`를 이 카드에 맞춰 일반화하지 않는다. 축이 `ProgressMetricKey`에 묶여 있다.

### 데이터 계약

**조회 (신규 없음)** — `GetProgressSummaryUseCase`가 이미 여는 `workoutLogRepository.observeEntriesInRange(previousFrom, today)` (`today - 13` ~ `today`). 그 결과를 `between(recentFrom, today)`·`between(previousFrom, recentFrom - 1)`로 갈라 넘긴다(같은 파일의 기존 private 확장).

`WorkoutEntry`에서 쓰는 필드: 단위 A의 `SummarizeBodyPartVolumeUseCase`와 같다(`bodyPart`, `intensityType`, `sets[]`; 나머지 미사용).

**신규 도메인 모델 `core/domain/model/BodyPartVolumeTrend.kt`**

```kotlin
/**
 * 한 부위의 두 구간 볼륨 비교. [changeKg] = recent − previous.
 * 두 구간 모두 무게 기록이 없는 부위는 만들어지지 않는다. 한쪽만 있으면 없는 쪽을 0으로 본다.
 */
data class BodyPartVolumeTrend(
    val bodyPart: BodyPart,
    val recentVolumeKg: Int,
    val changeKg: Int,
    val direction: ProgressDirection, // IMPROVING(>0) / WORSENING(<0) / STEADY(0). UNKNOWN은 쓰지 않는다
)
```

**기존 `ProgressSummary`에 필드 추가**: `val bodyPartVolumeTrends: List<BodyPartVolumeTrend> = emptyList()` → 홈 카드.

### 상태 계약

**UseCase `SummarizeBodyPartVolumeTrendUseCase` (`:core:domain`, 순수)**

- `@Inject constructor(summarizeBodyPartVolume: SummarizeBodyPartVolumeUseCase)`
- `operator fun invoke(recent: List<WorkoutEntry>, previous: List<WorkoutEntry>): List<BodyPartVolumeTrend>`
- 두 목록을 각각 `summarizeBodyPartVolume`으로 줄여 부위별로 맞춘다. 한쪽에만 있는 부위는 없는 쪽 0. 결과는 `BodyPart.entries` 순서.

**`GetProgressSummaryUseCase` 수정**

- 생성자에 `summarizeBodyPartVolumeTrend: SummarizeBodyPartVolumeTrendUseCase` 추가.
- `combine` 블록에서 `bodyPartVolumeTrends = summarizeBodyPartVolumeTrend(recentEntries.values.flatten(), previousEntries.values.flatten())`를 `ProgressSummary`에 담는다.

**화면 `Home`**: State·Intent·Effect 변경 없음 — `HomeState.summary`에 실려 온다.

### 화면 구성

**`BodyPartVolumeTrendCard` — 신규, `feature/home/impl/.../home/composable/BodyPartVolumeTrendCard.kt`**

```kotlin
@Composable
internal fun BodyPartVolumeTrendCard(trends: List<BodyPartVolumeTrend>, modifier: Modifier = Modifier)
```

- 트리: `BodyPlanCard`(재사용) → 제목 `BaseText("부위별 볼륨 변화", titleSmall, TextPrimary)` → `VerticalSpacer(4.dp)` → 부제 `BaseText(bodySmall, TextTertiary)` → `VerticalSpacer(16.dp)` → 분기:
  - `trends.isEmpty()`: `BaseText("최근 2주 무게 기록이 없습니다", bodyMedium, TextTertiary)`
  - 그 외: 메시지 `BaseText(titleMedium, TextPrimary)` → `VerticalSpacer(16.dp)` → `Column(spacedBy(12.dp))`에 행 반복
- 행: `Row` → 8dp 색 점(`bodyPart.color`) → `BaseText(bodyPart.label, bodyMedium, TextPrimary, padding start 8dp)` → `WeightSpacer()` → `BaseText(volumeText(recentVolumeKg), bodyMedium, TextSecondary)` → `BaseText(증감, bodyMedium, direction.valueColor, padding start 12dp)`
- `volumeText`는 단위 A에서 `core:ui:components`에 `internal`로 두었으므로 `public`으로 올린다(홈 impl이 `core:ui:components`를 의존한다).
- 증감 문자열·메시지: 같은 패키지 `home/VolumeTrendFormat.kt`에 `internal fun trendChangeText(trend: BodyPartVolumeTrend): String`(`String.format(Locale.US, "%+,dkg", changeKg)`, 0이면 `변화 없음`)과 `internal fun volumeTrendMessage(trends: List<BodyPartVolumeTrend>): String`.
- `direction.valueColor`는 기존 `ProgressFormat.kt`의 확장 재사용.
- 로딩·에러: 기존 홈 분기 그대로(카드는 `SummaryContent` 안). 빈: 위 문구. 권한: 해당 없음.

**`HomeView.kt`**: `SummaryContent`의 `BodyCompositionCard` 다음에 `BodyPartVolumeTrendCard(trends = summary.bodyPartVolumeTrends)`. Preview 두 개에 추이 예시·빈 목록.

**카피 원문**: `부위별 볼륨 변화` / `최근 7일과 그 앞 7일을 견준 결과입니다.` / `최근 2주 무게 기록이 없습니다` / `변화 없음` / `{부위들} 볼륨이 늘었어요` / `{부위들} 볼륨이 줄었어요. 이번 주에 챙겨보세요` / `{늘은 부위들} 볼륨이 늘고 {줄은 부위들} 볼륨이 줄었어요` / `모든 부위가 지난주와 비슷해요`

### 네비게이션

해당 없음.

### 버린 안

- 이번 주 vs 지난 주(월요일 시작): 주 초반엔 이번 주가 하루치뿐이라 늘 "퇴보"로 나온다. 채팅에서 버림.
- 부위마다 메시지 한 줄: 7줄이 되어 카드가 길어진다. 채팅에서 버림.
- 새 `GetBodyPartVolumeTrendUseCase`로 별도 구독: 홈이 이미 같은 14일 구간을 열고 있어 같은 구간을 두 번 연다. `GetProgressSummaryUseCase` 안에서 센다.
- 헤드라인에 부위 추이 반영: 부위 수만큼 표가 늘어 헤드라인이 볼륨에 치우친다.

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/domain/.../model/BodyPartVolumeTrend.kt` | 신규 | 모델 |
| `core/domain/.../model/ProgressSummary.kt` | 수정 | `bodyPartVolumeTrends` 필드 |
| `core/domain/.../usecase/SummarizeBodyPartVolumeTrendUseCase.kt` | 신규 | 두 구간 비교 |
| `core/domain/.../usecase/GetProgressSummaryUseCase.kt` | 수정 | 주입·`combine`에서 채움 |
| `core/ui/components/.../BodyPartVolumeCard.kt` | 수정 | `volumeText`를 public으로 |
| `feature/home/impl/.../home/VolumeTrendFormat.kt` | 신규 | `trendChangeText`·`volumeTrendMessage` |
| `feature/home/impl/.../home/composable/BodyPartVolumeTrendCard.kt` | 신규 | 카드 + Preview(값·빈) |
| `feature/home/impl/.../home/composable/HomeView.kt` | 수정 | 카드 배치, Preview |
| `core/domain/src/test/.../usecase/SummarizeBodyPartVolumeTrendUseCaseTest.kt` | 신규 | 테스트 단계 |
| `core/domain/src/test/.../usecase/GetProgressSummaryUseCaseTest.kt` | 수정 | 생성자·추이 케이스 — 테스트 단계 |
| `feature/home/impl/src/test/.../home/HomeViewModelTest.kt` | 수정 | 생성자 조립이 바뀌면 보수 — 테스트 단계 |
| `feature/home/impl/src/test/.../home/VolumeTrendFormatTest.kt` | 신규 | 메시지 4분기·증감 문자열 — 테스트 단계 |

### 구현 순서

1. **도메인** — 만드는 것: `BodyPartVolumeTrend`, `ProgressSummary.bodyPartVolumeTrends`, `SummarizeBodyPartVolumeTrendUseCase`, `GetProgressSummaryUseCase` 수정. 쓰는 것: A의 `SummarizeBodyPartVolumeUseCase`·`BodyPartVolume`, 기존 `ProgressDirection`. 검증: `./gradlew :core:domain:test`(기존 `GetProgressSummaryUseCaseTest`는 생성자 변경으로 깨질 수 있음 — 테스트 단계에서 보수).
2. **홈** — 만드는 것: `VolumeTrendFormat.kt`, `BodyPartVolumeTrendCard`, `HomeView` 배치; `volumeText` public. 쓰는 것: 1의 모델, `BodyPart.label`·`color`, `ProgressDirection.valueColor`. 검증: `:app:compileDebugKotlin`.
3. 리뷰(code-reviewer: 로직·아키텍처·재사용)와 테스트(test-engineer) 동시 위임. 통과 후 커밋 하나.

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| `ProgressSummary` 필드 추가(기본값 있음) | 홈 Preview·`HomeViewModelTest`의 `ProgressSummary(...)` 생성 |
| `GetProgressSummaryUseCase` 생성자 | `GetProgressSummaryUseCaseTest`·`HomeViewModelTest` 조립 |
| `volumeText` 가시성 | 단위 A 카드 — 동작 변화 없음 |

자기 대조: 통과 (대조 16/16)
- 고침: 상태 계약(`direction`에 UNKNOWN을 쓰지 않음을 명시), 화면 구성(`volumeText` 가시성 변경을 파일별 작업에 추가)
