package com.riseofthebird.data;

import java.util.List;

/**
 * Top-level game state for a single run.
 *
 * <p>Every field that evolves over the course of a run lives here, so the
 * whole game is one record graph. {@link com.riseofthebird.logic.Worlds#tick}
 * is a pure function from {@code (World, InputSnapshot)} to {@code World},
 * and the {@link com.riseofthebird.runtime.GameLoop} simply reassigns its
 * single {@code current} field to the returned value each tick.
 *
 * <p>Bird and mouse lists are read-only views: callers must construct a new
 * {@code World} (typically via the {@code with*} helpers) rather than
 * mutating the lists in place.
 *
 * @param roster       every bird the player will launch this run, in order
 * @param currentBird  index into {@link #roster} of the bird currently being
 *                     aimed or flying; {@code roster.size()} once exhausted
 * @param mice         every live mouse boss; dead mice are removed
 * @param controller   per-round UI state (angle, power, skill latch)
 * @param phase        current round-lifecycle phase
 * @param score        cumulative number of hits scored this run
 * @param playerWon    true if the run ended with the player meeting the win
 *                     condition; only meaningful in the {@link Phase#GAME_OVER}
 *                     phase
 */
public record World(
    List<Bird> roster,
    int currentBird,
    List<Mouse> mice,
    ControllerState controller,
    Phase phase,
    int score,
    boolean playerWon
) {

    /** Number of hits required to win a run. */
    public static final int WIN_THRESHOLD = 3;

    public World {
        if (roster == null) {
            throw new IllegalArgumentException("roster must not be null");
        }
        if (mice == null) {
            throw new IllegalArgumentException("mice must not be null");
        }
        if (controller == null) {
            throw new IllegalArgumentException("controller must not be null");
        }
        if (phase == null) {
            throw new IllegalArgumentException("phase must not be null");
        }
        if (currentBird < 0 || currentBird > roster.size()) {
            throw new IllegalArgumentException(
                "currentBird out of range [0, " + roster.size() + "]: " + currentBird
            );
        }
        if (score < 0) {
            throw new IllegalArgumentException("score must not be negative: " + score);
        }
        // Defensively snapshot the lists so callers cannot mutate them under us.
        roster = List.copyOf(roster);
        mice = List.copyOf(mice);
    }

    /** Builds a fresh start-of-run world from the given roster of birds. */
    public static World freshRun(List<Bird> roster) {
        return new World(
            roster,
            0,
            List.of(Mouse.defaultBoss()),
            ControllerState.freshRound(),
            Phase.READY,
            0,
            false
        );
    }

    /** True while there is still a bird available for the next round. */
    public boolean hasMoreBirds() {
        return currentBird < roster.size();
    }

    /** The bird currently being aimed or flying, or {@code null} if exhausted. */
    public Bird bird() {
        return hasMoreBirds() ? roster.get(currentBird) : null;
    }

    /** True if every mouse on the board has been killed. */
    public boolean allMiceDead() {
        return mice.isEmpty();
    }

    /** True if the run has met the win condition (score threshold or wipe). */
    public boolean isWon() {
        return score >= WIN_THRESHOLD || allMiceDead();
    }

    public World withRoster(List<Bird> newRoster) {
        return new World(newRoster, currentBird, mice, controller, phase, score, playerWon);
    }

    public World withCurrentBird(int newCurrentBird) {
        return new World(roster, newCurrentBird, mice, controller, phase, score, playerWon);
    }

    public World withMice(List<Mouse> newMice) {
        return new World(roster, currentBird, newMice, controller, phase, score, playerWon);
    }

    public World withController(ControllerState newController) {
        return new World(roster, currentBird, mice, newController, phase, score, playerWon);
    }

    public World withPhase(Phase newPhase) {
        return new World(roster, currentBird, mice, controller, newPhase, score, playerWon);
    }

    public World withScore(int newScore) {
        return new World(roster, currentBird, mice, controller, phase, newScore, playerWon);
    }

    public World withPlayerWon(boolean newPlayerWon) {
        return new World(roster, currentBird, mice, controller, phase, score, newPlayerWon);
    }

    /**
     * Replaces the bird at {@link #currentBird} with the given one. Throws
     * if the roster has been exhausted; callers should check {@link #hasMoreBirds()}
     * first.
     */
    public World withCurrentBirdReplaced(Bird newBird) {
        if (!hasMoreBirds()) {
            throw new IllegalStateException("Cannot replace current bird: roster is exhausted");
        }
        Bird[] copy = roster.toArray(Bird[]::new);
        copy[currentBird] = newBird;
        return withRoster(List.of(copy));
    }
}
