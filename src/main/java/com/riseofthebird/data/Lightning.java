package com.riseofthebird.data;

import io.soabase.recordbuilder.core.RecordBuilder;

/**
 * Thord's skill projectile.
 *
 * <p>Travels in a straight line at a fixed speed in the direction Thord was
 * facing at the moment the skill triggered. On overlap with any live mouse
 * it deals one point of damage and self-destructs ({@link #struck} latches).
 *
 * <p>Lightning is a component of {@link Thord} and is structurally absent
 * from {@link Bulk}: the type system makes "a Bulk with a lightning bolt"
 * unrepresentable, which is the point.
 *
 * <p>{@code with*} copy methods are generated at compile time by
 * RecordBuilder via the {@link LightningBuilder.With} marker interface.
 *
 * @param pos     current position in world coordinates
 * @param angle   heading in degrees, fixed at spawn time
 * @param spawned true once the bolt has been emitted by Thord
 * @param struck  true once the bolt has hit a mouse and dissipated
 */
@RecordBuilder
public record Lightning(Vec2 pos, double angle, boolean spawned, boolean struck) implements LightningBuilder.With {
    /** Per-tick travel speed in world units. */
    public static final double SPEED = 170;

    /** Render size (also drives the collision radius via {@code SIZE / 3}). */
    public static final double SIZE = 210;

    /** A lightning bolt that has not yet been spawned by its owner. */
    public static final Lightning DORMANT = new Lightning(Vec2.ZERO, 0, false, false);

    /** Spawns the bolt at the given position and heading. */
    public Lightning spawnedAt(Vec2 newPos, double newAngle) {
        return new Lightning(newPos, newAngle, true, false);
    }

    /** Latches the bolt as having struck a target. */
    public Lightning markStruck() {
        return new Lightning(pos, angle, spawned, true);
    }
}
