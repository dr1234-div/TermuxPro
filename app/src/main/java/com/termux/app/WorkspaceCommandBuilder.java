package com.termux.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.termux.TermuxConstants;

/** 为工作区生成完全经过 POSIX Shell 转义的 SSH/tmux 启动命令。 */
final class WorkspaceCommandBuilder {

    static final String POLICY_SSH_ONLY = "ssh_only";
    static final String POLICY_LIST_SESSIONS = "list_sessions";
    static final String POLICY_ATTACH_SESSION = "attach_session";
    static final String POLICY_CREATE_OR_ATTACH = "create_or_attach";

    static final String CONTROL_PATH = TermuxConstants.TERMUX_TMP_PREFIX_DIR_PATH + "/termuxpro-%C";

    private WorkspaceCommandBuilder() {}

    @NonNull
    static String buildSshCommand(@NonNull String host, int port, @NonNull String path,
                                  @Nullable String cli, @NonNull String policy,
                                  @NonNull String sessionName) {
        String directCommand = cli == null ? "exec ${SHELL:-sh}" : "exec " + cli;
        String remoteCommand = "if ! cd -- " + remotePathExpression(path)
            + "; then printf '\\n[TermuxPro] SSH authenticated, but the workspace path is unavailable: %s\\n' "
            + shellQuote(path) + " >&2; exec ${SHELL:-sh}; fi; "
            + "printf '\\n[TermuxPro] SSH authenticated; workspace ready: %s\\n' "
            + shellQuote(path) + "; ";

        if (POLICY_LIST_SESSIONS.equals(policy)) {
            remoteCommand += "if command -v tmux >/dev/null 2>&1; then "
                + "printf '\\n[TermuxPro] Available tmux sessions (not attached):\\n'; "
                + "tmux list-sessions 2>/dev/null || printf '[TermuxPro] No tmux sessions found.\\n'; "
                + "else printf '\\n[TermuxPro] Remote tmux is not installed.\\n' >&2; fi; "
                + directCommand;
        } else if (POLICY_ATTACH_SESSION.equals(policy)) {
            remoteCommand += "if ! command -v tmux >/dev/null 2>&1; then "
                + "printf '\\n[TermuxPro] Remote tmux is not installed; opened a normal shell.\\n' >&2; "
                + "exec ${SHELL:-sh}; elif tmux has-session -t " + shellQuote(sessionName)
                + " 2>/dev/null; then exec tmux attach-session -t " + shellQuote(sessionName)
                + "; else printf '\\n[TermuxPro] Configured tmux session does not exist: %s\\n' "
                + shellQuote(sessionName) + " >&2; exec ${SHELL:-sh}; fi";
        } else if (POLICY_CREATE_OR_ATTACH.equals(policy)) {
            String newSession = "tmux new-session -s " + shellQuote(sessionName);
            if (cli != null) newSession += " " + shellQuote("exec " + cli);
            remoteCommand += "if ! command -v tmux >/dev/null 2>&1; then "
                + "printf '\\n[TermuxPro] Remote tmux is not installed; opened without session recovery.\\n' >&2; "
                + directCommand + "; elif tmux has-session -t " + shellQuote(sessionName)
                + " 2>/dev/null; then exec tmux attach-session -t " + shellQuote(sessionName)
                + "; else exec " + newSession + "; fi";
        } else {
            // 安全默认值：只建立 SSH，不探测、不创建、不进入任何 tmux 会话。
            remoteCommand += directCommand;
        }

        return "ssh -t -o ControlMaster=auto -o ControlPersist=600 -o ControlPath="
            + shellQuote(CONTROL_PATH)
            + " -o ServerAliveInterval=15 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -p "
            + port + " -- " + shellQuote(host) + " " + shellQuote(remoteCommand);
    }

    /** 构造仅绑定手机 loopback 的 SSH 本地端口转发，禁止暴露到局域网。 */
    @NonNull
    static String buildPortForwardCommand(@NonNull String host, int sshPort, int localPort, int remotePort) {
        String forwarding = "127.0.0.1:" + localPort + ":127.0.0.1:" + remotePort;
        return "ssh -N -T -o ExitOnForwardFailure=yes -o ServerAliveInterval=15 "
            + "-o ServerAliveCountMax=3 -o TCPKeepAlive=yes -p " + sshPort + " -L " + shellQuote(forwarding)
            + " -- " + shellQuote(host);
    }

    /** 为原生审查页面构造只读 Git 命令，路径按远端 Shell 规则安全转义。 */
    @NonNull
    static String buildGitDiffRemoteCommand(@NonNull String path) {
        return "cd -- " + remotePathExpression(path)
            + " && git status --short --branch && printf '\\n--- UNSTAGED DIFF ---\\n'"
            + " && git diff --no-ext-diff --no-color --stat"
            + " && git diff --no-ext-diff --no-color"
            + " && printf '\\n--- STAGED DIFF ---\\n'"
            + " && git diff --cached --no-ext-diff --no-color --stat"
            + " && git diff --cached --no-ext-diff --no-color";
    }

    /** 列出项目内单层目录，使用 NUL 分隔以支持空格、Tab 和换行文件名。 */
    @NonNull
    static String buildListFilesRemoteCommand(@NonNull String projectPath, @NonNull String relativeDirectory) {
        return "cd -- " + remotePathExpression(projectPath)
            + " && find -- " + shellQuote(relativeDirectory)
            + " -mindepth 1 -maxdepth 1 ! -name .git -printf '%y\\0%f\\0'";
    }

    /** 只读预览文本文件；二进制文件只返回标记，不读取正文。 */
    @NonNull
    static String buildReadFileRemoteCommand(@NonNull String projectPath, @NonNull String relativeFile) {
        String file = shellQuote(relativeFile);
        return "cd -- " + remotePathExpression(projectPath)
            + " && if [ ! -r " + file + " ]; then printf 'ERROR\\0'; exit 2; "
            + "elif [ ! -s " + file + " ] || LC_ALL=C grep -Iq . -- " + file
            + "; then printf 'TEXT\\0'; head -c 1000000 -- "
            + file + "; else printf 'BINARY\\0'; fi";
    }

    /** 读取有限的项目描述文件，不执行项目代码。 */
    @NonNull
    static String buildProjectMetadataCommand(@NonNull String projectPath) {
        return "cd -- " + remotePathExpression(projectPath)
            + " && if [ -f package.json ]; then "
            + "if [ -f pnpm-lock.yaml ]; then manager=pnpm; elif [ -f yarn.lock ]; then manager=yarn; "
            + "elif [ -f bun.lock ] || [ -f bun.lockb ]; then manager=bun; else manager=npm; fi; "
            + "printf 'PACKAGE_JSON\\0%s\\0' \"$manager\"; head -c 500000 -- package.json; "
            + "elif [ -f pom.xml ]; then if [ -x ./mvnw ]; then printf 'MAVEN_WRAPPER\\0'; "
            + "else printf 'MAVEN\\0'; fi; "
            + "elif [ -x ./gradlew ]; then printf 'GRADLE_WRAPPER\\0'; "
            + "elif [ -f build.gradle ] || [ -f build.gradle.kts ]; then printf 'GRADLE\\0'; "
            + "else printf 'UNKNOWN\\0'; fi";
    }

    /** 检查远端开发环境，只返回版本/可用性信息，不修改服务器。 */
    @NonNull
    static String buildConnectionDiagnosticCommand(@NonNull String projectPath) {
        StringBuilder command = new StringBuilder();
        command.append("printf 'SYSTEM\\0%s\\0' \"$(uname -srm 2>/dev/null || printf unknown)\"; ");
        command.append("if cd -- ").append(remotePathExpression(projectPath))
            .append(" 2>/dev/null; then printf 'PROJECT\\0OK\\0'; else printf 'PROJECT\\0MISSING\\0'; fi; ");
        String[] tools = {"tmux", "git", "node", "java", "claude", "codex"};
        for (String tool : tools) {
            command.append("if command -v ").append(tool)
                .append(" >/dev/null 2>&1; then printf '")
                .append(tool.toUpperCase()).append("\\0OK\\0'; else printf '")
                .append(tool.toUpperCase()).append("\\0MISSING\\0'; fi; ");
        }
        command.append("printf 'SHELL\\0%s\\0' \"${SHELL:-unknown}\"");
        return command.toString();
    }

    /** 由官方 OpenSSH 交互生成 Ed25519 密钥，口令不会经过应用 UI 或持久化。 */
    @NonNull
    static String buildGenerateSshKeyCommand() {
        return "umask 077 && mkdir -p \"$HOME/.ssh\" && chmod 700 \"$HOME/.ssh\" "
            + "&& ssh-keygen -t ed25519 -a 64 -f \"$HOME/.ssh/id_ed25519\"";
    }

    /** 将本机公钥安装到经校验的 OpenSSH 目标，认证过程留在交互终端。 */
    @NonNull
    static String buildCopySshKeyCommand(@NonNull String host, int port) {
        return "ssh-copy-id -p " + port + " -- " + shellQuote(host);
    }

    /** 在单独的远端 tmux 任务会话运行经过结构化生成的项目命令。 */
    @NonNull
    static String buildSshTaskCommand(@NonNull String host, int port, @NonNull String projectPath,
                                      @NonNull String safeTaskCommand) {
        String tmux = "tmux new-session -A -s " + shellQuote(taskSessionName(safeTaskCommand))
            + " " + shellQuote(safeTaskCommand);
        String remote = "cd -- " + remotePathExpression(projectPath)
            + " && if command -v tmux >/dev/null 2>&1; then exec " + tmux
            + "; else exec ${SHELL:-sh} -lc " + shellQuote(safeTaskCommand) + "; fi";
        return "ssh -t -o ControlMaster=auto -o ControlPersist=600 -o ControlPath="
            + shellQuote(CONTROL_PATH) + " -o ServerAliveInterval=15 -o ServerAliveCountMax=3 "
            + "-o TCPKeepAlive=yes -p " + port + " -- " + shellQuote(host) + " " + shellQuote(remote);
    }

    /** 同一任务恢复原会话，不同任务允许并行运行。 */
    @NonNull
    static String taskSessionName(@NonNull String safeTaskCommand) {
        return "mobile-task-" + String.format("%08x", safeTaskCommand.hashCode());
    }

    /** 列出应用创建的项目任务会话，不包含普通 tmux 或 AI CLI 会话。 */
    @NonNull
    static String buildListTaskSessionsRemoteCommand() {
        return "if command -v tmux >/dev/null 2>&1; then "
            + "tmux list-sessions -F '#{session_name}' 2>/dev/null | while IFS= read -r s; do "
            + "case \"$s\" in mobile-task-*) "
            + "w=$(tmux display-message -p -t \"$s\" '#{session_windows}'); "
            + "a=$(tmux display-message -p -t \"$s\" '#{session_attached}'); "
            + "printf '%s\\0%s\\0%s\\0' \"$s\" \"$w\" \"$a\";; esac; done; fi";
    }

    @NonNull
    static String buildAttachTaskSessionCommand(@NonNull String host, int port,
                                                 @NonNull String sessionName) {
        String remote = "exec tmux attach-session -t " + shellQuote(sessionName);
        return "ssh -t -o ControlMaster=auto -o ControlPersist=600 -o ControlPath="
            + shellQuote(CONTROL_PATH) + " -o ServerAliveInterval=15 -o ServerAliveCountMax=3 "
            + "-o TCPKeepAlive=yes -p " + port + " -- " + shellQuote(host) + " " + shellQuote(remote);
    }

    @NonNull
    static String buildStopTaskSessionRemoteCommand(@NonNull String sessionName) {
        return "tmux kill-session -t " + shellQuote(sessionName);
    }

    /** 仅允许开头的 ~/ 展开为远端 HOME，其余内容仍按普通参数转义。 */
    @NonNull
    private static String remotePathExpression(@NonNull String path) {
        if ("~".equals(path)) return "\"$HOME\"";
        if (path.startsWith("~/")) return "\"$HOME\"/" + shellQuote(path.substring(2));
        return shellQuote(path);
    }

    /** POSIX Shell 单引号转义，防止工作区字段被解释成额外命令。 */
    @NonNull
    static String shellQuote(@NonNull String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
