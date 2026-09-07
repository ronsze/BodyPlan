# AI 분석

**작성일**: 2026-09-08

## 설계

### 요약

사용자가 등록한 API 키로 Claude를 불러 기록을 분석하는 기능을 넣는다. 온보딩에서 받은 신체 정보와 목적을 바탕으로 식단과 운동, 인바디를 분석하고 개선 방향을 낸다. 마이 탭이 생겨 키와 프로필을 관리한다.

### 배경

이 문서는 다섯 단위 전체의 설계를 담는다. 단위마다 계약이 독립적이므로 구현은 순서대로 하되, 앞 단위가 만든 것을 뒤 단위가 그대로 쓴다.

| 단위 | 만드는 것 |
|---|---|
| 1 | 네트워크 모듈, Claude 클라이언트, 토큰 저장, 마이 탭과 토큰 설정 화면 |
| 2 | 프로필 도메인·저장소, 첫 실행 온보딩, 마이 탭의 프로필 요약과 수정 화면 |
| 3 | 식단 분석 — 날짜별, 주간, 월간 |
| 4 | 운동 분석 — 같은 얼개 |
| 5 | 인바디 분석 — 사진 등록과 분석 |

이 프로젝트에는 지금 네트워크 계층이 없다. `coil-network-okhttp`가 의존 그래프에 있어 OkHttp는 이미 들어와 있지만 직접 쓰는 코드는 없다.

### 확정 전제

사용자가 답을 준 결정이다. 임의 판단이 아니다.

| 항목 | 결정 |
|---|---|
| 구축 순서 | 다섯 단위 순차. 단위마다 계획 대조·리뷰·테스트를 밟는다 |
| 칼로리와 목표치 | 둘 다 AI가 낸다. 사진과 메모로 섭취량을 추정하고, 프로필로 목표치를 계산해 함께 답한다. 기록 구조는 바꾸지 않는다 |
| 분석 결과 | 저장하고 다시 보여준다. 새로 분석하기를 눌렀을 때만 다시 호출한다 |
| 인바디 분석 위치 | 마이 탭 안 |
| 프로필 표시 | 마이 탭에 요약 카드로 보이고, 카드나 수정 버튼을 눌러 자세히 보기와 수정으로 들어간다 |
| AI 제공자 | 클로드·GPT·제미나이 중 하나를 고른다. 한 번에 하나만 쓴다 (요청으로 추가됨) |
| 연결 해제 | 해제하면 저장된 제공자와 키를 함께 지운다 (요청으로 추가됨) |
| 모델 | 클로드는 `claude-sonnet-5`. "medium"을 표준 모드로 읽어 확장 사고를 켜지 않는다 |

### 밝혀 둘 것

- 분석은 사진과 기록을 Anthropic 서버로 보낸다. 식단 사진, 인바디 사진, 신체 정보가 대상이다.
- API 키는 기기에 저장된다. 안드로이드에 사용자 키를 안전하게 둘 곳은 없다. 루팅된 기기나 백업에서 읽힐 수 있다.
- 칼로리는 사진에서 얻은 추정값이다. 정확한 값이 아니다.

### 수용 조건

**단위 1 — AI 기반과 마이 탭**

- [ ] 하단에 마이 탭이 생기고 눌러 들어갈 수 있다.
- [ ] 마이 탭에서 AI 토큰 설정으로 들어갈 수 있다.
- [ ] 클로드·GPT·제미나이 중 하나를 고를 수 있다.
- [ ] 고른 제공자와 키를 저장하면 앱을 껐다 켜도 남아 있다.
- [ ] 저장된 키는 앞뒤 몇 글자만 보이고 가운데가 가려진다.
- [ ] 연결을 해제하면 제공자와 키가 함께 저장소에서 사라진다.
- [ ] 연결 확인을 누르면 고른 제공자의 API를 실제로 불러 쓸 수 있는 키인지 알려준다.
- [ ] 잘못된 키로 연결 확인을 하면 실패 문구가 뜬다.
- [ ] 네트워크가 없을 때 연결 확인을 하면 실패 문구가 뜨고 앱이 죽지 않는다.
- [ ] 토큰이 비어 있으면 저장 버튼이 비활성이다.
- [ ] 마이 탭에서 어느 제공자에 연결되어 있는지 한눈에 알 수 있다.

**단위 2 — 온보딩과 프로필**

- [ ] 앱을 처음 켜면 온보딩이 뜬다.
- [ ] 온보딩을 마치거나 건너뛰면 다시 뜨지 않는다.
- [ ] 나이·키·체중·성별·목적을 입력할 수 있고 모두 비워 둘 수 있다.
- [ ] 목적은 여럿 고를 수 있다.
- [ ] 건너뛰면 아무 값도 저장되지 않고 온보딩만 완료로 남는다.
- [ ] 마이 탭에 프로필 요약이 카드로 보인다.
- [ ] 카드나 수정 버튼을 누르면 자세히 보기와 수정 화면으로 들어간다.
- [ ] 프로필을 고치면 마이 탭 요약에 즉시 반영된다.
- [ ] 값이 하나도 없으면 요약 카드가 비어 있음을 알리고 입력을 권한다.

**단위 3 — 식단 분석**

- [ ] 날짜별 기록 화면에서 그 날 식단을 분석할 수 있다.
- [ ] 캘린더에서 주간과 월간 분석을 할 수 있다.
- [ ] 분석 결과에 추정 섭취 칼로리와 목표치, 개선 방향이 담긴다.
- [ ] 기록이 없는 기간을 분석하면 분석하지 않고 안내한다.
- [ ] 저장된 결과가 있으면 다시 열 때 그대로 보이고 호출하지 않는다.
- [ ] 새로 분석하기를 누르면 다시 호출해 결과를 덮어쓴다.
- [ ] 호출이 실패하면 이전 결과가 남고 실패 문구가 뜬다.
- [ ] 토큰이 없으면 분석 대신 토큰 등록으로 가라는 팝업이 뜬다.
- [ ] 팝업의 확인을 누르면 토큰 설정 화면으로 이동한다.

**단위 4 — 운동 분석**

- [ ] 날짜별·주간·월간 분석이 식단과 같은 얼개로 동작한다.
- [ ] 분석 결과에 수행량 요약과 개선 방향이 담긴다.
- [ ] 기록이 없는 기간, 저장·재분석, 실패, 토큰 없음은 식단과 같게 동작한다.

**단위 5 — 인바디 분석**

- [ ] 마이 탭에서 인바디 분석으로 들어갈 수 있다.
- [ ] 사진을 고르고 분석 버튼을 눌러 결과를 받는다.
- [ ] 사진 없이 분석 버튼을 누를 수 없다.
- [ ] 결과에 요약과 식단·운동 개선 방향이 담긴다.
- [ ] 지난 분석이 목록으로 남고 다시 열어 볼 수 있다.
- [ ] 호출이 실패하면 사진과 입력이 남고 실패 문구가 뜬다.

### 비목표

- 서버를 두고 키를 대신 보관하는 것. 키는 기기에만 둔다.
- 키를 암호화해 보관하는 것. 안드로이드에 사용자 키를 안전하게 둘 곳이 없어 값을 더하지 못한다.
- 분석 결과를 사람이 고치는 것. 받은 그대로 보여준다.
- 칼로리를 기록에 항목으로 더하는 것. 추정은 분석 결과 안에만 남는다.
- 분석 이력을 기간별로 비교하거나 그래프로 그리는 것.
- 사용량과 비용을 앱에서 집계해 보여주는 것.
- 온보딩에서 받은 값으로 목표 칼로리를 앱이 직접 계산하는 것. 계산은 AI가 한다.
- 스트리밍 응답. 다 받은 뒤 한 번에 보여준다.

### 데이터 계약

#### 저장소 — 토큰과 온보딩 완료 여부

`core:local`에 DataStore를 둔다. Room에 넣지 않는 것은 행이 아니라 값 하나씩이기 때문이다.

| 키 | 타입 | 귀착지 |
|---|---|---|
| `ai_provider` | `String` | 고른 제공자 (`AiProvider.name`). 어느 API를 부를지 정한다 |
| `ai_token` | `String` | 인증 정보. 제공자마다 헤더에 싣는 자리가 다르다 |
| `onboarding_completed` | `Boolean` | 첫 실행 판정 |

제공자와 키는 한 벌이다. 따로 지우면 키만 남거나 제공자만 남아 어느 쪽도 쓸 수 없다. 연결 해제는 둘을 함께 지운다.

#### 테이블 `user_profile`

값이 하나뿐인 테이블이다. `id`를 0으로 고정해 항상 한 행만 둔다.

| 컬럼 | 타입 | 도메인 매핑 | 귀착지 |
|---|---|---|---|
| `id` | `Int` PK | 도메인 미노출 | **미사용**. 항상 0이다 |
| `ageYears` | `Int?` | `UserProfile.ageYears` | 요약 카드, 분석 요청 |
| `heightCm` | `Int?` | `UserProfile.heightCm` | 같음 |
| `weightKg` | `Int?` | `UserProfile.weightKg` | 같음 |
| `gender` | `String?` | `UserProfile.gender` (`Gender.name` ↔ `valueOf`) | 같음 |
| `goals` | `String` | `UserProfile.goals` (`Goal.name`을 `,`로 이어 붙임) | 같음 |
| `targetWeightKg` | `Int?` | `UserProfile.targetWeightKg` | 같음 |
| `targetNote` | `String?` | `UserProfile.targetNote` | 목표 근력처럼 숫자로 담기 어려운 것 |

모든 값이 nullable이다. 온보딩에서 아무것도 고르지 않아도 되기 때문이다.

#### 테이블 `analysis_result`

분석 결과를 저장해 다시 열 때 호출하지 않는다.

| 컬럼 | 타입 | 도메인 매핑 | 귀착지 |
|---|---|---|---|
| `id` | `Long` PK autoGenerate | `AnalysisResult.id` | 인바디 이력 목록의 키 |
| `kind` | `String` | `AnalysisResult.kind` (`AnalysisKind.name`) | 어떤 분석인지 구분 |
| `scopeKey` | `String` | `AnalysisResult.scopeKey` | 같은 대상의 결과를 찾는 키 |
| `summary` | `String` | `AnalysisResult.summary` | 결과 화면의 요약 |
| `detail` | `String` | `AnalysisResult.detail` | 결과 화면의 개선 방향 |
| `imageFileName` | `String?` | `AnalysisResult.imagePath`로 풀어서 낸다 | 인바디 사진. 다른 분석은 null |
| `createdAtMillis` | `Long` | `AnalysisResult.createdAtMillis` | 언제 분석했는지 표시, 목록 정렬 |

`kind`는 `DIET_DAILY`, `DIET_WEEKLY`, `DIET_MONTHLY`, `WORKOUT_DAILY`, `WORKOUT_WEEKLY`, `WORKOUT_MONTHLY`, `INBODY`다.

`scopeKey`는 대상을 문자열로 적은 것이다. 날짜별은 epochDay, 주간은 그 주 월요일의 epochDay, 월간은 `yyyy-MM`이다. 인바디는 매번 새 결과를 쌓으므로 빈 문자열을 쓴다.

`(kind, scopeKey)`에 인덱스를 건다. 같은 대상의 최신 결과를 찾는 것이 유일한 조회다.

`BodyPlanDatabase`의 `entities`에 두 Entity를 더하고 `version`을 3으로 올린다. 마이그레이션 `MIGRATION_2_3`이 두 테이블을 만든다.

#### 도메인 타입

```kotlin
enum class Gender { MALE, FEMALE }

enum class Goal { DIET, MUSCLE_GAIN, TARGET_WEIGHT, TARGET_STRENGTH }

data class UserProfile(
    val ageYears: Int? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val gender: Gender? = null,
    val goals: Set<Goal> = emptySet(),
    val targetWeightKg: Int? = null,
    val targetNote: String? = null,
) {
    val isEmpty: Boolean
}

enum class AnalysisKind { DIET_DAILY, DIET_WEEKLY, DIET_MONTHLY, WORKOUT_DAILY, WORKOUT_WEEKLY, WORKOUT_MONTHLY, INBODY }

data class AnalysisResult(
    val id: Long,
    val kind: AnalysisKind,
    val scopeKey: String,
    val summary: String,
    val detail: String,
    val imagePath: String? = null,
    val createdAtMillis: Long,
)
```

#### Repository·UseCase 시그니처

```kotlin
enum class AiProvider { CLAUDE, GPT, GEMINI }

/** 고른 제공자와 그 인증 정보. 둘은 한 벌로만 뜻이 있다. */
data class AiCredential(val provider: AiProvider, val token: String)

interface AiCredentialRepository {
    fun observeCredential(): Flow<AiCredential?>
    suspend fun getCredential(): AiCredential?
    suspend fun save(credential: AiCredential)
    /** 연결 해제. 제공자와 키를 함께 지운다. */
    suspend fun clear()
}

interface AiAnalysisRepository {
    /** 인증 정보가 실제로 쓸 수 있는지 확인한다. 실패하면 던진다. */
    suspend fun verifyCredential(credential: AiCredential)
}

interface UserProfileRepository {
    fun observeProfile(): Flow<UserProfile>
    suspend fun getProfile(): UserProfile
    suspend fun saveProfile(profile: UserProfile)
}

interface OnboardingRepository {
    suspend fun isCompleted(): Boolean
    suspend fun markCompleted()
}

interface AnalysisResultRepository {
    fun observeLatest(kind: AnalysisKind, scopeKey: String): Flow<AnalysisResult?>
    fun observeHistory(kind: AnalysisKind): Flow<List<AnalysisResult>>
    suspend fun save(result: AnalysisResult): Long
}
```

`AiAnalysisRepository`는 단위 1에서 `verifyToken`만 갖는다. 단위 3부터 분석 함수가 붙는다. 그 시점에 계약을 다시 적는다.

#### Claude 호출

`core:network`를 새로 만든다. 제공자마다 주소와 인증 방식, 요청 모양이 달라 클라이언트를 셋 두고 공통 인터페이스로 감싼다. 화면과 저장소는 어느 제공자인지 모른다.

| 제공자 | 주소 | 인증 | 모델 |
|---|---|---|---|
| 클로드 | `POST https://api.anthropic.com/v1/messages` | `x-api-key` 헤더와 `anthropic-version: 2023-06-01` | `claude-sonnet-5` |
| GPT | `POST https://api.openai.com/v1/chat/completions` | `Authorization: Bearer` | `gpt-4o` |
| 제미나이 | `POST https://generativelanguage.googleapis.com/v1beta/models/<model>:generateContent` | 쿼리 `key` | `gemini-2.0-flash` |

**모델 이름은 한 파일에 상수로 모은다.** 제공자가 모델을 갈아치우면 이름만 고치면 되게 한다. 클로드 외의 두 이름은 이 프로젝트가 확인한 값이 아니라 흔히 쓰이는 값을 적은 것이다.

연결 확인은 짧은 메시지를 보내고 응답 내용을 쓰지 않는다. 출력 길이를 최소로 두어 비용을 아낀다.

실패는 예외로 던진다. 401은 키가 틀린 것, 그 밖의 4xx·5xx와 네트워크 오류는 호출 실패다. 화면은 둘을 다른 문구로 알린다.

### 상태 계약

#### My (단위 1, 단위 2·5에서 늘어남)

| State 필드 | 타입 | 초기값 |
|---|---|---|
| `hasToken` | `Boolean` | `false` |
| `profile` | `UserProfile?` | `null`. 단위 2에서 채운다 |
| `isLoading` | `Boolean` | `false` |

`Intent`: `ClickAiToken` / `ClickProfile`(단위 2) / `ClickInbody`(단위 5)

`Effect`: `NavigateToAiToken` / `NavigateToProfile` / `NavigateToInbody`

#### AiToken (단위 1)

| State 필드 | 타입 | 초기값 |
|---|---|---|
| `selectedProvider` | `AiProvider` | `AiProvider.CLAUDE` |
| `input` | `String` | `""` |
| `saved` | `AiCredential?` | `null`. 저장된 한 벌 |
| `isLoading` | `Boolean` | `false` |
| `isVerifying` | `Boolean` | `false` |
| `errorMessage` | `String?` | `null` |

파생 값은 State의 계산 프로퍼티로 둔다.

| 프로퍼티 | 정의 |
|---|---|
| `savedTokenMask` | `saved`의 키를 앞뒤 네 글자만 남기고 가린 표기. 없으면 `null` |
| `canSave` | `input.isNotBlank() && !isVerifying` |
| `canVerify` | `saved != null && !isVerifying` |
| `canDisconnect` | `saved != null && !isVerifying` |

`Intent`: `SelectProvider(provider: AiProvider)` / `ChangeInput(value: String)` / `ClickSave` / `ClickDisconnect` / `ClickVerify` / `ClickBack`

`Effect`: `GoBack` / `ShowMessage(message: String)`

저장하면 `input`을 비우고 `saved`를 갱신한다. 연결 확인은 저장된 한 벌로 한다 — 입력 중인 값으로 하면 저장하지 않은 키가 통과했다고 오해한다. 해제하면 `saved`가 `null`이 되고 고른 제공자는 그대로 둔다.

단위 2·3·4·5의 상태 계약은 그 단위에 들어갈 때 이 문서에 덧붙인다. 지금 적으면 화면을 만들기 전의 추측이 된다.

### 화면 구성

#### 재사용 판정

시그니처는 대상 파일을 열어 확인했다.

| 심볼 | 파일 | 판정 | 시그니처 |
|---|---|---|---|
| `BodyPlanTopBar` | `core/designsystem/.../component/BodyPlanTopBar.kt` | 재사용 | `BodyPlanTopBar(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actionText: String? = null, onClickAction: () -> Unit = {})` |
| `BodyPlanCard` | `core/designsystem/.../component/BodyPlanCard.kt` | 재사용 | `BodyPlanCard(modifier: Modifier = Modifier, cornerRadius: Dp = 16.dp, contentPadding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit)` |
| `PrimaryButton` | `core/designsystem/.../component/BodyPlanButtons.kt` | 재사용 | `PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)` |
| `OutlinedActionButton` | `core/designsystem/.../component/BodyPlanButtons.kt` | 재사용 | `OutlinedActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)` |
| `BaseTextField` | `core/designsystem/.../component/BaseTextField.kt` | 재사용 | `BaseTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, placeholder: String? = null, textStyle: TextStyle = BaseTextDefaults.style, keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default, singleLine: Boolean = false, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE)` |
| `PillChip` | `core/designsystem/.../component/SelectableChip.kt` | 재사용 | `PillChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)`. 단위 2의 목적 고르기에 쓴다 |
| `BodyPlanIcon` | `core/designsystem/.../component/BodyPlanIcons.kt` | 재사용 | `BodyPlanIcon(painter: Painter, contentDescription: String?, boxSize: Dp, iconSize: Dp, tint: Color, modifier: Modifier = Modifier)` |

#### 신규 공용 컴포넌트

**`core:designsystem`**

- `ic_user.xml` — 마이 탭 아이콘. 시안에 없어 새로 그린다. 기존 아이콘과 같은 선 굵기와 24 단위 격자를 따른다.
- `SectionRow(title: String, description: String?, onClick: () -> Unit, modifier: Modifier)` — 마이 탭의 줄 하나. 제목과 부제, 오른쪽 화살표.

단위 3에서 `AiTokenRequiredDialog(onConfirm: () -> Unit, onDismiss: () -> Unit)`를 더한다. 단위 1에는 쓰는 곳이 없어 만들지 않는다.

#### 화면별 트리와 상태

**MyViewImpl** — `Column`. 상단 줄(제목 `마이`, 뒤로가기 없음), 아래 카드 목록. 단위 1에는 `AI 토큰` 줄 하나다. 단위 2에서 프로필 요약 카드가 맨 위에 붙고, 단위 5에서 `인바디 분석` 줄이 붙는다.

| 상태 | 표시 |
|---|---|
| 로딩 | 해당 없음. 값이 저장소에서 즉시 온다 |
| 빈 | 해당 없음. 줄은 항상 있다 |
| 에러 | 해당 없음. 읽기 실패가 없다 |

**AiTokenViewImpl** — `Column`. 상단 줄(뒤로, 제목 `AI 토큰`), 안내 문구 카드, 제공자 고르는 알약 칩 셋, 입력칸, 저장 버튼, 저장된 한 벌이 있으면 가린 표기와 연결 확인·연결 해제 버튼.

| 상태 | 표시 |
|---|---|
| 로딩 | 저장된 키를 읽는 동안 본문 자리에 진행 표시 |
| 빈 | 저장된 키가 없으면 가린 표기와 연결 확인·삭제를 그리지 않는다 |
| 에러 | 입력칸 아래에 실패 문구 |

#### 카피 원문

| 위치 | 문구 |
|---|---|
| 마이 탭 이름 | `마이` |
| 마이 화면 제목 | `마이` |
| 토큰 줄 제목 | `AI 토큰` |
| 토큰 줄 부제 (있음) | `<제공자 이름> 연결됨` |
| 토큰 줄 부제 (없음) | `연결되지 않음` |
| 제공자 이름 | `클로드` `GPT` `제미나이` |
| 토큰 화면 제목 | `AI 토큰` |
| 토큰 안내 | `분석 기능은 고른 제공자의 키로 AI를 부릅니다. 키는 이 기기에만 저장되고, 사진과 기록이 분석을 위해 외부로 전송됩니다.` |
| 입력 placeholder | `API 키를 붙여 넣으세요` |
| 저장 버튼 | `저장` |
| 연결 확인 버튼 | `연결 확인` |
| 연결 해제 버튼 | `연결 해제` |
| 저장 완료 | `저장했습니다` |
| 해제 완료 | `연결을 해제했습니다` |
| 연결 확인 성공 | `쓸 수 있는 키입니다` |
| 키가 틀림 | `키가 올바르지 않습니다` |
| 호출 실패 | `연결하지 못했습니다` |

### 네비게이션

`:feature:my:api`에 둔다.

```kotlin
@Serializable
data object MyNavKey : BodyPlanNavKey()

@Serializable
data object AiTokenNavKey : BodyPlanNavKey()

fun BodyPlanNavigator.navigateToMy()
fun BodyPlanNavigator.navigateToAiToken()
```

단위 2에서 `ProfileNavKey`와 `OnboardingNavKey`, 단위 5에서 `InbodyNavKey`가 늘어난다.

| 경로 | 진입 | 복귀 |
|---|---|---|
| 하단 탭 → 마이 | 탭 클릭 | 시스템 뒤로로 앱 종료 |
| 마이 → AI 토큰 | 토큰 줄 클릭 | 상단 뒤로 |

`MyNavKey`가 세 번째 탭의 뿌리다. 탭 목록은 `:app`의 `BodyPlanMainScreen`에 있고, 여기에 한 줄을 더한다.

온보딩은 탭이 아니라 앱 시작 시점의 분기다. 시작 목적지를 온보딩 완료 여부로 정한다. 단위 2에서 정한다.

### 버린 안

| 안 | 버린 이유 |
|---|---|
| 제공자를 클로드 하나로 고정 | 사용자가 셋 중에 고르기를 원했다 |
| 제공자별 키를 따로 저장해 두고 갈아 끼우기 | 한 번에 하나만 쓰므로 여러 키를 들고 있을 이유가 없다. 지금은 한 벌만 둔다 |
| 서버를 두고 키를 대신 보관 | 서버가 없고 이 앱에 도입할 이유가 없다. 사용자가 자기 키를 넣는 구조를 요청했다 |
| 키를 EncryptedSharedPreferences에 보관 | 복호화 키가 같은 기기에 있어 실질적인 보호가 아니다. 값을 더하지 못하면서 의존성만 는다 |
| 칼로리를 기록에 직접 입력 | 기록할 때마다 손이 간다. 사용자가 AI 추정을 골랐다 |
| 목표 칼로리를 앱이 계산 | 공식을 앱에 박으면 목적이 늘 때마다 고쳐야 한다. 프로필을 넘기고 AI가 계산한다 |
| 분석 결과를 매번 새로 호출 | 사용자의 키로 호출되어 열 때마다 비용이 든다. 사용자가 저장을 골랐다 |
| 인바디를 네 번째 탭으로 | 쓰는 빈도에 비해 자리를 많이 차지한다. 사용자가 마이 탭 안을 골랐다 |
| Retrofit·Ktor 도입 | 호출이 하나뿐이라 OkHttp와 kotlinx.serialization으로 충분하다 |
| 스트리밍 응답 | 화면이 복잡해지는 데 비해 얻는 것이 적다 |

## 실행

### 파일별 작업

#### 단위 1 — AI 기반과 마이 탭

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `gradle/libs.versions.toml` | 수정 | `okhttp`, `kotlinx-serialization-json`, `datastore-preferences` 별칭 추가 |
| `settings.gradle.kts` | 수정 | `:core:network`, `:feature:my:api`, `:feature:my:impl` 등록 |
| `core/network/build.gradle.kts` | 신규 | `bodyplan.android.library`, `bodyplan.android.hilt`, serialization |
| `core/network/.../AiClient.kt` | 신규 | 제공자를 감추는 공통 인터페이스 |
| `core/network/.../ClaudeClient.kt` | 신규 | 클로드 호출 |
| `core/network/.../GptClient.kt` | 신규 | GPT 호출 |
| `core/network/.../GeminiClient.kt` | 신규 | 제미나이 호출 |
| `core/network/.../AiModels.kt` | 신규 | 제공자별 모델 이름 상수 |
| `core/network/.../dto/*.kt` | 신규 | 요청·응답 DTO |
| `core/network/.../di/NetworkModule.kt` | 신규 | OkHttp·Json·Api 제공 |
| `core/local/build.gradle.kts` | 수정 | DataStore 의존 추가 |
| `core/local/.../datastore/AppPreferences.kt` | 신규 | 토큰과 온보딩 완료 여부 |
| `core/local/.../di/LocalModule.kt` | 수정 | DataStore 제공 |
| `core/domain/.../model/AiProvider.kt` | 신규 | 제공자 enum |
| `core/domain/.../model/AiCredential.kt` | 신규 | 제공자와 키 한 벌 |
| `core/domain/.../repository/AiCredentialRepository.kt` | 신규 | 인터페이스 |
| `core/domain/.../repository/AiAnalysisRepository.kt` | 신규 | `verifyToken`만 |
| `core/data/.../repository/AiCredentialRepositoryImpl.kt` | 신규 | DataStore를 감싼다 |
| `core/data/.../repository/AiAnalysisRepositoryImpl.kt` | 신규 | 제공자에 맞는 클라이언트를 고른다 |
| `core/data/.../di/DataModule.kt` | 수정 | 두 바인딩 추가 |
| `core/data/build.gradle.kts` | 수정 | `:core:network` 의존 추가 |
| `core/designsystem/src/main/res/drawable/ic_user.xml` | 신규 | 마이 탭 아이콘 |
| `core/designsystem/.../component/BodyPlanIcons.kt` | 수정 | `User` 추가 |
| `core/designsystem/.../component/SectionRow.kt` | 신규 | 마이 탭의 줄 |
| `feature/my/api/build.gradle.kts` | 신규 | 컨벤션 플러그인, namespace |
| `feature/my/api/.../MyNavKey.kt` | 신규 | NavKey 2종과 navigate 확장 2종 |
| `feature/my/impl/build.gradle.kts` | 신규 | 컨벤션 플러그인, namespace |
| `feature/my/impl/.../MyContracts.kt` | 신규 | State·Intent·Effect |
| `feature/my/impl/.../MyViewModel.kt` | 신규 | 토큰 등록 여부 구독 |
| `feature/my/impl/.../MyView.kt` | 신규 | 줄 목록 |
| `feature/my/impl/.../AiTokenContracts.kt` | 신규 | State·Intent·Effect |
| `feature/my/impl/.../AiTokenViewModel.kt` | 신규 | 저장·삭제·연결 확인 |
| `feature/my/impl/.../AiTokenView.kt` | 신규 | 입력과 버튼 |
| `feature/my/impl/.../MyNavGraph.kt` | 신규 | entry 두 개 |
| `app/.../navigation/BodyPlanMainScreen.kt` | 수정 | 탭에 마이 추가, navGraph 호출 추가 |

단위 2부터의 파일별 작업은 그 단위에 들어갈 때 적는다.

### 구현 순서

단위마다 검증을 통과한 뒤 커밋한다.

**단위 1 — AI 기반과 마이 탭**

- 쓰는 것: `BodyPlanTopBar`, `BodyPlanCard`, `PrimaryButton`, `OutlinedActionButton`, `BaseTextField`, `BodyPlanIcon`, `BodyPlanDatabase`가 아닌 DataStore
- 만드는 것: `AiClient`와 제공자별 클라이언트 셋, `AppPreferences`, `AiProvider`, `AiCredential`, `AiCredentialRepository`·`Impl`, `AiAnalysisRepository`·`Impl`, `SectionRow`, `ic_user`, `:feature:my:api`·`:impl`, `MyNavKey`·`AiTokenNavKey`, 두 화면 한 벌씩, 세 번째 탭
- 검증: `./gradlew :app:assembleDebug :feature:my:impl:testDebugUnitTest` + Preview 렌더
- 리뷰·테스트: 두 ViewModel과 `AiCredentialRepositoryImpl`이 대상이다. 실제 호출은 페이크로 막는다

**단위 2 이후**는 그 단위에 들어갈 때 이 절에 덧붙인다.

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| `BodyPlanMainScreen`의 탭 목록 | 운동·식단 탭. 탭이 셋이 되면서 폭 배분이 바뀐다 |
| `core/data/build.gradle.kts`에 `:core:network` 추가 | 전체 빌드 |
| `core/local`에 DataStore 추가 | Room 쪽 주입. 같은 모듈에 저장 방식이 둘이 된다 |
| `DataModule`에 바인딩 추가 | 기존 Repository 주입 |
| `BodyPlanDatabase` version 3 (단위 2) | 운동·식단 기록. `MIGRATION_2_3`이 기존 데이터를 지우지 않는지 확인한다 |
