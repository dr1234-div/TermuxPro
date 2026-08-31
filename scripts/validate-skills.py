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

# 额度规则曾因“每日额度”和“平台总额度窗口”混淆而导致持续 Goal 被错误阻塞。
# 对关键语义做最低限度的回归校验，避免后续维护时再次退化为需要人工恢复。
quota_rules = {
    "每个自然日独立拥有最多 10%": "缺少每日独立额度定义",
    "跨入下一个自然日": "缺少跨日续跑定义",
    "不要求用户恢复": "缺少无人值守自动恢复要求",
    "总剩余额度门禁": "缺少总剩余额度独立门禁",
    "不得因此把 Goal 标记为 `blocked`": "缺少每日额度不得阻塞 Goal 的约束",
}
common_skill_text = SKILLS[0].read_text(encoding="utf-8")
for phrase, message in quota_rules.items():
    if phrase not in common_skill_text:
        fail(message)

agents_text = (ROOT / "AGENTS.md").read_text(encoding="utf-8")
for phrase in ("每个自然日独立拥有最多 10%", "跨入下一个自然日", "不要求用户恢复", "总剩余额度"):
    if phrase not in agents_text:
        fail(f"AGENTS.md 未固化额度规则：{phrase}")

print("TermuxPro Skills 校验通过。")
