package com.riseofthebird.input;

import edu.princeton.cs.introcs.StdDraw;
import java.awt.event.KeyEvent;

/**
 * Polls keyboard state once per game tick and exposes edge-triggered events.
 *
 * <p>Edge signals ({@link #wasSpacePressed()}, {@link #wasSpaceReleased()},
 * {@link #wasEnterPressed()}) are stable for the whole tick they are
 * reported in.
 */
public final class InputState {

    private boolean spaceDown;
    private boolean spacePrev;
    private boolean enterDown;
    private boolean enterPrev;

    /**
     * Snapshots current keyboard state. Must be called exactly once per tick,
     * at the very top of the tick, before any consumer queries the input state.
     */
    public void poll() {
        spacePrev = spaceDown;
        enterPrev = enterDown;
        spaceDown = StdDraw.isKeyPressed(KeyEvent.VK_SPACE);
        enterDown = StdDraw.isKeyPressed(KeyEvent.VK_ENTER);
    }

    public boolean isSpaceDown() {
        return spaceDown;
    }

    public boolean wasSpacePressed() {
        return spaceDown && !spacePrev;
    }

    public boolean wasSpaceReleased() {
        return !spaceDown && spacePrev;
    }

    public boolean isEnterDown() {
        return enterDown;
    }

    public boolean wasEnterPressed() {
        return enterDown && !enterPrev;
    }

    /** Resets edge-detection history so the next poll cannot report a false edge. */
    public void reset() {
        spaceDown = false;
        spacePrev = false;
        enterDown = false;
        enterPrev = false;
    }
}
