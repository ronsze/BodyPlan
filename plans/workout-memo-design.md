# 일간 운동일지 메모

**작성일**: 2026-09-10

## 설계

### 요약

일간 운동일지 화면에 하루 한 개짜리 메모를 더한다. 메모는 `workout_memo` 표에 날짜를 키로 저장하고 `WorkoutLog.memo`로 흘러 화면 맨 위 카드에 보인다.
편집은 오늘·어제만 허용하며, 기존 `IsEditableDateUseCase` 판정을 운동 항목 추가와 그대로 공유한다.
편집은 카드 안에서 인라인으로 열리고 `저장`을 눌러야 확정된다 — 사용자가 A안으로 정했다.

### 배경

운동 항목만으로는 그날 컨디션·부상·특이사항이 남지 않는다. 항목 단위가 아니라 하루 단위로 남길 자리가 필요하다.
운동 기록에 걸어 둘 하루 단위 행이 지금 없으므로(`WorkoutLog`는 `workout_entry` 조회 결과를 묶어 만든다) 표를 새로 둔다.

### 수용 조건

- [ ] 편집 가능한 날짜(오늘·어제)에서 메모 카드의 `추가`/`편집`을 누르면 입력이 열리고, 저장한 문구가 카드에 보인다.
- [ ] 조회 전용 날짜(그저께 이전)에서는 `추가`/`편집`이 보이지 않고, 저장된 메모는 읽기 전용으로 보인다.
- [ ] 저장된 메모가 있는 날짜에 다시 들어가면 그 메모가 보인다.
- [ ] 메모를 지우고(공백만 남기고) 저장하면 메모가 없는 상태로 돌아간다.
- [ ] 운동 항목이 하나도 없는 날짜에서도 메모 카드가 보이고 저장할 수 있다.
- [ ] 조회 실패: `observeLog`가 실패하면 기존 에러 화면(`ErrorContent`)이 뜨고 `다시 시도`로 재구독된다 — 메모 카드는 이때 보이지 않는다.
- [ ] 저장 실패: `saveMemo`가 던지면 `메모를 저장하지 못했습니다` 토스트가 뜨고, 편집 모드와 입력 내용이 유지된다.
- [ ] 빈 결과: 메모가 없는 날짜에서 카드는 안내 문구(`메모를 남겨보세요` / `메모가 없습니다`)를 보인다.
- [ ] 권한: 해당 없음.

### 비목표

- AI 운동 분석 입력(`AnalyzeWorkoutUseCase`)에 메모를 넣지 않는다.
- 식단일지(`feature:dietlog`)에는 메모를 넣지 않는다.
- 캘린더 화면에 메모 유무 표시를 넣지 않는다.
- 메모 수정 이력·다중 메모를 두지 않는다 — 날짜당 한 개, 덮어쓰기.
- `WorkoutLogView.kt`의 기존 항목 카드·세트 표기 리팩터링은 하지 않는다. 다만 `EmptyContent`는 메모 카드와 함께 보여야 해서 교체 대상에 포함한다.

### 데이터 계약

**표 `workout_memo` (신규, DB 버전 8 → 9)**

| 컬럼 | 타입 | 귀착지 |
|---|---|---|
| `dateEpochDay` | INTEGER NOT NULL, PK | 조회 키. 도메인으로 올리지 않음 — `WorkoutLog.date`가 이미 같은 값이다 |
| `text` | TEXT NOT NULL | `WorkoutLog.memo` → `WorkoutLogState.memo` → 메모 카드 본문 |
| `updatedAtMillis` | INTEGER NOT NULL | 미사용. 언제 고쳤는지 나중에 보이려고 남긴다(`weight_record`와 같은 판단) |

**DAO `WorkoutMemoDao`**

- `fun observeByDate(dateEpochDay: Long): Flow<WorkoutMemoEntity?>` — `SELECT * FROM workout_memo WHERE dateEpochDay = :dateEpochDay`
- `suspend fun upsert(entity: WorkoutMemoEntity)` — `@Insert(onConflict = REPLACE)`
- `suspend fun deleteByDate(dateEpochDay: Long)` — `DELETE FROM workout_memo WHERE dateEpochDay = :dateEpochDay`

**매핑**

- `WorkoutMemoEntity?` → `WorkoutLog.memo: String?` = `entity?.text`
- `newMemoEntity(date: LocalDate, text: String, nowMillis: Long): WorkoutMemoEntity` — `WorkoutMapper.kt`에 더한다(`newEntryEntity`와 같은 자리·같은 이름 규칙).

**도메인 모델**

- `WorkoutLog(date, entries, memo: String? = null)` — 기본값을 둬 기존 생성부가 그대로 컴파일된다.

**Repository (`WorkoutLogRepository`)**

- `observeLog(date)`: `workoutEntryDao.observeByDate` 와 `workoutMemoDao.observeByDate`를 `combine`해 `WorkoutLog`를 만든다.
- 신규 `suspend fun saveMemo(date: LocalDate, text: String)`: `text.trim()`이 비면 `deleteByDate`, 아니면 `upsert(newMemoEntity(date, trimmed, System.currentTimeMillis()))`. 실패는 삼키지 않고 던진다(인터페이스 규약 그대로).

### 상태 계약

**화면: 일간 운동일지 (`WorkoutLogState` / `WorkoutLogIntent` / `WorkoutLogEffect`)**

State 추가 필드:

| 필드 | 타입 | 초기값 | 출처 |
|---|---|---|---|
| `memo` | `String?` | `null` | `observeLog`의 `WorkoutLog.memo` |
| `memoInput` | `String` | `""` | `ChangeMemoInput` |
| `isEditingMemo` | `Boolean` | `false` | `ClickEditMemo` / `ClickCancelMemo` / 저장 성공 |
| `isSavingMemo` | `Boolean` | `false` | `ClickSaveMemo` 진행 중 |

기존 필드(`date`, `entries`, `isEditable`, `isLoading`, `errorMessage`)는 그대로 둔다.

Intent 추가:

- `data object ClickEditMemo` — `isEditable`이 참일 때만 처리한다. `memoInput = memo ?: ""`, `isEditingMemo = true`.
- `data class ChangeMemoInput(val text: String)` — `memoInput = text.take(MEMO_MAX_LENGTH)`. `MEMO_MAX_LENGTH = 500`.
- `data object ClickSaveMemo` — `isSavingMemo = true` 후 `saveMemo(date, memoInput)`. 성공: `isSavingMemo = false`, `isEditingMemo = false`, `memoInput = ""`(카드 본문은 `observeLog` 스트림이 갱신한다). 실패: `isSavingMemo = false`만 되돌리고 `isEditingMemo`·`memoInput`은 유지, `ShowMessage(MEMO_SAVE_ERROR)`.
- `data object ClickCancelMemo` — `isEditingMemo = false`, `memoInput = ""`.

Effect: 추가 없음. 저장 실패는 기존 `WorkoutLogEffect.ShowMessage`를 쓴다. 문구 상수 `MEMO_SAVE_ERROR = "메모를 저장하지 못했습니다"`를 `WorkoutLogViewModel.kt` 하단 상수 자리에 더한다.

조회 실패는 기존 경로 그대로다 — `errorMessage = LOAD_ERROR`, `ClickRetry`로 재구독.

### 화면 구성

**일간 운동일지 (`WorkoutLogViewImpl`)**

```
Column
├ BodyPlanTopBar                        (재사용, 변경 없음)
├ Box(weight 1f)
│  ├ errorMessage != null → ErrorContent            (재사용, 변경 없음)
│  ├ isLoading && entries.isEmpty() && memo == null → LoadingContent (수정: 조건에 memo 추가)
│  └ else → LazyColumn                              (수정)
│       ├ item { WorkoutMemoCard(...) }             (신규)
│       ├ entries 비었으면 item { EmptyEntriesText } (신규 — 기존 EmptyContent 대체)
│       └ items(entries) { EntryCard }              (재사용, 변경 없음)
└ OutlinedActionButton("운동 분석")                   (재사용, 변경 없음)
```

기존 `EmptyContent`는 `Modifier.fillMaxSize()` Box라 `LazyColumn` 안에서 쓸 수 없다. `EmptyEntriesText`(`fillMaxWidth` + 세로 여백)로 교체하고 `EmptyContent`는 지운다.

**신규 컴포저블 `WorkoutMemoCard`** — `log/composable/WorkoutMemoCard.kt`

```kotlin
@Composable
internal fun WorkoutMemoCard(
    memo: String?,
    input: String,
    isEditable: Boolean,
    isEditing: Boolean,
    isSaving: Boolean,
    onClickEdit: () -> Unit,
    onChangeInput: (String) -> Unit,
    onClickSave: () -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
)
```

재사용하는 심볼(전부 파일을 열어 시그니처 확인함):

| 심볼 | 시그니처 | 판정 |
|---|---|---|
| `BodyPlanCard` | `(modifier: Modifier = Modifier, cornerRadius: Dp = 16.dp, contentPadding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit)` | 재사용 |
| `BaseText` | `(text: String, ...)` — `WorkoutLogView.kt`에서 쓰는 `style`·`color`·`modifier` 인자만 쓴다 | 재사용 |
| `BaseTextField` | `(value, onValueChange, modifier = Modifier, enabled = true, placeholder: String? = null, textStyle = BaseTextDefaults.style, keyboardOptions = KeyboardOptions.Default, keyboardActions = KeyboardActions.Default, singleLine = false, maxLines = ...)` | 재사용. `singleLine`은 기본값 `false` 그대로 둔다 |
| `PrimaryButton` | `(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)` | 재사용 — 저장 |
| `OutlinedActionButton` | `(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)` | 재사용 — 취소 |
| `VerticalSpacer` | `(space: Dp)` | 재사용 |
| `WeightSpacer` | 인자 없음 | 재사용 — 헤더 우측 정렬 |
| `Border`·`Surface`·`TextPrimary`·`TextSecondary`·`TextTertiary` | 색 상수 | 재사용 |

입력 박스 테두리 표기는 `WeightInputCard.kt`의 `WeightInputBox`와 같은 값을 쓴다 — `RoundedCornerShape(12.dp)` + `background(Surface)` + `border(BorderStroke(1.dp, Border), shape)` + `padding(16.dp)`. 공용으로 올리지 않는다(호출부가 둘뿐이고 모듈이 다르다).

카피 원문:

| 자리 | 문구 |
|---|---|
| 카드 제목 | `메모` |
| 헤더 액션 (편집 가능 + 메모 있음) | `편집` |
| 헤더 액션 (편집 가능 + 메모 없음) | `추가` |
| 본문 (메모 없음 + 편집 가능) | `메모를 남겨보세요` |
| 본문 (메모 없음 + 조회 전용) | `메모가 없습니다` |
| 입력 placeholder | `오늘 운동은 어땠나요?` |
| 저장 버튼 | `저장` / 저장 중 `저장 중...` |
| 취소 버튼 | `취소` |
| 항목 없음 | `기록이 없습니다` (기존 문구 유지) |

화면별 상태:

- 로딩: 기존 `LoadingContent`(전체 중앙 스피너). 메모 카드도 이때는 안 보인다.
- 빈: 메모 카드 + `기록이 없습니다`.
- 에러: 기존 `ErrorContent` + `다시 시도`.
- 권한: 해당 없음.

시안 노드 id: 해당 없음(Figma 시안 없음).

### 네비게이션

변경 없음. 진입은 기존 `WorkoutLogNavKey(dateEpochDay)` 그대로다.

### 버린 안

- **B안 — 상단 고정 TextField에 포커스 해제 자동 저장**: 저장 실패를 사용자가 인지할 지점이 없고 목록 공간을 상시 차지한다. 사용자가 A안을 골랐다.
- **C안 — 상단바 액션 → 메모 편집 다이얼로그**: 메모 내용이 화면에 보이지 않아 일지로서의 쓸모가 준다. 사용자가 A안을 골랐다.
- **메모를 `WorkoutEntry`에 항목별로 두기**: 요청이 하루 단위 메모다.
- **메모 전용 Flow를 화면이 따로 구독하기**: 스트림이 둘이 되면 로딩·에러 처리가 두 벌이 된다. `observeLog` 하나로 합친다.

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/local/src/main/java/kr/sdbk/bodyplan/core/local/entity/WorkoutMemoEntity.kt` | 신규 | `@Entity(tableName = "workout_memo")`, `dateEpochDay` PK / `text` / `updatedAtMillis` |
| `core/local/src/main/java/kr/sdbk/bodyplan/core/local/dao/WorkoutMemoDao.kt` | 신규 | `observeByDate` / `upsert` / `deleteByDate` |
| `core/local/src/main/java/kr/sdbk/bodyplan/core/local/BodyPlanDatabase.kt` | 수정 | 엔티티 등록, `version = 9`, `workoutMemoDao()` 추상 함수 |
| `core/local/src/main/java/kr/sdbk/bodyplan/core/local/migration/Migrations.kt` | 수정 | `MIGRATION_8_9` — `CREATE TABLE IF NOT EXISTS workout_memo` |
| `core/local/src/main/java/kr/sdbk/bodyplan/core/local/di/LocalModule.kt` | 수정 | `addMigrations`에 `MIGRATION_8_9`, `provideWorkoutMemoDao` |
| `core/local/schemas/kr.sdbk.bodyplan.core.local.BodyPlanDatabase/9.json` | 신규(생성물) | 컴파일이 내보낸다. 커밋에 포함 |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/model/WorkoutLog.kt` | 수정 | `memo: String? = null` 추가 |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/repository/WorkoutLogRepository.kt` | 수정 | `suspend fun saveMemo(date: LocalDate, text: String)` 추가 |
| `core/data/src/main/java/kr/sdbk/bodyplan/core/data/mapper/WorkoutMapper.kt` | 수정 | `newMemoEntity(date, text, nowMillis)` 추가 |
| `core/data/src/main/java/kr/sdbk/bodyplan/core/data/repository/WorkoutLogRepositoryImpl.kt` | 수정 | `WorkoutMemoDao` 주입, `observeLog` combine, `saveMemo` 구현 |
| `core/data/src/test/java/kr/sdbk/bodyplan/core/data/repository/fake/FakeWorkoutMemoDao.kt` | 신규 | `FakeWeightRecordDao` 짜임새를 따른다 |
| `core/data/src/test/java/kr/sdbk/bodyplan/core/data/repository/WorkoutLogRepositoryImplTest.kt` | 수정 | 생성자에 fake DAO 추가, 메모 조회·저장·공백 저장(삭제) 케이스 추가 |
| `core/domain/src/test/java/kr/sdbk/bodyplan/core/domain/usecase/AnalyzeWorkoutUseCaseTest.kt` | 수정 | 안에 있는 `WorkoutLogRepository` 가짜 구현에 `saveMemo` 추가(컴파일 유지) |
| `core/domain/src/test/java/kr/sdbk/bodyplan/core/domain/usecase/GetMonthlyDayStatusUseCaseTest.kt` | 수정 | 같은 이유로 가짜 구현에 `saveMemo` 추가 — 계획 작성 시 빠뜨린 네 번째 구현체다 |
| `feature/workoutlog/impl/.../log/WorkoutLogContracts.kt` | 수정 | State 4개 필드, Intent 4개 추가 |
| `feature/workoutlog/impl/.../log/WorkoutLogViewModel.kt` | 수정 | `memo` 반영, 메모 Intent 처리, `MEMO_SAVE_ERROR`·`MEMO_MAX_LENGTH` |
| `feature/workoutlog/impl/.../log/composable/WorkoutMemoCard.kt` | 신규 | 메모 카드 + Preview(보기·편집·조회전용) |
| `feature/workoutlog/impl/.../log/composable/WorkoutLogView.kt` | 수정 | `WorkoutLogUiEvents`에 콜백 4개, `LazyColumn` 구성 변경, `EmptyContent` → `EmptyEntriesText` |
| `feature/workoutlog/impl/src/test/.../fake/FakeWorkoutLogRepository.kt` | 수정 | `memo` 상태·`saveMemo` 구현·`mutateFailure` 반영 |
| `feature/workoutlog/impl/src/test/.../log/WorkoutLogViewModelTest.kt` | 수정 | 메모 Intent별 상태 전이·저장 실패 케이스 추가 |

### 구현 순서

**단위 1 — 저장 계층 (local)**
- 쓰는 것: `BodyPlanDatabase`, `LocalModule`, `Migrations.kt`(기존)
- 만드는 것: `WorkoutMemoEntity`, `WorkoutMemoDao`, `MIGRATION_8_9`, `9.json`
- 검증: `./gradlew :core:local:compileDebugKotlin` — 스키마 9.json이 생성되는지 확인
- 커밋 경계: 단위 2와 함께 한 커밋

**단위 2 — 도메인·데이터**
- 쓰는 것: 단위 1의 `WorkoutMemoDao`·`WorkoutMemoEntity`
- 만드는 것: `WorkoutLog.memo`, `WorkoutLogRepository.saveMemo`, `newMemoEntity`, `WorkoutLogRepositoryImpl` 구현, `FakeWorkoutMemoDao`
- 검증: `./gradlew :core:data:testDebugUnitTest :core:domain:test`
- 커밋: `운동일지에 하루치 메모를 저장하는 계층을 만듦`

**단위 3 — 화면**
- 쓰는 것: 단위 2의 `WorkoutLog.memo`, `WorkoutLogRepository.saveMemo`
- 만드는 것: State·Intent 추가, `WorkoutMemoCard`, `WorkoutLogView` 구성 변경, ViewModel 테스트
- 검증: `./gradlew :app:compileDebugKotlin` + `./gradlew :feature:workoutlog:impl:testDebugUnitTest` + Preview 렌더 확인
- 커밋: `일간 운동일지에 메모를 남길 수 있게 함`

### 회귀 대상

| 고치는 것 | 확인할 기존 호출부 |
|---|---|
| `WorkoutLog` 필드 추가 | `WorkoutLogRepositoryImpl.observeLog`, `FakeWorkoutLogRepository`(feature 테스트), `AnalyzeWorkoutUseCaseTest`의 가짜 저장소 — 기본값 `null`이라 컴파일은 유지되나 세 곳 모두 확인 |
| `WorkoutLogRepository` 인터페이스 확장 | 구현체 4곳: `WorkoutLogRepositoryImpl`, `FakeWorkoutLogRepository`(feature 테스트), `AnalyzeWorkoutUseCaseTest`·`GetMonthlyDayStatusUseCaseTest` 내부 가짜 |
| DB 버전 8 → 9 | 기존 표(`weight_record`·`workout_entry`·`analysis_result` 등)를 건드리지 않는지 마이그레이션 SQL 확인. 앱 실행 후 기존 기록 유지 확인 |
| `WorkoutLogView`의 빈 상태 처리 교체 | 일간 운동일지 화면의 "기록 없음" 표시 — 기록 0건 날짜 진입 |
| `WorkoutLogUiEvents` 생성자 확장 | 같은 파일의 `previewUiEvents` |

자기 대조: 통과 (대조 15/15)
- 고침: 수용 조건, 상태 계약, 화면 구성, 파일별 작업, 회귀 대상
