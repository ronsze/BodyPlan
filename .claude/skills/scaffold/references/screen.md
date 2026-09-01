# 화면 생성

목적: 화면 한 벌을 이 리포가 정한 파일 구성과 골격 그대로 만든다.

## 파일 구성

화면 하나는 `impl` 모듈의 **파일 3개**다. `api` 모듈에는 NavKey와 navigate 확장 함수를 둔다([module.md](module.md) 5번).

| 파일 | 담는 것 |
|---|---|
| `<화면>Contracts.kt` | `<화면>State`(data class) / `<화면>Intent`(sealed interface) / `<화면>Effect`(sealed interface) |
| `<화면>ViewModel.kt` | `BaseViewModel<S, I, E>` 상속, Hilt 주입. 화면 상태를 매핑해야 하면 `BaseViewModelImpl<S, U, I, E>` |
| `<화면>View.kt` | `<화면>Events` / `<화면>UiEvents` / `<화면>View` / `rememberUiEvents` / `<화면>ViewImpl` / Preview |

화면 안에서만 쓰는 타입은 전부 `internal`이다. 모듈 밖으로 나가는 것은 `api`의 NavKey와 `impl`의 navGraph 함수뿐이다.

### Events와 UiEvents를 나누는 기준

- `<화면>Events` — **화면 밖으로 나가는 것.** 다른 화면으로의 이동, 앱 종료 등. navGraph가 `navigator`로 만들어 넘긴다.
- `<화면>UiEvents` — **화면 안에서 나는 것.** 클릭·입력·스크롤 등. `rememberUiEvents`가 `Events`와 ViewModel을 묶어 만든다.

`ViewImpl`은 `UiEvents`만 받는다. `Events`도 ViewModel도 보지 않는다.

## 골격

### `<화면>Contracts.kt`

```kotlin
internal data class DetailState(
    val isLoading: Boolean = false,
    val plan: Plan? = null,
) : State

internal sealed interface DetailIntent : Intent {
    data class ClickPlan(val id: Long) : DetailIntent
}

internal sealed interface DetailEffect : Effect {
    data object GoBack : DetailEffect
}
```

`State`·`Intent`·`Effect`는 `kr.sdbk.bodyplan.core.ui.coordinator`의 것이다. 접두사 없는 단순명이라 `androidx.compose.runtime.State`·`android.content.Intent`와 겹칠 수 있다 — 같은 파일에서 둘 다 쓰면 `import ... as` 로 구분한다.

### `<화면>ViewModel.kt`

```kotlin
@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
internal class DetailViewModel
@AssistedInject
constructor(
    private val planRepository: PlanRepository,
    @Assisted private val navKey: DetailNavKey,
) : BaseViewModel<DetailState, DetailIntent, DetailEffect>(
    initialState = DetailState(),
) {
    override suspend fun initializeData() {
        loadPlan(navKey.planId)
    }

    override fun handleIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.ClickPlan -> loadPlan(intent.id)
        }
    }

    private fun loadPlan(id: Long) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            val plan = planRepository.getPlan(id)
            updateState { it.copy(isLoading = false, plan = plan) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: DetailNavKey): DetailViewModel
    }
}
```

- NavKey 인자가 필요 없으면 `@HiltViewModel` + `@Inject`만 쓰고 `Factory`는 만들지 않는다.
- 진입 시 1회 로드는 `initializeData()`를 재정의한다. `init` 블록에서 직접 하지 않는다 — 베이스가 화면이 `uiState`를 구독하는 시점에 한 번 호출한다. 구독마다 다시 태우려면 생성자에 `enableReInitialization = true`를 준다. 구독마다 매번 할 일은 `onStart()`다.
- 상태 변경은 `updateState { it.copy(...) }`, 일회성 동작은 `updateEffect(...)`로만 한다.
- 내부 상태와 화면이 그리는 상태가 달라 매핑이 필요하면 `BaseViewModel` 대신 `BaseViewModelImpl<S, U, I, E>`를 상속하고 `uiState`를 직접 만든다 — `state.map { ... }.setup(초기값)`.

### `<화면>View.kt`

```kotlin
internal data class DetailEvents(
    val goBack: () -> Unit,
)

internal data class DetailUiEvents(
    val onBackPressed: () -> Unit,
    val onClickPlan: (Long) -> Unit,
)

@Composable
internal fun DetailView(events: DetailEvents, viewModel: DetailViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvents = rememberUiEvents(events, viewModel)

    DetailViewImpl(
        state = state,
        uiEvents = uiEvents,
    )

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is DetailEffect.GoBack -> events.goBack()
        }
    }
}

@Composable
private fun rememberUiEvents(events: DetailEvents, viewModel: DetailViewModel): DetailUiEvents =
    remember {
        DetailUiEvents(
            onBackPressed = events.goBack,
            onClickPlan = { viewModel.handleIntent(DetailIntent.ClickPlan(it)) },
        )
    }

@Composable
internal fun DetailViewImpl(state: DetailState, uiEvents: DetailUiEvents) {
    // 실제 UI만 둔다. 상태 수집·effect 처리·ViewModel 접근은 여기 오지 않는다.
}

@Preview(showBackground = true)
@Composable
private fun DetailViewImplPreview() {
    BodyPlanTheme {
        DetailViewImpl(
            state = DetailState(),
            uiEvents = DetailUiEvents(onBackPressed = {}, onClickPlan = {}),
        )
    }
}
```

- `View`가 하는 일은 셋뿐이다 — `uiState` 수집, `UiEvents` 조립, effect 처리. UI는 한 줄도 두지 않는다.
- 상태는 `state`가 아니라 `uiState`를 구독한다 — 초기 로드가 이 구독에 걸려 있다.
- effect 처리는 `core:ui:coordinator`의 `CollectEffect`를 쓴다. `LaunchedEffect`로 직접 수집하지 않는다.
- Preview는 `ViewImpl`에 붙인다 — ViewModel이 없어야 렌더된다.
- `rememberUiEvents`의 `remember`는 키가 없다. `events`·`viewModel`이 화면 수명 동안 고정이라는 전제이고, `UiEvents`가 그 둘만 참조하면 맞다. **람다가 그 밖의 값을 캡처하면 키를 준다** — 캡처한 값이 바뀌어도 재생성되지 않아 옛 값을 계속 쓰게 된다.

  ```kotlin
  // uiEvents 람다가 state의 값을 캡처하는 경우
  @Composable
  private fun rememberUiEvents(
      events: DetailEvents,
      viewModel: DetailViewModel,
      planId: Long,
  ): DetailUiEvents =
      remember(planId) {
          DetailUiEvents(
              onClickApply = { viewModel.handleIntent(DetailIntent.Apply(planId)) },
          )
      }
  ```

  캡처를 피할 수 있으면 그게 먼저다 — 위 예는 `onClickApply: (Long) -> Unit`으로 두고 `ViewImpl`이 `state`에서 꺼내 넘기면 키가 필요 없다.

## 체크리스트

1. `api`에 NavKey와 navigate 확장 함수를 추가한다([module.md](module.md) 5번).
2. `impl`에 `<화면>Contracts.kt` → `<화면>ViewModel.kt` → `<화면>View.kt` 순으로 만든다.
3. `impl`의 navGraph에 entry를 추가한다.

   ```kotlin
   entry<DetailNavKey> { navKey ->
       val events = remember { DetailEvents(goBack = navigator::goBack) }
       DetailView(
           events = events,
           viewModel = hiltViewModel<DetailViewModel, DetailViewModel.Factory> { it.create(navKey) },
       )
   }
   ```

   NavKey 인자가 없으면 `viewModel = hiltViewModel()`.
4. 새 feature면 `:app`의 `BodyPlanNavDisplay`에 navGraph 호출을 추가한다([module.md](module.md) 7번).

## 검증

```
./gradlew :app:assembleDebug
```

추가로 `<화면>ViewImplPreview`가 Android Studio에서 렌더되는지 확인한다 — `ViewImpl`에 ViewModel 의존이 새어 들어갔는지 여기서 드러난다.
