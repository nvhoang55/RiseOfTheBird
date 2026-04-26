package com.riseofthebird.logic;

import com.riseofthebird.data.Hit;
import com.riseofthebird.data.Mouse;
import com.riseofthebird.data.Vec2;

/**
 * Pure functions over {@link Mouse}.
 *
 * <p>None of these functions touch StdDraw, the clock, or any global state.
 * Each one takes the relevant data in and returns the updated data out.
 *
 * <p>Collision detection returns a {@link Hit} sealed type rather than a
 * boolean + side effects, so callers can pattern-match on the result and
 * weave the post-damage mouse into the new world without losing structural
 * information.
 */
public final class Mice {

    private Mice() {}

    /**
     * Advances the mouse's oscillation by one tick. Pure: returns a new
     * {@link Mouse} with updated position, distance, and (if the range was
     * exceeded) flipped step direction. Dead mice do not move.
     */
    public static Mouse advance(Mouse mouse) {
        if (!mouse.isAlive()) {
            return mouse;
        }

        int step = mouse.step();
        if (Math.abs(mouse.distance()) >= Mouse.MAX_RANGE) {
            step = -step;
        }
        int newDistance = mouse.distance() + step;
        Vec2 newPos = new Vec2(mouse.spawn().x() + newDistance, mouse.spawn().y());

        return mouse.withStep(step).withDistance(newDistance).withPos(newPos);
    }

    /**
     * Reduces HP by one (down to a floor of 0), shrinks the sprite, speeds
     * up the oscillation, and latches the {@code justGotHit} flag so the
     * same attacker can't deal damage again before the round ends.
     */
    public static Mouse takeDamage(Mouse mouse) {
        if (!mouse.isAlive()) {
            return mouse;
        }

        int newSize = Math.max(0, mouse.size() - Mouse.SIZE_SHRINK_PER_HIT);
        int newStep = mouse.step();
        if (newStep != 0) {
            int direction = Integer.signum(newStep);
            newStep += direction * Mouse.STEP_GROWTH_PER_HIT;
        }

        return mouse.withHp(mouse.hp() - 1).withSize(newSize).withStep(newStep).withJustGotHit(true);
    }

    /**
     * Checks the mouse against an attacker's geometry for a collision on this
     * tick. Returns a {@link Hit.Landed} carrying the post-damage mouse on
     * overlap (the attacker is knocked back separately by the caller); returns
     * {@link Hit.Missed#INSTANCE} otherwise.
     *
     * <p>The per-round {@code justGotHit} latch suppresses repeat hits from
     * the same attacker until the latch is cleared by
     * {@link #clearHitLatch(Mouse)} between rounds.
     */
    public static Hit collide(Mouse mouse, Vec2 attackerPos, int attackerSize) {
        if (mouse.justGotHit() || !mouse.isAlive()) {
            return Hit.Missed.INSTANCE;
        }
        double reach = mouse.size() / 3.0 + attackerSize / 3.0;
        if (mouse.pos().distanceTo(attackerPos) <= reach) {
            return new Hit.Landed(takeDamage(mouse));
        }
        return Hit.Missed.INSTANCE;
    }

    /**
     * Clears the per-round hit latch so the next round's first overlap
     * counts as a hit. Called by {@link Worlds} between rounds.
     */
    public static Mouse clearHitLatch(Mouse mouse) {
        return mouse.withJustGotHit(false);
    }
}
