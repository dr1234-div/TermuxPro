package com.termux.app;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** 产品身份和非危险视觉语义的 Manifest 回归。 */
public class ManifestProductIdentityTest {
    @Test
    public void settingsUsesTermuxProThemeInsteadOfUpstreamRedPrimaryTheme() throws Exception {
        String manifest = new String(Files.readAllBytes(Paths.get("src/main/AndroidManifest.xml")),
            StandardCharsets.UTF_8);
        assertTrue(manifest.contains("android:name=\".app.activities.SettingsActivity\"\n"
            + "            android:exported=\"true\"\n"
            + "            android:label=\"@string/title_activity_termux_settings\"\n"
            + "            android:theme=\"@style/Theme.TermuxPro.DayNight.NoActionBar\""));
    }
}
