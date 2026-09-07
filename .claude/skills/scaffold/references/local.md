# 로컬 저장소 생성

목적: Room의 Entity·DAO와 그 등록을 `core:local`에 이 리포가 정한 배치와 골격 그대로 만든다.

## 배치

| 파일 | 위치 |
|---|---|
| `<대상>Entity.kt` | `core/local/.../entity/` |
| 조회 전용 POJO | `core/local/.../entity/` |
| `<대상>Dao.kt` | `core/local/.../dao/` |
| `BodyPlanDatabase.kt` | `core/local/.../` — 새로 만들지 않고 기존 것에 등록한다 |
| `LocalModule.kt` | `core/local/.../di/` — DAO provides를 더한다 |

`core:local`은 `core:domain`을 의존한다. 컬럼 값이 도메인 enum 이름을 쓰기 때문이다.

## Entity 규칙

- `@PrimaryKey(autoGenerate = true) val id: Long = 0L`. 기본값이 있어야 삽입 시 id를 비워 넘길 수 있다.
- `tableName`은 snake_case다. 클래스 이름과 다르다.
- **도메인 enum은 `String` 컬럼에 `.name`으로 담는다.** `TypeConverter`를 두지 않는다 — 변환은 `core:data`의 매핑이 맡는다.
- **날짜는 epochDay `Long`, 시각은 millis `Long`이다.** `LocalDate`·`Instant`를 그대로 담지 않는다.
- 조회 조건에 쓰는 컬럼에 `@Index`를 건다.
- 자식 테이블은 `ForeignKey(onDelete = CASCADE)`와 부모 키 인덱스를 함께 둔다. Room이 `PRAGMA foreign_keys`를 켜므로 부모를 지우면 자식이 함께 지워진다. 이 경로를 실제로 확인하려면 계측 테스트가 필요하다 — 인메모리 페이크로는 검증되지 않는다.
- 부모와 자식을 함께 읽으려면 `@Embedded` + `@Relation` POJO를 만든다. **`@Relation`은 순서를 보장하지 않는다.** 순서가 의미를 가지면 정렬 컬럼을 두고 매핑에서 정렬한다.
- 컬럼 일부만 읽는 조회는 그 컬럼만 담은 POJO를 따로 만든다. 쓰지 않는 자식 목록을 함께 읽지 않기 위한 것이다.

## DAO 규칙

- 관찰은 `Flow`, 나머지는 `suspend`다.
- `@Relation`을 담은 반환 타입에는 `@Transaction`을 붙인다.
- **여러 테이블을 건드리는 연산은 기본 구현 메서드로 묶고 `@Transaction`을 붙인다.** Repository가 DAO를 두 번 부르는 형태로 두지 않는다. 중간에 실패하면 반쪽만 남는다.
- 자식 행의 부모 키는 기본 구현 메서드가 채운다. 호출부가 더미 값을 넣어 넘기지 않는다.

## 등록

1. `BodyPlanDatabase`의 `entities` 배열에 Entity를 더하고 `version`을 올린다.
2. `LocalModule`에 DAO `@Provides`를 더한다. Database만 `@Singleton`이고 DAO에는 스코프를 두지 않는다.

배포 전까지 `fallbackToDestructiveMigration(dropAllTables = true)`을 쓰므로 마이그레이션을 쓰지 않는다. 스키마가 바뀌면 개발 기기의 데이터가 지워진다. 첫 배포 직전에 이 설정을 떼고 그 시점 스키마를 확정본으로 삼는다.

`core/local/schemas/`의 산출물은 커밋한다.

## 템플릿

### Entity와 자식 Entity

```kotlin
@Entity(tableName = "workout_entry", indices = [Index("dateEpochDay")])
data class WorkoutEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateEpochDay: Long,
    val bodyPart: String,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "workout_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId")],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val entryId: Long,
    val setNumber: Int,
)

/** [sets]의 순서는 보장되지 않는다. 도메인으로 옮길 때 `setNumber`로 정렬한다. */
data class WorkoutEntryWithSets(
    @Embedded val entry: WorkoutEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val sets: List<WorkoutSetEntity>,
)
```

### DAO

```kotlin
@Dao
interface WorkoutEntryDao {
    @Transaction
    @Query("SELECT * FROM workout_entry WHERE dateEpochDay = :epochDay ORDER BY createdAtMillis ASC, id ASC")
    fun observeByDate(epochDay: Long): Flow<List<WorkoutEntryWithSets>>

    @Insert
    suspend fun insert(entity: WorkoutEntryEntity): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Transaction
    suspend fun insertWithSets(entity: WorkoutEntryEntity, sets: List<WorkoutSetEntity>): Long {
        val entryId = insert(entity)
        insertSets(sets.map { it.copy(entryId = entryId) })
        return entryId
    }
}
```

### 초기 데이터

첫 생성 때 심을 행이 있으면 `RoomDatabase.Callback`으로 넣는다. **이 시점에는 DAO를 쓸 수 없어 원시 SQL로 넣는다.** 컬럼 목록을 직접 적으므로, Entity에 기본값 없는 컬럼이 늘면 첫 실행에서만 조용히 깨진다.

```kotlin
private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DefaultExercises.all.forEach { exercise ->
            db.execSQL(
                "INSERT INTO exercise (bodyPart, name, intensityType, isDeleted) VALUES (?, ?, ?, 0)",
                arrayOf(exercise.bodyPart, exercise.name, exercise.intensityType),
            )
        }
    }
}
```

## 체크리스트

1. `entity/`에 Entity를 만든다. 자식 테이블이면 FK와 인덱스를 함께 건다.
2. 부모와 자식을 함께 읽으면 `@Relation` POJO를, 컬럼 일부만 읽으면 전용 POJO를 만든다.
3. `dao/`에 DAO를 만든다. 여러 테이블을 건드리는 연산은 `@Transaction` 기본 구현으로 묶는다.
4. `BodyPlanDatabase`의 `entities`에 더하고 `version`을 올린다.
5. `LocalModule`에 DAO `@Provides`를 더한다.
6. 도메인으로 옮기는 매핑은 `core:data`가 맡는다. [repository.md](repository.md)를 읽는다.

## 검증

```
./gradlew :app:compileDebugKotlin
```

통과 후 `core/local/schemas/kr.sdbk.bodyplan.core.local.BodyPlanDatabase/<version>.json`이 새로 생겼는지 확인한다. 생기지 않았으면 `entities` 등록이나 `version` 증가가 빠진 것이다.
