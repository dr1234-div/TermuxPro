package com.termux.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

import java.util.List;

/** 按工作区管理、预览并执行用户快捷指令。 */
public final class CustomCommandsActivity extends AppCompatActivity {
    private static final int ACTION_EDIT = 1;
    private static final int ACTION_COPY = 2;
    private static final int ACTION_TOGGLE = 3;
    private static final int ACTION_MOVE_UP = 4;
    private static final int ACTION_MOVE_DOWN = 5;
    private static final int ACTION_DELETE = 6;

    private CustomCommandStore mStore;
    private WorkspaceTarget mTarget;
    private LinearLayout mList;
    private TextView mEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_commands);
        mStore = new CustomCommandStore(this);
        mTarget = WorkspaceTargetStore.readActive(this);
        mList = findViewById(R.id.custom_commands_list);
        mEmpty = findViewById(R.id.custom_commands_empty);

        findViewById(R.id.custom_commands_back).setOnClickListener(view -> finish());
        findViewById(R.id.custom_commands_add).setOnClickListener(view -> showEditor(null));
        bindTarget();
        renderCommands();
    }

    private void bindTarget() {
        TextView name = findViewById(R.id.custom_commands_target);
        TextView details = findViewById(R.id.custom_commands_target_details);
        View add = findViewById(R.id.custom_commands_add);
        if (mTarget == null || !mTarget.isConfigured()) {
            name.setText(R.string.custom_commands_invalid_workspace);
            details.setVisibility(View.GONE);
            add.setEnabled(false);
            return;
        }
        name.setText(mTarget.name);
        details.setText(getString(R.string.custom_commands_target_details,
            mTarget.host, mTarget.port, mTarget.path));
    }

    private void renderCommands() {
        mList.removeAllViews();
        if (mTarget == null || !mTarget.isConfigured()) {
            mEmpty.setText(R.string.custom_commands_invalid_workspace);
            mEmpty.setVisibility(View.VISIBLE);
            return;
        }
        List<CustomCommand> commands = mStore.list(mTarget.id);
        mEmpty.setVisibility(commands.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int index = 0; index < commands.size(); index++) {
            CustomCommand command = commands.get(index);
            View row = inflater.inflate(R.layout.item_custom_command, mList, false);
            ((TextView) row.findViewById(R.id.custom_command_name)).setText(command.name);
            ((TextView) row.findViewById(R.id.custom_command_state)).setText(command.enabled
                ? R.string.custom_commands_enabled_state : R.string.custom_commands_disabled_state);
            String group = TextUtils.isEmpty(command.group)
                ? getString(R.string.custom_commands_default_group) : command.group;
            String directory = TextUtils.isEmpty(command.workingDirectory)
                ? getString(R.string.custom_commands_default_directory) : command.workingDirectory;
            ((TextView) row.findViewById(R.id.custom_command_summary)).setText(
                getString(R.string.custom_commands_summary, group, directory));
            ((TextView) row.findViewById(R.id.custom_command_value)).setText(command.command);
            boolean requiresPreview = command.confirmation == CustomCommand.Confirmation.ALWAYS
                || CustomCommandValidator.isLikelyDangerous(command.command);
            ((TextView) row.findViewById(R.id.custom_command_run)).setText(requiresPreview
                ? R.string.custom_commands_run : R.string.custom_commands_run_now);
            row.findViewById(R.id.custom_command_run).setEnabled(command.enabled);
            row.findViewById(R.id.custom_command_run).setOnClickListener(view -> {
                if (requiresPreview) preview(command); else execute(command);
            });
            int position = index;
            row.findViewById(R.id.custom_command_manage).setOnClickListener(
                view -> showManagement(view, command, position, commands.size()));
            mList.addView(row);
        }
    }

    private void showManagement(View anchor, CustomCommand command, int position, int size) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(Menu.NONE, ACTION_EDIT, Menu.NONE, R.string.custom_commands_edit);
        popup.getMenu().add(Menu.NONE, ACTION_COPY, Menu.NONE, R.string.custom_commands_copy);
        popup.getMenu().add(Menu.NONE, ACTION_TOGGLE, Menu.NONE, command.enabled
            ? R.string.custom_commands_disable : R.string.custom_commands_enable);
        if (position > 0) popup.getMenu().add(Menu.NONE, ACTION_MOVE_UP, Menu.NONE,
            R.string.custom_commands_move_up);
        if (position < size - 1) popup.getMenu().add(Menu.NONE, ACTION_MOVE_DOWN, Menu.NONE,
            R.string.custom_commands_move_down);
        popup.getMenu().add(Menu.NONE, ACTION_DELETE, Menu.NONE, R.string.custom_commands_delete);
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case ACTION_EDIT:
                    showEditor(command);
                    return true;
                case ACTION_COPY:
                    CustomCommand copy = new CustomCommand(command.copyWithId().id,
                        getString(R.string.custom_commands_copy_suffix, command.name), command.command,
                        command.workingDirectory, command.group, command.enabled, command.confirmation);
                    mStore.save(mTarget.id, copy);
                    renderCommands();
                    return true;
                case ACTION_TOGGLE:
                    mStore.save(mTarget.id, command.withEnabled(!command.enabled));
                    renderCommands();
                    return true;
                case ACTION_MOVE_UP:
                    mStore.move(mTarget.id, command.id, position - 1);
                    renderCommands();
                    return true;
                case ACTION_MOVE_DOWN:
                    mStore.move(mTarget.id, command.id, position + 1);
                    renderCommands();
                    return true;
                case ACTION_DELETE:
                    confirmDelete(command);
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void showEditor(@Nullable CustomCommand existing) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_custom_command, null, false);
        EditText name = form.findViewById(R.id.custom_command_name_input);
        EditText value = form.findViewById(R.id.custom_command_value_input);
        EditText directory = form.findViewById(R.id.custom_command_directory_input);
        EditText group = form.findViewById(R.id.custom_command_group_input);
        CheckBox alwaysConfirm = form.findViewById(R.id.custom_command_always_confirm);
        CheckBox enabled = form.findViewById(R.id.custom_command_enabled);
        alwaysConfirm.setChecked(existing == null
            || existing.confirmation == CustomCommand.Confirmation.ALWAYS);
        enabled.setChecked(existing == null || existing.enabled);
        if (existing != null) {
            name.setText(existing.name);
            value.setText(existing.command);
            directory.setText(existing.workingDirectory);
            group.setText(existing.group);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(existing == null ? R.string.custom_commands_create_title
                : R.string.custom_commands_edit_title)
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.custom_commands_save, null)
            .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(view -> {
                CustomCommand candidate = new CustomCommand(existing == null
                    ? CustomCommand.create("temp", "true", "", "",
                        CustomCommand.Confirmation.ALWAYS).id : existing.id,
                    name.getText().toString().trim(), value.getText().toString().trim(),
                    directory.getText().toString().trim(), group.getText().toString().trim(),
                    enabled.isChecked(), alwaysConfirm.isChecked()
                        ? CustomCommand.Confirmation.ALWAYS
                        : CustomCommand.Confirmation.DANGEROUS_ONLY);
                CustomCommandValidator.Error error = CustomCommandValidator.validate(candidate);
                if (error == CustomCommandValidator.Error.NAME_REQUIRED) {
                    name.setError(getString(R.string.custom_commands_name_required));
                    name.requestFocus();
                } else if (error == CustomCommandValidator.Error.COMMAND_REQUIRED) {
                    value.setError(getString(R.string.custom_commands_value_required));
                    value.requestFocus();
                } else if (error == CustomCommandValidator.Error.POSSIBLE_SECRET) {
                    value.setError(getString(R.string.custom_commands_possible_secret));
                    value.requestFocus();
                } else if (error != null) {
                    value.setError(getString(R.string.custom_commands_invalid));
                } else {
                    mStore.save(mTarget.id, candidate);
                    dialog.dismiss();
                    renderCommands();
                }
            }));
        dialog.show();
    }

    private void preview(CustomCommand command) {
        if (!command.enabled) {
            new AlertDialog.Builder(this).setMessage(R.string.custom_commands_disabled_message)
                .setPositiveButton(android.R.string.ok, null).show();
            return;
        }
        String directory = TextUtils.isEmpty(command.workingDirectory)
            ? mTarget.path : command.workingDirectory;
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.custom_commands_preview_title, command.name))
            .setMessage(getString(R.string.custom_commands_preview_message, mTarget.host,
                mTarget.port, directory, command.command))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.custom_commands_execute,
                (dialog, which) -> execute(command))
            .show();
    }

    private void execute(CustomCommand command) {
        String startup = WorkspaceCommandBuilder.buildCustomCommandSshCommand(
            mTarget.host, mTarget.port, mTarget.path, command.workingDirectory, command.command);
        Intent intent = new Intent(this, TermuxActivity.class)
            .putExtra(TermuxActivity.EXTRA_STARTUP_COMMAND, startup)
            .putExtra(TermuxActivity.EXTRA_NEW_SESSION, true);
        startActivity(intent);
    }

    private void confirmDelete(CustomCommand command) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.custom_commands_delete_title, command.name))
            .setMessage(R.string.custom_commands_delete_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.custom_commands_delete, (dialog, which) -> {
                mStore.delete(mTarget.id, command.id);
                renderCommands();
            })
            .show();
    }
}
