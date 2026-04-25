package com.riseofthebird.data;

/**
 * Physical and visual state every bird carries in flight.
 *
 * <p>Extracted as its own record so that operations on bird physics
 * (advance, knock-back, reset, change form) can be written once over a
 * uniform shape, regardless of which {@link Bird} subtype currently owns
 * the state. Without this middleman, every operation would have to
 * reconstruct each {@link Bird} record by hand via a switch over the
 * sealed hierarchy - workable for two subtypes, painful for three or more.
 *
 * <p>Trade-off acknowledged: this is one level of indirection that a
 * strict reading of "model the data, the whole data, and nothing but
 * the data" might push back on. The justification is that {@code BirdState}
 * does model one thing - the in-flight state shared by every bird kind -
 * and {@link Bird} subtypes compose it with their kind-specific extras
 * (e.g. {@link Lightning} for {@code Thord}).
 *
 * @param spawn          launch position; never moves once a round starts
 * @param pos            current position in world coordinates
 * @param angle          current heading in degrees
 * @param velocity       initial launch velocity (signed; flipped on knock-back)
 * @param gravity        per-tick gravity constant; grows on knock-back
 * @param time           ticks elapsed since launch (used by the parabola)
 * @param form           which sprite frame to render
 * @param skillActivated true once the player has triggered the skill this round
 */
public record BirdState(
    Vec2 spawn,
    Vec2 pos,
    double angle,
    double velocity,
    double gravity,
    double time,
    Form form,
    boolean skillActivated
) {

    /** Default gravity used by every bird at spawn. */
    public static final double INITIAL_GRAVITY = 1.5;

    /** Builds the at-spawn state for a bird launched from the given coordinate. */
    public static BirdState atSpawn(Vec2 spawn) {
        return new BirdState(spawn, spawn, 0, 0, INITIAL_GRAVITY, 0, Form.REGULAR, false);
    }

    public BirdState withPos(Vec2 newPos) {
        return new BirdState(spawn, newPos, angle, velocity, gravity, time, form, skillActivated);
    }

    public BirdState withAngle(double newAngle) {
        return new BirdState(spawn, pos, newAngle, velocity, gravity, time, form, skillActivated);
    }

    public BirdState withVelocity(double newVelocity) {
        return new BirdState(spawn, pos, angle, newVelocity, gravity, time, form, skillActivated);
    }

    public BirdState withGravity(double newGravity) {
        return new BirdState(spawn, pos, angle, velocity, newGravity, time, form, skillActivated);
    }

    public BirdState withTime(double newTime) {
        return new BirdState(spawn, pos, angle, velocity, gravity, newTime, form, skillActivated);
    }

    public BirdState withForm(Form newForm) {
        return new BirdState(spawn, pos, angle, velocity, gravity, time, newForm, skillActivated);
    }

    public BirdState withSkillActivated(boolean newSkillActivated) {
        return new BirdState(spawn, pos, angle, velocity, gravity, time, form, newSkillActivated);
    }
}
