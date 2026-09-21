# 온디바이스 AI 제공자

**작성일**: 2026-09-22

## 설계

### 요약

분석에 쓸 AI 제공자로 온디바이스(Gemini Nano, ML Kit GenAI Prompt API)를 더한다. 키 없이 기기 안에서 추론하며, 연동 화면에서 모델을 내려받아 연결한다. 온디바이스를 지원하지 않는 기기에서는 선택지 자체가 보이지 않는다.

### 배경

기존 세 제공자(클로드·GPT·제미나이)는 `AiClient`(core:network) 하나로 추상화돼 있고 `core:data`의 `AiAnalysisRepositoryImpl`이 `AiProvider`로 고른다. 온디바이스는 토큰이 없고, 모델 다운로드·기기 지원 여부라는 다른 생명주기를 갖는다. 추론은 `AnalysisWorker`(WorkManager `CoroutineWorker`) 안에서 돈다.

ML Kit GenAI Prompt API 1.0.0-beta4 사실(AAR에서 확인):
- `Generation.getClient(): GenerativeModel`
- `GenerativeModel.checkStatus(): Int` — `FeatureStatus.UNAVAILABLE / DOWNLOADABLE / DOWNLOADING / AVAILABLE`
- `GenerativeModel.download(): Flow<DownloadStatus>` — `DownloadStarted(bytesToDownload)`, `DownloadProgress(totalBytesDownloaded)`, `DownloadCompleted`, `DownloadFailed(e: GenAiException)`
- `GenerativeModel.isSystemPromptAvailable(): Boolean`
- `generateContentRequest(SystemInstruction, Content) { maxOutputTokens = ... }` / `generateContentRequest(Content) { ... }`, `content { image(bitmap); text(str) }` — 이미지를 여러 장 `Part`로 넣을 수 있다
- `GenerateContentResponse.candidates[0].text`
- 실패는 `GenAiException(errorCode)`. 코드: `NOT_AVAILABLE=8`, `BUSY=9`, `REQUEST_TOO_LARGE=12`, `NOT_SUPPORTED=16`, `BACKGROUND_USE_BLOCKED=30`, `NOT_ENOUGH_DISK_SPACE=501`, `NEEDS_SYSTEM_UPDATE=604`, `INVALID_INPUT_IMAGE=-102`
- 입력은 4000토큰 미만, 출력은 4K토큰을 넘기지 말라고 문서가 권한다. minSdk 26 이상(이 앱은 28).

### 확정 전제

사용자가 답을 준 결정이다.

| 항목 | 결정 |
|---|---|
| 엔진 | Gemini Nano (ML Kit GenAI Prompt API). Gemma+MediaPipe는 버렸다 — `버린 안` 참조 |
| 노출 | 온디바이스 선택지는 지원되는 기기·조건에서만 보인다. `checkStatus()`가 `UNAVAILABLE`이면 제공자 목록에 나오지 않는다 |
| 작업 환경 | 새 브랜치 + worktree |

### 밝혀 둘 것

- 온디바이스 추론은 `AnalysisWorker` 안에서 돈다. AICore가 백그라운드 사용을 막으면(`BACKGROUND_USE_BLOCKED`) 분석이 실패한다. 이번에는 실패 사유로 알리기만 하고 워커를 포그라운드로 올리지 않는다.
- 사진 여러 장을 한 요청에 넣는 것이 모든 지원 기기에서 되는지는 문서에 없다. 거절되면 `NOT_SUPPORTED`/`INVALID_INPUT_IMAGE` 사유로 화면에 보인다.
- 기존 프롬프트는 한국어 JSON 출력을 요구하고 출력 상한이 8192다. 온디바이스는 4096으로 잡는다. 답이 끊기면 `AnalysisContentParser`가 이미 통째로 요약에 넣는다(변경 없음).
- 검증은 지원 기기(Pixel 9 이후 등)가 있어야 한다. 에뮬레이터에서는 선택지가 보이지 않는 것까지만 확인된다.

### 수용 조건

- [ ] 온디바이스를 지원하지 않는 기기에서는 AI 연동 화면의 제공자 목록에 온디바이스가 보이지 않고, 기존 셋만 보인다.
- [ ] 지원 기기에서 목록에 `온디바이스로 연동하기`가 보인다.
- [ ] 모델이 이미 준비된 기기에서 온디바이스를 누르면 바로 연결되고, 연결됨 화면에 토큰 대신 `기기 안에서 처리됩니다`가 보인다.
- [ ] 모델을 내려받아야 하는 기기에서 온디바이스를 누르면 내려받기 화면으로 가고, `내려받기`를 누르면 진행률이 보이다가 끝나면 연결된다.
- [ ] 내려받기 도중 `취소`를 누르면 제공자 목록으로 돌아가고 연결되지 않는다.
- [ ] 내려받기가 실패하면 실패 사유가 화면에 남고 내려받기 화면에 머문다.
- [ ] 온디바이스로 연결한 뒤 앱을 껐다 켜도 연결이 남아 있고, 마이 탭에 `온디바이스 연결됨`이 보인다.
- [ ] 연결 해제하면 저장소에서 제공자가 지워지고 목록으로 돌아간다.
- [ ] 온디바이스로 연결된 상태에서 운동·식단·인바디 분석이 기존과 같은 경로로 돌고 결과가 저장된다.
- [ ] 온디바이스 추론이 실패하면(`GenAiException`) 분석 화면의 실패 사유에 코드에 맞는 한국어 문구가 보인다.
- [ ] 저장된 온디바이스 연결이 있어도 모델이 준비되지 않았으면(`checkStatus() != AVAILABLE`) 분석이 `온디바이스 모델이 준비되지 않았어요`로 실패한다.
- [ ] 기존 세 제공자의 연동·분석 동작은 바뀌지 않는다.

### 비목표

- `AnalysisWorker`를 포그라운드 서비스로 올리는 것.
- 온디바이스용으로 프롬프트를 줄이거나 따로 두는 것. 프롬프트는 공유한다.
- 온디바이스 결과 스트리밍.
- 모델 삭제·갱신 UI. AICore가 관리한다.
- 구조화 출력(Structured Output API). JSON 지시로 충분하다.
- 온디바이스와 외부 제공자를 동시에 연결하는 것. 한 번에 하나 규칙은 유지한다.
- `AiClient`의 이미지 계약(`AiImage` base64)을 Bitmap으로 바꾸는 것. 온디바이스 클라이언트가 base64를 다시 디코드한다.

### 데이터 계약

#### 저장소 — `AppPreferences`(변경 없음)

| 키 | 온디바이스일 때 값 | 귀착지 |
|---|---|---|
| `ai_provider` | `"ON_DEVICE"` | `AiCredential.provider` |
| `ai_token` | `""` | `AiCredential.token` — 온디바이스는 빈 문자열이 정상이다 |

`AiCredentialRepositoryImpl.toCredential`은 지금 토큰이 비면 `null`을 준다. 제공자가 `ON_DEVICE`일 때만 빈 토큰을 허용한다.

#### 도메인 모델 (core:domain, 신규·수정)

```kotlin
enum class AiProvider { CLAUDE, GPT, GEMINI, ON_DEVICE }

/** 키를 넣어야 하는 제공자인지. 온디바이스는 키가 없다. */
val AiProvider.requiresToken: Boolean  // this != ON_DEVICE

enum class OnDeviceModelStatus { UNSUPPORTED, DOWNLOADABLE, DOWNLOADING, READY }

sealed interface OnDeviceDownload {
    data class Started(val totalBytes: Long) : OnDeviceDownload
    data class Progress(val downloadedBytes: Long) : OnDeviceDownload
    data object Completed : OnDeviceDownload
    data class Failed(val reason: String) : OnDeviceDownload
}

interface OnDeviceAiRepository {
    suspend fun getStatus(): OnDeviceModelStatus
    /** 내려받기 진행을 흘린다. Completed 또는 Failed로 끝난다. 수집을 취소하면 더 이상 관찰하지 않는다 */
    fun download(): Flow<OnDeviceDownload>
}
```

`AiCredential`은 그대로 둔다. 온디바이스는 `AiCredential(AiProvider.ON_DEVICE, "")`이며 이를 만드는 `AiCredential.onDevice()` 팩토리를 companion에 둔다.

#### SDK 래퍼 (core:ondevice, 신규 모듈)

core:network처럼 domain에 의존하지 않는다. core:network의 `AiClient`·`AiImage`를 구현·사용하므로 core:network에 의존한다.

| 심볼 | 내용 | 귀착지 |
|---|---|---|
| `OnDeviceApi` (Qualifier) | `@OnDeviceApi AiClient` 이름표 | `AiAnalysisRepositoryImpl` 생성자 |
| `OnDeviceFeatureStatus` enum `UNAVAILABLE, DOWNLOADABLE, DOWNLOADING, AVAILABLE` | `FeatureStatus` Int 상수를 감싼 것 | `OnDeviceAiRepositoryImpl`이 `OnDeviceModelStatus`로 매핑 |
| `OnDeviceDownloadEvent` sealed `Started(bytesToDownload)`, `Progress(bytesDownloaded)`, `Completed`, `Failed(exception: OnDeviceAiException)` | `DownloadStatus` 매핑 | 위와 같음 |
| `OnDeviceAiException(val code: Int, val reason: String) : Exception(reason)` | `GenAiException.errorCode` → 한국어 사유 | `AiAnalysisRepositoryImpl.toDomainFailure` → `AiRequestFailedException(this, reason)` |
| `GeminiNano` (public, `@Singleton`) | `suspend fun getStatus(): OnDeviceFeatureStatus`, `fun download(): Flow<OnDeviceDownloadEvent>`, 내부적으로 `Generation.getClient()` 하나를 lazy로 든다 | `OnDeviceAiRepositoryImpl`, `GeminiNanoClient` |
| `GeminiNanoClient : AiClient` (internal) | `verify(token)`: `getStatus() == AVAILABLE`이 아니면 `OnDeviceAiException(NOT_AVAILABLE)`. `complete(...)`: 이미지 base64→`Bitmap` 디코드, `content { images…; text(userPrompt) }`, `isSystemPromptAvailable()`이면 `SystemInstruction(systemPrompt)`, 아니면 `systemPrompt + "\n\n" + userPrompt`를 text로. `maxOutputTokens = 4096`. `GenAiException`은 `OnDeviceAiException`으로 바꿔 던진다. 답은 `candidates.firstOrNull()?.text.orEmpty()` | `AiAnalysisRepositoryImpl.call` |
| `OnDeviceModule` (Hilt, internal) | `@Provides @OnDeviceApi fun provideOnDeviceClient(client: GeminiNanoClient): AiClient` | Hilt 그래프 |

`GenAiException.errorCode` → 사유 문구:

| 코드 | 사유 |
|---|---|
| `NOT_AVAILABLE`(8) | `온디바이스 모델이 준비되지 않았어요` |
| `BUSY`(9) | `온디바이스 모델이 다른 작업 중이에요` |
| `REQUEST_TOO_LARGE`(12) | `기록이 길어 온디바이스 모델 한도를 넘었어요` |
| `NOT_SUPPORTED`(16) | `이 기기의 온디바이스 모델이 지원하지 않는 요청이에요` |
| `BACKGROUND_USE_BLOCKED`(30) | `온디바이스 분석은 앱이 화면에 있을 때만 돼요` |
| `NOT_ENOUGH_DISK_SPACE`(501) | `저장 공간이 부족해요` |
| `NEEDS_SYSTEM_UPDATE`(604) | `시스템 업데이트가 필요해요` |
| `INVALID_INPUT_IMAGE`(-102) | `온디바이스 모델이 읽을 수 없는 사진이에요` |
| 그 밖 | `온디바이스 오류 $code` |

#### core:data 매핑

| 소스 | 대상 |
|---|---|
| `OnDeviceFeatureStatus.UNAVAILABLE / DOWNLOADABLE / DOWNLOADING / AVAILABLE` | `OnDeviceModelStatus.UNSUPPORTED / DOWNLOADABLE / DOWNLOADING / READY` |
| `OnDeviceDownloadEvent.Started(bytesToDownload)` | `OnDeviceDownload.Started(totalBytes)` |
| `OnDeviceDownloadEvent.Progress(bytesDownloaded)` | `OnDeviceDownload.Progress(downloadedBytes)` |
| `OnDeviceDownloadEvent.Completed` | `OnDeviceDownload.Completed` |
| `OnDeviceDownloadEvent.Failed(exception)` | `OnDeviceDownload.Failed(exception.reason)` |
| `OnDeviceAiException` (verify·complete에서) | `AiRequestFailedException(cause = this, reason = reason)` |

### 상태 계약

#### AI 연동 화면 (`AiTokenState` 수정)

| 필드 | 타입 · 초기값 | 출처 |
|---|---|---|
| `onDeviceStatus` | `OnDeviceModelStatus? = null` | `initializeData`에서 `OnDeviceAiRepository.getStatus()` 한 번. `null`은 아직 확인 전 |
| `downloadTotalBytes` | `Long = 0` | `OnDeviceDownload.Started` |
| `downloadedBytes` | `Long = 0` | `OnDeviceDownload.Progress` |
| 기존 `connected`, `connectingProvider`, `input`, `isLoading`, `isConnecting`, `errorMessage` | 그대로 | |

파생값:
- `providers: List<AiProvider>` — `onDeviceStatus`가 `null`이거나 `UNSUPPORTED`면 `ON_DEVICE`를 뺀 `AiProvider.entries`. 목록의 노출 근거다.
- `connectedTokenMask` — `connected.provider.requiresToken`이 아니면 `null`.
- `canConnect` — `connectingProvider.requiresToken`이면 기존 규칙(`input.isNotBlank() && !isConnecting`), 아니면 `!isConnecting`.
- `downloadRatio: Float?` — `downloadTotalBytes > 0`이면 `downloadedBytes / downloadTotalBytes`를 0..1로, 아니면 `null`.
- `isBusyWithoutScreen: Boolean` — `(isLoading || isConnecting) && connected == null && connectingProvider == null`. 첫 로드와 온디바이스 바로 연결의 검증 동안 목록 대신 로딩을 보여 다른 제공자를 못 누르게 한다 (리뷰에서 추가).

Intent:
- `ClickProvider(provider)` — 기존. `isConnecting`이면 무시한다. `ON_DEVICE`이고 `onDeviceStatus == READY`면 `connectingProvider`를 거치지 않고 바로 `verifyCredential(onDevice()) → save`; 성공하면 `onDeviceStatus = READY`로 둔다(내려받기로 연결한 뒤 해제→재연결 시 내려받기를 건너뛰기 위해). `DOWNLOADABLE`·`DOWNLOADING`이면 `connectingProvider = ON_DEVICE`(내려받기 화면).
- `ClickDownloadModel` — 신규. `OnDeviceAiRepository.download()` 수집을 `connectJob`에 담아 시작. `isConnecting = true`. `Started`·`Progress`는 바이트 필드 갱신, `Completed`는 `verifyCredential(onDevice()) → save` → 기존 연동 성공 처리(`connected` 채움, `ShowMessage(CONNECTED)`), `Failed(reason)`은 `isConnecting = false, errorMessage = reason`.
- `ClickConnect`, `ClickCancelConnect`, `ClickDisconnect`, `ChangeInput`, `ClickBack` — 기존. `ClickCancelConnect`는 `connectJob`을 취소해 내려받기 수집도 함께 멈추고 바이트 필드를 0으로 되돌린다. 취소로 끝난 job의 `CancellationException`은 실패로 보지 않는다. 새 연결·내려받기를 시작할 때 이전 `connectJob`을 먼저 취소한다.

Effect: 기존 `GoBack`, `ShowMessage` 그대로.

실패 경로:
- `getStatus()`가 던지면 `onDeviceStatus = UNSUPPORTED`로 둔다(선택지를 숨긴다). 나머지 화면은 그대로 뜬다.
- 바로 연결(READY)에서 `verifyCredential`이 던지면 `errorMessage = failure.toMessage()`(기존)이고 `connectingProvider = ON_DEVICE`로 옮겨 내려받기 화면에서 사유를 보인다.

#### 마이 탭 (`MyState`)

변경 없음. `connectedProvider = ON_DEVICE`가 `label`로 `온디바이스 연결됨`이 된다.

#### 분석 화면 (dietlog·workoutlog·inbody)

변경 없음. 실패 사유는 `AnalysisRun.failureReason`으로 이미 흐른다.

### 화면 구성

#### AI 연동 화면 — 제공자 고르기 (`ProviderPicker`, 수정)

- `AiProvider.entries` 대신 `state.providers`를 돈다. → `ProviderPicker(providers: List<AiProvider>, onClickProvider: (AiProvider) -> Unit)`
- 각 줄: 재사용 `SectionRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, description: String? = null, leading: (@Composable () -> Unit)? = null)` — 기존 `ProviderButton` 그대로. 온디바이스 줄에는 `description = "키 없이 기기 안에서 처리"`.
- 하단 안내 문구 수정: `키는 이 기기에만 저장되고, 사진과 기록이 분석을 위해 외부로 전송됩니다. 온디바이스는 기기 안에서 처리됩니다.`
- 상태: 로딩(`isLoading && connected == null`) — 기존 `LoadingContent`. 빈 — 해당 없음(기존 셋은 항상 보인다). 에러 — 해당 없음. 권한 — 해당 없음.

#### AI 연동 화면 — 내려받기 (`OnDeviceDownloadContent`, 신규, `connectingProvider == ON_DEVICE`일 때 `ConnectingContent` 대신)

- 재사용 `BodyPlanCard { ... }` 안에 `AiProviderMark(provider: AiProvider, modifier: Modifier = Modifier, size: Dp = 28.dp)` + 제목 `온디바이스 모델 내려받기`(`titleSmall`, `TextPrimary`), 본문 `Gemini Nano 모델을 이 기기에 내려받습니다. 기록과 사진이 외부로 나가지 않습니다.`(`bodySmall`, `TextTertiary`).
- 진행: `isConnecting`이면 `LinearProgressIndicator`(material3, `progress = { downloadRatio }` — `null`이면 indeterminate 오버로드) + `내려받는 중 N%`(`downloadRatio`가 있을 때만 %).
- `state.errorMessage` — 기존과 같이 `Danger` 색 `bodyMedium`.
- 재사용 `PrimaryButton(text, onClick, enabled)`: `isConnecting`이면 `내려받는 중` 아니면 `내려받기`, `enabled = state.canConnect`, `onClick = onClickDownloadModel`.
- 재사용 `OutlinedActionButton(text = "취소", onClick = onClickCancelConnect)`.
- 상태: 로딩 — `isConnecting` 진행 표시. 빈 — 해당 없음. 에러 — `errorMessage`. 권한 — 해당 없음.

#### AI 연동 화면 — 연결됨 (`ConnectedContent`, 수정)

- 토큰 마스크 줄: `connectedTokenMask ?: "기기 안에서 처리됩니다"`.
- 그 밖은 그대로.

#### `AiTokenUiEvents` — `onClickDownloadModel: () -> Unit` 추가. Preview 셋에 내려받기 화면 하나 추가.

#### `AiProviderUi.kt` (core:ui:components, 수정)

- `label`: `ON_DEVICE -> "온디바이스"`
- `brandColor`: `ON_DEVICE -> AiOnDevice`(designsystem `Color.kt` 신규, `Color(0xFF7E57C2)`)
- `initial`: `ON_DEVICE -> "N"`

#### 마이 탭 (`MyView`)

변경 없음. Preview도 그대로.

### 네비게이션

해당 없음. 새 화면·NavKey 없음. 기존 AI 연동 화면 안의 분기다.

### 버린 안

- **Gemma 3n + MediaPipe LLM Inference**: 대부분 기기에서 되지만 3GB 모델을 앱이 직접 내려받아야 하고 그 파일을 둘 곳(HF는 로그인·약관 동의)이 없다. 채팅에서 사용자가 A안(Gemini Nano)을 골랐다.
- **`AiCredential.token`을 nullable로**: 호출부 전체에 `!!`·분기가 퍼진다. 빈 문자열 + `requiresToken`으로 충분하다.
- **core:ondevice가 core:domain에 의존**: core:network가 domain에 의존하지 않는 층 구조와 어긋난다. SDK 래퍼는 자기 타입을 내고 core:data가 매핑한다.
- **온디바이스 클라이언트를 core:network에 넣기**: 네트워크가 아닌 것이 네트워크 모듈에 들어간다. ML Kit 의존성이 core:network 전체에 번진다.
- **`AnalysisWorker`를 포그라운드로**: `BACKGROUND_USE_BLOCKED`를 피하는 방법이지만 알림·권한이 따라온다. 실제로 막히는지 확인된 뒤 별도 작업으로.

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `gradle/libs.versions.toml` | 수정 | `mlkitGenaiPrompt = "1.0.0-beta4"`, 라이브러리 `mlkit-genai-prompt` |
| `settings.gradle.kts` | 수정 | `include(":core:ondevice")` |
| `core/ondevice/build.gradle.kts` | 신규 | `bodyplan.android.library` + `bodyplan.android.hilt`, `namespace = "kr.sdbk.bodyplan.core.ondevice"`, `implementation(project(":core:network"))`, `implementation(libs.mlkit.genai.prompt)`, `implementation(libs.kotlinx.coroutines.core)` |
| `core/ondevice/src/main/java/kr/sdbk/bodyplan/core/ondevice/OnDeviceApi.kt` | 신규 | Qualifier |
| `core/ondevice/.../OnDeviceFeatureStatus.kt` | 신규 | enum |
| `core/ondevice/.../OnDeviceDownloadEvent.kt` | 신규 | sealed |
| `core/ondevice/.../OnDeviceAiException.kt` | 신규 | code→reason 표 포함 |
| `core/ondevice/.../GeminiNano.kt` | 신규 | `getStatus()`, `download()`, `GenerativeModel` 보관 |
| `core/ondevice/.../GeminiNanoClient.kt` | 신규 | `AiClient` 구현 |
| `core/ondevice/.../di/OnDeviceModule.kt` | 신규 | `@OnDeviceApi AiClient` 제공 |
| `core/domain/.../model/AiProvider.kt` | 수정 | `ON_DEVICE`, `requiresToken` |
| `core/domain/.../model/AiCredential.kt` | 수정 | `companion fun onDevice()` |
| `core/domain/.../model/OnDeviceModelStatus.kt` | 신규 | enum |
| `core/domain/.../model/OnDeviceDownload.kt` | 신규 | sealed |
| `core/domain/.../repository/OnDeviceAiRepository.kt` | 신규 | 인터페이스 |
| `core/data/build.gradle.kts` | 수정 | `implementation(project(":core:ondevice"))` |
| `core/data/.../repository/OnDeviceAiRepositoryImpl.kt` | 신규 | `GeminiNano` → 도메인 매핑 |
| `core/data/.../repository/AiCredentialRepositoryImpl.kt` | 수정 | `ON_DEVICE`면 빈 토큰 허용 |
| `core/data/.../repository/AiAnalysisRepositoryImpl.kt` | 수정 | `@OnDeviceApi` 주입, `clientFor`, `toDomainFailure`에 `OnDeviceAiException` |
| `core/data/.../di/DataModule.kt` | 수정 | `bindOnDeviceAiRepository` |
| `core/data/src/test/.../AiAnalysisRepositoryImplTest.kt` | 수정 | 생성자 인자 추가, `OnDeviceAiException` 매핑 테스트 |
| `core/designsystem/.../theme/Color.kt` | 수정 | `AiOnDevice` |
| `core/ui/components/.../AiProviderUi.kt` | 수정 | `label`·`brandColor`·`initial` 분기 |
| `feature/my/impl/.../aitoken/AiTokenContracts.kt` | 수정 | State 필드·파생값, `ClickDownloadModel` |
| `feature/my/impl/.../aitoken/AiTokenViewModel.kt` | 수정 | `OnDeviceAiRepository` 주입, status 조회, 온디바이스 연결·내려받기 |
| `feature/my/impl/.../aitoken/composable/AiTokenView.kt` | 수정 | `providers` 사용, `OnDeviceDownloadContent`, 연결됨 문구, UiEvents·Preview |
| `feature/my/impl/src/test/.../fake/FakeOnDeviceAiRepository.kt` | 신규 | status·download 제어 가능한 fake |
| `feature/my/impl/src/test/.../aitoken/AiTokenViewModelTest.kt` | 수정 | 생성자 인자 추가, 온디바이스 시나리오 |

### 구현 순서

1. **도메인 계약** — 만드는 것: `AiProvider.ON_DEVICE`·`requiresToken`, `AiCredential.onDevice()`, `OnDeviceModelStatus`, `OnDeviceDownload`, `OnDeviceAiRepository`. 쓰는 것: 없음. 검증: `./gradlew :core:domain:compileKotlin` — 이 시점에 `AiAnalysisRepositoryImpl.clientFor`·`AiProviderUi`의 `when`이 깨지므로 2·3과 함께 컴파일한다.
2. **SDK 래퍼 모듈** — 만드는 것: `core:ondevice` 전부. 쓰는 것: core:network `AiClient`·`AiImage`. 검증: `./gradlew :core:ondevice:compileDebugKotlin`.
3. **core:data·UI 공용** — 만드는 것: `OnDeviceAiRepositoryImpl`, `AiAnalysisRepositoryImpl`·`AiCredentialRepositoryImpl` 수정, `DataModule` 바인딩, `AiOnDevice`, `AiProviderUi` 분기. 쓰는 것: 1·2의 심볼. 검증: `./gradlew :app:compileDebugKotlin :core:data:testDebugUnitTest`. **커밋 1** (1~3).
4. **연동 화면** — 만드는 것: `AiTokenContracts`·`AiTokenViewModel`·`AiTokenView` 수정, fake·테스트. 쓰는 것: 1의 도메인 심볼, `AiProviderUi.label`. 검증: `./gradlew :app:compileDebugKotlin :feature:my:impl:testDebugUnitTest`. **커밋 2**.
5. 리뷰(code-reviewer: 로직·아키텍처·재사용) + 테스트(test-engineer: `AiAnalysisRepositoryImpl`, `AiCredentialRepositoryImpl`, `OnDeviceAiRepositoryImpl`, `AiTokenViewModel`). 수정분 재리뷰 후 **커밋 3**.

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| `AiProvider` enum 값 추가 | AI 연동 화면 제공자 목록(기존 셋 순서 유지), 마이 탭 `AI 연동` 줄 |
| `AiCredentialRepositoryImpl.toCredential` | 기존 제공자에서 빈 토큰이 여전히 `null`(연결 안 됨)인지 |
| `AiAnalysisRepositoryImpl` 생성자·`toDomainFailure` | 식단·운동·인바디 분석의 401/403/400/IOException 매핑 테스트 12개 |
| `AiTokenState.canConnect`·`connectedTokenMask` | 기존 키 입력 흐름(빈 입력 비활성, 마스크 표시) 테스트 |
| `AiProviderUi.brandColor`·`initial` | `AiProviderMark` Preview |

---

자기 대조: 통과 (대조 15/15)
- 고침: 상태 계약, 화면 구성
