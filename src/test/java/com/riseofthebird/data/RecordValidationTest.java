package com.riseofthebird.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Validates that the data layer's compact constructors reject illegal states
 * at the boundary, per Goetz/Parlog's "make illegal states unrepresentable"
 * principle.
 */
final class RecordValidationTest {

    // ---- Vec2 ----------------------------------------------------------

    @Test
    void vec2_rejectsNaNX() {
        assertThrows(IllegalArgumentException.class, () -> new Vec2(Double.NaN, 0));
    }

    @Test
    void vec2_rejectsNaNY() {
        assertThrows(IllegalArgumentException.class, () -> new Vec2(0, Double.NaN));
    }

    @Test
    void vec2_acceptsInfinity() {
        // Only NaN is rejected; +/- infinity is valid (e.g. degenerate trajectories).
        new Vec2(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    @Test
    void vec2_addReturnsNewInstance() {
        Vec2 a = new Vec2(1, 2);
        Vec2 b = a.add(3, 4);
        assertEquals(new Vec2(4, 6), b);
        // Original is unchanged - records are immutable.
        assertEquals(new Vec2(1, 2), a);
    }

    @Test
    void vec2_distanceTo_isSymmetric() {
        Vec2 a = new Vec2(0, 0);
        Vec2 b = new Vec2(3, 4);
        assertEquals(5.0, a.distanceTo(b));
        assertEquals(5.0, b.distanceTo(a));
    }

    // ---- Mouse ---------------------------------------------------------

    @Test
    void mouse_rejectsNegativeHp() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Mouse(Vec2.ZERO, Vec2.ZERO, -1, 100, 40, 0, false)
        );
    }

    @Test
    void mouse_rejectsHpAboveMax() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Mouse(Vec2.ZERO, Vec2.ZERO, Mouse.MAX_HP + 1, 100, 40, 0, false)
        );
    }

    @Test
    void mouse_rejectsNegativeSize() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Mouse(Vec2.ZERO, Vec2.ZERO, 1, -1, 40, 0, false)
        );
    }

    @Test
    void mouse_acceptsZeroHp() {
        // hp == 0 is valid (mouse is dead but not yet removed from the world).
        Mouse dead = new Mouse(Vec2.ZERO, Vec2.ZERO, 0, 100, 40, 0, false);
        assertEquals(0, dead.hp());
        assertTrue(!dead.isAlive());
    }

    @Test
    void mouse_defaultBoss_hasFullHp() {
        Mouse m = Mouse.defaultBoss();
        assertEquals(Mouse.MAX_HP, m.hp());
        assertEquals(Mouse.DEFAULT_SIZE, m.size());
        assertTrue(m.isAlive());
    }

    // ---- World ---------------------------------------------------------

    @Test
    void world_rejectsNullRoster() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new World(null, 0, List.of(Mouse.defaultBoss()),
                ControllerState.freshRound(), Phase.READY, 0, false)
        );
    }

    @Test
    void world_rejectsNullMice() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new World(List.of(Thord.atSpawn()), 0, null,
                ControllerState.freshRound(), Phase.READY, 0, false)
        );
    }

    @Test
    void world_rejectsCurrentBirdOutOfRange() {
        List<Bird> roster = List.of(Thord.atSpawn());
        assertThrows(
            IllegalArgumentException.class,
            () -> new World(roster, -1, List.of(Mouse.defaultBoss()),
                ControllerState.freshRound(), Phase.READY, 0, false)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new World(roster, 2, List.of(Mouse.defaultBoss()),
                ControllerState.freshRound(), Phase.READY, 0, false)
        );
    }

    @Test
    void world_acceptsCurrentBirdEqualToRosterSize() {
        // currentBird == roster.size() is valid: the run is exhausted but
        // the world is still well-formed (e.g. during GAME_OVER).
        List<Bird> roster = List.of(Thord.atSpawn());
        World exhausted = new World(roster, 1, List.of(Mouse.defaultBoss()),
            ControllerState.freshRound(), Phase.GAME_OVER, 0, false);
        assertTrue(!exhausted.hasMoreBirds());
    }

    @Test
    void world_rejectsNegativeScore() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new World(List.of(Thord.atSpawn()), 0, List.of(Mouse.defaultBoss()),
                ControllerState.freshRound(), Phase.READY, -1, false)
        );
    }

    @Test
    void world_defensivelyCopiesRoster() {
        // Mutating the input list after construction must not affect the World.
        List<Bird> input = new ArrayList<>();
        input.add(Thord.atSpawn());
        World w = World.freshRun(input);
        input.clear();
        assertEquals(1, w.roster().size());
    }

    @Test
    void world_defensivelyCopiesMice() {
        List<Mouse> input = new ArrayList<>();
        input.add(Mouse.defaultBoss());
        World w = new World(List.of(Thord.atSpawn()), 0, input,
            ControllerState.freshRound(), Phase.READY, 0, false);
        input.clear();
        assertEquals(1, w.mice().size());
    }

    // ---- Hit -----------------------------------------------------------

    @Test
    void hit_missedSingleton() {
        // Hit.Missed is stateless; the singleton avoids per-tick allocation
        // in the inner collision loop.
        assertSame(Hit.Missed.INSTANCE, Hit.Missed.INSTANCE);
    }

    // ---- Lightning -----------------------------------------------------

    @Test
    void lightning_dormantIsNotSpawned() {
        assertTrue(!Lightning.DORMANT.spawned());
        assertTrue(!Lightning.DORMANT.struck());
    }

    @Test
    void lightning_spawnedAt_setsSpawnedFlag() {
        Lightning bolt = Lightning.DORMANT.spawnedAt(new Vec2(10, 20), 45);
        assertTrue(bolt.spawned());
        assertTrue(!bolt.struck());
        assertEquals(new Vec2(10, 20), bolt.pos());
        assertEquals(45.0, bolt.angle());
    }

    @Test
    void lightning_markStruck_preservesPosAndAngle() {
        Lightning spawned = Lightning.DORMANT.spawnedAt(new Vec2(10, 20), 45);
        Lightning struck = spawned.markStruck();
        assertTrue(struck.struck());
        assertTrue(struck.spawned());
        assertEquals(new Vec2(10, 20), struck.pos());
        assertEquals(45.0, struck.angle());
    }

    // ---- BirdState -----------------------------------------------------

    @Test
    void birdState_atSpawn_initialisesPhysicsToZero() {
        BirdState s = BirdState.atSpawn(new Vec2(-700, -300));
        assertEquals(new Vec2(-700, -300), s.spawn());
        assertEquals(new Vec2(-700, -300), s.pos());
        assertEquals(0.0, s.angle());
        assertEquals(0.0, s.velocity());
        assertEquals(BirdState.INITIAL_GRAVITY, s.gravity());
        assertEquals(0.0, s.time());
        assertEquals(Form.REGULAR, s.form());
        assertTrue(!s.skillActivated());
    }

    @Test
    void birdState_withHelpers_returnNewInstances() {
        BirdState a = BirdState.atSpawn(Vec2.ZERO);
        BirdState b = a.withVelocity(50);
        assertEquals(0.0, a.velocity());
        assertEquals(50.0, b.velocity());
    }

    // ---- ControllerState -----------------------------------------------

    @Test
    void controllerState_freshRound_isZeroed() {
        ControllerState c = ControllerState.freshRound();
        assertEquals(0.0, c.angle());
        assertEquals(0, c.powerFrame());
        assertEquals(0.0, c.velocity());
        assertTrue(!c.skillActivated());
    }

    // ---- InputSnapshot -------------------------------------------------

    @Test
    void inputSnapshot_emptyHasNoEdges() {
        assertTrue(!InputSnapshot.EMPTY.wasSpacePressed());
        assertTrue(!InputSnapshot.EMPTY.wasSpaceReleased());
        assertTrue(!InputSnapshot.EMPTY.wasEnterPressed());
    }

    @Test
    void inputSnapshot_risingEdge() {
        InputSnapshot snap = new InputSnapshot(true, false, false, false);
        assertTrue(snap.wasSpacePressed());
        assertTrue(!snap.wasSpaceReleased());
    }

    @Test
    void inputSnapshot_fallingEdge() {
        InputSnapshot snap = new InputSnapshot(false, true, false, false);
        assertTrue(!snap.wasSpacePressed());
        assertTrue(snap.wasSpaceReleased());
    }

    @Test
    void inputSnapshot_heldHasNoEdge() {
        InputSnapshot snap = new InputSnapshot(true, true, false, false);
        assertTrue(!snap.wasSpacePressed());
        assertTrue(!snap.wasSpaceReleased());
    }
}
