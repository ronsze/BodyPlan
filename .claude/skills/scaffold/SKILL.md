---
name: scaffold
description: 이 리포에 새 구성요소를 만들 때 로드한다. 구성요소 = 새 파일·디렉토리가 생기고 배치·이름·골격이 리포 규칙에 묶인 것 — feature 모듈, core 모듈, 화면, UseCase, Repository, 공용 컴포넌트, Entity·DAO. 기존 파일 안의 함수·State 필드 추가, 값·문구 수정, `.claude/**` 하네스 작업은 대상이 아니다.
---

# scaffold

목적: 새 구성요소를 이 리포가 정한 배치·골격 그대로 만든다.

## 대상 판정

**새 파일이 생기고 그 위치·이름·골격이 리포 규칙에 묶여 있으면 대상이다.** 기존 파일 안에서 끝나면 비대상이다.

| 구성요소 | 만들어지는 것 | reference |
|---|---|---|
| feature 모듈 | `feature/<이름>/api` + `feature/<이름>/impl` | [module.md](references/module.md) |
| core 모듈 | `core/<이름>` | [module.md](references/module.md) |
| 화면 | NavKey·navigate 확장(api) + Contracts·ViewModel·View·navGraph(impl) | [screen.md](references/screen.md) |
| UseCase | `core:domain`의 UseCase | 미작성 |
| Repository | `core:domain` 인터페이스 + `core:data` 구현 + Hilt 바인딩 | 미작성 |
| 공용 컴포넌트 | `core:designsystem`의 Composable | 미작성 |
| 로컬 저장소 | `core:local`의 Entity·DAO | 미작성 |

reference가 `미작성`인 구성요소는 규칙이 아직 없다. 그 사실을 사용자에게 말하고, 주변 기존 코드의 배치·표기를 따라 만든 뒤 규칙화 여부를 묻는다. 없는 규칙을 지어내지 않는다.

## 절차

1. 만들 구성요소를 위 표에서 특정한다. 요청 하나가 여럿을 요구하면(예: "새 화면 추가" → feature 모듈 + 화면 + UseCase) 전부 나열하고 만드는 순서를 정한다 — 의존 방향의 하류부터 만든다.
2. 해당 reference를 읽는다. 읽지 않고 골격을 기억으로 쓰지 않는다.
3. reference의 체크리스트를 순서대로 수행한다.
4. reference에 적힌 검증 명령을 실행해 통과를 확인한다.

## 경계

- 대상: 위 표의 구성요소를 새로 만드는 것과 그에 딸린 등록(`settings.gradle.kts`, `libs.versions.toml`, DI 바인딩, NavDisplay 등록).
- 비대상: 만든 뒤의 내용 구현(화면 로직·비즈니스 규칙), 기존 구성요소 수정·삭제, 작업 절차(수준 판정·리뷰·테스트 — implement 스킬).

## 종료 조건

reference의 체크리스트를 모두 마치고 검증 명령이 통과하면 완료.

## 실패 시

reference와 실제 리포가 어긋나면(지목한 컨벤션 플러그인·모듈이 없음, 명령이 실패함) 우회하지 않고 멈춰 사용자에게 알린다. 어긋남은 `.claude/FEEDBACK.md`에 `규격 불일치`로 기록한다.

## 검증

- 호출 적절성: "정산 화면 추가해줘"(대상)와 "홈 화면 버튼 문구 바꿔줘"(비대상)가 description만으로 판별되는지 확인한다.
- 대상 판정: "HomeViewModel에 State 필드 추가"가 비대상으로, "feature 모듈 하나 만들어줘"가 대상으로 판정되는지 확인한다.
- 목적 달성: feature 모듈을 하나 실제로 만들어 `./gradlew :app:assembleDebug`가 통과하는지 확인한다.
- 미작성 처리: 규칙 없는 구성요소(UseCase 등)를 요청해 지어내지 않고 사실을 알리는지 확인한다.
