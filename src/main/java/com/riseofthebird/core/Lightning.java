package com.riseofthebird.core;

import com.riseofthebird.assets.Assets;
import edu.princeton.cs.introcs.StdDraw;
import java.awt.Point;
import java.util.List;

/**
 * Lightning bolt thrown by {@link Thord} when his skill is activated.
 *
 * <p>The bolt travels independently of the bird in a straight line at
 * {@link #SPEED} units per tick, in the direction the bird was facing at the
 * moment the skill triggered. On overlap with any live mouse it deals one
 * point of damage and self-destructs (the {@link #striked} flag latches).
 *
 * <p>Physics ({@link #dart(List)}) and rendering ({@link #show()}) are kept
 * separate so the game loop can clear the background between update and
 * render passes without losing the bolt's frame.
 */
public final class Lightning {

    private static final double SPEED = 170;
    private static final double SIZE = 210;

    /** Optional frame; {@code null} when the asset is missing. */
    private static final String FRAME = Assets.optional("bird/thord/lightning/frame_1.png");

    private double initialAngle;
    private Point currentPosition;
    private boolean striked;
    private boolean spawned;

    public Lightning() {
        this.initialAngle = 0;
        this.currentPosition = new Point(0, 0);
        this.striked = false;
        this.spawned = false;
    }

    /**
     * Advances the bolt one tick: checks for collisions against any live
     * mouse in {@code mice}, applies damage if it hits, and steps the bolt
     * forward along its initial heading. Pure physics; drawing happens in
     * {@link #show()}.
     *
     * @return {@code true} if the bolt hit a mouse on this tick.
     */
    public boolean dart(List<Mouse> mice) {
        if (striked) {
            return false;
        }

        boolean hitThisTick = false;
        for (Mouse mouse : mice) {
            if (mouse.getHP() <= 0) continue;
            double reach = SIZE / 3.0 + mouse.getModelSize() / 3.0;
            if (currentPosition.distance(mouse.getCurrentCoordinate()) <= reach) {
                mouse.takeDamage();
                striked = true;
                hitThisTick = true;
                break;
            }
        }

        currentPosition.x += (int) (SPEED * Math.cos(Math.toRadians(initialAngle)));
        currentPosition.y += (int) (SPEED * Math.sin(Math.toRadians(initialAngle)));

        return hitThisTick;
    }

    /** Renders the current frame; no-op when not spawned, already struck, or asset missing. */
    public void show() {
        if (!spawned || striked || FRAME == null) return;
        StdDraw.picture(currentPosition.x, currentPosition.y, FRAME, SIZE, SIZE, initialAngle + 100);
    }

    public void setInitialAngle(double initialAngle) {
        this.initialAngle = initialAngle;
    }

    /** Spawns the bolt slightly in front of Thord at the moment of skill activation. */
    public void setCurrentPosition(Point position) {
        this.currentPosition = new Point(position);
        this.currentPosition.x += 100;
        this.spawned = true;
    }

    public boolean hasStriked() {
        return striked;
    }

    public boolean isSpawned() {
        return spawned;
    }

    /** Resets state so the bolt can be reused on the next round. */
    public void reset() {
        this.striked = false;
        this.spawned = false;
        this.currentPosition = new Point(0, 0);
        this.initialAngle = 0;
    }
}
