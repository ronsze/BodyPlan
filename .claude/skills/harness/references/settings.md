# settings 작성법

목적: `.claude/settings.json`·`settings.local.json`을 공식 규격과 하네스 원칙에 맞게 작성한다.

## 파일 규격

| 파일 | 공유 | 두는 것 |
|---|---|---|
| `.claude/settings.json` | 팀 (git 커밋) | 팀이 합의한 permissions·hooks·모델 설정 |
| `.claude/settings.local.json` | 개인 (gitignored) | 개인 확장 권한, 로컬 환경변수, "don't ask again" 저장분 |
| `~/.claude/settings.json` | 개인 전역 | 프로젝트와 무관한 개인 기본값 |

우선순위: Local > Project > User. deny는 어느 범위에 있든 모든 allow를 이긴다.

## permissions 문법

- 평가 순서: **deny → ask → allow**, 첫 매치가 결정한다. 구체성은 고려되지 않는다.
- `Tool` 전체, `Tool(지정자)` 세부: `Bash(./gradlew *)`(공백 경계 와일드카드), `Read(src/**)`, `WebFetch(domain:*.example.com)`, `mcp__server__*`.
- `&&`·`;`·`|`로 이어진 복합 명령은 각 부분이 따로 평가된다.
- `ls`·`cat`·`git log` 등 읽기 전용 명령은 규칙 없이 자동 허용된다.

## 작성 규칙

- **project에는 팀 합의만**: 개인 편의 허용은 local로. 비밀·개인 경로는 어디에도 커밋하지 않는다.
- **allow는 좁게**: 명령 단위로 적는다(`Bash(./gradlew test*)`). `Bash(*)`류 광역 allow는 쓰지 않는다.
- **차단은 deny로**: 훅이 아니라 permissions로 충분한 단순 차단(특정 명령·파일 접근)은 deny 규칙으로 한다.
- **키는 필요한 것만**: 기본값과 같은 설정은 적지 않는다. hooks 작성은 [hooks.md](hooks.md)를 따른다.

## 템플릿

```json
{
  "permissions": {
    "allow": ["<Tool(좁은 지정자)>"],
    "deny": ["<차단할 규칙>"]
  }
}
```

## 검증

- 문법: `python3 -m json.tool .claude/settings.json`으로 JSON 유효성을 확인한다.
- 적용: `/permissions`로 규칙이 인식됐는지 보고, 대상 명령 하나를 실제 실행해 allow/deny가 의도대로 동작하는지 확인한다. permissions·hooks는 저장 즉시 적용된다.
- 매칭 진단: `claude --verbose`로 도구 호출의 실제 파라미터를 확인한다.
