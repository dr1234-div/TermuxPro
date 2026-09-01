package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** 验证终端工具箱复用当前工作区打开 Web 预览增值能力。 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public final class WebPreviewNavigationTest {
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences(WorkspaceTargetStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit();
    }

    @Test
    public void startsLoopbackOnlyTunnelInNewTerminalSession() throws Exception {
        saveProfile("workspace-a", "hdr@192.168.1.153", "22", "3000", "13000");

        Intent intent = WebPreviewNavigation.newStartTunnelIntentForActiveWorkspace(context);

        assertEquals(TermuxActivity.class.getName(), intent.getComponent().getClassName());
        assertTrue(intent.getBooleanExtra(TermuxActivity.EXTRA_NEW_SESSION, false));
        String command = intent.getStringExtra(TermuxActivity.EXTRA_STARTUP_COMMAND);
        assertTrue(command.contains("ssh -N -T"));
        assertTrue(command.contains("-L '127.0.0.1:13000:127.0.0.1:3000'"));
        assertTrue(command.contains("-- 'hdr@192.168.1.153'"));
    }

    @Test
    public void opensBrowserForConfiguredLocalPort() throws Exception {
        saveProfile("workspace-a", "hdr@192.168.1.153", "22", "5173", "15173");

        Intent intent = WebPreviewNavigation.newOpenBrowserIntentForActiveWorkspace(context);

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("http://127.0.0.1:15173", intent.getDataString());
    }

    @Test
    public void rejectsInvalidWorkspaceForTunnel() throws Exception {
        saveProfile("workspace-a", "hdr@192.168.1.153", "22", "70000", "15173");

        assertNull(WebPreviewNavigation.newStartTunnelIntentForActiveWorkspace(context));
    }

    @Test
    public void rejectsMissingPreviewPortsForBrowser() {
        assertNull(WebPreviewNavigation.newOpenBrowserIntentForActiveWorkspace(context));
    }

    private void saveProfile(String id, String host, String port, String remotePort,
                             String localPort) throws Exception {
        JSONArray profiles = new JSONArray()
            .put(new JSONObject()
                .put("id", id)
                .put("name", "153")
                .put("host", host)
                .put("port", port)
                .put("path", "~/project")
                .put("remotePort", remotePort)
                .put("localPort", localPort));
        context.getSharedPreferences(WorkspaceTargetStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES, profiles.toString())
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, id)
            .commit();
    }
}
