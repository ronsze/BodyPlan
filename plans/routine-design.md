# 루틴 관리

**작성일**: 2026-09-12

## 설계

### 요약

부위별로 이름 붙인 루틴(종목+세트 묶음)을 미리 만들어 두고, 일간 운동일지에서 루틴 하나를 골라 그날 기록으로 한 번에 넣는다.
루틴은 `routine`·`routine_entry`·`routine_set` 세 표에 저장하며, 항목은 기록 한 건과 같은 모양이라 도메인에서 `WorkoutEntry`를 그대로 쓴다.
화면은 전부 `feature:workoutlog:impl`에 두고 마이페이지는 `루틴 관리` 메뉴로 진입만 한다 — 사용자가 이 배치를 골랐다.

### 배경

같은 부위를 할 때마다 종목·세트를 처음부터 고르는 일이 반복된다. 요청은 "각 부위에 대한 루틴을 미리 만들어 두고 운동 일지에서 불러올 수 있도록"이며, 사용자가 방향 합의에서 다음을 정했다.

- 루틴 모델: 부위마다 이름 있는 루틴을 여러 개 둔다(부위당 하나 고정 안은 버림). 일지에서 루틴을 고르면 그 루틴의 항목이 그대로 기록에 들어간다.
- 배치: 화면은 `feature:workoutlog`에, 마이페이지는 진입만.
- 불러오기: 일자 화면에서 시트로 고른다. 루틴이 여러 개이므로 시트는 부위가 아니라 **루틴**을 고르는 시트다.
- 저장소: 기존 운동일지와 같은 Room.

부위-종목 2단계 접기/펼치기는 별도 단위(B)로 이 계획 뒤에 한다. 여기서는 루틴 관리·루틴 상세 목록을 기존 일지와 같은 카드 나열로 만든다.

### 수용 조건

**루틴 관리 (`RoutineListNavKey`)**
- [ ] 마이 탭의 `루틴 관리`를 누르면 루틴 관리 화면이 열린다.
- [ ] `루틴 추가`를 누르면 이름 입력과 부위 선택이 있는 대화상자가 뜨고, `만들기`를 누르면 루틴이 생기며 그 루틴의 상세 화면으로 이동한다.
- [ ] 이름이 공백뿐이면 `루틴 이름을 입력하세요` 토스트가 뜨고 대화상자는 유지된다.
- [ ] 루틴 목록은 부위 순서(`BodyPart.entries`)로 묶여 보이고, 각 루틴은 이름과 `종목 N개`를 보인다. 루틴이 없는 부위는 묶음이 나오지 않는다.
- [ ] 루틴의 `삭제`를 누르면 목록에서 사라진다. 실패하면 `삭제하지 못했습니다` 토스트.
- [ ] 빈 결과: 루틴이 하나도 없으면 `만든 루틴이 없습니다`가 보인다.
- [ ] 조회 실패: `observeRoutines`가 실패하면 `ErrorContent`와 `다시 시도`가 뜬다.
- [ ] 생성 실패: `addRoutine`이 던지면 `루틴을 만들지 못했습니다` 토스트가 뜨고 대화상자와 입력이 유지된다.
- [ ] 권한: 해당 없음.

**루틴 상세 (`RoutineDetailNavKey(routineId)`)**
- [ ] 상단 제목은 루틴 이름이고 부위 배지가 함께 보인다. `운동 추가`를 누르면 운동 편집 화면이 루틴 대상으로 열린다.
- [ ] 항목 카드를 누르면 그 항목의 편집 화면이, `삭제`를 누르면 항목이 지워진다. 실패하면 `삭제하지 못했습니다` 토스트.
- [ ] 빈 결과: 항목이 없으면 `종목이 없습니다`가 보인다.
- [ ] 조회 실패: `observeRoutine`이 실패하거나 null을 내면 `ErrorContent`와 `다시 시도`가 뜬다.
- [ ] 권한: 해당 없음.

**운동 편집 화면(기존, 루틴 대상 확장)**
- [ ] 루틴 대상으로 열면 부위 탭이 보이지 않고 루틴의 부위 종목만 나열된다. 저장하면 `RoutineRepository.addEntry`/`updateEntry`로 저장되고 뒤로 간다.
- [ ] 기존 일지 대상 동작(부위 선택·종목 선택·세트 편집·저장·복원)은 그대로다.
- [ ] 조회 실패: 루틴 대상에서 루틴 조회나 항목 복원이 실패하면 기존 `ErrorContent` + `다시 시도`.
- [ ] 저장 실패: 기존과 같이 `저장하지 못했습니다` 토스트, 입력 유지.

**일간 운동일지(기존, 불러오기 추가)**
- [ ] 편집 가능한 날짜에서 `루틴 불러오기` 버튼이 보이고, 누르면 루틴을 부위별로 묶어 보이는 시트가 뜬다. 조회 전용 날짜에는 버튼이 없다.
- [ ] 시트에서 루틴을 고르면 그 루틴의 모든 항목이 그날 기록 뒤에 덧붙고 시트가 닫힌다. 이미 있던 기록은 그대로다.
- [ ] 빈 결과: 루틴이 없으면 시트에 `만든 루틴이 없습니다`가 보인다. 항목이 없는 루틴을 고르면 `루틴에 종목이 없습니다` 토스트, 시트 유지.
- [ ] 저장 실패: `addEntries`가 던지면 `루틴을 불러오지 못했습니다` 토스트, 시트 유지. 한 항목이라도 실패하면 아무 항목도 남지 않는다(한 트랜잭션).
- [ ] 조회 실패: `observeRoutines`가 실패하면 `루틴을 불러오지 못했습니다` 토스트 한 번, 시트는 빈 목록으로 열린다.
- [ ] 권한: 해당 없음.

### 비목표

- 루틴 이름·부위 수정(생성 후 변경 없음). 필요하면 지우고 다시 만든다.
- 루틴 안 항목 순서 바꾸기.
- 부위-종목 2단계 접기/펼치기 — 단위 B.
- 루틴을 캘린더·분석·홈 요약에 반영하는 것.
- 각 화면에 사본으로 있는 `LoadingContent`·`ErrorContent`를 공용으로 올리는 리팩터링. 새 화면도 같은 사본 방식을 따른다.
- `WorkoutEntryEditView.kt`의 세트 카드 표기 변경.

### 데이터 계약

**표 `routine` (신규, DB 버전 9 → 10)**

| 컬럼 | 타입 | 귀착지 |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | `Routine.id` → 목록 키·상세 진입 인자 |
| `name` | TEXT NOT NULL | `Routine.name` → 목록 카드 제목·상세 상단 제목·시트 행 |
| `bodyPart` | TEXT NOT NULL (`BodyPart.name`) | `Routine.bodyPart` → 목록·시트 묶음, 상세 배지, 편집 화면의 고정 부위 |
| `createdAtMillis` | INTEGER NOT NULL | 정렬 키. 도메인으로 올리지 않음 |

**표 `routine_entry` (신규)** — `workout_entry`와 같은 컬럼에서 `dateEpochDay`만 `routineId`로 바뀐다.

| 컬럼 | 타입 | 귀착지 |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | `WorkoutEntry.id` → 항목 편집 진입 인자·삭제 대상 |
| `routineId` | INTEGER NOT NULL, FK `routine(id)` CASCADE, INDEX `index_routine_entry_routineId` | 조회 키. 미사용(도메인은 `Routine.entries` 소속으로 표현) |
| `exerciseId` | INTEGER NOT NULL | `WorkoutEntry.exerciseId` → 편집 시 종목 복원, 일지 복사 |
| `exerciseName` | TEXT NOT NULL | `WorkoutEntry.exerciseName` → 카드 제목 |
| `bodyPart` | TEXT NOT NULL | `WorkoutEntry.bodyPart` → 카드 배지, 일지 복사 |
| `intensityType` | TEXT NOT NULL | `WorkoutEntry.intensityType` → 세트 표기·편집 |
| `createdAtMillis` | INTEGER NOT NULL | 정렬 키. 미사용 |

**표 `routine_set` (신규)** — `workout_set`과 같은 컬럼, FK만 `routine_entry(id)` CASCADE, INDEX `index_routine_set_entryId`.

| 컬럼 | 귀착지 |
|---|---|
| `id` | 미사용 |
| `entryId` | 조회 키. 미사용 |
| `setNumber` | 정렬 키(`@Relation`은 순서를 보장하지 않는다). 미사용 |
| `repeatCount` | `WorkoutSet.repeatCount` |
| `intensityValue` | `WorkoutSet.intensity` (`Intensity.of(intensityType, value)`) |

**마이그레이션 `MIGRATION_9_10`** — 세 표와 두 인덱스를 `CREATE ... IF NOT EXISTS`로 만든다. SQL은 9.json의 `workout_entry`·`workout_set` `createSql`을 옮겨 표·컬럼 이름만 바꾼다. 기존 표는 건드리지 않는다.

**조회 POJO**

- `RoutineEntryWithSets(@Embedded entry: RoutineEntryEntity, @Relation(parentColumn = "id", entityColumn = "entryId") sets: List<RoutineSetEntity>)`
- `RoutineWithEntries(@Embedded routine: RoutineEntity, @Relation(entity = RoutineEntryEntity::class, parentColumn = "id", entityColumn = "routineId") entries: List<RoutineEntryWithSets>)` — 중첩 관계. 항목 순서도 보장되지 않으므로 매핑에서 `createdAtMillis, id`로 정렬한다.

**DAO `RoutineDao`** (`WorkoutEntryDao`의 항목·세트 메서드 짜임새를 그대로 따른다)

- `@Transaction @Query("SELECT * FROM routine ORDER BY createdAtMillis ASC, id ASC") fun observeAll(): Flow<List<RoutineWithEntries>>`
- `@Transaction @Query("SELECT * FROM routine WHERE id = :id") fun observeById(id: Long): Flow<RoutineWithEntries?>`
- `@Transaction @Query("SELECT * FROM routine WHERE id = :id") suspend fun getById(id: Long): RoutineWithEntries?`
- `@Insert suspend fun insert(entity: RoutineEntity): Long`
- `@Query("DELETE FROM routine WHERE id = :id") suspend fun deleteById(id: Long)` — 항목·세트는 CASCADE
- `@Transaction @Query("SELECT * FROM routine_entry WHERE id = :id") suspend fun getEntryWithSets(id: Long): RoutineEntryWithSets?`
- `@Insert suspend fun insertEntry(entity: RoutineEntryEntity): Long` / `@Update suspend fun updateEntry(entity: RoutineEntryEntity)` / `@Query("DELETE FROM routine_entry WHERE id = :id") suspend fun deleteEntryById(id: Long)`
- `@Insert suspend fun insertSets(sets: List<RoutineSetEntity>)` / `@Query("DELETE FROM routine_set WHERE entryId = :entryId") suspend fun deleteSetsByEntryId(entryId: Long)`
- `@Transaction suspend fun insertEntryWithSets(entity: RoutineEntryEntity, sets: List<RoutineSetEntity>): Long` / `@Transaction suspend fun updateEntryWithSets(entity: RoutineEntryEntity, sets: List<RoutineSetEntity>)` — 기본 구현, 부모 키는 여기서 채운다

**DAO `WorkoutEntryDao` 추가**

- `@Transaction suspend fun insertAllWithSets(items: List<WorkoutEntryWithSets>)` — `items.forEach { insertWithSets(it.entry, it.sets) }`. 루틴 불러오기가 한 트랜잭션으로 들어가게 한다.

**매핑 (`core/data/.../mapper/RoutineMapper.kt`, 신규 — 표 묶음이 다르다)**

- `RoutineWithEntries.toDomain(): Routine` — `entries.sortedWith(compareBy({ it.entry.createdAtMillis }, { it.entry.id })).map { it.toDomain() }`
- `RoutineEntryWithSets.toDomain(): WorkoutEntry` — `WorkoutMapper.kt`의 `WorkoutEntryWithSets.toDomain()`과 같은 조립. `id = entry.id`
- `RoutineSetEntity.toDomain(intensityType): WorkoutSet`
- `List<WorkoutSet>.toRoutineSetEntities(entryId: Long): List<RoutineSetEntity>` — `setNumber = index + 1`
- `newRoutineEntity(name, bodyPart, createdAtMillis): RoutineEntity`
- `newRoutineEntryEntity(routineId, exercise, createdAtMillis): RoutineEntryEntity`

**매핑 (`WorkoutMapper.kt` 추가)**

- `WorkoutEntry.toEntityWithSets(date: LocalDate, createdAtMillis: Long): WorkoutEntryWithSets` — id는 0으로 두고 스냅샷 필드를 옮긴다. `sets.toEntities(entryId = 0L)`.

**도메인 모델 (`core/domain/.../model/Routine.kt`, 신규)**

```kotlin
/**
 * 미리 짜 둔 종목·세트 묶음. 부위 하나에 묶이고 이름으로 구분한다.
 * 항목은 기록 한 건과 같은 모양이라 [WorkoutEntry]를 그대로 쓴다 — [WorkoutEntry.id]는 루틴 항목의 id다.
 */
data class Routine(val id: Long, val name: String, val bodyPart: BodyPart, val entries: List<WorkoutEntry>)
```

**Repository `RoutineRepository` (신규)** — 실패는 던진다.

```kotlin
fun observeRoutines(): Flow<List<Routine>>
fun observeRoutine(id: Long): Flow<Routine?>
suspend fun getRoutine(id: Long): Routine?
suspend fun addRoutine(name: String, bodyPart: BodyPart): Long
suspend fun deleteRoutine(id: Long)
suspend fun getEntry(id: Long): WorkoutEntry?
suspend fun addEntry(routineId: Long, exercise: Exercise, sets: List<WorkoutSet>): Long
suspend fun updateEntry(entryId: Long, exercise: Exercise, sets: List<WorkoutSet>)
suspend fun deleteEntry(id: Long)
```

`updateEntry`는 `WorkoutLogRepositoryImpl.updateEntry`와 같이 저장된 행을 읽어 `routineId`·`createdAtMillis`는 두고 종목 스냅샷만 갈아 끼운다. 없으면 `requireNotNull`로 던진다.

**Repository `WorkoutLogRepository` 추가**

- `suspend fun addEntries(date: LocalDate, entries: List<WorkoutEntry>)` — 각 항목의 `id`는 무시하고 새 행으로 넣는다. `createdAtMillis`는 호출 시각 하나로 같고 `id ASC`가 순서를 지킨다. 빈 목록이면 아무것도 하지 않는다. 구현은 `workoutEntryDao.insertAllWithSets(entries.map { it.toEntityWithSets(date, now) })`.

**DI**: `DataModule`에 `bindRoutineRepository`, `LocalModule`에 `provideRoutineDao`, `addMigrations`에 `MIGRATION_9_10`.

### 상태 계약

**화면: 루틴 관리 (`routinelist` 패키지, `RoutineListState` / `RoutineListIntent` / `RoutineListEffect`)**

| 필드 | 타입 | 초기값 | 출처 |
|---|---|---|---|
| `routines` | `List<Routine>` | `emptyList()` | `observeRoutines` |
| `isLoading` | `Boolean` | `false` | 구독 시작~첫 값 |
| `errorMessage` | `String?` | `null` | 구독 실패 시 `LOAD_ERROR` |
| `isDialogVisible` | `Boolean` | `false` | `ClickAddRoutine` / `DismissDialog` / 생성 성공 |
| `dialogName` | `String` | `""` | `ChangeDialogName` (`take(ROUTINE_NAME_MAX_LENGTH)`, 20) |
| `dialogBodyPart` | `BodyPart` | `BodyPart.CHEST` | `SelectDialogBodyPart` |
| `isCreating` | `Boolean` | `false` | `ConfirmDialog` 진행 중 |

파생: `val sections: List<Pair<BodyPart, List<Routine>>>` = `BodyPart.entries`를 순회해 그 부위 루틴이 있는 것만.

Intent:
- `data object ClickAddRoutine` — `isDialogVisible = true, dialogName = "", dialogBodyPart = CHEST`
- `data class ChangeDialogName(val name: String)`
- `data class SelectDialogBodyPart(val bodyPart: BodyPart)`
- `data object ConfirmDialog` — `dialogName.trim()`이 비면 `ShowMessage(NAME_REQUIRED)`. 아니면 `isCreating = true` → `addRoutine(trimmed, dialogBodyPart)` → 성공: `isCreating = false, isDialogVisible = false`, `NavigateToRoutineDetail(id)`. 실패: `isCreating = false`만 되돌리고 `ShowMessage(CREATE_ERROR)`.
- `data object DismissDialog` — `isCreating`이면 무시. `isDialogVisible = false`
- `data class ClickRoutine(val id: Long)` — `NavigateToRoutineDetail(id)`
- `data class ClickDeleteRoutine(val id: Long)` — `deleteRoutine` 실패 시 `ShowMessage(DELETE_ERROR)`
- `data object ClickBack` — `GoBack`
- `data object ClickRetry` — 재구독

Effect: `GoBack`, `data class NavigateToRoutineDetail(val routineId: Long)`, `data class ShowMessage(val message: String)`.

상수(ViewModel 파일 하단): `LOAD_ERROR = "불러오지 못했습니다"`, `DELETE_ERROR = "삭제하지 못했습니다"`, `NAME_REQUIRED = "루틴 이름을 입력하세요"`, `CREATE_ERROR = "루틴을 만들지 못했습니다"`, `ROUTINE_NAME_MAX_LENGTH = 20`.

**화면: 루틴 상세 (`routinedetail` 패키지, `RoutineDetailState` / `RoutineDetailIntent` / `RoutineDetailEffect`)**

| 필드 | 타입 | 초기값 | 출처 |
|---|---|---|---|
| `routineId` | `Long` | NavKey | `RoutineDetailNavKey.routineId` |
| `routine` | `Routine?` | `null` | `observeRoutine(routineId)` |
| `isLoading` | `Boolean` | `false` | 구독 시작~첫 값 |
| `errorMessage` | `String?` | `null` | 구독 실패 또는 null 수신 시 `LOAD_ERROR` |

Intent: `ClickAddEntry` → `NavigateToEntryEdit(routineId, null)`; `ClickEntry(id)` → `NavigateToEntryEdit(routineId, id)`; `ClickDeleteEntry(id)` → `deleteEntry`, 실패 `ShowMessage(DELETE_ERROR)`; `ClickBack` → `GoBack`; `ClickRetry` → 재구독.

Effect: `data class NavigateToEntryEdit(val routineId: Long, val entryId: Long?)`, `GoBack`, `ShowMessage`.

**화면: 운동 편집 (`entryedit`, 기존 계약 변경)**

`WorkoutEntryEditContracts.kt`에 추가:

```kotlin
/** 저장 대상. 일지는 날짜에, 루틴은 루틴 id에 묶인다. */
internal sealed interface WorkoutEntryEditTarget {
    data class Log(val date: LocalDate) : WorkoutEntryEditTarget
    data class Routine(val routineId: Long) : WorkoutEntryEditTarget
}
```

State: `date: LocalDate` → `target: WorkoutEntryEditTarget`으로 바꾼다. 파생 `val isBodyPartLocked: Boolean get() = target is WorkoutEntryEditTarget.Routine`. 나머지 필드·Intent·Effect는 그대로.

ViewModel:
- 생성자 `@Assisted target: WorkoutEntryEditTarget, @Assisted editingEntryId: Long?`, `RoutineRepository` 주입 추가. `Factory.create(target, editingEntryId)`.
- `initializeData`: `editingEntryId != null` → `restoreEntry`. 아니고 `target is Routine` → `loadRoutineBodyPart()`: `isLoading = true` → `routineRepository.getRoutine(routineId)` → null이면 `errorMessage = LOAD_ERROR`, 있으면 `selectBodyPart(routine.bodyPart)`.
- `restoreEntry`: 대상에 따라 `workoutLogRepository.getEntry` / `routineRepository.getEntry`.
- `save`: 대상에 따라 `workoutLogRepository.addEntry(date, …)`·`updateEntry` / `routineRepository.addEntry(routineId, …)`·`updateEntry`.
- `retry`: `editingEntryId`가 있으면 복원. 없으면 `selectedBodyPart`가 있을 때 그 부위 종목만 재구독하고(루틴을 다시 읽어 부위를 되돌리지 않는다 — 되돌리면 잠금 가드에 걸려 로딩이 풀리지 않는다), 없고 루틴 대상이면 `loadRoutineBodyPart`.
- `SelectBodyPart`는 `isBodyPartLocked`면 `handleIntent`에서 무시한다(탭이 없지만 상태 쪽에서도 막는다). 내부의 `selectBodyPart`는 가드 없이 `loadRoutineBodyPart`가 쓴다.

**화면: 일간 운동일지 (`log`, 기존 계약에 추가)**

| 필드 | 타입 | 초기값 | 출처 |
|---|---|---|---|
| `routines` | `List<Routine>` | `emptyList()` | `observeRoutines` (`isEditable`일 때만 구독) |
| `isRoutineSheetVisible` | `Boolean` | `false` | `ClickLoadRoutine` / `DismissRoutineSheet` / 적용 성공 |
| `isApplyingRoutine` | `Boolean` | `false` | `SelectRoutine` 진행 중 |

파생: `val routineSections: List<Pair<BodyPart, List<Routine>>>` — 루틴 관리 화면과 같은 계산. 두 곳이 같은 식이므로 `Routine.kt`에 `fun List<Routine>.groupedByBodyPart(): List<Pair<BodyPart, List<Routine>>>` 확장으로 둔다.

Intent:
- `data object ClickLoadRoutine` — `isEditable`이 아니면 무시. `isRoutineSheetVisible = true`
- `data object DismissRoutineSheet` — `isApplyingRoutine`이면 무시. `isRoutineSheetVisible = false`
- `data class SelectRoutine(val id: Long)` — `isApplyingRoutine`이면 무시. `isApplyingRoutine = true` → `routineRepository.getRoutine(id)`. null이거나 `entries`가 비면 `ShowMessage(ROUTINE_EMPTY)`, 시트 유지. 아니면 `workoutLogRepository.addEntries(date, routine.entries)` → 성공: `isApplyingRoutine = false, isRoutineSheetVisible = false`(항목은 `observeLog` 스트림이 갱신). 실패: `isApplyingRoutine = false`, `ShowMessage(ROUTINE_APPLY_ERROR)`, 시트 유지.

Effect: 추가 없음. `ShowMessage` 재사용.

`initializeData`: 기존 `observeLog()` 뒤에 `if (isEditable) observeRoutines()`. `observeRoutines`는 `.catch { updateEffect(ShowMessage(ROUTINE_APPLY_ERROR)) }` 후 `routines`에 담는다.

상수 추가: `ROUTINE_EMPTY = "루틴에 종목이 없습니다"`, `ROUTINE_APPLY_ERROR = "루틴을 불러오지 못했습니다"`.

**화면: 마이 (`feature/my/impl/home`, 기존 계약에 추가)**

- `MyIntent.ClickRoutine` → `MyEffect.NavigateToRoutine`. State 변경 없음.

### 화면 구성

재사용하는 심볼(전부 파일을 열어 시그니처 확인함):

| 심볼 | 시그니처 | 판정 |
|---|---|---|
| `BodyPlanTopBar` | `(title: String, modifier = Modifier, onBack: (() -> Unit)? = null, actionText: String? = null, onClickAction: () -> Unit = {}, actionIcon: Painter? = null, actionIconDescription: String? = null, onClickActionIcon: () -> Unit = {})` | 재사용 |
| `BodyPlanCard` | `(modifier = Modifier, cornerRadius: Dp = 16.dp, contentPadding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit)` | 재사용 |
| `SectionRow` | `(title: String, onClick: () -> Unit, modifier = Modifier, description: String? = null, leading: (@Composable () -> Unit)? = null)` | 재사용 — 마이 메뉴 |
| `BaseTextField` | `(value, onValueChange, modifier = Modifier, enabled = true, placeholder: String? = null, textStyle = BaseTextDefaults.style, keyboardOptions = KeyboardOptions.Default, keyboardActions = KeyboardActions.Default, singleLine = false, maxLines = …)` | 재사용 — 루틴 이름, `singleLine = true` |
| `BodyPartTabRow` | `(selected: BodyPart?, onSelect: (BodyPart) -> Unit, modifier = Modifier)` | 재사용 — 생성 대화상자의 부위 선택 |
| `Badge` | `(text: String, modifier = Modifier)` | 재사용 — 부위 배지 |
| `PrimaryButton` | `(text, onClick, modifier = Modifier, enabled = true)` | 재사용 |
| `OutlinedActionButton` | `(text, onClick, modifier = Modifier)` | 재사용 |
| `BaseText`·`VerticalSpacer`·`WeightSpacer`·`HorizontalDivider` | `WorkoutLogView.kt`가 쓰는 인자만 | 재사용 |
| `BodyPart.label` | `core/ui/components/BodyPartUi.kt` 확장 | 재사용 |
| `AlertDialog`(material3) | `ExerciseManageView.kt`의 `ExerciseDialog`와 같은 짜임새 — `onDismissRequest`, `title`, `text`, `confirmButton`, `dismissButton` | 재사용 |
| `ModalBottomSheet`(material3) | `(onDismissRequest: () -> Unit, modifier = Modifier, sheetState = rememberModalBottomSheetState(), …, content: @Composable ColumnScope.() -> Unit)` | 재사용 — 리포에 첫 사용. 기본값만 쓴다 |

**공용으로 올리는 것 — `core/ui/components/WorkoutEntryCard.kt` (신규)**

`WorkoutLogView.kt`의 `EntryCard`·`SetRow`·`intensityText`를 옮긴다. 일지 상세와 루틴 상세가 같은 카드를 쓰고, 도메인(`WorkoutEntry`)에 묶여 있어 `core:ui:components`가 자리다.

```kotlin
@Composable
fun WorkoutEntryCard(
    entry: WorkoutEntry,
    isEditable: Boolean,
    onClick: () -> Unit,
    onClickDelete: () -> Unit,
    modifier: Modifier = Modifier,
)
```

표기는 지금 `EntryCard`와 같다(제목·부위 배지·`삭제`·세트 행 `N세트 10kg × 4회`).

**루틴 관리 (`RoutineListViewImpl`)**

```
Column(fillMaxSize, Background)
├ BodyPlanTopBar(title = "루틴 관리", onBack, actionText = "루틴 추가")
└ Box(weight 1f)
   ├ errorMessage != null → ErrorContent(사본)
   ├ isLoading && routines.isEmpty() → LoadingContent(사본)
   ├ routines.isEmpty() → EmptyContent("만든 루틴이 없습니다")
   └ LazyColumn(contentPadding 16.dp, spacedBy 12.dp)
      └ sections.forEach { (bodyPart, routines) ->
           item { 부위 제목 BaseText(bodyPart.label, titleSmall, TextSecondary) }
           items(routines, key = id) { RoutineCard }   (신규, 화면 전용)
        }
if (isDialogVisible) RoutineCreateDialog(state, uiEvents)   (신규, 화면 전용)
```

`RoutineCard(routine: Routine, onClick, onClickDelete)`: `BodyPlanCard` + `Row(이름 titleSmall / WeightSpacer / "삭제" bodySmall TextTertiary)` + `종목 N개` bodySmall TextSecondary.

`RoutineCreateDialog`: `AlertDialog(title = "루틴 추가", text = Column { BaseTextField(placeholder = "루틴 이름", singleLine = true); VerticalSpacer(12.dp); BodyPartTabRow(selected = dialogBodyPart) }, confirmButton = "만들기"(`isCreating`이면 "만드는 중..."), dismissButton = "취소")`. 버튼 표기는 `ExerciseDialog`와 같다.

화면별 상태: 로딩 — 중앙 스피너 / 빈 — `만든 루틴이 없습니다` / 에러 — `ErrorContent` + `다시 시도` / 권한 — 해당 없음.

**루틴 상세 (`RoutineDetailViewImpl`)**

```
Column(fillMaxSize, Background)
├ BodyPlanTopBar(title = routine?.name ?: "", onBack, actionText = "운동 추가")
└ Box(weight 1f)
   ├ errorMessage != null → ErrorContent(사본)
   ├ isLoading && routine == null → LoadingContent(사본)
   └ LazyColumn(contentPadding 16.dp, spacedBy 12.dp)
      ├ item { Badge(routine.bodyPart.label) }
      ├ entries 비었으면 item { EmptyEntriesText("종목이 없습니다") }
      └ items(entries, key = id) { WorkoutEntryCard(entry, isEditable = true, onClick, onClickDelete) }
```

화면별 상태: 로딩 — 중앙 스피너 / 빈 — `종목이 없습니다` / 에러 — `ErrorContent` + `다시 시도` / 권한 — 해당 없음.

**운동 편집 (`WorkoutEntryEditViewImpl`, 수정)**

`BodyPartTabRow`와 그 아래 `VerticalSpacer(12.dp)`를 `if (!state.isBodyPartLocked)`로 감싼다. 나머지 트리는 그대로. Preview에 루틴 대상 하나를 더한다. 기존 Preview의 `date =`는 `target = WorkoutEntryEditTarget.Log(date)`로 바꾼다.

화면별 상태: 기존과 같다.

**일간 운동일지 (`WorkoutLogViewImpl`, 수정)**

- 하단 `Column`: `if (state.isEditable) { OutlinedActionButton("루틴 불러오기", onClickLoadRoutine); VerticalSpacer(8.dp) }` 뒤에 기존 `운동 분석`.
- `EntryCard` → `WorkoutEntryCard` 호출로 바꾸고 private 사본을 지운다.
- `if (state.isRoutineSheetVisible) RoutineSheet(...)` — `log/composable/RoutineSheet.kt` (신규, 화면 전용):

```kotlin
@Composable
internal fun RoutineSheet(
    sections: List<Pair<BodyPart, List<Routine>>>,
    isApplying: Boolean,
    onSelectRoutine: (Long) -> Unit,
    onDismiss: () -> Unit,
)
```

`ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(confirmValueChange = { !isApplying }))` — 넣는 중에는 스와이프로도 닫히지 않게 한다(닫힌 뒤 온 dismiss를 ViewModel이 무시하면 화면과 상태가 어긋난다). 안에 제목 `루틴 불러오기`(titleMedium), 빈 목록이면 `만든 루틴이 없습니다`(bodyMedium TextTertiary, 세로 여백 40.dp), 아니면 `LazyColumn`에 부위 제목 + 루틴 행(`Row(이름 bodyLarge / WeightSpacer / "종목 N개" bodySmall TextSecondary)`, `clickable(enabled = !isApplying)`). 하단 `navigationBarsPadding()`.

`WorkoutLogUiEvents`에 `onClickLoadRoutine`, `onDismissRoutineSheet`, `onSelectRoutine: (Long) -> Unit` 추가.

화면별 상태: 기존 + 시트의 빈 상태.

**마이 (`MyViewImpl`, 수정)** — `체중 기록` 아래 `SectionRow(title = "루틴 관리", onClick = uiEvents.onClickRoutine, description = "부위별 루틴을 만들어 일지에 불러와요")`.

카피 원문:

| 자리 | 문구 |
|---|---|
| 마이 메뉴 | `루틴 관리` / `부위별 루틴을 만들어 일지에 불러와요` |
| 루틴 관리 상단 | `루틴 관리` / 액션 `루틴 추가` |
| 루틴 카드 | `종목 N개` / `삭제` |
| 생성 대화상자 | 제목 `루틴 추가` / placeholder `루틴 이름` / `만들기` / `만드는 중...` / `취소` |
| 루틴 관리 빈 상태 | `만든 루틴이 없습니다` |
| 루틴 상세 상단 | 루틴 이름 / 액션 `운동 추가` |
| 루틴 상세 빈 상태 | `종목이 없습니다` |
| 일지 하단 버튼 | `루틴 불러오기` |
| 시트 | 제목 `루틴 불러오기` / 빈 `만든 루틴이 없습니다` / 행 `종목 N개` |
| 에러 | `불러오지 못했습니다` / `다시 시도` |
| 토스트 | `루틴 이름을 입력하세요` / `루틴을 만들지 못했습니다` / `삭제하지 못했습니다` / `루틴에 종목이 없습니다` / `루틴을 불러오지 못했습니다` / `저장하지 못했습니다` |

시안 노드 id: 해당 없음(Figma 시안 없음).

### 네비게이션

`feature/workoutlog/api/.../WorkoutLogNavKey.kt`에 추가:

```kotlin
@Serializable data object RoutineListNavKey : BodyPlanNavKey()
@Serializable data class RoutineDetailNavKey(val routineId: Long) : BodyPlanNavKey()
@Serializable data class RoutineEntryEditNavKey(val routineId: Long, val entryId: Long? = null) : BodyPlanNavKey()

fun BodyPlanNavigator.navigateToRoutineList() = navigate(RoutineListNavKey)
fun BodyPlanNavigator.navigateToRoutineDetail(routineId: Long) = navigate(RoutineDetailNavKey(routineId))
fun BodyPlanNavigator.navigateToRoutineEntryEdit(routineId: Long, entryId: Long? = null) =
    navigate(RoutineEntryEditNavKey(routineId, entryId))
```

기존 `WorkoutEntryEditNavKey(dateEpochDay, entryId)`는 그대로 둔다. 하나의 NavKey에 다형 대상을 넣지 않는다 — 직렬화 다형성을 끌어들이지 않기 위해서다.

경로:
- 마이 → `루틴 관리` → `RoutineListNavKey` (my:impl이 `workoutlog:api`의 `navigateToRoutineList`를 호출. `feature/my/impl/build.gradle.kts`에 `implementation(project(":feature:workoutlog:api"))` 추가. workoutlog:impl → my:api 의존이 이미 있으나 api끼리는 서로 의존하지 않으므로 순환 없음)
- 루틴 관리 → 카드 클릭 / 생성 성공 → `RoutineDetailNavKey(id)`
- 루틴 상세 → `운동 추가` / 카드 클릭 → `RoutineEntryEditNavKey(routineId, entryId?)`
- 운동 편집 저장·뒤로 → `goBack`
- `workoutLogNavGraph`에 entry 세 개 추가. `RoutineEntryEditNavKey` entry는 `WorkoutEntryEditView`를 `Factory.create(WorkoutEntryEditTarget.Routine(navKey.routineId), navKey.entryId)`로 띄우고, 기존 `WorkoutEntryEditNavKey` entry는 `create(WorkoutEntryEditTarget.Log(LocalDate.ofEpochDay(navKey.dateEpochDay)), navKey.entryId)`로 바꾼다.

### 버린 안

- **부위당 루틴 하나 고정**: 이름·선택 단계가 없어 단순하지만 같은 부위의 변형(A/B)을 둘 수 없다. 사용자가 이름 있는 루틴 여러 개를 골랐다.
- **루틴 화면을 `feature:my:impl`에 두고 편집 UI를 `core:ui:components`로 승격**: 종목 선택·세트 편집 컴포저블과 `SetInput` 로직까지 끌어올려야 해 기존 운동 추가 화면까지 손댄다. 사용자가 workoutlog 배치를 골랐다.
- **운동 추가 화면 안에서 루틴 불러오기**: 추가 화면은 종목 하나 단위라 여러 항목을 한 번에 넣는 흐름과 맞지 않는다. 사용자가 일자 화면 시트를 골랐다.
- **루틴 항목 전용 도메인 모델 `RoutineEntry`**: `WorkoutEntry`와 필드가 같다. 모델을 둘로 두면 카드·편집 화면·매핑이 두 벌이 된다. `WorkoutEntry`를 쓰고 KDoc으로 id의 뜻을 적는다.
- **루틴 편집 화면에서 저장 전 항목을 화면 상태로만 들고 있다가 한 번에 저장**: 항목 편집 화면으로 오가는 동안 미저장 상태를 화면 간에 넘겨야 해 Nav3의 화면별 ViewModel과 맞지 않는다. 루틴 헤더를 먼저 만들고 항목은 일지처럼 건별로 저장한다.
- **`WorkoutEntryEditNavKey`에 sealed 대상을 넣기**: NavKey 직렬화에 다형성이 들어간다. NavKey를 하나 더 두는 쪽이 싸다.
- **루틴 불러오기를 ViewModel에서 `addEntry` 반복 호출**: 중간 실패 시 반쪽만 남는다. DAO `@Transaction`으로 묶는 `addEntries`를 둔다.
- **루틴 불러오기 시 `LoadRoutineUseCase`**: 조회 하나와 저장 하나를 잇는 것뿐이고 도메인 규칙이 없다. ViewModel이 두 저장소를 직접 부른다.

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/local/.../entity/RoutineEntity.kt` | 신규 | `@Entity(tableName = "routine")` — `id`·`name`·`bodyPart`·`createdAtMillis` |
| `core/local/.../entity/RoutineEntryEntity.kt` | 신규 | `routine_entry`, FK `routineId` CASCADE, `Index("routineId")` |
| `core/local/.../entity/RoutineSetEntity.kt` | 신규 | `routine_set`, FK `entryId` CASCADE, `Index("entryId")` |
| `core/local/.../entity/RoutineEntryWithSets.kt` | 신규 | `@Embedded` + `@Relation` |
| `core/local/.../entity/RoutineWithEntries.kt` | 신규 | 중첩 `@Relation(entity = RoutineEntryEntity::class)` |
| `core/local/.../dao/RoutineDao.kt` | 신규 | 데이터 계약의 메서드 |
| `core/local/.../dao/WorkoutEntryDao.kt` | 수정 | `insertAllWithSets` 추가 |
| `core/local/.../BodyPlanDatabase.kt` | 수정 | 엔티티 3개 등록, `version = 10`, `routineDao()` |
| `core/local/.../migration/Migrations.kt` | 수정 | `MIGRATION_9_10` |
| `core/local/.../di/LocalModule.kt` | 수정 | `addMigrations`에 `MIGRATION_9_10`, `provideRoutineDao` |
| `core/local/schemas/.../10.json` | 신규(생성물) | 컴파일이 내보낸다. 커밋 포함 |
| `core/domain/.../model/Routine.kt` | 신규 | `Routine` + `List<Routine>.groupedByBodyPart()` |
| `core/domain/.../repository/RoutineRepository.kt` | 신규 | 인터페이스 |
| `core/domain/.../repository/WorkoutLogRepository.kt` | 수정 | `addEntries` |
| `core/data/.../mapper/RoutineMapper.kt` | 신규 | 데이터 계약의 매핑 |
| `core/data/.../mapper/WorkoutMapper.kt` | 수정 | `WorkoutEntry.toEntityWithSets` |
| `core/data/.../repository/RoutineRepositoryImpl.kt` | 신규 | 구현 |
| `core/data/.../repository/WorkoutLogRepositoryImpl.kt` | 수정 | `addEntries` |
| `core/data/.../di/DataModule.kt` | 수정 | `bindRoutineRepository` |
| `core/data/src/test/.../repository/fake/FakeRoutineDao.kt` | 신규 | `FakeWorkoutEntryDao` 짜임새 |
| `core/data/src/test/.../repository/fake/FakeWorkoutEntryDao.kt` | 수정 | `insertAllWithSets`는 인터페이스 기본 구현이라 그대로 동작 — 확인만 |
| `core/data/src/test/.../repository/RoutineRepositoryImplTest.kt` | 신규 | 루틴 생성·조회 정렬·항목 추가/수정/삭제·없는 항목 수정 예외 |
| `core/data/src/test/.../repository/WorkoutLogRepositoryImplTest.kt` | 수정 | `addEntries` — 순서 유지·빈 목록 |
| `core/domain/src/test/.../usecase/{AnalyzeWorkout,GetExerciseTrend,GetMonthlyDayStatus,GetProgressSummary}UseCaseTest.kt` | 수정 | 내부 가짜 `WorkoutLogRepository`에 `addEntries` 추가(컴파일 유지) |
| `feature/home/impl/src/test/.../home/HomeViewModelTest.kt` | 수정 | 같은 이유 |
| `core/ui/components/.../WorkoutEntryCard.kt` | 신규 | `EntryCard`·`SetRow`·`intensityText` 이전 + Preview |
| `feature/workoutlog/api/.../WorkoutLogNavKey.kt` | 수정 | NavKey 3개 + navigate 3개 |
| `feature/workoutlog/impl/.../entryedit/WorkoutEntryEditContracts.kt` | 수정 | `WorkoutEntryEditTarget`, `State.target`, `isBodyPartLocked` |
| `feature/workoutlog/impl/.../entryedit/WorkoutEntryEditViewModel.kt` | 수정 | 대상 분기, `RoutineRepository` 주입, Factory 시그니처 |
| `feature/workoutlog/impl/.../entryedit/composable/WorkoutEntryEditView.kt` | 수정 | 탭 숨김, Preview 갱신 |
| `feature/workoutlog/impl/.../routinelist/RoutineListContracts.kt` | 신규 | |
| `feature/workoutlog/impl/.../routinelist/RoutineListViewModel.kt` | 신규 | `@HiltViewModel @Inject` (NavKey 인자 없음) |
| `feature/workoutlog/impl/.../routinelist/composable/RoutineListView.kt` | 신규 | View·ViewImpl·RoutineCard·RoutineCreateDialog·Preview(목록/빈/대화상자) |
| `feature/workoutlog/impl/.../routinedetail/RoutineDetailContracts.kt` | 신규 | |
| `feature/workoutlog/impl/.../routinedetail/RoutineDetailViewModel.kt` | 신규 | assisted `RoutineDetailNavKey` |
| `feature/workoutlog/impl/.../routinedetail/composable/RoutineDetailView.kt` | 신규 | View·ViewImpl·Preview(항목/빈) |
| `feature/workoutlog/impl/.../log/WorkoutLogContracts.kt` | 수정 | State 3개 필드, Intent 3개 |
| `feature/workoutlog/impl/.../log/WorkoutLogViewModel.kt` | 수정 | `RoutineRepository` 주입, `observeRoutines`, `applyRoutine`, 상수 |
| `feature/workoutlog/impl/.../log/composable/WorkoutLogView.kt` | 수정 | `WorkoutEntryCard` 사용, 하단 버튼, 시트 호출, UiEvents 3개, Preview |
| `feature/workoutlog/impl/.../log/composable/RoutineSheet.kt` | 신규 | 시트 + Preview |
| `feature/workoutlog/impl/.../WorkoutLogNavGraph.kt` | 수정 | entry 3개 추가, 기존 편집 entry의 Factory 호출 변경 |
| `feature/workoutlog/impl/src/test/.../fake/FakeRoutineRepository.kt` | 신규 | `MutableStateFlow` 기반, `observeFailure`/`mutateFailure`, 마지막 저장 인자 |
| `feature/workoutlog/impl/src/test/.../fake/FakeWorkoutLogRepository.kt` | 수정 | `addEntries` — `mutateFailure`, `lastAddedEntries`, `entries`에 반영 |
| `feature/workoutlog/impl/src/test/.../entryedit/WorkoutEntryEditViewModelTest.kt` | 수정 | 생성자 변경 반영, 루틴 대상 케이스(부위 고정·저장 경로·복원·조회 실패) |
| `feature/workoutlog/impl/src/test/.../log/WorkoutLogViewModelTest.kt` | 수정 | 시트 열기(조회 전용 무시)·루틴 적용 성공/빈 루틴/실패·루틴 조회 실패 |
| `feature/workoutlog/impl/src/test/.../routinelist/RoutineListViewModelTest.kt` | 신규 | 수용 조건 그대로 |
| `feature/workoutlog/impl/src/test/.../routinedetail/RoutineDetailViewModelTest.kt` | 신규 | 수용 조건 그대로 |
| `feature/my/impl/build.gradle.kts` | 수정 | `implementation(project(":feature:workoutlog:api"))` |
| `feature/my/impl/.../home/MyContracts.kt` | 수정 | `ClickRoutine` / `NavigateToRoutine` |
| `feature/my/impl/.../home/MyViewModel.kt` | 수정 | 분기 한 줄 |
| `feature/my/impl/.../home/composable/MyView.kt` | 수정 | `MyEvents.goToRoutine`, `MyUiEvents.onClickRoutine`, `SectionRow` |
| `feature/my/impl/.../MyNavGraph.kt` | 수정 | `goToRoutine = navigator::navigateToRoutineList` |
| `feature/my/impl/src/test/.../home/MyViewModelTest.kt` | 수정 | `ClickRoutine` → `NavigateToRoutine` |

### 구현 순서

**단위 1 — 저장 계층·도메인·데이터**
- 쓰는 것: `WorkoutEntryDao`, `BodyPlanDatabase`, `LocalModule`, `Migrations.kt`, `WorkoutMapper.kt`, `DataModule`(기존)
- 만드는 것: `RoutineEntity`·`RoutineEntryEntity`·`RoutineSetEntity`·두 POJO·`RoutineDao`·`MIGRATION_9_10`·`10.json`·`insertAllWithSets`, `Routine`·`groupedByBodyPart`·`RoutineRepository`·`WorkoutLogRepository.addEntries`, `RoutineMapper.kt`·`toEntityWithSets`·`RoutineRepositoryImpl`·`addEntries` 구현·바인딩, `FakeRoutineDao`·테스트, 가짜 `WorkoutLogRepository` 5곳 갱신
- 검증: `./gradlew :app:compileDebugKotlin :core:data:testDebugUnitTest :core:domain:test :feature:home:impl:testDebugUnitTest` + `10.json` 생성 확인
- 커밋: `루틴을 저장하는 계층을 만듦`

**단위 2 — 공용 카드·운동 편집 화면의 대상 일반화·NavKey**
- 쓰는 것: 단위 1의 `RoutineRepository`, `Routine`
- 만드는 것: `WorkoutEntryCard`, NavKey·navigate 3개, `WorkoutEntryEditTarget`·State/ViewModel/View 변경, navGraph의 기존 편집 entry 변경, `FakeRoutineRepository`, `WorkoutEntryEditViewModelTest` 갱신
- 검증: `./gradlew :app:compileDebugKotlin :feature:workoutlog:impl:testDebugUnitTest` + `WorkoutEntryEditViewImpl` Preview
- 커밋: `운동 편집 화면이 루틴 항목도 저장할 수 있게 함`

**단위 3 — 루틴 관리·상세 화면과 마이 진입**
- 쓰는 것: 단위 1의 `RoutineRepository`·`groupedByBodyPart`, 단위 2의 `WorkoutEntryCard`·`RoutineEntryEditNavKey`·`navigateToRoutineList`
- 만드는 것: `routinelist`·`routinedetail` 패키지, navGraph entry 2개(+ 단위 2의 편집 entry), 마이 계약·메뉴·navGraph·build.gradle, ViewModel 테스트 2개 + `MyViewModelTest`
- 검증: `./gradlew :app:compileDebugKotlin :feature:workoutlog:impl:testDebugUnitTest :feature:my:impl:testDebugUnitTest` + Preview
- 커밋: `마이페이지에서 부위별 루틴을 만들고 관리할 수 있게 함`

**단위 4 — 일지에서 루틴 불러오기**
- 쓰는 것: 단위 1의 `RoutineRepository.observeRoutines`·`getRoutine`, `WorkoutLogRepository.addEntries`, `groupedByBodyPart`
- 만드는 것: `WorkoutLogState` 3개 필드·Intent 3개·ViewModel 처리, `RoutineSheet`, 하단 버튼, `FakeWorkoutLogRepository.addEntries`, `WorkoutLogViewModelTest` 케이스
- 검증: `./gradlew :app:compileDebugKotlin :feature:workoutlog:impl:testDebugUnitTest` + Preview
- 커밋: `운동일지에서 루틴을 불러와 기록에 넣을 수 있게 함`

### 회귀 대상

| 고치는 것 | 확인할 기존 화면·호출부 |
|---|---|
| `WorkoutLogRepository` 인터페이스 확장 | 구현체 7곳: `WorkoutLogRepositoryImpl`, feature `FakeWorkoutLogRepository`, `AnalyzeWorkoutUseCaseTest`·`GetExerciseTrendUseCaseTest`·`GetMonthlyDayStatusUseCaseTest`·`GetProgressSummaryUseCaseTest`의 내부 가짜, `HomeViewModelTest`의 가짜 |
| DB 버전 9 → 10 | 기존 표를 건드리지 않는지 SQL 확인. 앱 실행 후 기존 기록·메모 유지 |
| `WorkoutEntryEditViewModel` 생성자·State 변경 | `WorkoutLogNavGraph`의 편집 entry, `WorkoutEntryEditViewModelTest` 전체, `WorkoutEntryEditView.kt` Preview 3개 |
| `WorkoutLogView.kt`의 `EntryCard` 제거 | 일간 운동일지 화면의 항목 카드 표기 — 편집 가능/조회 전용 날짜 각각 |
| `WorkoutLogUiEvents`·`MyUiEvents`·`MyEvents` 생성자 확장 | 같은 파일의 `previewUiEvents`, `MyNavGraph` |
| `WorkoutLogViewModel` 생성자에 `RoutineRepository` 추가 | `WorkoutLogViewModelTest`의 생성부 전체 |
| `feature:my:impl` → `feature:workoutlog:api` 의존 추가 | 순환 없음(`workoutlog:impl → my:api`, api끼리 무의존). `./gradlew :app:assembleDebug`로 확인 |

자기 대조: 통과 (대조 15/15)
- 고침: 상태 계약, 화면 구성, 네비게이션, 파일별 작업, 회귀 대상
