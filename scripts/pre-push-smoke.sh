#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

base_ref="${1:-origin/dev}"
dry_run="${TERMUXPRO_DRY_RUN:-0}"

collect_changed_files() {
    {
        git diff --name-only "$base_ref"...HEAD 2>/dev/null || true
        git diff --name-only --cached
        git diff --name-only
        git ls-files --others --exclude-standard
    } | awk 'NF && !seen[$0]++'
}

mapfile -t changed_files < <(collect_changed_files)

if [[ "${#changed_files[@]}" -eq 0 ]]; then
    echo "推送前冒烟：没有发现相对 $base_ref 的变更。"
    exit 0
fi

echo "推送前冒烟：基线 $base_ref，变更文件 ${#changed_files[@]} 个。"

declare -A tests=()
add_test() {
    tests["$1"]=1
}

needs_android_compile=0
for file in "${changed_files[@]}"; do
    case "$file" in
        app/src/main/res/*|app/src/main/java/com/termux/app/WorkspaceActivity.java|app/src/main/java/com/termux/app/AiSessionDialog.java)
            add_test "com.termux.app.WorkspaceActivitySmokeTest"
            add_test "com.termux.app.CustomLayoutsSmokeTest"
            needs_android_compile=1
            ;;
        app/src/main/AndroidManifest.xml|app/src/main/res/mipmap-*|app/src/main/res/drawable*/ic_launcher*)
            add_test "com.termux.app.ManifestProductIdentityTest"
            needs_android_compile=1
            ;;
        app/src/main/java/com/termux/app/*Git*|app/src/main/java/com/termux/app/Git*.java|app/src/main/java/com/termux/app/WorkspaceCommandBuilder.java)
            add_test "com.termux.app.GitDiffActivityTest"
            add_test "com.termux.app.GitRepositoryOverviewTest"
            add_test "com.termux.app.WorkspaceCommandBuilderTest"
            needs_android_compile=1
            ;;
        app/src/main/java/com/termux/app/*Tmux*|app/src/main/java/com/termux/app/TaskSessionsActivity.java)
            add_test "com.termux.app.TaskSessionsActivityTest"
            add_test "com.termux.app.TmuxSessionParserTest"
            add_test "com.termux.app.TmuxSessionNameValidatorTest"
            add_test "com.termux.app.WorkspaceCommandBuilderTest"
            needs_android_compile=1
            ;;
        app/src/main/java/com/termux/app/ProjectTask*|app/src/main/java/com/termux/app/ProjectTasksActivity.java)
            add_test "com.termux.app.ProjectTaskDetectorTest"
            add_test "com.termux.app.RemoteToolRecoveryTest"
            add_test "com.termux.app.WorkspaceCommandBuilderTest"
            needs_android_compile=1
            ;;
        app/src/main/java/com/termux/app/CustomCommand*|app/src/main/java/com/termux/app/CustomCommandsActivity.java)
            add_test "com.termux.app.CustomCommandsActivityTest"
            add_test "com.termux.app.CustomCommandStoreTest"
            add_test "com.termux.app.WorkspaceCommandBuilderTest"
            needs_android_compile=1
            ;;
        app/src/main/java/com/termux/app/RemoteFilesNavigation.java|app/src/main/java/com/termux/app/RemoteFilesActivity.java)
            add_test "com.termux.app.RemoteFilesNavigationTest"
            add_test "com.termux.app.RemoteToolRecoveryTest"
            add_test "com.termux.app.WorkspaceCommandBuilderTest"
            needs_android_compile=1
            ;;
        app/src/main/java/com/termux/app/WebPreviewNavigation.java|app/src/main/java/com/termux/app/WorkspaceTarget.java|app/src/main/java/com/termux/app/WorkspaceTargetStore.java)
            add_test "com.termux.app.WebPreviewNavigationTest"
            add_test "com.termux.app.WorkspaceTargetStoreTest"
            add_test "com.termux.app.WorkspaceCommandBuilderTest"
            needs_android_compile=1
            ;;
        app/src/main/java/com/termux/app/ConnectionDiagnosticNavigation.java|app/src/main/java/com/termux/app/ConnectionDiagnosticActivity.java)
            add_test "com.termux.app.ConnectionDiagnosticNavigationTest"
            add_test "com.termux.app.ConnectionDiagnosticReportTest"
            add_test "com.termux.app.RemoteToolRecoveryTest"
            add_test "com.termux.app.WorkspaceCommandBuilderTest"
            needs_android_compile=1
            ;;
        app/src/main/java/com/termux/app/SshKeysNavigation.java|app/src/main/java/com/termux/app/SshKeysActivity.java)
            add_test "com.termux.app.SshKeysNavigationTest"
            add_test "com.termux.app.RemoteToolRecoveryTest"
            needs_android_compile=1
            ;;
        app/src/main/java/com/termux/app/Terminal*|terminal-view/*|terminal-emulator/*)
            add_test "com.termux.app.TerminalProjectToolsMenuTest"
            add_test "com.termux.app.TerminalTouchScrollPolicyTest"
            add_test "com.termux.app.terminal.TermuxTerminalSessionActivityClientTest"
            needs_android_compile=1
            ;;
        app/src/main/java/com/termux/app/*Command*|app/src/main/java/com/termux/app/Ai*|app/src/main/java/com/termux/app/Remote*)
            add_test "com.termux.app.AiCliSessionCenterActivityTest"
            add_test "com.termux.app.AiCliLaunchCommandTest"
            add_test "com.termux.app.AiTerminalActionTest"
            add_test "com.termux.app.RemoteToolRecoveryTest"
            add_test "com.termux.app.WorkspaceCommandBuilderTest"
            needs_android_compile=1
            ;;
        app/src/main/java/*|app/src/test/java/*)
            needs_android_compile=1
            ;;
    esac
done

static_checks=(
    "git diff --check"
    "./scripts/validate-skills.sh"
    "./test/version-metadata-test.sh"
    "./test/workflow-trigger-policy-test.sh"
    "./test/github-cli-wrapper-test.sh"
    "./test/github-check-suites-test.sh"
)

for check in "${static_checks[@]}"; do
    if [[ "$dry_run" == "1" ]]; then
        echo "DRY-RUN static: $check"
    else
        bash -lc "$check"
    fi
done

if (( needs_android_compile == 0 && "${#tests[@]}" == 0 )); then
    echo "推送前冒烟：本轮只涉及文档/脚本等非 Android 运行时代码，静态门禁已完成。"
    exit 0
fi

gradle_args=(--no-daemon --max-workers=2)
if [[ "${TERMUXPRO_OFFLINE:-0}" == "1" ]]; then
    gradle_args+=(--offline)
fi

if [[ "${#tests[@]}" -gt 0 ]]; then
    mapfile -t sorted_tests < <(printf '%s\n' "${!tests[@]}" | sort)
    test_args=()
    for test_name in "${sorted_tests[@]}"; do
        test_args+=(--tests "$test_name")
    done
    gradle_cmd=(./gradlew "${gradle_args[@]}" :app:testDebugUnitTest "${test_args[@]}")
else
    gradle_cmd=(./gradlew "${gradle_args[@]}" :app:compileDebugJavaWithJavac)
fi

if [[ "$dry_run" == "1" ]]; then
    printf 'DRY-RUN gradle:'
    printf ' %q' "${gradle_cmd[@]}"
    printf '\n'
else
    ./scripts/resource-guard.sh normal
    # shellcheck source=/dev/null
    source "$project_dir/scripts/resolve-jdk17.sh"
    TERMUXPRO_USE_CHINA_MIRROR="${TERMUXPRO_USE_CHINA_MIRROR:-1}" "${gradle_cmd[@]}"
fi
