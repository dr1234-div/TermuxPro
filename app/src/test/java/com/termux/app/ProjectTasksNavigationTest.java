package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Intent;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class ProjectTasksNavigationTest {
    private SharedPreferences preferences;

    @Before
    public void setUp() {
        preferences = RuntimeEnvironment.getApplication().getSharedPreferences(
            WorkspaceTargetStore.PREFERENCES_NAME, 0);
        preferences.edit().clear().commit();
    }

    @Test
    public void opensProjectTasksForActiveWorkspace() {
        preferences.edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES,
                "[{\"id\":\"a\",\"name\":\"A\",\"host\":\"hdr@192.168.1.153\",\"port\":\"22\",\"path\":\"~/project\"}]")
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, "a")
            .commit();

        Intent intent = ProjectTasksNavigation.newIntentForActiveWorkspace(
            RuntimeEnvironment.getApplication());

        assertNotNull(intent);
        assertEquals(ProjectTasksActivity.class.getName(), intent.getComponent().getClassName());
    }

    @Test
    public void failsClosedWithoutConfiguredWorkspace() {
        assertNull(ProjectTasksNavigation.newIntentForActiveWorkspace(
            RuntimeEnvironment.getApplication()));

        preferences.edit()
            .putString(WorkspaceTargetStore.KEY_PROFILES,
                "[{\"id\":\"a\",\"name\":\"A\",\"host\":\"hdr@192.168.1.153\",\"port\":\"0\",\"path\":\"~/project\"}]")
            .putString(WorkspaceTargetStore.KEY_ACTIVE_PROFILE, "a")
            .commit();

        assertNull(ProjectTasksNavigation.newIntentForActiveWorkspace(
            RuntimeEnvironment.getApplication()));
    }
}
