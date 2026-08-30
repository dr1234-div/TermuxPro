#!/usr/bin/env python3
"""验证 TermuxPro 的 Codex/Claude Skill 元数据与共用入口。"""

from pathlib import Path
import sys

import yaml


ROOT = Path(__file__).resolve().parent.parent
SKILLS = (
    ROOT / ".agents/skills/termuxpro-development/SKILL.md",
    ROOT / ".claude/skills/termuxpro-development/SKILL.md",
)


def fail(message: str) -> None:
    print(f"Skill 校验失败：{message}", file=sys.stderr)
    raise SystemExit(1)


for skill in SKILLS:
    if not skill.is_file():
        fail(f"缺少 {skill.relative_to(ROOT)}")
    text = skill.read_text(encoding="utf-8")
    if not text.startswith("---\n"):
        fail(f"{skill.relative_to(ROOT)} 缺少 YAML frontmatter")
    try:
        _, frontmatter, body = text.split("---", 2)
        metadata = yaml.safe_load(frontmatter)
    except (ValueError, yaml.YAMLError) as error:
        fail(f"{skill.relative_to(ROOT)} frontmatter 无效：{error}")
    if not isinstance(metadata, dict):
        fail(f"{skill.relative_to(ROOT)} frontmatter 必须是对象")
    for key in ("name", "description"):
        if not isinstance(metadata.get(key), str) or not metadata[key].strip():
            fail(f"{skill.relative_to(ROOT)} 缺少 {key}")
    if not body.strip():
        fail(f"{skill.relative_to(ROOT)} 正文为空")

claude_text = SKILLS[1].read_text(encoding="utf-8")
if ".agents/skills/termuxpro-development/SKILL.md" not in claude_text:
    fail("Claude Skill 未指向共用 Skill")

print("TermuxPro Skills 校验通过。")
