# 운동 일지

**작성일**: 2026-09-07

## 설계

### 요약

부위별 운동 기록을 날짜 단위로 남기는 feature를 추가한다. 기록 한 건은 종목 하나와 그 종목으로 수행한 세트 목록이며, 세트마다 무게(또는 각도)와 횟수를 따로 지정한다. 오늘과 어제만 작성·수정할 수 있고, 캘린더에서 날짜를 골라 과거 기록을 조회한다. 작성 기간이 지나도록 기록이 없는 날은 휴식으로 확정돼 캘린더에 그렇게 표시된다. 운동 종목은 기본 목록을 심어 두되 사용자가 직접 추가·수정·삭제할 수 있다.

### 배경

`core:local`은 지금 빌드 스크립트만 있는 빈 껍데기이고, Room이 `gradle/libs.versions.toml`에 없다. 이 작업이 Room 도입의 첫 사례이므로 Database·DI 골격을 여기서 세운다.

식단 일지를 만드는 다른 세션(`bodyplan-a4`)과 세 가지를 공유하기로 합의했다. 세 항목 모두 이 작업이 먼저 만들고 상대가 그대로 쓴다.

| 공유 대상 | 합의 내용 |
|---|---|
| `BodyPlanCalendar` | 시그니처를 이 계획의 `화면 구성` 절대로 고정한다. 셀 내용은 `dayContent` 슬롯으로 열어 두 feature가 각자 그린다 |
| `IsEditableDateUseCase` | `core:domain`에 두고 두 feature가 같은 판정을 쓴다. 자정 경계는 양쪽 다 화면 진입 시점에만 판정한다 |
| Room 인프라 | 이 작업이 `libs.versions.toml` 등록과 `BodyPlanDatabase` 생성까지 올린다. 식단 Entity·DAO는 상대가 얹으며, `entities` 배열에 줄을 더하고 `version`을 올린다 |
| 마이그레이션 정책 | 배포 전까지 `fallbackToDestructiveMigration(dropAllTables = true)`을 쓴다. 스키마가 바뀔 때마다 개발 기기의 데이터가 지워지는 것을 양쪽이 전제로 둔다. 첫 배포 직전에 설정을 떼고 그 시점 스키마를 확정본으로 삼는다 |

### 확정 전제

사용자가 답을 준 결정이다. 임의 판단이 아니다.

| 항목 | 결정 |
|---|---|
| 구축 순서 | 단위별 순차. 각 단위마다 계획 대조·리뷰·테스트를 밟는다 |
| 영속 저장 | Room 도입. 인메모리·DataStore는 쓰지 않는다 |
| 캘린더 UI | 직접 구현. 외부 캘린더 라이브러리를 넣지 않는다 |
| 작업 브랜치 | `feature/workout-log` |
| 종목 편집과 과거 기록 | 기록 행에 종목명·부위를 스냅샷으로 함께 저장한다. 종목은 soft delete로 목록에서만 숨긴다 |
| 각도 단위 | 15도 단위 0~60도. 0 / 15 / 30 / 45 / 60 다섯 단계 |
| 세트 수 | 1~10 |
| 종목 관리 위치 | 별도 관리 화면. 기록 작성 화면은 고르는 일만 한다 |
| 무게 단위 | 5kg 단위 5~100kg. 20단계 (요청 원문) |
| 횟수 단위 | 4개 단위 4~20개. 4 / 8 / 12 / 16 / 20 다섯 단계 (요청 원문) |
| 세트 기록 방식 | 세트마다 무게와 횟수를 따로 지정한다. 전체 세트에 같은 값을 적용하지 않는다 (요청으로 변경됨) |
| 휴식 판정 | 기록이 없는 채로 작성 가능 기간이 지난 날은 휴식으로 본다. 즉 그저께 이전의 빈 날이다 (요청으로 추가됨) |
| 마이그레이션 | 정식 마이그레이션. 처음에는 배포 전까지 파괴적 마이그레이션으로 합의했으나, 사용자 지시로 기존 기록을 지우지 않도록 바꿨다 |

무게·횟수 단위는 이제 **세트마다** 적용된다. 같은 종목의 1세트가 40kg 12회, 2세트가 35kg 8회일 수 있다.

### 수용 조건

**저장소·도메인**

- [ ] 앱을 처음 실행하면 기본 종목 25개가 6개 부위에 심어져 있다.
- [ ] `IsEditableDateUseCase`가 오늘과 어제에 `true`, 그저께와 내일에 `false`를 낸다.
- [ ] 세트별 무게와 횟수가 서로 다른 기록을 저장하고 그대로 다시 읽을 수 있다.
- [ ] 기록을 읽을 때 세트가 저장한 순서대로 나온다.
- [ ] 기록을 수정해 세트 수를 줄이면 사라진 세트가 저장소에서도 지워진다.
- [ ] 기록을 삭제하면 그 기록의 세트도 함께 지워진다.
- [ ] 종목을 삭제한 뒤에도 그 종목으로 남긴 과거 기록의 종목명과 부위가 그대로 조회된다.
- [ ] 종목 이름을 바꿔도 이미 저장된 기록의 종목명은 바뀌지 않는다.
- [ ] 삭제한 종목이 `observeExercises(bodyPart)` 결과에 나오지 않는다.
- [ ] `observeBodyPartsInRange`가 기록이 없는 날짜를 결과 맵에서 뺀다.
- [ ] 저장·삭제가 실패하면 예외가 호출부로 전파된다. Repository가 삼키지 않는다.

**기록 조회 화면**

- [ ] 오늘 날짜로 진입하면 항목 추가 버튼과 각 항목의 삭제 버튼이 보인다.
- [ ] 그저께 이전 날짜로 진입하면 추가·삭제·수정 진입점이 보이지 않는다.
- [ ] 항목 행이 종목명과 함께 세트별 무게·횟수를 한 줄씩 보여준다.
- [ ] 항목을 추가하면 목록에 즉시 나타난다.
- [ ] 기존 항목을 눌러 수정하면 저장된 세트가 그대로 채워진 상태로 편집 화면이 열린다.
- [ ] 기록이 하나도 없는 날에 빈 상태 문구가 보인다.
- [ ] 조회가 실패하면 에러 문구와 재시도 버튼이 보이고, 재시도가 다시 조회한다.

**항목 편집 화면**

- [ ] 부위를 고르기 전에는 종목 목록이 비어 있다.
- [ ] 부위를 고르면 그 부위의 삭제되지 않은 종목만 나온다.
- [ ] 종목을 고르면 세트가 한 줄 생긴다.
- [ ] 세트를 추가하면 앞 세트 값을 물려받지 않고 기본값 줄이 뒤에 붙는다. 앞 세트는 그대로 남는다.
- [ ] 세트마다 무게(또는 각도)와 횟수를 서로 다르게 고를 수 있다.
- [ ] 강도 타입이 무게인 종목은 세트 줄에 5~100kg 선택지가, 각도인 종목은 0~60도 선택지가 나온다.
- [ ] 세트가 10개면 세트 추가 버튼이 비활성이다.
- [ ] 세트가 하나만 남으면 그 세트의 삭제 버튼이 비활성이다.
- [ ] 종목을 고르지 않으면 저장 버튼이 비활성이다.
- [ ] 종목을 바꾸면 세트 목록이 한 줄로 초기화된다.
- [ ] 고른 부위에 종목이 하나도 없으면 빈 상태 문구가 보인다.
- [ ] 저장이 실패하면 화면이 닫히지 않고 실패 문구가 뜬다.
- [ ] 저장에 성공하면 저장 중 표시가 풀린다. 같은 화면을 다시 열었을 때 저장 버튼이 잠겨 있으면 안 된다.

**캘린더 화면**

- [ ] 기록이 있는 날짜 셀에 그 날 수행한 부위의 점이 부위 색으로 표시된다.
- [ ] 기록이 없고 그저께 이전인 날짜 셀에 휴식 표시가 나온다.
- [ ] 오늘과 어제가 기록이 없어도 휴식으로 표시되지 않는다. 아직 작성할 수 있기 때문이다.
- [ ] 오늘 이후의 날짜 셀에는 아무 표시가 없다.
- [ ] 그저께 이전이던 빈 날에 뒤늦게 기록을 넣을 수는 없다. 그 날은 휴식으로 남는다.
- [ ] 이전·다음 달 버튼이 표시 월을 바꾸고 그 달의 기록을 다시 읽는다.
- [ ] 날짜를 누르면 그 날짜의 기록 화면으로 이동한다.
- [ ] 조회가 실패하면 에러 문구와 재시도 버튼이 보인다.

**종목 관리 화면**

- [ ] 부위 탭을 고르면 그 부위의 종목만 나온다.
- [ ] 종목을 추가하면 목록에 즉시 나타나고 기록 작성 화면에서도 고를 수 있다.
- [ ] 이름이 빈 상태로 저장하려 하면 저장되지 않고 안내 문구가 뜬다.
- [ ] 종목을 삭제하면 목록에서 사라진다.
- [ ] 고른 부위에 종목이 없으면 빈 상태 문구가 보인다.
- [ ] 추가·수정·삭제가 실패하면 안내 문구가 뜨고 목록은 그대로 남는다.

### 비목표

- 세트별 휴식 시간, 운동 시간, 메모, 총 볼륨 통계.
- 세트 순서를 드래그로 바꾸는 것. 추가는 맨 뒤에 붙고 삭제는 그 줄만 지운다.
- 기록의 날짜 이동, 다른 날짜로 복사.
- 휴식으로 확정된 날을 되돌리는 것. 작성 가능 기간이 지나면 그 날은 편집되지 않는다.
- 종목 순서를 사용자가 바꾸는 것. 목록은 id 오름차순 고정이다.
- 기본 종목을 되살리는 초기화 기능.
- 식단 일지의 Entity·DAO·화면. 상대 세션이 만든다.
- `core:designsystem`의 `Color.kt`에 남아 있는 템플릿 색(`Purple80` 등) 정리. 이번 변경에 필요하지 않다.
- `HomeView`의 화면 구성 개편. 진입 버튼 한 개만 더한다.
- 첫 버전의 마이그레이션. `version = 1`이 첫 스키마이므로 이 작업에는 마이그레이션이 없다. 버전을 올리는 뒤 작업이 마이그레이션을 더한다.

### 데이터 계약

#### 테이블 `exercise`

| 컬럼 | 타입 | 도메인 매핑 | 귀착지 |
|---|---|---|---|
| `id` | `Long` PK autoGenerate | `Exercise.id` | `ExerciseManageState.exercises`, `WorkoutEntryEditState.selectedExercise` |
| `bodyPart` | `String` | `Exercise.bodyPart` (`BodyPart.name` ↔ `valueOf`) | 관리 화면의 부위 탭 필터 |
| `name` | `String` | `Exercise.name` | 관리 화면 목록 행, 편집 화면 종목 칩 |
| `intensityType` | `String` | `Exercise.intensityType` (`IntensityType.name` ↔ `valueOf`) | 편집 화면이 세트 줄에 무게·각도 중 무엇을 그릴지 결정 |
| `isDeleted` | `Boolean` | `Exercise.isDeleted` | 목록 조회 필터. 화면에 직접 그리지 않는다 |

#### 테이블 `workout_entry`

기록 한 건이다. 세트는 갖지 않는다.

| 컬럼 | 타입 | 도메인 매핑 | 귀착지 |
|---|---|---|---|
| `id` | `Long` PK autoGenerate | `WorkoutEntry.id` | 수정·삭제 대상 지정, `workout_set.entryId`의 참조 대상 |
| `dateEpochDay` | `Long` (인덱스) | `WorkoutLog.date` (`LocalDate.toEpochDay()` ↔ `LocalDate.ofEpochDay`) | 날짜별 조회 키, 캘린더 맵의 키 |
| `exerciseId` | `Long` | `WorkoutEntry.exerciseId` | 수정 시 편집 화면의 종목 재선택 |
| `exerciseName` | `String` | `WorkoutEntry.exerciseName` | 기록 목록 행 제목. 종목이 지워져도 이 값을 그린다 |
| `bodyPart` | `String` | `WorkoutEntry.bodyPart` | 기록 목록 행 부위 라벨, 캘린더 셀 점 색 |
| `intensityType` | `String` | `WorkoutEntry.intensityType` | 세트의 `intensityValue`를 `Intensity.Weight` 또는 `Intensity.Angle` 중 무엇으로 되살릴지 결정 |
| `createdAtMillis` | `Long` | 도메인 미노출 | **미사용**. 같은 날 항목의 정렬 안정성만 담당한다 |

#### 테이블 `workout_set`

세트 하나다. 세트별 무게·횟수를 따로 두기 위해 분리했다.

| 컬럼 | 타입 | 도메인 매핑 | 귀착지 |
|---|---|---|---|
| `id` | `Long` PK autoGenerate | 도메인 미노출 | **미사용**. Room의 행 식별에만 쓴다 |
| `entryId` | `Long` (인덱스, FK → `workout_entry.id`, `onDelete = CASCADE`) | 도메인 미노출 | 부모 기록과의 연결. `@Relation`이 쓴다 |
| `setNumber` | `Int` | 리스트 인덱스 + 1 | 기록 행과 편집 화면의 `N세트` 라벨. 정렬 키 |
| `repeatCount` | `Int` | `WorkoutSet.repeatCount` | 기록 행, 편집 화면의 횟수 칩 |
| `intensityValue` | `Int` | `WorkoutSet.intensity`의 값 (`Weight.kilograms` 또는 `Angle.degrees`) | 기록 행, 편집 화면의 무게·각도 칩 |

`intensityType`을 세트가 아니라 기록에 둔 것은 한 기록의 모든 세트가 같은 종목이라 강도 타입이 같기 때문이다. `exercise`와 중복돼 보이지만 스냅샷이다. 종목의 강도 타입을 나중에 바꿔도 과거 기록의 표기가 바뀌면 안 된다.

`onDelete = CASCADE`가 기록 삭제 시 세트 삭제를 맡는다. Repository가 따로 지우지 않는다. Room의 외래 키는 `PRAGMA foreign_keys`가 켜져 있어야 동작하므로, `LocalModule`이 Database를 만들 때 이를 확인한다.

#### 조회 POJO

```kotlin
data class WorkoutEntryWithSets(
    @Embedded val entry: WorkoutEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val sets: List<WorkoutSetEntity>,
)
```

매핑 시 `sets`를 `setNumber` 오름차순으로 정렬한다. `@Relation`은 순서를 보장하지 않는다.

#### DAO 쿼리

| DAO | 함수 | 쿼리 |
|---|---|---|
| `ExerciseDao` | `observeByBodyPart(bodyPart: String): Flow<List<ExerciseEntity>>` | `WHERE bodyPart = :bodyPart AND isDeleted = 0 ORDER BY id ASC` |
| `ExerciseDao` | `getById(id: Long): ExerciseEntity?` | `WHERE id = :id` |
| `ExerciseDao` | `insert(entity: ExerciseEntity): Long` | `@Insert` |
| `ExerciseDao` | `updateFields(id: Long, bodyPart: String, name: String, intensityType: String)` | `UPDATE exercise SET bodyPart = :bodyPart, name = :name, intensityType = :intensityType WHERE id = :id`. 엔티티를 통째로 덮지 않는다 — 호출부가 들고 온 `isDeleted` 기본값이 지워진 종목을 되살린다 |
| `ExerciseDao` | `markDeleted(id: Long)` | `UPDATE exercise SET isDeleted = 1 WHERE id = :id` |
| `WorkoutEntryDao` | `observeByDate(epochDay: Long): Flow<List<WorkoutEntryWithSets>>` | `@Transaction`, `WHERE dateEpochDay = :epochDay ORDER BY createdAtMillis ASC, id ASC` |
| `WorkoutEntryDao` | `observeInRange(from: Long, to: Long): Flow<List<DateBodyPart>>` | `SELECT DISTINCT dateEpochDay, bodyPart FROM workout_entry WHERE dateEpochDay BETWEEN :from AND :to`. 캘린더는 세트를 읽지 않는다 |
| `WorkoutEntryDao` | `getWithSets(id: Long): WorkoutEntryWithSets?` | `@Transaction`, `WHERE id = :id` |
| `WorkoutEntryDao` | `insert(entity: WorkoutEntryEntity): Long` | `@Insert` |
| `WorkoutEntryDao` | `update(entity: WorkoutEntryEntity)` | `@Update` |
| `WorkoutEntryDao` | `deleteById(id: Long)` | `DELETE FROM workout_entry WHERE id = :id` |
| `WorkoutEntryDao` | `insertSets(sets: List<WorkoutSetEntity>)` | `@Insert` |
| `WorkoutEntryDao` | `deleteSetsByEntryId(entryId: Long)` | `DELETE FROM workout_set WHERE entryId = :entryId` |
| `WorkoutEntryDao` | `replaceSets(entryId: Long, sets: List<WorkoutSetEntity>)` | `@Transaction` 기본 구현. `deleteSetsByEntryId` 후 `insertSets` |
| `WorkoutEntryDao` | `insertWithSets(entity: WorkoutEntryEntity, sets: List<WorkoutSetEntity>): Long` | `@Transaction` 기본 구현. 기록과 세트를 한 트랜잭션에 넣는다 |
| `WorkoutEntryDao` | `updateWithSets(entity: WorkoutEntryEntity, sets: List<WorkoutSetEntity>)` | `@Transaction` 기본 구현. `update` 후 `replaceSets` |

`DateBodyPart`는 `dateEpochDay: Long`과 `bodyPart: String` 두 컬럼만 담는 조회 전용 data class다. `core:local`에 둔다.

수정은 세트를 통째로 갈아끼운다. `replaceSets`가 그 단위를 트랜잭션으로 묶는다. 세트 줄마다 diff를 계산하지 않는 이유는 세트가 최대 10개라 이득이 없기 때문이다.

#### 기본 종목 seed

`RoomDatabase.Callback.onCreate`에서 넣는다. 괄호 안은 강도 타입이다.

| 부위 | 종목 |
|---|---|
| `CHEST` | 인클라인 벤치프레스 머신(무게) / 플랫 벤치프레스 머신(무게) / 팩덱 플라이 머신(무게) / 체스트프레스 머신(무게) / 푸쉬업(각도) |
| `BACK` | 랫풀다운(무게) / 티바로우 하부(무게) / 티바로우 상부(무게) / 케이블로우(무게) / 바벨로우(무게) / 원암로우 머신(무게) |
| `SHOULDER` | 사이드 레터럴 레이즈 머신(무게) / 리버스 펙덱 플라이(무게) / 숄더프레스 머신(무게) / OHP(무게) |
| `LEG` | 레그 익스텐션(무게) / 레그컬(무게) / 레그프레스(무게) / 스쿼트(무게) |
| `BICEPS` | 해머컬(무게) / 덤벨컬(무게) / 피처컬 머신(무게) |
| `TRICEPS` | 케이블 푸쉬다운(무게) / 라잉 트라이셉스 익스텐션(무게) / 케이블 오버헤드 익스텐션(무게) |

요청 원문의 `티바로우(하부/상부)`는 강도를 따로 기록해야 하므로 두 종목으로 나눴다. `라잉트라이라이셉스익스텐션`은 `라잉 트라이셉스 익스텐션`으로 적었다.

#### 도메인 타입

`core:domain`은 순수 JVM이고 `minSdk`가 28이므로 `java.time`을 쓴다. 리포에 대응하는 날짜 타입은 없다.

```kotlin
enum class BodyPart { CHEST, BACK, SHOULDER, LEG, BICEPS, TRICEPS }

enum class IntensityType { WEIGHT, ANGLE }

sealed interface Intensity {
    val value: Int

    data class Weight(val kilograms: Int) : Intensity {
        override val value: Int get() = kilograms
    }

    data class Angle(val degrees: Int) : Intensity {
        override val value: Int get() = degrees
    }
}

data class Exercise(
    val id: Long,
    val bodyPart: BodyPart,
    val name: String,
    val intensityType: IntensityType,
    val isDeleted: Boolean = false,
)

data class WorkoutSet(
    val repeatCount: Int,
    val intensity: Intensity,
)

data class WorkoutEntry(
    val id: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val bodyPart: BodyPart,
    val intensityType: IntensityType,
    val sets: List<WorkoutSet>,
)

data class WorkoutLog(
    val date: LocalDate,
    val entries: List<WorkoutEntry>,
)

sealed interface DayStatus {
    /** 기록이 있는 날. 그 날 수행한 부위 집합을 담는다. */
    data class Recorded(val bodyParts: Set<BodyPart>) : DayStatus

    /** 아직 작성할 수 있는 빈 날. 오늘과 어제다. */
    data object Pending : DayStatus

    /** 작성 기간이 지나도록 비어 있는 날. 휴식으로 확정한다. */
    data object Rest : DayStatus

    /** 아직 오지 않은 날. */
    data object Upcoming : DayStatus
}
```

`DayStatus`는 저장하지 않는다. 기록 유무와 오늘 날짜만으로 매번 도출한다. 어제까지 `Pending`이던 날이 시간이 지나면 저절로 `Rest`가 되며, 이 때문에 휴식을 테이블에 적어 둘 수 없다.

`Intensity.value`는 저장 시 `intensityValue` 컬럼에 그대로 들어간다. 짝이 되는 `Intensity.of(type, value)`가 반대 방향을 맡아, 매핑과 화면이 같은 `when`을 각자 쓰지 않게 한다.

선택지 값은 `core:domain`의 `WorkoutOptions` object에 둔다. 화면 둘이 같은 목록을 그린다.

```kotlin
object WorkoutOptions {
    val weightKilograms: List<Int> = (5..100 step 5).toList()
    val angleDegrees: List<Int> = (0..60 step 15).toList()
    val repeatCounts: List<Int> = (4..20 step 4).toList()
    const val MAX_SET_COUNT: Int = 10

    fun defaultIntensity(type: IntensityType): Intensity
}
```

`defaultIntensity`는 `WEIGHT`면 `Intensity.Weight(5)`, `ANGLE`이면 `Intensity.Angle(0)`을 낸다. 종목을 처음 고를 때 만드는 첫 세트가 이 값을 쓴다.

#### Repository·UseCase 시그니처

```kotlin
interface ExerciseRepository {
    fun observeExercises(bodyPart: BodyPart): Flow<List<Exercise>>
    suspend fun getExercise(id: Long): Exercise?
    suspend fun addExercise(bodyPart: BodyPart, name: String, intensityType: IntensityType): Long
    suspend fun updateExercise(exercise: Exercise)
    suspend fun deleteExercise(id: Long)
}

interface WorkoutLogRepository {
    fun observeLog(date: LocalDate): Flow<WorkoutLog>
    fun observeBodyPartsInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<BodyPart>>>
    suspend fun getEntry(id: Long): WorkoutEntry?
    suspend fun addEntry(date: LocalDate, exercise: Exercise, sets: List<WorkoutSet>): Long
    suspend fun updateEntry(entryId: Long, exercise: Exercise, sets: List<WorkoutSet>)
    suspend fun deleteEntry(id: Long)
}

class IsEditableDateUseCase @Inject constructor(private val clock: Clock) {
    operator fun invoke(date: LocalDate): Boolean

    /** 기준일을 직접 받는 판정. 한 번 읽은 오늘을 여러 날짜에 적용하는 호출부가 쓴다. */
    operator fun invoke(date: LocalDate, today: LocalDate): Boolean
}

class GetMonthlyDayStatusUseCase @Inject constructor(
    private val workoutLogRepository: WorkoutLogRepository,
    private val isEditableDate: IsEditableDateUseCase,
    private val clock: Clock,
) {
    operator fun invoke(yearMonth: YearMonth): Flow<Map<LocalDate, DayStatus>>
}
```

`GetMonthlyDayStatusUseCase`는 그 달의 모든 날짜를 키로 갖는 맵을 낸다. 값은 이 순서로 정한다.

1. `date > today` → `Upcoming`
2. 그 날에 기록이 있음 → `Recorded(bodyParts)`
3. `isEditableDate(date)` → `Pending`
4. 나머지 → `Rest`

`today`는 Flow를 만드는 시점에 `LocalDate.now(clock)`으로 한 번 읽고, 네 갈래 판정이 모두 그 값을 쓴다. 편집 가능 판정도 기준일을 받는 오버로드로 부른다 — 인자 없는 쪽을 부르면 방출 때마다 시계를 다시 읽어 구독 중 자정을 넘길 때 기준이 어긋난다.

`updateEntry`가 `exercise`를 받는 것은 수정 화면에서 종목을 바꿀 수 있기 때문이다. 종목이 바뀌면 `exerciseId`·`exerciseName`·`bodyPart`·`intensityType` 스냅샷을 새 값으로 갱신하고 세트를 통째로 갈아끼운다. 날짜는 바뀌지 않으므로 인자에 없다.

`Clock`은 `java.time.Clock`이다. Hilt가 `Clock.systemDefaultZone()`을 제공하고, 테스트는 `Clock.fixed`를 넣는다. 판정은 `date == today || date == today.minusDays(1)`이다.

**실패 전파**: Repository는 예외를 잡지 않는다. `suspend` 함수는 던지고, `Flow`는 에러를 흘린다. ViewModel이 `runCatching`과 `catch`로 받아 State에 옮긴다.

### 상태 계약

문자열 State 필드에 담기는 에러 문구는 카피 원문 절에 있다.

#### WorkoutCalendar

| State 필드 | 타입 | 초기값 |
|---|---|---|
| `yearMonth` | `YearMonth` | `YearMonth.now()` |
| `today` | `LocalDate` | `LocalDate.now()`. `BodyPlanCalendar`의 `selectedDate`로 넘겨 오늘 셀을 강조한다 |
| `dayStatuses` | `Map<LocalDate, DayStatus>` | `emptyMap()` |
| `isLoading` | `Boolean` | `false` |
| `errorMessage` | `String?` | `null` |

`Intent`: `ChangeMonth(yearMonth: YearMonth)` / `ClickDate(date: LocalDate)` / `ClickManageExercise` / `ClickRetry`

앞뒤 이동을 따로 두지 않고 옮겨 간 달을 그대로 받는다. `BodyPlanCalendar`의 `onChangeMonth`가 결과 월을 주므로, 나눠 두면 화면이 이동 방향을 되짚어야 한다.

`Effect`: `NavigateToLog(date: LocalDate)` / `NavigateToExerciseManage`

조회 실패 시 `isLoading = false`, `errorMessage`를 채우고 `dayStatuses`는 직전 값을 유지한다. `ClickRetry`가 현재 `yearMonth`로 다시 구독한다. 실패는 Effect로 나가지 않는다.

#### WorkoutLog

| State 필드 | 타입 | 초기값 |
|---|---|---|
| `date` | `LocalDate` | NavKey의 `dateEpochDay`에서 만든 값. 기본값 없음 |
| `entries` | `List<WorkoutEntry>` | `emptyList()` |
| `isEditable` | `Boolean` | `false` |
| `isLoading` | `Boolean` | `false` |
| `errorMessage` | `String?` | `null` |

`Intent`: `ClickAddEntry` / `ClickEntry(id: Long)` / `ClickDeleteEntry(id: Long)` / `ClickBack` / `ClickRetry`

`Effect`: `NavigateToEntryEdit(date: LocalDate, entryId: Long?)` / `GoBack` / `ShowMessage(message: String)`

`isEditable`은 `initializeData()`에서 `IsEditableDateUseCase`로 한 번만 정한다. 자정을 넘겨도 다시 판정하지 않는다. `isEditable`이 `false`면 추가·수정·삭제 진입점이 화면에 없다. 삭제 실패는 `ShowMessage`로 알리고 목록은 그대로 둔다.

#### WorkoutEntryEdit

세트 편집을 담기 위해 화면 전용 입력 타입을 둔다. 도메인 `WorkoutSet`을 그대로 쓰지 않는 이유는 세트 줄마다 안정된 식별자가 필요하기 때문이다. `LazyColumn`의 `key`와 삭제 대상 지정에 쓴다.

```kotlin
internal data class SetInput(
    val id: Long,
    val repeatCount: Int,
    val intensityValue: Int,
)
```

`id`는 저장소와 무관한 화면 안의 일련번호다. `nextSetInputId`가 발급한다.

| State 필드 | 타입 | 초기값 |
|---|---|---|
| `date` | `LocalDate` | NavKey의 `dateEpochDay`에서 만든 값. 기본값 없음 |
| `editingEntryId` | `Long?` | NavKey의 `entryId`. 신규면 `null` |
| `selectedBodyPart` | `BodyPart?` | `null` |
| `exercises` | `List<Exercise>` | `emptyList()` |
| `selectedExercise` | `Exercise?` | `null` |
| `sets` | `List<SetInput>` | `emptyList()` |
| `nextSetInputId` | `Long` | `0L` |
| `isLoading` | `Boolean` | `false` |
| `isSaving` | `Boolean` | `false` |
| `errorMessage` | `String?` | `null` |

파생 값은 State의 계산 프로퍼티로 둔다. 생성자 필드가 아니라 getter이므로 상태 비교에 영향을 주지 않고, `ViewImpl`과 ViewModel이 같은 판정을 각자 쓰지 않게 된다.

| 프로퍼티 | 정의 |
|---|---|
| `canSave` | `selectedExercise != null && sets.isNotEmpty() && !isSaving` |
| `canAddSet` | `selectedExercise != null && sets.size < WorkoutOptions.MAX_SET_COUNT` |
| `canRemoveSet` | `sets.size > 1` |
| `selectableExercises` | `selectedExercise`가 `exercises`에 없으면 앞에 끼운 목록, 아니면 `exercises` 그대로 |

`selectableExercises`가 필요한 이유는 지워진 종목의 기록을 수정할 때다. 그 종목은 `observeExercises`가 내지 않으므로, 그대로 그리면 세트는 보이는데 고른 종목만 사라져 보인다.

`Intent`:

| Intent | 결과 |
|---|---|
| `SelectBodyPart(bodyPart: BodyPart)` | `selectedBodyPart` 교체, `selectedExercise = null`, `sets = emptyList()`, 그 부위 종목 구독 |
| `SelectExercise(id: Long)` | `selectedExercise` 교체, `sets`를 `defaultIntensity` 값으로 만든 한 줄로 초기화 |
| `ClickAddSet` | 기본값 줄을 뒤에 붙인다. `sets.size == MAX_SET_COUNT`면 무시 |
| `ClickRemoveSet(setInputId: Long)` | 그 줄을 지운다. `sets.size == 1`이면 무시 |
| `SelectSetRepeatCount(setInputId: Long, value: Int)` | 그 줄의 `repeatCount` 교체 |
| `SelectSetIntensity(setInputId: Long, value: Int)` | 그 줄의 `intensityValue` 교체 |
| `ClickSave` | 저장 |
| `ClickBack` | `GoBack` |
| `ClickRetry` | `initializeData` 재실행 |

`Effect`: `GoBack` / `ShowMessage(message: String)`

수정 진입이면 `initializeData()`가 `getEntry`로 값을 채운다. `selectedBodyPart`는 기록의 `bodyPart` 스냅샷으로, `selectedExercise`는 `getExercise(exerciseId)`로 복원한다. 종목이 삭제돼 조회되지 않으면 기록 스냅샷으로 `Exercise`를 만들어 `selectedExercise`에 넣는다. 이래야 삭제된 종목의 기록도 세트만 고칠 수 있다. 복원 실패 시 `errorMessage`를 세우고 화면에 남는다.

저장은 `editingEntryId`가 `null`이면 `addEntry`, 아니면 `updateEntry`를 부른다. 성공하면 `GoBack`, 실패하면 `isSaving = false`와 `ShowMessage`로 끝나며 화면을 닫지 않는다. `SetInput`은 `selectedExercise.intensityType`에 따라 `Intensity.Weight` 또는 `Intensity.Angle`로 바뀌어 넘어간다.

#### ExerciseManage

| State 필드 | 타입 | 초기값 |
|---|---|---|
| `selectedBodyPart` | `BodyPart` | `BodyPart.CHEST` |
| `exercises` | `List<Exercise>` | `emptyList()` |
| `editingExercise` | `Exercise?` | `null` |
| `isDialogVisible` | `Boolean` | `false` |
| `dialogName` | `String` | `""` |
| `dialogIntensityType` | `IntensityType` | `IntensityType.WEIGHT` |
| `isLoading` | `Boolean` | `false` |
| `errorMessage` | `String?` | `null` |

`Intent`: `SelectBodyPart(bodyPart: BodyPart)` / `ClickAdd` / `ClickEdit(id: Long)` / `ClickDelete(id: Long)` / `ChangeDialogName(value: String)` / `SelectDialogIntensityType(type: IntensityType)` / `ConfirmDialog` / `DismissDialog` / `ClickBack` / `ClickRetry`

`Effect`: `GoBack` / `ShowMessage(message: String)`

`ClickAdd`는 `editingExercise = null`, `dialogName = ""`, `dialogIntensityType = WEIGHT`로 두고 다이얼로그를 연다. `ClickEdit`은 대상 값을 채워 연다. `ConfirmDialog`는 `dialogName.isBlank()`면 저장하지 않고 `ShowMessage`만 낸다. 저장·삭제 실패는 `ShowMessage`로 알리고 목록을 유지한다.

### 화면 구성

#### 재사용 판정

시그니처는 대상 파일을 열어 확인했다.

| 심볼 | 파일 | 판정 | 시그니처 |
|---|---|---|---|
| `BaseText` | `core/designsystem/.../component/BaseText.kt` | 재사용 | `BaseText(text: String, modifier: Modifier = Modifier, color: Color = BaseTextDefaults.color, style: TextStyle = BaseTextDefaults.style, textAlign: TextAlign? = null, maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip)` |
| `BaseTextField` | `core/designsystem/.../component/BaseTextField.kt` | 재사용 | `BaseTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, placeholder: String? = null, textStyle: TextStyle = BaseTextDefaults.style, keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default, singleLine: Boolean = false, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE)` |
| `VerticalSpacer` / `HorizontalSpacer` | `core/designsystem/.../component/Spacer.kt` | 재사용 | `VerticalSpacer(space: Dp)`, `HorizontalSpacer(space: Dp)`. 기본값 없음 |
| `WeightSpacer` | `core/designsystem/.../component/Spacer.kt` | 재사용 | `RowScope.WeightSpacer(weight: Float = 1f)`, `ColumnScope.WeightSpacer(weight: Float = 1f)` |
| `BodyPlanTheme` | `core/designsystem/.../theme/Theme.kt` | 재사용 | `BodyPlanTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit)`. Preview에 쓴다 |
| `CollectEffect` | `core/ui/coordinator/.../CollectEffect.kt` | 재사용 | `CollectEffect(effect: Flow<E>, onEffect: (E) -> Unit)` |
| `BaseImage` | `core/designsystem/.../component/BaseImage.kt` | 미사용 | 이 feature에 이미지가 없다 |

#### 신규 공용 컴포넌트

**`core:designsystem`** — 도메인에 종속되지 않는다.

```kotlin
@Composable
fun BodyPlanCalendar(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onChangeMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    dayContent: @Composable (LocalDate) -> Unit = {},
)
```

식단 일지 세션과 합의한 시그니처다. 월 헤더(이전·다음 버튼과 `yyyy년 M월`), 요일 헤더, 6주 고정 그리드를 그린다. 셀 아래쪽에 `dayContent`를 놓는다. 이전·다음 달 날짜 칸은 비운다.

```kotlin
@Composable
fun OptionChipRow(
    options: List<Int>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String = "",
)
```

세트 줄의 횟수·무게·각도 세 곳이 같은 모양을 쓴다. 가로 스크롤 칩 줄이다.

**`core:ui:components`** — `BodyPart`에 종속되므로 여기다. 이 모듈에 `:core:domain` 의존을 새로 더해야 한다.

```kotlin
val BodyPart.label: String
val BodyPart.color: Color

@Composable
fun DayStatusIndicator(status: DayStatus, modifier: Modifier = Modifier)

@Composable
fun BodyPartTabRow(selected: BodyPart?, onSelect: (BodyPart) -> Unit, modifier: Modifier = Modifier)
```

`DayStatusIndicator`가 캘린더 셀 안의 표시를 전담한다. `Recorded`면 부위 색 점을 나열하고, `Rest`면 `휴식` 문구를 흐린 색으로 그리며, `Pending`과 `Upcoming`은 아무것도 그리지 않는다. 상태별 분기를 셀 호출부가 아니라 이 컴포넌트가 갖는 이유는 식단 일지도 같은 자리에 자기 표시를 넣기 때문이다. 두 feature가 각자의 표시 컴포넌트를 `dayContent`에 꽂는다.

`BodyPart.color`가 쓰는 6색과 휴식 표시 색은 `core:designsystem`의 `Color.kt`에 더한다.

#### 화면별 트리와 상태

**WorkoutCalendarViewImpl** — `Column`. 상단에 종목 관리 진입 버튼, 그 아래 `BodyPlanCalendar`. `yearMonth`에 `state.yearMonth`, `selectedDate`에 `state.today`를 넘긴다. 이 화면에는 날짜 선택 상태가 없다. 날짜를 누르면 곧바로 기록 화면으로 가므로 `selectedDate`는 오늘 셀을 강조하는 용도로만 쓴다. `dayContent`는 `DayStatusIndicator(state.dayStatuses[date] ?: DayStatus.Pending)`.

| 상태 | 표시 |
|---|---|
| 로딩 | 캘린더 그리드는 그대로 두고 상단에 얇은 진행 표시 |
| 빈 | 해당 없음. 기록이 없어도 달력 자체를 그린다 |
| 에러 | 캘린더 대신 에러 문구와 재시도 버튼 |

**WorkoutLogViewImpl** — `Column`. 상단 바(뒤로, `yyyy년 M월 d일`, `isEditable`이면 추가 버튼), 아래 `LazyColumn`의 항목 카드. 카드 하나는 종목명과 부위 라벨을 머리에 두고, 그 아래 세트를 한 줄씩 나열한다. `isEditable`이면 카드에 삭제 버튼이 붙고 카드 전체가 수정 진입점이다.

| 상태 | 표시 |
|---|---|
| 로딩 | 목록 자리에 진행 표시 |
| 빈 | `기록이 없습니다` |
| 에러 | 에러 문구와 재시도 버튼 |

**WorkoutEntryEditViewImpl** — `Column`. 상단 바(뒤로, 제목), `BodyPartTabRow`, 종목 칩 줄, 그 아래 세트 목록을 `LazyColumn`으로 그린다. 세트 줄 하나는 `N세트` 라벨과 삭제 버튼을 머리에 두고, 횟수 `OptionChipRow`와 강도 `OptionChipRow`를 담는다. 목록 끝에 `세트 추가` 버튼, 하단에 저장 버튼.

| 상태 | 표시 |
|---|---|
| 로딩 | 본문 자리에 진행 표시 |
| 빈 | 부위 미선택이면 `부위를 먼저 선택하세요`. 부위를 골랐는데 종목이 없으면 `이 부위에 등록된 운동이 없습니다`. 종목 미선택이면 세트 목록 자리에 `운동을 선택하세요` |
| 에러 | 에러 문구와 재시도 버튼 |

**ExerciseManageViewImpl** — `Column`. 상단 바(뒤로, 제목, 추가 버튼), `BodyPartTabRow`, `LazyColumn` 목록(이름, 강도 타입 라벨, 수정·삭제 버튼). `isDialogVisible`이면 `AlertDialog`에 `BaseTextField`와 강도 타입 선택.

| 상태 | 표시 |
|---|---|
| 로딩 | 목록 자리에 진행 표시 |
| 빈 | `등록된 운동이 없습니다` |
| 에러 | 에러 문구와 재시도 버튼 |

#### 카피 원문

| 위치 | 문구 |
|---|---|
| 캘린더 상단 버튼 | `운동 종목 관리` |
| 캘린더 월 표기 | `yyyy년 M월` |
| 요일 헤더 | `일` `월` `화` `수` `목` `금` `토` |
| 캘린더 휴식 셀 | `휴식` |
| 기록 화면 제목 | `yyyy년 M월 d일` |
| 기록 화면 추가 버튼 | `운동 추가` |
| 기록 화면 빈 상태 | `기록이 없습니다` |
| 기록 카드의 세트 줄 | `%d세트  %dkg x %d회` / `%d세트  %d도 x %d회` |
| 편집 화면 제목 (신규) | `운동 추가` |
| 편집 화면 제목 (수정) | `운동 수정` |
| 편집 화면 부위 미선택 | `부위를 먼저 선택하세요` |
| 편집 화면 종목 없음 | `이 부위에 등록된 운동이 없습니다` |
| 편집 화면 종목 미선택 | `운동을 선택하세요` |
| 세트 줄 라벨 | `%d세트` |
| 세트 줄 섹션 라벨 | `횟수` `무게` `각도` |
| 세트 추가 버튼 | `세트 추가` |
| 세트 삭제 버튼 contentDescription | `세트 삭제` |
| 편집 화면 저장 버튼 | `저장` |
| 관리 화면 제목 | `운동 종목 관리` |
| 관리 화면 빈 상태 | `등록된 운동이 없습니다` |
| 관리 다이얼로그 제목 (신규) | `종목 추가` |
| 관리 다이얼로그 제목 (수정) | `종목 수정` |
| 관리 다이얼로그 입력 placeholder | `종목 이름` |
| 관리 다이얼로그 강도 타입 라벨 | `무게로 기록` / `각도로 기록` |
| 관리 다이얼로그 버튼 | `확인` / `취소` |
| 이름 미입력 안내 | `종목 이름을 입력하세요` |
| 조회 실패 | `불러오지 못했습니다` |
| 재시도 버튼 | `다시 시도` |
| 저장 실패 | `저장하지 못했습니다` |
| 삭제 실패 | `삭제하지 못했습니다` |
| 부위 라벨 | `가슴` `등` `어깨` `하체` `이두` `삼두` |
| 홈 진입 버튼 | `운동 일지` |

### 네비게이션

`:feature:workoutlog:api`에 둔다.

```kotlin
@Serializable
data object WorkoutCalendarNavKey : BodyPlanNavKey()

@Serializable
data class WorkoutLogNavKey(val dateEpochDay: Long) : BodyPlanNavKey()

@Serializable
data class WorkoutEntryEditNavKey(val dateEpochDay: Long, val entryId: Long? = null) : BodyPlanNavKey()

@Serializable
data object ExerciseManageNavKey : BodyPlanNavKey()

fun BodyPlanNavigator.navigateToWorkoutCalendar()
fun BodyPlanNavigator.navigateToWorkoutLog(date: LocalDate)
fun BodyPlanNavigator.navigateToWorkoutEntryEdit(date: LocalDate, entryId: Long? = null)
fun BodyPlanNavigator.navigateToExerciseManage()
```

`LocalDate`는 `kotlinx.serialization`이 다루지 못하므로 NavKey에는 `dateEpochDay: Long`을 담고, navigate 확장 함수가 `date.toEpochDay()`로 변환한다.

| 경로 | 진입 | 복귀 |
|---|---|---|
| 홈 → 캘린더 | `HomeView`의 `운동 일지` 버튼 | 시스템 뒤로 |
| 캘린더 → 기록 | 날짜 셀 클릭 | 상단 뒤로 버튼, 시스템 뒤로 |
| 캘린더 → 종목 관리 | 상단 `운동 종목 관리` 버튼 | 상단 뒤로 버튼 |
| 기록 → 항목 편집 | `운동 추가` 버튼(신규), 항목 카드 클릭(수정) | 저장 성공 또는 뒤로 |

`WorkoutLogViewModel`과 `WorkoutEntryEditViewModel`은 NavKey 인자를 받으므로 assisted factory로 만든다. `WorkoutCalendarViewModel`과 `ExerciseManageViewModel`은 `@Inject`만 쓴다.

### 버린 안

| 안 | 버린 이유 |
|---|---|
| 인메모리 Repository로 시작 | 과거 날짜 조회가 요구사항이라 영속 저장이 실제로 필요하다. 사용자가 Room을 골랐다 |
| DataStore + JSON 직렬화 | 날짜 범위 조회를 코드로 직접 걸러야 하고 스키마 관리가 없다. 사용자가 버렸다 |
| kizitonwose Calendar 라이브러리 | 요구가 월 이동·날짜 선택·셀 표시뿐이라 의존성을 늘릴 이유가 없다. 사용자가 버렸다 |
| 운동 종목을 도메인 enum으로 고정 | 사용자가 종목을 직접 추가·수정·삭제하겠다고 해서 저장 대상이 됐다 |
| 종목 하드 삭제 후 기록도 삭제 | 기록 손실이 생긴다. 사용자가 스냅샷 + soft delete를 골랐다 |
| 기록 조회 시 종목 테이블 join | 종목명을 바꾸면 과거 기록 표기까지 바뀐다. 사용자가 스냅샷을 골랐다 |
| 기록 한 건에 세트 수·횟수·무게를 한 벌만 두기 | 처음 계획한 형태다. 사용자가 세트별 지정으로 바꿔 `workout_set` 테이블을 분리했다 |
| 세트를 JSON 문자열 한 컬럼에 담기 | 테이블을 안 늘려도 되지만 세트 단위 조회·집계가 막힌다. 세트가 최대 10개라 테이블 분리 비용이 작다 |
| 세트 수정 시 줄 단위 diff 계산 | 세트가 최대 10개라 이득이 없다. `replaceSets`로 통째로 갈아끼운다 |
| 항목 추가·수정을 기록 화면 안의 bottom sheet로 | 기록 화면의 State와 Intent가 두 배가 된다. 별도 화면이 MVI 규격에 맞는다 |
| 종목 관리를 작성 화면 안에서 인라인으로 | 같은 이유. 사용자가 별도 화면을 골랐다 |
| 휴식 상태를 테이블에 저장 | `Pending`인 날이 시간이 지나면 저절로 `Rest`가 되므로, 저장하면 매일 갱신하는 작업이 따라붙는다. 기록 유무와 오늘 날짜로 매번 도출한다 |
| 사용자가 휴식을 직접 표시하는 기능 | 요청은 미작성 상태가 자동으로 휴식이 되는 것이다. 수동 표시는 요청에 없다 |
| 앱 시작 화면을 캘린더로 교체 | 요청에 없다. 홈에 진입 버튼 하나만 더한다 |

## 실행

### 파일별 작업

#### 단위 1 — 도메인·저장소

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `gradle/libs.versions.toml` | 수정 | `room = "2.8.4"`와 `room-runtime`·`room-compiler` 라이브러리 별칭 추가 |
| `core/domain/build.gradle.kts` | 수정 | 코루틴을 `implementation`에서 `api`로. Repository가 `Flow`를 공개 시그니처로 낸다 |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/model/BodyPart.kt` | 신규 | `BodyPart` enum |
| `core/domain/.../model/IntensityType.kt` | 신규 | `IntensityType` enum |
| `core/domain/.../model/Intensity.kt` | 신규 | `Intensity` sealed interface, `value`, `Weight`·`Angle` |
| `core/domain/.../model/Exercise.kt` | 신규 | `Exercise` data class |
| `core/domain/.../model/WorkoutSet.kt` | 신규 | `WorkoutSet` data class |
| `core/domain/.../model/WorkoutEntry.kt` | 신규 | `WorkoutEntry` data class |
| `core/domain/.../model/WorkoutLog.kt` | 신규 | `WorkoutLog` data class |
| `core/domain/.../model/DayStatus.kt` | 신규 | `DayStatus` sealed interface 4종 |
| `core/domain/.../model/WorkoutOptions.kt` | 신규 | 무게·각도·횟수 선택지, `MAX_SET_COUNT`, `defaultIntensity` |
| `core/domain/.../repository/ExerciseRepository.kt` | 신규 | 인터페이스 |
| `core/domain/.../repository/WorkoutLogRepository.kt` | 신규 | 인터페이스 |
| `core/domain/.../usecase/IsEditableDateUseCase.kt` | 신규 | `Clock` 주입, 오늘·어제 판정 |
| `core/domain/.../usecase/GetMonthlyDayStatusUseCase.kt` | 신규 | 월 단위 `DayStatus` 맵 산출 |
| `core/local/build.gradle.kts` | 수정 | `bodyplan.android.hilt`, Room 의존성, KSP `room.schemaLocation` 인자, `implementation(project(":core:domain"))` 추가. seed와 컬럼 값이 도메인 enum 이름을 그대로 쓰므로 문자열로 베끼지 않는다 |
| `core/local/src/main/java/kr/sdbk/bodyplan/core/local/entity/ExerciseEntity.kt` | 신규 | Entity |
| `core/local/.../entity/WorkoutEntryEntity.kt` | 신규 | Entity |
| `core/local/.../entity/WorkoutSetEntity.kt` | 신규 | Entity. FK CASCADE, `entryId` 인덱스 |
| `core/local/.../entity/WorkoutEntryWithSets.kt` | 신규 | `@Embedded` + `@Relation` POJO |
| `core/local/.../entity/DateBodyPart.kt` | 신규 | 캘린더 조회 전용 POJO |
| `core/local/.../dao/ExerciseDao.kt` | 신규 | DAO |
| `core/local/.../dao/WorkoutEntryDao.kt` | 신규 | 기록·세트 DAO. `replaceSets` 트랜잭션 포함 |
| `core/local/.../BodyPlanDatabase.kt` | 신규 | `@Database(version = 1)`, DAO 접근자 |
| `core/local/.../DefaultExercises.kt` | 신규 | seed 목록 |
| `core/local/.../di/LocalModule.kt` | 신규 | Database·DAO 제공, seed `Callback` 설치, 배포 전 파괴적 마이그레이션 설정 |
| `core/data/.../mapper/WorkoutMapper.kt` | 신규 | Entity ↔ 도메인 매핑. 세트 정렬 포함 |
| `core/data/.../repository/ExerciseRepositoryImpl.kt` | 신규 | DAO를 도메인으로 매핑 |
| `core/data/.../repository/WorkoutLogRepositoryImpl.kt` | 신규 | 같음. 저장 시 기록·세트를 함께 쓴다 |
| `core/data/.../di/DataModule.kt` | 신규 | Repository 바인딩, `Clock` 제공 |

#### 단위 2 — 기록 화면과 항목 편집 화면

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `settings.gradle.kts` | 수정 | `:feature:workoutlog:api`·`:feature:workoutlog:impl` 등록 |
| `feature/workoutlog/api/build.gradle.kts` | 신규 | `bodyplan.android.feature.api`, namespace |
| `feature/workoutlog/api/.../WorkoutLogNavKey.kt` | 신규 | NavKey 4종과 navigate 확장 함수 4종 |
| `feature/workoutlog/impl/build.gradle.kts` | 신규 | `bodyplan.android.feature.impl`, namespace |
| `feature/workoutlog/impl/.../WorkoutLogContracts.kt` | 신규 | State·Intent·Effect |
| `feature/workoutlog/impl/.../WorkoutLogViewModel.kt` | 신규 | assisted factory, 목록 구독, 삭제 |
| `feature/workoutlog/impl/.../WorkoutLogView.kt` | 신규 | Events·UiEvents·View·ViewImpl·Preview. 세트 줄 렌더 포함 |
| `feature/workoutlog/impl/.../WorkoutEntryEditContracts.kt` | 신규 | State·Intent·Effect, `SetInput` |
| `feature/workoutlog/impl/.../WorkoutEntryEditViewModel.kt` | 신규 | assisted factory, 종목 구독, 세트 추가·삭제·변경, 저장 |
| `feature/workoutlog/impl/.../WorkoutEntryEditView.kt` | 신규 | 같은 골격. 세트 목록 편집 UI |
| `feature/workoutlog/impl/.../WorkoutLogNavGraph.kt` | 신규 | 두 entry 등록 |
| `core/ui/components/build.gradle.kts` | 수정 | `api(project(":core:domain"))` 추가 |
| `core/ui/components/.../BodyPartUi.kt` | 신규 | `BodyPart.label`·`BodyPart.color`, `BodyPartTabRow` |
| `core/designsystem/.../theme/Color.kt` | 수정 | 부위 6색 추가 |
| `core/designsystem/.../component/OptionChipRow.kt` | 신규 | 선택지 칩 줄 |
| `app/.../navigation/BodyPlanNavDisplay.kt` | 수정 | `workoutLogNavGraph(navigator)` 호출과 `entryDecorators` 추가. `NavDisplay` 기본값에는 ViewModel 저장소 데코레이터가 없어, 없으면 모든 화면의 ViewModel이 액티비티에 붙는다 |

#### 단위 3 — 캘린더 화면

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/designsystem/.../component/BodyPlanCalendar.kt` | 신규 | 월 그리드. 식단 일지와 공유하는 시그니처 |
| `core/ui/components/.../BodyPartUi.kt` | 수정 | `DayStatusIndicator` 추가 |
| `feature/workoutlog/impl/.../WorkoutCalendarContracts.kt` | 신규 | State·Intent·Effect |
| `feature/workoutlog/impl/.../WorkoutCalendarViewModel.kt` | 신규 | 월 범위 구독, 월 이동 |
| `feature/workoutlog/impl/.../WorkoutCalendarView.kt` | 신규 | 같은 골격 |
| `feature/workoutlog/impl/.../WorkoutLogNavGraph.kt` | 수정 | 캘린더 entry 추가 |
| `feature/home/impl/.../HomeView.kt` | 수정 | `운동 일지` 버튼과 `onClickWorkoutLog` UiEvent 추가 |
| `feature/home/impl/.../HomeNavGraph.kt` | 수정 | `HomeEvents`에 `goToWorkoutLog` 추가 |
| `feature/home/impl/build.gradle.kts` | 수정 | `implementation(project(":feature:workoutlog:api"))` 추가 |

`HomeContracts.kt`는 고치지 않는다. 진입은 화면 밖으로 나가는 이벤트라 `HomeEvents`로 처리하며 State·Intent·Effect가 늘지 않는다.

#### 단위 4 — 종목 관리 화면

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `feature/workoutlog/impl/.../ExerciseManageContracts.kt` | 신규 | State·Intent·Effect |
| `feature/workoutlog/impl/.../ExerciseManageViewModel.kt` | 신규 | 부위별 구독, 추가·수정·삭제 |
| `feature/workoutlog/impl/.../ExerciseManageView.kt` | 신규 | 같은 골격, 다이얼로그 포함 |
| `feature/workoutlog/impl/.../WorkoutLogNavGraph.kt` | 수정 | 관리 entry 추가 |

### 구현 순서

단위마다 검증을 통과한 뒤 커밋한다. 단위 경계가 커밋 경계다.

**단위 1 — 도메인·저장소**

- 쓰는 것: `bodyplan.jvm.library`, `bodyplan.android.library`, `bodyplan.android.hilt` (기존)
- 만드는 것: `BodyPart`, `IntensityType`, `Intensity`, `Exercise`, `WorkoutSet`, `WorkoutEntry`, `WorkoutLog`, `WorkoutOptions`, `DayStatus`, `ExerciseRepository`, `WorkoutLogRepository`, `IsEditableDateUseCase`, `GetMonthlyDayStatusUseCase`, `ExerciseEntity`, `WorkoutEntryEntity`, `WorkoutSetEntity`, `WorkoutEntryWithSets`, `DateBodyPart`, `ExerciseDao`, `WorkoutEntryDao`, `BodyPlanDatabase`, `DefaultExercises`, `LocalModule`, `WorkoutMapper`, `ExerciseRepositoryImpl`, `WorkoutLogRepositoryImpl`, `DataModule`
- 검증: `./gradlew :core:domain:test :app:compileDebugKotlin`
- 리뷰·테스트: code-reviewer와 test-engineer에 위임한다. 도메인·데이터 계층 변경이므로 테스트가 필수다

**단위 2 — 기록 화면과 항목 편집 화면**

- 쓰는 것: 단위 1의 `WorkoutLogRepository`, `ExerciseRepository`, `IsEditableDateUseCase`, `WorkoutOptions`, `BodyPart`, `IntensityType`, `Intensity`, `Exercise`, `WorkoutSet`, `WorkoutEntry`
- 만드는 것: `:feature:workoutlog:api`·`:impl` 모듈, NavKey 4종과 navigate 확장 4종, `SetInput`, `WorkoutLogContracts`·`ViewModel`·`View`, `WorkoutEntryEditContracts`·`ViewModel`·`View`, `WorkoutLogNavGraph`, `BodyPartUi`의 `label`·`color`·`BodyPartTabRow`, `OptionChipRow`, `Color.kt`의 부위 6색
- 이 단위가 NavKey 4종을 전부 만든다. 단위 3·4가 나머지 둘을 쓴다
- 검증: `./gradlew :app:assembleDebug :feature:workoutlog:impl:testDebugUnitTest` + `WorkoutLogViewImplPreview`·`WorkoutEntryEditViewImplPreview` 렌더
- 리뷰·테스트: 두 ViewModel이 대상이다
- ViewModel 테스트는 `Dispatchers.setMain`에 즉시 실행 디스패처를 넣고, `uiState`와 `effect` 수집기를 그 디스패처에서 돌린다. 큐에 쌓는 디스패처로 수집기를 돌리면 Effect가 한 번의 진행으로 도달하지 않아 검증 시점이 어긋난다

**단위 3 — 캘린더 화면**

- 쓰는 것: 단위 1의 `GetMonthlyDayStatusUseCase`와 `DayStatus`, 단위 2의 `WorkoutCalendarNavKey`·`WorkoutLogNavKey`·`ExerciseManageNavKey`와 `BodyPartUi`
- 만드는 것: `BodyPlanCalendar`, `DayStatusIndicator`, `WorkoutCalendarContracts`·`ViewModel`·`View`, `HomeView`의 진입 버튼
- 종목 관리 진입점(`ClickManageExercise`·`NavigateToExerciseManage`와 상단 버튼)은 단위 4로 미룬다. 화면이 없는 목적지로 가는 버튼을 중간 커밋에 남기지 않기 위한 것이다
- 검증: `./gradlew :app:assembleDebug` + `WorkoutCalendarViewImplPreview`·`HomeViewImplPreview` 렌더
- 리뷰·테스트: `WorkoutCalendarViewModel`이 대상이다. `BodyPlanCalendar`는 Compose UI라 Preview 확인으로 대체한다
- 이 단위가 끝나면 식단 일지 세션에 `BodyPlanCalendar`가 올라갔음을 알린다

**단위 4 — 종목 관리 화면**

- 쓰는 것: 단위 1의 `ExerciseRepository`, 단위 2의 `ExerciseManageNavKey`와 `BodyPartTabRow`
- 만드는 것: `ExerciseManageContracts`·`ViewModel`·`View`, navGraph의 관리 entry
- 검증: `./gradlew :app:assembleDebug` + `ExerciseManageViewImplPreview` 렌더
- 리뷰·테스트: `ExerciseManageViewModel`이 대상이다

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| `gradle/libs.versions.toml`에 Room 추가 | 전체 빌드. `./gradlew :app:assembleDebug`가 통과하는지 |
| `core/local/build.gradle.kts`에 Hilt·KSP 추가 | `:app` 조립. `android.disallowKotlinSourceSets=false`가 이미 있으므로 KSP 소스셋은 문제되지 않는다 |
| `core/ui/components/build.gradle.kts`에 `:core:domain` 추가 | `core:ui:components`를 쓰는 모든 feature. 현재는 `feature:home:impl`뿐이다 |
| `core/designsystem/.../theme/Color.kt`에 색 추가 | `BodyPlanTheme`를 쓰는 모든 Preview. 기존 색은 건드리지 않는다 |
| `app/.../navigation/BodyPlanNavDisplay.kt` | 홈 화면 진입. 시작 NavKey는 `HomeNavKey` 그대로다 |
| `feature/home/impl`의 `HomeView`·`HomeNavGraph` | 홈 화면. `HomeViewImplPreview`가 계속 렌더되는지 |
| `core:local`에 `BodyPlanDatabase` 신설 | 식단 일지 세션이 이 클래스에 Entity를 얹는다. `entities` 배열 변경 시 양쪽이 조율한다 |
