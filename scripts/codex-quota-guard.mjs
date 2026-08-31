#!/usr/bin/env node

import {createReadStream, existsSync, readdirSync, statSync} from "node:fs";
import {homedir} from "node:os";
import {join} from "node:path";
import {createInterface} from "node:readline";

const sessionsRoot = process.env.CODEX_SESSIONS_ROOT || join(homedir(), ".codex", "sessions");
const threadId = process.env.CODEX_THREAD_ID;
const minimumRemaining = Number(process.env.CODEX_MIN_REMAINING_PERCENT || "15");

function listJsonlFiles(root) {
  if (!existsSync(root)) return [];
  const files = [];
  const stack = [root];
  while (stack.length > 0) {
    const current = stack.pop();
    for (const entry of readdirSync(current)) {
      const path = join(current, entry);
      const stat = statSync(path);
      if (stat.isDirectory()) stack.push(path);
      else if (entry.endsWith(".jsonl")) files.push(path);
    }
  }
  return files;
}

async function readSamples(file) {
  const samples = [];
  const lines = createInterface({input: createReadStream(file), crlfDelay: Infinity});
  for await (const line of lines) {
    if (!line.includes('"type":"token_count"') || !line.includes('"rate_limits"')) continue;
    try {
      const event = JSON.parse(line);
      const primary = event?.payload?.rate_limits?.primary;
      if (!primary || !Number.isFinite(primary.used_percent)) continue;
      samples.push({
        timestamp: event.timestamp,
        usedPercent: primary.used_percent,
        resetsAt: primary.resets_at,
        windowMinutes: primary.window_minutes
      });
    } catch {
      // 忽略并发写入产生的不完整行，等待下一条完整额度事件。
    }
  }
  return samples;
}

if (!threadId || !/^[0-9a-f-]{36}$/.test(threadId)) {
  console.error("缺少有效的 CODEX_THREAD_ID，拒绝混合核算其他 Codex 会话。");
  process.exit(2);
}
if (!Number.isFinite(minimumRemaining) || minimumRemaining < 0 || minimumRemaining > 100) {
  console.error("CODEX_MIN_REMAINING_PERCENT 必须是 0 到 100 之间的数字。");
  process.exit(2);
}

const threadFiles = listJsonlFiles(sessionsRoot).filter(file => file.includes(threadId));
const nested = await Promise.all(threadFiles.map(readSamples));
const samples = nested.flat().filter(sample => Number.isFinite(Date.parse(sample.timestamp)))
  .sort((left, right) => Date.parse(left.timestamp) - Date.parse(right.timestamp));

if (samples.length === 0) {
  console.error("未找到当前线程的 Codex token_count 额度元数据，无法安全核算。");
  process.exit(2);
}

const latest = samples[samples.length - 1];
const remainingPercent = Math.max(0, 100 - latest.usedPercent);
const allowed = remainingPercent >= minimumRemaining;
console.log(JSON.stringify({
  latest: {
    timestamp: latest.timestamp,
    usedPercent: latest.usedPercent,
    remainingPercent,
    resetsAt: latest.resetsAt,
    windowMinutes: latest.windowMinutes
  },
  minimumRemainingPercent: minimumRemaining,
  allowed,
  policy: "不按自然日限额；总剩余额度不少于阈值时持续运行"
}, null, 2));
process.exit(allowed ? 0 : 10);
