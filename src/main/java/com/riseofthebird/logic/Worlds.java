package com.riseofthebird.logic;

import com.riseofthebird.data.Bird;
import com.riseofthebird.data.Bulk;
import com.riseofthebird.data.ControllerState;
import com.riseofthebird.data.Hit;
import com.riseofthebird.data.InputSnapshot;
import com.riseofthebird.data.Mouse;
import com.riseofthebird.data.Phase;
import com.riseofthebird.data.Thord;
import com.riseofthebird.data.Vec2;
import com.riseofthebird.data.World;
import java.util.ArrayList;
import java.util.List;

/**
 * Central pure transition function: {@code (World, InputSnapshot) -> World}.
 *
 * <p>This is the only function the {@link com.riseofthebird.runtime.GameLoop}
 * calls per tick to evolve game state. Dispatch over the round-lifecycle
 * {@link Phase} is an exhaustive {@code switch}, so adding a new phase is a
 * compile-time deliverable.
 *
 * <p>None of these functions touch StdDraw, the clock, or any global state.
 * Side effects (rendering, sleeping, polling the keyboard) live in the
 * runtime, render, and input modules respectively.
 */
public final class Worlds {

    /** World width used by the bird overreach check. */
    public static final int WORLD_WIDTH = 1900;

    /** World height used by the bird overreach check. */
    public static final int WORLD_HEIGHT = 1000;

    private Worlds() {}

    /**
     * Evolves the world by one tick under the supplied input snapshot.
     * Pure: returns a new {@link World}; does not mutate the input.
     *
     * @param powerFrameCount number of available power-bar frames; the
     *                        runtime reads this from the asset layer, tests
     *                        pass whatever value matches the scenario
     */
    public static World tick(World w, InputSnapshot in, int powerFrameCount) {
        return switch (w.phase()) {
            case READY -> tickReady(w, in);
            case AIMING_ANGLE -> tickAimingAngle(w, in);
            case AIMING_POWER -> tickAimingPower(w, in, powerFrameCount);
            case FLYING -> tickFlying(w, in);
            case GAME_OVER -> tickGameOver(w, in);
        };
    }

    // =====================================================================
    // Phase handlers
    // =====================================================================

    private static World tickReady(World w, InputSnapshot in) {
        // Bird sits at spawn; mice keep oscillating in the background.
        World next = withMiceAdvanced(w);
        if (in.wasSpacePressed() && next.hasMoreBirds()) {
            return next.withPhase(Phase.AIMING_ANGLE);
        }
        return next;
    }

    private static World tickAimingAngle(World w, InputSnapshot in) {
        if (!w.hasMoreBirds()) {
            return w.withPhase(Phase.GAME_OVER).withPlayerWon(false);
        }

        ControllerState ctrl = w.controller();
        Bird bird = w.bird();

        // While SPACE is held, the angle oscillates and is mirrored onto the bird.
        if (in.spaceDown()) {
            ctrl = tickAngle(ctrl);
            bird = Birds.replaceState(bird, bird.state().withAngle(ctrl.angle()));
        }

        World next = w.withController(ctrl).withCurrentBirdReplaced(bird);
        next = withMiceAdvanced(next);

        // Releasing SPACE locks the angle and moves to the power phase.
        if (in.wasSpaceReleased()) {
            bird = Birds.replaceState(bird, bird.state().withAngle(ctrl.angle()));
            next = next.withCurrentBirdReplaced(bird).withPhase(Phase.AIMING_POWER);
        }
        return next;
    }

    private static World tickAimingPower(World w, InputSnapshot in, int powerFrameCount) {
        if (!w.hasMoreBirds()) {
            return w.withPhase(Phase.GAME_OVER).withPlayerWon(false);
        }

        ControllerState ctrl = w.controller();

        if (in.spaceDown()) {
            ctrl = tickPower(ctrl, powerFrameCount);
        }

        World next = w.withController(ctrl);
        next = withMiceAdvanced(next);

        if (in.wasSpaceReleased()) {
            Bird launched = Birds.replaceState(next.bird(), next.bird().state().withVelocity(ctrl.velocity()));
            next = next.withCurrentBirdReplaced(launched).withPhase(Phase.FLYING);
        }
        return next;
    }

    private static World tickFlying(World w, InputSnapshot in) {
        if (!w.hasMoreBirds()) {
            return endRound(w);
        }

        ControllerState ctrl = w.controller();

        // SPACE during flight latches the skill; subsequent presses are no-ops.
        if (in.wasSpacePressed()) {
            ctrl = ctrl.withSkillActivated(true);
        }

        Bird bird = w.bird();
        List<Mouse> mice = w.mice();
        int score = w.score();

        // Drive the skill (may spawn / advance an independent projectile).
        if (ctrl.skillActivated()) {
            Birds.SkillResult skill = Birds.useSkill(bird, mice);
            bird = skill.bird();
            mice = skill.mice();
            score += skill.hits();
        }

        // Advance the bird's own trajectory.
        bird = Birds.advance(bird);

        // Bird-vs-mouse collisions and mouse motion.
        CollisionPass pass = collideAndAdvance(mice, bird);
        mice = pass.mice();
        bird = pass.bird();
        score += pass.hits();

        World next = w.withController(ctrl).withCurrentBirdReplaced(bird).withMice(mice).withScore(score);

        boolean roundEnded =
            Birds.isOverreached(bird.state().pos(), Birds.sizeOf(bird), WORLD_WIDTH, WORLD_HEIGHT) ||
            score >= World.WIN_THRESHOLD ||
            next.allMiceDead();

        return roundEnded ? endRound(next) : next;
    }

    private static World tickGameOver(World w, InputSnapshot in) {
        // ENTER restarts the run with the original roster.
        if (in.wasEnterPressed()) {
            // Re-spawn each bird kind from a fresh state to clear any per-round
            // physics that survived from the previous run.
            List<Bird> freshRoster = new ArrayList<>(w.roster().size());
            for (Bird b : w.roster()) {
                freshRoster.add(respawn(b));
            }
            return World.freshRun(freshRoster);
        }
        return w;
    }

    // =====================================================================
    // Controller oscillators
    // =====================================================================

    /**
     * Advances the oscillating aim by one tick. Flips direction once the
     * angle overshoots {@link ControllerState#MAX_ANGLE} or dips below zero.
     */
    private static ControllerState tickAngle(ControllerState s) {
        int direction = s.angleDirection();
        if (s.angle() >= ControllerState.MAX_ANGLE || s.angle() < 0) {
            direction = -direction;
        }
        double newAngle = s.angle() + ControllerState.ANGLE_STEP * direction;
        return s.withAngle(newAngle).withAngleDirection(direction);
    }

    /**
     * Advances the oscillating power-bar by one tick. Bounces off either end
     * of the bar and recomputes the derived velocity. {@code frameCount <= 0}
     * is a no-op (e.g. when the asset is missing).
     */
    private static ControllerState tickPower(ControllerState s, int frameCount) {
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

    // =====================================================================
    // Helpers
    // =====================================================================

    /**
     * Advances every live mouse one tick. Used by phases where the bird is
     * not yet flying, so no collision check is needed.
     */
    private static World withMiceAdvanced(World w) {
        List<Mouse> next = new ArrayList<>(w.mice().size());
        for (Mouse mouse : w.mice()) {
            Mouse advanced = Mice.advance(mouse);
            if (advanced.isAlive()) {
                next.add(advanced);
            }
            // Dead mice are dropped from the list.
        }
        return w.withMice(next);
    }

    /**
     * Per-tick collision check + motion for every mouse against the
     * currently-flying bird. Hits update the mouse and knock the bird back;
     * dead mice are dropped from the returned list.
     */
    private static CollisionPass collideAndAdvance(List<Mouse> mice, Bird bird) {
        List<Mouse> next = new ArrayList<>(mice.size());
        Bird currentBird = bird;
        int hits = 0;

        for (Mouse mouse : mice) {
            Vec2 attackerPos = currentBird.state().pos();
            int attackerSize = Birds.sizeOf(currentBird);
            Hit hit = Mice.collide(mouse, attackerPos, attackerSize);
            Mouse afterHit = switch (hit) {
                case Hit.Landed(Mouse damaged) -> damaged;
                case Hit.Missed m -> mouse;
            };
            if (hit instanceof Hit.Landed) {
                currentBird = Birds.knockBack(currentBird);
                hits++;
            }

            Mouse afterMove = Mice.advance(afterHit);
            if (afterMove.isAlive()) {
                next.add(afterMove);
            }
        }

        return new CollisionPass(next, currentBird, hits);
    }

    /**
     * Wraps up the current round: clears per-round latches, advances to the
     * next bird, and computes the next phase based on whether the player
     * has met the win condition or exhausted the roster.
     */
    private static World endRound(World w) {
        // Clear the per-round hit latch on every surviving mouse.
        List<Mouse> latchCleared = new ArrayList<>(w.mice().size());
        for (Mouse mouse : w.mice()) {
            latchCleared.add(Mice.clearHitLatch(mouse));
        }

        World next = w
            .withMice(latchCleared)
            .withController(ControllerState.freshRound())
            .withCurrentBird(w.currentBird() + 1);

        if (next.isWon()) {
            return next.withPhase(Phase.GAME_OVER).withPlayerWon(true);
        }
        if (!next.hasMoreBirds()) {
            return next.withPhase(Phase.GAME_OVER).withPlayerWon(false);
        }
        return next.withPhase(Phase.READY);
    }

    /**
     * Returns a fresh at-spawn instance of the same bird kind, used when
     * the player restarts the run.
     */
    private static Bird respawn(Bird bird) {
        return switch (bird) {
            case Thord t -> Thord.atSpawn();
            case Bulk b -> Bulk.atSpawn();
        };
    }

    /** Result of {@link #collideAndAdvance}: post-tick mice, bird, and hit count. */
    private record CollisionPass(List<Mouse> mice, Bird bird, int hits) {}
}
