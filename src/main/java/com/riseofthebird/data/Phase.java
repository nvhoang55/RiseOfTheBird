package com.riseofthebird.data;

/**
 * Lifecycle of a single round, exhaustively dispatched on by
 * {@link com.riseofthebird.logic.Worlds#tick}.
 *
 * <pre>
 *     READY -&gt; AIMING_ANGLE -&gt; AIMING_POWER -&gt; FLYING
 *                                                  |
 *                                                  v
 *               (next bird) READY  &lt;------  end of round  ------&gt;  GAME_OVER
 *                                                                       |
 *                                                                  ENTER press
 *                                                                       v
 *                                                                  new run -&gt; READY
 * </pre>
 */
public enum Phase {
    /** Bird at spawn; SPACE press starts aiming. */
    READY,

    /** Angle oscillates while SPACE is held; release locks the angle. */
    AIMING_ANGLE,

    /** Power oscillates while SPACE is held; release launches. */
    AIMING_POWER,

    /** Bird in flight; SPACE activates the skill. */
    FLYING,

    /** End-of-run screen; ENTER starts a new run. */
    GAME_OVER
}
