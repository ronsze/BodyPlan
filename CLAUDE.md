# CLAUDE.md

## 출력 규칙

- 중간 과정(탐색·검토·시도)은 출력하지 않는다. 단, 시도가 실패해 접근을 변경한 경우 그 사실은 결론에 포함한다.
- 두괄식으로 답한다. 결론·요청받은 값을 첫 문장에 먼저 제시하고, 전제·단서·배경 설명은 그 뒤에 붙인다.
- 요청이 모호해 확인이 필요한 경우, 확인 질문을 결론 대신 첫 문장에 둔다.
- 세부 내용·근거·이상 없는 항목은 사용자가 추가로 요청할 때만 출력한다. 조사·검사·리뷰 결과도 예외가 아니다 — 사용자가 판단하거나 실행할 항목만 낸다.
- 판단·실행 항목이 5개를 넘으면 "지금 할 것 / 나중"으로 나눠 5개까지만 낸다.
- 보내기 전에 확인한다: 첫 줄만 읽고 무엇을 해야 할지 알 수 있는가.

## 대화 방식

- 요청이 지정한 입력(파일·시트·티켓·API 등)을 확보하지 못하면 그 시점에 보고하고 중단한다. 남은 작업이 그 입력과 무관해도 진행하지 않는다.
- 명령을 그대로 따르기 전에 각 요소가 타당한지, 실제로 필요한지 먼저 판단하고, 문제가 없을 때만 그대로 진행한다.
- 타당하지 않거나 더 나은 방법이 있다고 판단되면 실행 전에 근거와 대안을 제시하고 합의한다.
- 방향·방법·개선안을 사용자에게 물을 때는 접근 2~3개를 제시하고, 각각의 장단점과 추천 하나를 그 이유와 함께 붙인다. 선택지 없는 열린 질문만 던지지 않는다.
- 완료·수정됨·통과를 보고하기 전에, 그것을 확인하는 명령을 실제 실행해 출력으로 확인한다 — 확인 명령은 보고 대상 범위로 한정한다.

## 프로젝트

Android 앱. Kotlin + Jetpack Compose, 멀티모듈 Gradle(Kotlin DSL). 의존성 버전은 전부 `gradle/libs.versions.toml`에서 관리하고 빌드 스크립트에 직접 적지 않는다.

모듈 공통 빌드 설정(SDK·Java·Compose·Hilt)은 `build-logic`의 컨벤션 플러그인에 있다 — `bodyplan.android.application` / `.library` / `.compose` / `.hilt` / `.feature`, `bodyplan.jvm.library`. 모듈의 `build.gradle.kts`에는 적용할 컨벤션 플러그인, `namespace`, 그 모듈 고유 의존성만 적는다. 여러 모듈에 같은 설정을 반복하게 되면 컨벤션 플러그인으로 올린다.

- 빌드: `./gradlew :app:assembleDebug`
- 컴파일 확인: `./gradlew :app:compileDebugKotlin` (`:core:*`·`:feature:*`는 `:app` 의존 그래프에 있어 함께 컴파일된다)
- 테스트: Android 모듈은 `./gradlew :<모듈>:testDebugUnitTest`, JVM 모듈(`:core:domain`)은 `./gradlew :core:domain:test`
- 기본 브랜치: `master`

## 아키텍처

| 모듈 | 역할 |
|---|---|
| `:app` | Application·MainActivity·NavKey 정의·루트 `NavDisplay` |
| `:feature:<이름>` | 화면 한 벌 — Route/Screen/Contract/ViewModel |
| `:core:ui` | MVI 베이스(`MviViewModel`, `UiState`/`UiIntent`/`UiEffect`), 공용 UI 유틸 |
| `:core:designsystem` | Theme·Color·Typography, 공용 컴포넌트 |
| `:core:domain` | 순수 JVM — 도메인 모델, Repository 인터페이스, UseCase |
| `:core:data` | Repository 구현, DI 모듈 |
| `:core:local` | 로컬 저장소. **껍데기만 생성된 상태 — 스키마·DAO는 아직 없다** |

- 의존 방향은 `app → feature → core:ui → core:designsystem`, `feature·core:data → core:domain` 한 방향이다. `core:domain`은 Android에 의존하지 않는다.
- 화면 상태 관리는 MVI다. `core:ui`의 `MviViewModel<S, I, E>`을 상속하고 `onIntent`로만 입력을 받는다.
- 네비게이션은 Navigation 3. 목적지는 `:app`의 `navigation/NavKeys.kt`에 `@Serializable` NavKey로 선언하고 `BodyPlanNavDisplay`의 `entryProvider`에 등록한다.
- DI는 Hilt. KSP 생성 소스 때문에 `gradle.properties`의 `android.disallowKotlinSourceSets=false`가 필요하다.

## 하네스

`.claude/**`가 이 프로젝트의 하네스다. 각 구성요소의 호출 조건은 그 구성요소의 description이 정의하므로, 여기서 언제 무엇을 쓸지는 정하지 않는다.

| 위치 | 내용 |
|---|---|
| `.claude/skills/` | 특정 종류의 작업에서 반복되는 멀티스텝 절차 |
| `.claude/agents/` | 독립 컨텍스트가 필요한 위임 작업 |
| `.claude/rules/` | 파일 경로 조건으로 로드되는 규칙 |
| `.claude/hooks/`·`settings.json` | 모델 판단에 의존하지 않는 결정적 강제 |
| `.claude/FEEDBACK.md` | 하네스 결함 기록 |
| `CLAUDE.md` | 모든 세션에서 필요한, 변경 빈도 낮은 지침·사실 |

- 하네스의 추가·제거·수정·검토는 harness 스킬의 원칙과 배치 기준을 따른다.
- 이 파일은 200줄을 넘기지 않는다. 절차로 자라난 내용은 스킬로 옮긴다.
