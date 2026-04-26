package com.riseofthebird.runtime;

import com.riseofthebird.data.Bird;
import com.riseofthebird.data.InputSnapshot;
import com.riseofthebird.data.World;
import com.riseofthebird.input.Input;
import com.riseofthebird.logic.Worlds;
import com.riseofthebird.render.Background;
import com.riseofthebird.render.Renderer;
import edu.princeton.cs.introcs.StdDraw;
import java.util.List;

/**
 * Imperative shell that drives the pure {@link Worlds#tick} function.
 * Holds the only mutable state in the game: the current {@link World}
 * and the previous tick's {@link InputSnapshot}.
 */
public final class GameLoop {

    private static final int TICK_MS = 30;

    private final Background background;
    private final int powerFrameCount;

    private World current;
    private InputSnapshot lastInput = InputSnapshot.EMPTY;

    public GameLoop(List<Bird> roster) {
        this.background = new Background(Worlds.WORLD_WIDTH, Worlds.WORLD_HEIGHT);
        this.powerFrameCount = Renderer.powerFrameCount();
        this.current = World.freshRun(roster);
        StdDraw.enableDoubleBuffering();
    }

    public void run() {
        long nextTick = System.currentTimeMillis();
        while (true) {
            lastInput = Input.poll(lastInput);
            current = Worlds.tick(current, lastInput, powerFrameCount);
            Renderer.render(current, background);
            StdDraw.show();

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
}
