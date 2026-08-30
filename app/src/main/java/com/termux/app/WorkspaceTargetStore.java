package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** 只读解析当前工作区，供终端内工具与首页共享同一目标。 */
final class WorkspaceTargetStore {
    static final String PREFERENCES_NAME = "ai_terminal_workspace";
    static final String KEY_PROFILES = "profiles_v2";
    static final String KEY_ACTIVE_PROFILE = "active_profile";

    private WorkspaceTargetStore() {}

    @Nullable
    static WorkspaceTarget readActive(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
            PREFERENCES_NAME, Context.MODE_PRIVATE);
        String serialized = preferences.getString(KEY_PROFILES, null);
        if (serialized != null) {
            try {
                JSONArray profiles = new JSONArray(serialized);
                String activeId = preferences.getString(KEY_ACTIVE_PROFILE, "");
                JSONObject fallback = null;
                for (int index = 0; index < profiles.length(); index++) {
                    JSONObject item = profiles.getJSONObject(index);
                    if (fallback == null) fallback = item;
                    if (activeId.equals(item.optString("id"))) return fromJson(item);
                }
                if (activeId.isEmpty() && fallback != null) return fromJson(fallback);
                return null;
            } catch (JSONException ignored) {
                return null;
            }
        }
        return readLegacy(preferences);
    }

    @Nullable
    private static WorkspaceTarget fromJson(JSONObject item) {
        String id = item.optString("id").trim();
        if (id.isEmpty()) return null;
        return new WorkspaceTarget(id, item.optString("name", "远程开发"),
            item.optString("host").trim(), parsePort(item.optString("port", "22")),
            item.optString("path", "~/").trim());
    }

    @Nullable
    private static WorkspaceTarget readLegacy(SharedPreferences preferences) {
        String host = preferences.getString("host", "").trim();
        if (host.isEmpty()) return null;
        return new WorkspaceTarget("legacy", preferences.getString("name", "远程开发"), host,
            parsePort(preferences.getString("port", "22")),
            preferences.getString("path", "~/").trim());
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value.trim());
            return port >= 1 && port <= 65535 ? port : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}
