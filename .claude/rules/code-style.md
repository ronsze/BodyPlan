---
paths:
  - "**/*.kt"
---

# 코드 스타일

목적: Kotlin 파일을 쓰기 전에 이 리포의 표기·배치 규칙을 근거로 삼게 한다.

## 표기

- 포맷·import·줄바꿈은 ktlint(spotless)가 강제한다 — `.kt` 편집 후 훅이 `./gradlew spotlessApply`를 실행한다. 규칙 값은 루트 `build.gradle.kts`의 `ktlintSettings`와 `.editorconfig`에 있고, 기준은 [Kotlin 공식 코딩 컨벤션](https://kotlinlang.org/docs/coding-conventions.html)과 [Android Kotlin 스타일 가이드](https://developer.android.com/kotlin/style-guide)다. 아래는 도구가 판정하지 못하는 항목이다.
- 이름은 축약하지 않는다 — `vm`이 아니라 `viewModel`. 널리 쓰이는 축약(`id`, `url`, `db`)은 예외다.
- 주석은 "어떻게"가 아니라 "왜"를 적는다. 코드가 어떻게 동작하는지가 아니라, 이 코드가 왜 존재하는지를 적는다. 코드를 읽으면 알 수 있는 것은 주석으로 반복하지 않는다.

## Compose

- `@Composable` 함수는 PascalCase, 반환값이 없으면 `Unit`을 적지 않는다.
- `Modifier`는 첫 optional 파라미터로 두고 기본값 `Modifier`를 준다. 받은 modifier는 최상위 요소에 그대로 넘긴다.
- `@Preview` 함수는 `private`으로 선언하고 이름은 `<대상>Preview`.
- 색·타이포는 하드코딩하지 않고 `MaterialTheme` 또는 `core:designsystem`의 값을 쓴다.

## MVI

- 화면 하나는 `<화면>Contracts.kt`(`<화면>State` data class / `<화면>Intent` sealed interface / `<화면>Effect` sealed interface) / `<화면>ViewModel.kt` / `<화면>View.kt` 세 파일이다. 화면 안에서만 쓰는 타입은 `internal`.
- ViewModel은 `core:ui:coordinator`의 `BaseViewModel<S, I, E>`을 상속하고 `handleIntent`만 공개한다. 상태 변경은 `updateState`, 일회성 동작은 `updateEffect`, 진입 초기 로드는 `initializeData()` 재정의로만 한다.
- `<화면>View`는 `uiState` 수집·`UiEvents` 조립·effect 처리만 하고, UI는 `<화면>ViewImpl`에만 둔다. `ViewImpl`은 `state`와 `uiEvents`만 받고 ViewModel을 보지 않는다.
- 화면 밖으로 나가는 이벤트(화면 이동)는 `<화면>Events`, 화면 안에서 나는 이벤트(클릭·입력)는 `<화면>UiEvents`로 나눈다.
- effect 수집은 `CollectEffect`를 쓴다. `LaunchedEffect`로 직접 수집하지 않는다.

## 배치

- 계층 의존은 `ui → domain ← data` 한 방향이다. `domain`은 Android·Compose·다른 모듈에 의존하지 않는다.
- 데이터 소스 타입(Entity·DTO)은 `data` 밖으로 내보내지 않는다 — 경계에서 domain 모델로 매핑한다.
- 새 화면은 `:feature:<이름>:impl`에 만들고, NavKey와 navigate 확장 함수는 `:feature:<이름>:api`에 둔다.
- 여러 화면이 함께 쓰는 컴포넌트는 도메인에 종속되면 `core:ui:components`, 종속되지 않으면 `core:designsystem`에 둔다. MVI 베이스(`BaseViewModel`)·`State`/`Intent`/`Effect` 인터페이스·`CollectEffect`는 `core:ui:coordinator`다.
