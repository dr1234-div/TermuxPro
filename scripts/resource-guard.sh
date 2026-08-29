#!/usr/bin/env bash
set -euo pipefail

mode="${1:-normal}"
if [[ "$mode" != "normal" && "$mode" != "heavy" ]]; then
    echo "用法：$0 [normal|heavy]" >&2
    exit 2
fi

read -r _ mem_available_kb < <(awk '/^MemAvailable:/ {print $1, $2}' /proc/meminfo)
cpu_count="$(getconf _NPROCESSORS_ONLN)"
load_one="$(awk '{print $1}' /proc/loadavg)"
disk_available_kb="$(df -Pk . | awk 'NR == 2 {print $4}')"
disk_used_percent="$(df -Pk . | awk 'NR == 2 {gsub(/%/, "", $5); print $5}')"
session_count="$(who 2>/dev/null | awk 'END {print NR + 0}')"

remote=false
if [[ -n "${SSH_CONNECTION:-}" || -z "${DISPLAY:-}${WAYLAND_DISPLAY:-}" || "$session_count" -gt 1 ]]; then
    remote=true
fi

min_memory_kb=$((3 * 1024 * 1024))
min_disk_kb=$((8 * 1024 * 1024))
if [[ "$mode" == "heavy" ]]; then
    min_memory_kb=$((8 * 1024 * 1024))
    min_disk_kb=$((25 * 1024 * 1024))
fi

printf '环境：%s\n' "$([[ "$remote" == true ]] && printf '远程/共享' || printf '本地')"
printf 'CPU：%s 核，1 分钟负载：%s\n' "$cpu_count" "$load_one"
printf '可用内存：%s MiB\n' "$((mem_available_kb / 1024))"
printf '工作区可用磁盘：%s MiB\n' "$((disk_available_kb / 1024))"
printf '工作区磁盘使用率：%s%%\n' "$disk_used_percent"
printf 'KVM：%s\n' "$([[ -r /dev/kvm && -w /dev/kvm ]] && printf '可用' || printf '不可用')"

if (( mem_available_kb < min_memory_kb )); then
    echo "资源守卫拒绝：可用内存低于 $((min_memory_kb / 1024 / 1024)) GiB。" >&2
    exit 3
fi
if (( disk_available_kb < min_disk_kb )); then
    echo "资源守卫拒绝：可用磁盘低于 $((min_disk_kb / 1024 / 1024)) GiB。" >&2
    exit 4
fi
if [[ "$mode" == "heavy" ]] && (( disk_used_percent >= 85 )); then
    echo "资源守卫拒绝：重任务要求磁盘使用率低于 85%。" >&2
    exit 5
fi
if [[ "$mode" == "heavy" ]] && ! awk -v load="$load_one" -v cpu="$cpu_count" 'BEGIN {exit !(load <= cpu * 0.75)}'; then
    echo "资源守卫拒绝：1 分钟负载超过 CPU 核数的 75%。" >&2
    exit 6
fi
if [[ "$mode" == "heavy" && ! -r /dev/kvm ]]; then
    echo "资源守卫拒绝：重型模拟器验收需要可访问的 /dev/kvm。" >&2
    exit 7
fi

echo "资源守卫通过：保持单个重任务，Gradle 使用 --max-workers=2。"
