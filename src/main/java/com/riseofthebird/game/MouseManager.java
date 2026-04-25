package com.riseofthebird.game;

import com.riseofthebird.assets.Sprites;
import com.riseofthebird.core.Bird;
import com.riseofthebird.core.Mouse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Owns the live set of {@link Mouse} bosses for the current run, runs their
 * per-tick motion and bird-collision logic, and reports how many points the
 * player scored on this tick.
 */
public final class MouseManager {

    private static final int SPAWN_X = 700;
    private static final int SPAWN_Y = -300;
    private static final int DEFAULT_SIZE = 225;

    private final List<Mouse> mice = new ArrayList<>();

    /** Constructs a manager pre-populated with a single Mouseleficent boss. */
    public static MouseManager defaultRoster() {
        MouseManager manager = new MouseManager();
        manager.addMouse(new Mouse(SPAWN_X, SPAWN_Y, Sprites.MOUSELEFICENT, DEFAULT_SIZE));
        return manager;
    }

    public MouseManager() {}

    public void addMouse(Mouse mouse) {
        mice.add(mouse);
    }

    /** Read-only view of every live mouse. */
    public List<Mouse> all() {
        return List.copyOf(mice);
    }

    /** Clears the per-round "just got hit" latch on every mouse. */
    public void resetHitLatches() {
        for (Mouse mouse : mice) {
            mouse.setJustGotHit(false);
        }
    }

    /**
     * Advances every mouse's motion, checks for collisions against {@code birds},
     * applies damage, and removes mice that have died. Iteration is safe via
     * {@link Iterator#remove()}.
     *
     * @return how many fresh hits were registered this tick
     */
    public int tick(List<Bird> birds) {
        int hitsThisTick = 0;

        Iterator<Mouse> it = mice.iterator();
        while (it.hasNext()) {
            Mouse mouse = it.next();

            if (!mouse.isAlive()) {
                it.remove();
                continue;
            }

            for (Bird bird : birds) {
                if (mouse.isHitBy(bird)) {
                    mouse.takeDamage();
                    bird.knockBack();
                    hitsThisTick++;
                    // One bird can only register one hit per tick.
                    break;
                }
            }

            mouse.move();
        }

        return hitsThisTick;
    }

    /** Renders the boss HP bar for every live mouse. */
    public void renderHud(int worldWidth, int worldHeight) {
        for (Mouse mouse : mice) {
            mouse.renderHpBar(worldWidth, worldHeight);
        }
    }

    public boolean allDead() {
        return mice.isEmpty();
    }
}
