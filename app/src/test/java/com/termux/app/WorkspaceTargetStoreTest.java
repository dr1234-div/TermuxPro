package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WorkspaceTargetStoreTest {
    private SharedPreferences preferences;

    @Before
    public void setUp() {
        preferences = RuntimeEnvironment.getApplication().getSharedPreferences(
            WorkspaceTargetStore.PREFERENCES_NAME, 0);
        preferences.edit().clear().commit();
    }

    @Test
    public void readsSelectedProfileInsteadOfFirstProfile() {
        preferences.edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES,
                "[{\"id\":\"a\",\"name\":\"A\",\"host\":\"a@example.com\",\"port\":\"22\",\"path\":\"~/a\"},"
                    + "{\"id\":\"b\",\"name\":\"B\",\"host\":\"b@example.com\",\"port\":\"2222\",\"path\":\"~/b\"}]")
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, "b").commit();

        WorkspaceTarget target = WorkspaceTargetStore.readActive(RuntimeEnvironment.getApplication());
        assertEquals("b", target.id);
        assertEquals("B", target.name);
        assertEquals("b@example.com", target.host);
        assertEquals(2222, target.port);
        assertEquals("~/b", target.path);
        assertTrue(target.isConfigured());
    }

    @Test
    public void staleSelectionFailsClosedInsteadOfTargetingFirstProfile() {
        preferences.edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES,
                "[{\"id\":\"a\",\"name\":\"A\",\"host\":\"a@example.com\",\"port\":\"22\",\"path\":\"~/a\"}]")
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, "missing").commit();

        assertNull(WorkspaceTargetStore.readActive(RuntimeEnvironment.getApplication()));
    }

    @Test
    public void migratesReadableLegacyTargetAndRejectsBrokenData() {
        preferences.edit().putString("name", "旧工作区")
            .putString("host", "legacy@example.com").putString("port", "22")
            .putString("path", "~/legacy").commit();
        WorkspaceTarget legacy = WorkspaceTargetStore.readActive(RuntimeEnvironment.getApplication());
        assertEquals("legacy", legacy.id);
        assertTrue(legacy.isConfigured());

        preferences.edit().clear()
            .putString(WorkspaceTargetStore.KEY_PROFILES, "broken-json").commit();
        assertNull(WorkspaceTargetStore.readActive(RuntimeEnvironment.getApplication()));
    }

    @Test
    public void marksInvalidPortAsNotConfigured() {
        preferences.edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES,
                "[{\"id\":\"a\",\"host\":\"a@example.com\",\"port\":\"70000\",\"path\":\"~/a\"}]")
            .commit();
        WorkspaceTarget target = WorkspaceTargetStore.readActive(RuntimeEnvironment.getApplication());
        assertFalse(target.isConfigured());
    }
}
