package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class CustomCommandStoreTest {
    private CustomCommandStore store;

    @Before
    public void setUp() {
        RuntimeEnvironment.getApplication().getSharedPreferences(
            "termuxpro_custom_commands", 0).edit().clear().commit();
        store = new CustomCommandStore(RuntimeEnvironment.getApplication());
    }

    @Test
    public void supportsCreateReadUpdateCopyDeleteAndReorder() {
        CustomCommand build = command("构建", "./gradlew assembleDebug");
        CustomCommand test = command("测试", "./gradlew test");
        store.save("workspace-a", build);
        store.save("workspace-a", test);

        assertEquals(2, store.list("workspace-a").size());
        store.save("workspace-a", new CustomCommand(build.id, "构建 APK",
            build.command, build.workingDirectory, build.group, false, build.confirmation));
        assertEquals("构建 APK", store.list("workspace-a").get(0).name);
        assertFalse(store.list("workspace-a").get(0).enabled);

        CustomCommand copy = build.copyWithId();
        assertNotEquals(build.id, copy.id);
        store.save("workspace-a", copy);
        assertTrue(store.move("workspace-a", copy.id, 0));
        assertEquals(copy.id, store.list("workspace-a").get(0).id);
        assertTrue(store.delete("workspace-a", test.id));
        assertFalse(store.delete("workspace-a", "missing"));
        assertEquals(2, store.list("workspace-a").size());
    }

    @Test
    public void isolatesCommandsByWorkspaceAndSurvivesStoreRecreation() {
        store.save("workspace-a", command("A", "pwd"));
        store.save("workspace-b", command("B", "git status"));

        List<CustomCommand> a = new CustomCommandStore(
            RuntimeEnvironment.getApplication()).list("workspace-a");
        assertEquals(1, a.size());
        assertEquals("A", a.get(0).name);
        assertEquals("B", store.list("workspace-b").get(0).name);
    }

    @Test
    public void rejectsPossibleSecretsAndInvalidRequiredFields() {
        assertEquals(CustomCommandValidator.Error.NAME_REQUIRED,
            CustomCommandValidator.validate(command(" ", "pwd")));
        assertEquals(CustomCommandValidator.Error.COMMAND_REQUIRED,
            CustomCommandValidator.validate(command("空命令", " ")));
        assertEquals(CustomCommandValidator.Error.POSSIBLE_SECRET,
            CustomCommandValidator.validate(command("危险", "TOKEN=plain-secret npm test")));
        assertTrue(CustomCommandValidator.containsPossibleSecret(
            "https://user:password@example.com/repository"));
        assertTrue(CustomCommandValidator.containsPossibleSecret(
            "-----BEGIN OPENSSH PRIVATE KEY-----"));
        assertFalse(CustomCommandValidator.containsPossibleSecret("TOKEN=$TOKEN npm test"));
        assertNull(CustomCommandValidator.validate(command("状态", "git status --short")));
    }

    @Test
    public void corruptedWorkspaceDataFailsClosedWithoutLeakingAnotherWorkspace() {
        store.save("workspace-safe", command("安全", "pwd"));
        RuntimeEnvironment.getApplication().getSharedPreferences(
            "termuxpro_custom_commands", 0).edit()
            .putString("commands_workspace-broken", "not-json").commit();

        assertTrue(store.list("workspace-broken").isEmpty());
        assertEquals(1, store.list("workspace-safe").size());
    }

    private static CustomCommand command(String name, String value) {
        return CustomCommand.create(name, value, "~/project", "开发",
            CustomCommand.Confirmation.ALWAYS);
    }
}
