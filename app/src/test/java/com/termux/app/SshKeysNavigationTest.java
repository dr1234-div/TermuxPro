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

/** 验证终端工具箱可打开 SSH 公钥管理增值页，且工作区无效时失败关闭。 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public final class SshKeysNavigationTest {
    @Test
    public void opensSshKeysForActiveWorkspace() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        JSONArray profiles = new JSONArray()
            .put(new JSONObject()
                .put("id", "workspace-a")
                .put("host", "hdr@192.168.1.153")
                .put("port", "22")
                .put("path", "~/project"));
        context.getSharedPreferences(WorkspaceTargetStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES, profiles.toString())
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, "workspace-a")
            .commit();

        Intent intent = SshKeysNavigation.newIntentForActiveWorkspace(context);

        assertEquals(SshKeysActivity.class.getName(), intent.getComponent().getClassName());
    }

    @Test
    public void rejectsMissingWorkspace() {
        assertNull(SshKeysNavigation.newIntentForActiveWorkspace(
            RuntimeEnvironment.getApplication()));
    }
}
