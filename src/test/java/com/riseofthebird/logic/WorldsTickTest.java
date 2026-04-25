package com.riseofthebird.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.riseofthebird.data.Bird;
import com.riseofthebird.data.BirdState;
import com.riseofthebird.data.Bulk;
import com.riseofthebird.data.ControllerState;
import com.riseofthebird.data.Form;
import com.riseofthebird.data.InputSnapshot;
import com.riseofthebird.data.Lightning;
import com.riseofthebird.data.Mouse;
import com.riseofthebird.data.Phase;
import com.riseofthebird.data.Thord;
import com.riseofthebird.data.Vec2;
import com.riseofthebird.data.World;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Exercises the central pure {@link Worlds#tick} function end-to-end.
 *
 * <p>These are the highest-value tests in the suite: each one drives a
 * {@link World} through one or more ticks with a synthetic
 * {@link InputSnapshot} and asserts on the post-tick {@link World}. No
 * AWT, no canvas, no threads, no asset files - the whole thing is a
 * pure value transition.
 */
final class WorldsTickTest {

    /** Convenient power-frame count for tests that exercise the AIMING_POWER phase. */
    private static final int POWER_FRAMES = 11;

    // ---- Input edge fixtures ------------------------------------------

    /** All keys released. */
    private static final InputSnapshot IDLE = InputSnapshot.EMPTY;

    /** SPACE rising edge: was up, now down. */
    private static final InputSnapshot SPACE_PRESSED = new InputSnapshot(true, false, false, false);

    /** SPACE held: was down, still down. */
    private static final InputSnapshot SPACE_HELD = new InputSnapshot(true, true, false, false);

    /** SPACE falling edge: was down, now up. */
    private static final InputSnapshot SPACE_RELEASED = new InputSnapshot(false, true, false, false);

    /** ENTER rising edge: was up, now down. */
    private static final InputSnapshot ENTER_PRESSED = new InputSnapshot(false, false, true, false);

    // ---- Helpers ------------------------------------------------------

    private static World freshThordRun() {
        return World.freshRun(List.of(Thord.atSpawn()));
    }

    private static World freshTwoBirdRun() {
        return World.freshRun(List.of(Thord.atSpawn(), Bulk.atSpawn()));
    }

    /** Replaces the world's mouse list with a single mouse positioned exactly on the bird. */
    private static World withMouseOnBird(World w) {
        Bird bird = w.bird();
        if (bird == null) {
            return w;
        }
        Vec2 birdPos = bird.state().pos();
        Mouse onBird = new Mouse(
            birdPos, // spawn
            birdPos, // pos - perfect overlap
            Mouse.MAX_HP,
            Mouse.DEFAULT_SIZE,
            Mouse.INITIAL_STEP,
            0,
            false
        );
        return w.withMice(List.of(onBird));
    }

    // ===================================================================
    // Phase transitions
    // ===================================================================

    @Test
    void ready_idleTick_staysReady() {
        World w = freshThordRun();
        World next = Worlds.tick(w, IDLE, POWER_FRAMES);
        assertEquals(Phase.READY, next.phase());
    }

    @Test
    void ready_spacePressed_transitionsToAimingAngle() {
        World w = freshThordRun();
        World next = Worlds.tick(w, SPACE_PRESSED, POWER_FRAMES);
        assertEquals(Phase.AIMING_ANGLE, next.phase());
    }

    @Test
    void ready_spacePressedWithEmptyRoster_staysReady() {
        // currentBird == roster.size() means the run is exhausted; SPACE
        // should not advance into aiming because there is nothing to aim.
        World w = World.freshRun(List.of(Thord.atSpawn()));
        World exhausted = w.withCurrentBird(1);
        World next = Worlds.tick(exhausted, SPACE_PRESSED, POWER_FRAMES);
        assertEquals(Phase.READY, next.phase());
    }

    @Test
    void aimingAngle_spaceHeld_oscillatesAngle() {
        World w = freshThordRun().withPhase(Phase.AIMING_ANGLE);
        World next = Worlds.tick(w, SPACE_HELD, POWER_FRAMES);
        // From angle=0, direction=+1, ANGLE_STEP=3 => +3 degrees.
        assertEquals(3.0, next.controller().angle(), 1e-9);
        // The bird's launch angle is mirrored on the controller value.
        assertEquals(3.0, next.bird().state().angle(), 1e-9);
        assertEquals(Phase.AIMING_ANGLE, next.phase());
    }

    @Test
    void aimingAngle_spaceReleased_transitionsToAimingPower() {
        World w = freshThordRun().withPhase(Phase.AIMING_ANGLE);
        World next = Worlds.tick(w, SPACE_RELEASED, POWER_FRAMES);
        assertEquals(Phase.AIMING_POWER, next.phase());
    }

    @Test
    void aimingPower_spaceHeld_oscillatesPower() {
        World w = freshThordRun().withPhase(Phase.AIMING_POWER);
        World next = Worlds.tick(w, SPACE_HELD, POWER_FRAMES);
        // From frame=0, direction=+1 => frame becomes 1.
        assertEquals(1, next.controller().powerFrame());
        // Velocity = frame * VELOCITY_MULTIPLIER.
        assertEquals(ControllerState.VELOCITY_MULTIPLIER, next.controller().velocity(), 1e-9);
    }

    @Test
    void aimingPower_spaceReleased_transitionsToFlying_andLocksVelocity() {
        // Pre-charge the controller so the launch carries a non-zero velocity.
        World base = freshThordRun().withPhase(Phase.AIMING_POWER);
        ControllerState charged = base
            .controller()
            .withPowerFrame(5)
            .withVelocity(5 * ControllerState.VELOCITY_MULTIPLIER);
        World w = base.withController(charged);

        World next = Worlds.tick(w, SPACE_RELEASED, POWER_FRAMES);

        assertEquals(Phase.FLYING, next.phase());
        assertEquals(5.0 * ControllerState.VELOCITY_MULTIPLIER, next.bird().state().velocity(), 1e-9);
    }

    @Test
    void flying_spacePressed_latchesSkill() {
        World w = freshThordRun().withPhase(Phase.FLYING);
        // Give the bird a non-zero launch state so it actually moves and
        // we can latch the skill without immediately ending the round.
        Bird launched = Birds.replaceState(w.bird(), w.bird().state().withVelocity(50).withAngle(45));
        World ready = w.withCurrentBirdReplaced(launched);

        World next = Worlds.tick(ready, SPACE_PRESSED, POWER_FRAMES);

        assertTrue(next.controller().skillActivated());
    }

    // ===================================================================
    // FLYING - bird advance
    // ===================================================================

    @Test
    void flying_advancesBirdPosition() {
        // Construct a bird with a known launch velocity / angle and a
        // non-zero trajectory clock so we can observe a position change
        // after one tick of physics. (At time == 0 the parabola equation
        // pos = vx*time + spawn yields the spawn point exactly, so the
        // first tick after launch reports no displacement; gameplay-wise
        // this matches the original game.)
        World base = freshThordRun().withPhase(Phase.FLYING);
        Bird launched = Birds.replaceState(
            base.bird(),
            base.bird().state().withVelocity(100).withAngle(45).withTime(1)
        );
        World w = base.withCurrentBirdReplaced(launched);

        Vec2 before = w.bird().state().pos();
        World next = Worlds.tick(w, IDLE, POWER_FRAMES);
        Vec2 after = next.bird().state().pos();

        // Whether the round ended depends on parameters; either way, the
        // bird's position must have advanced from where it was last tick.
        assertNotEquals(before, after);
    }

    @Test
    void flying_overreached_endsRoundAndAdvancesBird() {
        // Place the bird's spawn anchor far past the right edge of the world;
        // since pos = vx*time + spawn, one tick of FLYING physics will then
        // place the bird past the overreach threshold and end the round.
        // (Setting only pos without spawn would not survive the advance call,
        // which recomputes pos from spawn each tick.)
        World base = freshTwoBirdRun().withPhase(Phase.FLYING);
        Vec2 farRight = new Vec2(Worlds.WORLD_WIDTH * 2.0, 0);
        BirdState s = base.bird().state();
        BirdState past = new BirdState(
            farRight,
            farRight,
            s.angle(),
            s.velocity(),
            s.gravity(),
            s.time(),
            s.form(),
            s.skillActivated()
        );
        Bird overreached = Birds.replaceState(base.bird(), past);
        World w = base.withCurrentBirdReplaced(overreached);

        World next = Worlds.tick(w, IDLE, POWER_FRAMES);

        // Round ended: controller is reset, currentBird advanced, phase reset
        // to READY (because there is still a second bird in the roster).
        assertEquals(1, next.currentBird());
        assertEquals(Phase.READY, next.phase());
        assertEquals(0.0, next.controller().angle(), 1e-9);
    }

    @Test
    void flying_overreached_lastBird_transitionsToGameOver() {
        // Single-bird roster: overreach must transition to GAME_OVER, not READY.
        // We anchor the bird's spawn past the world edge so the next advance
        // tick places its pos past the overreach threshold.
        World base = freshThordRun().withPhase(Phase.FLYING);
        Vec2 farRight = new Vec2(Worlds.WORLD_WIDTH * 2.0, 0);
        BirdState s = base.bird().state();
        BirdState past = new BirdState(
            farRight,
            farRight,
            s.angle(),
            s.velocity(),
            s.gravity(),
            s.time(),
            s.form(),
            s.skillActivated()
        );
        Bird overreached = Birds.replaceState(base.bird(), past);
        World w = base.withCurrentBirdReplaced(overreached);

        World next = Worlds.tick(w, IDLE, POWER_FRAMES);

        assertEquals(Phase.GAME_OVER, next.phase());
        assertFalse(next.playerWon());
    }

    // ===================================================================
    // FLYING - bird-vs-mouse collision and scoring
    // ===================================================================

    @Test
    void flying_birdOverlapsMouse_scoresOneAndDamagesMouse() {
        World base = freshThordRun().withPhase(Phase.FLYING);
        World w = withMouseOnBird(base);

        int hpBefore = w.mice().get(0).hp();
        World next = Worlds.tick(w, IDLE, POWER_FRAMES);

        // Score went up by one.
        assertEquals(1, next.score());
        // Mouse took one point of damage (round may or may not have ended;
        // the mouse state we examine is whatever ended up in the next world).
        if (!next.allMiceDead()) {
            assertEquals(hpBefore - 1, next.mice().get(0).hp());
        }
    }

    @Test
    void flying_threeHits_endsRunAsWon() {
        // Drive the world through three hit ticks; with WIN_THRESHOLD == 3
        // the run should end in GAME_OVER with playerWon = true.
        World w = freshThordRun().withPhase(Phase.FLYING);

        for (int i = 0; i < 3; i++) {
            // Re-seat the mouse on top of the bird before each tick so we
            // guarantee an overlap; clear the per-round latch so the hit lands.
            w = withMouseOnBird(w);
            // Clear justGotHit so the collision check doesn't suppress the hit.
            Mouse fresh = w.mice().get(0).withJustGotHit(false);
            w = w.withMice(List.of(fresh));

            // If the round ended on the previous tick, jump back into FLYING
            // for the next hit so this test can drive the score up.
            if (w.phase() != Phase.FLYING && w.phase() != Phase.GAME_OVER) {
                w = w.withPhase(Phase.FLYING);
            }
            if (w.phase() == Phase.GAME_OVER) {
                break;
            }
            w = Worlds.tick(w, IDLE, POWER_FRAMES);
        }

        assertEquals(Phase.GAME_OVER, w.phase());
        assertTrue(w.playerWon());
        assertTrue(w.score() >= World.WIN_THRESHOLD);
    }

    @Test
    void flying_birdMissesMouse_doesNotScore() {
        // Move the mouse far away from the bird so no collision is possible.
        World base = freshThordRun().withPhase(Phase.FLYING);
        Vec2 farAway = new Vec2(10_000, 10_000);
        Mouse offscreen = new Mouse(farAway, farAway, Mouse.MAX_HP, Mouse.DEFAULT_SIZE, Mouse.INITIAL_STEP, 0, false);
        World w = base.withMice(List.of(offscreen));

        World next = Worlds.tick(w, IDLE, POWER_FRAMES);

        assertEquals(0, next.score());
        assertEquals(Mouse.MAX_HP, next.mice().get(0).hp());
    }

    @Test
    void flying_justGotHitLatch_suppressesRepeatHits() {
        // A mouse with the latch set should not register a hit even on overlap.
        World base = freshThordRun().withPhase(Phase.FLYING);
        World seeded = withMouseOnBird(base);
        Mouse latched = seeded.mice().get(0).withJustGotHit(true);
        World w = seeded.withMice(List.of(latched));

        World next = Worlds.tick(w, IDLE, POWER_FRAMES);

        assertEquals(0, next.score());
        assertEquals(Mouse.MAX_HP, next.mice().get(0).hp());
    }

    @Test
    void endRound_clearsJustGotHitLatch() {
        // Trigger a round end via overreach with a latched mouse, then assert
        // the mouse's latch is cleared in the new world. The bird's spawn
        // anchor is placed past the world edge so the next advance tick
        // pushes it past the overreach threshold (see overreach test above
        // for why setting only pos isn't sufficient).
        World base = freshTwoBirdRun().withPhase(Phase.FLYING);
        Vec2 farRight = new Vec2(Worlds.WORLD_WIDTH * 2.0, 0);
        BirdState s = base.bird().state();
        BirdState past = new BirdState(
            farRight,
            farRight,
            s.angle(),
            s.velocity(),
            s.gravity(),
            s.time(),
            s.form(),
            s.skillActivated()
        );
        Bird overreached = Birds.replaceState(base.bird(), past);
        Mouse latched = base.mice().get(0).withJustGotHit(true);
        World w = base.withCurrentBirdReplaced(overreached).withMice(List.of(latched));

        World next = Worlds.tick(w, IDLE, POWER_FRAMES);

        assertEquals(Phase.READY, next.phase());
        assertFalse(next.mice().get(0).justGotHit());
    }

    // ===================================================================
    // Skill: Bulk
    // ===================================================================

    @Test
    void flying_bulkSkill_growsHitboxAndGravity() {
        World w = World.freshRun(List.of(Bulk.atSpawn())).withPhase(Phase.FLYING);

        // Position Bulk so he doesn't immediately overreach, with some velocity.
        Bulk launched = (Bulk) w.bird();
        BirdState charged = launched.state().withVelocity(50).withAngle(45);
        Bulk ready = (Bulk) launched.withState(charged);
        w = w.withCurrentBirdReplaced(ready);

        int sizeBefore = ((Bulk) w.bird()).size();
        double gravityBefore = w.bird().state().gravity();

        // First tick: latch the skill (rising-edge SPACE).
        World afterPress = Worlds.tick(w, SPACE_PRESSED, POWER_FRAMES);
        assertTrue(afterPress.controller().skillActivated());

        // After one skill tick, Bulk's size and gravity should have grown
        // (assuming the round didn't end on this tick).
        if (afterPress.phase() == Phase.FLYING) {
            Bulk grown = (Bulk) afterPress.bird();
            assertEquals(sizeBefore + Bulk.SIZE_GROWTH_PER_TICK, grown.size());
            assertEquals(gravityBefore + Bulk.GRAVITY_GROWTH_PER_TICK, grown.state().gravity(), 1e-9);
            assertEquals(Form.USING_SKILL, grown.state().form());
        }
    }

    // ===================================================================
    // Skill: Thord (lightning bolt)
    // ===================================================================

    @Test
    void flying_thordSkill_spawnsLightningOnFirstActivation() {
        World base = freshThordRun().withPhase(Phase.FLYING);
        Thord launched = (Thord) base.bird();
        Thord charged = (Thord) launched.withState(launched.state().withVelocity(50).withAngle(45));
        World w = base.withCurrentBirdReplaced(charged);

        // Sanity: bolt is dormant before activation.
        assertSame(Lightning.DORMANT, ((Thord) w.bird()).bolt());

        World afterPress = Worlds.tick(w, SPACE_PRESSED, POWER_FRAMES);

        if (afterPress.phase() == Phase.FLYING) {
            Thord post = (Thord) afterPress.bird();
            assertTrue(post.bolt().spawned(), "bolt should be spawned after first skill tick");
            assertNotSame(Lightning.DORMANT, post.bolt());
            // Velocity boost should be applied exactly once.
            assertEquals(50 + Thord.VELOCITY_BOOST, post.state().velocity(), 1e-9);
        }
    }

    @Test
    void flying_thordLightning_hitsMouseAndScores() {
        World base = freshThordRun().withPhase(Phase.FLYING);
        Thord launched = (Thord) base.bird();
        Vec2 birdPos = launched.state().pos();

        // Pre-spawn the bolt directly on top of a mouse so the very next
        // tick scores a lightning hit. Mouse is positioned a hair past the
        // bolt so the bird-vs-mouse check does not steal the hit first.
        Vec2 mousePos = birdPos.add(105, 0);
        Mouse onBolt = new Mouse(mousePos, mousePos, Mouse.MAX_HP, Mouse.DEFAULT_SIZE, 0, 0, false);
        Lightning bolt = Lightning.DORMANT.spawnedAt(birdPos.add(100, 0), 0);
        Thord armed = new Thord(launched.state().withSkillActivated(true), bolt);

        World w = base
            .withCurrentBirdReplaced(armed)
            .withMice(List.of(onBolt))
            .withController(base.controller().withSkillActivated(true));

        World next = Worlds.tick(w, IDLE, POWER_FRAMES);

        // Either the bird or the bolt scored - both routes count.
        assertTrue(next.score() >= 1, "expected at least one hit, got " + next.score());
    }

    // ===================================================================
    // GAME_OVER
    // ===================================================================

    @Test
    void gameOver_idle_staysGameOver() {
        World w = freshThordRun().withPhase(Phase.GAME_OVER).withPlayerWon(true);
        World next = Worlds.tick(w, IDLE, POWER_FRAMES);
        assertEquals(Phase.GAME_OVER, next.phase());
        assertTrue(next.playerWon());
    }

    @Test
    void gameOver_enterPressed_startsNewRun() {
        World w = freshTwoBirdRun().withPhase(Phase.GAME_OVER).withPlayerWon(true).withScore(3).withCurrentBird(2);

        World next = Worlds.tick(w, ENTER_PRESSED, POWER_FRAMES);

        // Fresh run: phase reset to READY, score zeroed, currentBird back to 0.
        assertEquals(Phase.READY, next.phase());
        assertEquals(0, next.score());
        assertEquals(0, next.currentBird());
        assertFalse(next.playerWon());
        // Roster is preserved in shape (same number of birds, freshly spawned).
        assertEquals(2, next.roster().size());
        // The fresh roster instances must be at-spawn (no per-round physics).
        for (Bird b : next.roster()) {
            assertEquals(BirdState.INITIAL_GRAVITY, b.state().gravity(), 1e-9);
            assertEquals(0.0, b.state().time(), 1e-9);
            assertEquals(Form.REGULAR, b.state().form());
        }
    }

    // ===================================================================
    // Purity: tick must not mutate the input world
    // ===================================================================

    @Test
    void tick_doesNotMutateInputWorld() {
        World w = freshTwoBirdRun().withPhase(Phase.FLYING);
        Bird launched = Birds.replaceState(w.bird(), w.bird().state().withVelocity(50).withAngle(45));
        World seeded = w.withCurrentBirdReplaced(launched);

        // Snapshot the relevant fields before the tick.
        Phase phaseBefore = seeded.phase();
        int scoreBefore = seeded.score();
        int currentBefore = seeded.currentBird();
        int rosterSize = seeded.roster().size();
        int miceSize = seeded.mice().size();

        World next = Worlds.tick(seeded, IDLE, POWER_FRAMES);

        // Returned world is a new instance.
        assertNotSame(seeded, next);
        assertNotNull(next);

        // Original world is unchanged.
        assertEquals(phaseBefore, seeded.phase());
        assertEquals(scoreBefore, seeded.score());
        assertEquals(currentBefore, seeded.currentBird());
        assertEquals(rosterSize, seeded.roster().size());
        assertEquals(miceSize, seeded.mice().size());
    }

    // ---- JUnit's assertNotEquals isn't statically imported above ------
    // (using a tiny shim avoids adding another import line at the top)

    private static void assertNotEquals(Object expectedNotEqual, Object actual) {
        if (java.util.Objects.equals(expectedNotEqual, actual)) {
            throw new AssertionError("Expected values to differ, but both were: " + actual);
        }
    }
}
