package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 以工作区为边界保存快捷指令；损坏记录失败关闭，不跨工作区回退。 */
final class CustomCommandStore {
    private static final String PREFERENCES_NAME = "termuxpro_custom_commands";
    private static final String KEY_PREFIX = "commands_";
    private static final int MAX_COMMANDS_PER_WORKSPACE = 100;

    private final SharedPreferences mPreferences;

    CustomCommandStore(@NonNull Context context) {
        mPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    synchronized List<CustomCommand> list(@NonNull String workspaceId) {
        requireWorkspaceId(workspaceId);
        String serialized = mPreferences.getString(KEY_PREFIX + workspaceId, "[]");
        try {
            JSONArray array = new JSONArray(serialized);
            List<CustomCommand> values = new ArrayList<>();
            for (int index = 0; index < array.length(); index++) {
                CustomCommand value = fromJson(array.getJSONObject(index));
                if (CustomCommandValidator.validate(value) == null) values.add(value);
            }
            return Collections.unmodifiableList(values);
        } catch (JSONException | IllegalArgumentException exception) {
            return Collections.emptyList();
        }
    }

    synchronized void save(@NonNull String workspaceId, @NonNull CustomCommand value) {
        requireWorkspaceId(workspaceId);
        CustomCommandValidator.Error error = CustomCommandValidator.validate(value);
        if (error != null) throw new IllegalArgumentException(error.name());
        List<CustomCommand> values = new ArrayList<>(list(workspaceId));
        int existingIndex = indexOf(values, value.id);
        if (existingIndex >= 0) {
            values.set(existingIndex, value);
        } else {
            if (values.size() >= MAX_COMMANDS_PER_WORKSPACE) {
                throw new IllegalStateException("Too many commands");
            }
            values.add(value);
        }
        persist(workspaceId, values);
    }

    synchronized boolean delete(@NonNull String workspaceId, @NonNull String commandId) {
        requireWorkspaceId(workspaceId);
        requireCommandId(commandId);
        List<CustomCommand> values = new ArrayList<>(list(workspaceId));
        int index = indexOf(values, commandId);
        if (index < 0) return false;
        values.remove(index);
        persist(workspaceId, values);
        return true;
    }

    synchronized boolean move(@NonNull String workspaceId, @NonNull String commandId,
                              int destinationIndex) {
        requireWorkspaceId(workspaceId);
        requireCommandId(commandId);
        List<CustomCommand> values = new ArrayList<>(list(workspaceId));
        int sourceIndex = indexOf(values, commandId);
        if (sourceIndex < 0 || destinationIndex < 0 || destinationIndex >= values.size()) {
            return false;
        }
        CustomCommand value = values.remove(sourceIndex);
        values.add(destinationIndex, value);
        persist(workspaceId, values);
        return true;
    }

    synchronized void clear(@NonNull String workspaceId) {
        requireWorkspaceId(workspaceId);
        mPreferences.edit().remove(KEY_PREFIX + workspaceId).apply();
    }

    private void persist(String workspaceId, List<CustomCommand> values) {
        JSONArray array = new JSONArray();
        for (CustomCommand value : values) array.put(toJson(value));
        mPreferences.edit().putString(KEY_PREFIX + workspaceId, array.toString()).apply();
    }

    private static JSONObject toJson(CustomCommand value) {
        try {
            return new JSONObject()
                .put("id", value.id)
                .put("name", value.name)
                .put("command", value.command)
                .put("workingDirectory", value.workingDirectory)
                .put("group", value.group)
                .put("enabled", value.enabled)
                .put("confirmation", value.confirmation.name());
        } catch (JSONException exception) {
            throw new IllegalStateException("Unable to serialize custom command", exception);
        }
    }

    private static CustomCommand fromJson(JSONObject value) throws JSONException {
        return new CustomCommand(value.getString("id"), value.getString("name"),
            value.getString("command"), value.optString("workingDirectory"),
            value.optString("group"), value.optBoolean("enabled", true),
            CustomCommand.Confirmation.valueOf(value.optString("confirmation",
                CustomCommand.Confirmation.ALWAYS.name())));
    }

    private static int indexOf(List<CustomCommand> values, String id) {
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).id.equals(id)) return index;
        }
        return -1;
    }

    private static void requireWorkspaceId(String value) {
        if (value.trim().isEmpty() || value.length() > 128) {
            throw new IllegalArgumentException("Invalid workspace id");
        }
    }

    private static void requireCommandId(String value) {
        if (value.trim().isEmpty() || value.length() > 128) {
            throw new IllegalArgumentException("Invalid command id");
        }
    }
}
