#!/usr/bin/env python3
# 목적: 세션 모델이 하네스 재검토를 마치지 않은 모델이면 재검토를 요청한다.
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
STATE = os.path.join(HERE, os.pardir, "model-review.txt")

NOTICE = (
    "하네스 재검토 미완료 모델: {model}\n"
    "harness 스킬의 '모델 교체 재검토' 절을 사용자에게 먼저 제안한다. "
    "사용자가 미루면 그대로 진행하고 이번 세션에서 다시 꺼내지 않는다."
)


def normalize(model):
    if isinstance(model, dict):
        model = model.get("id") or model.get("display_name")
    if not isinstance(model, str):
        return None
    # `claude-opus-5[1m]` 등 컨텍스트 변형 접미사는 같은 모델로 본다.
    model = re.sub(r"\[[^\]]*\]$", "", model.strip()).strip()
    return model or None


def model_from_transcripts(transcript_path):
    # SessionStart 페이로드에 model 필드가 없는 빌드가 있어, 직전 세션 기록에서 읽는다.
    if not transcript_path:
        return None
    directory = os.path.dirname(transcript_path)
    try:
        files = [os.path.join(directory, n) for n in os.listdir(directory) if n.endswith(".jsonl")]
    except OSError:
        return None
    files = [f for f in files if os.path.abspath(f) != os.path.abspath(transcript_path)]
    for path in sorted(files, key=os.path.getmtime, reverse=True)[:5]:
        try:
            with open(path, "rb") as f:
                f.seek(0, os.SEEK_END)
                f.seek(max(0, f.tell() - 262144))
                chunk = f.read().decode("utf-8", "ignore")
        except OSError:
            continue
        for line in reversed(chunk.splitlines()):
            try:
                entry = json.loads(line)
            except ValueError:
                continue
            model = normalize((entry.get("message") or {}).get("model"))
            if model:
                return model
    return None


def read_state():
    state = {}
    try:
        with open(STATE, encoding="utf-8") as f:
            for line in f:
                parts = line.split()
                if len(parts) == 2:
                    state[parts[0]] = parts[1]
    except OSError:
        return None
    return state


def write_state(state):
    with open(STATE, "w", encoding="utf-8") as f:
        for model in sorted(state):
            f.write("%s %s\n" % (model, state[model]))


def main():
    try:
        payload = json.load(sys.stdin)
    except Exception:
        return
    model = normalize(payload.get("model")) or model_from_transcripts(payload.get("transcript_path"))
    if not model:
        return

    state = read_state()
    if state is None:
        # 최초 실행: 현재 모델은 하네스가 작성된 모델로 보고 기록만 한다.
        write_state({model: "reviewed"})
        return
    if model not in state:
        state[model] = "pending"
        write_state(state)
    if state[model] != "pending":
        return

    json.dump(
        {
            "hookSpecificOutput": {
                "hookEventName": "SessionStart",
                "additionalContext": NOTICE.format(model=model),
            }
        },
        sys.stdout,
    )


main()
