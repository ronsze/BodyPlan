# 이전 기록 자동 채움 + 개인 기록(PR)

**작성일**: 2026-09-14

## 설계

### 요약

기록 작성 화면에서 종목을 고르면 그 종목의 가장 최근 기록의 세트가 그대로 채워진다(신규 작성, 일지 대상만).
기록 화면의 종목 카드에는 그날 그 종목의 한 세트 최고값이 이전 모든 기록을 넘겼을 때 `PR` 배지가 붙고, 저장 직후 `벤치프레스 최고 무게 갱신!` 토스트가 뜬다.
PR은 저장하지 않고 표시 시점에 계산한다. 이전 최고값은 DAO 집계 쿼리 하나로 읽는다.

### 배경

사용자가 방향을 확정했다: 자동 채움은 신규 작성·일지 대상만(수정·루틴 항목 제외), PR은 강도 축별 한 세트 최고값(무게→최고 무게, 각도→최고 횟수, 시간→최장 시간)을 표시 시점에 계산, 배지는 기록 화면 종목 카드, 저장 직후 토스트.

이전 기록이 하나도 없는 종목의 첫 기록은 PR이 아니다 — "갱신"이라는 말대로 견줄 것이 있어야 한다. 처음 쓰는 사용자의 모든 카드에 배지가 붙는 것을 막는다.

브랜치는 `master`에서 `feature/previous-record-pr`로 분기한다. 세션 모드(다음 단위)는 이 브랜치 위에 쌓는다.

### 수용 조건

- [ ] 일지 대상 신규 작성에서 종목을 고르면, 그 종목의 가장 최근 기록(그 날짜 이하)의 세트가 같은 개수·값으로 채워진다. 이전 기록이 없으면 지금처럼 기본 세트 1개다.
- [ ] 수정 진입(`editingEntryId != null`)과 루틴 항목 편집에서는 자동 채움이 일어나지 않는다. 최근 기록의 강도 축이 지금 종목의 축과 다르면(종목 축을 바꾼 경우) 채우지 않는다 — 리뷰 뒤 추가.
- [ ] 자동 채움 조회가 실패하면 기본 세트 1개가 그대로 남고 화면은 에러로 바뀌지 않는다 — 채움은 편의이지 필수가 아니다.
- [ ] 종목을 빠르게 두 번 바꿨을 때 먼저 고른 종목의 조회 결과가 나중 종목의 세트를 덮지 않는다.
- [ ] 기록 화면에서 그날 종목의 최고값(무게 종목은 세트 무게, 각도 종목은 세트 횟수, 시간 종목은 세트 시간)이 그 날짜 이전 모든 기록의 최고값보다 크면 그 종목 카드 제목 옆에 `PR` 배지가 뜬다. 같으면 뜨지 않는다.
- [ ] 이전 기록이 없는 종목은 배지가 뜨지 않는다. 다른 축으로 잰 이전 기록만 있어도 마찬가지다 — 리뷰 뒤 추가.
- [ ] 과거 기록을 고쳐 최고값이 바뀌면 다른 날의 배지도 따라 바뀐다(표시 시점 계산).
- [ ] 일지 대상으로 신규 저장·수정 저장이 성공했을 때 그 기록의 최고값이 그 날짜 이전 최고값을 넘으면 토스트 `{종목명} {최고 무게|최고 횟수|최장 시간} 갱신!`이 뜬다. 넘지 않으면 토스트 없음. 루틴 대상은 토스트 없음.
- [ ] 조회 실패(기록 화면): 최고값 조회가 실패하면 기존 `observeLog` 실패와 같이 `불러오지 못했습니다` + `다시 시도`. 두 흐름을 하나로 합치므로 에러 경로도 하나다.
- [ ] 저장 실패: 기존과 같이 `저장하지 못했습니다` 토스트. PR 판정은 저장 성공 뒤에만 한다.
- [ ] 권한: 해당 없음.
- [ ] 루틴 상세의 종목 카드는 배지 없이 지금과 같다.

### 비목표

- 1RM 추정을 넣지 않는다.
- 하루 총볼륨 PR·기간별 PR 목록 화면을 만들지 않는다.
- PR을 DB에 저장하지 않는다.
- 루틴 항목 편집의 자동 채움을 하지 않는다 — 루틴은 기준값이라 지난 기록에 끌려가면 안 된다.
- `WorkoutOptions`의 선택지 밖 값(예: 지운 기록의 33kg)이 자동 채움으로 들어오는 경우를 따로 다루지 않는다 — 휠 피커가 가장 가까운 눈금으로 보이는 기존 동작 그대로다.

### 데이터 계약

**신규 DAO 쿼리 `core/local/.../dao/WorkoutEntryDao.kt`**

```kotlin
/** 종목·축마다 그 날짜 이전 세트의 최고 강도값·최고 횟수. 리뷰 뒤 축(`e.intensityType`)까지 갈랐다 — 종목의 축을 바꾸면 옛 축 기록은 견줄 대상이 아니다. */
@Query(
    "SELECT e.exerciseId AS exerciseId, e.intensityType AS intensityType, MAX(s.intensityValue) AS maxIntensityValue, MAX(s.repeatCount) AS maxRepeatCount " +
        "FROM workout_entry e JOIN workout_set s ON s.entryId = e.id " +
        "WHERE e.dateEpochDay < :beforeEpochDay GROUP BY e.exerciseId, e.intensityType",
)
fun observeBestBefore(beforeEpochDay: Long): Flow<List<ExerciseBestRow>>

/** 그 종목의 가장 최근 기록 하나. 같은 날이면 나중에 만든 것. */
@Transaction
@Query(
    "SELECT * FROM workout_entry WHERE exerciseId = :exerciseId AND dateEpochDay <= :untilEpochDay " +
        "ORDER BY dateEpochDay DESC, createdAtMillis DESC, id DESC LIMIT 1",
)
suspend fun getLatestByExercise(exerciseId: Long, untilEpochDay: Long): WorkoutEntryWithSets?
```

조회 전용 POJO `core/local/.../entity/ExerciseBestRow.kt`: `data class ExerciseBestRow(val exerciseId: Long, val intensityType: String, val maxIntensityValue: Int, val maxRepeatCount: Int)`.

`workout_set`의 `intensityValue`·`repeatCount`, `workout_entry`의 `exerciseId`·`dateEpochDay`·`createdAtMillis`·`id`만 쓴다. 스키마 변경 없음.

**신규 도메인 모델 `core/domain/model/ExerciseBest.kt`**

```kotlin
/** 한 종목이 한 축으로 잰 지난 최고값. 축마다 무엇이 최고인지가 달라 둘 다 든다. */
data class ExerciseBest(val exerciseId: Long, val intensityType: IntensityType, val maxIntensityValue: Int, val maxRepeatCount: Int)

/** 이 축에서 PR을 가르는 값 — 각도만 횟수, 나머지는 강도값. */
val ExerciseBest.recordValue: Int
fun List<WorkoutSet>.recordValue(type: IntensityType): Int   // 같은 기준으로 최댓값. 비면 0
val WorkoutEntry.recordValue: Int
/** 그 종목을 지금 축으로 잰 지난 최고값. 다른 축 기록만 있으면 null — 견줄 것이 없다. */
fun List<ExerciseBest>.previousOf(exerciseId: Long, type: IntensityType): Int?
```

`ExerciseBestRow → ExerciseBest` 매핑은 `core/data/.../mapper/WorkoutMapper.kt`에 둔다.

**`WorkoutLogRepository`에 추가**

| 심볼 | 시그니처 | 귀착지 |
|---|---|---|
| `observeBestBefore` | `(date: LocalDate): Flow<List<ExerciseBest>>` | PR 판정 |
| `getLatestEntry` | `suspend (exerciseId: Long, until: LocalDate): WorkoutEntry?` | 자동 채움. `sets`만 쓴다 |

`FakeWorkoutLogRepository`(feature 테스트)·`WorkoutLogRepositoryImplTest`의 fake DAO에도 구현을 더한다.

### 상태 계약

**UseCase `FindPersonalRecordsUseCase` (`:core:domain`, 순수)**

- `@Inject constructor()`
- `operator fun invoke(entries: List<WorkoutEntry>, bestBefore: List<ExerciseBest>): Set<Long>` — PR을 세운 `exerciseId` 집합.
- 종목마다 그날 기록들의 `recordValue` 최댓값을 `bestBefore.previousOf(exerciseId, type)`과 견준다. 이전 값이 없거나 다른 축뿐이면 PR 아님. 큰 경우만 PR(같으면 아님).

**UseCase `ObserveWorkoutLogUseCase` (`:core:domain`)**

- `@Inject constructor(workoutLogRepository: WorkoutLogRepository, findPersonalRecords: FindPersonalRecordsUseCase)`
- `operator fun invoke(date: LocalDate): Flow<WorkoutLogWithRecords>`
- `combine(observeLog(date), observeBestBefore(date))`로 만든다. 실패는 Flow로 전파.
- 신규 모델 `core/domain/model/WorkoutLogWithRecords.kt`: `data class WorkoutLogWithRecords(val log: WorkoutLog, val personalRecordExerciseIds: Set<Long>)`

**UseCase `IsPersonalRecordUseCase` (`:core:domain`)**

- `@Inject constructor(workoutLogRepository: WorkoutLogRepository)`
- `suspend operator fun invoke(date: LocalDate, exerciseId: Long, type: IntensityType, sets: List<WorkoutSet>): Boolean` — `observeBestBefore(date).first().previousOf(exerciseId, type)`과 견준다. 저장 직후 토스트가 쓴다. 판정 규칙은 `FindPersonalRecordsUseCase`와 같아야 하므로 `WorkoutEntry.recordValue`·`ExerciseBest.recordValue` 확장을 공유한다.

**화면 `WorkoutLog` — State 필드 추가**

| 필드 | 타입 | 초기값 | 출처 |
|---|---|---|---|
| `personalRecordExerciseIds` | `Set<Long>` | `emptySet()` | `ObserveWorkoutLogUseCase` collect |

- ViewModel의 `observeLog()`가 `workoutLogRepository.observeLog` 대신 `observeWorkoutLog(date)`를 구독한다. 생성자에 `observeWorkoutLog: ObserveWorkoutLogUseCase` 추가. `workoutLogRepository`는 삭제·메모 저장에 계속 쓴다.
- Intent·Effect 변경 없음.

**화면 `WorkoutEntryEdit`**

- State 변경 없음. 자동 채움은 `sets`·`nextSetInputId`를 갱신한다.
- ViewModel 생성자에 `isPersonalRecord: IsPersonalRecordUseCase` 추가.
- `selectExercise(id)`: 지금처럼 기본 세트 1개를 먼저 넣은 뒤, `target is Log && editingEntryId == null`이면 `viewModelScope.launch`로 `getLatestEntry(id, target.date)`를 읽어 `selectedExercise?.id == id`일 때만 `sets`를 그 기록의 세트로 바꾼다(`SetInput.id`는 `nextSetInputId`부터 이어 붙인다). 실패는 무시. 앞선 조회 Job은 `prefillJob?.cancel()`로 끊는다.
- `save()` 성공 뒤: `target is Log`면 `isPersonalRecord(date, exercise.id, exercise.intensityType, sets)`가 true일 때 `ShowMessage("${exercise.name} ${type.recordLabel} 갱신!")`을 `GoBack` 앞에 낸다. 판정 실패는 무시하고 `GoBack`만 낸다.
- Intent·Effect 변경 없음.

### 화면 구성

**`core/ui/components/WorkoutEntryGroups.kt` — `workoutEntryGroups` 파라미터 추가**

```kotlin
fun LazyListScope.workoutEntryGroups(
    entries: List<WorkoutEntry>,
    isEditable: Boolean,
    expansion: WorkoutEntryGroupExpansion,
    onClickEntry: (Long) -> Unit,
    onClickDeleteEntry: (Long) -> Unit,
    personalRecordExerciseIds: Set<Long> = emptySet(),
)
```

- `ExerciseGroupCard` 헤더: 종목 이름 `BaseText` 오른쪽, `세트 N개` 왼쪽에 `Badge(text = "PR")`(재사용, `core/designsystem/component/SelectableChip.kt` — `text: String, modifier: Modifier = Modifier`)를 `group.first().exerciseId in personalRecordExerciseIds`일 때만 둔다. 이름이 길면 배지가 먼저 자리를 잡도록 이름 `BaseText`는 `weight(1f)` 그대로.
- Preview에 PR 있는 종목 하나.

**`WorkoutLogView.kt`**: `workoutEntryGroups(..., personalRecordExerciseIds = state.personalRecordExerciseIds)`. Preview 상태에 집합 하나.

**`RoutineDetailView.kt`**: 수정 없음(기본값).

**`WorkoutEntryEditView.kt`**: 수정 없음 — 세트가 State로 들어오면 지금 화면이 그린다.

**문구 `feature/workoutlog/impl/.../entryedit/RecordLabel.kt`(신규)**: `internal val IntensityType.recordLabel: String` = WEIGHT→`최고 무게`, ANGLE→`최고 횟수`, DURATION→`최장 시간`. 편집 화면 토스트가 쓴다.

**카피 원문**: `PR` / `{종목명} 최고 무게 갱신!` / `{종목명} 최고 횟수 갱신!` / `{종목명} 최장 시간 갱신!`

로딩·빈·에러·권한: 기록 화면은 기존 분기 그대로. 편집 화면은 자동 채움 중 표시가 없다(기본 세트가 먼저 보이고 값이 바뀐다).

### 네비게이션

해당 없음.

### 버린 안

- PR을 저장 시 플래그로 DB에 기록: 과거 기록을 고치면 어긋난다. 채팅에서 버림.
- PR을 기록(entry) 단위로: 같은 종목을 하루에 두 건 적으면 카드가 종목 단위라 배지 자리가 없다. 종목·날짜 단위로 낸다.
- 첫 기록도 PR: 처음 쓰는 사용자의 모든 카드에 배지가 붙는다.
- ViewModel에서 `observeLog`와 `observeBestBefore`를 직접 `combine`: 판정 로직이 VM에 들어간다. UseCase로 묶어 기록 화면 VM은 구독만 한다.
- 자동 채움을 `selectExercise` 안에서 `suspend`로 기다린 뒤 한 번에 반영: 조회가 느리면 종목을 골랐는데 세트가 안 보인다. 기본 세트를 먼저 보이고 덮는다.

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/local/.../entity/ExerciseBestRow.kt` | 신규 | 조회 POJO |
| `core/local/.../dao/WorkoutEntryDao.kt` | 수정 | `observeBestBefore`, `getLatestByExercise` |
| `core/domain/.../model/ExerciseBest.kt` | 신규 | 모델 + `recordValue` 확장 둘 |
| `core/domain/.../model/WorkoutLogWithRecords.kt` | 신규 | 모델 |
| `core/domain/.../repository/WorkoutLogRepository.kt` | 수정 | 두 메서드 추가 |
| `core/data/.../mapper/WorkoutMapper.kt` | 수정 | `ExerciseBestRow.toDomain()` |
| `core/data/.../repository/WorkoutLogRepositoryImpl.kt` | 수정 | 두 메서드 구현 |
| `core/domain/.../usecase/FindPersonalRecordsUseCase.kt` | 신규 | 순수 판정 |
| `core/domain/.../usecase/ObserveWorkoutLogUseCase.kt` | 신규 | combine |
| `core/domain/.../usecase/IsPersonalRecordUseCase.kt` | 신규 | 저장 직후 판정 |
| `core/ui/components/.../WorkoutEntryGroups.kt` | 수정 | 파라미터·배지·Preview |
| `feature/workoutlog/impl/.../log/WorkoutLogContracts.kt` | 수정 | State 필드 |
| `feature/workoutlog/impl/.../log/WorkoutLogViewModel.kt` | 수정 | UseCase 구독 |
| `feature/workoutlog/impl/.../log/composable/WorkoutLogView.kt` | 수정 | 파라미터 연결, Preview |
| `feature/workoutlog/impl/.../entryedit/RecordLabel.kt` | 신규 | `recordLabel` |
| `feature/workoutlog/impl/.../entryedit/WorkoutEntryEditViewModel.kt` | 수정 | 자동 채움, 저장 후 토스트 |
| `feature/workoutlog/impl/src/test/.../fake/FakeWorkoutLogRepository.kt` | 수정 | 두 메서드 — 테스트 단계 |
| `core/data/src/test/.../repository/WorkoutLogRepositoryImplTest.kt` + fake DAO | 수정 | 두 메서드 — 테스트 단계 |
| `core/domain/src/test/.../usecase/{FindPersonalRecords,ObserveWorkoutLog,IsPersonalRecord}UseCaseTest.kt` | 신규 | 테스트 단계 |
| `feature/workoutlog/impl/src/test/.../log/WorkoutLogViewModelTest.kt` | 수정 | 생성자·PR 케이스 — 테스트 단계 |
| `feature/workoutlog/impl/src/test/.../entryedit/WorkoutEntryEditViewModelTest.kt` | 수정 | 생성자·자동 채움·토스트 케이스 — 테스트 단계 |

### 구현 순서

1. **저장소** — 만드는 것: `ExerciseBestRow`, DAO 쿼리 둘, `ExerciseBest`(+확장), `WorkoutLogWithRecords`, Repository 인터페이스·구현·매핑. 쓰는 것: 기존 `WorkoutEntryWithSets`, `toDomain`. 검증: `:app:compileDebugKotlin`(Room 쿼리 검증 포함).
2. **도메인** — 만드는 것: `FindPersonalRecordsUseCase`, `ObserveWorkoutLogUseCase`, `IsPersonalRecordUseCase`. 쓰는 것: 1의 심볼. 검증: `./gradlew :core:domain:test`.
3. **기록 화면** — 만드는 것: `workoutEntryGroups` 파라미터·배지, State 필드, VM 구독 교체, View 연결. 쓰는 것: 2의 `ObserveWorkoutLogUseCase`, `Badge`. 검증: `:app:compileDebugKotlin`.
4. **편집 화면** — 만드는 것: `recordLabel`, 자동 채움, 저장 후 토스트. 쓰는 것: 1의 `getLatestEntry`, 2의 `IsPersonalRecordUseCase`. 검증: `:app:compileDebugKotlin`.
5. 리뷰(code-reviewer: 로직·아키텍처·재사용)와 테스트(test-engineer) 동시 위임. 통과 후 커밋 하나.

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| `WorkoutLogRepository` 인터페이스 | `FakeWorkoutLogRepository`(feature 테스트), `WorkoutLogRepositoryImplTest`의 fake DAO, `SnapshotStore`는 무관 |
| `workoutEntryGroups` 시그니처(기본값 있음) | 루틴 상세 화면 |
| `WorkoutLogViewModel` 생성자 | `WorkoutLogViewModelTest` 헬퍼 |
| `WorkoutEntryEditViewModel` 생성자 | `WorkoutEntryEditViewModelTest` 헬퍼, 루틴 항목 편집(자동 채움·토스트가 일어나지 않아야 함) |

자기 대조: 통과 (대조 16/16)
- 고침: 수용 조건(같은 값이면 PR 아님을 명시, 기록 화면 에러 경로가 하나로 합쳐짐), 상태 계약(`IsPersonalRecordUseCase`가 `FindPersonalRecordsUseCase`와 판정을 공유하는 방법을 확장 함수로 명시), 화면 구성(`recordLabel` 파일 위치를 `entryedit`로 통일)
