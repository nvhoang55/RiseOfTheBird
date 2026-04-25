package com.riseofthebird.logic;

import com.riseofthebird.data.Lightning;
import com.riseofthebird.data.Mouse;
import com.riseofthebird.data.Vec2;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure functions over {@link Lightning} projectiles.
 *
 * <p>None of these functions touch StdDraw, the clock, or any global state.
 * Each one takes the relevant data in and returns the updated data out.
 *
 * <p>The advance step both moves the bolt and checks it against every live
 * mouse for a collision; on overlap the bolt latches as struck and the
 * mouse takes one point of damage. Caller weaves the resulting
 * {@link Advance} record into the enclosing {@code Thord} and {@code World}.
 */
public final class Lightnings {

    private Lightnings() {}

    /**
     * Advances the bolt one tick: checks for collisions against any live
     * mouse in {@code mice}, applies damage if it hits, and steps the bolt
     * forward along its initial heading.
     *
     * <p>No-op when the bolt has not been spawned yet, or when it has
     * already struck a target.
     *
     * @return the post-advance bolt, the post-collision mouse list, and the
     *         number of fresh hits scored on this tick (0 or 1)
     */
    public static Advance advance(Lightning bolt, List<Mouse> mice) {
        if (!bolt.spawned() || bolt.struck()) {
            return new Advance(bolt, mice, 0);
        }

        // Collision: walk the mouse list once, swap in the damaged mouse if
        // we overlap a live one, and latch the bolt as struck.
        List<Mouse> nextMice = new ArrayList<>(mice.size());
        boolean hit = false;
        Lightning nextBolt = bolt;

        for (Mouse mouse : mice) {
            if (!hit && mouse.isAlive() && overlaps(bolt, mouse)) {
                nextMice.add(Mice.takeDamage(mouse));
                nextBolt = bolt.markStruck();
                hit = true;
            } else {
                nextMice.add(mouse);
            }
        }

        // Even on a hit the bolt advances one more step so the renderer can
        // paint the strike at the impact frame; the next tick will short-circuit
        // via the struck-latch check above.
        nextBolt = step(nextBolt);

        return new Advance(nextBolt, nextMice, hit ? 1 : 0);
    }

    /** Translates the bolt one tick along its heading. */
    private static Lightning step(Lightning bolt) {
        double angleRad = Math.toRadians(bolt.angle());
        Vec2 next = bolt.pos().add(
            Lightning.SPEED * Math.cos(angleRad),
            Lightning.SPEED * Math.sin(angleRad)
        );
        return bolt.withPos(next);
    }

    /** Distance-based overlap check using the canonical {@code SIZE / 3} reach. */
    private static boolean overlaps(Lightning bolt, Mouse mouse) {
        double reach = Lightning.SIZE / 3.0 + mouse.size() / 3.0;
        return bolt.pos().distanceTo(mouse.pos()) <= reach;
    }

    /**
     * Outcome of one advance tick: the post-step bolt, the mouse list with
     * any damaged mouse swapped in, and the number of fresh hits this tick.
     */
    public record Advance(Lightning bolt, List<Mouse> mice, int hits) {}
}
