package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import com.termux.shared.logger.ForegroundFeedbackHost;
import com.termux.shared.logger.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TerminalFeedbackControllerTest {

    @Test
    public void showAlwaysCarriesVisibleTextAndHideClearsSurface() {
        Context context = RuntimeEnvironment.getApplication();
        TextView banner = new TextView(context);
        banner.setVisibility(View.GONE);
        TerminalFeedbackController controller = new TerminalFeedbackController(banner);

        controller.show("已切换到会话：开发", false);

        assertEquals(View.VISIBLE, banner.getVisibility());
        assertEquals("已切换到会话：开发", banner.getText().toString());
        assertEquals("已切换到会话：开发", banner.getContentDescription().toString());

        controller.hide();
        assertEquals(View.GONE, banner.getVisibility());
    }

    @Test
    public void blankFeedbackCannotCreateAnEmptyVisibleBlock() {
        TextView banner = new TextView(RuntimeEnvironment.getApplication());
        banner.setVisibility(View.GONE);
        TerminalFeedbackController controller = new TerminalFeedbackController(banner);

        controller.show("   ", true);

        assertEquals(View.GONE, banner.getVisibility());
        assertEquals("", banner.getText().toString());
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    public void sharedLoggerRoutesForegroundFeedbackToApplicationSurface() {
        FeedbackContext context = new FeedbackContext(RuntimeEnvironment.getApplication());

        Logger.showToast(context, "连接失败，请检查网络", true);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertEquals("连接失败，请检查网络", context.message);
        assertTrue(context.longDuration);
    }

    private static final class FeedbackContext extends ContextWrapper
        implements ForegroundFeedbackHost {

        String message;
        boolean longDuration;

        FeedbackContext(Context base) {
            super(base);
        }

        @Override
        public void showForegroundFeedback(String text, boolean longDuration) {
            this.message = text;
            this.longDuration = longDuration;
        }
    }
}
