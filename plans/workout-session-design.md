# 운동 세션 모드 — 세트 체크 + 휴식 타이머

**작성일**: 2026-09-14

## 설계

### 요약

오늘 날짜의 기록 화면에 「운동 시작」을 두고, 누르면 같은 화면이 세션 모드가 된다. 세트마다 체크할 수 있고, 체크하면 하단에 휴식 타이머 바(기본 90초, ±30초, 건너뛰기)가 뜨며 끝나면 진동한다.
세션 동안 화면이 꺼지지 않고, 「운동 종료」를 누르면 `운동 종료 · 42분 · 볼륨 3,400kg` 토스트와 함께 평소 화면으로 돌아온다.
세트 완료 상태는 저장하지 않는다 — 세션은 화면 상태이고, 앱이 죽으면 세션도 끝난다. 스키마 변경 없음.

### 배경

사용자가 방향을 확정했다: 별도 화면 없이 기록 화면이 모드 전환, 완료 상태 비저장, 휴식 기본 90초·±30초·건너뛰기·진동, 화면 꺼짐 방지, 설정 화면은 두지 않음.

세션은 오늘에만 연다. 어제 기록도 편집은 되지만 "지금 운동 중"이 아니므로 세션은 뜻이 없다.

브랜치는 `feature/previous-record-pr`(단위 2)에서 `feature/workout-session`으로 분기한다 — 같은 화면·컴포넌트를 고친다.

### 수용 조건

- [ ] 오늘 날짜의 기록 화면 하단에 「운동 시작」 버튼이 뜬다. 어제·과거 날짜에는 뜨지 않는다.
- [ ] 「운동 시작」을 누르면 각 세트 줄 왼쪽에 체크 칸이 생기고, 하단 바에 `운동 중 · 0분`이 뜨며 1분마다 갱신된다. 상단 「운동 추가」·루틴 불러오기·수정·삭제는 세션 중에도 그대로 된다.
- [ ] 세트 줄을 누르면 체크가 토글된다. 체크가 켜지면 휴식 타이머가 90초로 시작해 매초 줄고, 바에 `휴식 1:30`과 `-30초` `+30초` `건너뛰기`가 뜬다. 체크를 끄는 것은 타이머에 영향이 없다.
- [ ] 휴식 중 다른 세트를 체크하면 타이머가 90초로 다시 시작한다.
- [ ] `-30초`로 0 이하가 되면 즉시 끝난다. `+30초`는 제한 없이 더한다. `건너뛰기`는 진동 없이 끝낸다.
- [ ] 타이머가 0이 되면 기기가 짧게 진동하고(진동 권한은 설치 시 자동) 바가 `운동 중 · N분`으로 돌아온다.
- [ ] 세션 중에는 화면이 꺼지지 않는다. 세션이 끝나거나 화면을 떠나면 원래대로다.
- [ ] 「운동 종료」를 누르면 토스트 `운동 종료 · {N}분 · 볼륨 {1,240}kg`(볼륨은 그날 무게 볼륨 합, 기존 `bodyPartVolumes`)가 뜨고 체크·타이머·바가 사라진다.
- [ ] 세션 중 기록을 삭제하면 그 기록의 체크는 사라지고, 세트를 고쳐 개수가 줄면 없는 번호의 체크는 무시된다(체크 키는 기록 id + 세트 번호).
- [ ] 화면을 떠났다 돌아오면(뒤로 갔다 다시 진입) 세션은 없다 — ViewModel이 새로 만들어진다. 같은 ViewModel 안에서의 화면 회전은 세션이 유지된다.
- [ ] 조회 실패·저장 실패·권한: 기존 경로 그대로. 세션은 조회 실패 화면에서는 시작할 수 없다(버튼이 `LogContent` 밖 하단 Column에 있어도 `errorMessage != null`이면 숨긴다).
- [ ] 루틴 상세의 세트 줄은 체크 없이 지금과 같다.

### 비목표

- 세트 완료를 DB에 저장하지 않는다. 세션 이력 화면도 없다.
- 휴식 기본값 설정 화면을 두지 않는다. 90초 고정이다.
- 알림(백그라운드에서 타이머 끝 알림)을 띄우지 않는다. 진동은 앱이 뒤에 있어도 화면이 살아 있으면 울린다 — 리뷰 뒤 실제 동작에 맞춰 고침.
- 세션 중 기록 편집 흐름을 바꾸지 않는다 — 기존 편집 화면으로 간다.
- 루틴 상세·어제 날짜에 세션을 두지 않는다.

### 데이터 계약

저장소·DAO 변경 없음. `WorkoutLogState`가 이미 드는 `entries`(기록 id·세트 목록)·`bodyPartVolumes`(볼륨 합)만 쓴다.

`app/src/main/AndroidManifest.xml`에 `<uses-permission android:name="android.permission.VIBRATE" />` 추가(일반 권한, 런타임 요청 없음).

### 상태 계약

**신규 UI 모델 `core/ui/components/WorkoutSetKey.kt`**

```kotlin
/** 세션 중 체크한 세트를 가리키는 키. 세트에는 id가 없어 기록 id와 번호로 가리킨다. */
data class WorkoutSetKey(val entryId: Long, val setIndex: Int)
```

`core:ui:components`에 두는 이유: `workoutEntryGroups`가 파라미터로 받고 feature State가 든다. 저장하지 않으므로 도메인 모델이 아니다.

**신규 화면 모델 `feature/workoutlog/impl/.../log/WorkoutSession.kt`**

```kotlin
internal data class RestTimer(val remainingSeconds: Int)

internal data class WorkoutSession(
    val startedAtMillis: Long,
    val elapsedSeconds: Long = 0L,
    val completedSets: Set<WorkoutSetKey> = emptySet(),
    val rest: RestTimer? = null,
)
```

**화면 `WorkoutLog` — State 필드 추가**

| 필드 | 타입 | 초기값 | 출처 |
|---|---|---|---|
| `canStartSession` | `Boolean` | `false` | `initializeData`에서 `date == LocalDate.now(clock)` |
| `session` | `WorkoutSession?` | `null` | 세션 Intent |

파생: `val isSessionActive get() = session != null`.

**Intent 추가**

| Intent | 처리 |
|---|---|
| `ClickStartSession` | `canStartSession && session == null`이면 `session = WorkoutSession(startedAtMillis = clock.millis())`, 틱 Job 시작 |
| `ClickEndSession` | 틱 Job 취소, `ShowMessage("운동 종료 · ${elapsed/60}분 · 볼륨 ${volumeText}")`, `session = null` |
| `ToggleSetCompleted(key: WorkoutSetKey)` | 세션 중이면 `completedSets` 토글. 켜질 때 `rest = RestTimer(REST_SECONDS)` |
| `ClickAdjustRest(deltaSeconds: Int)` | `rest`가 있으면 `remaining + delta`. 0 이하면 `rest = null`(진동 없음) |
| `ClickSkipRest` | `rest = null` |

**Effect 추가**: `VibrateRestEnd` — 타이머가 0에 닿을 때 한 번.

**틱 Job**: 세션 시작 시 `viewModelScope.launch { while (true) { delay(1_000); tick() } }`. `tick()`은 `elapsedSeconds = (clock.millis() - startedAtMillis) / 1000`, `rest`가 있으면 `remaining - 1`; 0에 닿으면 `rest = null` + `VibrateRestEnd`. 세션 종료 시 취소. 시각은 주입한 `Clock`으로만 읽는다.

**ViewModel 생성자**: `clock: Clock` 추가(`DataModule`이 제공).

**볼륨 문구**: `volumeText(bodyPartVolumes.sumOf { it.weightVolumeKg })` — `core:ui:components`의 기존 함수 재사용.

### 화면 구성

**`core/ui/components/WorkoutEntryGroups.kt` — `workoutEntryGroups` 파라미터 추가**

```kotlin
completedSets: Set<WorkoutSetKey>? = null,        // null이면 세션 아님 — 체크 칸을 그리지 않는다
onToggleSet: (WorkoutSetKey) -> Unit = {},
```

- `EntrySets` → `SetRow`에 `checked: Boolean?`·`onToggle`을 내려, `checked != null`이면 줄 왼쪽에 `Checkbox`(Material3, `colors = CheckboxDefaults.colors(checkedColor = Accent)`)를 두고 줄 전체를 `clickable`로 토글한다. 체크된 줄의 요약 글자는 `TextTertiary`.
- 루틴 상세는 기본값이라 변화 없음.

**`WorkoutLogView.kt`**

- `LogContent`: `workoutEntryGroups(..., completedSets = state.session?.completedSets, onToggleSet = uiEvents.onToggleSet)`.
- 하단 Column(`errorMessage == null`일 때만 그린다):
  - 세션 중: `SessionBar(session, uiEvents)` → 그 아래 기존 버튼들 → 마지막에 `PrimaryButton(text = "운동 종료")`(재사용, `BodyPlanButtons.kt` — `text, onClick, modifier, enabled`).
  - 세션 아님 & `canStartSession`: 기존 버튼들 위에 `PrimaryButton(text = "운동 시작")`.
- `SessionBar`(신규, `log/composable/SessionBar.kt`): `BodyPlanCard` 안 한 줄.
  - 휴식 중: `BaseText("휴식 ${m}:${ss}", titleMedium, Accent)` → `WeightSpacer` → `OutlinedActionButton`이 아니라 글자 액션 셋 `-30초`·`+30초`·`건너뛰기`(`BaseText` + `clickable`, bodySmall, Accent/TextTertiary)
  - 휴식 아님: `BaseText("운동 중 · ${elapsed/60}분", titleMedium, TextPrimary)`
- 화면 꺼짐 방지: `WorkoutLogView`에서 `val view = LocalView.current; DisposableEffect(state.isSessionActive) { view.keepScreenOn = state.isSessionActive; onDispose { view.keepScreenOn = false } }`.
- 진동: `CollectEffect`에서 `VibrateRestEnd` → `context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(500, DEFAULT_AMPLITUDE))`.
- `WorkoutLogUiEvents`에 `onClickStartSession`, `onClickEndSession`, `onToggleSet`, `onClickAdjustRest: (Int) -> Unit`, `onClickSkipRest` 추가.
- Preview: 세션 중(체크 하나·휴식 중) 상태 하나 추가.

**카피 원문**: `운동 시작` / `운동 종료` / `운동 중 · {N}분` / `휴식 {m}:{ss}` / `-30초` / `+30초` / `건너뛰기` / `운동 종료 · {N}분 · 볼륨 {1,240}kg`

로딩·빈·에러·권한: 기존 분기 그대로. 빈 기록에서도 세션은 시작할 수 있다(추가하면서 체크). 진동 권한은 일반 권한이라 흐름 없음.

### 네비게이션

해당 없음.

### 버린 안

- 별도 세션 화면: 기록 추가·수정 흐름을 다시 만들어야 한다. 채팅에서 버림.
- 완료 상태 DB 저장: 스키마 변경과 "언제 초기화하나"가 생긴다. 채팅에서 버림.
- 세션 상태를 `rememberSaveable`로 View에: 타이머·진동 판정이 Compose에 들어가고 테스트가 안 된다. ViewModel에 둔다.
- 타이머를 View의 `LaunchedEffect`로: 위와 같다.
- 세트 키를 `WorkoutSet` 값으로: 같은 무게·횟수 세트가 둘이면 구분이 안 된다. 기록 id + 번호.

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `app/src/main/AndroidManifest.xml` | 수정 | VIBRATE 권한 |
| `core/ui/components/.../WorkoutSetKey.kt` | 신규 | 키 모델 |
| `core/ui/components/.../WorkoutEntryGroups.kt` | 수정 | 체크 파라미터·`SetRow` 체크박스·Preview |
| `feature/workoutlog/impl/.../log/WorkoutSession.kt` | 신규 | `WorkoutSession`·`RestTimer` |
| `feature/workoutlog/impl/.../log/WorkoutLogContracts.kt` | 수정 | State 필드·Intent 5개·Effect 1개 |
| `feature/workoutlog/impl/.../log/WorkoutLogViewModel.kt` | 수정 | `Clock` 주입, `canStartSession`, 세션·틱·휴식 처리 |
| `feature/workoutlog/impl/.../log/composable/SessionBar.kt` | 신규 | 바 |
| `feature/workoutlog/impl/.../log/composable/WorkoutLogView.kt` | 수정 | 버튼·바·체크 연결·keepScreenOn·진동·Preview |
| `feature/workoutlog/impl/src/test/.../log/WorkoutLogViewModelTest.kt` | 수정 | 헬퍼에 `Clock`, 세션 케이스 — 테스트 단계 |

### 구현 순서

1. **공용 컴포넌트** — 만드는 것: `WorkoutSetKey`, `workoutEntryGroups` 체크 파라미터. 검증: `:app:compileDebugKotlin`.
2. **ViewModel** — 만드는 것: `WorkoutSession`, Contracts, VM 세션 로직. 쓰는 것: 1의 `WorkoutSetKey`, `volumeText`, `Clock`. 검증: `:app:compileDebugKotlin`.
3. **View** — 만드는 것: `SessionBar`, 버튼·연결·keepScreenOn·진동, 매니페스트. 쓰는 것: 2의 State·UiEvents. 검증: `:app:compileDebugKotlin`.
4. 리뷰(code-reviewer: 로직·아키텍처·재사용·컨벤션)와 테스트(test-engineer: `WorkoutLogViewModel`) 동시 위임. 통과 후 커밋 하나.

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| `workoutEntryGroups` 시그니처(기본값 있음) | 루틴 상세 |
| `WorkoutLogViewModel` 생성자 | `WorkoutLogViewModelTest` 헬퍼 |
| `WorkoutLogView` 하단 Column | 어제 날짜(시작 버튼 없음)·과거 날짜(분석 버튼만) 화면 |

자기 대조: 통과 (대조 16/16)
- 고침: 수용 조건(에러 화면에서 세션 시작 불가·화면 이탈 시 세션 소멸 명시), 상태 계약(`-30초`로 0 이하일 때 진동 없음을 Intent 표에 명시)
