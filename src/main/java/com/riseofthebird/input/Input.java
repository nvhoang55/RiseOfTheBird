package com.riseofthebird.input;

import com.riseofthebird.data.InputSnapshot;
import edu.princeton.cs.introcs.StdDraw;
import java.awt.event.KeyEvent;

/**
 * Reads keyboard state from {@link StdDraw} and returns an immutable
 * {@link InputSnapshot}.
 *
 * <p>This is the only impure step in the input pipeline: it reads the live
 * StdDraw key state. Every consumer downstream operates on the returned
 * snapshot value, so edge events (press / release transitions) are stable
 * for the entire tick.
 */
public final class Input {

    private Input() {}

    /**
     * Polls the current keyboard state and folds it together with the
     * previous tick's snapshot to produce a fresh {@link InputSnapshot}.
     * Must be called exactly once per tick, at the very top of the tick.
     *
     * @param previous the snapshot returned from the previous tick, or
     *                 {@link InputSnapshot#EMPTY} for the first tick
     */
    public static InputSnapshot poll(InputSnapshot previous) {
        boolean spaceDown = StdDraw.isKeyPressed(KeyEvent.VK_SPACE);
        boolean enterDown = StdDraw.isKeyPressed(KeyEvent.VK_ENTER);
        return new InputSnapshot(
            spaceDown,
            previous.spaceDown(),
            enterDown,
            previous.enterDown()
        );
    }
}
