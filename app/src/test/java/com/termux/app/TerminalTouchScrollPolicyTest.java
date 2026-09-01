package com.termux.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.InputDevice;
import android.view.MotionEvent;

import com.termux.view.TerminalView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** 验证手机触摸滑动不会退化为命令历史方向键，外接鼠标滚轮仍走程序内滚动兼容路径。 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "zh-rCN")
public class TerminalTouchScrollPolicyTest {

    @Test
    public void touchScrollAlwaysForcesScrollback() {
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 16, 128, 0);
        try {
            assertTrue(TerminalView.shouldForceScrollbackForScrollEvent(event));
            assertTrue(TerminalView.shouldForceScrollbackForScrollEvent(event,
                TerminalView.TOUCH_SCROLL_MODE_SCROLLBACK, true));
        } finally {
            event.recycle();
        }
    }

    @Test
    public void stylusLikeNonMouseScrollForcesScrollback() {
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 16, 128, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            assertTrue(TerminalView.shouldForceScrollbackForScrollEvent(event));
        } finally {
            event.recycle();
        }
    }

    @Test
    public void externalMouseScrollKeepsTerminalProgramCompatibility() {
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_SCROLL, 16, 128, 0);
        event.setSource(InputDevice.SOURCE_MOUSE);
        try {
            assertFalse(TerminalView.shouldForceScrollbackForScrollEvent(event));
            assertFalse(TerminalView.shouldForceScrollbackForScrollEvent(event,
                TerminalView.TOUCH_SCROLL_MODE_SCROLLBACK, true));
        } finally {
            event.recycle();
        }
    }

    @Test
    public void tuiModeSendsWheelOnlyWhenMouseTrackingActive() {
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 16, 128, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            assertFalse(TerminalView.shouldForceScrollbackForScrollEvent(event,
                TerminalView.TOUCH_SCROLL_MODE_TUI, true));
            assertTrue(TerminalView.shouldForceScrollbackForScrollEvent(event,
                TerminalView.TOUCH_SCROLL_MODE_TUI, false));
        } finally {
            event.recycle();
        }
    }

    @Test
    public void unknownTouchScrollModeFallsBackToScrollback() {
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 16, 128, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            assertTrue(TerminalView.shouldForceScrollbackForScrollEvent(event, "legacy", true));
        } finally {
            event.recycle();
        }
    }

    @Test
    public void missingEventUsesLegacyCompatibilityPath() {
        assertFalse(TerminalView.shouldForceScrollbackForScrollEvent(null));
        assertFalse(TerminalView.shouldForceScrollbackForScrollEvent(null,
            TerminalView.TOUCH_SCROLL_MODE_TUI, true));
    }
}
