package com.riseofthebird.data;

/**
 * Immutable snapshot of keyboard state for a single tick.
 *
 * <p>Replaces the stateful {@code InputState} class with a value type that
 * carries both the current and previous tick's key state, so edge events
 * (press / release transitions) are derivable as pure accessors.
 *
 * <p>The polling itself - the only impure step, since it reads
 * {@code StdDraw.isKeyPressed} - lives in
 * {@link com.riseofthebird.input.Input#poll}.
 *
 * @param spaceDown true if SPACE is held this tick
 * @param spacePrev true if SPACE was held last tick
 * @param enterDown true if ENTER is held this tick
 * @param enterPrev true if ENTER was held last tick
 */
public record InputSnapshot(
    boolean spaceDown,
    boolean spacePrev,
    boolean enterDown,
    boolean enterPrev
) {

    /** All keys released; suitable as the initial value before the first poll. */
    public static final InputSnapshot EMPTY = new InputSnapshot(false, false, false, false);

    /** Rising edge: SPACE was released last tick and is held this tick. */
    public boolean wasSpacePressed() {
        return spaceDown && !spacePrev;
    }

    /** Falling edge: SPACE was held last tick and is released this tick. */
    public boolean wasSpaceReleased() {
        return !spaceDown && spacePrev;
    }

    /** Rising edge: ENTER was released last tick and is held this tick. */
    public boolean wasEnterPressed() {
        return enterDown && !enterPrev;
    }
}
