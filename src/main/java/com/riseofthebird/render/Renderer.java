package com.riseofthebird.render;

import com.riseofthebird.assets.Assets;
import com.riseofthebird.assets.Sprites;
import com.riseofthebird.data.Bird;
import com.riseofthebird.data.Bulk;
import com.riseofthebird.data.Form;
import com.riseofthebird.data.Lightning;
import com.riseofthebird.data.Mouse;
import com.riseofthebird.data.Phase;
import com.riseofthebird.data.Thord;
import com.riseofthebird.data.Vec2;
import com.riseofthebird.data.World;
import com.riseofthebird.logic.Worlds;
import edu.princeton.cs.introcs.StdDraw;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads {@link World} and emits {@link StdDraw} calls.
 *
 * <p>This is the only module that imports {@link StdDraw} for gameplay
 * rendering (the runtime imports it once for {@code enableDoubleBuffering}
 * / {@code show}; the input module imports it for {@code isKeyPressed}).
 * Everything painted to the canvas is derived from the supplied
 * {@code World} - no instance state, no hidden caches.
 *
 * <p>Dispatch over the round lifecycle is an exhaustive {@code switch} on
 * {@link Phase}; dispatch over bird kinds is an exhaustive {@code switch}
 * on the sealed {@link Bird} hierarchy. The compiler enforces that adding
 * a new phase or bird kind must be reflected here.
 */
public final class Renderer {

    // ---- Power bar -----------------------------------------------------

    /** Frame paths a.png .. k.png; index in the list == velocity tier. */
    private static final List<String> POWER_FRAMES = loadPowerFrames();

    /** Optional aiming-arrow sprite; {@code null} when the asset is missing. */
    private static final String ARROW_PATH = Assets.optional("controller/angle/arrow.png");

    /** Optional lightning sprite; {@code null} when the asset is missing. */
    private static final String LIGHTNING_FRAME = Assets.optional("bird/thord/lightning/frame_1.png");

    private static final int POWER_BAR_HEIGHT = 70;
    private static final int ARROW_WIDTH = 150;
    private static final int ARROW_HEIGHT = 150;
    private static final int ARROW_GAP = 150;

    // ---- HUD -----------------------------------------------------------

    /** Optional HP-bar heart icon; {@code null} when the asset is missing. */
    private static final String HEART_ICON = Assets.optional("score/human-heart.png");

    private static final int HEART_SIZE = 100;
    private static final int HEART_SPACING = 110;
    private static final int HEART_MARGIN = 100;

    // ---- Game-over screen ---------------------------------------------

    private static final String WON_PATH = Assets.optional("menu/won.jpg");
    private static final String LOST_PATH = Assets.optional("menu/lost.jpg");
    private static final String REPLAY_PATH = Assets.optional("menu/replay.png");

    private static final int REPLAY_PROMPT_WIDTH = 600;
    private static final int REPLAY_PROMPT_HEIGHT = 100;

    private Renderer() {}

    /** Returns the number of available power-bar frames; useful to the runtime. */
    public static int powerFrameCount() {
        return POWER_FRAMES.size();
    }

    /**
     * Paints one frame for the given {@link World}. Caller is responsible
     * for {@code StdDraw.enableDoubleBuffering()} once at startup and
     * {@code StdDraw.show()} after this call to flush.
     */
    public static void render(World world, Background background) {
        switch (world.phase()) {
            case READY, AIMING_ANGLE, AIMING_POWER, FLYING -> renderRound(world, background);
            case GAME_OVER -> renderGameOver(world.playerWon());
        }
    }

    // =====================================================================
    // Round rendering
    // =====================================================================

    private static void renderRound(World world, Background background) {
        background.clear();

        // Mice (any HUD-overlay mice render after the bird).
        for (Mouse mouse : world.mice()) {
            renderMouse(mouse);
        }

        // Bird and any skill-spawned visual effect.
        Bird bird = world.bird();
        if (bird != null) {
            boolean skillActive = world.phase() == Phase.FLYING && world.controller().skillActivated();
            renderBird(bird, skillActive);
        }

        // Phase-specific overlays.
        switch (world.phase()) {
            case AIMING_ANGLE -> {
                if (bird != null) {
                    renderAimingArrow(world.controller().angle(), bird.state().spawn());
                }
            }
            case AIMING_POWER -> renderPowerBar(world.controller().powerFrame());
            default -> {
                // No overlay for READY or FLYING.
            }
        }

        // HUD on top.
        renderHpBar(world.mice());
    }

    private static void renderBird(Bird bird, boolean skillActive) {
        switch (bird) {
            case Thord thord -> {
                drawBirdSprite(
                    thord.state().pos(),
                    bird.state().form(),
                    Sprites.THORD,
                    Thord.SIZE,
                    bird.state().angle()
                );
                if (skillActive) {
                    renderLightning(thord.bolt());
                }
            }
            case Bulk bulk -> drawBirdSprite(
                bulk.state().pos(),
                bulk.state().form(),
                Sprites.BULK,
                bulk.size(),
                bulk.state().angle()
            );
        }
    }

    private static void drawBirdSprite(Vec2 pos, Form form, String[] frames, int size, double angle) {
        if (frames.length == 0) return;
        int idx = Math.min(form.ordinal(), frames.length - 1);
        String frame = frames[idx];
        if (frame == null) return;
        StdDraw.picture(pos.x(), pos.y(), frame, size, size, angle);
    }

    private static void renderLightning(Lightning bolt) {
        if (LIGHTNING_FRAME == null) return;
        if (!bolt.spawned() || bolt.struck()) return;
        StdDraw.picture(
            bolt.pos().x(),
            bolt.pos().y(),
            LIGHTNING_FRAME,
            Lightning.SIZE,
            Lightning.SIZE,
            bolt.angle() + 100
        );
    }

    private static void renderMouse(Mouse mouse) {
        String[] frames = Sprites.MOUSELEFICENT;
        if (frames.length == 0) return;
        int idx = Math.min(Math.max(mouse.hp(), 0), frames.length - 1);
        String frame = frames[idx];
        if (frame == null) return;
        StdDraw.picture(mouse.pos().x(), mouse.pos().y(), frame, mouse.size(), mouse.size());
    }

    private static void renderAimingArrow(double angle, Vec2 spawn) {
        if (ARROW_PATH == null) return;
        double distance = ARROW_WIDTH / 2.0 + ARROW_GAP;
        double angleRad = Math.toRadians(angle);
        double arrowX = spawn.x() + distance * Math.cos(angleRad);
        double arrowY = spawn.y() + distance * Math.sin(angleRad);
        StdDraw.picture(arrowX, arrowY, ARROW_PATH, ARROW_WIDTH, ARROW_HEIGHT, angle);
    }

    private static void renderPowerBar(int frame) {
        if (POWER_FRAMES.isEmpty()) return;
        int idx = Math.min(Math.max(frame, 0), POWER_FRAMES.size() - 1);
        StdDraw.picture(
            0,
            -Worlds.WORLD_HEIGHT / 2.0 + 50,
            POWER_FRAMES.get(idx),
            Worlds.WORLD_WIDTH,
            POWER_BAR_HEIGHT
        );
    }

    private static void renderHpBar(List<Mouse> mice) {
        if (HEART_ICON == null || mice.isEmpty()) return;
        int totalHp = 0;
        for (Mouse mouse : mice) {
            totalHp += Math.max(mouse.hp(), 0);
        }
        int firstX = Worlds.WORLD_WIDTH / 2 - HEART_MARGIN;
        int y = Worlds.WORLD_HEIGHT / 2 - HEART_MARGIN;
        for (int i = 0; i < totalHp; i++) {
            StdDraw.picture(firstX - i * HEART_SPACING, y, HEART_ICON, HEART_SIZE, HEART_SIZE);
        }
    }

    // =====================================================================
    // Game-over screen
    // =====================================================================

    private static void renderGameOver(boolean playerWon) {
        StdDraw.clear(Color.BLACK);
        String banner = playerWon ? WON_PATH : LOST_PATH;
        if (banner != null) {
            int width = playerWon ? Worlds.WORLD_WIDTH / 2 : Worlds.WORLD_WIDTH;
            StdDraw.picture(0, 0, banner, width, Worlds.WORLD_HEIGHT);
        }
        if (REPLAY_PATH != null) {
            StdDraw.picture(0, -Worlds.WORLD_HEIGHT / 2.0 + 80, REPLAY_PATH, REPLAY_PROMPT_WIDTH, REPLAY_PROMPT_HEIGHT);
        }
    }

    // =====================================================================
    // Asset loading
    // =====================================================================

    private static List<String> loadPowerFrames() {
        List<String> out = new ArrayList<>();
        for (char c = 'a'; c <= 'k'; c++) {
            String path = Assets.optional("controller/power/" + c + ".png");
            if (path != null) out.add(path);
        }
        return out;
    }
}
