package com.termux.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.termux.TermuxConstants;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** 为工作区生成完全经过 POSIX Shell 转义的 SSH/tmux 启动命令。 */
final class WorkspaceCommandBuilder {

    static final String POLICY_SSH_ONLY = "ssh_only";
    static final String POLICY_LIST_SESSIONS = "list_sessions";
    static final String POLICY_ATTACH_SESSION = "attach_session";
    static final String POLICY_CREATE_OR_ATTACH = "create_or_attach";
    static final String TMUX_OWNER_OPTION = "@termuxpro_owner";
    static final String TMUX_WORKSPACE_OPTION = "@termuxpro_workspace";

    static final String CONTROL_PATH = TermuxConstants.TERMUX_TMP_PREFIX_DIR_PATH + "/termuxpro-%C";

    private WorkspaceCommandBuilder() {}

    @NonNull
    static String buildSshCommand(@NonNull String host, int port, @NonNull String path,
                                  @Nullable String cli, @NonNull String policy,
                                  @NonNull String sessionName) {
        if (POLICY_CREATE_OR_ATTACH.equals(policy)) {
            throw new IllegalArgumentException("Managed tmux policy requires a workspace owner token");
        }
        return buildSshCommand(host, port, path, cli, policy, sessionName, "");
    }

    @NonNull
    static String buildSshCommand(@NonNull String host, int port, @NonNull String path,
                                  @Nullable String cli, @NonNull String policy,
                                  @NonNull String sessionName, @NonNull String ownerToken) {
        if (POLICY_CREATE_OR_ATTACH.equals(policy)) requireOwnerToken(ownerToken);
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
            String target = exactTmuxTarget(sessionName);
            remoteCommand += "if ! command -v tmux >/dev/null 2>&1; then "
                + "printf '\\n[TermuxPro] Remote tmux is not installed; opened a normal shell.\\n' >&2; "
                + "exec ${SHELL:-sh}; elif tmux has-session -t " + shellQuote(target)
                + " 2>/dev/null; then exec tmux attach-session -t " + shellQuote(target)
                + "; else printf '\\n[TermuxPro] Configured tmux session does not exist: %s\\n' "
                + shellQuote(sessionName) + " >&2; exec ${SHELL:-sh}; fi";
        } else if (POLICY_CREATE_OR_ATTACH.equals(policy)) {
            String fingerprint = workspaceFingerprint(host, port, path);
            String attachOwned = managedSessionAction("attach-session", ownerToken, fingerprint);
            String newSession = buildCreateManagedTmuxSessionCommand(
                sessionName, cli == null ? null : "exec " + cli, ownerToken, fingerprint);
            remoteCommand += "if ! command -v tmux >/dev/null 2>&1; then "
                + "printf '\\n[TermuxPro] Remote tmux is not installed; opened without session recovery.\\n' >&2; "
                + directCommand + "; else " + resolveTmuxSessionHandle(sessionName)
                + " if [ -n \"$sid\" ]; then " + attachOwned + "; else " + newSession + "; fi; fi";
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

    /**
     * 在明确的 SSH 工作区中执行用户已确认的快捷指令。
     *
     * 命令正文属于用户主动配置的脚本，不与应用生成的控制语句拼接；目标和目录分别转义，并在目录不可用
     * 时直接失败，避免静默退回 HOME 后误执行。
     */
    @NonNull
    static String buildCustomCommandSshCommand(@NonNull String host, int port,
                                               @NonNull String workspacePath,
                                               @NonNull String workingDirectory,
                                               @NonNull String command) {
        if (!SshTargetValidator.isValid(host) || port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid SSH target");
        }
        if (command.trim().isEmpty() || command.length() > 4096
            || CustomCommandValidator.containsPossibleSecret(command)) {
            throw new IllegalArgumentException("Invalid custom command");
        }
        String targetDirectory = workingDirectory.trim().isEmpty()
            ? workspacePath : workingDirectory;
        String remote = "if ! cd -- " + remotePathExpression(targetDirectory)
            + "; then printf '\\n[TermuxPro] Command directory is unavailable: %s\\n' "
            + shellQuote(targetDirectory) + " >&2; exit 2; fi; exec ${SHELL:-sh} -lc "
            + shellQuote(command);
        return "ssh -t -o ControlMaster=auto -o ControlPersist=600 -o ControlPath="
            + shellQuote(CONTROL_PATH)
            + " -o ServerAliveInterval=15 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -p "
            + port + " -- " + shellQuote(host) + " " + shellQuote(remote);
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

    /** 输出供原生 Git 工作台解析的稳定协议，不依赖用户 locale 或 Git 彩色配置。 */
    @NonNull
    static String buildGitOverviewRemoteCommand(@NonNull String path) {
        return "cd -- " + remotePathExpression(path)
            + " && git rev-parse --is-inside-work-tree >/dev/null 2>&1"
            + " && head=$(git symbolic-ref --quiet --short HEAD 2>/dev/null)"
            + " && if [ -n \"$head\" ]; then detached=0; else detached=1; "
            + "head=$(git rev-parse --short HEAD 2>/dev/null || printf 'unborn'); fi"
            + " && changed=$(git status --porcelain=v1 -z | tr -cd '\\000' | wc -c | tr -d ' ')"
            + " && staged=$(git diff --cached --name-only -z | tr -cd '\\000' | wc -c | tr -d ' ')"
            + " && unstaged_tracked=$(git diff --name-only -z | tr -cd '\\000' | wc -c | tr -d ' ')"
            + " && untracked=$(git ls-files --others --exclude-standard -z | tr -cd '\\000' | wc -c | tr -d ' ')"
            + " && unstaged=$((unstaged_tracked + untracked))"
            + " && if counts=$(git rev-list --left-right --count '@{upstream}...HEAD' 2>/dev/null); then "
            + "behind=${counts%%[[:space:]]*}; ahead=${counts##*[[:space:]]}; upstream=1; "
            + "else behind=; ahead=; upstream=0; fi"
            + " && printf 'TP_OVERVIEW\\t%s\\t%s\\t%s\\t%s\\t%s\\t%s\\t%s\\t%s\\n' "
            + "\"$head\" \"$detached\" \"$changed\" \"$staged\" \"$unstaged\" "
            + "\"$ahead\" \"$behind\" \"$upstream\""
            + " && git for-each-ref --sort=-committerdate --format='TP_LOCAL%09%(refname:short)' refs/heads"
            + " && git for-each-ref --sort=-committerdate --format='TP_REMOTE%09%(refname:short)' refs/remotes"
            + " | grep -v '/HEAD$' || true"
            + " && (git log -20 --date=relative --pretty=format:'TP_LOG%x09%h%x09%ar%x09%s'"
            + " 2>/dev/null || true)";
    }

    /** 只允许调用方从已读取的本地分支列表中选择目标；这里仍执行完整 Shell 转义。 */
    @NonNull
    static String buildGitSwitchBranchRemoteCommand(@NonNull String path, @NonNull String branch) {
        if (branch.isEmpty() || branch.indexOf('\n') >= 0 || branch.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Invalid branch");
        }
        return "cd -- " + remotePathExpression(path) + " && git switch -- " + shellQuote(branch);
    }

    /**
     * 从当前 HEAD 创建新的本地分支并切换。
     *
     * 不覆盖已有分支，不推送到远端；分支名先本地保守校验，再交给 Git 自身规则复核。
     */
    @NonNull
    static String buildGitCreateBranchRemoteCommand(@NonNull String path, @NonNull String branch) {
        if (!isSafeGitBranchName(branch)) throw new IllegalArgumentException("Invalid branch");
        return "cd -- " + remotePathExpression(path)
            + " && git check-ref-format --branch " + shellQuote(branch) + " >/dev/null"
            + " && if git show-ref --verify --quiet refs/heads/" + shellQuote(branch)
            + "; then exit 74; fi"
            + " && git switch -c " + shellQuote(branch);
    }

    /** 暂存当前 Git 仓库的全部工作区改动；只改 index，不提交也不推送。 */
    @NonNull
    static String buildGitStageAllRemoteCommand(@NonNull String path) {
        return "cd -- " + remotePathExpression(path)
            + " && git rev-parse --is-inside-work-tree >/dev/null 2>&1"
            + " && root=$(git rev-parse --show-toplevel)"
            + " && cd -- \"$root\""
            + " && unstaged_tracked=$(git diff --name-only -z | tr -cd '\\000' | wc -c | tr -d ' ')"
            + " && untracked=$(git ls-files --others --exclude-standard -z | tr -cd '\\000' | wc -c | tr -d ' ')"
            + " && if [ $((unstaged_tracked + untracked)) -eq 0 ]; then exit 75; fi"
            + " && git add -A -- .";
    }

    /** 取消暂存当前 Git 仓库的全部 staged 改动；保留工作区文件，不执行 reset --hard。 */
    @NonNull
    static String buildGitUnstageAllRemoteCommand(@NonNull String path) {
        return "cd -- " + remotePathExpression(path)
            + " && git rev-parse --is-inside-work-tree >/dev/null 2>&1"
            + " && root=$(git rev-parse --show-toplevel)"
            + " && cd -- \"$root\""
            + " && staged=$(git diff --cached --name-only -z | tr -cd '\\000' | wc -c | tr -d ' ')"
            + " && if [ \"$staged\" -eq 0 ]; then exit 75; fi"
            + " && if git rev-parse --verify HEAD >/dev/null 2>&1; then "
            + "git restore --staged -- .; else git rm -r --cached -- . >/dev/null; fi";
    }

    static boolean isSafeGitBranchName(@NonNull String branch) {
        if (branch.isEmpty() || branch.length() > 128 || branch.startsWith("-")
            || branch.startsWith("/") || branch.endsWith("/") || branch.endsWith(".")
            || branch.endsWith(".lock") || branch.contains("..") || branch.contains("//")
            || branch.contains("@{")) {
            return false;
        }
        for (int index = 0; index < branch.length(); index++) {
            char value = branch.charAt(index);
            if (Character.isISOControl(value) || Character.isWhitespace(value)
                || value == '~' || value == '^' || value == ':' || value == '?'
                || value == '*' || value == '[' || value == '\\') {
                return false;
            }
        }
        return true;
    }

    /**
     * 从用户显式选择的远端跟踪分支创建同名本地跟踪分支并切换。
     *
     * 不自动覆盖已有本地分支；如果脏工作树会被覆盖，Git 自身会拒绝并保留当前工作树。
     */
    @NonNull
    static String buildGitTrackRemoteBranchCommand(@NonNull String path, @NonNull String remoteBranch) {
        if (remoteBranch.isEmpty()
            || remoteBranch.indexOf('\n') >= 0
            || remoteBranch.indexOf('\r') >= 0
            || remoteBranch.endsWith("/HEAD")
            || remoteBranch.indexOf('/') <= 0
            || remoteBranch.startsWith("-")) {
            throw new IllegalArgumentException("Invalid remote branch");
        }
        String localBranch = remoteBranch.substring(remoteBranch.indexOf('/') + 1);
        if (localBranch.isEmpty() || localBranch.startsWith("-")) {
            throw new IllegalArgumentException("Invalid local branch");
        }
        return "cd -- " + remotePathExpression(path)
            + " && if git show-ref --verify --quiet refs/heads/" + shellQuote(localBranch)
            + "; then exit 74; fi"
            + " && git switch --track " + shellQuote(remoteBranch);
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
                                      @NonNull String safeTaskCommand, @NonNull String ownerToken) {
        requireOwnerToken(ownerToken);
        String fingerprint = workspaceFingerprint(host, port, projectPath);
        String sessionName = taskSessionName(projectPath, safeTaskCommand, ownerToken);
        String attachOwned = managedSessionAction("attach-session", ownerToken, fingerprint);
        String create = buildCreateManagedTmuxSessionCommand(
            sessionName, "exec " + safeTaskCommand, ownerToken, fingerprint);
        String tmux = resolveTmuxSessionHandle(sessionName) + " if [ -n \"$sid\" ]; then "
            + attachOwned + "; else " + create + "; fi";
        String remote = "cd -- " + remotePathExpression(projectPath)
            + " && if command -v tmux >/dev/null 2>&1; then " + tmux
            + "; else exec ${SHELL:-sh} -lc " + shellQuote(safeTaskCommand) + "; fi";
        return "ssh -t -o ControlMaster=auto -o ControlPersist=600 -o ControlPath="
            + shellQuote(CONTROL_PATH) + " -o ServerAliveInterval=15 -o ServerAliveCountMax=3 "
            + "-o TCPKeepAlive=yes -p " + port + " -- " + shellQuote(host) + " " + shellQuote(remote);
    }

    /** 同一任务恢复原会话，不同任务允许并行运行。 */
    @NonNull
    static String taskSessionName(@NonNull String projectPath, @NonNull String safeTaskCommand,
                                  @NonNull String ownerToken) {
        requireOwnerToken(ownerToken);
        return "mobile-task-" + sha256(ownerToken).substring(0, 12) + "-"
            + sha256(ownerToken + "\0" + projectPath.trim() + "\0" + safeTaskCommand).substring(0, 24);
    }

    /** 列出应用创建的项目任务会话，不包含普通 tmux 或 AI CLI 会话。 */
    @NonNull
    static String buildListTaskSessionsRemoteCommand(@NonNull String ownerToken) {
        requireOwnerToken(ownerToken);
        return "if command -v tmux >/dev/null 2>&1; then "
            + "tmux list-sessions -F '#{session_id}' 2>/dev/null | "
            + "while IFS= read -r sid; do "
            + "s=$(tmux display-message -p -t \"$sid\" '#{session_name}'); "
            + "case \"$s\" in mobile-task-*) "
            + "w=$(tmux display-message -p -t \"$sid\" '#{session_windows}'); "
            + "a=$(tmux display-message -p -t \"$sid\" '#{session_attached}'); "
            + "c=$(tmux display-message -p -t \"$sid\" '#{session_created}'); "
            + "r=$(tmux display-message -p -t \"$sid\" '#{session_activity}'); "
            + "o=$(tmux show-options -v -t \"$sid\" " + TMUX_OWNER_OPTION + " 2>/dev/null || true); "
            + "f=$(tmux show-options -v -t \"$sid\" " + TMUX_WORKSPACE_OPTION + " 2>/dev/null || true); "
            + "[ \"$o\" = " + shellQuote(ownerToken)
            + " ] && printf '%s\\0%s\\0%s\\0%s\\0%s\\0%s\\0%s\\0' \"$s\" \"$w\" \"$a\" \"$c\" \"$r\" \"$o\" \"$f\";; esac; done; fi";
    }

    /** 列出当前远端 Unix 用户的全部 tmux 会话，不读取窗格内容或命令。 */
    @NonNull
    static String buildListTmuxSessionsRemoteCommand() {
        return "if command -v tmux >/dev/null 2>&1; then "
            + "tmux list-sessions -F '#{session_id}' 2>/dev/null | "
            + "while IFS= read -r sid; do "
            + "s=$(tmux display-message -p -t \"$sid\" '#{session_name}'); "
            + "w=$(tmux display-message -p -t \"$sid\" '#{session_windows}'); "
            + "a=$(tmux display-message -p -t \"$sid\" '#{session_attached}'); "
            + "c=$(tmux display-message -p -t \"$sid\" '#{session_created}'); "
            + "r=$(tmux display-message -p -t \"$sid\" '#{session_activity}'); "
            + "o=$(tmux show-options -v -t \"$sid\" " + TMUX_OWNER_OPTION + " 2>/dev/null || true); "
            + "f=$(tmux show-options -v -t \"$sid\" " + TMUX_WORKSPACE_OPTION + " 2>/dev/null || true); "
            + "printf '%s\\0%s\\0%s\\0%s\\0%s\\0%s\\0%s\\0' \"$s\" \"$w\" \"$a\" \"$c\" \"$r\" \"$o\" \"$f\"; done; "
            + "else printf '" + TmuxSessionParser.MISSING_MARKER + "\\0\\0\\0\\0\\0\\0\\0'; fi";
    }

    @NonNull
    static String buildAttachTaskSessionCommand(@NonNull String host, int port,
                                                 @NonNull String sessionName,
                                                 @Nullable String expectedOwnerToken,
                                                 @NonNull String projectPath) {
        String target = exactTmuxTarget(sessionName);
        String remote;
        if (expectedOwnerToken == null) {
            // 用户已显式选择未知会话；精确附着，但不宣称它属于当前工作区。
            remote = "exec tmux attach-session -t " + shellQuote(target);
        } else {
            requireOwnerToken(expectedOwnerToken);
            remote = resolveTmuxSessionHandle(sessionName) + " [ -n \"$sid\" ] || exit 72; "
                + managedSessionAction("attach-session", expectedOwnerToken,
                workspaceFingerprint(host, port, projectPath));
        }
        return "ssh -t -o ControlMaster=auto -o ControlPersist=600 -o ControlPath="
            + shellQuote(CONTROL_PATH) + " -o ServerAliveInterval=15 -o ServerAliveCountMax=3 "
            + "-o TCPKeepAlive=yes -p " + port + " -- " + shellQuote(host) + " " + shellQuote(remote);
    }

    @NonNull
    static String buildStopTaskSessionRemoteCommand(@NonNull String sessionName,
                                                     @NonNull String expectedOwnerToken,
                                                     @NonNull String host,
                                                     int port,
                                                     @NonNull String projectPath) {
        requireOwnerToken(expectedOwnerToken);
        return resolveTmuxSessionHandle(sessionName) + " [ -n \"$sid\" ] || exit 72; "
            + managedSessionAction("kill-session", expectedOwnerToken,
            workspaceFingerprint(host, port, projectPath));
    }

    /** 新建归属于当前工作区的后台 tmux 会话；同名存在时失败，不会进入或覆盖现有会话。 */
    @NonNull
    static String buildCreateTaskSessionRemoteCommand(@NonNull String sessionName,
                                                       @NonNull String expectedOwnerToken,
                                                       @NonNull String host,
                                                       int port,
                                                       @NonNull String projectPath) {
        requireSessionName(sessionName);
        requireOwnerToken(expectedOwnerToken);
        String target = exactTmuxTarget(sessionName);
        return "command -v tmux >/dev/null 2>&1 || exit 127; cd -- "
            + remotePathExpression(projectPath) + " || exit 2; "
            + "tmux has-session -t " + shellQuote(target) + " 2>/dev/null && exit 74; "
            + "tmux new-session -d -s " + shellQuote(sessionName)
            + " \\; set-option -t " + shellQuote(sessionName) + " " + TMUX_OWNER_OPTION + " "
            + shellQuote(expectedOwnerToken)
            + " \\; set-option -t " + shellQuote(sessionName) + " " + TMUX_WORKSPACE_OPTION + " "
            + shellQuote(workspaceFingerprint(host, port, projectPath));
    }

    /** 仅重命名再次核验为当前工作区所有的会话；未知归属会话没有对应写操作。 */
    @NonNull
    static String buildRenameTaskSessionRemoteCommand(@NonNull String sessionName,
                                                       @NonNull String newName,
                                                       @NonNull String expectedOwnerToken,
                                                       @NonNull String host,
                                                       int port,
                                                       @NonNull String projectPath) {
        requireSessionName(newName);
        requireOwnerToken(expectedOwnerToken);
        String target = exactTmuxTarget(newName);
        return "tmux has-session -t " + shellQuote(target) + " 2>/dev/null && exit 74; "
            + resolveTmuxSessionHandle(sessionName) + " [ -n \"$sid\" ] || exit 72; "
            + managedSessionCommand("rename-session -t '$sid' " + shellQuote(newName),
            expectedOwnerToken, workspaceFingerprint(host, port, projectPath));
    }

    @NonNull
    private static String managedSessionAction(@NonNull String tmuxAction, @NonNull String ownerToken,
                                                @NonNull String workspaceFingerprint) {
        return managedSessionCommand(tmuxAction + " -t '$sid'", ownerToken, workspaceFingerprint);
    }

    @NonNull
    private static String managedSessionCommand(@NonNull String success, @NonNull String ownerToken,
                                                 @NonNull String workspaceFingerprint) {
        String identity = "#{&&:#{==:#{pid},$server_pid},#{==:#{session_created},$created}}";
        String ownership = "#{&&:#{==:#{" + TMUX_OWNER_OPTION + "}," + ownerToken
            + "},#{==:#{" + TMUX_WORKSPACE_OPTION + "}," + workspaceFingerprint + "}}";
        String condition = "#{&&:" + identity + "," + ownership + "}";
        String failure = "run-shell \"printf '[TermuxPro] Refused: session ownership changed.\\n' "
            + ">&2; exit 73\"";
        return "exec tmux if-shell -F -t \"$sid\" \"" + condition + "\" \""
            + success + "\" " + shellQuote(failure);
    }

    @NonNull
    static String buildCreateManagedTmuxSessionCommand(@NonNull String sessionName,
                                                        @Nullable String paneCommand,
                                                        @NonNull String ownerToken,
                                                        @NonNull String workspaceFingerprint) {
        requireOwnerToken(ownerToken);
        StringBuilder command = new StringBuilder("exec tmux new-session -d -s ")
            .append(shellQuote(sessionName))
            .append(" \\; set-option -t ").append(shellQuote(sessionName)).append(" ")
            .append(TMUX_OWNER_OPTION).append(" ").append(shellQuote(ownerToken))
            .append(" \\; set-option -t ").append(shellQuote(sessionName)).append(" ")
            .append(TMUX_WORKSPACE_OPTION).append(" ").append(shellQuote(workspaceFingerprint));
        if (paneCommand != null) {
            command.append(" \\; respawn-pane -k -t ").append(shellQuote(sessionName + ":0.0"))
                .append(" ").append(shellQuote(paneCommand));
        }
        return command.append(" \\; attach-session -t ").append(shellQuote(sessionName)).toString();
    }

    private static void requireOwnerToken(@NonNull String ownerToken) {
        if (!WorkspaceOwnershipStore.isValid(ownerToken)) {
            throw new IllegalArgumentException("Invalid workspace owner token");
        }
    }

    private static void requireSessionName(@NonNull String sessionName) {
        if (!TmuxSessionNameValidator.isValid(sessionName)) {
            throw new IllegalArgumentException("Invalid tmux session name");
        }
    }

    @NonNull
    private static String resolveTmuxSessionHandle(@NonNull String sessionName) {
        return "handle=$(tmux list-sessions -F '#{pid}:#{session_id}:#{session_created}:#{session_name}' "
            + "2>/dev/null | "
            + "while IFS=: read -r candidate_pid candidate_sid candidate_created candidate_name; do "
            + "if [ \"$candidate_name\" = " + shellQuote(sessionName)
            + " ]; then printf '%s' \"$candidate_pid:$candidate_sid:$candidate_created\"; break; fi; done); "
            + "server_pid=${handle%%:*}; remainder=${handle#*:}; sid=${remainder%%:*}; "
            + "remainder=${remainder#*:}; created=${remainder%%:*}; ";
    }

    @NonNull
    static String workspaceFingerprint(@NonNull String host, int port, @NonNull String projectPath) {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid SSH port");
        return sha256(host.trim() + "\0" + port + "\0" + projectPath.trim());
    }

    @NonNull
    private static String sha256(@NonNull String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) hex.append(String.format("%02x", item & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    @NonNull
    private static String exactTmuxTarget(@NonNull String sessionName) {
        return "=" + sessionName;
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
