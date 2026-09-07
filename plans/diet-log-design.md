# 식단 일지

**작성일**: 2026-09-07

## 설계

### 요약

하루 동안 먹은 것을 사진과 함께 기록하는 feature를 추가한다. 항목 하나가 사진 한 장과 선택 입력인 텍스트를 갖는다. 오늘과 어제만 작성·수정·삭제할 수 있고, 캘린더에서 날짜를 골라 과거 기록을 조회한다.

### 배경

이 작업은 다른 세션(`bodyplan-a4`)이 시작해 모듈 골격까지 만들었고(`e213385`), 사용자 지시로 이 세션이 이어받았다. 상대 세션은 자기 사용자의 확인을 기다리는 중이며 그 워크트리에 더 쓰지 않기로 했다. 확인이 뒤집히면 그 시점에 다시 조율한다.

브랜치 `feature/diet-log`를 `feature/workout-log` 위로 옮겼다. 운동 일지가 먼저 만든 것을 그대로 쓰기 위해서다.

| 물려받는 것 | 위치 | 쓰는 방식 |
|---|---|---|
| `BodyPlanCalendar` | `core:designsystem` | 셀 내용을 `dayContent` 슬롯에 꽂는다 |
| `IsEditableDateUseCase` | `core:domain` | 오늘·어제 판정을 그대로 쓴다 |
| Room 인프라 | `core:local` | `BodyPlanDatabase`에 테이블을 더하고 `version`을 올린다 |
| `Clock` 제공 | `core:data`의 `DataModule` | 그대로 주입받는다 |

### 확정 전제

상대 세션과 합의해 정한 것이다. 임의 판단이 아니다.

| 항목 | 결정 |
|---|---|
| 편집 기간 | 오늘과 어제만 작성·수정·삭제. 그 이전은 조회만 |
| 사진 | 항목마다 한 장, 필수. 없이는 저장하지 않는다 |
| 사진 출처 | Android Photo Picker만. 카메라 촬영은 넣지 않는다 |
| 사진 보관 | 앱 내부 저장소로 복사하고 상대 경로만 저장한다. 항목을 지우면 파일도 지운다 |
| 사진 복사 실패 | 저장 자체를 취소하고 실패를 알린다. 반쪽 기록을 남기지 않는다 |
| 텍스트 | 선택 입력 |
| 끼니 구분 | 두지 않는다. 하루 끼니 수가 정해져 있지 않다. 생성 시각 순으로 정렬하고 제목은 순번으로 쓴다 |
| 수정 범위 | 사진 교체까지 허용 |
| 캘린더 셀 | 그날 첫 항목의 사진 썸네일 한 장. 기록이 없으면 빈 칸 |
| 자정 경계 | 화면 진입 시점에 한 번만 판정한다 |
| 마이그레이션 | 정식 마이그레이션. 테이블을 더하고 `version`을 올리며 기존 기록은 지우지 않는다 (사용자 지시로 변경됨) |

### 수용 조건

**저장소·도메인**

- [ ] 사진과 텍스트를 담은 항목을 저장하고 그대로 다시 읽을 수 있다.
- [ ] 텍스트 없이 사진만으로도 저장된다.
- [ ] 같은 날 항목이 추가한 순서대로 나온다.
- [ ] 저장하면 고른 사진이 앱 내부 저장소로 복사되고, 데이터베이스에는 상대 경로만 남는다.
- [ ] 도메인 모델은 화면이 바로 그릴 수 있는 절대 경로를 낸다.
- [ ] 사진 복사가 실패하면 데이터베이스에 행이 남지 않고 예외가 호출부로 전파된다.
- [ ] 항목을 지우면 그 항목의 사진 파일도 지워진다.
- [ ] 수정하며 사진을 바꾸면 새 파일이 생기고 이전 파일은 지워진다.
- [ ] 수정하며 사진을 바꾸지 않으면 파일이 그대로 유지된다.
- [ ] `observeFirstImageInRange`가 날짜마다 첫 항목의 사진 경로만 내고, 기록이 없는 날짜는 결과에서 뺀다.

**기록 조회 화면**

- [ ] 오늘 날짜로 진입하면 항목 추가 버튼과 각 항목의 삭제 버튼이 보인다.
- [ ] 그저께 이전 날짜로 진입하면 추가·삭제·수정 진입점이 보이지 않는다.
- [ ] 항목이 사진과 함께 `첫째 끼니`, `둘째 끼니` 순으로 제목이 붙어 나온다.
- [ ] 텍스트가 없는 항목은 텍스트 자리가 비어 있고 사진만 보인다.
- [ ] 항목을 추가하거나 지우면 목록이 즉시 갱신된다.
- [ ] 기록이 없는 날에 빈 상태 문구가 보인다.
- [ ] 조회가 실패하면 에러 문구와 재시도 버튼이 보인다.
- [ ] 삭제가 실패하면 안내가 뜨고 목록은 그대로 남는다.

**항목 편집 화면**

- [ ] 사진을 고르기 전에는 저장 버튼이 비활성이다.
- [ ] 사진을 고르면 화면에 미리 보인다.
- [ ] 텍스트는 비워 두어도 저장된다.
- [ ] 수정 진입이면 저장된 사진과 텍스트가 채워진 상태로 열린다.
- [ ] 수정 진입에서 사진을 바꾸지 않고 저장하면 사진이 그대로 유지된다.
- [ ] 저장이 실패하면 화면이 닫히지 않고 실패 문구가 뜬다.
- [ ] 저장에 성공하면 저장 중 표시가 풀린다.
- [ ] 사진 선택을 취소하면 이전 선택이 유지되고 아무 일도 일어나지 않는다.

**캘린더 화면**

- [ ] 기록이 있는 날짜 셀에 그날 첫 항목의 사진 썸네일이 보인다.
- [ ] 기록이 없는 날짜 셀은 비어 있다.
- [ ] 이전·다음 달 버튼이 표시 월을 바꾸고 그 달을 다시 읽는다.
- [ ] 날짜를 누르면 그 날짜의 기록 화면으로 이동한다.
- [ ] 조회가 실패하면 에러 문구와 재시도 버튼이 보인다.

### 비목표

- 끼니 구분, 시간대 분류, 열량·영양소 입력과 집계.
- 사진 여러 장 첨부, 카메라 촬영, 사진 편집·회전.
- 항목 순서를 사용자가 바꾸는 것. 생성 시각 순으로 고정한다.
- 기록의 날짜 이동, 다른 날짜로 복사.
- 사진 원본 크기 축소·압축. 고른 파일을 그대로 복사한다.
- 지난 사진 파일을 청소하는 별도 작업. 삭제 시점에만 지운다.
- 운동 일지 쪽 코드 정리. 물려받은 것을 그대로 쓴다.
- 홈 화면 개편. 진입 버튼 한 개만 더한다.

### 데이터 계약

#### 테이블 `diet_entry`

| 컬럼 | 타입 | 도메인 매핑 | 귀착지 |
|---|---|---|---|
| `id` | `Long` PK autoGenerate | `DietEntry.id` | 수정·삭제 대상 지정 |
| `dateEpochDay` | `Long` (인덱스) | `DietLog.date` (`LocalDate.toEpochDay()` ↔ `LocalDate.ofEpochDay`) | 날짜별 조회 키, 캘린더 맵의 키 |
| `imageFileName` | `String` | `DietEntry.imagePath`로 풀어서 낸다 | 목록 행과 캘린더 셀의 사진. **저장은 파일명만** 한다 |
| `memo` | `String?` | `DietEntry.memo` | 목록 행의 텍스트 |
| `createdAtMillis` | `Long` | 도메인 미노출 | **미사용**. 같은 날 항목의 정렬 키다 |

`imageFileName`에 절대 경로를 담지 않는다. 앱 내부 저장소의 절대 경로는 재설치·백업 복원으로 바뀔 수 있어, 그 경로를 적어 두면 사진을 잃는다. 매핑이 `filesDir`와 합쳐 `DietEntry.imagePath`를 만든다.

`BodyPlanDatabase`의 `entities`에 `DietEntryEntity`를 더하고 `version`을 2로 올린다. 파괴적 마이그레이션이 켜져 있어 마이그레이션 코드는 쓰지 않는다.

#### DAO 쿼리

| DAO | 함수 | 쿼리 |
|---|---|---|
| `DietEntryDao` | `observeByDate(epochDay: Long): Flow<List<DietEntryEntity>>` | `WHERE dateEpochDay = :epochDay ORDER BY createdAtMillis ASC, id ASC` |
| `DietEntryDao` | `observeFirstImageInRange(from: Long, to: Long): Flow<List<DateImage>>` | 날짜마다 `createdAtMillis`가 가장 이른 행 하나. 아래 쿼리 |
| `DietEntryDao` | `getById(id: Long): DietEntryEntity?` | `WHERE id = :id` |
| `DietEntryDao` | `insert(entity: DietEntryEntity): Long` | `@Insert` |
| `DietEntryDao` | `update(entity: DietEntryEntity)` | `@Update` |
| `DietEntryDao` | `deleteById(id: Long)` | `DELETE FROM diet_entry WHERE id = :id` |

```sql
SELECT dateEpochDay, imageFileName FROM diet_entry AS e
WHERE e.dateEpochDay BETWEEN :from AND :to
  AND e.createdAtMillis = (
    SELECT MIN(createdAtMillis) FROM diet_entry AS inner_entry
    WHERE inner_entry.dateEpochDay = e.dateEpochDay
  )
GROUP BY e.dateEpochDay
```

`GROUP BY`는 같은 시각에 들어온 항목이 둘일 때 한 행만 남기기 위한 것이다.

`DateImage`는 `dateEpochDay: Long`과 `imageFileName: String` 두 컬럼만 담은 조회 전용 data class다. `core:local`에 둔다. 캘린더가 하루에 한 행만 읽게 해, 항목이 많은 날에도 목록 전체를 끌어오지 않는다.

#### 사진 보관

`core:data`에 `DietImageStore`를 둔다. `core:domain`은 이 타입을 모른다 — 안드로이드의 `Uri`와 파일 시스템을 다루기 때문이다.

```kotlin
internal interface DietImageStore {
    fun save(sourceUri: String): String

    fun delete(fileName: String)

    fun pathOf(fileName: String): String
}
```

인터페이스로 두는 것은 Repository 테스트가 파일 시스템을 타지 않게 하기 위해서다. 구현 `DietImageStoreImpl`은 `@ApplicationContext`를 받아 `DataModule`이 바인딩한다.

- `save`는 `context.contentResolver`로 연 스트림을 `context.filesDir/diet_images/<UUID>.jpg`로 복사하고 파일명을 낸다. 실패하면 예외를 던지고, 만들다 만 파일은 지운다.
- `delete`는 파일이 없어도 조용히 지나간다. 삭제 실패로 기록 삭제를 막지 않는다.
- `pathOf`는 절대 경로를 만든다. 매핑이 쓴다.

디렉터리는 `filesDir` 아래에 둔다. `cacheDir`는 시스템이 지울 수 있어 사진이 사라진다.

#### 도메인 타입

```kotlin
data class DietEntry(
    val id: Long,
    /** 화면이 바로 그릴 수 있는 절대 경로. 저장소에는 파일명만 남는다. */
    val imagePath: String,
    val memo: String?,
)

data class DietLog(
    val date: LocalDate,
    val entries: List<DietEntry>,
)
```

#### Repository 시그니처

```kotlin
interface DietLogRepository {
    fun observeLog(date: LocalDate): Flow<DietLog>

    /** 날짜마다 첫 항목의 사진 절대 경로. 기록이 없는 날짜는 담지 않는다. */
    fun observeFirstImageInRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, String>>

    suspend fun getEntry(id: Long): DietEntry?

    suspend fun addEntry(date: LocalDate, sourceImageUri: String, memo: String?): Long

    /** [sourceImageUri]가 null이면 사진을 그대로 두고 텍스트만 고친다. */
    suspend fun updateEntry(entryId: Long, sourceImageUri: String?, memo: String?)

    suspend fun deleteEntry(id: Long)
}
```

`sourceImageUri`는 Photo Picker가 준 `Uri`를 문자열로 넘긴 것이다. 도메인이 안드로이드 타입을 알지 않으면서도 출처를 전달하기 위한 것이며, 파일로 옮기는 일은 `core:data`가 한다.

**실패 전파**: Repository는 예외를 잡지 않는다. 사진 복사가 실패하면 행을 넣기 전에 던진다. 수정에서 새 사진 복사가 실패하면 이전 파일을 지우지 않고 던진다.

### 상태 계약

#### DietCalendar

| State 필드 | 타입 | 초기값 |
|---|---|---|
| `yearMonth` | `YearMonth` | `YearMonth.now(clock)` |
| `today` | `LocalDate` | `LocalDate.now(clock)`. `BodyPlanCalendar`의 `selectedDate`로 넘겨 오늘 셀을 강조한다 |
| `imagesByDate` | `Map<LocalDate, String>` | `emptyMap()` |
| `isLoading` | `Boolean` | `false` |
| `errorMessage` | `String?` | `null` |

`Intent`: `ChangeMonth(yearMonth: YearMonth)` / `ClickDate(date: LocalDate)` / `ClickRetry`

`Effect`: `NavigateToLog(date: LocalDate)`

조회 실패 시 `isLoading = false`, `errorMessage`를 채우고 `imagesByDate`는 직전 값을 유지한다. 실패는 Effect로 나가지 않는다.

#### DietLog

기존 골격의 `DietLogState`·`Intent`·`Effect`를 이 내용으로 바꾼다.

| State 필드 | 타입 | 초기값 |
|---|---|---|
| `date` | `LocalDate` | NavKey의 `dateEpochDay`에서 만든 값. 기본값 없음 |
| `entries` | `List<DietEntry>` | `emptyList()` |
| `isEditable` | `Boolean` | `false` |
| `isLoading` | `Boolean` | `false` |
| `errorMessage` | `String?` | `null` |

`Intent`: `ClickAddEntry` / `ClickEntry(id: Long)` / `ClickDeleteEntry(id: Long)` / `ClickBack` / `ClickRetry`

`Effect`: `NavigateToEntryEdit(date: LocalDate, entryId: Long?)` / `GoBack` / `ShowMessage(message: String)`

`isEditable`은 `initializeData()`에서 `IsEditableDateUseCase`로 한 번만 정한다. 삭제 실패는 `ShowMessage`로 알리고 목록은 그대로 둔다.

#### DietEntryEdit

| State 필드 | 타입 | 초기값 |
|---|---|---|
| `date` | `LocalDate` | NavKey의 `dateEpochDay`에서 만든 값. 기본값 없음 |
| `editingEntryId` | `Long?` | NavKey의 `entryId`. 신규면 `null` |
| `storedImagePath` | `String?` | `null`. 수정 진입에서 채운다 |
| `pickedImageUri` | `String?` | `null`. 이번에 새로 고른 사진 |
| `memo` | `String` | `""` |
| `isLoading` | `Boolean` | `false` |
| `isSaving` | `Boolean` | `false` |
| `errorMessage` | `String?` | `null` |

파생 값은 State의 계산 프로퍼티로 둔다.

| 프로퍼티 | 정의 |
|---|---|
| `previewImage` | `pickedImageUri ?: storedImagePath` |
| `canSave` | `previewImage != null && !isSaving` |

`Intent`: `PickImage(uri: String)` / `ChangeMemo(value: String)` / `ClickSave` / `ClickBack` / `ClickRetry`

`Effect`: `GoBack` / `ShowMessage(message: String)`

사진 선택 취소는 화면이 `PickImage`를 보내지 않는 것으로 처리한다. Photo Picker가 `null`을 주면 아무 Intent도 올라가지 않는다.

수정 진입이면 `initializeData()`가 `getEntry`로 `storedImagePath`와 `memo`를 채운다. 항목이 없으면 `errorMessage`를 세우고 화면에 남는다.

저장은 `editingEntryId`가 `null`이면 `addEntry(date, pickedImageUri!!, memo)`, 아니면 `updateEntry(entryId, pickedImageUri, memo)`를 부른다. 수정에서 `pickedImageUri`가 `null`이면 사진은 그대로 둔다는 뜻이다. `memo`는 공백만 있으면 `null`로 넘긴다. 성공하면 `isSaving`을 되돌리고 `GoBack`, 실패하면 `isSaving = false`와 `ShowMessage`로 끝나며 화면을 닫지 않는다.

### 화면 구성

#### 재사용 판정

시그니처는 대상 파일을 열어 확인했다.

| 심볼 | 파일 | 판정 | 시그니처 |
|---|---|---|---|
| `BodyPlanCalendar` | `core/designsystem/.../component/BodyPlanCalendar.kt` | 재사용 | `BodyPlanCalendar(yearMonth: YearMonth, selectedDate: LocalDate?, onSelectDate: (LocalDate) -> Unit, onChangeMonth: (YearMonth) -> Unit, modifier: Modifier = Modifier, dayContent: @Composable (LocalDate) -> Unit = {})` |
| `BaseImage` | `core/designsystem/.../component/BaseImage.kt` | 재사용 | 파일 오버로드 `BaseImage(file: File, contentDescription: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop, placeholder: Painter? = null, error: Painter? = placeholder)` |
| `BaseText` | `core/designsystem/.../component/BaseText.kt` | 재사용 | `BaseText(text: String, modifier: Modifier = Modifier, color: Color = BaseTextDefaults.color, style: TextStyle = BaseTextDefaults.style, textAlign: TextAlign? = null, maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip)` |
| `BaseTextField` | `core/designsystem/.../component/BaseTextField.kt` | 재사용 | `BaseTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, placeholder: String? = null, textStyle: TextStyle = BaseTextDefaults.style, keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default, singleLine: Boolean = false, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE)` |
| `VerticalSpacer` / `WeightSpacer` | `core/designsystem/.../component/Spacer.kt` | 재사용 | `VerticalSpacer(space: Dp)`, `RowScope.WeightSpacer(weight: Float = 1f)`, `ColumnScope.WeightSpacer(weight: Float = 1f)` |
| `CollectEffect` | `core/ui/coordinator/.../CollectEffect.kt` | 재사용 | `CollectEffect(effect: Flow<E>, onEffect: (E) -> Unit)` |
| `OptionChipRow` | `core/designsystem/.../component/OptionChipRow.kt` | 미사용 | 이 기능에 숫자 선택지가 없다 |
| `BodyPartTabRow` / `DayStatusIndicator` | `core/ui/components/.../BodyPartUi.kt` | 미사용 | 운동 부위 전용이다 |

사진은 `BaseImage`의 `File` 오버로드로 그린다. 저장된 사진은 앱 내부 파일이라 `File(imagePath)`로 만들고, 아직 저장 전인 선택 사진은 `Uri` 문자열이므로 URL 오버로드에 그대로 넘긴다 — Coil이 `content://`를 처리한다.

#### 신규 공용 컴포넌트

없다. 사진 한 장을 그리는 일은 `BaseImage`로 충분하고, 이 기능에서만 쓰는 항목 카드는 `feature:dietlog:impl`에 둔다.

#### 화면별 트리와 상태

**DietCalendarViewImpl** — `Column`. `BodyPlanCalendar` 하나만 둔다. `dayContent`는 `imagesByDate[date]`가 있으면 `BaseImage`로 작은 썸네일을, 없으면 아무것도 그리지 않는다.

| 상태 | 표시 |
|---|---|
| 로딩 | 캘린더 위에 얇은 진행 표시 |
| 빈 | 해당 없음. 기록이 없어도 달력을 그린다 |
| 에러 | 캘린더 대신 에러 문구와 재시도 버튼 |

**DietLogViewImpl** — `Column`. 상단 바(뒤로, `yyyy년 M월 d일`, `isEditable`이면 추가 버튼), 아래 `LazyColumn` 카드 목록. 카드는 사진, 순번 제목, 텍스트, `isEditable`이면 삭제 버튼을 담고 카드 전체가 수정 진입점이다.

| 상태 | 표시 |
|---|---|
| 로딩 | 목록 자리에 진행 표시 |
| 빈 | `기록이 없습니다` |
| 에러 | 에러 문구와 재시도 버튼 |

**DietEntryEditViewImpl** — `Column`. 상단 바(뒤로, 제목), 사진 자리(고른 사진이 있으면 미리 보기, 없으면 안내 문구), 사진 고르기 버튼, 텍스트 입력, 하단 저장 버튼.

| 상태 | 표시 |
|---|---|
| 로딩 | 본문 자리에 진행 표시 |
| 빈 | 사진 미선택이면 사진 자리에 `사진을 선택하세요` |
| 에러 | 에러 문구와 재시도 버튼 |

#### 카피 원문

| 위치 | 문구 |
|---|---|
| 홈 진입 버튼 | `식단 일지` |
| 기록 화면 제목 | `yyyy년 M월 d일` |
| 기록 화면 추가 버튼 | `식단 추가` |
| 기록 화면 빈 상태 | `기록이 없습니다` |
| 항목 제목 | `첫째 끼니` `둘째 끼니` `셋째 끼니` `넷째 끼니` `다섯째 끼니` `여섯째 끼니` `일곱째 끼니` `여덟째 끼니` `아홉째 끼니` `열째 끼니`, 그 뒤는 `%d번째 끼니` |
| 편집 화면 제목 (신규) | `식단 추가` |
| 편집 화면 제목 (수정) | `식단 수정` |
| 사진 미선택 안내 | `사진을 선택하세요` |
| 사진 고르기 버튼 | `사진 선택` / 사진이 있으면 `사진 변경` |
| 텍스트 placeholder | `무엇을 먹었는지 적어 주세요` |
| 편집 화면 저장 버튼 | `저장` |
| 뒤로 버튼 | `뒤로` |
| 삭제 버튼 | `삭제` |
| 조회 실패 | `불러오지 못했습니다` |
| 재시도 버튼 | `다시 시도` |
| 저장 실패 | `저장하지 못했습니다` |
| 삭제 실패 | `삭제하지 못했습니다` |

### 네비게이션

`:feature:dietlog:api`에 둔다. 기존 `DietLogNavKey`는 인자 없는 `data object`인데, 날짜를 받아야 하므로 `data class`로 바꾼다. 운동 일지와 같은 규약으로 epochDay를 담는다.

```kotlin
@Serializable
data object DietCalendarNavKey : BodyPlanNavKey()

@Serializable
data class DietLogNavKey(val dateEpochDay: Long) : BodyPlanNavKey()

@Serializable
data class DietEntryEditNavKey(val dateEpochDay: Long, val entryId: Long? = null) : BodyPlanNavKey()

fun BodyPlanNavigator.navigateToDietCalendar()
fun BodyPlanNavigator.navigateToDietLog(date: LocalDate)
fun BodyPlanNavigator.navigateToDietEntryEdit(date: LocalDate, entryId: Long? = null)
```

| 경로 | 진입 | 복귀 |
|---|---|---|
| 홈 → 캘린더 | `HomeView`의 `식단 일지` 버튼 | 시스템 뒤로 |
| 캘린더 → 기록 | 날짜 셀 클릭 | 상단 뒤로 버튼 |
| 기록 → 항목 편집 | `식단 추가` 버튼(신규), 카드 클릭(수정) | 저장 성공 또는 뒤로 |

`DietLogViewModel`과 `DietEntryEditViewModel`은 NavKey 인자를 받으므로 assisted factory로 만든다. `DietCalendarViewModel`은 `@Inject`만 쓴다.

### 버린 안

| 안 | 버린 이유 |
|---|---|
| 사진 URI를 그대로 데이터베이스에 저장 | 원본이 지워지거나 권한이 회수되면 사진을 잃는다. 상대 세션과 합의해 내부 복사로 정했다 |
| 사진 절대 경로를 저장 | 재설치·백업 복원으로 경로가 바뀐다. 파일명만 저장하고 매핑이 풀어 준다 |
| 사진을 `cacheDir`에 보관 | 시스템이 지울 수 있다 |
| 카메라 촬영 지원 | 요청에 없고 권한 처리가 따라붙는다. 상대 세션이 뺐다 |
| 끼니(아침·점심·저녁) 분류 | 하루 끼니 수가 정해져 있지 않다. 상대 세션이 순번 표기로 정했다 |
| 항목당 사진 여러 장 | 요청은 한 장이다 |
| 캘린더 셀에 항목 개수 점 | 상대 세션이 썸네일로 정했다 |
| 날짜별 조회로 캘린더를 채우기 | 한 달치 항목을 전부 읽게 된다. 날짜마다 첫 행만 읽는 쿼리를 둔다 |
| 사진 삭제 실패 시 기록 삭제를 막기 | 파일이 이미 없을 수 있다. 파일 삭제 실패로 기록을 남기지 않는다 |

## 실행

### 파일별 작업

#### 단위 1 — 도메인·저장소

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/domain/.../model/DietEntry.kt` | 신규 | `DietEntry` data class |
| `core/domain/.../model/DietLog.kt` | 신규 | `DietLog` data class |
| `core/domain/.../repository/DietLogRepository.kt` | 신규 | 인터페이스 |
| `core/local/.../entity/DietEntryEntity.kt` | 신규 | Entity. `dateEpochDay` 인덱스 |
| `core/local/.../entity/DateImage.kt` | 신규 | 캘린더 조회 전용 POJO |
| `core/local/.../dao/DietEntryDao.kt` | 신규 | DAO |
| `core/local/.../BodyPlanDatabase.kt` | 수정 | `entities`에 추가, `version = 2` |
| `core/local/.../di/LocalModule.kt` | 수정 | `DietEntryDao` provides 추가 |
| `core/data/.../image/DietImageStore.kt` | 신규 | 인터페이스와 `DietImageStoreImpl`. 사진 복사·삭제·경로 조립 |
| `core/data/.../mapper/DietMapper.kt` | 신규 | Entity ↔ 도메인 매핑 |
| `core/data/.../repository/DietLogRepositoryImpl.kt` | 신규 | 사진 복사와 행 저장을 묶는다 |
| `core/data/.../di/DataModule.kt` | 수정 | `DietLogRepository` 바인딩 추가 |

#### 단위 2 — 기록 화면과 항목 편집 화면

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `feature/dietlog/api/.../DietLogNavKey.kt` | 수정 | NavKey 3종과 navigate 확장 3종으로 바꾼다 |
| `feature/dietlog/impl/.../DietLogContracts.kt` | 수정 | 골격을 실제 계약으로 바꾼다 |
| `feature/dietlog/impl/.../DietLogViewModel.kt` | 수정 | assisted factory, 목록 구독, 삭제 |
| `feature/dietlog/impl/.../DietLogView.kt` | 수정 | 사진 카드 목록 |
| `feature/dietlog/impl/.../DietEntryEditContracts.kt` | 신규 | State·Intent·Effect |
| `feature/dietlog/impl/.../DietEntryEditViewModel.kt` | 신규 | assisted factory, 복원, 저장 |
| `feature/dietlog/impl/.../DietEntryEditView.kt` | 신규 | Photo Picker 연동 |
| `feature/dietlog/impl/.../DietLogNavGraph.kt` | 수정 | entry 두 개 등록 |
| `feature/dietlog/impl/.../MealTitle.kt` | 신규 | 순번을 `첫째 끼니` 문구로 바꾼다 |

#### 단위 3 — 캘린더 화면

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `feature/dietlog/impl/.../DietCalendarContracts.kt` | 신규 | State·Intent·Effect |
| `feature/dietlog/impl/.../DietCalendarViewModel.kt` | 신규 | 월 범위 구독, 월 이동 |
| `feature/dietlog/impl/.../DietCalendarView.kt` | 신규 | 썸네일 셀 |
| `feature/dietlog/impl/.../DietLogNavGraph.kt` | 수정 | 캘린더 entry 추가 |
| `feature/home/impl/**` | 수정 후 제거 | 이 단위에서 `식단 일지` 버튼을 더했으나, 뒤이은 하단 탭 도입으로 홈 화면 자체가 없어졌다 |

### 구현 순서

단위마다 검증을 통과한 뒤 커밋한다.

**단위 1 — 도메인·저장소**

- 쓰는 것: `BodyPlanDatabase`, `LocalModule`, `DataModule`, `Clock` (운동 일지가 만든 것)
- 만드는 것: `DietEntry`, `DietLog`, `DietLogRepository`, `DietEntryEntity`, `DateImage`, `DietEntryDao`, `DietImageStore`·`DietImageStoreImpl`, `DietMapper`, `DietLogRepositoryImpl`
- 검증: `./gradlew :app:compileDebugKotlin :core:data:testDebugUnitTest`
- 사진 저장은 파일 시스템을 타므로 `DietImageStore`를 가짜로 두고 Repository만 검증한다. 실제 복사는 계측 테스트 영역이며 이번에 만들지 않는다

**단위 2 — 기록 화면과 항목 편집 화면**

- 쓰는 것: 단위 1의 `DietLogRepository`·`DietEntry`, `IsEditableDateUseCase`
- 만드는 것: NavKey 3종과 navigate 확장 3종, `DietLogContracts`·`ViewModel`·`View`, `DietEntryEditContracts`·`ViewModel`·`View`, `MealTitle`, navGraph의 entry 두 개
- 이 단위가 NavKey 3종을 전부 만든다. 단위 3이 캘린더 키를 쓴다
- 검증: `./gradlew :app:assembleDebug :feature:dietlog:impl:testDebugUnitTest` + Preview 렌더

**단위 3 — 캘린더 화면**

- 쓰는 것: 단위 1의 `observeFirstImageInRange`, 단위 2의 `DietCalendarNavKey`·`DietLogNavKey`, `BodyPlanCalendar`
- 만드는 것: `DietCalendarContracts`·`ViewModel`·`View`, `HomeView`의 진입 버튼
- 검증: `./gradlew :app:assembleDebug :feature:dietlog:impl:testDebugUnitTest` + Preview 렌더

ViewModel 테스트는 `Dispatchers.setMain`에 즉시 실행 디스패처를 넣고, `uiState`와 `effect` 수집기를 그 디스패처에서 돌린다. 운동 일지의 `MainDispatcherRule`과 같은 규칙이며, 큐에 쌓는 디스패처로는 Effect가 한 번의 진행으로 도달하지 않는다.

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| `BodyPlanDatabase`의 `entities`와 `version` | 운동 일지 전체. `MIGRATION_1_2`가 식단 테이블만 더하므로 기존 운동 기록은 남는다 |
| `core/local/.../di/LocalModule.kt` | 운동 일지의 DAO 주입 |
| `core/data/.../di/DataModule.kt` | 운동 일지의 Repository 바인딩 |
| `DietLogNavKey`를 `data object`에서 `data class`로 | `dietLogNavGraph`와 `BodyPlanNavDisplay`. 인자 없는 `navigateToDietLog()` 호출부가 남아 있는지 확인한다 |
| `feature/home/impl`의 `HomeView`·`HomeNavGraph` | 홈 화면. 운동 일지 버튼이 그대로 있는지, `HomeViewImplPreview`가 렌더되는지 |
