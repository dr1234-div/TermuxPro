package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** 验证终端工具箱复用当前工作区打开远端文件增值页。 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public final class RemoteFilesNavigationTest {
    @Test
    public void opensRemoteFilesForActiveWorkspace() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        JSONArray profiles = new JSONArray()
            .put(new JSONObject()
                .put("id", "workspace-a")
                .put("name", "153")
                .put("host", "hdr@192.168.1.153")
                .put("port", "22")
                .put("path", "~/project"));
        context.getSharedPreferences(WorkspaceTargetStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES, profiles.toString())
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, "workspace-a")
            .commit();

        Intent intent = RemoteFilesNavigation.newIntentForActiveWorkspace(context);

        assertEquals(RemoteFilesActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("hdr@192.168.1.153", intent.getStringExtra("host"));
        assertEquals(22, intent.getIntExtra("port", -1));
        assertEquals("~/project", intent.getStringExtra("project_path"));
    }

    @Test
    public void rejectsMissingWorkspace() {
        assertNull(RemoteFilesNavigation.newIntentForActiveWorkspace(
            RuntimeEnvironment.getApplication()));
    }

    @Test
    public void rejectsInvalidPort() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        JSONArray profiles = new JSONArray()
            .put(new JSONObject()
                .put("id", "workspace-a")
                .put("host", "hdr@192.168.1.153")
                .put("port", "70000")
                .put("path", "~/project"));
        context.getSharedPreferences(WorkspaceTargetStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES, profiles.toString())
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, "workspace-a")
            .commit();

        assertNull(RemoteFilesNavigation.newIntentForActiveWorkspace(context));
    }
}
