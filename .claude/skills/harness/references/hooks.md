# 훅 작성법

목적: settings.json의 훅을 공식 규격과 하네스 원칙에 맞게 작성한다.

## 설정 규격

`.claude/settings.json`(팀 공유) 또는 `.claude/settings.local.json`(개인)의 `hooks` 객체. 이벤트별 배열에 `matcher`와 핸들러(`type`, `command` 등)를 둔다.

주요 이벤트: `PreToolUse`(도구 실행 전, 차단 가능) · `PostToolUse`(도구 성공 후) · `UserPromptSubmit`(프롬프트 제출 전) · `Stop`(응답 완료 시) · `SessionStart` · `FileChanged`(감시 파일 변경). 전체 목록은 공식 문서(hooks.md)를 확인한다.

## command 훅 계약

- 입력: stdin으로 JSON (`tool_name`, `tool_input`, `cwd` 등 이벤트별 필드).
- 출력: exit `0` = 통과(stdout JSON으로 세부 제어), exit `2` = 차단(stderr가 피드백), 그 외 = 비차단 오류.
- exit 0의 stdout JSON 주요 필드: `hookSpecificOutput.permissionDecision`(`allow|deny|ask`, PreToolUse), `decision: block`(Stop·PostToolUse), `additionalContext`(UserPromptSubmit), `systemMessage`.
- 같은 이벤트의 여러 훅은 병렬 실행되고 가장 제한적인 결과가 이긴다(deny > ask > allow).

## matcher 문법

- 생략·빈 문자열 = 전부 일치. `Edit|Write` = 목록 일치. 그 외 문자가 있으면 JS 정규식(예: `mcp__.*`).
- 도구 이벤트는 도구 이름, `SessionStart`는 시작 방식(`startup|resume|clear|compact`), `FileChanged`는 파일명과 매칭된다.
- 도구 이벤트는 `if` 필드로 인자까지 필터할 수 있다(예: `"if": "Bash(git *)"`).

## 작성 규칙

- **훅 하나에 목적 하나**: 한 줄을 넘는 로직은 `.claude/hooks/<name>.sh` 스크립트로 분리하고, 스크립트 최상단 주석에 목적을 서술한다(제1원칙 — JSON에는 주석을 둘 수 없다). settings.json에는 `${CLAUDE_PROJECT_DIR}/.claude/hooks/<name>.sh`로 참조한다.
- **matcher를 좁게**: 경계는 matcher(+`if`)가 강제한다. 빈 matcher로 전 도구에 걸지 않는다.
- **차단은 exit 2 + stderr 이유**: 모델이 왜 막혔는지 읽고 수정할 수 있게 이유를 쓴다.
- **실패를 설계한다**: 스크립트 오류가 차단으로 오인되지 않게 한다 — 판정 불가 시 exit 0(통과)인지 exit 2(차단)인지 목적에 맞게 정하고 주석에 남긴다.
- **Stop 훅**: stdin의 `stop_hook_active`가 true면 즉시 exit 0 해 무한 차단 루프를 막는다.
- **PostToolUse로 되돌리기 금지**: 이미 실행된 액션은 취소할 수 없다. 막아야 하면 PreToolUse를 쓴다.

## 템플릿

settings.json:

```json
{
  "hooks": {
    "<이벤트>": [
      {
        "matcher": "<좁은 matcher>",
        "hooks": [
          { "type": "command", "command": "${CLAUDE_PROJECT_DIR}/.claude/hooks/<name>.sh" }
        ]
      }
    ]
  }
}
```

.claude/hooks/<name>.sh:

```bash
#!/bin/bash
# 목적: <한 문장>
# 판정 불가 시: <통과/차단>과 그 이유
input=$(cat)
# <판정 로직>
# 차단: echo "<이유>" >&2; exit 2
exit 0
```

## 검증

- 계약 검증: 샘플 stdin JSON을 만들어 스크립트를 직접 실행하고 exit code와 출력을 확인한다.
  `echo '{"tool_name":"Bash","tool_input":{"command":"ls"}}' | .claude/hooks/<name>.sh; echo $?`
- 등록 검증: `/hooks`로 등록을 확인하고, 대상 이벤트를 실제로 발생시켜 발화하는지 본다. matcher 비대상 케이스에서 발화하지 않는 것도 확인한다.
- 디버그: `claude --debug`로 훅 실행 로그를 확인한다.
