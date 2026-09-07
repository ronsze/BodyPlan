# 분석 요청을 화면 밖에서 돌리기

**작성일**: 2026-09-08

## 설계

### 요약

분석을 화면의 ViewModel이 아니라 WorkManager가 돌린다. 화면을 나가도 요청이 살아 있고, 끝나면 알림이 뜨며, 돌아오면 결과가 그대로 있다. 도는 동안 어느 단계인지 보여준다.

### 배경

지금은 분석이 `viewModelScope`에서 돈다. 화면을 나가면 NavEntry가 사라지며 ViewModel이 정리되고, 그 코루틴이 취소된다. 사용자는 자기 키로 AI를 부른 뒤 화면을 나갔다가, 돌아와 보면 결과도 없고 "분석 중"도 아닌 상태를 만난다 — 호출 비용만 나갔다.

사용자가 실제로 겪고 알려 온 증상이다: "식단 분석 누른다음 나갔다 들어가면 다시 분석 누를 수 있게됨".

### 확정 전제

사용자가 답을 준 결정이다. 임의 판단이 아니다.

| 항목 | 결정 |
|---|---|
| 어디까지 살아남나 | WorkManager. 앱이 죽었다 살아나도 이어간다 |
| 완료를 알리는 방법 | 푸시 알림 |
| 진행 표시 | 단계 표시. 스트리밍으로 바꾸지 않는다 |

### 밝혀 둘 것

- 안드로이드 13부터 알림에 런타임 권한이 필요하다. 거절하면 분석은 그대로 돌고 알림만 뜨지 않는다.
- WorkManager는 시스템 스케줄러를 타므로, 즉시 시작이 보장되지 않는다. 기기가 절전 상태면 몇 초 늦을 수 있다.

### 수용 조건

- [ ] 분석을 누르고 화면을 나가도 요청이 계속 진행된다.
- [ ] 화면에 돌아오면 "분석 중"이 그대로 보이고 분석 버튼이 눌리지 않는다.
- [ ] 도는 동안 어느 단계인지 문구로 보인다.
- [ ] 끝나면 결과가 저장되고, 화면에 돌아왔을 때 그 결과가 보인다.
- [ ] 끝나면 알림이 뜨고, 누르면 그 분석 화면이 열린다.
- [ ] 알림 권한을 거절해도 분석은 끝까지 돌고 결과가 저장된다.
- [ ] 같은 대상을 두 번 누르면 두 번 호출되지 않는다.
- [ ] 서로 다른 대상은 동시에 돌 수 있다.
- [ ] 실패하면 알림에 실패가 뜨고, 화면에 돌아왔을 때 실패 문구가 보인다.
- [ ] 실패해도 이전 결과는 남는다.
- [ ] 앱을 껐다 켜도 돌던 요청이 이어진다.
- [ ] 토큰이 없으면 작업을 넣지 않고 지금처럼 팝업을 띄운다.

### 비목표

- 스트리밍 응답. 답이 써지는 것을 실시간으로 보여주지 않는다 — 백그라운드 실행과 부딪힌다.
- 분석을 사용자가 도중에 취소하는 기능.
- 실패한 분석의 자동 재시도. 실패는 사용자가 다시 누른다.
- 여러 분석을 줄줄이 예약하는 기능. 단위 C가 계층을 바꾼 뒤에 다시 본다.
- 진행률(%) 표시. 스트리밍이 없어 진짜 진행률을 알 수 없다.

### 데이터 계약

#### 작업 하나를 가리키는 것

| 값 | 내용 |
|---|---|
| 고유 작업 이름 | `analysis:<kind>:<scopeKey>`. 같은 대상을 두 번 넣지 못하게 하는 열쇠다 |
| 입력 `kind` | `AnalysisKind.name` |
| 입력 `scopeKey` | 저장 열쇠. 인바디는 빈 문자열 |
| 입력 `periodLabel` | 사람이 읽는 기간 표기 |
| 입력 `fromEpochDay`·`toEpochDay` | 분석 범위. 인바디는 쓰지 않는다 |
| 입력 `sourceUri` | 인바디만. 고른 사진 |
| 진행 `stage` | 아래 단계 이름 |
| 실패 출력 `reason` | 화면이 그대로 보여줄 문구 |

`Data`에 담을 수 있는 것이 원시 타입뿐이라 `LocalDate`는 epochDay로, `AnalysisKind`는 이름으로 넣는다.

#### 단계

```kotlin
enum class AnalysisStage { COLLECTING, PREPARING_IMAGES, CALLING, PARSING }
```

| 단계 | 문구 | 언제 |
|---|---|---|
| `COLLECTING` | `기록을 모으는 중` | 기록·프로필·인증 정보를 읽는 동안 |
| `PREPARING_IMAGES` | `사진을 준비하는 중` | 사진을 읽어 base64로 바꾸는 동안. 사진이 없으면 건너뛴다 |
| `CALLING` | `AI에게 보내는 중` | 응답을 기다리는 동안. 가장 오래 걸린다 |
| `PARSING` | `정리하는 중` | 답을 가르고 저장하는 동안 |

**단계는 UseCase가 낸다.** 세 UseCase가 `onStage: (AnalysisStage) -> Unit = {}`를 받고 각 대목에서 부른다. 워커가 그것을 `setProgress`로 옮긴다. 워커가 짐작해 단계를 매기면 실제로 하는 일과 어긋난다.

#### 도메인 계약

```kotlin
/** 지금 도는 분석 하나. 화면은 이것만 보고 그린다. */
data class AnalysisRun(
    val kind: AnalysisKind,
    val scopeKey: String,
    val state: AnalysisRunState,
    val stage: AnalysisStage?,
    val failureReason: String?,
)

enum class AnalysisRunState { RUNNING, SUCCEEDED, FAILED }

interface AnalysisRunner {
    /** 이미 같은 대상이 돌고 있으면 새로 넣지 않는다. */
    suspend fun start(request: AnalysisRunRequest)

    /** 그 대상의 지금 상태. 돌고 있지 않으면 null. */
    fun observe(kind: AnalysisKind, scopeKey: String): Flow<AnalysisRun?>
}
```

`AnalysisRunner`는 `core:domain`에 두고, WorkManager를 쓰는 구현은 `core:data`에 둔다 — 화면과 도메인이 WorkManager를 모른다.

### 상태 계약

세 분석 화면(`DietAnalysis`·`WorkoutAnalysis`·`Inbody`)이 같은 방식으로 바뀐다.

| 지금 | 바뀐 뒤 |
|---|---|
| `isAnalyzing: Boolean` — ViewModel이 들고 있음 | `run: AnalysisRun?` — `AnalysisRunner.observe`가 내려줌 |
| `errorMessage: String?` — 호출 실패 시 채움 | 그대로. 다만 실패는 `run.failureReason`에서 온다 |

`isAnalyzing`은 `run?.state == RUNNING`으로 바뀐다. **화면이 상태를 들고 있지 않으므로 나갔다 들어와도 그대로다** — 이것이 이 단위의 핵심이다.

`ClickAnalyze`는 UseCase를 직접 부르지 않고 `AnalysisRunner.start`를 부른다. 토큰이 없을 때 팝업을 띄우는 판정은 지금처럼 화면이 먼저 한다.

`AnalysisScreenState`에 `stageMessage: String?`이 붙어 버튼 아래에 단계 문구가 나온다.

### 화면 구성

#### 재사용 판정

| 심볼 | 파일 | 판정 | 시그니처 |
|---|---|---|---|
| `AnalysisScreen` | `core/ui/components/.../AnalysisScreen.kt` | 확장 | `AnalysisScreenState`에 `stageMessage: String?` 추가 |
| `AnalysisScreenState` | 같은 파일 | 확장 | `isAnalyzing`은 그대로 두고 `stageMessage`만 더한다 |

#### AnalysisScreen (수정)

분석 버튼 위에 단계 문구 한 줄을 둔다. 도는 동안만 보이고, 버튼 글자는 지금처럼 `분석 중`이다.

| 상태 | 표시 |
|---|---|
| 로딩 | 그대로 |
| 빈 | 그대로 |
| 에러 | 그대로 |
| 권한 | 알림 권한을 거절해도 화면은 그대로 돈다. 화면에 권한 안내를 두지 않는다 — 분석 자체는 권한과 무관하다 |

#### 알림

| 항목 | 값 |
|---|---|
| 채널 이름 | `분석 결과` |
| 채널 중요도 | 기본. 소리 없이 뜬다 |
| 성공 제목 | `분석이 끝났어요` |
| 성공 본문 | `<기간 표기> <식단/운동/인바디> 분석` |
| 실패 제목 | `분석하지 못했어요` |
| 실패 본문 | 실패 사유. 없으면 `다시 시도해 주세요` |
| 누르면 | 그 분석 화면. 앱이 꺼져 있었으면 켜고 그 화면까지 연다 |

권한 요청은 분석 화면에서 처음 `분석하기`를 누를 때 한 번 묻는다. 거절해도 작업은 넣는다.

### 네비게이션

알림을 눌러 화면을 여는 길이 새로 생긴다. `MainActivity`가 인텐트의 분석 대상을 읽어 시작 백스택에 그 화면을 얹는다. NavKey는 이미 있는 것을 그대로 쓴다 — `DietAnalysisNavKey`·`WorkoutAnalysisNavKey`·`InbodyNavKey`.

인텐트에는 NavKey를 직렬화해 넣지 않고 `kind`와 `dateEpochDay`만 넣는다. NavKey의 모양이 바뀌어도 알림이 깨지지 않는다.

### 버린 안

| 안 | 버린 이유 |
|---|---|
| 앱 수명 스코프 싱글턴으로 돌리기 | 작지만 앱이 죽으면 요청도 죽는다. 사용자가 WorkManager를 골랐다 |
| 포그라운드 서비스 | 분석 한 번에 상주 알림까지 띄우는 것은 과하다 |
| 스트리밍 응답 | 백그라운드 실행과 부딪힌다. 화면을 나갔다 오면 받다 만 글을 이어 붙여야 한다 |
| 진행률(%) | 스트리밍이 없어 진짜 진행률을 알 수 없다. 가짜 진행률은 없느니만 못하다 |
| 워커가 단계를 짐작해 매기기 | 실제로 하는 일과 어긋난다. UseCase가 낸다 |
| 실패 시 자동 재시도 | 사용자 키로 나가는 비용이라 스스로 고르게 둔다 |

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `gradle/libs.versions.toml` | 수정 | `androidx.work`, `androidx.hilt.work`, `androidx.hilt.compiler` |
| `app/build.gradle.kts` | 수정 | 위 의존성 |
| `app/src/main/AndroidManifest.xml` | 수정 | `POST_NOTIFICATIONS` 권한, 기본 WorkManager 초기화 제거 |
| `app/.../BodyPlanApplication.kt` | 수정 | `Configuration.Provider`로 Hilt 워커 팩터리 연결 |
| `core/domain/.../model/AnalysisStage.kt` | 신규 | 단계 |
| `core/domain/.../model/AnalysisRun.kt` | 신규 | `AnalysisRun`·`AnalysisRunState`·`AnalysisRunRequest` |
| `core/domain/.../repository/AnalysisRunner.kt` | 신규 | 인터페이스 |
| `core/domain/.../usecase/Analyze*UseCase.kt` | 수정 | `onStage` 콜백을 받아 대목마다 부른다 |
| `core/data/.../work/AnalysisWorker.kt` | 신규 | UseCase를 부르고 단계를 `setProgress`로 낸다 |
| `core/data/.../work/AnalysisRunnerImpl.kt` | 신규 | 고유 이름으로 넣고 `WorkInfo`를 `AnalysisRun`으로 옮긴다 |
| `core/data/.../work/AnalysisNotifier.kt` | 신규 | 채널과 알림 |
| `core/data/.../di/DataModule.kt` | 수정 | 바인딩 |
| `core/data/build.gradle.kts` | 수정 | work·hilt-work 의존 |
| `core/ui/components/.../AnalysisScreen.kt` | 수정 | `stageMessage` |
| `feature/dietlog/impl/.../analysis/**` | 수정 | `AnalysisRunner`를 보도록 |
| `feature/workoutlog/impl/.../analysis/**` | 수정 | 같음 |
| `feature/my/impl/.../inbody/**` | 수정 | 같음 |
| `app/.../MainActivity.kt` | 수정 | 알림 인텐트를 읽어 시작 화면 정하기 |
| `app/.../navigation/BodyPlanMainScreen.kt` | 수정 | 시작 백스택에 그 화면 얹기 |

### 구현 순서

**단위 B1 — 백그라운드 실행**

- 쓰는 것: 세 `Analyze*UseCase`, `AnalysisResultRepository`
- 만드는 것: `AnalysisStage`·`AnalysisRun`·`AnalysisRunner`, `AnalysisWorker`, `AnalysisRunnerImpl`, 워커 팩터리 연결
- 검증: `./gradlew :app:assembleDebug :core:domain:test :core:data:testDebugUnitTest`
- 리뷰·테스트: `AnalysisRunnerImpl`의 중복 방지와 `WorkInfo` 옮기기가 대상이다

**단위 B2 — 화면과 단계 표시**

- 쓰는 것: B1의 `AnalysisRunner`, `AnalysisScreen`
- 만드는 것: 세 화면의 `run` 상태, `stageMessage`
- 검증: 위에 더해 세 feature 모듈 테스트 + Preview 렌더

**단위 B3 — 알림**

- 쓰는 것: B1의 워커
- 만드는 것: `AnalysisNotifier`, 권한 요청, 알림 인텐트로 화면 열기
- 검증: `./gradlew :app:assembleDebug`. 알림은 기기에서 눈으로 확인한다

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| 세 `Analyze*UseCase`에 `onStage` 추가 | 기본값이 있어 기존 호출부는 그대로다. 도메인 테스트가 그대로 도는지 확인 |
| `AnalysisScreenState`에 `stageMessage` 추가 | 식단·운동·인바디 분석 화면 셋과 Preview |
| 세 ViewModel에서 `isAnalyzing` 제거 | 각 화면의 ViewModel 테스트가 통째로 바뀐다 |
| `BodyPlanApplication`에 `Configuration.Provider` | 앱 시작. 기본 초기화 제거를 빠뜨리면 워커가 Hilt 주입을 못 받는다 |
| `MainActivity`의 시작 화면 판정 | 온보딩 분기. 알림으로 들어온 경우와 겹치지 않는지 확인 |

## 자기 대조

자기 대조: 통과 (대조 15/15)
- 미해결 [데이터 계약]: WorkManager·Hilt Work의 버전을 아직 카탈로그에 넣지 않아 실제 값이 미확인이다 — 착수 시점에 정한다. 계약 자체는 버전과 무관하다.
