package com.riseofthebird.data;

/**
 * Outcome of a collision check between an attacker and a single mouse on a
 * given tick.
 *
 * <p>Modelled as a sealed sum type rather than a {@code boolean} + side
 * effects so the caller can pattern-match on the result and reconstruct
 * the affected entities without losing structural information. This is the
 * canonical "make illegal states unrepresentable" application:
 * a {@link Landed} carries the post-damage {@link Mouse}; a {@link Missed}
 * structurally cannot, so there is no way to forget the new state on a hit
 * or invent one on a miss.
 */
public sealed interface Hit permits Hit.Landed, Hit.Missed {

    /** No overlap, or overlap suppressed by the per-round latch. */
    record Missed() implements Hit {

        public static final Missed INSTANCE = new Missed();
    }

    /**
     * The attacker overlapped a live mouse on this tick.
     *
     * @param damaged the mouse after taking one point of damage; HP, size,
     *                step, and {@code justGotHit} latch are all already
     *                updated
     */
    record Landed(Mouse damaged) implements Hit {}
}
