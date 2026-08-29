package com.termux.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.app.terminal.io.TermuxTerminalExtraKeys;
import com.termux.shared.termux.TermuxConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 面向移动开发场景的工作区首页。
 *
 * 这里只保存主机、端口和项目路径等非敏感元数据。密码、私钥和 known_hosts 继续交给
 * Termux 内的 OpenSSH 管理，避免在产品外壳中复制一套不完整的凭据系统。
 */
public final class WorkspaceActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATIONS = 1001;

    private static final String PREFERENCES_NAME = "ai_terminal_workspace";
    private static final String KEY_NAME = "name";
    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";
    private static final String KEY_PATH = "path";
    private static final String KEY_PROFILES = "profiles_v2";
    private static final String KEY_ACTIVE_PROFILE = "active_profile";

    private EditText mNameInput;
    private EditText mHostInput;
    private EditText mPortInput;
    private EditText mPathInput;
    private EditText mSessionNameInput;
    private EditText mRemotePortInput;
    private EditText mLocalPortInput;
    private Spinner mWorkspaceSelector;
    private Spinner mConnectionPolicySelector;
    private final List<WorkspaceProfile> mProfiles = new ArrayList<>();
    private String mActiveProfileId;
    private boolean mUpdatingSelector;
    private boolean mBindingProfile;
    private boolean mHasUnsavedChanges;

    private static final String INSTALL_SSH_COMMAND = "pkg update -y && pkg install -y openssh";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workspace);
        requestNotificationPermissionIfNeeded();

        mNameInput = findViewById(R.id.workspace_name_input);
        mHostInput = findViewById(R.id.workspace_host_input);
        mPortInput = findViewById(R.id.workspace_port_input);
        mPathInput = findViewById(R.id.workspace_path_input);
        mSessionNameInput = findViewById(R.id.workspace_session_name_input);
        mRemotePortInput = findViewById(R.id.workspace_remote_port_input);
        mLocalPortInput = findViewById(R.id.workspace_local_port_input);
        mWorkspaceSelector = findViewById(R.id.workspace_selector);
        mConnectionPolicySelector = findViewById(R.id.workspace_connection_policy_selector);

        ArrayAdapter<CharSequence> policyAdapter = ArrayAdapter.createFromResource(this,
            R.array.workspace_connection_policy_labels, R.layout.item_workspace_spinner);
        policyAdapter.setDropDownViewResource(R.layout.item_workspace_spinner_dropdown);
        mConnectionPolicySelector.setAdapter(policyAdapter);
        configureLargeFontLayout();

        mConnectionPolicySelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSessionNameState();
                if (!mBindingProfile) updateWorkspaceDirtyState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        restoreWorkspaces();

        TextWatcher dirtyWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                if (!mBindingProfile) updateWorkspaceDirtyState();
            }
            @Override public void afterTextChanged(Editable editable) {}
        };
        mNameInput.addTextChangedListener(dirtyWatcher);
        mHostInput.addTextChangedListener(dirtyWatcher);
        mPortInput.addTextChangedListener(dirtyWatcher);
        mPathInput.addTextChangedListener(dirtyWatcher);
        mSessionNameInput.addTextChangedListener(dirtyWatcher);
        mRemotePortInput.addTextChangedListener(dirtyWatcher);
        mLocalPortInput.addTextChangedListener(dirtyWatcher);

        findViewById(R.id.workspace_connect_button).setOnClickListener(view -> launchRemote(null));
        findViewById(R.id.workspace_connection_diagnostic_primary).setOnClickListener(
            view -> openConnectionDiagnostic());
        findViewById(R.id.workspace_claude_button).setOnClickListener(view ->
            showAiLaunchDialog(AiCliLaunchCommand.Tool.CLAUDE));
        findViewById(R.id.workspace_codex_button).setOnClickListener(view ->
            showAiLaunchDialog(AiCliLaunchCommand.Tool.CODEX));
        findViewById(R.id.workspace_local_terminal_button).setOnClickListener(view -> {
            persistExtraKeysPreset(TermuxTerminalExtraKeys.PRESET_SHELL);
            openTerminal(null);
        });
        findViewById(R.id.workspace_setup_button).setOnClickListener(view -> installSshClient());
        findViewById(R.id.workspace_new_button).setOnClickListener(view ->
            runAfterDiscardConfirmation(this::createWorkspace));
        findViewById(R.id.workspace_save_button).setOnClickListener(view -> saveCurrentWorkspace());
        findViewById(R.id.workspace_copy_button).setOnClickListener(view -> copyCurrentWorkspace());
        findViewById(R.id.workspace_delete_button).setOnClickListener(view -> confirmDeleteWorkspace());
        findViewById(R.id.workspace_start_preview_button).setOnClickListener(view -> startPreviewTunnel());
        findViewById(R.id.workspace_open_preview_button).setOnClickListener(view -> openPreviewInBrowser());
        findViewById(R.id.workspace_review_diff_button).setOnClickListener(view -> openGitDiffReview());
        findViewById(R.id.workspace_remote_files_button).setOnClickListener(view -> openRemoteFiles());
        findViewById(R.id.workspace_project_tasks_button).setOnClickListener(view -> openProjectTasks());
        findViewById(R.id.workspace_diagnostic_button).setOnClickListener(view -> openConnectionDiagnostic());
        findViewById(R.id.workspace_ssh_keys_button).setOnClickListener(view -> openSshKeys());
        findViewById(R.id.workspace_task_sessions_button).setOnClickListener(view -> openTaskSessions());

        mWorkspaceSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mUpdatingSelector || position < 0 || position >= mProfiles.size()) return;
                int current = findActiveProfileIndex();
                if (position == current) return;
                if (mHasUnsavedChanges) {
                    mUpdatingSelector = true;
                    mWorkspaceSelector.setSelection(current, false);
                    mUpdatingSelector = false;
                    confirmDiscardChanges(() -> selectWorkspace(position));
                } else {
                    selectWorkspace(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private void configureLargeFontLayout() {
        if (getResources().getConfiguration().fontScale < 1.5f) return;
        stackButtonRow(R.id.workspace_ai_actions);
        stackButtonRow(R.id.workspace_tools_row_one);
        stackButtonRow(R.id.workspace_tools_row_two);
        stackButtonRow(R.id.workspace_tools_row_three);
    }

    /** 大字体下取消双列，避免按钮文字被横向省略或固定高度裁切。 */
    private void stackButtonRow(int rowId) {
        LinearLayout row = findViewById(rowId);
        row.setOrientation(LinearLayout.VERTICAL);
        int margin = Math.round(8 * getResources().getDisplayMetrics().density);
        int visibleIndex = 0;
        for (int index = 0; index < row.getChildCount(); index++) {
            View child = row.getChildAt(index);
            if (!(child instanceof android.widget.Button)) {
                child.setVisibility(View.GONE);
                continue;
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (visibleIndex++ > 0) params.topMargin = margin;
            child.setLayoutParams(params);
            child.setMinimumHeight(Math.round(56 * getResources().getDisplayMetrics().density));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSetupState();
    }

    @Override
    public void onBackPressed() {
        runAfterDiscardConfirmation(super::onBackPressed);
    }

    private boolean isSshClientInstalled() {
        return new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH, "ssh").canExecute();
    }

    private void refreshSetupState() {
        boolean ready = isSshClientInstalled();
        findViewById(R.id.workspace_setup_button).setEnabled(!ready);
        ((android.widget.Button) findViewById(R.id.workspace_setup_button)).setText(
            ready ? R.string.workspace_setup_ready : R.string.workspace_setup_action);
    }

    private void installSshClient() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.workspace_setup_title)
            .setMessage(R.string.workspace_setup_description)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.workspace_setup_action,
                (dialog, which) -> openTerminal(INSTALL_SSH_COMMAND))
            .show();
    }

    private void restoreWorkspaces() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        String serialized = preferences.getString(KEY_PROFILES, null);
        if (serialized != null) {
            try {
                JSONArray profiles = new JSONArray(serialized);
                for (int index = 0; index < profiles.length(); index++) {
                    JSONObject item = profiles.getJSONObject(index);
                    mProfiles.add(new WorkspaceProfile(
                        item.optString("id", UUID.randomUUID().toString()),
                        item.optString("name", getString(R.string.workspace_default_name)),
                        item.optString("host", ""), item.optString("port", "22"),
                        item.optString("path", "~/"), item.optString("remotePort", "5173"),
                        item.optString("localPort", "5173"),
                        item.optString("connectionPolicy", WorkspaceCommandBuilder.POLICY_SSH_ONLY),
                        item.optString("sessionName", defaultSessionName(item.optString("id", "")))));
                }
            } catch (JSONException ignored) {
                // 配置损坏时保留应用可用性，下面会创建默认工作区。
            }
        }

        if (mProfiles.isEmpty()) {
            mProfiles.add(new WorkspaceProfile(UUID.randomUUID().toString(),
                preferences.getString(KEY_NAME, getString(R.string.workspace_default_name)),
                preferences.getString(KEY_HOST, ""), preferences.getString(KEY_PORT, "22"),
                preferences.getString(KEY_PATH, "~/"), "5173", "5173",
                WorkspaceCommandBuilder.POLICY_SSH_ONLY, ""));
        }
        mActiveProfileId = preferences.getString(KEY_ACTIVE_PROFILE, mProfiles.get(0).id);
        refreshWorkspaceSelector();
    }

    private void refreshWorkspaceSelector() {
        mUpdatingSelector = true;
        ArrayAdapter<WorkspaceProfile> adapter = new ArrayAdapter<>(this,
            R.layout.item_workspace_spinner, mProfiles);
        adapter.setDropDownViewResource(R.layout.item_workspace_spinner_dropdown);
        mWorkspaceSelector.setAdapter(adapter);
        int selected = findActiveProfileIndex();
        mWorkspaceSelector.setSelection(selected, false);
        bindProfile(mProfiles.get(selected));
        mUpdatingSelector = false;
    }

    private int findActiveProfileIndex() {
        for (int index = 0; index < mProfiles.size(); index++) {
            if (mProfiles.get(index).id.equals(mActiveProfileId)) return index;
        }
        mActiveProfileId = mProfiles.get(0).id;
        return 0;
    }

    private void bindProfile(WorkspaceProfile profile) {
        mBindingProfile = true;
        mNameInput.setText(profile.name);
        mHostInput.setText(profile.host);
        mPortInput.setText(profile.port);
        mPathInput.setText(profile.path);
        mConnectionPolicySelector.setSelection(policyToIndex(profile.connectionPolicy), false);
        mSessionNameInput.setText(profile.sessionName);
        updateSessionNameState();
        mRemotePortInput.setText(profile.remotePort);
        mLocalPortInput.setText(profile.localPort);
        mBindingProfile = false;
        setWorkspaceDirty(false);
    }

    private void selectWorkspace(int position) {
        mActiveProfileId = mProfiles.get(position).id;
        bindProfile(mProfiles.get(position));
        persistProfiles();
        mUpdatingSelector = true;
        mWorkspaceSelector.setSelection(position, false);
        mUpdatingSelector = false;
    }

    private void updateWorkspaceDirtyState() {
        WorkspaceProfile profile = mProfiles.get(findActiveProfileIndex());
        boolean dirty = !TextUtils.equals(profile.name, mNameInput.getText().toString())
            || !TextUtils.equals(profile.host, mHostInput.getText().toString())
            || !TextUtils.equals(profile.port, mPortInput.getText().toString())
            || !TextUtils.equals(profile.path, mPathInput.getText().toString())
            || !TextUtils.equals(profile.sessionName, mSessionNameInput.getText().toString())
            || !TextUtils.equals(profile.remotePort, mRemotePortInput.getText().toString())
            || !TextUtils.equals(profile.localPort, mLocalPortInput.getText().toString())
            || !TextUtils.equals(profile.connectionPolicy,
                indexToPolicy(mConnectionPolicySelector.getSelectedItemPosition()));
        setWorkspaceDirty(dirty);
    }

    private void setWorkspaceDirty(boolean dirty) {
        mHasUnsavedChanges = dirty;
        findViewById(R.id.workspace_unsaved_indicator).setVisibility(dirty ? View.VISIBLE : View.GONE);
    }

    private void runAfterDiscardConfirmation(Runnable action) {
        if (mHasUnsavedChanges) confirmDiscardChanges(action); else action.run();
    }

    private void confirmDiscardChanges(Runnable action) {
        WorkspaceProfile profile = mProfiles.get(findActiveProfileIndex());
        new AlertDialog.Builder(this)
            .setTitle(R.string.workspace_discard_changes_title)
            .setMessage(getString(R.string.workspace_discard_changes_message, profile.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.workspace_discard_changes_action,
                (dialog, which) -> action.run())
            .show();
    }

    private void createWorkspace() {
        WorkspaceProfile profile = new WorkspaceProfile(UUID.randomUUID().toString(),
            getString(R.string.workspace_default_name), "", "22", "~/", "5173", "5173",
            WorkspaceCommandBuilder.POLICY_SSH_ONLY, "");
        mProfiles.add(profile);
        mActiveProfileId = profile.id;
        persistProfiles();
        refreshWorkspaceSelector();
        mHostInput.requestFocus();
    }

    private void copyCurrentWorkspace() {
        WorkspaceProfile copy = new WorkspaceProfile(UUID.randomUUID().toString(),
            getString(R.string.workspace_copy_name, normalizedWorkspaceName()),
            mHostInput.getText().toString().trim(), mPortInput.getText().toString().trim(),
            mPathInput.getText().toString().trim(), mRemotePortInput.getText().toString().trim(),
            mLocalPortInput.getText().toString().trim(),
            indexToPolicy(mConnectionPolicySelector.getSelectedItemPosition()),
            mSessionNameInput.getText().toString().trim());
        mProfiles.add(copy);
        mActiveProfileId = copy.id;
        persistProfiles();
        refreshWorkspaceSelector();
        mNameInput.requestFocus();
        mNameInput.selectAll();
    }

    private void saveCurrentWorkspace() {
        WorkspaceProfile profile = mProfiles.get(findActiveProfileIndex());
        profile.name = normalizedWorkspaceName();
        profile.host = mHostInput.getText().toString().trim();
        profile.port = mPortInput.getText().toString().trim();
        profile.path = mPathInput.getText().toString().trim();
        profile.connectionPolicy = indexToPolicy(mConnectionPolicySelector.getSelectedItemPosition());
        profile.sessionName = mSessionNameInput.getText().toString().trim();
        profile.remotePort = mRemotePortInput.getText().toString().trim();
        profile.localPort = mLocalPortInput.getText().toString().trim();
        persistProfiles();
        refreshWorkspaceSelector();
    }

    private String normalizedWorkspaceName() {
        String name = mNameInput.getText().toString().trim();
        return TextUtils.isEmpty(name) ? getString(R.string.workspace_default_name) : name;
    }

    private void confirmDeleteWorkspace() {
        WorkspaceProfile profile = mProfiles.get(findActiveProfileIndex());
        new AlertDialog.Builder(this)
            .setTitle(R.string.workspace_delete_title)
            .setMessage(getString(R.string.workspace_delete_message, profile.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.workspace_delete_action, (dialog, which) -> {
                mProfiles.remove(profile);
                if (mProfiles.isEmpty()) {
                    mProfiles.add(new WorkspaceProfile(UUID.randomUUID().toString(),
                        getString(R.string.workspace_default_name), "", "22", "~/", "5173", "5173",
                        WorkspaceCommandBuilder.POLICY_SSH_ONLY, ""));
                }
                mActiveProfileId = mProfiles.get(0).id;
                persistProfiles();
                refreshWorkspaceSelector();
            })
            .show();
    }

    private void persistProfiles() {
        JSONArray profiles = new JSONArray();
        for (WorkspaceProfile profile : mProfiles) {
            JSONObject item = new JSONObject();
            try {
                item.put("id", profile.id);
                item.put("name", profile.name);
                item.put("host", profile.host);
                item.put("port", profile.port);
                item.put("path", profile.path);
                item.put("remotePort", profile.remotePort);
                item.put("localPort", profile.localPort);
                item.put("connectionPolicy", profile.connectionPolicy);
                item.put("sessionName", profile.sessionName);
                profiles.put(item);
            } catch (JSONException ignored) {
                // String 字段不会触发 JSON 编码失败。
            }
        }
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit()
            .putString(KEY_PROFILES, profiles.toString())
            .putString(KEY_ACTIVE_PROFILE, mActiveProfileId)
            .apply();
    }

    private void launchRemote(String cli) {
        if (!isSshClientInstalled()) {
            installSshClient();
            return;
        }
        String host = mHostInput.getText().toString().trim();
        String portText = mPortInput.getText().toString().trim();
        String path = mPathInput.getText().toString().trim();
        String policy = indexToPolicy(mConnectionPolicySelector.getSelectedItemPosition());
        String sessionName = mSessionNameInput.getText().toString().trim();

        if (!SshTargetValidator.isValid(host)) {
            mHostInput.setError(getString(R.string.workspace_error_host));
            return;
        }

        int port;
        try {
            port = TextUtils.isEmpty(portText) ? 22 : Integer.parseInt(portText);
        } catch (NumberFormatException exception) {
            port = -1;
        }
        if (port < 1 || port > 65535) {
            mPortInput.setError(getString(R.string.workspace_error_port));
            return;
        }
        if (TextUtils.isEmpty(path)) {
            mPathInput.setError(getString(R.string.workspace_error_path));
            return;
        }
        if (requiresSessionName(policy) && !sessionName.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) {
            mSessionNameInput.setError(getString(R.string.workspace_error_session_name));
            return;
        }

        mNameInput.setText(normalizedWorkspaceName());
        mPortInput.setText(String.valueOf(port));
        saveCurrentWorkspace();

        TextView connectionStatus = findViewById(R.id.workspace_connection_feedback);
        connectionStatus.setText(R.string.workspace_connection_terminal_opened);
        connectionStatus.setContentDescription(getString(R.string.workspace_connection_terminal_opened));

        persistExtraKeysPreset(cli == null ? TermuxTerminalExtraKeys.PRESET_SHELL :
            TermuxTerminalExtraKeys.PRESET_AI);
        // 远程连接必须进入独立本地终端会话，避免命令被写入正在运行任务的旧 Shell。
        openTerminal(WorkspaceCommandBuilder.buildSshCommand(
            host, port, path, cli, policy, sessionName), true);
    }

    private void showAiLaunchDialog(AiCliLaunchCommand.Tool tool) {
        String[] actions = {
            getString(R.string.ai_session_new_action),
            getString(R.string.ai_session_pick_history_action)
        };
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.ai_session_launch_title,
                AiCliLaunchCommand.displayName(tool)))
            .setItems(actions, (selectionDialog, which) -> launchRemote(AiCliLaunchCommand.command(tool,
                which == 0 ? AiCliLaunchCommand.Mode.NEW_SESSION :
                    AiCliLaunchCommand.Mode.PICK_HISTORY)))
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.tp_primary)));
        dialog.show();
    }

    private void updateSessionNameState() {
        boolean enabled = requiresSessionName(
            indexToPolicy(mConnectionPolicySelector.getSelectedItemPosition()));
        mSessionNameInput.setEnabled(enabled);
        mSessionNameInput.setAlpha(enabled ? 1f : 0.55f);
    }

    private static boolean requiresSessionName(String policy) {
        return WorkspaceCommandBuilder.POLICY_ATTACH_SESSION.equals(policy)
            || WorkspaceCommandBuilder.POLICY_CREATE_OR_ATTACH.equals(policy);
    }

    private static String indexToPolicy(int index) {
        if (index == 1) return WorkspaceCommandBuilder.POLICY_LIST_SESSIONS;
        if (index == 2) return WorkspaceCommandBuilder.POLICY_ATTACH_SESSION;
        if (index == 3) return WorkspaceCommandBuilder.POLICY_CREATE_OR_ATTACH;
        return WorkspaceCommandBuilder.POLICY_SSH_ONLY;
    }

    private static int policyToIndex(String policy) {
        if (WorkspaceCommandBuilder.POLICY_LIST_SESSIONS.equals(policy)) return 1;
        if (WorkspaceCommandBuilder.POLICY_ATTACH_SESSION.equals(policy)) return 2;
        if (WorkspaceCommandBuilder.POLICY_CREATE_OR_ATTACH.equals(policy)) return 3;
        return 0;
    }

    private static String defaultSessionName(String id) {
        if (TextUtils.isEmpty(id)) return "";
        return "termuxpro-" + id.substring(0, Math.min(8, id.length()));
    }

    private void persistExtraKeysPreset(String preset) {
        getSharedPreferences("ai_terminal_ui", MODE_PRIVATE).edit()
            .putString("extra_keys_preset", preset).apply();
    }

    private void startPreviewTunnel() {
        if (!isSshClientInstalled()) {
            installSshClient();
            return;
        }
        String host = mHostInput.getText().toString().trim();
        if (!SshTargetValidator.isValid(host)) {
            mHostInput.setError(getString(R.string.workspace_error_host));
            return;
        }
        int sshPort = parsePort(mPortInput.getText().toString(), 22);
        int remotePort = parsePort(mRemotePortInput.getText().toString(), -1);
        int localPort = parsePort(mLocalPortInput.getText().toString(), -1);
        if (sshPort < 1) {
            mPortInput.setError(getString(R.string.workspace_error_port));
            return;
        }
        if (remotePort < 1) {
            mRemotePortInput.setError(getString(R.string.workspace_error_preview_port));
            return;
        }
        if (localPort < 1) {
            mLocalPortInput.setError(getString(R.string.workspace_error_preview_port));
            return;
        }
        saveCurrentWorkspace();
        openTerminal(WorkspaceCommandBuilder.buildPortForwardCommand(host, sshPort, localPort, remotePort), true);
    }

    private void openGitDiffReview() {
        if (!isSshClientInstalled()) {
            installSshClient();
            return;
        }
        String host = mHostInput.getText().toString().trim();
        String path = mPathInput.getText().toString().trim();
        int sshPort = parsePort(mPortInput.getText().toString(), 22);
        if (!SshTargetValidator.isValid(host)) {
            mHostInput.setError(getString(R.string.workspace_error_host));
            return;
        }
        if (TextUtils.isEmpty(path)) {
            mPathInput.setError(getString(R.string.workspace_error_path));
            return;
        }
        if (sshPort < 1) {
            mPortInput.setError(getString(R.string.workspace_error_port));
            return;
        }
        saveCurrentWorkspace();
        startActivity(GitDiffActivity.newIntent(this, host, sshPort, path));
    }

    private void openRemoteFiles() {
        if (!isSshClientInstalled()) {
            installSshClient();
            return;
        }
        String host = mHostInput.getText().toString().trim();
        String path = mPathInput.getText().toString().trim();
        int sshPort = parsePort(mPortInput.getText().toString(), 22);
        if (!SshTargetValidator.isValid(host)) {
            mHostInput.setError(getString(R.string.workspace_error_host));
            return;
        }
        if (TextUtils.isEmpty(path)) {
            mPathInput.setError(getString(R.string.workspace_error_path));
            return;
        }
        if (sshPort < 1) {
            mPortInput.setError(getString(R.string.workspace_error_port));
            return;
        }
        saveCurrentWorkspace();
        startActivity(RemoteFilesActivity.newIntent(this, host, sshPort, path));
    }

    private void openProjectTasks() {
        if (!isSshClientInstalled()) {
            installSshClient();
            return;
        }
        String host = mHostInput.getText().toString().trim();
        String path = mPathInput.getText().toString().trim();
        int sshPort = parsePort(mPortInput.getText().toString(), 22);
        if (!SshTargetValidator.isValid(host)) {
            mHostInput.setError(getString(R.string.workspace_error_host));
            return;
        }
        if (TextUtils.isEmpty(path)) {
            mPathInput.setError(getString(R.string.workspace_error_path));
            return;
        }
        if (sshPort < 1) {
            mPortInput.setError(getString(R.string.workspace_error_port));
            return;
        }
        saveCurrentWorkspace();
        startActivity(ProjectTasksActivity.newIntent(this, host, sshPort, path));
    }

    private void openConnectionDiagnostic() {
        if (!isSshClientInstalled()) {
            installSshClient();
            return;
        }
        String host = mHostInput.getText().toString().trim();
        String path = mPathInput.getText().toString().trim();
        int sshPort = parsePort(mPortInput.getText().toString(), 22);
        if (!SshTargetValidator.isValid(host)) {
            mHostInput.setError(getString(R.string.workspace_error_host));
            return;
        }
        if (TextUtils.isEmpty(path)) {
            mPathInput.setError(getString(R.string.workspace_error_path));
            return;
        }
        if (sshPort < 1) {
            mPortInput.setError(getString(R.string.workspace_error_port));
            return;
        }
        saveCurrentWorkspace();
        startActivity(ConnectionDiagnosticActivity.newIntent(this, host, sshPort, path));
    }

    private void openSshKeys() {
        if (!isSshClientInstalled()) {
            installSshClient();
            return;
        }
        String host = mHostInput.getText().toString().trim();
        int sshPort = parsePort(mPortInput.getText().toString(), 22);
        if (!SshTargetValidator.isValid(host)) {
            mHostInput.setError(getString(R.string.workspace_error_host));
            return;
        }
        if (sshPort < 1) {
            mPortInput.setError(getString(R.string.workspace_error_port));
            return;
        }
        saveCurrentWorkspace();
        startActivity(SshKeysActivity.newIntent(this, host, sshPort));
    }

    private void openTaskSessions() {
        if (!isSshClientInstalled()) {
            installSshClient();
            return;
        }
        String host = mHostInput.getText().toString().trim();
        int sshPort = parsePort(mPortInput.getText().toString(), 22);
        if (!SshTargetValidator.isValid(host)) {
            mHostInput.setError(getString(R.string.workspace_error_host));
            return;
        }
        if (sshPort < 1) {
            mPortInput.setError(getString(R.string.workspace_error_port));
            return;
        }
        startActivity(TaskSessionsActivity.newIntent(this, host, sshPort));
    }

    private int parsePort(String value, int defaultValue) {
        try {
            int port = TextUtils.isEmpty(value.trim()) ? defaultValue : Integer.parseInt(value.trim());
            return port >= 1 && port <= 65535 ? port : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private void openPreviewInBrowser() {
        int localPort = parsePort(mLocalPortInput.getText().toString(), -1);
        if (localPort < 1) {
            mLocalPortInput.setError(getString(R.string.workspace_error_preview_port));
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:" + localPort)));
    }

    private void openTerminal(String startupCommand) {
        openTerminal(startupCommand, false);
    }

    private void openTerminal(String startupCommand, boolean newSession) {
        Intent intent = new Intent(this, TermuxActivity.class);
        if (startupCommand != null) intent.putExtra(TermuxActivity.EXTRA_STARTUP_COMMAND, startupCommand);
        if (newSession) intent.putExtra(TermuxActivity.EXTRA_NEW_SESSION, true);
        startActivity(intent);
    }

    private static final class WorkspaceProfile {
        final String id;
        String name;
        String host;
        String port;
        String path;
        String remotePort;
        String localPort;
        String connectionPolicy;
        String sessionName;

        WorkspaceProfile(String id, String name, String host, String port, String path,
                         String remotePort, String localPort, String connectionPolicy,
                         String sessionName) {
            this.id = id;
            this.name = name;
            this.host = host;
            this.port = port;
            this.path = path;
            this.remotePort = remotePort;
            this.localPort = localPort;
            this.connectionPolicy = connectionPolicy;
            this.sessionName = TextUtils.isEmpty(sessionName) ? defaultSessionName(id) : sessionName;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
