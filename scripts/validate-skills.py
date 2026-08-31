#!/usr/bin/env python3
"""验证 TermuxPro 的 Codex/Claude Skill 元数据与共用入口。"""

import os
from pathlib import Path
import sys

try:
    import yaml
except ModuleNotFoundError:
    # 直接执行本文件时也自动使用仓库级依赖，不修改系统 Python。
    bootstrap = Path(__file__).resolve().with_name("validate-skills.sh")
    os.execv(str(bootstrap), [str(bootstrap)])


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

# 对额度门禁关键语义做最低限度的回归校验，避免再次引入按日暂停或人工恢复。
quota_rules = {
    "不再按自然日核算或限制迭代额度": "仍缺少取消每日额度的明确约束",
    "总剩余额度大于或等于 15%": "缺少持续运行阈值",
    "总剩余额度低于 15%": "缺少暂停阈值",
    "不要求用户恢复": "缺少无人值守持续运行要求",
    "不得主动把 Goal 标记为 `blocked`": "缺少额度暂停不得阻塞 Goal 的约束",
}
common_skill_text = SKILLS[0].read_text(encoding="utf-8")
for phrase, message in quota_rules.items():
    if phrase not in common_skill_text:
        fail(message)

agents_text = (ROOT / "AGENTS.md").read_text(encoding="utf-8")
for phrase in ("不再按自然日计算或限制", "大于或等于 15%", "低于 15%", "不要求用户恢复"):
    if phrase not in agents_text:
        fail(f"AGENTS.md 未固化额度规则：{phrase}")

print("TermuxPro Skills 校验通过。")
