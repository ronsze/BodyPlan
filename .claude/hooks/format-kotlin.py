#!/usr/bin/env python3
# 목적: Kotlin 파일을 편집한 직후 ktlint 포맷을 적용해 표기 규칙을 강제한다.
import json
import os
import subprocess
import sys

SUFFIXES = (".kt", ".kts")


def main():
    try:
        payload = json.load(sys.stdin)
    except Exception:
        return

    path = (payload.get("tool_input") or {}).get("file_path") or ""
    if not path.endswith(SUFFIXES):
        return

    project = os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
    result = subprocess.run(
        [os.path.join(project, "gradlew"), "spotlessApply", "-q"],
        cwd=project,
        capture_output=True,
        text=True,
    )
    if result.returncode == 0:
        return

    # 포맷으로 고칠 수 없는 위반(네이밍·와일드카드 import 등)은 모델이 직접 고쳐야 한다.
    sys.stderr.write(
        "spotlessApply 실패 — ktlint 위반을 고쳐야 한다.\n"
        + (result.stdout or "")[-2000:]
        + (result.stderr or "")[-2000:]
    )
    sys.exit(2)


main()
