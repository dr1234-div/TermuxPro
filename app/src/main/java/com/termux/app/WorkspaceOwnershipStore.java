package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.UUID;

/** 为每个本地工作区保存独立的非敏感随机标识，用于核对远端 tmux 会话归属。 */
final class WorkspaceOwnershipStore {
    private static final String PREFERENCES_NAME = "termuxpro_workspace_ownership";
    private static final String KEY_PREFIX = "owner_";

    private final SharedPreferences mPreferences;

    WorkspaceOwnershipStore(@NonNull Context context) {
        mPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    synchronized String getOrCreate(@NonNull String workspaceId) {
        requireWorkspaceId(workspaceId);
        String key = KEY_PREFIX + workspaceId;
        String existing = mPreferences.getString(key, null);
        if (isValid(existing)) return existing;
        String generated = UUID.randomUUID().toString();
        mPreferences.edit().putString(key, generated).apply();
        return generated;
    }

    synchronized void clear(@NonNull String workspaceId) {
        requireWorkspaceId(workspaceId);
        mPreferences.edit().remove(KEY_PREFIX + workspaceId).apply();
    }

    private static void requireWorkspaceId(@NonNull String workspaceId) {
        if (workspaceId.trim().isEmpty() || workspaceId.length() > 128) {
            throw new IllegalArgumentException("Invalid workspace id");
        }
    }

    static boolean isValid(String value) {
        if (value == null) return false;
        try {
            return value.equals(UUID.fromString(value).toString());
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
