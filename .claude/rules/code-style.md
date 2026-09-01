---
paths:
  - "**/*.kt"
---

# 코드 스타일

목적: Kotlin 파일을 쓰기 전에 이 리포의 표기·배치 규칙을 근거로 삼게 한다.

## 표기

- 포맷·import·줄바꿈은 ktlint(spotless)가 강제한다 — `.kt` 편집 후 훅이 `./gradlew spotlessApply`를 실행한다. 규칙 값은 루트 `build.gradle.kts`의 `ktlintSettings`와 `.editorconfig`에 있고, 기준은 [Kotlin 공식 코딩 컨벤션](https://kotlinlang.org/docs/coding-conventions.html)과 [Android Kotlin 스타일 가이드](https://developer.android.com/kotlin/style-guide)다. 아래는 도구가 판정하지 못하는 항목이다.
- 이름은 축약하지 않는다 — `vm`이 아니라 `viewModel`. 널리 쓰이는 축약(`id`, `url`, `db`)은 예외다.
- 주석은 "왜"만 적는다. 코드가 그대로 말하는 "무엇"은 적지 않는다.

## Compose

- `@Composable` 함수는 PascalCase, 반환값이 없으면 `Unit`을 적지 않는다.
- `Modifier`는 첫 optional 파라미터로 두고 기본값 `Modifier`를 준다. 받은 modifier는 최상위 요소에 그대로 넘긴다.
- `@Preview` 함수는 `private`으로 선언하고 이름은 `<대상>Preview`.
- 색·타이포는 하드코딩하지 않고 `MaterialTheme` 또는 `core:designsystem`의 값을 쓴다.

## MVI

- 화면 하나에 `<화면>UiState`(data class) / `<화면>Intent`(sealed interface) / `<화면>Effect`(sealed interface) 한 벌.
- ViewModel은 `core:ui`의 `MviViewModel<S, I, E>`을 상속하고 `onIntent`만 공개한다. 상태 변경은 `setState`, 일회성 동작은 `sendEffect`로만 한다.
- 화면 컴포저블은 `state`와 `onIntent: (Intent) -> Unit`을 받는 stateless 함수로 쓰고, ViewModel 주입은 그 위의 route 컴포저블이 한다.

## 배치

- 계층 의존은 `ui → domain ← data` 한 방향이다. `domain`은 Android·Compose·다른 모듈에 의존하지 않는다.
- 데이터 소스 타입(Entity·DTO)은 `data` 밖으로 내보내지 않는다 — 경계에서 domain 모델로 매핑한다.
- 새 화면은 `:feature:<이름>` 모듈에 만든다. 여러 화면이 함께 쓰는 컴포넌트는 `core:designsystem`, MVI·공용 UI 유틸은 `core:ui`에 둔다.
