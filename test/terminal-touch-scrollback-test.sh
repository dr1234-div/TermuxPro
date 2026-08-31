#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source_file="$project_dir/terminal-view/src/main/java/com/termux/view/TerminalView.java"

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
