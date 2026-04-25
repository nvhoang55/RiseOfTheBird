package com.riseofthebird.logic;

import com.riseofthebird.data.Bird;
import com.riseofthebird.data.ControllerState;

/**
 * Pure functions over {@link ControllerState}.
 *
 * <p>Replaces the original stateful {@code Controller} class and its three
 * nested sub-controllers with a flat function set. None of these touch
 * StdDraw, the clock, or any global state - each one takes the relevant
 * data in and returns the updated data out.
 *
 * <p>Naming convention: {@code tickX} advances UI state by one tick;
 * {@code applyX} writes the locked UI value into the bird's
 * {@link com.riseofthebird.data.BirdState}; {@code activateSkill} latches
 * the skill flag.
 */
public final class Controllers {

    private Controllers() {}

    // ---- Angle ---------------------------------------------------------

    /**
     * Advances the oscillating aim by one tick. Flips direction once the
     * angle overshoots {@link ControllerState#MAX_ANGLE} or dips below zero.
     */
    public static ControllerState tickAngle(ControllerState s) {
        int direction = s.angleDirection();
        if (s.angle() >= ControllerState.MAX_ANGLE || s.angle() < 0) {
            direction = -direction;
        }
        double newAngle = s.angle() + ControllerState.ANGLE_STEP * direction;
        return s.withAngle(newAngle).withAngleDirection(direction);
    }

    /** Locks the bird's launch angle to whatever value the controller is currently displaying. */
    public static Bird applyAngle(ControllerState s, Bird bird) {
        return Birds.replaceState(bird, bird.state().withAngle(s.angle()));
    }

    // ---- Power ---------------------------------------------------------

    /**
     * Advances the oscillating power-bar by one tick. Bounces off either end
     * of the bar and recomputes the derived velocity.
     *
     * @param s         current controller state
     * @param frameCount number of available power-bar frames; pass 0 for
     *                   no-op (e.g. when the asset is missing)
     */
    public static ControllerState tickPower(ControllerState s, int frameCount) {
        if (frameCount <= 0) {
            return s;
        }
        int direction = s.powerDirection();
        int frame = s.powerFrame();
        if (frame <= 0) {
            direction = 1;
        } else if (frame >= frameCount - 1) {
            direction = -1;
        }
        int newFrame = frame + direction;
        double newVelocity = newFrame * ControllerState.VELOCITY_MULTIPLIER;
        return s.withPowerFrame(newFrame).withPowerDirection(direction).withVelocity(newVelocity);
    }

    /** Locks the bird's launch velocity to whatever value the controller is currently displaying. */
    public static Bird applyPower(ControllerState s, Bird bird) {
        return Birds.replaceState(bird, bird.state().withVelocity(s.velocity()));
    }

    // ---- Skill ---------------------------------------------------------

    /** Latches the skill flag. Idempotent: subsequent calls are no-ops. */
    public static ControllerState activateSkill(ControllerState s) {
        return s.withSkillActivated(true);
    }
}
