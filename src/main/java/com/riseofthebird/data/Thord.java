package com.riseofthebird.data;

import io.soabase.recordbuilder.core.RecordBuilder;

/**
 * Thord: a fast bird whose skill grants a one-time velocity boost and hurls
 * a {@link Lightning} bolt in the direction he is currently flying.
 *
 * <p>Carries a {@link Lightning} component in addition to the shared
 * {@link BirdState}: the bolt's lifetime is tied to a single Thord round
 * and travels independently of the bird itself once spawned.
 *
 * <p>{@code with*} copy methods (including the {@link Bird#withState}
 * contract method) are generated at compile time by RecordBuilder via the
 * {@link ThordBuilder.With} marker interface. The generated
 * {@code withState(BirdState)} returns {@code Thord}, which covariantly
 * satisfies {@link Bird#withState(BirdState)}'s {@code Bird} return type.
 *
 * @param state shared in-flight physics and visual state
 * @param bolt  Thord's lightning bolt; {@link Lightning#DORMANT} until the
 *              skill is activated
 */
@RecordBuilder
public record Thord(BirdState state, Lightning bolt) implements Bird, ThordBuilder.With {
    /** Spawn position shared with {@link Bulk}, mirroring the original game. */
    public static final Vec2 SPAWN = new Vec2(-700, -300);

    /** Render size in world units. */
    public static final int SIZE = 200;

    /** One-shot velocity boost applied on the first skill-activation tick. */
    public static final double VELOCITY_BOOST = 50;

    /** Builds a fresh Thord at his spawn point with a dormant bolt. */
    public static Thord atSpawn() {
        return new Thord(BirdState.atSpawn(SPAWN), Lightning.DORMANT);
    }
}
