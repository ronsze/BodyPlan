# 운동 세션 — 초 단위 시간·포그라운드 알림·일시정지

**작성일**: 2026-09-21

## 설계

### 요약

세션 상태를 화면 ViewModel에서 앱 전역 싱글턴(`WorkoutSessionEngine`, `core:data`; 계약은 `core:domain`의 `WorkoutSessionController`)으로 올린다. 화면과 알림이 같은 상태를 보고, 화면을 나갔다 와도 세션이 남는다.
`core:data`의 foreground service가 세션 동안 알림에 경과 시간을 초 단위로 그리고, 알림 버튼으로 일시정지·재개·종료가 된다. 휴식 종료 진동도 서비스가 울린다.
화면 바는 `운동 중 · 12:34`처럼 초 단위로 갱신되고, 하단에 「일시정지 / 재개」가 「운동 종료」 옆에 붙는다.

### 배경

사용자 요청: "운동 시작했을때 시간을 초단위로 보여주고, foreground service로 작업표시줄 표시, 앱 내렸을때도 시간 표시, 일시정지도 추가".

사용자가 확정한 것(임의 판단 아님):

| 항목 | 결정 |
|---|---|
| 세션이 사는 곳 | A안 — 앱 전역 싱글턴. 화면을 나가도 세션이 남는다 |
| 일시정지 범위 | 경과 시간과 휴식 타이머가 함께 멈춘다 |
| 알림 버튼 | `일시정지`/`재개`·`종료`를 둔다. 알림을 누르면 앱을 여는 것까지만 하고 특정 화면으로 보내지 않는다 |
| 표기 | 화면·알림 모두 `12:34`, 1시간 넘으면 `1:02:34`. 종료 토스트의 `N분`은 그대로 |

foreground service는 사용자가 지정한 방법이다(`.claude/INTERVENTION.md` 2026-09-21). 앱이 뒤로 간 뒤 초 단위로 갱신되는 알림은 FGS 외에 수단이 없어 AI 판단도 같다.

기존 `plans/workout-session-design.md`의 "화면을 떠났다 돌아오면 세션은 없다"는 이 계획으로 뒤집힌다 — 알림이 살아 있는데 화면에 세션이 없는 상태는 성립하지 않는다.

밝혀 둘 것:
- Android 13+는 알림 권한을 거절하면 서비스는 돌지만 알림이 보이지 않는다. `운동 시작` 때 한 번 묻고, 답과 무관하게 세션을 시작한다(분석 버튼과 같은 방식).
- FGS 타입은 `specialUse`다. `health`는 센서 권한(ACTIVITY_RECOGNITION 등)이 전제라 맞지 않는다.
- 세션은 프로세스가 살아 있는 동안만이다. 앱이 죽으면 세션·알림도 없다(비목표).

### 수용 조건

엔진(`WorkoutSessionEngine`):
- [ ] `start()` 뒤 `session`이 `WorkoutSession(elapsedSeconds = 0, isPaused = false, completedSets = 빈, rest = null)`이고, 시계가 1초 갈 때마다 `elapsedSeconds`가 1씩 는다.
- [ ] `pause()` 뒤 `isPaused = true`이고 시계가 가도 `elapsedSeconds`·`rest.remainingSeconds`가 그대로다. `resume()` 뒤 다시 는다·준다. 일시정지 중 흐른 시간은 경과에 들어가지 않는다.
- [ ] 세션이 없을 때 `pause`·`resume`·`toggleSet`·`adjustRest`·`skipRest`는 아무 일도 하지 않는다. 세션 중 `start()`는 무시된다. 일시정지가 아닐 때 `resume()`·일시정지 중 `pause()`도 무시된다.
- [ ] `toggleSet(key)`: 켤 때 `completedSets`에 담기고 `rest = RestTimer(90)`. 끌 때 타이머는 그대로. 휴식 중 다른 세트를 켜면 90으로 다시 시작.
- [ ] 휴식은 매초 1씩 줄고, 0에 닿으면 `rest = null`이 되며 `restEnded`가 한 번 나간다.
- [ ] `adjustRest(-30)`으로 0 이하가 되면 즉시 `rest = null`, `restEnded` 없음. `adjustRest(+30)`은 제한 없이 더한다. `skipRest()`는 `restEnded` 없이 `rest = null`.
- [ ] `end()` 뒤 `session = null`이고 시계가 가도 바뀌지 않는다.

화면(오늘 기록):
- [ ] `운동 시작`을 누르면 Android 13+에서 알림 권한을 한 번 묻고, 허용·거절과 무관하게 세션이 시작된다.
- [ ] 세션 중 하단 바가 `운동 중 · 0:01`처럼 매초 갱신된다. 1시간을 넘으면 `1:00:00`.
- [ ] 하단에 「일시정지」와 「운동 종료」가 한 줄로 보인다. 「일시정지」를 누르면 바가 `일시정지 · 12:34`(회색)로 바뀌고 버튼이 「재개」가 된다. 휴식 중이었으면 휴식 표시·`-30초`·`+30초`·`건너뛰기`가 숨고, 재개하면 남은 시간부터 다시 보인다.
- [ ] 화면을 뒤로 나갔다가 오늘 기록에 다시 들어오면 세션이 그대로 있다(경과 시간·체크·일시정지 상태 유지).
- [ ] 세션 중 다른 날짜의 기록 화면에는 세션 바·체크 칸이 없다.
- [ ] `운동 종료` 토스트 `운동 종료 · {N}분 · 볼륨 {1,240}kg`은 지금과 같다.
- [ ] 세트 체크·휴식 ±30초·건너뛰기·keepScreenOn·조회 실패 화면에서 세션 시작 불가는 지금과 같다.

알림·서비스:
- [ ] 세션이 시작되면 알림 채널 `운동 세션`에 진행 중(ongoing) 알림이 뜨고, 매초 `12:34`로 갱신된다. 소리·진동 없이 갱신된다.
- [ ] 알림 제목은 운동 중 `운동 중`, 일시정지 중 `일시정지`. 본문은 `12:34`, 휴식 중이면 `12:34 · 휴식 1:20`.
- [ ] 알림 버튼: 운동 중에는 `일시정지`·`종료`, 일시정지 중에는 `재개`·`종료`. 누르면 화면을 열지 않고 상태가 바뀌고, 화면이 떠 있으면 화면도 같이 바뀐다.
- [ ] 알림 본문을 누르면 앱이 열린다(마지막 화면 그대로).
- [ ] 앱을 홈으로 내려도 알림 시간이 계속 간다.
- [ ] 휴식이 0에 닿으면 앱이 뒤에 있어도 짧게 진동한다(500ms).
- [ ] 세션이 끝나면(화면·알림 어느 쪽에서든) 알림이 사라지고 서비스가 멈춘다.
- [ ] 알림 권한 거절: 세션·화면 바·서비스는 그대로 돌고 알림만 보이지 않는다. 알림에서 `종료`한 세션은 화면에서 토스트가 뜨지 않는다(화면이 시작한 종료가 아니다).

### 비목표

- 세션의 영속화·프로세스 사망 복구.
- 알림을 눌렀을 때 오늘 기록 화면으로 직접 이동.
- 휴식 기본값 설정, 휴식 종료 알림(별도 알림).
- 어제·루틴 상세 세션.
- `AnalysisNotifier`·분석 알림 경로 정리. 권한 요청 헬퍼만 공용으로 올린다.
- 자정을 넘긴 세션의 날짜 처리 — 세션은 시작한 화면(오늘)의 것이고 자정 뒤 `canStartSession` 판정은 진입 시점 그대로다.

### 데이터 계약

저장소·DAO 변경 없음. 시각은 주입한 `java.time.Clock`(`DataModule.provideClock`)으로만 읽는다.

**엔진 내부 시간 셈(공개하지 않음)**

| 값 | 내용 | 귀착지 |
|---|---|---|
| `accumulatedMillis` | 일시정지 전까지 누적된 경과 | `elapsedSeconds` |
| `resumedAtMillis` | 마지막 재개(또는 시작) 시각. 일시정지 중 `null` | `elapsedSeconds`·`isPaused` |

`elapsedSeconds = (accumulatedMillis + (now - resumedAtMillis ?: 0)) / 1000`. `pause()`는 `accumulatedMillis += now - resumedAtMillis; resumedAtMillis = null`. `resume()`은 `resumedAtMillis = now`.

**매니페스트** — `core/data/src/main/AndroidManifest.xml`(신규, 라이브러리 매니페스트라 `package` 없음):

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<service
    android:name=".session.WorkoutSessionService"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="운동 세션의 경과 시간을 알림에 표시" />
</service>
```

`VIBRATE`·`POST_NOTIFICATIONS`는 `app` 매니페스트에 이미 있다.

### 상태 계약

**도메인 모델 `core/domain/model/WorkoutSession.kt`(신규)** — `core/ui/components/WorkoutSetKey.kt`와 `feature/.../log/WorkoutSession.kt`는 삭제하고 여기로 옮긴다.

```kotlin
data class WorkoutSetKey(val entryId: Long, val setIndex: Int)
data class RestTimer(val remainingSeconds: Int)
data class WorkoutSession(
    val elapsedSeconds: Long,
    val isPaused: Boolean,
    val completedSets: Set<WorkoutSetKey>,
    val rest: RestTimer?,
)
fun clockText(totalSeconds: Long): String   // 754 → "12:34", 3_600 → "1:00:00"
val WorkoutSession.elapsedText: String get() = clockText(elapsedSeconds)
val RestTimer.text: String get() = clockText(remainingSeconds.toLong())
```

`startedAtMillis`는 공개 모델에서 뺀다 — 화면이 시계를 읽지 않는다는 기존 원칙 그대로이고, 일시정지가 생겨 시작 시각만으로는 경과를 셀 수 없다.

**컨트롤러 계약 `core/domain/repository/WorkoutSessionController.kt`(신규)** — `AnalysisRunner`와 같은 자리(도메인 인터페이스·데이터 구현).

```kotlin
interface WorkoutSessionController {
    val session: StateFlow<WorkoutSession?>
    /** 휴식이 0에 닿을 때 한 번. 건너뛰기·-30초로 끝낼 때는 나가지 않는다. */
    val restEnded: SharedFlow<Unit>
    fun start()
    fun pause()
    fun resume()
    fun end()
    fun toggleSet(key: WorkoutSetKey)
    fun adjustRest(deltaSeconds: Int)
    fun skipRest()
}
```

**엔진 `core/data/session/WorkoutSessionEngine.kt`(신규)** — `internal class WorkoutSessionEngine(clock: Clock, scope: CoroutineScope) : WorkoutSessionController`. Android를 모르는 순수 Kotlin 상태 기계 — `core:domain`은 모델·인터페이스·UseCase만 두는 구조라 구현은 `core:data`에 둔다. `start()`가 `scope`에 1초 틱 Job을 띄우고 `end()`가 취소한다. 틱은 일시정지 중이면 아무것도 바꾸지 않는다. `restEnded`는 `MutableSharedFlow<Unit>(extraBufferCapacity = 1)`.

**구현체 `core/data/session/WorkoutSessionControllerImpl.kt`(신규)** — `@Singleton internal class ... @Inject constructor(engine: WorkoutSessionEngine, @ApplicationContext context: Context) : WorkoutSessionController by engine`. `start()`만 덮어 `engine.start()` 뒤 `ContextCompat.startForegroundService(context, Intent(context, WorkoutSessionService::class.java))`. 엔진이 Android를 모르게 하려는 분리다.

**DI(`DataModule`)**: `@Binds @Singleton bindWorkoutSessionController(impl: WorkoutSessionControllerImpl): WorkoutSessionController`, `@Provides @Singleton provideWorkoutSessionEngine(clock: Clock): WorkoutSessionEngine = WorkoutSessionEngine(clock, CoroutineScope(SupervisorJob() + Dispatchers.Default))`.

**서비스 `core/data/session/WorkoutSessionService.kt`(신규)** — `@AndroidEntryPoint internal class WorkoutSessionService : Service()`. 주입: `WorkoutSessionController`, `WorkoutSessionNotifier`. 자체 `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`를 `onCreate`에서 만들고 `onDestroy`에서 취소한다(lifecycle-service 의존성을 더하지 않는다).
- `onCreate`: `startForeground(NOTIFICATION_ID, notifier.build(session.value ?: 빈 세션))`. 이후 `session` 수집 — `null`이면 `stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf()`, 아니면 `notifier.update(session)`. `restEnded` 수집 — `Vibrator.vibrate(VibrationEffect.createOneShot(500, DEFAULT_AMPLITUDE))`.
- `onStartCommand`: `intent.action`이 `ACTION_PAUSE`→`controller.pause()`, `ACTION_RESUME`→`controller.resume()`, `ACTION_END`→`controller.end()`. `START_NOT_STICKY`.
- `onBind`: `null`.

**알림 `core/data/session/WorkoutSessionNotifier.kt`(신규)** — `@Singleton internal class ... @Inject constructor(@ApplicationContext context)`. 채널 `workout_session` / `운동 세션` / `IMPORTANCE_LOW`. `fun build(session: WorkoutSession): Notification`, `fun update(session: WorkoutSession)`(`POST_NOTIFICATIONS` 없으면 아무것도 안 함 — `AnalysisNotifier.hasPermission`과 같은 판정).
알림 내용: `setOngoing(true)`, `setOnlyAlertOnce(true)`, `setSilent(true)`, `setSmallIcon(android.R.drawable.ic_media_play)`, 제목·본문·버튼은 수용 조건대로. 버튼은 `PendingIntent.getService(context, requestCode, Intent(context, WorkoutSessionService::class.java).setAction(ACTION_*), FLAG_IMMUTABLE)`. 본문 탭은 `packageManager.getLaunchIntentForPackage(packageName)`을 `PendingIntent.getActivity(... FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)`로 — `AnalysisNotifier.openIntent`와 같은 형태, 여분 값 없음.

**화면 `WorkoutLog` — State**

| 필드 | 타입 | 초기값 | 출처 |
|---|---|---|---|
| `canStartSession` | `Boolean` | `false` | 그대로 |
| `session` | `WorkoutSession?`(도메인) | `null` | `canStartSession`일 때 `controller.session` 수집. 아니면 항상 `null` |

`isSessionActive` 파생 그대로.

**Intent**

| Intent | 처리 |
|---|---|
| `ClickStartSession` | `canStartSession && !isSessionActive`면 `controller.start()` |
| `ClickPauseSession`(신규) | `controller.pause()` |
| `ClickResumeSession`(신규) | `controller.resume()` |
| `ClickEndSession` | `session`이 있으면 `elapsedSeconds`를 읽어 `controller.end()` 뒤 `ShowMessage("운동 종료 · ${elapsed/60}분 · 볼륨 $volume")` |
| `ToggleSetCompleted(key)` | `controller.toggleSet(key)` |
| `ClickAdjustRest(delta)` | `controller.adjustRest(delta)` |
| `ClickSkipRest` | `controller.skipRest()` |

**Effect**: `VibrateRestEnd` 삭제 — 진동은 서비스가 울린다. 나머지 그대로.

**ViewModel 생성자**: `sessionController: WorkoutSessionController` 추가. `clock`은 오늘 판정에 계속 쓴다. `sessionJob`·`tick`·`updateSession`·`REST_SECONDS`·`TICK_MILLIS`·`MILLIS_PER_SECOND`는 엔진으로 옮기고 VM에서 지운다.

실패 경로: 컨트롤러 호출은 실패하지 않는다(메모리 상태). 서비스 시작 실패(`ForegroundServiceStartNotAllowedException`)는 화면이 앞에 있을 때 누르므로 일어나지 않는다 — 잡지 않는다.

### 화면 구성

**`core/ui/components/NotificationPermission.kt`(신규)** — `AnalysisScreen.kt`의 `private fun requestNotificationThen(onClickAnalyze)`를 그대로 옮겨 `@Composable fun requestNotificationThen(action: () -> Unit): () -> Unit`으로 공개한다. `AnalysisScreen`은 이것을 쓴다(`POST_NOTIFICATIONS` 상수도 함께 이동).

**`core/ui/components/WorkoutEntryGroups.kt`** — `WorkoutSetKey` import만 도메인으로 바꾼다. 시그니처 `workoutEntryGroups(entries, isEditable, expansion, onClickEntry, onClickDeleteEntry, personalRecordExerciseIds = emptySet(), completedSets: Set<WorkoutSetKey>? = null, onToggleSet: (WorkoutSetKey) -> Unit = {})` 그대로.

**`log/composable/SessionBar.kt`** — 시그니처 `SessionBar(session: WorkoutSession, onClickAdjustRest: (Int) -> Unit, onClickSkipRest: () -> Unit, modifier)` 그대로. 내용:
- `session.isPaused`: `BaseText("일시정지 · ${session.elapsedText}", titleMedium, TextTertiary)`만.
- 휴식 중: `BaseText("휴식 ${rest.text}", titleMedium, Accent)` + 기존 `-30초`·`+30초`·`건너뛰기`.
- 그 외: `BaseText("운동 중 · ${session.elapsedText}", titleMedium, TextPrimary)`.
- `restText`·`SECONDS_PER_MINUTE` 삭제(도메인 `clockText`로). Preview에 일시정지 상태 하나 추가.

**`log/composable/WorkoutLogView.kt`**
- `운동 시작`: `PrimaryButton(text = "운동 시작", onClick = requestNotificationThen(uiEvents.onClickStartSession))`.
- 세션 중 마지막 줄: `Row(horizontalArrangement = Arrangement.spacedBy(8.dp))` 안에 `OutlinedActionButton(text = if (session.isPaused) "재개" else "일시정지", onClick = ..., modifier = Modifier.weight(1f))`(재사용, 시그니처 `(text, onClick, modifier)`)와 `PrimaryButton(text = "운동 종료", onClick = uiEvents.onClickEndSession, modifier = Modifier.weight(1f))`(재사용, `(text, onClick, modifier, enabled)`).
- `CollectEffect`의 `VibrateRestEnd` 분기·`Vibrator` import·`VIBRATION_MILLIS` 삭제.
- `WorkoutLogUiEvents`에 `onClickPauseSession: () -> Unit`, `onClickResumeSession: () -> Unit` 추가. `rememberUiEvents`·`previewUiEvents` 갱신.
- Preview의 `WorkoutSession(...)` 생성을 도메인 모델 생성자로 바꾼다.
- keepScreenOn 그대로.

**카피 원문**: `운동 시작` / `운동 종료` / `일시정지` / `재개` / `운동 중 · {12:34}` / `일시정지 · {12:34}` / `휴식 {1:20}` / 알림 채널 `운동 세션` / 알림 제목 `운동 중`·`일시정지` / 알림 본문 `{12:34}`·`{12:34} · 휴식 {1:20}` / 알림 버튼 `일시정지`·`재개`·`종료`

화면별 상태(오늘 기록): 로딩·빈·에러 — 기존 분기 그대로, 에러면 하단 Column을 그리지 않아 세션 시작 불가. 권한 — `운동 시작` 때 `requestNotificationThen`이 한 번 묻고 답과 무관하게 시작.

### 네비게이션

해당 없음. 알림 본문 탭은 런처 인텐트로 앱을 열 뿐이다.

### 버린 안

- **B. 세션은 VM에 두고 서비스는 거울**: 화면을 나가면 VM이 죽어 알림만 남는 고아 상태. 채팅에서 버림.
- **C. A + DataStore 영속화**: 프로세스 사망 복구까지 하면 범위 초과. 채팅에서 버림.
- 엔진 없이 `core:data` 구현체 하나에 다 넣기: 서비스 시작 때문에 `Context`가 들어가 순수 단위 테스트가 안 된다. 엔진과 얇은 구현체로 나눈다.
- 엔진을 `core:domain`에 두기: 도메인은 모델·인터페이스·UseCase만 두는 구조다(`AnalysisRunner`도 구현은 `core:data`). 계획 착수 시 scaffold 규칙과 맞춰 `core:data`로 옮겼다.
- 서비스 시작을 `BodyPlanApplication`에서 세션 흐름을 구독해 하기: 앱 클래스에 배선이 생기고 구독 시작 시점이 불명확하다. `start()`를 덮는 구현체가 단순하다.
- `LifecycleService` 사용: `lifecycle-service` 의존성이 새로 들어온다. 자체 scope 두 줄이면 된다.
- FGS 타입 `health`: 센서 런타임 권한 중 하나가 전제라 타이머 앱에는 맞지 않는다.
- 진동을 계속 화면(Effect)에서: 앱이 뒤에 있으면 화면이 없어 못 울린다. 서비스로 옮긴다.
- 일시정지를 `SessionBar` 안의 글자 액션으로: 휴식 중엔 액션이 넷이 되어 한 줄에 안 들어간다. 「운동 종료」 옆 버튼으로 둔다.

## 실행

### 파일별 작업

| 경로 | 신규/수정 | 할 일 |
|---|---|---|
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/model/WorkoutSession.kt` | 신규 | `WorkoutSetKey`·`RestTimer`·`WorkoutSession`·`clockText`·`elapsedText`·`text` |
| `core/domain/src/main/java/kr/sdbk/bodyplan/core/domain/repository/WorkoutSessionController.kt` | 신규 | 인터페이스 |
| `core/data/src/main/java/kr/sdbk/bodyplan/core/data/session/WorkoutSessionEngine.kt` | 신규 | 상태 기계·틱 |
| `core/data/src/test/java/kr/sdbk/bodyplan/core/data/session/WorkoutSessionEngineTest.kt` | 신규 | 엔진 수용 조건 — 테스트 단계 |
| `feature/workoutlog/impl/src/test/java/.../fake/FakeWorkoutSessionController.kt` | 신규 | 호출을 기록하고 `session`을 밖에서 밀어 넣는 페이크 — 테스트 단계 |
| `core/ui/components/src/main/java/kr/sdbk/bodyplan/core/ui/components/WorkoutSetKey.kt` | 삭제 | 도메인으로 이동 |
| `core/ui/components/src/main/java/kr/sdbk/bodyplan/core/ui/components/WorkoutEntryGroups.kt` | 수정 | import |
| `core/ui/components/src/main/java/kr/sdbk/bodyplan/core/ui/components/NotificationPermission.kt` | 신규 | `requestNotificationThen` 공개 |
| `core/ui/components/src/main/java/kr/sdbk/bodyplan/core/ui/components/AnalysisScreen.kt` | 수정 | private 헬퍼 삭제, 공용 사용 |
| `core/data/build.gradle.kts` | 수정 | `implementation(libs.androidx.core.ktx)` 명시(ContextCompat·NotificationCompat) |
| `core/data/src/main/AndroidManifest.xml` | 신규 | 권한 2개·서비스 선언 |
| `core/data/src/main/java/kr/sdbk/bodyplan/core/data/session/WorkoutSessionControllerImpl.kt` | 신규 | 엔진 위임 + 서비스 시작 |
| `core/data/src/main/java/kr/sdbk/bodyplan/core/data/session/WorkoutSessionService.kt` | 신규 | FGS·알림 갱신·버튼 처리·진동 |
| `core/data/src/main/java/kr/sdbk/bodyplan/core/data/session/WorkoutSessionNotifier.kt` | 신규 | 채널·알림 빌드·갱신 |
| `core/data/src/main/java/kr/sdbk/bodyplan/core/data/di/DataModule.kt` | 수정 | 엔진 Provides·컨트롤러 Binds |
| `feature/workoutlog/impl/src/main/java/.../log/WorkoutSession.kt` | 삭제 | 도메인으로 이동 |
| `feature/workoutlog/impl/src/main/java/.../log/WorkoutLogContracts.kt` | 수정 | 도메인 모델 import, Intent 2개 추가, `VibrateRestEnd` 삭제 |
| `feature/workoutlog/impl/src/main/java/.../log/WorkoutLogViewModel.kt` | 수정 | 컨트롤러 주입·수집, 세션 로직 제거 |
| `feature/workoutlog/impl/src/main/java/.../log/composable/SessionBar.kt` | 수정 | 초 단위·일시정지 표시 |
| `feature/workoutlog/impl/src/main/java/.../log/composable/WorkoutLogView.kt` | 수정 | 권한 요청·일시정지 버튼·진동 제거·UiEvents |
| `feature/workoutlog/impl/src/test/java/.../log/WorkoutLogViewModelTest.kt` | 수정 | 헬퍼에 `FakeWorkoutSessionController`, 세션 케이스는 위임·수집 확인으로 조정(틱·휴식 규칙은 엔진 테스트로 이동) — 테스트 단계 |

### 구현 순서

1. **도메인** — 만드는 것: `WorkoutSetKey`·`RestTimer`·`WorkoutSession`·`clockText`, `WorkoutSessionController`. 쓰는 것: `kotlinx.coroutines` Flow. 검증: `./gradlew :core:domain:compileKotlin`.
2. **공용 UI** — 만드는 것: `NotificationPermission.kt`. 고치는 것: `WorkoutSetKey.kt` 삭제, `WorkoutEntryGroups`·`AnalysisScreen` import. 쓰는 것: 1의 `WorkoutSetKey`. 검증: 3과 함께.
3. **데이터** — 만드는 것: `WorkoutSessionEngine`, `WorkoutSessionControllerImpl`, `WorkoutSessionService`, `WorkoutSessionNotifier`, 매니페스트, DI. 쓰는 것: 1의 인터페이스·모델, `java.time.Clock`. 검증: `./gradlew :app:compileDebugKotlin`(feature가 아직 옛 모델을 참조하면 실패하므로 4와 같이 확인).
4. **feature** — 고치는 것: Contracts·ViewModel·SessionBar·WorkoutLogView, `WorkoutSession.kt` 삭제. 쓰는 것: 1의 모델·인터페이스, 2의 `requestNotificationThen`. 검증: `./gradlew :app:compileDebugKotlin`.
5. **리뷰·테스트** — code-reviewer(로직·아키텍처·재사용·컨벤션)와 test-engineer(`WorkoutSessionEngine` 신규 테스트, `WorkoutLogViewModel` 테스트 보수) 동시 위임. 검증: `./gradlew :core:data:testDebugUnitTest :feature:workoutlog:impl:testDebugUnitTest`. 통과 후 커밋 하나.
6. **기기 확인** — 알림 표시·버튼·백그라운드 갱신·진동은 단위 테스트가 못 덮는다. 빌드 설치 후 수용 조건의 알림·서비스 절을 손으로 확인한다.

### 회귀 대상

| 고치는 것 | 확인할 기존 화면 |
|---|---|
| `WorkoutSetKey` 패키지 이동 | 루틴 상세(`workoutEntryGroups` 기본값 경로)·운동 기록 체크 |
| `requestNotificationThen` 공용화 | 분석 화면 3종(식단·운동·인바디)의 분석 버튼 권한 요청 |
| `WorkoutLogViewModel` 생성자 | `WorkoutLogViewModelTest` 헬퍼 |
| `WorkoutLogView` 하단 Column | 어제(시작 버튼 없음)·과거(분석 버튼만) 화면 |
| `DataModule` | 앱 기동(Hilt 그래프) |

자기 대조: 통과 (대조 16/16)
- 고침: 수용 조건(알림에서 종료 시 토스트 없음·세션 없을 때 무시 조건 명시), 상태 계약(`ClickEndSession`이 `end()` 전에 경과를 읽는 순서, 다른 날짜 화면의 `session = null`), 비목표(자정 처리)
