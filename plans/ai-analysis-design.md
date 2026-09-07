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
| 날짜별 분석의 내용 | 무엇을 먹었는지, 칼로리, 영양 성분을 정리한다 (요청으로 추가됨) |
| 주간·월간 분석의 재료 | 식단은 사진이 아니라 이미 저장된 날짜별 분석을 종합한다 (요청으로 변경됨) |
| 운동 주간·월간의 재료 | 기록 원문을 그대로 보낸다. 사용자가 고른 안이다 |
| 인바디 사진의 보관 | 앱 내부 저장소에 복사해 둔다. 이력에서 어떤 사진을 분석한 것인지 다시 보여야 한다 |
| 인바디 이력의 자리 | 분석 화면 하나 안에 목록으로 둔다. 화면을 더 만들지 않는다 |
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
- [ ] 날짜별 결과에 먹은 음식·칼로리·영양 성분·개선 방향 네 묶음이 담긴다.
- [ ] 캘린더에서 주간과 월간 분석을 할 수 있다.
- [ ] 주간·월간은 사진을 다시 보내지 않고 저장된 날짜별 분석을 종합한다.
- [ ] 주간·월간 결과에 기간 흐름·칼로리 흐름·영양 균형·개선 방향 네 묶음이 담긴다.
- [ ] 기간 안에 날짜별 분석이 하나도 없으면 호출하지 않고 날짜별 분석을 먼저 하라고 안내한다.
- [ ] 기록이 없는 날을 분석하면 호출하지 않고 안내한다.
- [ ] 저장된 결과가 있으면 다시 열 때 그대로 보이고 호출하지 않는다.
- [ ] 새로 분석하기를 누르면 다시 호출해 결과를 덮어쓴다.
- [ ] 호출이 실패하면 이전 결과가 남고 실패 문구가 뜬다.
- [ ] 토큰이 없으면 분석 대신 토큰 등록으로 가라는 팝업이 뜬다.
- [ ] 팝업의 확인을 누르면 토큰 설정 화면으로 이동한다.

**단위 4 — 운동 분석**

- [ ] 날짜별 기록 화면에서 그 날 운동을 분석할 수 있다.
- [ ] 캘린더에서 주간과 월간 분석을 할 수 있다.
- [ ] 날짜별 결과에 수행한 운동·볼륨·부위 균형·개선 방향 네 묶음이 담긴다.
- [ ] 주간·월간 결과에 기간 흐름·볼륨 흐름·부위 균형·개선 방향 네 묶음이 담긴다.
- [ ] 주간·월간 분석에 그 기간의 운동한 날 수와 쉰 날 수가 함께 넘어간다.
- [ ] 기록이 없는 기간을 분석하면 호출하지 않고 안내한다.
- [ ] 저장된 결과가 있으면 다시 열 때 그대로 보이고 호출하지 않는다.
- [ ] 새로 분석하기를 누르면 다시 호출해 결과를 덮어쓴다.
- [ ] 호출이 실패하면 이전 결과가 남고 실패 문구가 뜬다.
- [ ] 토큰이 없으면 분석 대신 토큰 등록으로 가라는 팝업이 뜬다.
- [ ] 팝업의 확인을 누르면 토큰 설정 화면으로 이동한다.

**단위 5 — 인바디 분석**

- [ ] 마이 탭에 `인바디 분석` 줄이 생기고 눌러 들어갈 수 있다.
- [ ] 사진을 고르고 분석 버튼을 눌러 결과를 받는다.
- [ ] 사진 없이 분석 버튼을 누를 수 없다.
- [ ] 결과에 측정값·체성분 평가·식단 개선·운동 개선 네 묶음이 담긴다.
- [ ] 고른 사진이 앱 내부 저장소로 복사돼, 갤러리에서 지워도 이력에 남는다.
- [ ] 지난 분석이 최신순 목록으로 남고, 하나를 누르면 그 결과와 그때 사진이 보인다.
- [ ] 호출이 실패하면 고른 사진이 남고 실패 문구가 뜬다.
- [ ] 호출이 실패하면 복사한 사진 파일을 남기지 않는다.
- [ ] 토큰이 없으면 분석 대신 토큰 등록으로 가라는 팝업이 뜬다.
- [ ] 팝업의 확인을 누르면 토큰 설정 화면으로 이동한다.

### 비목표

- 서버를 두고 키를 대신 보관하는 것. 키는 기기에만 둔다.
- 키를 암호화해 보관하는 것. 안드로이드에 사용자 키를 안전하게 둘 곳이 없어 값을 더하지 못한다.
- 분석 결과를 사람이 고치는 것. 받은 그대로 보여준다.
- 칼로리를 기록에 항목으로 더하는 것. 추정은 분석 결과 안에만 남는다.
- 음식 하나하나를 칼로리·영양소 숫자 컬럼으로 저장하는 것. 묶음은 AI가 쓴 글로만 남는다.
- 볼륨을 기록에 항목으로 더하거나 표로 저장하는 것. 분석에 넘길 때만 계산한다.
- 인바디 측정값을 숫자 컬럼으로 뽑아 저장하는 것. 읽은 값은 결과 글 안에만 남는다.
- 인바디 수치의 시간에 따른 변화를 그래프로 그리는 것.
- 인바디 이력을 지우는 기능.
- 부위별 볼륨 추이를 그래프로 그리는 것.
- 주간·월간을 위해 분석되지 않은 날을 자동으로 먼저 분석하는 것.
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
    /** 열쇠마다 마지막 결과 하나씩. 결과가 없는 열쇠는 담기지 않는다. */
    suspend fun getLatestOf(kind: AnalysisKind, scopeKeys: List<String>): Map<String, AnalysisResult>
    suspend fun save(result: AnalysisResult): Long
}
```

#### 분석 결과의 모양 (단위 3에서 확정)

결과를 요약 한 덩어리와 개선 방향 한 덩어리로만 두면 먹은 음식·칼로리·영양 성분을 갈라 보여줄 수 없다. 묶음 목록을 더한다. 종류마다 묶음 제목이 다르므로 제목도 결과에 담는다 — 식단 전용 필드를 두지 않아 운동·인바디가 같은 모양을 쓴다.

```kotlin
data class AnalysisSection(val title: String, val body: String)

data class AnalysisContent(
    val summary: String,
    val sections: List<AnalysisSection>,
)
```

| 종류 | AI에게 요구하는 묶음 제목 |
|---|---|
| `DIET_DAILY` | `먹은 음식` `칼로리` `영양 성분` `개선 방향` |
| `DIET_WEEKLY`·`DIET_MONTHLY` | `기간 흐름` `칼로리 흐름` `영양 균형` `개선 방향` |
| `WORKOUT_DAILY` | `수행한 운동` `볼륨` `부위 균형` `개선 방향` |
| `WORKOUT_WEEKLY`·`WORKOUT_MONTHLY` | `기간 흐름` `볼륨 흐름` `부위 균형` `개선 방향` |
| `INBODY` | `측정값` `체성분 평가` `식단 개선` `운동 개선` |

AI가 다른 제목을 돌려줘도 받은 그대로 그린다 — 제목을 검사해 되돌리면 답 전체를 버리게 된다.

`AnalysisResultEntity`는 묶음을 `sections` 한 컬럼에 JSON 문자열로 담는다. 묶음을 따로 조회하거나 정렬할 일이 없어 표를 나눌 이유가 없다. 이 표는 아직 커밋되지 않았으므로 `MIGRATION_3_4`의 `CREATE TABLE`을 고치고 새 마이그레이션을 더하지 않는다.

| Entity 필드 | 타입 | 귀착지 |
|---|---|---|
| `id` | `Long` | `AnalysisResult.id` |
| `kind` | `String` | `AnalysisResult.kind` — `AnalysisKind.name` |
| `scopeKey` | `String` | `AnalysisResult.scopeKey` |
| `summary` | `String` | `AnalysisContent.summary` → 요약 카드 |
| `sections` | `String` | `AnalysisContent.sections` → 묶음 카드 목록. JSON 배열 |
| `imageFileName` | `String?` | `AnalysisResult.imagePath`로 풀어 낸다 → 인바디 이력의 사진. 다른 분석은 `null` |
| `createdAtMillis` | `Long` | `AnalysisResult.createdAtMillis` → 분석 시각 표기 |

`imageFileName`은 단위 5에서 더한다. 이 표 역시 그때까지 커밋되지 않았으면 `MIGRATION_3_4`의
`CREATE TABLE`을 고치고, 이미 커밋됐으면 `MIGRATION_4_5`로 컬럼을 더한다 — 어느 쪽인지는 착수
시점의 `git log`가 정한다.

#### 주간·월간이 읽는 것

| DAO | 함수 | 쿼리 |
|---|---|---|
| `AnalysisResultDao` | `getByScopeKeys(kind: String, scopeKeys: List<String>): List<AnalysisResultEntity>` | `WHERE kind = :kind AND scopeKey IN (:scopeKeys) ORDER BY createdAtMillis DESC, id DESC` |

한 날짜를 여러 번 분석했으면 행이 여럿이므로 저장소가 날짜마다 첫 행만 남긴다.

`AiAnalysisRepository`는 단위 1에서 `verifyCredential`만 갖는다. 단위 3에서 식단 분석 둘이 붙는다.

```kotlin
/** 하루치 분석에 보낼 기록 하나. 사진 경로를 함께 넘긴다. */
data class DietAnalysisEntry(val date: LocalDate, val memo: String?, val imagePath: String)

data class DietAnalysisRequest(
    val credential: AiCredential,
    val profile: UserProfile,
    val periodLabel: String,
    val entries: List<DietAnalysisEntry>,
)

/** 종합에 보낼 하루치 분석 하나. 사진은 넘기지 않는다. */
data class DietDailySummary(val date: LocalDate, val content: AnalysisContent)

data class DietSummaryRequest(
    val credential: AiCredential,
    val profile: UserProfile,
    val periodLabel: String,
    val dailyResults: List<DietDailySummary>,
)

interface AiAnalysisRepository {
    suspend fun verifyCredential(credential: AiCredential)

    /** 하루치 식단. 사진과 메모로 먹은 음식·칼로리·영양 성분을 정리한다. */
    suspend fun analyzeDiet(request: DietAnalysisRequest): AnalysisContent

    /** 날짜별 분석을 모아 기간의 흐름을 종합한다. 사진을 다시 보내지 않는다. */
    suspend fun summarizeDiet(request: DietSummaryRequest): AnalysisContent
}
```

단위 4에서 운동 분석 하나가 더 붙는다. 운동은 날짜별과 기간이 같은 재료(기록 원문)를 쓰므로
함수가 하나다 — 무엇을 쓸지는 [WorkoutAnalysisRequest.kind]가 정한다.

```kotlin
/** 세트 하나를 분석에 넘길 모양으로 편 것. */
data class WorkoutAnalysisSet(val repeatCount: Int, val intensityValue: Int, val intensityType: IntensityType)

data class WorkoutAnalysisEntry(
    val date: LocalDate,
    val bodyPart: BodyPart,
    val exerciseName: String,
    val sets: List<WorkoutAnalysisSet>,
)

data class WorkoutAnalysisRequest(
    val credential: AiCredential,
    val profile: UserProfile,
    val kind: AnalysisKind,
    val periodLabel: String,
    val entries: List<WorkoutAnalysisEntry>,
    /** 기간 안에 기록이 있는 날 수. AI가 세지 않게 앱이 세어 넘긴다. */
    val workoutDayCount: Int,
    /** 기간 안에 기록이 없는 날 수. 오늘 이후의 날은 세지 않는다. */
    val restDayCount: Int,
    /** 무게 종목의 총 볼륨(kg). `무게 × 횟수`의 합이다. */
    val totalWeightVolume: Int,
    /** 각도 종목의 총 횟수. 무게가 없어 볼륨에 섞지 않는다. */
    val totalBodyweightReps: Int,
)

interface AiAnalysisRepository {
    // ... 식단 둘에 더해
    suspend fun analyzeWorkout(request: WorkoutAnalysisRequest): AnalysisContent
}
```

**볼륨과 날 수는 앱이 세어 넘긴다.** 정확한 산술이라 추정할 이유가 없고, AI에게 시키면 틀린 숫자가
결과에 남는다. 칼로리를 AI에게 맡긴 것과 다른 판단인 이유는 칼로리가 사진에서 얻는 추정값이기
때문이다. 각도 종목(푸쉬업 등)은 무게가 없어 kg 볼륨에 섞지 않고 횟수만 따로 센다.

단위 5에서 인바디 하나가 더 붙는다.

```kotlin
data class InbodyAnalysisRequest(
    val credential: AiCredential,
    val profile: UserProfile,
    /** 앱 내부 저장소에 복사된 사진의 경로. */
    val imagePath: String,
)

interface AiAnalysisRepository {
    // ... 식단 둘, 운동 하나에 더해
    suspend fun analyzeInbody(request: InbodyAnalysisRequest): AnalysisContent
}
```

#### 인바디 이력이 읽는 것

| DAO | 함수 | 쿼리 |
|---|---|---|
| `AnalysisResultDao` | `observeByKind(kind: String): Flow<List<AnalysisResultEntity>>` | `WHERE kind = :kind ORDER BY createdAtMillis DESC, id DESC` |

저장소는 이것을 `AnalysisResultRepository.observeHistory(kind)`로 낸다.

#### 사진 저장소를 식단과 함께 쓴다

`DietImageStore`가 하는 일(내부 저장소 복사·삭제·경로·갤러리 내보내기)이 인바디에 그대로 필요하다.
디렉터리 이름만 다르다. 클래스를 `LocalImageStore`로 옮기고 디렉터리를 생성자로 받아, Hilt가
한정자로 둘을 낸다.

```kotlin
@Qualifier annotation class DietImages
@Qualifier annotation class InbodyImages
```

**식단 디렉터리 이름(`diet_images`)은 그대로 둔다.** 바꾸면 이미 저장된 사진이 전부 사라진다.
갤러리 내보내기는 식단만 쓰지만 같은 클래스에 남긴다 — 쓰지 않는다고 갈라 두면 클래스가 둘이 된다.

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

#### Onboarding (단위 2, 구현 완료)

| State 필드 | 타입 | 초기값 |
|---|---|---|
| `input` | `ProfileInput` | `ProfileInput()` — 모든 칸이 빈 문자열과 빈 집합 |
| `isSaving` | `Boolean` | `false` |

`Intent`: `ChangeInput(input: ProfileInput)` / `ClickStart` / `ClickSkip`

`Effect`: `ShowMessage(message: String)`

저장이 실패하면 완료 표시를 켜지 않고 `isSaving`을 되돌린다 — 켜면 다시 물을 길이 없어진다. 완료 표시가 켜지면 앱 껍데기가 첫 탭으로 옮기므로 이 화면에는 이동 Effect가 없다.

#### Profile (단위 2, 구현 완료)

| State 필드 | 타입 | 초기값 |
|---|---|---|
| `input` | `ProfileInput` | `ProfileInput()` |
| `isLoading` | `Boolean` | `false` |
| `isSaving` | `Boolean` | `false` |

`Intent`: `ChangeInput(input: ProfileInput)` / `ClickSave` / `ClickBack`

`Effect`: `GoBack` / `ShowMessage(message: String)`

`ProfileInput`은 숫자도 문자열로 들고 있다. 지우는 도중의 빈 칸과 0을 구분해야 한다.

#### DietAnalysis (단위 3)

날짜별·주간·월간이 같은 화면이다. 화면이 하는 일은 같고, 무엇을 재료로 부르는지만 다르다 — 날짜별은 사진을, 주간·월간은 저장된 날짜별 분석을 보낸다. 그 갈림은 UseCase 안에 있고 화면은 모른다.

| State 필드 | 타입 | 초기값 |
|---|---|---|
| `periodLabel` | `String` | NavKey의 기간으로 만든 표기. 기본값 없음 |
| `kind` | `AnalysisKind` | NavKey의 기간 종류로 정한다. 기본값 없음 |
| `result` | `AnalysisResult?` | `null`. 저장된 결과가 있으면 채워진다 |
| `hasCredential` | `Boolean` | `false` |
| `isLoading` | `Boolean` | `false` — 저장된 결과를 읽는 동안 |
| `isAnalyzing` | `Boolean` | `false` — AI를 부르는 동안 |
| `isTokenDialogVisible` | `Boolean` | `false` |
| `errorMessage` | `String?` | `null` |

파생 값은 State의 계산 프로퍼티로 둔다. `canAnalyze = !isAnalyzing && !isLoading`.

`Intent`: `ClickAnalyze` / `ClickBack` / `ConfirmTokenDialog` / `DismissTokenDialog`

`Effect`: `GoBack` / `NavigateToAiToken` / `ShowMessage(message: String)`

`ClickAnalyze`는 `hasCredential`이 거짓이면 부르지 않고 `isTokenDialogVisible`을 켠다. 팝업의 확인은 `NavigateToAiToken`을 낸다.

호출이 실패하면 `result`는 그대로 두고 `errorMessage`를 채운다 — 이전 결과가 남아 있는 편이 빈 화면보다 낫다.

부르지 않고 되돌리는 경우가 둘이다. 날짜별은 그 날 기록이 없을 때, 주간·월간은 기간 안에 날짜별 분석이 하나도 없을 때다. 둘은 `errorMessage`에 서로 다른 문구를 넣는다 — 사용자가 다음에 할 일이 다르다.

#### WorkoutAnalysis (단위 4)

State 필드·Intent·Effect가 `DietAnalysis`와 같다. 이름만 `WorkoutAnalysis*`이고, 초기값과 실패 처리도
같다. 다른 것은 두 가지뿐이다.

- 주간·월간이 날짜별 분석에 기대지 않으므로 `먼저 날짜별로 분석해 주세요` 경로가 없다. 기간에 기록이
  하나도 없으면 날짜별과 같은 `분석할 기록이 없습니다`로 되돌린다.
- 화면 제목이 `운동 분석`이다.

두 화면의 State를 하나로 합치지 않는다. 화면 하나는 패키지 하나이고 State는 그 화면의 것이라는
규칙(`.claude/rules/code-style.md`)을 깨지 않는다. 겹치는 것은 그리는 부분이며 그쪽을 공용으로 뺀다.

#### Inbody (단위 5)

| State 필드 | 타입 | 초기값 |
|---|---|---|
| `pickedImageUri` | `String?` | `null` — 고르기만 하고 아직 분석하지 않은 사진 |
| `result` | `AnalysisResult?` | `null` — 방금 받은 것 또는 이력에서 고른 것 |
| `history` | `List<AnalysisResult>` | `emptyList()` |
| `hasCredential` | `Boolean` | `false` |
| `isLoading` | `Boolean` | `false` — 이력을 읽는 동안 |
| `isAnalyzing` | `Boolean` | `false` |
| `isTokenDialogVisible` | `Boolean` | `false` |
| `errorMessage` | `String?` | `null` |

파생 값은 State의 계산 프로퍼티로 둔다. `canAnalyze = pickedImageUri != null && !isAnalyzing`.
사진을 고르지 않으면 버튼이 눌리지 않는다.

`Intent`: `PickImage(uri: String)` / `ClickAnalyze` / `ClickHistory(id: Long)` / `ClickBack` /
`ConfirmTokenDialog` / `DismissTokenDialog`

`Effect`: `GoBack` / `NavigateToAiToken`

`ClickHistory`는 그 결과를 `result`에 넣고 `pickedImageUri`를 비운다 — 지난 결과를 보는 동안 고르던
사진이 남아 있으면 무엇을 분석할지가 흐려진다.

성공하면 `pickedImageUri`를 비운다. 사진은 결과에 붙어 이력으로 남으므로 고르던 자리에 남길 이유가 없다.
실패하면 `pickedImageUri`를 그대로 두어 다시 누를 수 있게 하고, 복사한 파일은 지운다.

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

#### 시안 노드

이 다섯 단위에는 Figma 시안이 없다. 마이 탭·온보딩·프로필·분석 화면은 시안에 없는 화면이라, 기존 시안이 정한 색·글자 단계·컴포넌트를 그대로 따라 만든다.

#### 신규 공용 컴포넌트

**`core:designsystem`**

- `ic_user.xml` — 마이 탭 아이콘. 시안에 없어 새로 그린다. 기존 아이콘과 같은 선 굵기와 24 단위 격자를 따른다.
- `SectionRow(title: String, onClick: () -> Unit, modifier: Modifier, description: String?)` — 마이 탭의 줄 하나. 제목과 부제, 오른쪽 화살표.

**`core:ui:components`** (단위 3)

- `AiTokenRequiredDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier)` — 인증 정보 없이 분석을 누른 경우의 팝업. 어디로 갈지는 부르는 쪽이 정한다 — core는 feature의 목적지를 알 수 없다.

**`feature:my:impl`** (단위 2, 구현 완료)

- `ProfileForm(input: ProfileInput, onChange: (ProfileInput) -> Unit, modifier: Modifier)` — 온보딩과 프로필 수정이 함께 쓴다.
- `ProfileSummaryCard(profile: UserProfile, onClick: () -> Unit, modifier: Modifier)` — 마이 탭의 요약.

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
| 빈 | 저장된 키가 없으면 가린 표기와 연결 확인·해제를 그리지 않는다 |
| 에러 | 입력칸 아래에 실패 문구 |
| 권한 | 해당 없음 — 이 화면은 권한을 쓰지 않는다 |

**OnboardingViewImpl** — `Column`. 상단 줄(제목 `시작하기`, 뒤로가기 없음), 안내 문구, 입력 폼, 하단에 `시작하기`와 `건너뛰기`.

| 상태 | 표시 |
|---|---|
| 로딩 | 해당 없음 — 처음 여는 화면이라 읽어 올 값이 없다 |
| 빈 | 해당 없음 — 모든 칸이 비어 있는 것이 정상이다 |
| 에러 | 저장 실패를 안내로 알리고 화면에 머문다 |
| 권한 | 해당 없음 |

**ProfileViewImpl** — `Column`. 상단 줄(뒤로, 제목 `내 정보`), 입력 폼, 하단 저장 버튼.

| 상태 | 표시 |
|---|---|
| 로딩 | 본문 자리에 진행 표시 |
| 빈 | 해당 없음 — 빈 칸이 그대로 정상 상태다 |
| 에러 | 저장 실패를 안내로 알리고 화면에 머문다 |
| 권한 | 해당 없음 |

**DietAnalysisViewImpl** — `Column`. 상단 줄(뒤로, 제목 `식단 분석`, 기간 표기), 요약 카드 하나, 그 아래 묶음 카드가 받은 순서대로 이어지고, 하단 버튼. 결과가 없으면 `분석하기`, 있으면 `새로 분석하기`. 묶음 카드는 `BodyPlanCard` 안에 제목과 본문 두 줄이며, 개수가 종류마다 달라 목록으로 그린다.

| 상태 | 표시 |
|---|---|
| 로딩 | 저장된 결과를 읽는 동안 본문 자리에 진행 표시 |
| 빈 | 결과가 없으면 안내 문구와 `분석하기` 버튼만 |
| 에러 | 결과 카드 아래에 실패 문구. 이전 결과는 지우지 않는다 |
| 권한 | 해당 없음 — 사진은 앱 내부 저장소에서 읽어 권한이 필요 없다 |

#### 결과를 그리는 부분을 공용으로 뺀다 (단위 4)

식단 분석 화면이 요약 카드와 묶음 카드, 분석 시각을 그린다. 운동 분석 화면이 같은 것을 그리고
인바디도 같다. 세 번 베끼지 않고 `core:ui:components`로 뺀다.

| 심볼 | 파일 | 판정 | 시그니처 |
|---|---|---|---|
| `AnalysisResultContent` | `core/ui/components/.../AnalysisResultContent.kt` | 신규 | `AnalysisResultContent(result: AnalysisResult, modifier: Modifier = Modifier)` |
| `AiTokenRequiredDialog` | `core/ui/components/.../AiTokenRequiredDialog.kt` | 재사용 | `AiTokenRequiredDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier)` |
| `BodyPart.label` | `core/ui/components/.../BodyPartUi.kt` | 재사용 | `val BodyPart.label: String` — 화면에만 쓴다 |

지시문의 부위 이름은 이것을 쓰지 못한다. `core:data`가 `core:ui:components`를 의존하면 계층 방향이
뒤집힌다. `AnalysisPrompt.kt`에 같은 뜻의 비공개 매핑을 하나 둔다 — 표기가 겹치지만, 하나는 화면 문구고
하나는 AI에게 보내는 글이라 함께 바뀌어야 할 이유가 없다.

`AnalysisResult`가 도메인 모델이라 `core:ui:components`가 맞다 — 도메인에 종속되지 않는 것만
`core:designsystem`에 둔다.

#### WorkoutAnalysisViewImpl (단위 4)

`DietAnalysisViewImpl`과 같은 얼개다. `Column`에 상단 줄(뒤로, 제목 `운동 분석`), 기간 표기,
`AnalysisResultContent`, 하단 버튼.

| 상태 | 표시 |
|---|---|
| 로딩 | 저장된 결과를 읽는 동안 본문 자리에 진행 표시 |
| 빈 | 결과가 없으면 `아직 분석하지 않았어요`와 `분석하기` 버튼만 |
| 에러 | 결과 아래에 실패 문구. 이전 결과는 지우지 않는다 |
| 권한 | 해당 없음 — 운동 기록은 사진도 외부 저장소도 쓰지 않는다 |

#### InbodyViewImpl (단위 5)

`Column`. 상단 줄(뒤로, 제목 `인바디 분석`), 사진 자리, `AnalysisResultContent`, 이력 목록, 하단 버튼.

사진 자리는 `pickedImageUri`가 있으면 그것을, 없고 `result`에 사진이 붙어 있으면 그것을 그린다.
둘 다 없으면 사진을 고르라는 빈 자리다. 어느 쪽이든 누르면 사진 고르기가 열린다.

이력은 한 줄에 분석 시각과 요약 첫 줄을 담고, 누르면 위 결과 자리가 그 결과로 바뀐다.

| 상태 | 표시 |
|---|---|
| 로딩 | 이력을 읽는 동안 목록 자리에 진행 표시 |
| 빈 | 이력이 없으면 목록을 그리지 않고, 결과 자리에 `사진을 올리고 분석해 보세요` |
| 에러 | 결과 자리 아래에 실패 문구. 고른 사진은 지우지 않는다 |
| 권한 | 해당 없음 — `PickVisualMedia`는 권한을 받지 않는다. 갤러리 내보내기가 없어 쓰기 권한도 없다 |

#### 카피 원문

| 위치 | 문구 |
|---|---|
| 분석 화면 제목 | `식단 분석` |
| 분석 버튼 (결과 없음) | `분석하기` |
| 분석 버튼 (결과 있음) | `새로 분석하기` |
| 분석 중 버튼 | `분석 중` |
| 결과 없음 안내 | `아직 분석하지 않았어요` |
| 요약 제목 | `요약` |
| 묶음 제목 | AI가 돌려준 제목을 그대로 쓴다. 지시문이 요구하는 제목은 데이터 계약의 표에 있다 |
| 분석 시각 표기 | `yyyy년 M월 d일 HH:mm 분석` |
| 기록 없음 (날짜별) | `분석할 기록이 없습니다` |
| 날짜별 분석 없음 (주간·월간) | `먼저 날짜별로 분석해 주세요` |
| 분석 실패 | `분석하지 못했습니다` |
| 토큰 팝업 제목 | `AI 연결이 필요해요` |
| 토큰 팝업 본문 | `분석하려면 먼저 AI 토큰을 등록해야 합니다.` |
| 토큰 팝업 확인 | `등록하러 가기` |
| 토큰 팝업 취소 | `닫기` |
| 캘린더 주간 버튼 | `이번 주 분석` |
| 캘린더 월간 버튼 | `이번 달 분석` |
| 식단 기록 화면 분석 버튼 | `식단 분석` |
| 운동 분석 화면 제목 | `운동 분석` |
| 운동 기록 화면 분석 버튼 | `운동 분석` |
| 마이 탭 인바디 줄 제목 | `인바디 분석` |
| 마이 탭 인바디 줄 부제 | `사진으로 체성분을 분석해요` |
| 인바디 화면 제목 | `인바디 분석` |
| 사진 고르기 빈 자리 | `인바디 사진을 올려 주세요` |
| 결과 없음 안내 | `사진을 올리고 분석해 보세요` |
| 이력 제목 | `지난 분석` |
| 사진 복사 실패 | `사진을 불러오지 못했습니다` |
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

단위 2에서 `OnboardingNavKey`와 `ProfileNavKey`가 늘었고, 단위 5에서 `InbodyNavKey`가 늘어난다.

단위 3은 `:feature:dietlog:api`에 분석 목적지를 더한다. 기간 종류를 문자열이 아니라 enum으로 두는 것은 오타가 컴파일에서 걸리게 하기 위해서다.

```kotlin
enum class DietAnalysisPeriod { DAILY, WEEKLY, MONTHLY }

/** 기간에 든 아무 날 하나만 담는다. 시작과 끝은 화면이 [DietAnalysisPeriod]로 되짚는다. */
@Serializable
data class DietAnalysisNavKey(val period: DietAnalysisPeriod, val dateEpochDay: Long) : BodyPlanNavKey()

fun BodyPlanNavigator.navigateToDietAnalysis(period: DietAnalysisPeriod, date: LocalDate)
```

시작·끝을 둘 다 담지 않는 것은 열쇠와 범위가 갈라져 어긋날 자리를 없애기 위해서다. 화면은
`dietAnalysisPeriodInfo(period, date)` 하나로 종류·저장 열쇠·표기·범위를 한 번에 만든다.

| 경로 | 진입 | 복귀 |
|---|---|---|
| 식단 기록 → 날짜별 분석 | 하단 `식단 분석` | 상단 뒤로 |
| 식단 캘린더 → 주간 분석 | 하단 `이번 주 분석` | 상단 뒤로 |
| 식단 캘린더 → 월간 분석 | 하단 `이번 달 분석` | 상단 뒤로 |
| 분석 → AI 토큰 | 인증 정보가 없을 때 뜨는 팝업의 확인 | 상단 뒤로 |

분석 화면이 토큰 화면으로 가려면 `:feature:dietlog:impl`이 `:feature:my:api`를 의존한다. feature끼리는 서로의 `api`만 의존하는 규칙 안이다.

단위 4는 `:feature:workoutlog:api`에 같은 모양을 더한다.

```kotlin
enum class WorkoutAnalysisPeriod { DAILY, WEEKLY, MONTHLY }

@Serializable
data class WorkoutAnalysisNavKey(val period: WorkoutAnalysisPeriod, val dateEpochDay: Long) : BodyPlanNavKey()

fun BodyPlanNavigator.navigateToWorkoutAnalysis(period: WorkoutAnalysisPeriod, date: LocalDate)
```

기간 enum을 두 feature가 각자 갖는 것은 NavKey가 그 feature의 계약이기 때문이다. 공용으로 올리면
`:core:navigation`이 어떤 화면이 있는지 알게 된다.

| 경로 | 진입 | 복귀 |
|---|---|---|
| 운동 기록 → 날짜별 분석 | 하단 `운동 분석` | 상단 뒤로 |
| 운동 캘린더 → 주간 분석 | 하단 `이번 주 분석` | 상단 뒤로 |
| 운동 캘린더 → 월간 분석 | 하단 `이번 달 분석` | 상단 뒤로 |

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
| 주간·월간에 그 기간의 사진을 다시 보내기 | 한 달치 사진은 요청이 커져 느리고 비싸다. 솎아 보내면 추정이 거칠어진다. 사용자가 날짜별 분석을 종합하는 쪽을 골랐다 |
| 종합할 때 빠진 날을 자동으로 먼저 분석하기 | 버튼 한 번에 호출이 서른 번 날 수 있고 비용은 사용자 몫이다. 스스로 고르게 둔다 |
| 운동 주간·월간도 날짜별 분석을 모아 종합하기 | 식단이 그렇게 한 이유는 사진이 무겁기 때문이다. 운동 기록은 글자뿐이라 한 달치를 그대로 보내도 가볍고, 날짜별 분석을 먼저 하게 만들면 쓰지도 않을 제약만 는다 |
| 볼륨과 운동한 날 수를 AI가 세게 하기 | 정확한 산술이라 추정할 이유가 없고, 틀린 숫자가 결과에 남는다 |
| 식단·운동 분석 화면을 State까지 하나로 합치기 | 화면 하나가 패키지 하나라는 규칙을 깬다. 겹치는 것은 그리는 부분이라 그쪽만 공용으로 뺀다 |
| 인바디 이력을 별도 화면으로 빼기 | 목록이 짧고 누르면 위 결과가 바뀌면 그만이다. 화면과 NavKey가 늘 만큼의 값이 없다 |
| 인바디 사진을 복사하지 않고 고른 URI만 저장 | 갤러리에서 지우면 이력의 사진이 깨진다. 접근 권한도 앱을 다시 켜면 사라진다 |
| 인바디 사진 저장소를 새 클래스로 따로 만들기 | 식단 것과 하는 일이 같고 디렉터리만 다르다. 60줄을 베끼는 대신 디렉터리를 생성자로 받는다 |
| 인바디 측정값을 숫자로 뽑아 저장 | 뽑은 숫자로 할 일(추이 비교)이 비목표다. 저장만 늘고 쓰는 곳이 없다 |
| 음식·칼로리·영양소를 구조화된 표로 저장 | 저장한 숫자로 할 일(비교·그래프)이 비목표다. 저장만 늘고 쓰는 곳이 없다 |
| 결과를 `summary`·`detail` 두 덩어리로 유지 | 먹은 음식·칼로리·영양 성분을 갈라 보여줄 수 없다 |

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

#### 단위 2 — 온보딩과 프로필 (구현 완료)

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/domain/.../model/UserProfile.kt` | 신규 | `Gender`·`Goal`·`UserProfile` |
| `core/domain/.../repository/UserProfileRepository.kt` | 신규 | 인터페이스 |
| `core/domain/.../repository/OnboardingRepository.kt` | 신규 | 인터페이스 |
| `core/local/.../entity/UserProfileEntity.kt` | 신규 | 한 행짜리 Entity |
| `core/local/.../dao/UserProfileDao.kt` | 신규 | DAO |
| `core/local/.../migration/Migrations.kt` | 수정 | `MIGRATION_2_3` |
| `core/local/.../datastore/AppPreferences.kt` | 수정 | 온보딩 완료 여부 |
| `core/data/.../mapper/UserProfileMapper.kt` | 신규 | 매핑 |
| `core/data/.../repository/UserProfileRepositoryImpl.kt` | 신규 | 구현 |
| `core/data/.../repository/OnboardingRepositoryImpl.kt` | 신규 | 구현 |
| `feature/my/api/.../MyNavKey.kt` | 수정 | 목적지 둘 추가 |
| `feature/my/impl/.../onboarding/**` | 신규 | 화면 한 벌 |
| `feature/my/impl/.../profile/**` | 신규 | 화면 한 벌과 공용 폼 |
| `feature/my/impl/.../home/**` | 수정 | 요약 카드 추가 |
| `app/.../navigation/MainShellViewModel.kt` | 신규 | 첫 화면 판정 |
| `app/.../navigation/BodyPlanMainScreen.kt` | 수정 | 온보딩 분기 |

#### 단위 3 — 식단 분석

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/domain/.../model/AnalysisResult.kt` | 신규 | `AnalysisKind`·`AnalysisContent`·`AnalysisResult` |
| `core/domain/.../model/DietAnalysisRequest.kt` | 신규 | 날짜별 요청과 종합 요청 두 묶음 |
| `core/domain/.../repository/AnalysisResultRepository.kt` | 신규 | 인터페이스 |
| `core/domain/.../repository/AiAnalysisRepository.kt` | 수정 | `analyzeDiet`·`summarizeDiet` 추가 |
| `core/domain/.../usecase/AnalyzeDietUseCase.kt` | 신규 | 기간 종류로 갈라 모으고 부르고 남긴다 |
| `core/local/.../entity/AnalysisResultEntity.kt` | 신규 | Entity |
| `core/local/.../dao/AnalysisResultDao.kt` | 신규 | DAO |
| `core/local/.../migration/Migrations.kt` | 수정 | `MIGRATION_3_4` |
| `core/network/.../AiClient.kt` | 수정 | `complete`와 `AiImage` |
| `core/network/.../{Claude,Gpt,Gemini}Client.kt` | 수정 | 사진을 실은 호출과 응답 읽기 |
| `core/data/.../ai/AnalysisImageLoader.kt` | 신규 | 사진을 base64로. 최대 12장 |
| `core/data/.../ai/AnalysisPrompt.kt` | 신규 | 날짜별과 종합 지시문 둘 |
| `core/data/.../ai/AnalysisContentParser.kt` | 신규 | 답을 요약과 묶음 목록으로 가름 |
| `core/data/.../repository/AnalysisResultRepositoryImpl.kt` | 신규 | 구현 |
| `core/ui/components/.../AiTokenRequiredDialog.kt` | 신규 | 유도 팝업 |
| `feature/dietlog/api/.../DietLogNavKey.kt` | 수정 | 분석 목적지 |
| `feature/dietlog/impl/.../analysis/**` | 신규 | 화면 한 벌 |
| `feature/dietlog/impl/.../log/**` | 수정 | 분석 진입 |
| `feature/dietlog/impl/.../calendar/**` | 수정 | 주간·월간 진입 |
| `feature/dietlog/impl/build.gradle.kts` | 수정 | `:feature:my:api` 의존 |

#### 단위 4 — 운동 분석

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/domain/.../model/WorkoutAnalysisRequest.kt` | 신규 | 요청 묶음과 편 세트 |
| `core/domain/.../repository/AiAnalysisRepository.kt` | 수정 | `analyzeWorkout` 추가 |
| `core/domain/.../usecase/AnalyzeWorkoutUseCase.kt` | 신규 | 기록을 모으고 볼륨·날 수를 세고 부르고 남긴다 |
| `core/data/.../ai/AnalysisPrompt.kt` | 수정 | 운동 지시문. 날짜별과 기간을 `kind`로 가른다 |
| `core/data/.../repository/AiAnalysisRepositoryImpl.kt` | 수정 | `analyzeWorkout` 구현. 사진은 넘기지 않는다 |
| `core/ui/components/.../AnalysisResultContent.kt` | 신규 | 요약·묶음·분석 시각을 그리는 공용 |
| `feature/workoutlog/api/.../WorkoutLogNavKey.kt` | 수정 | `WorkoutAnalysisPeriod`·`WorkoutAnalysisNavKey`·navigate |
| `feature/workoutlog/impl/.../analysis/**` | 신규 | 화면 한 벌 |
| `feature/workoutlog/impl/.../WorkoutLogNavGraph.kt` | 수정 | 분석 entry 등록 |
| `feature/workoutlog/impl/.../log/composable/WorkoutLogView.kt` | 수정 | `운동 분석` 버튼과 진입 |
| `feature/workoutlog/impl/.../calendar/composable/WorkoutCalendarView.kt` | 수정 | 주간·월간 진입 |
| `feature/workoutlog/impl/build.gradle.kts` | 수정 | `:feature:my:api` 의존 |
| `feature/dietlog/impl/.../analysis/composable/DietAnalysisView.kt` | 수정 | 공용 `AnalysisResultContent`로 갈아끼운다 |

#### 단위 5 — 인바디 분석

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/domain/.../model/AnalysisResult.kt` | 수정 | `imagePath: String?` 추가 |
| `core/domain/.../model/InbodyAnalysisRequest.kt` | 신규 | 요청 묶음 |
| `core/domain/.../repository/AiAnalysisRepository.kt` | 수정 | `analyzeInbody` 추가 |
| `core/domain/.../repository/AnalysisResultRepository.kt` | 수정 | `observeHistory`, 사진을 함께 받는 `save` |
| `core/domain/.../usecase/AnalyzeInbodyUseCase.kt` | 신규 | 복사·호출·저장, 실패 시 복사한 파일 되돌리기 |
| `core/local/.../entity/AnalysisResultEntity.kt` | 수정 | `imageFileName` 컬럼 |
| `core/local/.../dao/AnalysisResultDao.kt` | 수정 | `observeByKind` |
| `core/local/.../migration/Migrations.kt` | 수정 | 컬럼 반영 (착수 시점의 커밋 상태에 따름) |
| `core/data/.../image/LocalImageStore.kt` | 신규 | `DietImageStore`를 옮겨 디렉터리를 받게 한다 |
| `core/data/.../image/DietImageStore.kt` | 삭제 | 위로 합친다 |
| `core/data/.../di/DataModule.kt` | 수정 | 한정자로 두 저장소 제공 |
| `core/data/.../repository/DietLogRepositoryImpl.kt` | 수정 | 한정자 붙인 주입으로 |
| `core/data/.../ai/AnalysisPrompt.kt` | 수정 | 인바디 지시문 |
| `core/data/.../repository/AiAnalysisRepositoryImpl.kt` | 수정 | `analyzeInbody` 구현 |
| `core/data/.../repository/AnalysisResultRepositoryImpl.kt` | 수정 | 이력 조회, 사진 파일명 저장 |
| `core/data/.../mapper/AnalysisResultMapper.kt` | 수정 | `imagePath` 매핑 |
| `feature/my/api/.../MyNavKey.kt` | 수정 | `InbodyNavKey`와 navigate |
| `feature/my/impl/.../inbody/**` | 신규 | 화면 한 벌 |
| `feature/my/impl/.../MyNavGraph.kt` | 수정 | entry 등록 |
| `feature/my/impl/.../home/**` | 수정 | `인바디 분석` 줄 추가 |

### 구현 순서

단위마다 검증을 통과한 뒤 커밋한다.

**단위 1 — AI 기반과 마이 탭**

- 쓰는 것: `BodyPlanTopBar`, `BodyPlanCard`, `PrimaryButton`, `OutlinedActionButton`, `BaseTextField`, `BodyPlanIcon`, `BodyPlanDatabase`가 아닌 DataStore
- 만드는 것: `AiClient`와 제공자별 클라이언트 셋, `AppPreferences`, `AiProvider`, `AiCredential`, `AiCredentialRepository`·`Impl`, `AiAnalysisRepository`·`Impl`, `SectionRow`, `ic_user`, `:feature:my:api`·`:impl`, `MyNavKey`·`AiTokenNavKey`, 두 화면 한 벌씩, 세 번째 탭
- 검증: `./gradlew :app:assembleDebug :feature:my:impl:testDebugUnitTest` + Preview 렌더
- 리뷰·테스트: 두 ViewModel과 `AiCredentialRepositoryImpl`이 대상이다. 실제 호출은 페이크로 막는다

**단위 2 — 온보딩과 프로필** (구현 완료)

- 쓰는 것: 단위 1의 `AppPreferences`, `BodyPlanCard`, `PillChip`, `PrimaryButton`, `BaseTextField`
- 만드는 것: `UserProfile`, 두 Repository와 구현, `UserProfileEntity`·DAO, `MIGRATION_2_3`, 온보딩·프로필 화면, `ProfileForm`, `ProfileSummaryCard`, `MainShellViewModel`
- 검증: `./gradlew :app:assembleDebug :feature:my:impl:testDebugUnitTest`

**단위 3 — 식단 분석**

- 쓰는 것: 단위 1의 `AiClient`·`AiCredentialRepository`, 단위 2의 `UserProfileRepository`, 기존 `DietLogRepository`
- 만드는 것: 분석 도메인과 저장소, `complete` 호출, 지시문 둘과 답 가르기, `AnalyzeDietUseCase`, `AiTokenRequiredDialog`, 분석 화면 한 벌, 기록·캘린더의 진입점
- 검증: `./gradlew :app:assembleDebug :core:data:testDebugUnitTest :feature:dietlog:impl:testDebugUnitTest`

**단위 4 — 운동 분석**

- 쓰는 것: 단위 1의 `AiClient`·`AiCredentialRepository`, 단위 2의 `UserProfileRepository`, 단위 3의
  `AnalysisResultRepository`·`AnalysisScopeKey`·`AnalysisContentParser`·`AiTokenRequiredDialog`,
  기존 `WorkoutLogRepository`·`Clock`
- 만드는 것: `WorkoutAnalysisRequest`와 편 세트, `analyzeWorkout`, 운동 지시문, `AnalyzeWorkoutUseCase`,
  `AnalysisResultContent`, `WorkoutAnalysisPeriod`·`WorkoutAnalysisNavKey`, 분석 화면 한 벌,
  기록·캘린더의 진입점
- 검증: `./gradlew :app:assembleDebug :core:domain:test :feature:workoutlog:impl:testDebugUnitTest :feature:dietlog:impl:testDebugUnitTest` + Preview 렌더
- 리뷰·테스트: `AnalyzeWorkoutUseCase`와 `WorkoutAnalysisViewModel`이 대상이다. 실제 호출은 페이크로 막는다

**단위 5 — 인바디 분석**

- 쓰는 것: 단위 1의 `AiClient`·`AiCredentialRepository`·`SectionRow`, 단위 2의 `UserProfileRepository`,
  단위 3의 `AnalysisResultRepository`·`AnalysisContentParser`·`AiTokenRequiredDialog`, 단위 4의
  `AnalysisResultContent`, 기존 `DietImageStore`의 파일 다루기, `BaseImage`
- 만드는 것: `imagePath`가 붙은 `AnalysisResult`, `InbodyAnalysisRequest`, `analyzeInbody`, 인바디
  지시문, `AnalyzeInbodyUseCase`, `observeHistory`, `LocalImageStore`와 두 한정자, `InbodyNavKey`,
  인바디 화면 한 벌, 마이 탭의 줄
- 검증: `./gradlew :app:assembleDebug :core:domain:test :core:data:testDebugUnitTest :feature:my:impl:testDebugUnitTest` + Preview 렌더
- 리뷰·테스트: `AnalyzeInbodyUseCase`와 `InbodyViewModel`이 대상이다. 파일 시스템과 실제 호출은 페이크로 막는다

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| `BodyPlanMainScreen`의 탭 목록 | 운동·식단 탭. 탭이 셋이 되면서 폭 배분이 바뀐다 |
| `core/data/build.gradle.kts`에 `:core:network` 추가 | 전체 빌드 |
| `core/local`에 DataStore 추가 | Room 쪽 주입. 같은 모듈에 저장 방식이 둘이 된다 |
| `DataModule`에 바인딩 추가 | 기존 Repository 주입 |
| `BodyPlanDatabase` version 3·4 | 운동·식단 기록. `MIGRATION_2_3`·`MIGRATION_3_4`가 기존 데이터를 지우지 않는지 확인한다 |
| `AiClient`에 `complete` 추가 | 세 클라이언트 전부. 연결 확인 경로가 그대로 도는지 확인한다 |
| `feature:dietlog:impl`이 `:feature:my:api` 의존 | 식단 기록·캘린더 화면. 다른 feature의 `impl`을 끌어오지 않았는지 확인한다 |
| `DietAnalysisViewImpl`을 공용 `AnalysisResultContent`로 교체 | 식단 분석 화면. 요약·묶음·분석 시각 표기가 그대로인지, Preview가 그대로 렌더되는지 확인한다 |
| `AiAnalysisRepository`에 `analyzeWorkout` 추가 | 식단 분석 경로. 구현이 하나뿐이라 컴파일로 드러난다 |
| `WorkoutLogView`·`WorkoutCalendarView`의 Events | 두 화면의 기존 진입(기록 작성, 종목 관리). 파라미터가 늘어 Preview와 navGraph가 함께 바뀐다 |
| `DietImageStore`를 `LocalImageStore`로 합침 | 식단 사진 추가·삭제·갤러리 내보내기. **디렉터리 이름이 `diet_images` 그대로인지** 확인한다 — 바뀌면 저장된 사진이 전부 사라진다 |
| `AnalysisResultRepository.save`가 사진 파일명을 받음 | 식단·운동 분석 저장. 사진 없는 호출이 `null`로 도는지 확인한다 |
| `AnalysisResultEntity`에 `imageFileName` 추가 | 저장된 식단·운동 분석. 마이그레이션이 기존 결과를 지우지 않는지 확인한다 |
| `AiAnalysisRepository`에 `analyzeInbody` 추가 | 식단·운동 분석 경로와 테스트의 페이크 셋 |

## 자기 대조

자기 대조: 통과 (대조 15/15)
- 고침: 확정 전제, 수용 조건, 비목표, 데이터 계약, 상태 계약, 화면 구성, 네비게이션, 버린 안, 파일별 작업, 구현 순서, 회귀 대상
