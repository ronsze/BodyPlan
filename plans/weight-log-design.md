# 체중 기록

**작성일**: 2026-09-10

## 설계

### 요약

날짜별 체중(kg)을 손으로 기록하는 화면을 마이 탭 아래 새로 만든다. 오늘과 어제만 쓰거나 고칠 수 있다.
같은 화면에서 일간 변동, 주간 평균 변동, 월간 평균 변동을 숫자로 보여 준다.
저장소는 `weight_record` 테이블을 새로 두고, 인바디 분석이 읽은 체중이나 프로필의 체중과 섞지 않는다.

### 배경

인바디는 사진을 AI가 읽은 추정값이고 잴 때마다 찍히지 않는다. 매일의 흐름을 보려면 사용자가 직접 넣는
값이 따로 있어야 한다. 편집 가능 범위(오늘·어제)는 운동·식단 일지와 같은 규칙을 쓴다 —
`IsEditableDateUseCase`가 이미 그 판정을 한다.

사용자가 정한 것(2026-09-10 채팅):
- 화면 배치는 마이 탭 아래 새 화면. 인바디 화면에 통합하지 않는다.
- 변동은 이동 구간 기준. 달력 주·월 경계를 쓰지 않는다.
- 인바디가 읽은 체중과 분리한다. 손으로 넣은 값만 변동 계산에 쓴다.

### 수용 조건

- [ ] 화면에 들어가면 오늘이 선택된 상태로 열리고, 오늘의 기록이 있으면 입력칸에 그 값이 채워져 있다.
- [ ] 날짜 칩으로 어제를 고르면 입력칸이 어제의 기록 값(없으면 빈 칸)으로 바뀐다.
- [ ] 오늘·어제 두 날짜만 칩으로 나오고, 그 밖의 날짜를 고르는 길이 화면에 없다.
- [ ] 값을 넣고 저장하면 그 날짜의 기록이 남고, 같은 날짜에 다시 저장하면 행이 늘지 않고 값만 바뀐다.
- [ ] 20.0 미만 또는 300.0 초과, 빈 칸, 숫자가 아닌 입력이면 저장 버튼이 눌리지 않는다.
- [ ] 0.1kg 단위 소수를 넣을 수 있다(`72.4`). 소수점은 하나, 소수 자리는 한 자리까지만 입력된다.
- [ ] 화면을 열어 둔 채 자정을 넘겨 선택 날짜가 편집 불가가 되면, 저장이 거절되고 "오늘과 어제만 기록할 수 있어요"가 뜬다.
- [ ] 오늘과 어제 기록이 모두 있으면 일간 변동에 `오늘 − 어제`가 부호와 함께 나온다. 둘 중 하나라도 없으면 `-`가 나온다.
- [ ] 최근 7일(오늘 포함)과 그 이전 7일 각각에 기록이 하나 이상 있으면 주간 평균 변동에 두 구간 평균의 차가 나온다. 한쪽이 비면 `-`.
- [ ] 최근 30일과 그 이전 30일 각각에 기록이 하나 이상 있으면 월간 평균 변동에 두 구간 평균의 차가 나온다. 한쪽이 비면 `-`.
- [ ] 기록이 하나도 없으면 목록 자리에 "아직 기록이 없어요"가 나오고 변동 세 칸은 모두 `-`다.
- [ ] 조회가 실패하면 로딩이 걷히고 "체중 기록을 불러오지 못했습니다"가 뜬다.
- [ ] 저장이 실패하면 저장 중 표시가 걷히고 "체중을 저장하지 못했습니다"가 뜨며, 입력한 값은 칸에 남는다.
- [ ] 마이 홈에 "체중 기록" 줄이 생기고 눌러 이 화면으로 들어간다.

### 비목표

- 기록 삭제. 요청에 없다. Repository에 delete를 두지 않는다.
- 체중 추이 그래프. 변동은 숫자 세 칸으로 낸다. `InbodyTrendChart`를 일반화하지 않는다.
- 저장한 체중으로 `UserProfile.weightKg`를 갱신하는 일. 인바디와 분리한다는 결정과 같은 이유다.
- 인바디 분석의 `weightKg`를 이 화면의 기록·변동에 합치는 일.
- `ProfileForm`의 `private fun NumberField`를 designsystem으로 올리는 일. 그 함수는 정수 전용이고
  호출부가 프로필 하나뿐이다. 이번 화면은 소수를 받아야 하므로 화면 안에 전용 입력 컴포저블을 둔다.
- 캘린더로 지난 날짜를 훑어보는 일.

### 데이터 계약

**테이블 `weight_record`** (신규, DB 버전 7 → 8)

| 컬럼 | 타입 | 제약 | 도메인 | 귀착지 |
|---|---|---|---|---|
| `dateEpochDay` | INTEGER | PRIMARY KEY (NOT NULL) | `WeightRecord.date` (`LocalDate.ofEpochDay`) | 목록 줄의 날짜, 변동 계산의 구간 판정 |
| `weightKg` | REAL | NOT NULL | `WeightRecord.weightKg` | 입력칸 초기값, 목록 줄의 값, 변동 계산의 피연산자 |
| `updatedAtMillis` | INTEGER | NOT NULL | 없음 | 미사용 — 나중에 "언제 고쳤는지"를 보이려 남긴다. 화면에 쓰지 않는다 |

날짜를 PK로 두는 것은 하루 한 값이기 때문이다. `@Insert(onConflict = REPLACE)` 하나로 넣기와 고치기를
겸한다 — `UserProfileDao.upsert`와 같은 방식이다. 인덱스를 따로 두지 않는다(PK가 곧 조회 키다).

**DAO 쿼리** (`WeightRecordDao`)

| 함수 | 쿼리 | 쓰는 곳 |
|---|---|---|
| `observeInRange(from: Long, to: Long): Flow<List<WeightRecordEntity>>` | `SELECT * FROM weight_record WHERE dateEpochDay BETWEEN :from AND :to ORDER BY dateEpochDay ASC` | 변동 계산·목록 |
| `upsert(entity: WeightRecordEntity)` | `@Insert(onConflict = OnConflictStrategy.REPLACE)` | 저장 |

날짜 하나를 집어 읽는 `getByDate`는 두지 않는다. 화면은 이미 구독 중인 60일치 목록에서 선택 날짜의
값을 고르므로 호출부가 없다.

**Entity → Domain 매핑** (`core/data/mapper/WeightRecordMapper.kt`)

- `internal fun WeightRecordEntity.toDomain(): WeightRecord` — `dateEpochDay → LocalDate.ofEpochDay(it)`, `weightKg → weightKg`, `updatedAtMillis` 미사용.
- `internal fun WeightRecord.toEntity(updatedAtMillis: Long): WeightRecordEntity` — `date → date.toEpochDay()`, `weightKg → weightKg`, `updatedAtMillis`는 인자(`clock.millis()`).

**도메인 모델**

```kotlin
// core/domain/model/WeightRecord.kt
data class WeightRecord(val date: LocalDate, val weightKg: Double)

// core/domain/model/WeightTrend.kt
data class WeightTrend(
    val dailyChangeKg: Double? = null,
    val weeklyAverageChangeKg: Double? = null,
    val monthlyAverageChangeKg: Double? = null,
)
```

**Repository 계약** (`core/domain/repository/WeightLogRepository.kt`)

```kotlin
interface WeightLogRepository {
    /** [from]~[to] 사이의 기록. 오래된 날짜부터다. 기록이 없는 날짜는 담지 않는다. */
    fun observeRecordsInRange(from: LocalDate, to: LocalDate): Flow<List<WeightRecord>>

    /** 그 날짜의 값을 넣거나 덮어쓴다. 실패하면 던진다. */
    suspend fun save(date: LocalDate, weightKg: Double)
}
```

**변동 계산** (`core/domain/usecase/GetWeightTrendUseCase.kt`)

```kotlin
class GetWeightTrendUseCase @Inject constructor(
    private val weightLogRepository: WeightLogRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<WeightTrend>
}
```

`LocalDate.now(clock)`을 함수 시작에서 한 번만 읽는다(`GetMonthlyDietStatusUseCase`와 같은 규칙).
조회 구간은 `today.minusDays(59) .. today`, 60일. 구간 정의는 사용자가 고른 이동 구간 기준이다.

| 값 | 정의 | `null`이 되는 조건 |
|---|---|---|
| `dailyChangeKg` | `today의 값 − (today-1)의 값` | 두 날짜 중 하나라도 기록이 없을 때 |
| `weeklyAverageChangeKg` | `평균(today-6..today) − 평균(today-13..today-7)` | 두 구간 중 하나라도 기록이 0건일 때 |
| `monthlyAverageChangeKg` | `평균(today-29..today) − 평균(today-59..today-30)` | 두 구간 중 하나라도 기록이 0건일 때 |

평균은 기록이 있는 날만 더해 그 날 수로 나눈다(빠진 날을 0으로 세지 않는다).

### 상태 계약

**체중 기록 화면** (`WeightState` / `WeightIntent` / `WeightEffect`, `feature/my/impl/weight/WeightContracts.kt`)

State 필드 — 전부 초기값 포함:

| 필드 | 타입 | 초기값 | 출처 |
|---|---|---|---|
| `today` | `LocalDate?` | `null` | `initializeData()`에서 `LocalDate.now(clock)` |
| `selectedDate` | `LocalDate?` | `null` | 초기화 때 `today`, 이후 `SelectDate` |
| `input` | `String` | `""` | 선택 날짜의 기록 값 또는 `ChangeInput` |
| `records` | `List<WeightRecord>` | `emptyList()` | `WeightLogRepository.observeRecordsInRange` (60일, 오래된 날짜부터) |
| `trend` | `WeightTrend` | `WeightTrend()` | `GetWeightTrendUseCase` |
| `isLoading` | `Boolean` | `false` | 구독 시작에서 `true`, 첫 방출·실패에서 `false` |
| `isSaving` | `Boolean` | `false` | 저장 시작/종료 |

파생 값(State의 `get()`):

- `editableDates: List<LocalDate>` — `today?.let { listOf(it, it.minusDays(1)) } ?: emptyList()`. 칩 목록이다.
- `canSave: Boolean` — `!isSaving && input.toDoubleOrNull()?.let { it in 20.0..300.0 } == true`.
- `recentRecords: List<WeightRecord>` — `records.sortedByDescending { it.date }.take(14)`. 목록은 최신순이다.

Intent:

| Intent | 결과 |
|---|---|
| `SelectDate(date: LocalDate)` | `selectedDate = date`, `input`을 그 날짜의 기록 값(없으면 `""`)으로 바꾼다 |
| `ChangeInput(text: String)` | 걸러진 문자열을 `input`에 담는다 |
| `ClickSave` | 저장. 아래 참조 |
| `ClickBack` | `WeightEffect.GoBack` |

Effect: `GoBack`, `ShowMessage(message: String)`.

실패·경계 경로:

- 조회 실패(`observeRecordsInRange` 또는 UseCase의 Flow가 던짐) → `isLoading = false`, `ShowMessage("체중 기록을 불러오지 못했습니다")`. 자동 재시도하지 않는다. 화면을 다시 열면 다시 구독한다.
- 저장 실패 → `isSaving = false`, `ShowMessage("체중을 저장하지 못했습니다")`. `input`은 그대로 둔다.
- `ClickSave` 시 `selectedDate`가 `null`이거나 `canSave`가 `false`면 아무 일도 하지 않는다.
- `ClickSave` 시 `isEditableDate(selectedDate)`가 `false`면(화면을 열어 둔 채 자정을 넘긴 경우) 저장하지 않고 `ShowMessage("오늘과 어제만 기록할 수 있어요")`. `IsEditableDateUseCase`의 `invoke(date: LocalDate)` 오버로드를 쓴다 — 지금 시각으로 다시 판정해야 하므로 `today`를 넘기는 오버로드를 쓰지 않는다.
- 저장 성공 시 `input`을 지우지 않는다. 방금 넣은 값이 그 날짜의 값이므로 그대로 두는 것이 맞다.

**마이 홈** (`feature/my/impl/home/MyContracts.kt`, 기존 파일 확장)

- `MyIntent.ClickWeight` 추가 → `MyEffect.NavigateToWeight`.
- State 필드는 더하지 않는다.

### 화면 구성

**체중 기록 화면** — `feature/my/impl/weight/composable/WeightView.kt`

```
Column(fillMaxSize, background(Background))
├ BodyPlanTopBar(title = "체중 기록", onBack = ...)            [재사용]
└ Column(padding(horizontal = 16.dp), spacedBy(12.dp))
  ├ WeightInputCard(...)                                       [신규]
  ├ WeightTrendCard(trend = state.trend)                       [신규]
  └ WeightHistoryCard(records = state.recentRecords)           [신규]
```

재사용 판정(시그니처를 파일에서 확인한 것만 적는다):

| 심볼 | 파일 | 시그니처 | 판정 |
|---|---|---|---|
| `BodyPlanTopBar` | `core/designsystem/.../component/BodyPlanTopBar.kt` | `(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actionText: String? = null, onClickAction: () -> Unit = {})` | 재사용. `onBack`만 채운다 |
| `BodyPlanCard` | `.../component/BodyPlanCard.kt` | `(modifier: Modifier = Modifier, cornerRadius: Dp = 16.dp, contentPadding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit)` | 재사용. 세 카드의 겉면 |
| `BaseText` | `.../component/BaseText.kt` | `(text: String, modifier, color: Color = BaseTextDefaults.color, style: TextStyle = BaseTextDefaults.style, textAlign: TextAlign? = null, maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip)` | 재사용 |
| `BaseTextField` | `.../component/BaseTextField.kt` | `(value: String, onValueChange: (String) -> Unit, modifier, enabled = true, placeholder: String? = null, textStyle, keyboardOptions, keyboardActions, singleLine = false, maxLines)` | 재사용. `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)`, `singleLine = true` |
| `PrimaryButton` | `.../component/BodyPlanButtons.kt` | `(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)` | 재사용. `enabled = state.canSave` |
| `PillChip` | `.../component/SelectableChip.kt` | `(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)` | 재사용. 오늘·어제 날짜 칩 |
| `VerticalSpacer` | `.../component/Spacer.kt` | `(space: Dp)` — 기본값 없음, 값을 반드시 준다 | 재사용. 카드 안 간격 |
| `SectionRow` | `.../component/SectionRow.kt` | `(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, description: String? = null, leading: (@Composable () -> Unit)? = null)` | 재사용. 마이 홈의 진입 줄 |
| `IsEditableDateUseCase` | `core/domain/usecase/IsEditableDateUseCase.kt` | `invoke(date: LocalDate): Boolean`, `invoke(date: LocalDate, today: LocalDate): Boolean` | 재사용. 새로 만들지 않는다 |
| `CollectEffect` | `core/ui/coordinator/CollectEffect.kt` | `<E : Effect> (effect: Flow<E>, onEffect: (E) -> Unit)` | 재사용 |

신규 컴포저블:

- `WeightInputCard(dates: List<LocalDate>, selectedDate: LocalDate?, input: String, canSave: Boolean, isSaving: Boolean, onSelectDate: (LocalDate) -> Unit, onChangeInput: (String) -> Unit, onClickSave: () -> Unit, modifier: Modifier = Modifier)` — `feature/my/impl/weight/composable/WeightInputCard.kt`. 날짜 칩 두 개, 소수 입력칸, 저장 버튼.
- `WeightTrendCard(trend: WeightTrend, modifier: Modifier = Modifier)` — `.../composable/WeightTrendCard.kt`. 세 줄(제목 · 값).
- `WeightHistoryCard(records: List<WeightRecord>, modifier: Modifier = Modifier)` — `.../composable/WeightHistoryCard.kt`. 최신순 목록.

입력 필터(`onChangeInput`에 들어가기 전, `WeightViewModel.handleIntent`가 수행):
정규식 `^\d{0,3}(\.\d?)?$`에 맞는 문자열만 `input`에 담는다. 맞지 않으면 이전 값을 유지한다.
빈 문자열은 통과시킨다(지우는 중이다).

숫자 표기:
- 체중 값: `String.format(Locale.US, "%.1f", value)` + `"kg"` (예: `72.4kg`).
- 변동 값: 부호를 붙인다 — `String.format(Locale.US, "%+.1f", value)` + `"kg"` (예: `+0.4kg`, `-1.2kg`). `null`이면 `"-"`.
- 날짜: 오늘·어제 칩은 `"오늘"`/`"어제"`, 목록 줄은 `DateTimeFormatter.ofPattern("M월 d일")`.

화면 문구(원문):
- 상단 제목: `체중 기록`
- 입력 카드 제목: `오늘의 체중`
- 입력칸 placeholder: `72.4`
- 저장 버튼: `저장`
- 변동 카드 제목: `변동`
- 변동 항목 이름: `일간`, `주간 평균`, `월간 평균`
- 변동 카드 설명: `최근 구간과 그 이전 같은 길이 구간의 차이입니다.`
- 이력 카드 제목: `최근 기록`
- 빈 목록: `아직 기록이 없어요`
- 오류: `체중 기록을 불러오지 못했습니다`, `체중을 저장하지 못했습니다`, `오늘과 어제만 기록할 수 있어요`

화면별 상태:

| 상태 | 체중 기록 화면 | 마이 홈 |
|---|---|---|
| 로딩 | `isLoading`이면 이력 카드 자리에 `BodyPlanCard` 안 `BaseText("불러오는 중...")`. 입력 카드는 그대로 보인다 | 해당 없음(이번 변경으로 로딩이 늘지 않는다) |
| 빈 | 기록 0건 → 이력 카드에 `아직 기록이 없어요`, 변동 세 칸 모두 `-` | 해당 없음 |
| 에러 | `ShowMessage` Effect → Toast. 화면은 그대로 남는다 | 해당 없음 |
| 권한 | 해당 없음 — 저장소·카메라·알림 권한을 쓰지 않는다 | 해당 없음 |

Preview: `WeightViewImpl`에 대해 세 개 — 기록 있음(변동 세 값 모두 있음), 기록 없음(빈 목록·`-`), 로딩.
`BodyPlanTheme {}`로 감싸고 `private val previewUiEvents`를 파일 하단에 둔다(`ProfileView.kt` 관례).

Figma 시안: 해당 없음.

### 네비게이션

- `feature/my/api/.../MyNavKey.kt`에 `@Serializable data object WeightNavKey : BodyPlanNavKey()`와
  `fun BodyPlanNavigator.navigateToWeight() = navigate(WeightNavKey)`를 더한다.
- `feature/my/impl/.../MyNavGraph.kt`의 `myNavGraph(navigator)`에 `entry<WeightNavKey>`를 더하고
  `WeightEvents(goBack = navigator::goBack)`를 넘긴다.
- 진입: 마이 홈의 `SectionRow("체중 기록")` → `MyIntent.ClickWeight` → `MyEffect.NavigateToWeight` → `navigator.navigateToWeight()`.
- 복귀: 상단 뒤로가기 → `WeightIntent.ClickBack` → `WeightEffect.GoBack` → `navigator.goBack()`.
- `:app`은 고치지 않는다. `myNavGraph`가 이미 등록돼 있다.
- 인자 없음. 화면이 스스로 오늘을 읽는다.

### 버린 안

- **인바디 화면에 통합**(채팅에서 사용자가 버림): 인바디는 사진 분석 흐름이라 손입력 기록과 성격이 섞이고, 한 ViewModel이 두 종류의 작업을 들게 된다.
- **달력 경계 기준 변동**(채팅에서 사용자가 버림): 주 초·월 초에 표본이 1~2일뿐이라 값이 크게 흔들린다.
- **인바디 체중과 병합**(채팅에서 사용자가 버림): 인바디는 사진에서 읽은 추정값이라 일간 변동이 오염된다.
- **`weight_record`를 `user_profile`에 컬럼으로 붙이기**: 프로필은 한 행짜리 현재 값이고 체중 기록은 날짜별 여러 행이다. 같은 표에 들어갈 수 없다.
- **`ProfileForm.NumberField`를 designsystem으로 승격해 소수 입력을 얹기**: 그 함수는 정수 전용 필터를 안고 있고 호출부가 프로필뿐이다. 소수 허용 플래그를 더하면 프로필 쪽 회귀 위험만 생긴다.
- **`InbodyTrendChart`를 일반화해 체중 그래프로 재사용**: 요청은 변동 숫자다. 그래프는 비목표.

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/model/WeightRecord.kt` | 신규 | `WeightRecord(date, weightKg)` |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/model/WeightTrend.kt` | 신규 | `WeightTrend(dailyChangeKg, weeklyAverageChangeKg, monthlyAverageChangeKg)` |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/repository/WeightLogRepository.kt` | 신규 | `observeRecordsInRange`, `save` |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/usecase/GetWeightTrendUseCase.kt` | 신규 | 60일 구간 구독 → `WeightTrend` |
| `core/local/src/main/java/kr/sdbk/bodyplan/core/local/entity/WeightRecordEntity.kt` | 신규 | `@Entity(tableName = "weight_record")`, PK `dateEpochDay` |
| `core/local/src/main/java/kr/sdbk/bodyplan/core/local/dao/WeightRecordDao.kt` | 신규 | `observeInRange`, `upsert` |
| `core/local/src/main/java/kr/sdbk/bodyplan/core/local/BodyPlanDatabase.kt` | 수정 | 엔티티 추가, `version = 8`, `abstract fun weightRecordDao()` |
| `core/local/src/main/java/kr/sdbk/bodyplan/core/local/migration/Migrations.kt` | 수정 | `MIGRATION_7_8` — `weight_record` CREATE TABLE |
| `core/local/src/main/java/kr/sdbk/bodyplan/core/local/di/LocalModule.kt` | 수정 | `addMigrations`에 `MIGRATION_7_8`, `provideWeightRecordDao` |
| `core/local/schemas/kr.sdbk.bodyplan.core.local.BodyPlanDatabase/8.json` | 신규(빌드 생성) | 커밋에 포함 |
| `core/data/src/main/java/kr/sdbk/bodyplan/core/data/mapper/WeightRecordMapper.kt` | 신규 | `toDomain` / `toEntity(updatedAtMillis)` |
| `core/data/src/main/java/kr/sdbk/bodyplan/core/data/repository/WeightLogRepositoryImpl.kt` | 신규 | DAO + `Clock` |
| `core/data/src/main/java/kr/sdbk/bodyplan/core/data/di/DataModule.kt` | 수정 | `bindWeightLogRepository` |
| `feature/my/api/src/main/java/kr/sdbk/bodyplan/feature/my/api/MyNavKey.kt` | 수정 | `WeightNavKey`, `navigateToWeight()` |
| `feature/my/impl/src/main/java/kr/sdbk/bodyplan/feature/my/impl/weight/WeightContracts.kt` | 신규 | State/Intent/Effect |
| `feature/my/impl/src/main/java/kr/sdbk/bodyplan/feature/my/impl/weight/WeightViewModel.kt` | 신규 | 구독·저장·입력 필터 |
| `feature/my/impl/src/main/java/kr/sdbk/bodyplan/feature/my/impl/weight/composable/WeightView.kt` | 신규 | `WeightEvents`/`WeightUiEvents`/`WeightView`/`WeightViewImpl`/Preview 3종 |
| `feature/my/impl/src/main/java/kr/sdbk/bodyplan/feature/my/impl/weight/composable/WeightInputCard.kt` | 신규 | 날짜 칩·입력칸·저장 버튼 |
| `feature/my/impl/src/main/java/kr/sdbk/bodyplan/feature/my/impl/weight/composable/WeightTrendCard.kt` | 신규 | 변동 세 줄 |
| `feature/my/impl/src/main/java/kr/sdbk/bodyplan/feature/my/impl/weight/composable/WeightHistoryCard.kt` | 신규 | 최근 기록 목록 |
| `feature/my/impl/src/main/java/kr/sdbk/bodyplan/feature/my/impl/MyNavGraph.kt` | 수정 | `entry<WeightNavKey>` |
| `feature/my/impl/src/main/java/kr/sdbk/bodyplan/feature/my/impl/home/MyContracts.kt` | 수정 | `ClickWeight` / `NavigateToWeight` |
| `feature/my/impl/src/main/java/kr/sdbk/bodyplan/feature/my/impl/home/MyViewModel.kt` | 수정 | `ClickWeight` 분기 |
| `feature/my/impl/src/main/java/kr/sdbk/bodyplan/feature/my/impl/home/composable/MyView.kt` | 수정 | `goToWeight`/`onClickWeight`, `SectionRow("체중 기록")`, Preview 인자 보수 |
| `core/domain/src/test/java/kr/sdbk/bodyplan/core/domain/usecase/GetWeightTrendUseCaseTest.kt` | 신규 | 변동 세 값과 `null` 조건 |
| `core/data/src/test/java/kr/sdbk/bodyplan/core/data/repository/WeightLogRepositoryImplTest.kt` | 신규 | 매핑·upsert |
| `core/data/src/test/java/kr/sdbk/bodyplan/core/data/repository/fake/FakeWeightRecordDao.kt` | 신규 | Room 없이 DAO 대역 |
| `feature/my/impl/src/test/java/kr/sdbk/bodyplan/feature/my/impl/fake/FakeWeightLogRepository.kt` | 신규 | 실패 주입·호출 횟수 |
| `feature/my/impl/src/test/java/kr/sdbk/bodyplan/feature/my/impl/weight/WeightViewModelTest.kt` | 신규 | 수용 조건 검증 |
| `feature/my/impl/src/test/java/kr/sdbk/bodyplan/feature/my/impl/home/MyViewModelTest.kt` | 수정 | `ClickWeight` → `NavigateToWeight` |

### 구현 순서

**1단위 — 도메인 계약**
쓰는 것: `java.time.LocalDate`, `java.time.Clock`, `kotlinx.coroutines.flow.Flow`.
만드는 것: `WeightRecord`, `WeightTrend`, `WeightLogRepository`, `GetWeightTrendUseCase`.
검증: `./gradlew :core:domain:test` (이 단위에서 `GetWeightTrendUseCaseTest`도 함께 쓴다).
커밋: 하지 않는다 — 2단위와 함께 묶는다.

**2단위 — 저장소**
쓰는 것: 1단위의 `WeightRecord`, `WeightLogRepository`.
만드는 것: `WeightRecordEntity`, `WeightRecordDao`, `MIGRATION_7_8`, DB 버전 8, `LocalModule` 등록, `WeightRecordMapper`, `WeightLogRepositoryImpl`, `DataModule` 바인딩, `FakeWeightRecordDao`, `WeightLogRepositoryImplTest`.
검증: `./gradlew :app:compileDebugKotlin`, `./gradlew :core:data:testDebugUnitTest`, `8.json` 생성 확인.
커밋: `체중 기록을 저장하는 계층을 만듦`.

**3단위 — 화면**
쓰는 것: 1·2단위의 `WeightLogRepository`, `GetWeightTrendUseCase`, `WeightRecord`, `WeightTrend`, 기존 `IsEditableDateUseCase`, `BaseViewModel`, `BodyPlanTopBar`, `BodyPlanCard`, `BaseText`, `BaseTextField`, `PrimaryButton`, `PillChip`, `VerticalSpacer`, `CollectEffect`.
만드는 것: `WeightNavKey`/`navigateToWeight`, `WeightContracts`, `WeightViewModel`, `WeightView`·`WeightInputCard`·`WeightTrendCard`·`WeightHistoryCard`, `MyNavGraph` 등록, `FakeWeightLogRepository`, `WeightViewModelTest`.
검증: `./gradlew :app:compileDebugKotlin`, `./gradlew :feature:my:impl:testDebugUnitTest`.
커밋: `체중 기록 화면을 더함`.

**4단위 — 마이 홈 진입**
쓰는 것: 3단위의 `WeightNavKey`, `navigateToWeight`.
만드는 것: `MyIntent.ClickWeight`, `MyEffect.NavigateToWeight`, `MyEvents.goToWeight`, `MyUiEvents.onClickWeight`, `SectionRow("체중 기록")`, `MyViewModelTest` 보강.
검증: `./gradlew :app:compileDebugKotlin`, `./gradlew :feature:my:impl:testDebugUnitTest`.
커밋: `마이에서 체중 기록으로 들어가게 함`.

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| `MyEvents` 생성자(인자 추가) | `MyNavGraph.kt`의 `entry<MyNavKey>` — 인자를 채우지 않으면 컴파일이 깨진다 |
| `MyUiEvents` 생성자(인자 추가) | `MyView.kt`의 `rememberUiEvents`, `previewUiEvents`, Preview 2종 |
| `MyIntent`/`MyEffect` sealed 확장 | `MyViewModel.handleIntent`의 `when`, `MyView.kt`의 `CollectEffect` `when` — 두 곳 모두 분기를 더해야 한다 |
| `BodyPlanDatabase` 버전 7 → 8 | 기존 기록이 남는지 — 마이그레이션이 `weight_record` CREATE만 하고 다른 표를 건드리지 않는지 확인 |
| `LocalModule.provideDatabase` | 운동·식단·인바디 화면이 그대로 뜨는지(같은 DB 인스턴스를 공유한다) |
| `DataModule` 바인딩 추가 | Hilt 그래프 — `:app:compileDebugKotlin`이 KSP 생성까지 돈다 |

자기 대조: 통과 (대조 15/15)
- 고침: 데이터 계약, 상태 계약, 화면 구성, 구현 순서
