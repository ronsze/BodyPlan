# 백업 파일 내보내기·가져오기

**작성일**: 2026-09-13

## 설계

### 요약

마이페이지에 `백업` 화면을 두고, 기록 전부(DB 11개 표)와 사진(식단·인바디)을 zip 파일 하나로 내보내고 그 파일에서 되돌린다.
저장 위치는 Android 파일 선택창(SAF)에서 사용자가 고른다 — Google Drive 폴더를 고르면 앱을 지워도 파일이 남는다. 백엔드·로그인·외부 SDK는 쓰지 않는다.
AI 토큰(DataStore)은 파일에 담지 않는다 — 평문 토큰이 기기 밖으로 나가지 않게 하려는 기존 백업 규칙(`backup_rules.xml`)과 같은 이유다.

### 배경

요청은 "앱 삭제 후 재설치해도 데이터 유지"다. 방향 합의에서 사용자가 다음을 정했다.

- Firebase·Google Drive API 같은 설정이 필요한 안은 버리고, **설정 없이 되는 파일 내보내기/가져오기(안 A)** 로 간다. 혼자 쓰는 앱이라 자동 동기화의 이득보다 콘솔 설정 비용이 크다.
- Drive는 파일 선택창의 제공자로만 쓴다.

기록 형식은 SQLite 파일 복사가 아니라 **표별 JSON**이다. DB 파일을 통째로 바꾸려면 Hilt 싱글턴인 `BodyPlanDatabase`를 닫고 다시 열어야 해 프로세스 재시작이 필요하지만, JSON은 한 트랜잭션으로 지우고 넣으면 화면의 `Flow`가 알아서 갱신된다.

### 수용 조건

**마이 탭(기존)**
- [ ] `백업` 행(설명 `파일로 내보내기·가져오기`)이 보이고 누르면 백업 화면이 열린다.

**백업 화면 (`BackupNavKey`)**
- [ ] `백업 파일 내보내기`를 누르면 파일 만들기 선택창이 `bodyplan-backup-yyyyMMdd-HHmm.zip` 이름으로 열린다. 위치를 고르면 zip이 써지고 `백업 파일을 저장했습니다` 토스트가 뜬다.
- [ ] 내보낸 zip에는 `snapshot.json`과 `images/diet_images/*`, `images/inbody_images/*`가 들어 있다. `snapshot.json`은 11개 표의 행 전부를 담고 `dbVersion`은 그 시점 Room 버전이다. 기록이 가리키지만 파일이 없는 사진은 건너뛴다.
- [ ] 선택창을 취소하면 아무 일도 없다.
- [ ] 내보내기 실패(스트림을 열 수 없음·쓰기 실패)면 `백업 파일을 저장하지 못했습니다` 토스트.
- [ ] `백업 파일 가져오기`를 누르면 파일 열기 선택창이 뜨고, 파일을 고르면 `가져오면 이 기기의 기록이 백업 파일 내용으로 모두 바뀝니다. 계속할까요?` 대화상자가 뜬다. `취소`면 닫히고 아무 일도 없다.
- [ ] `가져오기`를 누르면 11개 표가 비워지고 파일의 행이 원래 id 그대로 들어가며(한 트랜잭션), 사진 디렉터리 두 개가 파일의 사진으로 바뀐다. 끝나면 `백업을 복원했습니다` 토스트. 마이 탭으로 돌아가면 프로필·기록이 바뀌어 있다.
- [ ] 트랜잭션이 실패하면 표는 이전 상태 그대로다(전부 아니면 전무).
- [ ] 파일이 zip이 아니거나 `snapshot.json`이 없거나 JSON이 깨졌으면 `백업 파일이 아니거나 손상됐습니다` 토스트, 기록은 그대로.
- [ ] `dbVersion`이 현재 Room 버전과 다르면 `이 앱 버전에서 지원하지 않는 백업 파일입니다` 토스트, 기록은 그대로.
- [ ] 그 밖의 실패면 `복원하지 못했습니다` 토스트.
- [ ] 내보내기·가져오기가 도는 동안 버튼 자리에 로딩 표시가 보이고 버튼은 보이지 않는다.
- [ ] 빈 결과: 기록이 하나도 없어도 내보내기는 성공한다(빈 배열).
- [ ] 권한: 해당 없음 — SAF는 저장소 권한을 요구하지 않는다.

### 비목표

- 자동 백업(변경 감지·주기 업로드), 계정 로그인, 클라우드 API 연동.
- AI 토큰·온보딩 완료 여부(DataStore) 포함. 재설치 후에는 온보딩을 다시 거치고 토큰을 다시 넣는다.
- 다른 Room 버전의 백업을 변환해 들이는 것. 스키마가 바뀌면 이전 백업은 거부된다 — 앱을 올리기 전에 새로 내보내야 한다. 필요해지면 임시 DB에 넣고 마이그레이션을 태우는 방식으로 이 계획 뒤에 한다.
- 백업 파일 암호화.
- 기존 자동 백업 규칙(`backup_rules.xml`·`data_extraction_rules.xml`) 변경.
- `OutlinedActionButton`에 `enabled` 추가 등 designsystem 변경. 로딩 중에는 버튼을 숨기는 것으로 대신한다.
- 각 화면에 사본으로 있는 `LoadingContent` 공용화.

### 데이터 계약

**스냅샷 (`core/local/.../snapshot/BodyPlanSnapshot.kt`, 신규, `@Serializable`)**

| 필드 | 타입 | 귀착지 |
|---|---|---|
| `formatVersion` | `Int` (= `1`) | 파일 형식 판별. 지금은 검사만 하고 분기 없음 |
| `dbVersion` | `Int` | 가져오기 때 현재 Room 버전과 비교 → 다르면 `UnsupportedBackupVersionException` |
| `exportedAtMillis` | `Long` | 미사용(파일 안 기록용) |
| `exercises` | `List<ExerciseEntity>` | `exercise` 표 |
| `workoutEntries` / `workoutSets` | `List<WorkoutEntryEntity>` / `List<WorkoutSetEntity>` | `workout_entry` / `workout_set` |
| `dietEntries` | `List<DietEntryEntity>` | `diet_entry`. `imageFileName`이 사진 목록의 근거 |
| `userProfiles` | `List<UserProfileEntity>` | `user_profile` (0 또는 1행) |
| `analysisResults` | `List<AnalysisResultEntity>` | `analysis_result`. `imageFileName`(null 아님)이 인바디 사진 목록의 근거 |
| `weightRecords` | `List<WeightRecordEntity>` | `weight_record` |
| `workoutMemos` | `List<WorkoutMemoEntity>` | `workout_memo` |
| `routines` / `routineEntries` / `routineSets` | `List<RoutineEntity>` / `List<RoutineEntryEntity>` / `List<RoutineSetEntity>` | `routine` / `routine_entry` / `routine_set` |

Room Entity 11개에 `@Serializable`을 붙인다 — 스냅샷은 스키마의 사본이고 `dbVersion`으로 묶이므로 별도 DTO를 두지 않는다. `core:local`에 `kotlin.serialization` 플러그인과 `libs.kotlinx.serialization.json`을 더한다. JSON 인코딩·디코딩은 `core:data`의 `Json`(이미 `core:network`가 제공, `ignoreUnknownKeys = true`)이 한다.

**`SnapshotStore` (`core/local/.../snapshot/SnapshotStore.kt`, 신규, `@Singleton`, `BodyPlanDatabase` 주입)**

- `suspend fun export(exportedAtMillis: Long): BodyPlanSnapshot` — `database.withTransaction { }` 안에서 8개 DAO의 `getAll*`을 읽는다. `dbVersion = database.openHelper.readableDatabase.version`. 시각은 호출부(`Clock`을 가진 Repository)가 넘긴다.
- `suspend fun import(snapshot: BodyPlanSnapshot)` — `snapshot.dbVersion != 현재 버전`이면 `UnsupportedBackupVersionException(snapshot.dbVersion)`을 던진다. `withTransaction` 안에서 부모 표 8개를 `deleteAll`(자식은 CASCADE)한 뒤 부모 → 자식 순으로 `insertAll`. id는 스냅샷 값 그대로 들어간다(`@Insert`는 0이 아닌 id를 그대로 쓴다).

**DAO 추가** (전부 `suspend`)

| DAO | 추가 |
|---|---|
| `ExerciseDao` | `getAll(): List<ExerciseEntity>`, `insertAll(list)`, `deleteAll()` |
| `WorkoutEntryDao` | `getAll(): List<WorkoutEntryEntity>`, `getAllSets(): List<WorkoutSetEntity>`, `insertAll(list)`, `deleteAll()` (`insertSets`는 기존) |
| `DietEntryDao` | `getAll()`, `insertAll(list)`, `deleteAll()` |
| `UserProfileDao` | `getAll()`, `insertAll(list)`, `deleteAll()` |
| `AnalysisResultDao` | `getAll()`, `insertAll(list)`, `deleteAll()` |
| `WeightRecordDao` | `getAll()`, `insertAll(list)`, `deleteAll()` |
| `WorkoutMemoDao` | `getAll()`, `insertAll(list)`, `deleteAll()` |
| `RoutineDao` | `getAll(): List<RoutineEntity>`, `getAllEntries()`, `getAllSets()`, `insertAll(list)`, `insertEntries(list)`, `deleteAll()` (`insertSets`는 기존) |

`getAll`은 `ORDER BY` 없이 읽는다 — 순서는 id가 보존하므로 의미가 없다. `SnapshotStore`가 유일한 호출부라 트랜잭션 묶음은 DAO가 아니라 `SnapshotStore`가 `withTransaction`으로 한다(여러 DAO에 걸치므로 한 DAO의 기본 구현으로 묶을 수 없다).

**zip 형식 (`core/data/.../backup/BackupArchive.kt`, 신규, `internal object`)**

- `snapshot.json` — `BodyPlanSnapshot`의 JSON.
- `images/diet_images/<fileName>`, `images/inbody_images/<fileName>` — 디렉터리 이름은 `DIET_IMAGE_DIRECTORY`·`INBODY_IMAGE_DIRECTORY` 상수 그대로.
- `fun write(output: OutputStream, snapshotJson: String, images: List<ImageEntry>)` — `ImageEntry(entryName: String, file: File)`. `java.util.zip.ZipOutputStream`.
- `fun read(input: InputStream, extractDir: File): ReadResult` — `ReadResult(snapshotJson: String, imageDirs: Map<String, File>)`. `images/<dir>/<name>`은 `extractDir/<dir>/<name>`으로 푼다. `snapshot.json`이 없으면 `InvalidBackupFileException`. `ZipException`도 `InvalidBackupFileException`으로 감싼다. 경로에 `..`가 있는 항목은 버린다.

**`LocalImageStore` 추가** (`core/data/.../image/LocalImageStore.kt`)

- `fun replaceAll(sourceDirectory: File)` — 디렉터리의 기존 파일을 전부 지우고 `sourceDirectory`의 파일을 옮겨 넣는다. `sourceDirectory`가 없으면 비우기만 한다. `FakeDietImageStore`도 구현한다(`replacedFrom: MutableList<File>` 기록).
- 내보내기는 기존 `pathOf(fileName)`으로 `File`을 만들어 쓴다.

**Repository `BackupRepository` (`core/domain/.../repository/BackupRepository.kt`, 신규)** — 실패는 던진다.

```kotlin
/** 기록 전체를 파일 하나로 내보내고 되돌린다. 실패는 삼키지 않고 호출부로 던진다. */
interface BackupRepository {
    /** [destinationUri]에 zip을 쓴다. */
    suspend fun exportTo(destinationUri: String)
    /** [sourceUri]의 zip으로 기록 전체를 바꾼다. 형식이 아니면 [InvalidBackupFileException], 버전이 다르면 [UnsupportedBackupVersionException]. */
    suspend fun importFrom(sourceUri: String)
}
```

**예외 (`core/domain/.../model/BackupFailure.kt`, 신규)** — `AiFailure.kt`와 같은 꼴.

- `class InvalidBackupFileException : Exception("백업 파일이 아니거나 손상됐습니다")`
- `class UnsupportedBackupVersionException(val backupVersion: Int) : Exception("이 앱 버전에서 지원하지 않는 백업 파일입니다")`

**구현 `BackupRepositoryImpl` (`core/data/.../repository/BackupRepositoryImpl.kt`, 신규, `internal`)** — 주입: `@ApplicationContext context`, `SnapshotStore`, `Json`, `@DietImages LocalImageStore`, `@InbodyImages LocalImageStore`, `Clock`.

- `exportTo`: `snapshotStore.export(exportedAtMillis = clock.millis())` → 사진 목록 = `dietEntries.map { imageFileName }` + `analysisResults.mapNotNull { imageFileName }` → 존재하는 파일만 `ImageEntry`로 → `contentResolver.openOutputStream(uri)`(null이면 `error`)에 `BackupArchive.write`. `Dispatchers.IO`.
- `importFrom`: `contentResolver.openInputStream(uri)` → `BackupArchive.read(input, context.cacheDir/backup_import)` → `json.decodeFromString<BodyPlanSnapshot>`(`SerializationException`·`IllegalArgumentException`은 `InvalidBackupFileException`) → `snapshotStore.import(snapshot)` → `dietImageStore.replaceAll(imageDirs["diet_images"])`, `inbodyImageStore.replaceAll(imageDirs["inbody_images"])` → `finally`에서 `backup_import` 삭제. DB 트랜잭션이 사진 교체보다 먼저다 — 트랜잭션이 실패하면 사진도 그대로다.

DI: `DataModule`에 `@Binds @Singleton bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository`. `SnapshotStore`는 `@Singleton @Inject constructor`라 별도 provides 없음.

### 상태 계약

**마이 탭(`home/MyContracts.kt`, 기존 확장)**
- Intent 추가: `ClickBackup`. Effect 추가: `NavigateToBackup`. State 변경 없음.

**백업 화면 (`backup/BackupContracts.kt`, 신규)**

State `BackupState`
- `isExporting: Boolean = false`, `isImporting: Boolean = false` — 둘 중 하나면 버튼 대신 로딩(`val isBusy get() = isExporting || isImporting`).
- `pendingImportUri: String? = null` — 선택한 파일. null이 아니면 확인 대화상자를 띄운다.

Intent `BackupIntent`
- `ClickExport` → `LaunchExportPicker(suggestedFileName)` effect. 파일명은 `clock` 기준 `bodyplan-backup-yyyyMMdd-HHmm.zip`.
- `ExportDestinationPicked(uri: String?)` — null(취소)이면 무시. 아니면 `isExporting = true` → `backupRepository.exportTo(uri)` → 성공 `ShowMessage("백업 파일을 저장했습니다")`, 실패 `ShowMessage("백업 파일을 저장하지 못했습니다")` → `isExporting = false`.
- `ClickImport` → `LaunchImportPicker` effect.
- `ImportSourcePicked(uri: String?)` — null이면 무시. 아니면 `pendingImportUri = uri`.
- `ConfirmImport` — `pendingImportUri`가 null이면 무시. `pendingImportUri = null, isImporting = true` → `importFrom(uri)` → 성공 `ShowMessage("백업을 복원했습니다")`; `InvalidBackupFileException`·`UnsupportedBackupVersionException`은 `ShowMessage(e.message)`; 그 밖은 `ShowMessage("복원하지 못했습니다")` → `isImporting = false`. 재시도는 사용자가 다시 누른다.
- `DismissImport` — `pendingImportUri = null`.
- `ClickBack` → `GoBack`.

Effect `BackupEffect`
- `GoBack`, `LaunchExportPicker(val suggestedFileName: String)`, `LaunchImportPicker`, `ShowMessage(val message: String)`.

ViewModel 주입: `BackupRepository`, `java.time.Clock`(`DataModule.provideClock`이 제공, `WeightViewModel`이 같은 방식으로 받는다).

### 화면 구성

**마이 탭 `MyViewImpl`** — 기존 `SectionRow` 4개 뒤에 `SectionRow(title = "백업", onClick = uiEvents.onClickBackup, description = "파일로 내보내기·가져오기")` 추가. `MyEvents.goToBackup`, `MyUiEvents.onClickBackup` 추가. 재사용: `SectionRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, description: String? = null, leading: (@Composable () -> Unit)? = null)`.

**백업 화면 `BackupViewImpl`**

```
Column(fillMaxSize().background(Background))
  BodyPlanTopBar(title = "백업", onBack = uiEvents.onBackPressed)          — 재사용
  Column(padding(horizontal = 16.dp), spacedBy(12.dp))
    BodyPlanCard                                                           — 재사용 (기본 cornerRadius·contentPadding)
      BaseText("내보내기", titleMedium, TextPrimary)
      VerticalSpacer(8.dp)
      BaseText("운동·식단·체중·인바디·루틴 기록과 사진을 파일 하나로 저장합니다. 저장 위치로 Google Drive를 고르면 앱을 지워도 남습니다. AI 토큰은 담지 않습니다.", bodyMedium, TextSecondary)
      VerticalSpacer(16.dp)
      isBusy ? LoadingContent() : PrimaryButton(text = "백업 파일 내보내기", onClick = uiEvents.onClickExport)
    BodyPlanCard
      BaseText("가져오기", titleMedium, TextPrimary)
      VerticalSpacer(8.dp)
      BaseText("백업 파일의 내용으로 이 기기의 기록을 바꿉니다. 지금 기록은 남지 않습니다.", bodyMedium, TextSecondary)
      VerticalSpacer(16.dp)
      isBusy ? LoadingContent() : OutlinedActionButton(text = "백업 파일 가져오기", onClick = uiEvents.onClickImport)
  if (state.pendingImportUri != null) ImportConfirmDialog(onConfirm = uiEvents.onConfirmImport, onDismiss = uiEvents.onDismissImport)
```

- `LoadingContent()` — `AiTokenView.kt`의 private 사본과 같은 꼴(`Box(fillMaxWidth().padding(vertical = 40.dp), Alignment.Center) { CircularProgressIndicator() }`), 화면 안 private.
- `ImportConfirmDialog` — 화면 안 private. `AiTokenRequiredDialog`와 같은 꼴: `Dialog { BodyPlanCard { BaseText(제목 "백업에서 복원") ; BaseText("가져오면 이 기기의 기록이 백업 파일 내용으로 모두 바뀝니다. 계속할까요?") ; Row { OutlinedActionButton("취소") ; PrimaryButton("가져오기") } } }`.
- 재사용 시그니처: `PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)`, `OutlinedActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)`, `BodyPlanCard(modifier: Modifier = Modifier, cornerRadius: Dp = 16.dp, contentPadding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit)`, `BaseText(text: String, modifier, color: Color = BaseTextDefaults.color, style: TextStyle = BaseTextDefaults.style, ...)`, `VerticalSpacer(space: Dp)`, `BodyPlanTopBar(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, ...)`.
- 화면별 상태: 로딩 — 버튼 자리 `LoadingContent`. 빈 — 해당 없음(기록이 없어도 화면은 같다). 에러 — 토스트뿐, 화면은 그대로. 권한 — 해당 없음.
- 시안 노드: 해당 없음(시안 없음, 기존 마이 하위 화면과 같은 구성).

**`BackupView`** — `InbodyView`의 사진 선택기와 같은 방식으로 `rememberLauncherForActivityResult` 둘을 둔다.
- `createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> viewModel.handleIntent(BackupIntent.ExportDestinationPicked(uri?.toString())) }`
- `openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> viewModel.handleIntent(BackupIntent.ImportSourcePicked(uri?.toString())) }` — launch 인자 `arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")` (Drive가 zip을 octet-stream으로 내는 경우가 있다).
- `CollectEffect`: `GoBack → events.goBack()`, `LaunchExportPicker → createDocument.launch(suggestedFileName)`, `LaunchImportPicker → openDocument.launch(mimeTypes)`, `ShowMessage → Toast.makeText(context, message, LENGTH_SHORT).show()`.
- `BackupUiEvents(onBackPressed, onClickExport, onClickImport, onConfirmImport, onDismissImport)`, `BackupEvents(goBack)`.

### 네비게이션

- `feature/my/api/.../MyNavKey.kt`에 `@Serializable data object BackupNavKey : BodyPlanNavKey()`와 `fun BodyPlanNavigator.navigateToBackup() = navigate(BackupNavKey)` 추가.
- `MyNavGraph.kt`에 `entry<BackupNavKey> { val events = remember { BackupEvents(goBack = navigator::goBack) }; BackupView(events, hiltViewModel()) }`. `MyEvents(goToBackup = navigator::navigateToBackup)` 추가.
- 진입: 마이 탭 `백업` 행. 복귀: 상단 뒤로 → 마이 탭.

### 버린 안

- **Firebase(Auth+Firestore/Storage) 자동 동기화** — 방향 합의에서 처음 골랐다가 사용자가 "혼자 쓰는데 Firebase가 필요한가"로 되물어 버림. 프로젝트 생성·SHA-1·google-services.json 비용이 이득보다 크다.
- **Google Drive API 자동 백업** — Google Cloud OAuth 설정이 Firebase와 같은 수준이라 버림. Drive는 SAF 제공자로만 쓴다.
- **Android 자동 백업(allowBackup) 보강** — 이미 켜져 있으나 Google 계정 백업 설정·25MB 한도·타이밍에 묶여 "반드시 유지"를 보장하지 못한다.
- **SQLite 파일 통째 복사** — 복원 때 Hilt 싱글턴 DB를 닫고 바꿔야 해 프로세스 재시작이 필요하다. 표별 JSON으로 대체.
- **스냅샷 전용 DTO 11개** — Entity 사본에 불과해 `@Serializable`을 Entity에 직접 붙이는 것으로 대체.
- **자식 표까지 명시 `deleteAll`** — 부모 삭제의 CASCADE로 충분해 부모 8개만 지운다.

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/local/build.gradle.kts` | 수정 | `kotlin.serialization` 플러그인, `libs.kotlinx.serialization.json` 추가 |
| `core/local/.../entity/*Entity.kt` (11개) | 수정 | `@Serializable` 추가 |
| `core/local/.../snapshot/BodyPlanSnapshot.kt` | 신규 | 스냅샷 데이터 클래스 |
| `core/local/.../snapshot/SnapshotStore.kt` | 신규 | `export`/`import`, 버전 검사, `withTransaction` |
| `core/local/.../dao/{Exercise,WorkoutEntry,DietEntry,UserProfile,AnalysisResult,WeightRecord,WorkoutMemo,Routine}Dao.kt` | 수정 | `getAll*`/`insertAll*`/`deleteAll` 추가 |
| `core/domain/.../model/BackupFailure.kt` | 신규 | 예외 2개 |
| `core/domain/.../repository/BackupRepository.kt` | 신규 | 인터페이스 |
| `core/data/.../backup/BackupArchive.kt` | 신규 | zip 쓰기·읽기 |
| `core/data/.../image/LocalImageStore.kt` | 수정 | `replaceAll(sourceDirectory: File)` |
| `core/data/src/test/.../repository/fake/FakeDietImageStore.kt` | 수정 | `replaceAll` 구현 |
| `core/data/src/test/.../repository/fake/Fake{DietEntry,Exercise,Routine,WeightRecord,WorkoutEntry,WorkoutMemo}Dao.kt` | 수정 | DAO 추가 메서드 구현(인메모리) |
| `core/data/.../repository/BackupRepositoryImpl.kt` | 신규 | 내보내기·가져오기 |
| `core/data/.../di/DataModule.kt` | 수정 | `bindBackupRepository` |
| `feature/my/api/.../MyNavKey.kt` | 수정 | `BackupNavKey`, `navigateToBackup` |
| `feature/my/impl/.../home/MyContracts.kt` | 수정 | `ClickBackup`, `NavigateToBackup` |
| `feature/my/impl/.../home/MyViewModel.kt` | 수정 | intent → effect |
| `feature/my/impl/.../home/composable/MyView.kt` | 수정 | `goToBackup`/`onClickBackup`, `SectionRow` |
| `feature/my/impl/.../backup/BackupContracts.kt` | 신규 | State/Intent/Effect |
| `feature/my/impl/.../backup/BackupViewModel.kt` | 신규 | |
| `feature/my/impl/.../backup/composable/BackupView.kt` | 신규 | View/ViewImpl/다이얼로그/Preview |
| `feature/my/impl/.../MyNavGraph.kt` | 수정 | `entry<BackupNavKey>`, `goToBackup` |
| `feature/my/impl/src/test/.../fake/FakeBackupRepository.kt` | 신규 | 호출 기록·실패 주입 |
| `feature/my/impl/src/test/.../backup/BackupViewModelTest.kt` | 신규 | 수용 조건의 ViewModel 부분 |
| `feature/my/impl/src/test/.../home/MyViewModelTest.kt` | 수정 | `ClickBackup` → `NavigateToBackup` |
| `core/data/src/test/.../backup/BackupArchiveTest.kt` | 신규 | zip 왕복, `snapshot.json` 없음, zip 아님, `..` 경로 항목 |

### 구현 순서

1. **로컬 스냅샷** — 만드는 것: `@Serializable` Entity, `BodyPlanSnapshot`, `SnapshotStore`, DAO 추가 메서드, `UnsupportedBackupVersionException`(도메인). 쓰는 것: 기존 `BodyPlanDatabase`. 검증 `./gradlew :app:compileDebugKotlin` + `core/local/schemas/.../10.json`이 바뀌지 않았는지(스키마 변경 없음) 확인.
2. **도메인·데이터** — 만드는 것: `BackupRepository`, `InvalidBackupFileException`, `BackupArchive`, `LocalImageStore.replaceAll`, `BackupRepositoryImpl`, `DataModule` 바인딩. 쓰는 것: 1의 `SnapshotStore`·`BodyPlanSnapshot`, 기존 `Json`·`Clock`·`LocalImageStore`. 검증 `./gradlew :app:compileDebugKotlin :core:data:testDebugUnitTest`. 여기서 커밋 1회.
3. **화면** — 만드는 것: `BackupNavKey`/`navigateToBackup`, `backup/` 패키지 3파일, 마이 탭 확장, navGraph entry. 쓰는 것: 2의 `BackupRepository`·예외 2개. 검증 `./gradlew :app:assembleDebug :feature:my:impl:testDebugUnitTest` + `BackupViewImplPreview` 렌더. 커밋 1회.
4. 리뷰(code-reviewer: 로직·아키텍처) → 테스트(test-engineer: `BackupArchive`·`BackupViewModel`·`MyViewModel`; `SnapshotStore`·`BackupRepositoryImpl`은 Room·ContentResolver에 묶여 단위 테스트 범위 밖) → 발견 반영 → 커밋.

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| `LocalImageStore` 인터페이스(`replaceAll` 추가) | `DietLogRepositoryImpl`·`InbodyImageRepositoryImpl`·`AnalysisResultRepositoryImpl` 컴파일, `FakeDietImageStore` 테스트 |
| DAO 인터페이스에 메서드 추가 | `core/data/src/test/.../fake/Fake*Dao.kt` 6개가 구현해야 컴파일된다 |
| Room Entity에 `@Serializable` | 스키마 JSON(`10.json`) 불변 |
| `MyContracts`·`MyView` 확장 | 마이 탭 기존 4개 행 동작, `MyViewModelTest` |
| `core:local` 빌드 스크립트 | `:app:assembleDebug` |

자기 대조: 통과 (대조 15/15)
- 고침: 데이터 계약, 상태 계약, 화면 구성, 파일별 작업
