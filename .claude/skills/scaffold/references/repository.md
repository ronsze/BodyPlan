# Repository 생성

목적: Repository 한 벌을 이 리포가 정한 배치와 골격 그대로 만든다. 한 벌은 도메인 인터페이스, `core:data` 구현, 매핑, Hilt 바인딩 네 가지다.

## 배치

| 파일 | 위치 | 공개 범위 |
|---|---|---|
| `<대상>Repository.kt` | `core/domain/.../repository/` | public |
| `<대상>RepositoryImpl.kt` | `core/data/.../repository/` | `internal` |
| `<대상>Mapper.kt` | `core/data/.../mapper/` | `internal` 확장 함수 |
| `DataModule.kt`에 `@Binds` 한 줄 | `core/data/.../di/` | `internal` |

매핑 파일은 대상마다 새로 만들지 않고 기존 것을 쓴다 — 같은 테이블 묶음을 다루면 한 파일에 모은다.

## 규칙

- 함수 이름은 셋으로 나뉜다. 관찰은 `observe<X>(): Flow<T>`, 단발 조회는 `get<X>(): T?`, 변경은 `suspend fun`이다.
- **실패를 삼키지 않는다.** `suspend` 함수는 던지고 `Flow`는 에러를 흘린다. `runCatching`·`catch`는 ViewModel이 한다. Repository가 잡으면 화면이 실패를 알 수 없다.
- **Entity 타입을 `core:data` 밖으로 내보내지 않는다.** 인터페이스 시그니처에는 도메인 모델만 나온다. 구현·매핑·DI 모듈을 전부 `internal`로 두면 이 규칙이 컴파일로 강제된다.
- `Flow`를 공개 시그니처로 내면 `core/domain/build.gradle.kts`의 coroutines가 `api`여야 한다. `implementation`이면 소비 모듈에서 타입이 보이지 않는다.
- 여러 테이블에 걸치는 변경은 Repository에서 순서대로 부르지 않고 DAO의 `@Transaction` 메서드 하나로 내린다. [local.md](local.md) 참조.
- 도메인 모델을 Entity로 만드는 조립이 두 곳 이상에 생기면 매핑 파일로 올린다.

## 템플릿

### 인터페이스

```kotlin
package kr.sdbk.bodyplan.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.sdbk.bodyplan.core.domain.model.Exercise

/** <무엇을 다루는 저장소인지 한 줄>. 실패는 삼키지 않고 호출부로 던진다. */
interface ExerciseRepository {
    fun observeExercises(bodyPart: BodyPart): Flow<List<Exercise>>

    suspend fun getExercise(id: Long): Exercise?

    suspend fun addExercise(bodyPart: BodyPart, name: String, intensityType: IntensityType): Long

    suspend fun deleteExercise(id: Long)
}
```

### 구현

```kotlin
package kr.sdbk.bodyplan.core.data.repository

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.data.mapper.toDomain
import kr.sdbk.bodyplan.core.domain.repository.ExerciseRepository
import kr.sdbk.bodyplan.core.local.dao.ExerciseDao

internal class ExerciseRepositoryImpl
@Inject
constructor(private val exerciseDao: ExerciseDao) : ExerciseRepository {
    override fun observeExercises(bodyPart: BodyPart): Flow<List<Exercise>> =
        exerciseDao.observeByBodyPart(bodyPart.name).map { entities -> entities.map { it.toDomain() } }
}
```

### 매핑

```kotlin
package kr.sdbk.bodyplan.core.data.mapper

internal fun ExerciseEntity.toDomain(): Exercise =
    Exercise(
        id = id,
        bodyPart = BodyPart.valueOf(bodyPart),
        name = name,
    )
```

### 바인딩

`core/data/.../di/DataModule.kt`에 줄을 더한다. 모듈을 새로 만들지 않는다.

```kotlin
@Binds
@Singleton
abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository
```

## 체크리스트

1. `core:domain`에 인터페이스를 쓴다. 도메인 모델만 시그니처에 넣는다.
2. `core:data`에 `internal` 구현을 만든다. DAO가 없으면 [local.md](local.md)를 먼저 읽고 만든다.
3. 매핑 함수를 `core:data`의 매핑 파일에 더한다. Entity → 도메인 방향과, 저장에 필요하면 반대 방향도 만든다.
4. `DataModule`에 `@Binds`를 더한다.
5. `core/data/src/test/.../repository/`에 DAO 페이크를 만들어 테스트한다. Room 인스트루먼트 테스트를 만들지 않는다.

## 검증

```
./gradlew :app:compileDebugKotlin :core:data:testDebugUnitTest
```

Hilt 그래프는 소비처가 생기기 전까지 검증되지 않는다. Repository만 만든 시점에는 컴파일이 통과해도 바인딩 누락이 드러나지 않으니, 화면이 붙는 단위에서 다시 확인한다.
