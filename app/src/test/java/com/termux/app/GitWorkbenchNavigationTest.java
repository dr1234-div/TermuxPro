package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class GitWorkbenchNavigationTest {
    private SharedPreferences preferences;

    @Before
    public void setUp() {
        preferences = RuntimeEnvironment.getApplication().getSharedPreferences(
            WorkspaceTargetStore.PREFERENCES_NAME, 0);
        preferences.edit().clear().commit();
    }

    @Test
    public void opensOverviewForActiveWorkspace() {
        preferences.edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES,
                "[{\"id\":\"a\",\"name\":\"A\",\"host\":\"hdr@192.168.1.153\",\"port\":\"22\",\"path\":\"~/project\"}]")
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, "a")
            .commit();

        Intent intent = GitWorkbenchNavigation.newIntentForActiveWorkspace(
            RuntimeEnvironment.getApplication(), false);

        assertNotNull(intent);
        assertEquals(GitDiffActivity.class.getName(), intent.getComponent().getClassName());
        assertFalse(intent.getBooleanExtra(GitDiffActivity.EXTRA_START_IN_DIFF, true));
    }

    @Test
    public void opensDiffForActiveWorkspaceWhenRequested() {
        preferences.edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES,
                "[{\"id\":\"a\",\"name\":\"A\",\"host\":\"hdr@192.168.1.153\",\"port\":\"22\",\"path\":\"~/project\"}]")
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, "a")
            .commit();

        Intent intent = GitWorkbenchNavigation.newIntentForActiveWorkspace(
            RuntimeEnvironment.getApplication(), true);

        assertNotNull(intent);
        assertTrue(intent.getBooleanExtra(GitDiffActivity.EXTRA_START_IN_DIFF, false));
    }

    @Test
    public void failsClosedWithoutConfiguredWorkspace() {
        assertNull(GitWorkbenchNavigation.newIntentForActiveWorkspace(
            RuntimeEnvironment.getApplication(), false));

        preferences.edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES,
                "[{\"id\":\"a\",\"name\":\"A\",\"host\":\"\",\"port\":\"22\",\"path\":\"~/project\"}]")
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, "a")
            .commit();

        assertNull(GitWorkbenchNavigation.newIntentForActiveWorkspace(
            RuntimeEnvironment.getApplication(), false));
    }
}
