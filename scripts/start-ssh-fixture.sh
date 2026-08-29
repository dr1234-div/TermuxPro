#!/usr/bin/env bash
set -euo pipefail

if [[ "${CI:-}" != "true" || -z "${RUNNER_TEMP:-}" || -z "${GITHUB_ENV:-}" ]]; then
    echo "SSH fixture 仅允许在隔离的 GitHub Actions Runner 中启动。" >&2
    exit 2
fi

missing=()
command -v sshd >/dev/null 2>&1 || missing+=(openssh-server)
command -v tmux >/dev/null 2>&1 || missing+=(tmux)
if (( ${#missing[@]} > 0 )); then
    sudo apt-get update -qq
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends "${missing[@]}"
fi

fixture_dir="$RUNNER_TEMP/termuxpro-ssh-fixture"
install -d -m 700 "$fixture_dir"
ssh-keygen -q -t ed25519 -N '' -f "$fixture_dir/client_key"
ssh-keygen -q -t ed25519 -N '' -f "$fixture_dir/host_key"
cp "$fixture_dir/client_key.pub" "$fixture_dir/authorized_keys"
chmod 600 "$fixture_dir/client_key" "$fixture_dir/authorized_keys"

port=''
for candidate in $(seq 32222 32242); do
    if ! ss -ltnH | awk '{print $4}' | grep -Eq "(^|:)$candidate$"; then
        port="$candidate"
        break
    fi
done
if [[ -z "$port" ]]; then
    echo "无法为 SSH fixture 分配回环端口。" >&2
    exit 3
fi

cat > "$fixture_dir/sshd_config" <<EOF
Port $port
ListenAddress 127.0.0.1
HostKey $fixture_dir/host_key
PidFile $fixture_dir/sshd.pid
AuthorizedKeysFile $fixture_dir/authorized_keys
StrictModes no
PasswordAuthentication no
KbdInteractiveAuthentication no
ChallengeResponseAuthentication no
UsePAM no
PermitRootLogin no
AllowUsers $USER
PrintMotd no
LogLevel VERBOSE
EOF

sudo install -d -m 755 /run/sshd
sudo /usr/sbin/sshd -t -f "$fixture_dir/sshd_config"
sudo /usr/sbin/sshd -D -f "$fixture_dir/sshd_config" -E "$fixture_dir/sshd.log" &
for attempt in $(seq 1 40); do
    if timeout 1 bash -c "</dev/tcp/127.0.0.1/$port" 2>/dev/null; then
        break
    fi
    if [[ "$attempt" == 40 ]]; then
        cat "$fixture_dir/sshd.log" >&2 || true
        echo "SSH fixture 启动超时。" >&2
        exit 4
    fi
    sleep 0.25
done

ssh-keyscan -p "$port" 127.0.0.1 > "$fixture_dir/known_hosts" 2>/dev/null
{
    echo "TERMUXPRO_SSH_FIXTURE_TARGET=$USER@127.0.0.1"
    echo "TERMUXPRO_SSH_FIXTURE_PORT=$port"
    echo "TERMUXPRO_SSH_FIXTURE_CLIENT=$(command -v ssh)"
    echo "TERMUXPRO_SSH_FIXTURE_IDENTITY=$fixture_dir/client_key"
    echo "TERMUXPRO_SSH_FIXTURE_KNOWN_HOSTS=$fixture_dir/known_hosts"
} >> "$GITHUB_ENV"

echo "隔离 SSH/tmux fixture 已启动：127.0.0.1:$port"
