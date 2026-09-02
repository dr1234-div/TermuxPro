package com.termux.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

/**
 * AI CLI 会话中心。
 *
 * 该页面只整理 TermuxPro 增值层上下文，不读取 Claude/Codex 私有历史，也不自动进入 tmux。
 * 真正启动命令仍由工作台或终端中的显式安全弹窗完成。
 */
public final class AiCliSessionCenterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_cli_session_center);

        findViewById(R.id.ai_cli_center_back).setOnClickListener(view -> finish());
        findViewById(R.id.ai_cli_center_open_workspace).setOnClickListener(view ->
            startActivity(new Intent(this, WorkspaceActivity.class)));
        findViewById(R.id.ai_cli_center_open_templates).setOnClickListener(view ->
            startActivity(new Intent(this, CustomCommandsActivity.class)));
        findViewById(R.id.ai_cli_center_open_tmux).setOnClickListener(view -> openTmuxSessions());
        findViewById(R.id.ai_cli_center_open_git).setOnClickListener(view -> openGitWorkbench());
        findViewById(R.id.ai_cli_center_claude_new).setOnClickListener(view ->
            launchAiCli(AiCliLaunchCommand.Tool.CLAUDE, AiCliLaunchCommand.Mode.NEW_SESSION));
        findViewById(R.id.ai_cli_center_claude_history).setOnClickListener(view ->
            launchAiCli(AiCliLaunchCommand.Tool.CLAUDE, AiCliLaunchCommand.Mode.PICK_HISTORY));
        findViewById(R.id.ai_cli_center_codex_new).setOnClickListener(view ->
            launchAiCli(AiCliLaunchCommand.Tool.CODEX, AiCliLaunchCommand.Mode.NEW_SESSION));
        findViewById(R.id.ai_cli_center_codex_history).setOnClickListener(view ->
            launchAiCli(AiCliLaunchCommand.Tool.CODEX, AiCliLaunchCommand.Mode.PICK_HISTORY));

        bindTarget();
        bindCommands();
    }

    private void bindTarget() {
        TextView target = findViewById(R.id.ai_cli_center_target);
        TextView detail = findViewById(R.id.ai_cli_center_target_detail);
        WorkspaceTarget workspace = WorkspaceTargetStore.readActive(this);
        if (workspace == null || workspace.host == null || workspace.host.trim().isEmpty()
            || workspace.port < 1 || workspace.path == null || workspace.path.trim().isEmpty()) {
            target.setText(R.string.ai_cli_center_target_missing);
            detail.setText(R.string.ai_cli_center_target_missing_detail);
            return;
        }
        target.setText(workspace.name);
        detail.setText(getString(R.string.ai_cli_center_target_detail,
            workspace.host, workspace.port, workspace.path));
    }

    private void bindCommands() {
        ((TextView) findViewById(R.id.ai_cli_center_claude_commands)).setText(
            getString(R.string.ai_cli_center_command_pair,
                AiCliLaunchCommand.command(AiCliLaunchCommand.Tool.CLAUDE,
                    AiCliLaunchCommand.Mode.NEW_SESSION),
                AiCliLaunchCommand.command(AiCliLaunchCommand.Tool.CLAUDE,
                    AiCliLaunchCommand.Mode.PICK_HISTORY)));
        ((TextView) findViewById(R.id.ai_cli_center_codex_commands)).setText(
            getString(R.string.ai_cli_center_command_pair,
                AiCliLaunchCommand.command(AiCliLaunchCommand.Tool.CODEX,
                    AiCliLaunchCommand.Mode.NEW_SESSION),
                AiCliLaunchCommand.command(AiCliLaunchCommand.Tool.CODEX,
                    AiCliLaunchCommand.Mode.PICK_HISTORY)));
    }

    private void openTmuxSessions() {
        Intent intent = TaskSessionsNavigation.newIntentForActiveWorkspace(this);
        if (intent == null) {
            startActivity(new Intent(this, WorkspaceActivity.class));
            return;
        }
        startActivity(intent);
    }

    private void openGitWorkbench() {
        Intent intent = GitWorkbenchNavigation.newIntentForActiveWorkspace(this, false);
        if (intent == null) {
            startActivity(new Intent(this, WorkspaceActivity.class));
            return;
        }
        startActivity(intent);
    }

    private void launchAiCli(AiCliLaunchCommand.Tool tool, AiCliLaunchCommand.Mode mode) {
        WorkspaceTarget workspace = WorkspaceTargetStore.readActive(this);
        if (workspace == null || !SshTargetValidator.isValid(workspace.host)
            || workspace.port < 1 || workspace.port > 65535
            || workspace.path == null || workspace.path.trim().isEmpty()) {
            startActivity(new Intent(this, WorkspaceActivity.class));
            return;
        }
        String command = WorkspaceCommandBuilder.buildSshCommand(
            workspace.host, workspace.port, workspace.path,
            AiCliLaunchCommand.command(tool, mode),
            WorkspaceCommandBuilder.POLICY_SSH_ONLY, "");
        startActivity(new Intent(this, TermuxActivity.class)
            .putExtra(TermuxActivity.EXTRA_STARTUP_COMMAND, command)
            .putExtra(TermuxActivity.EXTRA_NEW_SESSION, true));
    }
}
