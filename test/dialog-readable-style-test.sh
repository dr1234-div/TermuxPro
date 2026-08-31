#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
app_sources="$project_dir/app/src/main/java/com/termux/app"
themes_file="$project_dir/app/src/main/res/values/themes.xml"
night_themes_file="$project_dir/app/src/main/res/values-night/themes.xml"
styles_file="$project_dir/app/src/main/res/values/styles.xml"

python3 - "$app_sources" <<'PY'
import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1])
violations = []

for source_path in root.rglob("*.java"):
    if "src/test" in source_path.parts:
        continue
    text = source_path.read_text(encoding="utf-8")
    if source_path.name == "TermuxProDialogStyle.java":
        continue

    direct_builder_show = re.finditer(
        r"new\s+AlertDialog\.Builder\([^;]+?\)\s*\.show\s*\(\s*\)\s*;",
        text,
        flags=re.S,
    )
    for match in direct_builder_show:
        line = text.count("\n", 0, match.start()) + 1
        violations.append(f"{source_path}:{line}: AlertDialog Builder 不能直接 show，必须走 TermuxProDialogStyle.show 或专用可读样式")

    direct_fluent_show = re.finditer(
        r"\.set(?:Positive|Negative|Neutral)Button\([^;]+?\)\s*\.show\s*\(\s*\)\s*;",
        text,
        flags=re.S,
    )
    for match in direct_fluent_show:
        line = text.count("\n", 0, match.start()) + 1
        violations.append(f"{source_path}:{line}: AlertDialog 链式按钮后不能直接 show，必须先 create 并套用产品样式")

if violations:
    print("\n".join(violations), file=sys.stderr)
    sys.exit(1)

PY

if ! grep -Fq 'parent="@android:style/Theme.Material.Dialog.Alert"' "$styles_file"; then
    echo "TermuxPro AlertDialog 必须使用深色 Material AlertDialog 基类。" >&2
    exit 1
fi

for theme_file in "$themes_file" "$night_themes_file"; do
    if ! grep -Fq '<item name="android:alertDialogTheme">@style/TermuxAlertDialogStyle</item>' "$theme_file"; then
        echo "$theme_file 缺少 TermuxPro 弹窗主题绑定。" >&2
        exit 1
    fi
done

echo "弹窗可读样式静态校验通过。"
