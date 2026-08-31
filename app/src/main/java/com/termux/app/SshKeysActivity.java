package com.termux.app;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;
import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

/** 仅管理可公开的 SSH 公钥；私钥内容不读取、不展示、不复制。 */
public final class SshKeysActivity extends AppCompatActivity {

    private static final String EXTRA_HOST = "host";
    private static final String EXTRA_PORT = "port";
    private static final int MAX_PUBLIC_KEY_BYTES = 32_768;
    private static final String[] PUBLIC_KEY_FILES = {
        "id_ed25519.pub", "id_ecdsa.pub", "id_rsa.pub"
    };

    private String mHost;
    private int mPort;
    private String mPublicKeyDisplay = "";
    private String mPublicKeyPayload = "";
    private TextView mKeyView;
    private Button mCopyButton;
    private Button mInstallButton;

    @NonNull
    static Intent newIntent(@NonNull Context context, @NonNull String host, int port) {
        return new Intent(context, SshKeysActivity.class)
            .putExtra(EXTRA_HOST, host).putExtra(EXTRA_PORT, port);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ssh_keys);
        mHost = getIntent().getStringExtra(EXTRA_HOST);
        mPort = getIntent().getIntExtra(EXTRA_PORT, 22);
        mKeyView = findViewById(R.id.ssh_keys_public_key);
        mCopyButton = findViewById(R.id.ssh_keys_copy_button);
        mInstallButton = findViewById(R.id.ssh_keys_install_button);
        configureLargeFontActions();
        findViewById(R.id.ssh_keys_back_button).setOnClickListener(view -> finish());
        findViewById(R.id.ssh_keys_generate_button).setOnClickListener(view -> confirmGenerate());
        mCopyButton.setOnClickListener(view -> copyPublicKeys());
        mInstallButton.setOnClickListener(view -> confirmInstall());
    }

    /** 大字体下并排操作无法容纳完整语义，改为纵向全宽按钮。 */
    private void configureLargeFontActions() {
        float fontScale = getResources().getConfiguration().fontScale;
        if (fontScale < 1.5f) return;
        LinearLayout actions = findViewById(R.id.ssh_keys_secondary_actions);
        actions.setOrientation(LinearLayout.VERTICAL);
        configureStackedAction(mCopyButton, false);
        configureStackedAction(mInstallButton, true);
    }

    private void configureStackedAction(@NonNull Button button, boolean addTopMargin) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
        params.width = LinearLayout.LayoutParams.MATCH_PARENT;
        params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        params.weight = 0f;
        params.setMarginStart(0);
        params.topMargin = addTopMargin ? dp(8) : 0;
        button.setLayoutParams(params);
        button.setMinHeight(dp(50));
        button.setSingleLine(false);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPublicKeys();
    }

    private void loadPublicKeys() {
        StringBuilder display = new StringBuilder();
        StringBuilder payload = new StringBuilder();
        File sshDirectory = new File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".ssh");
        try {
            String allowedDirectory = sshDirectory.getCanonicalPath() + File.separator;
            for (String name : PUBLIC_KEY_FILES) {
                File key = new File(sshDirectory, name);
                if (!key.isFile() || key.length() > MAX_PUBLIC_KEY_BYTES ||
                    !key.getCanonicalPath().startsWith(allowedDirectory)) continue;
                byte[] bytes = new byte[(int) key.length()];
                try (FileInputStream input = new FileInputStream(key)) {
                    int offset = 0;
                    while (offset < bytes.length) {
                        int count = input.read(bytes, offset, bytes.length - offset);
                        if (count < 0) break;
                        offset += count;
                    }
                    String value = new String(bytes, 0, offset, StandardCharsets.UTF_8).trim();
                    if (!value.isEmpty()) {
                        display.append(name).append("\n").append(value).append("\n\n");
                        payload.append(value).append('\n');
                    }
                }
            }
        } catch (Exception ignored) {
            // 页面保持可用且不泄露路径或异常细节。
        }
        mPublicKeyDisplay = display.toString().trim();
        mPublicKeyPayload = payload.toString().trim();
        boolean available = !mPublicKeyPayload.isEmpty();
        mKeyView.setText(available ? mPublicKeyDisplay : getString(R.string.ssh_keys_empty));
        mCopyButton.setEnabled(available);
        mInstallButton.setEnabled(available && SshTargetValidator.isValid(mHost) && mPort > 0 && mPort <= 65535);
    }

    private void confirmGenerate() {
        TermuxProDialogStyle.show(this, new AlertDialog.Builder(this)
            .setTitle(R.string.ssh_keys_generate_title)
            .setMessage(R.string.ssh_keys_generate_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.ssh_keys_generate_action,
                (dialog, which) -> openTerminal(WorkspaceCommandBuilder.buildGenerateSshKeyCommand()))
            .create());
    }

    private void copyPublicKeys() {
        if (TextUtils.isEmpty(mPublicKeyPayload)) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(
            getString(R.string.ssh_keys_clipboard_label), mPublicKeyPayload));
        mKeyView.announceForAccessibility(getString(R.string.ssh_keys_copied));
    }

    private void confirmInstall() {
        String command = WorkspaceCommandBuilder.buildCopySshKeyCommand(mHost, mPort);
        TermuxProDialogStyle.show(this, new AlertDialog.Builder(this)
            .setTitle(R.string.ssh_keys_install_title)
            .setMessage(getString(R.string.ssh_keys_install_message, mHost, command))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.ssh_keys_install_action,
                (dialog, which) -> openTerminal(command))
            .create());
    }

    private void openTerminal(String command) {
        startActivity(new Intent(this, TermuxActivity.class)
            .putExtra(TermuxActivity.EXTRA_STARTUP_COMMAND, command)
            .putExtra(TermuxActivity.EXTRA_NEW_SESSION, true));
    }
}
