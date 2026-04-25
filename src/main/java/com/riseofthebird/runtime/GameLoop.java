package com.riseofthebird.runtime;

import com.riseofthebird.data.Bird;
import com.riseofthebird.data.Bulk;
import com.riseofthebird.data.InputSnapshot;
import com.riseofthebird.data.Thord;
import com.riseofthebird.data.World;
import com.riseofthebird.input.Input;
import com.riseofthebird.logic.Worlds;
import com.riseofthebird.render.Background;
import com.riseofthebird.render.Renderer;
import edu.princeton.cs.introcs.StdDraw;
import java.util.ArrayList;
import java.util.List;

/**
 * Imperative shell that drives the pure {@link Worlds#tick} function.
 *
 * <p>Holds exactly two pieces of mutable state: the current {@link World}
 * and the previous tick's {@link InputSnapshot} (needed to derive edge
 * events). Everything else - physics, scoring, phase transitions - is
 * pulled out of {@link Worlds#tick}, which is a pure function.
 *
 * <p>Each tick is a tight three-step pipeline:
 * <ol>
 *     <li>Poll the keyboard via {@link Input#poll(InputSnapshot)} to obtain
 *         a fresh immutable snapshot.</li>
 *     <li>Reassign {@code current} to {@code Worlds.tick(current, snapshot,
 *         powerFrameCount)} - the only place world state evolves.</li>
 *     <li>Hand the new world to the {@link Renderer} and flush the back
 *         buffer with {@link StdDraw#show()}.</li>
 * </ol>
 *
 * <p>The fixed tick interval ({@link #TICK_MS}) is the only timing
 * concern this class owns. If a tick takes longer than {@code TICK_MS}
 * we drop the catch-up and reset the schedule rather than spinning.
 */
public final class GameLoop {

    /** Fixed tick interval in ms (~33 fps). */
    public static final int TICK_MS = 30;

    private final List<Bird> initialRoster;
    private final Background background;
    private final int powerFrameCount;

    private World current;
    private InputSnapshot lastInput = InputSnapshot.EMPTY;
    private boolean running = true;

    /**
     * Constructs a loop ready to run with the supplied roster of bird
     * archetypes. Each entry is replaced with a fresh at-spawn instance
     * before the run starts, so the caller does not have to construct
     * birds in any particular state.
     */
    public GameLoop(List<Bird> roster) {
        this.initialRoster = freshenRoster(roster);
        this.background = new Background(Worlds.WORLD_WIDTH, Worlds.WORLD_HEIGHT);
        this.powerFrameCount = Renderer.powerFrameCount();
        this.current = World.freshRun(this.initialRoster);
        StdDraw.enableDoubleBuffering();
    }

    /**
     * Runs the loop on the calling thread until {@link #stop()} is called
     * or the calling thread is interrupted. Blocks for the lifetime of the
     * game window.
     */
    public void run() {
        long nextTick = System.currentTimeMillis();
        while (running) {
            tick();

            nextTick += TICK_MS;
            long sleep = nextTick - System.currentTimeMillis();
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } else {
                // Fell behind; reset the schedule rather than spinning to catch up.
                nextTick = System.currentTimeMillis();
            }
        }
    }

    /** Signals the loop to exit at the start of the next tick. */
    public void stop() {
        running = false;
    }

    /** The current world. Exposed for diagnostics and tests. */
    public World current() {
        return current;
    }

    private void tick() {
        lastInput = Input.poll(lastInput);
        current = Worlds.tick(current, lastInput, powerFrameCount);
        Renderer.render(current, background);
        StdDraw.show();
    }

    /**
     * Returns a fresh at-spawn instance of every bird in {@code roster}.
     * Used both at construction and when the player restarts a run via
     * the GAME_OVER screen, so no per-round physics ever leaks across runs.
     */
    private static List<Bird> freshenRoster(List<Bird> roster) {
        List<Bird> out = new ArrayList<>(roster.size());
        for (Bird b : roster) {
            out.add(switch (b) {
                case Thord t -> Thord.atSpawn();
                case Bulk bulk -> Bulk.atSpawn();
            });
        }
        return out;
    }
}
