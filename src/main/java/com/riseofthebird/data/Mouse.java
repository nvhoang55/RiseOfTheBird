package com.riseofthebird.data;

import io.soabase.recordbuilder.core.RecordBuilder;

/**
 * Mouseleficent: the boss enemy.
 *
 * <p>Oscillates back and forth around its spawn coordinate within a fixed
 * range, taking up to {@link #MAX_HP} hits before dying. Each hit shrinks
 * the sprite, increases the oscillation step, and advances the sprite-array
 * index so the rendered frame matches the remaining HP.
 *
 * <p>{@code with*} copy methods are generated at compile time by
 * RecordBuilder via the {@link MouseBuilder.With} marker interface.
 *
 * @param spawn      anchor point around which the mouse oscillates
 * @param pos        current position in world coordinates
 * @param hp         remaining hit points; {@code 0} means dead
 * @param size       current render and hitbox size in world units
 * @param step       per-tick horizontal displacement; signed (direction)
 * @param distance   signed accumulated displacement from {@link #spawn}
 * @param justGotHit latches after a hit so a single bird cannot deal damage
 *                   every tick; cleared between rounds
 */
@RecordBuilder
public record Mouse(
    Vec2 spawn,
    Vec2 pos,
    int hp,
    int size,
    int step,
    int distance,
    boolean justGotHit
) implements MouseBuilder.With {
    /** Maximum hit points a freshly-spawned boss has. */
    public static final int MAX_HP = 3;

    /** Maximum signed displacement from {@link #spawn} before the step flips. */
    public static final int MAX_RANGE = 150;

    /** Per-tick step magnitude at full HP. */
    public static final int INITIAL_STEP = 40;

    /** How much the sprite shrinks each time the boss takes damage. */
    public static final int SIZE_SHRINK_PER_HIT = 50;

    /** How much the per-tick step magnitude grows each time the boss takes damage. */
    public static final int STEP_GROWTH_PER_HIT = 40;

    /** Default spawn position, mirroring the original game. */
    public static final Vec2 DEFAULT_SPAWN = new Vec2(700, -300);

    /** Default render size at full HP. */
    public static final int DEFAULT_SIZE = 225;

    public Mouse {
        if (hp < 0) {
            throw new IllegalArgumentException("Mouse hp must not be negative: " + hp);
        }
        if (hp > MAX_HP) {
            throw new IllegalArgumentException("Mouse hp must not exceed " + MAX_HP + ": " + hp);
        }
        if (size < 0) {
            throw new IllegalArgumentException("Mouse size must not be negative: " + size);
        }
    }

    /** Builds a fresh Mouseleficent boss at the canonical spawn position. */
    public static Mouse defaultBoss() {
        return new Mouse(DEFAULT_SPAWN, DEFAULT_SPAWN, MAX_HP, DEFAULT_SIZE, INITIAL_STEP, 0, false);
    }

    public boolean isAlive() {
        return hp > 0;
    }
}
