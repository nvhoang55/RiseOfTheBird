package com.riseofthebird.data;

import io.soabase.recordbuilder.core.RecordBuilder;

/**
 * Per-round UI state for angle aiming, power charging, and skill activation.
 *
 * <p>Replaces the original stateful {@code Controller} class and its three
 * nested sub-controllers with a single immutable record. The corresponding
 * tick / apply / activate operations live in
 * {@link com.riseofthebird.logic.Controllers}.
 *
 * <p>Reset between rounds is just {@link #freshRound()} - a fresh value
 * rather than a mutating {@code reset()} call.
 *
 * <p>{@code with*} copy methods are generated at compile time by
 * RecordBuilder via the {@link ControllerStateBuilder.With} marker interface.
 *
 * @param angle             current aiming angle in degrees, oscillating in
 *                          {@code [0, MAX_ANGLE]}
 * @param angleDirection    sign of the angle's per-tick change ({@code +1}
 *                          climbing, {@code -1} falling)
 * @param powerFrame        current index into the power-bar frame array;
 *                          oscillates in {@code [0, frames-1]}
 * @param powerDirection    sign of the power frame's per-tick change
 * @param velocity          current power-bar velocity, derived from
 *                          {@code powerFrame * VELOCITY_MULTIPLIER}
 * @param skillActivated    true once the player has triggered the bird's
 *                          skill during the FLYING phase of this round
 */
@RecordBuilder
public record ControllerState(
    double angle,
    int angleDirection,
    int powerFrame,
    int powerDirection,
    double velocity,
    boolean skillActivated
) implements ControllerStateBuilder.With {
    /** Maximum aiming angle in degrees. */
    public static final int MAX_ANGLE = 80;

    /** Per-tick angle change in degrees. */
    public static final int ANGLE_STEP = 3;

    /** Multiplier applied to the power frame index to derive launch velocity. */
    public static final int VELOCITY_MULTIPLIER = 30;

    /** Start-of-round value: zeroed angle and power, climbing direction, skill latch off. */
    public static ControllerState freshRound() {
        return new ControllerState(0, 1, 0, 1, 0, false);
    }
}
