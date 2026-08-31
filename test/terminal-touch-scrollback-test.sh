#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source_file="$project_dir/terminal-view/src/main/java/com/termux/view/TerminalView.java"

python3 - "$source_file" <<'PY'
import re
import sys

source_path = sys.argv[1]
source = open(source_path, encoding="utf-8").read()


def require(pattern: str, message: str) -> None:
    if not re.search(pattern, source, flags=re.S):
        print(message, file=sys.stderr)
        sys.exit(1)


require(
    r'public boolean onScroll\(MotionEvent e, float distanceX, float distanceY\).*?'
    r'else \{.*?scrolledWithFinger = true;.*?doScroll\(e, deltaRows, true\);',
    "手指拖动必须直接进入 scrollback，不能走默认 doScroll 或方向键历史。",
)

require(
    r'final boolean forceScrollbackAtStartOfFling = !e2\.isFromSource\(InputDevice\.SOURCE_MOUSE\);',
    "手指惯性滑动必须以非鼠标事件作为 scrollback 判断来源。",
)

require(
    r'doScroll\(e2, diff, forceScrollbackAtStartOfFling\);',
    "手指惯性滑动必须把 scrollback 策略传入 doScroll。",
)

require(
    r'if \(!forceScrollback && mEmulator\.isMouseTrackingActive\(\)\).*?'
    r'else if \(!forceScrollback && mEmulator\.isAlternateBufferActive\(\)\).*?'
    r'handleKeyCode\(up \? KeyEvent\.KEYCODE_DPAD_UP : KeyEvent\.KEYCODE_DPAD_DOWN, 0\);',
    "只有非触摸 scrollback 才允许在 alternate screen 中转换为方向键。",
)
PY

if ! grep -Fq 'doScroll(e, deltaRows, true);' "$source_file"; then
    echo "触摸滑动必须强制滚动 scrollback，不能调用默认 doScroll。" >&2
    exit 1
fi

if ! grep -Fq 'final boolean forceScrollbackAtStartOfFling = !e2.isFromSource(InputDevice.SOURCE_MOUSE);' "$source_file"; then
    echo "触摸 fling 必须使用 scrollback 范围，不能沿用鼠标追踪范围。" >&2
    exit 1
fi

if ! grep -Fq 'void doScroll(MotionEvent event, int rowsDown, boolean forceScrollback)' "$source_file"; then
    echo "doScroll 必须显式区分触摸 scrollback 与鼠标/程序内滚动。" >&2
    exit 1
fi

if ! grep -Fq 'if (!forceScrollback && mEmulator.isMouseTrackingActive())' "$source_file"; then
    echo "触摸 scrollback 不应被 mouse tracking 截获。" >&2
    exit 1
fi

if ! grep -Fq '} else if (!forceScrollback && mEmulator.isAlternateBufferActive())' "$source_file"; then
    echo "触摸 scrollback 不应在 alternate screen 中转成上下键。" >&2
    exit 1
fi

if ! grep -Fq 'mEmulator.mRows, true);' "$source_file"; then
    echo "Shift+Page 也必须强制滚动 scrollback。" >&2
    exit 1
fi

echo "终端触摸 scrollback 策略校验通过。"
