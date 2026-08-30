package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WorkspaceOwnershipStoreTest {

    @Test
    public void keepsStableRandomTokenPerWorkspaceAndRotatesAfterClear() {
        WorkspaceOwnershipStore store = new WorkspaceOwnershipStore(
            RuntimeEnvironment.getApplication());

        String first = store.getOrCreate("workspace-a");
        String same = store.getOrCreate("workspace-a");
        String other = store.getOrCreate("workspace-b");

        assertTrue(WorkspaceOwnershipStore.isValid(first));
        assertEquals(first, same);
        assertNotEquals(first, other);

        store.clear("workspace-a");
        assertNotEquals(first, store.getOrCreate("workspace-a"));
    }

    @Test
    public void survivesStoreRecreationAndRotatesMalformedStoredValue() {
        WorkspaceOwnershipStore firstStore = new WorkspaceOwnershipStore(
            RuntimeEnvironment.getApplication());
        String first = firstStore.getOrCreate("workspace-recreated");
        WorkspaceOwnershipStore recreated = new WorkspaceOwnershipStore(
            RuntimeEnvironment.getApplication());

        assertEquals(first, recreated.getOrCreate("workspace-recreated"));
        RuntimeEnvironment.getApplication()
            .getSharedPreferences("termuxpro_workspace_ownership", 0)
            .edit().putString("owner_workspace-malformed", "broken").commit();
        assertTrue(WorkspaceOwnershipStore.isValid(recreated.getOrCreate("workspace-malformed")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankWorkspaceId() {
        new WorkspaceOwnershipStore(RuntimeEnvironment.getApplication()).getOrCreate("   ");
    }

    @Test
    public void rejectsMissingOrMalformedOwnershipTokens() {
        assertFalse(WorkspaceOwnershipStore.isValid(null));
        assertFalse(WorkspaceOwnershipStore.isValid("termuxpro-owner"));
        assertFalse(WorkspaceOwnershipStore.isValid("11111111-2222-3333-4444-55555555555Z"));
    }
}
