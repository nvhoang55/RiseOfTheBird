package com.riseofthebird.game;

import com.riseofthebird.assets.Assets;
import com.riseofthebird.core.Bird;
import com.riseofthebird.input.InputState;
import com.riseofthebird.render.Background;
import edu.princeton.cs.introcs.StdDraw;
import java.awt.Color;
import java.util.List;

/**
 * Main game loop, driven by a single fixed-rate tick.
 *
 * <p>Each tick: {@link InputState#poll()} -&gt; {@link #update()} -&gt;
 * {@link #render()} -&gt; {@link StdDraw#show()}. Phase transitions happen
 * on edge events from {@link InputState}, never on raw key-held state, so
 * a single SPACE tap cleanly advances exactly one phase.
 *
 * <pre>
 *     READY -&gt; AIMING_ANGLE -&gt; AIMING_POWER -&gt; FLYING -&gt; (next bird | GAME_OVER)
 *                                                              |
 *                                                              v
 *                                                          GAME_OVER -&gt; (replay -&gt; READY)
 * </pre>
 */
public final class GameLoop {

    private static final int WORLD_WIDTH = 1900;
    private static final int WORLD_HEIGHT = 1000;
    private static final int WIN_THRESHOLD = 3;

    /** Fixed tick interval in ms (~33 fps). */
    private static final int TICK_MS = 30;

    private static final String WON_PATH = Assets.optional("menu/won.jpg");
    private static final String LOST_PATH = Assets.optional("menu/lost.jpg");
    private static final String REPLAY_PATH = Assets.optional("menu/replay.png");

    private enum Phase {
        READY,
        AIMING_ANGLE,
        AIMING_POWER,
        FLYING,
        GAME_OVER,
    }

    private final BirdCharacter[] roster;
    private final Background background;
    private final InputState input = new InputState();

    private BirdManager birds;
    private MouseManager mice;
    private Controller controller;

    private Phase phase = Phase.READY;
    private int score = 0;
    private boolean playerWon = false;
    private boolean running = true;

    public GameLoop(BirdCharacter[] roster) {
        this.roster = roster.clone();
        this.background = new Background(WORLD_WIDTH, WORLD_HEIGHT);
        StdDraw.enableDoubleBuffering();
        startNewRun();
    }

    private void startNewRun() {
        this.birds = new BirdManager(roster);
        this.mice = MouseManager.defaultRoster();
        this.controller = new Controller(background);
        this.score = 0;
        this.playerWon = false;
        this.phase = Phase.READY;
        this.input.reset();
    }

    /** Runs the loop on the calling thread until {@link #stop()} is called. */
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

    public void stop() {
        running = false;
    }

    private void tick() {
        input.poll();
        update();
        render();
        StdDraw.show();
    }

    // =====================================================================
    // Update
    // =====================================================================

    private void update() {
        switch (phase) {
            case READY -> updateReady();
            case AIMING_ANGLE -> updateAimingAngle();
            case AIMING_POWER -> updateAimingPower();
            case FLYING -> updateFlying();
            case GAME_OVER -> updateGameOver();
        }
    }

    private void updateReady() {
        mice.tick(List.of());
        if (input.wasSpacePressed() && birds.current().isPresent()) {
            phase = Phase.AIMING_ANGLE;
        }
    }

    private void updateAimingAngle() {
        Bird bird = birds.current().orElseThrow();

        if (input.isSpaceDown()) {
            controller.angle().tick();
            controller.angle().applyTo(bird);
        }

        mice.tick(List.of());

        if (input.wasSpaceReleased()) {
            controller.angle().applyTo(bird);
            phase = Phase.AIMING_POWER;
        }
    }

    private void updateAimingPower() {
        Bird bird = birds.current().orElseThrow();

        if (input.isSpaceDown()) {
            controller.power().tick();
        }

        mice.tick(List.of());

        if (input.wasSpaceReleased()) {
            controller.power().applyTo(bird);
            phase = Phase.FLYING;
        }
    }

    private void updateFlying() {
        Bird bird = birds.current().orElseThrow();

        if (input.wasSpacePressed()) {
            controller.skill().activate();
        }

        score += controller.skill().applyTo(bird, mice.all());

        bird.move();

        score += mice.tick(List.of(bird));

        boolean roundEnded = bird.isOverreached(WORLD_WIDTH, WORLD_HEIGHT) || score >= WIN_THRESHOLD || mice.allDead();

        if (roundEnded) {
            endRound();
        }
    }

    private void endRound() {
        controller.reset();
        mice.resetHitLatches();
        input.reset();

        birds.advance();

        if (score >= WIN_THRESHOLD || mice.allDead()) {
            playerWon = true;
            phase = Phase.GAME_OVER;
        } else if (!birds.hasMore()) {
            playerWon = false;
            phase = Phase.GAME_OVER;
        } else {
            phase = Phase.READY;
        }
    }

    private void updateGameOver() {
        if (input.wasEnterPressed()) {
            startNewRun();
        }
    }

    // =====================================================================
    // Render
    // =====================================================================

    private void render() {
        switch (phase) {
            case READY, AIMING_ANGLE, AIMING_POWER, FLYING -> renderWorld();
            case GAME_OVER -> renderGameOver();
        }
    }

    private void renderWorld() {
        background.clear();

        for (var mouse : mice.all()) {
            mouse.show();
        }

        birds.current().ifPresent(this::drawBird);

        switch (phase) {
            case AIMING_ANGLE -> birds.current().ifPresent(controller.angle()::render);
            case AIMING_POWER -> controller.power().render();
            default -> {
            }
        }

        mice.renderHud(WORLD_WIDTH, WORLD_HEIGHT);
    }

    private void drawBird(Bird bird) {
        bird.show();
        if (phase == Phase.FLYING && controller.skill().isActivated()) {
            bird.drawSkillEffect();
        }
    }

    private void renderGameOver() {
        StdDraw.clear(Color.BLACK);
        String banner = playerWon ? WON_PATH : LOST_PATH;
        if (banner != null) {
            int w = playerWon ? WORLD_WIDTH / 2 : WORLD_WIDTH;
            StdDraw.picture(0, 0, banner, w, WORLD_HEIGHT);
        }
        if (REPLAY_PATH != null) {
            StdDraw.picture(0, -WORLD_HEIGHT / 2.0 + 80, REPLAY_PATH, 600, 100);
        }
    }
}
