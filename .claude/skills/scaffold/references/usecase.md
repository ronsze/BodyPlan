# UseCase 생성

목적: UseCase를 `core:domain`에 이 리포가 정한 배치와 골격 그대로 만든다.

## 배치

`core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/usecase/<이름>UseCase.kt`

이름은 `<동사><대상>UseCase`다 — `IsEditableDateUseCase`, `GetMonthlyDayStatusUseCase`.

DI 등록은 없다. 생성자 주입만으로 Hilt가 해결하므로 모듈에 적지 않는다.

## 규칙

- `operator fun invoke`로 공개한다. 공개 함수는 하나다.
- 생성자에 `@Inject`를 붙이고 Repository와 `Clock` 같은 주변 장치를 받는다.
- `core:domain`은 Android에 의존하지 않는다. `java.time`·`javax.inject`·coroutines만 쓴다.
- 현재 시각은 주입받은 `Clock`으로 읽는다. `LocalDate.now()`·`System.currentTimeMillis()`를 직접 부르지 않는다 — 테스트가 시각을 고정할 수 없다.
- **구독 중에 변하면 안 되는 값은 `invoke` 진입 시점에 한 번 읽고, 그 값을 모든 판정에 넘긴다.** `Flow`를 내는 UseCase에서 갈린다. 방출 때마다 시계를 다시 읽는 경로가 하나라도 남으면 구독 도중 자정을 넘길 때 판정끼리 어긋난다.
- 같은 판정을 다른 UseCase도 써야 하면 기준값을 받는 오버로드를 더한다. 규칙을 복사하지 않는다.

## 템플릿

### 값을 내는 UseCase

```kotlin
package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** <무엇을 판정·계산하는지 한 줄>. */
class IsEditableDateUseCase
@Inject
constructor(private val clock: Clock) {
    operator fun invoke(date: LocalDate): Boolean = invoke(date, LocalDate.now(clock))

    /** 기준일을 직접 받는 판정. 한 번 읽은 오늘을 여러 날짜에 적용하는 호출부가 쓴다. */
    operator fun invoke(date: LocalDate, today: LocalDate): Boolean =
        date == today || date == today.minusDays(1)
}
```

### Flow를 내는 UseCase

```kotlin
package kr.sdbk.bodyplan.core.domain.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.sdbk.bodyplan.core.domain.model.DayStatus
import kr.sdbk.bodyplan.core.domain.repository.WorkoutLogRepository

/** <무엇을 내는지 한 줄>. */
class GetMonthlyDayStatusUseCase
@Inject
constructor(
    private val workoutLogRepository: WorkoutLogRepository,
    private val isEditableDate: IsEditableDateUseCase,
    private val clock: Clock,
) {
    operator fun invoke(yearMonth: YearMonth): Flow<Map<LocalDate, DayStatus>> {
        // 구독 중 자정을 넘겨도 흔들리지 않도록 여기서 한 번만 읽고, 모든 판정이 이 값을 쓴다.
        val today = LocalDate.now(clock)

        return workoutLogRepository.observeBodyPartsInRange(from, to).map { recorded ->
            // 판정에 today를 넘긴다. 안쪽에서 시계를 다시 읽지 않는다.
        }
    }
}
```

## 체크리스트

1. `core/domain/.../usecase/`에 파일을 만든다.
2. 생성자 의존을 정한다. 시각이 필요하면 `Clock`을 받는다 — `DataModule`이 이미 제공한다.
3. `invoke`를 쓴다. `Flow`를 낸다면 위의 "한 번만 읽는다" 규칙을 적용한다.
4. `core/domain/src/test/.../usecase/`에 테스트를 만든다. 시각은 `Clock.fixed`로 고정한다.

## 검증

```
./gradlew :core:domain:test
```

`Clock`을 주입받지 않고 시각을 직접 읽었다면 이 테스트를 고정 시각으로 쓸 수 없다. 그 지점에서 드러난다.
