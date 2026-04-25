package com.riseofthebird.data;

/**
 * Bulk: a heavyweight bird whose skill grows its hitbox and increases gravity
 * mid-flight, causing a steep "stomp" descent.
 *
 * <p>Carries no projectile component; its skill modifies the bird's own
 * physics in place (within {@link BirdState}). Damage is registered through
 * the standard bird-mouse collision check, not via an independent projectile.
 *
 * @param state  shared in-flight physics and visual state
 * @param size   current render and hitbox size in world units; grows each
 *               tick the skill is active
 */
public record Bulk(BirdState state, int size) implements Bird {

    /** Spawn position shared with {@link Thord}, mirroring the original game. */
    public static final Vec2 SPAWN = new Vec2(-700, -300);

    /** Render and hitbox size at spawn. */
    public static final int INITIAL_SIZE = 170;

    /** Per-tick hitbox growth while the skill is active. */
    public static final int SIZE_GROWTH_PER_TICK = 20;

    /** Per-tick gravity growth while the skill is active. */
    public static final double GRAVITY_GROWTH_PER_TICK = 0.1;

    /** Builds a fresh Bulk at his spawn point at the initial size. */
    public static Bulk atSpawn() {
        return new Bulk(BirdState.atSpawn(SPAWN), INITIAL_SIZE);
    }

    @Override
    public Bird withState(BirdState newState) {
        return new Bulk(newState, size);
    }

    public Bulk withSize(int newSize) {
        return new Bulk(state, newSize);
    }
}
