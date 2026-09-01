# 모듈 생성

목적: `feature`·`core` 모듈을 이 리포의 구조 그대로 만든다.

## feature 모듈

feature 하나는 **`api`와 `impl` 두 모듈**이다. 단일 모듈로 만들지 않는다.

| 모듈 | 담는 것 | 의존 |
|---|---|---|
| `feature/<이름>/api` | NavKey 선언, `BodyPlanNavigator` 확장 navigate 함수, NavKey 인자로 쓰는 타입 | `:core:navigation` (컨벤션 플러그인이 붙임) |
| `feature/<이름>/impl` | `BodyPlanEntryProviderScope` 확장 navGraph 함수, Contracts·ViewModel·View | 자기 `api`를 `api()`로 노출, `:core:ui:components`, `:core:domain` (컨벤션 플러그인이 붙임) |

**다른 feature는 상대의 `api`만 의존한다.** `impl`을 의존하면 안 된다 — 화면 간 이동은 `api`의 navigate 확장 함수로 한다.

`:app`은 아무것도 적지 않는다. `bodyplan.android.application` 컨벤션 플러그인이 `:feature:*:impl`을 전부 자동으로 의존한다.

### 체크리스트

1. 디렉토리 생성 — `feature/<이름>/api/src/main/java/kr/sdbk/bodyplan/feature/<이름>/api/`, `feature/<이름>/impl/src/main/java/kr/sdbk/bodyplan/feature/<이름>/impl/`
2. `feature/<이름>/api/build.gradle.kts`

   ```kotlin
   plugins {
       alias(libs.plugins.bodyplan.android.feature.api)
   }

   android {
       namespace = "kr.sdbk.bodyplan.feature.<이름>.api"
   }
   ```

3. `feature/<이름>/impl/build.gradle.kts`

   ```kotlin
   plugins {
       alias(libs.plugins.bodyplan.android.feature.impl)
   }

   android {
       namespace = "kr.sdbk.bodyplan.feature.<이름>.impl"
   }
   ```

   `impl`이 자기 `api`를 의존하는 것도 컨벤션 플러그인이 경로 규칙(`:feature:<이름>:impl` → `:feature:<이름>:api`)으로 붙인다. 여기 적지 않는다.

4. `settings.gradle.kts`에 두 줄 추가

   ```kotlin
   include(":feature:<이름>:api")
   include(":feature:<이름>:impl")
   ```

5. `api`에 NavKey와 navigate 확장 함수를 쓴다. 인자가 없으면 `data object`, 있으면 `data class`.

   ```kotlin
   package kr.sdbk.bodyplan.feature.<이름>.api

   @Serializable
   data object <이름>NavKey : BodyPlanNavKey()

   fun BodyPlanNavigator.navigateTo<이름>() = navigate(<이름>NavKey)
   ```

   인자가 있으면 navigate 함수가 인자를 받아 NavKey를 만든다.

   ```kotlin
   @Serializable
   data class DetailNavKey(val planId: Long) : BodyPlanNavKey()

   fun BodyPlanNavigator.navigateToDetail(planId: Long) = navigate(DetailNavKey(planId))
   ```

6. `impl`에 navGraph 확장 함수를 쓴다. 파일명은 `<이름>NavGraph.kt`.

   ```kotlin
   package kr.sdbk.bodyplan.feature.<이름>.impl

   fun BodyPlanEntryProviderScope.<이름>NavGraph(navigator: BodyPlanNavigator) {
       entry<<이름>NavKey> {
           val events = remember { <이름>Events(goBack = navigator::goBack) }
           <이름>View(events = events, viewModel = hiltViewModel())
       }
   }
   ```

   NavKey 인자를 ViewModel에 넘겨야 할 때만 assisted factory로 만들어 넘긴다.

   ```kotlin
   entry<DetailNavKey> { navKey ->
       val events = remember { DetailEvents(goBack = navigator::goBack) }
       DetailView(
           events = events,
           viewModel = hiltViewModel<DetailViewModel, DetailViewModel.Factory> { it.create(navKey) },
       )
   }
   ```

   `hiltViewModel`은 `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`을 쓴다 — `androidx.hilt.navigation.compose` 쪽이 아니다.

7. `:app`의 `BodyPlanNavDisplay`의 `entryProvider`에 navGraph 호출 한 줄 추가

   ```kotlin
   entryProvider {
       homeNavGraph(navigator)
       <이름>NavGraph(navigator)
   }
   ```

8. 화면 파일은 `impl`에 만든다 — [screen.md](screen.md)를 읽고 그 골격을 따른다.

## core 모듈

`core/<이름>` 단일 모듈이 기본이다. feature처럼 api/impl로 나누지 않는다. 단 `core:ui`는 예외로 하위 모듈을 갖는다 — `core/ui/coordinator`(MVI 베이스·Contract 인터페이스, Compose 없음)와 `core/ui/components`(도메인에 종속된 공용 뷰). 도메인에 종속되지 않는 범용 컴포넌트는 `core:designsystem`이다.

1. `core/<이름>/src/main/java/kr/sdbk/bodyplan/core/<이름>/` 생성
2. `core/<이름>/build.gradle.kts` — Android가 필요 없으면 `bodyplan.jvm.library`, 필요하면 `bodyplan.android.library`(+ Compose·Hilt는 각 컨벤션 플러그인 추가)
3. `settings.gradle.kts`에 `include(":core:<이름>")`
4. 이 모듈을 쓰는 모듈의 `build.gradle.kts`에 `implementation(project(":core:<이름>"))` — core 모듈은 자동 의존이 없다

## 컨벤션 플러그인을 고칠 때

여러 모듈이 같은 의존성을 반복하면 `build-logic/convention/`의 컨벤션 플러그인으로 올린다. 플러그인을 새로 만들면 `build-logic/convention/build.gradle.kts`의 `gradlePlugin`에 등록하고 `gradle/libs.versions.toml`의 `[plugins]`에 `version = "unspecified"`로 별칭을 더한다.

## 검증

```
./gradlew :app:assembleDebug
```

`settings.gradle.kts` 등록 누락은 `build.gradle.kts`가 무시돼 조용히 통과하므로, 새 모듈의 클래스를 실제로 참조하는 코드가 컴파일되는지까지 확인한다.
