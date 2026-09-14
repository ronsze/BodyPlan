# 주간 운동 목표 + 스트릭

**작성일**: 2026-09-14

## 설계

### 요약

프로필에 「주간 운동 목표」(주 1~7회)를 두고, 홈 맨 위에 이번 주 달성 게이지(`이번 주 3 / 4회`)와 연속 달성 주 수(`2주 연속 달성`), 한 줄 메시지를 보이는 카드를 더한다.
운동한 날은 기존 `observeBodyPartsInRange`로 세고, 주는 월요일 시작(`AnalysisScopeKey.weekStart`)이다. 목표가 없으면 카드는 정하라는 문구만 보인다.
프로필 스키마에 컬럼 하나가 늘어 DB 10→11 마이그레이션이 생긴다.

### 배경

사용자가 방향을 확정했다: 목표는 프로필 수정 화면에서만(온보딩 제외), 목표 없을 때 홈 카드는 문구만(이동 없음), 스트릭은 이번 주가 달성이면 세고 아직이면 깨지 않으며 최대 52주.

브랜치 `feature/weekly-goal`, `master`에서 분기. 병합·삭제까지 한 번에.

### 수용 조건

- [ ] 프로필 수정 화면 「목표 체중」 아래에 「주간 운동 목표」 숫자 칸(단위 `회`, 비워 둘 수 있음)이 뜬다. 1~7 밖의 값은 저장 시 버려 없음으로 저장한다(온보딩 화면에는 없다).
- [ ] 저장한 목표가 홈에 반영된다 — 홈 맨 위 「주간 목표」 카드에 `이번 주 {done} / {goal}회` 글자와 채움 비율 막대, `{n}주 연속 달성`(n=0이면 `아직 연속 달성이 없어요`), 메시지.
- [ ] 메시지: 달성(`done >= goal`) `이번 주 목표를 채웠어요!`, 남음 `{goal-done}번만 더 하면 목표예요`.
- [ ] 스트릭: 지난 주부터 거슬러 올라가며 `운동한 날 >= goal`인 주를 연속으로 센다. 이번 주가 달성이면 +1, 아니면 세지 않되 깨지도 않는다. 최대 52주.
- [ ] 목표가 없으면 카드에 `마이 탭에서 주간 목표를 정해 보세요`만 뜬다. 게이지·스트릭 없음.
- [ ] 운동한 날은 그 날 기록이 하나라도 있으면 1일이다(부위·종목 수 무관).
- [ ] 조회 실패: 목표 카드 조회가 실패하면 기존 홈 에러 화면(`불러오지 못했습니다` + `다시 시도`)이 덮는다 — 홈은 두 흐름을 하나로 합쳐 구독한다.
- [ ] 저장 실패: 프로필 저장 실패는 기존 토스트 그대로.
- [ ] 권한: 해당 없음.
- [ ] 마이그레이션: 10→11에서 `user_profile`에 `weeklyWorkoutGoal INTEGER` 컬럼이 더해지고 기존 행은 `NULL`이다. 백업 스냅샷은 `dbVersion`이 달라 이전 파일을 들이지 않는다(기존 규칙).
- [ ] 기존 홈 카드 셋(지금 흐름·몸 구성·부위별 볼륨 변화)과 프로필의 다른 칸은 그대로다.

### 비목표

- 온보딩에 목표 칸을 두지 않는다.
- 홈 카드에서 프로필로 이동하지 않는다.
- 목표 달성 알림·배지 이력을 두지 않는다.
- 스트릭 52주 상한을 넘겨 세지 않는다.
- 분석 프롬프트에 주간 목표를 싣지 않는다.

### 데이터 계약

**`UserProfile`(도메인)**: `val weeklyWorkoutGoal: Int? = null` 추가. `isEmpty`에 포함.

**`UserProfileEntity`(local)**: `val weeklyWorkoutGoal: Int? = null` 추가(`@Serializable` 기본값 — 스냅샷 역직렬화가 깨지지 않게).

**마이그레이션 `MIGRATION_10_11`**: `ALTER TABLE user_profile ADD COLUMN weeklyWorkoutGoal INTEGER`. `BodyPlanDatabase.version = 11`, `LocalModule.addMigrations`에 추가. 스키마 `11.json`은 빌드가 내보낸다.

**`UserProfileMapper`**: 양방향에 필드 추가. 저장 시 1..7 밖이면 `null`.

**조회 (신규 없음)**: `UserProfileRepository.observeProfile(): Flow<UserProfile>`, `WorkoutLogRepository.observeBodyPartsInRange(from, to): Flow<Map<LocalDate, Set<BodyPart>>>` — `from = weekStart(today).minusWeeks(52)`, `to = today`. 값(`Set<BodyPart>`)은 미사용, 키(날짜)만 센다.

**신규 도메인 모델 `core/domain/model/WeeklyGoalProgress.kt`**

```kotlin
/** 이번 주 목표 대비 진행. 목표가 없으면 만들어지지 않는다. */
data class WeeklyGoalProgress(val goalDays: Int, val doneDays: Int, val streakWeeks: Int) {
    val isAchieved: Boolean get() = doneDays >= goalDays
}
```

### 상태 계약

**UseCase `GetWeeklyGoalProgressUseCase` (`:core:domain`)**

- `@Inject constructor(userProfileRepository, workoutLogRepository, clock)`
- `operator fun invoke(): Flow<WeeklyGoalProgress?>` — `today`는 진입 시 한 번. `combine(observeProfile(), observeBodyPartsInRange(from, today))`. 목표가 없으면 `null`.
- 셈: `thisWeekStart = weekStart(today)`. 주별 운동일 = 날짜 키를 `weekStart`로 묶어 센 수. `doneDays = count(thisWeekStart)`. 스트릭: `w = thisWeekStart - 1주`부터 `count(w) >= goal`인 동안 1씩, 최대 `STREAK_MAX_WEEKS = 52`까지; 이번 주가 달성이면 +1(상한 안에서).

**화면 `Home` — State 필드 추가**: `weeklyGoal: WeeklyGoalProgress? = null`. 로딩 전 `null`과 "목표 없음" `null`은 로딩 판정이 기존 `summary == null`이라 화면에서 겹치지 않는다(둘이 한 방출로 함께 온다).

- `HomeViewModel`: 생성자에 `getWeeklyGoalProgress`. `observeSummary()`가 `combine(getProgressSummary(), getWeeklyGoalProgress())`를 한 Job으로 구독 — 실패·재시도 경로 하나.
- Intent·Effect 변경 없음.

**화면 `Profile`**: `ProfileInput.weeklyWorkoutGoal: String = ""`; `toProfile()`에서 `toIntOrNull()?.takeIf { it in 1..7 }`; `from()`에서 문자열화. State·Intent 변경 없음.

### 화면 구성

**`feature/home/impl/.../home/composable/WeeklyGoalCard.kt`(신규)**

```kotlin
@Composable
internal fun WeeklyGoalCard(progress: WeeklyGoalProgress?, modifier: Modifier = Modifier)
```

- `BodyPlanCard` → 제목 `BaseText("주간 목표", titleSmall, TextPrimary)` → `VerticalSpacer(16.dp)` →
  - `progress == null`: `BaseText("마이 탭에서 주간 목표를 정해 보세요", bodyMedium, TextTertiary)`
  - 그 외: 메시지 `BaseText(titleMedium, TextPrimary)` → `VerticalSpacer(12.dp)` → `Row`(`이번 주 {done} / {goal}회` bodyMedium TextSecondary, `WeightSpacer`, `{n}주 연속 달성` 또는 `아직 연속 달성이 없어요` bodySmall TextTertiary) → `VerticalSpacer(8.dp)` → 막대: `Box` 높이 8dp, 둥근 모서리, 바탕 `Border`, 채움 `Accent` 폭 `min(done/goal, 1f)`.
- Preview: 달성·진행 중·목표 없음.

**`HomeView.kt`**: `SummaryContent`의 첫 카드로 `WeeklyGoalCard(progress = state.weeklyGoal)`. `SummaryContent`가 `summary`만 받던 것을 `state`도 받게 바꾼다. Preview에 진행 예시.

**`ProfileForm.kt`**: 「목표 체중」 다음에 `NumberField("주간 운동 목표", input.weeklyWorkoutGoal, "회") { onChange(input.copy(weeklyWorkoutGoal = it)) }`. 자릿수 제한은 두지 않는다(범위 밖은 저장 시 버림).

**카피 원문**: `주간 목표` / `이번 주 {done} / {goal}회` / `{n}주 연속 달성` / `아직 연속 달성이 없어요` / `이번 주 목표를 채웠어요!` / `{n}번만 더 하면 목표예요` / `마이 탭에서 주간 목표를 정해 보세요` / `주간 운동 목표`

로딩·에러: 홈 기존 분기. 빈: 목표 없음 문구. 권한: 해당 없음.

### 네비게이션

해당 없음.

### 버린 안

- 온보딩에도 목표 칸: 온보딩이 길어진다. 채팅에서 버림.
- 홈 카드에서 프로필로 이동: 홈이 `feature:my:api`를 의존하게 된다. 채팅에서 버림.
- 이번 주를 스트릭에서 제외: 달성했는데 안 세면 억울하다. 채팅에서 버림.
- `ProgressSummary`에 목표 진행을 넣기: `GetProgressSummaryUseCase`가 이미 흐름 넷을 합친다. 별도 UseCase로 두고 홈 VM이 둘을 합친다.
- 운동일을 `observeEntriesInRange`로 세기: 52주치 세트까지 읽는다. 부위만 읽는 쿼리로 충분하다.

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/domain/.../model/UserProfile.kt` | 수정 | 필드·`isEmpty` |
| `core/domain/.../model/WeeklyGoalProgress.kt` | 신규 | 모델 |
| `core/domain/.../usecase/GetWeeklyGoalProgressUseCase.kt` | 신규 | 셈 |
| `core/local/.../entity/UserProfileEntity.kt` | 수정 | 컬럼 |
| `core/local/.../migration/Migrations.kt` | 수정 | `MIGRATION_10_11` |
| `core/local/.../BodyPlanDatabase.kt` | 수정 | version 11 |
| `core/local/.../di/LocalModule.kt` | 수정 | 마이그레이션 등록 |
| `core/data/.../mapper/UserProfileMapper.kt` | 수정 | 양방향 |
| `feature/home/impl/.../home/HomeContracts.kt` | 수정 | State 필드 |
| `feature/home/impl/.../home/HomeViewModel.kt` | 수정 | combine 구독 |
| `feature/home/impl/.../home/composable/WeeklyGoalCard.kt` | 신규 | 카드 |
| `feature/home/impl/.../home/composable/HomeView.kt` | 수정 | 배치·Preview |
| `feature/my/impl/.../profile/ProfileContracts.kt` | 수정 | `ProfileInput` 필드 |
| `feature/my/impl/.../profile/composable/ProfileForm.kt` | 수정 | 칸 추가 |
| 테스트: `GetWeeklyGoalProgressUseCaseTest`(신규), `HomeViewModelTest`·`ProfileViewModelTest`·`UserProfileRepositoryImplTest`·`ProfileInput` 관련(보수) | — | 테스트 단계 |

### 구현 순서

1. **저장소·도메인** — 만드는 것: 모델 필드, 엔티티·마이그레이션·매핑, `WeeklyGoalProgress`, `GetWeeklyGoalProgressUseCase`. 검증: `:app:compileDebugKotlin`, `:core:domain:test`.
2. **프로필** — `ProfileInput`·`ProfileForm`. 검증: `:app:compileDebugKotlin`.
3. **홈** — State·VM·카드·View. 검증: `:app:compileDebugKotlin`.
4. 리뷰(code-reviewer)·테스트(test-engineer: `GetWeeklyGoalProgressUseCase`, `HomeViewModel`, `ProfileInput`/`ProfileViewModel`, `UserProfileRepositoryImpl`) 동시 위임. 통과 후 커밋, 병합, 브랜치 삭제.

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| `UserProfile` 필드(기본값 있음) | 인바디 분석의 `updateProfile`(`copy`라 영향 없음), 온보딩 저장 |
| `HomeViewModel` 생성자·구독 | `HomeViewModelTest` |
| `ProfileInput` | 온보딩·프로필 수정 저장 |
| DB version | 기존 기기 업그레이드 경로(마이그레이션) |

자기 대조: 통과 (대조 16/16)
- 고침: 상태 계약(로딩 전 `null`과 목표 없음 `null` 구분 — `hasWeeklyGoal` 대신 `summary == null`로 로딩을 판정한다고 명시해 필드 하나로 정리), 수용 조건(범위 밖 값 저장 처리 명시)
