package com.riseofthebird.data;

/**
 * Thord: a fast bird whose skill grants a one-time velocity boost and hurls
 * a {@link Lightning} bolt in the direction he is currently flying.
 *
 * <p>Carries a {@link Lightning} component in addition to the shared
 * {@link BirdState}: the bolt's lifetime is tied to a single Thord round
 * and travels independently of the bird itself once spawned.
 *
 * @param state shared in-flight physics and visual state
 * @param bolt  Thord's lightning bolt; {@link Lightning#DORMANT} until the
 *              skill is activated
 */
public record Thord(BirdState state, Lightning bolt) implements Bird {

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

    @Override
    public Bird withState(BirdState newState) {
        return new Thord(newState, bolt);
    }

    public Thord withBolt(Lightning newBolt) {
        return new Thord(state, newBolt);
    }
}
