package com.riseofthebird.core;

import com.riseofthebird.assets.Assets;
import edu.princeton.cs.introcs.StdDraw;

/**
 * Mouseleficent: the boss enemy.
 *
 * <p>Oscillates back and forth around its spawn coordinate within a fixed
 * range, taking up to {@link #MAX_HP} hits before dying. Each hit shrinks
 * the sprite, increases the oscillation step, and advances the sprite-array
 * index so the rendered frame matches the remaining HP.
 *
 * <p>{@link #move()} only updates physics; {@link #renderHpBar(int, int)}
 * and {@link #show()} are called separately by the game loop.
 */
public final class Mouse extends GameObject {

    public static final int MAX_HP = 3;

    private static final int MAX_RANGE = 150;
    private static final int INITIAL_STEP = 40;
    private static final int SIZE_SHRINK_PER_HIT = 50;
    private static final int STEP_GROWTH_PER_HIT = 40;

    /** Optional HP-bar heart icon; {@code null} when the asset is missing. */
    private static final String HEART_ICON = Assets.optional("score/human-heart.png");

    private int step;
    private int movingDistance;
    private int hp;

    /** Latches after a hit so a single bird can't deal damage every tick. */
    private boolean justGotHit;

    public Mouse(int x, int y, String[] model, int modelSize) {
        super(x, y, model, modelSize);
        this.step = INITIAL_STEP;
        this.movingDistance = 0;
        this.hp = MAX_HP;
        this.justGotHit = false;
    }

    /** Reduces HP by one, shrinks the sprite, and speeds up the oscillation. */
    public void takeDamage() {
        if (hp <= 0) return;

        hp--;
        setModelSize(Math.max(0, getModelSize() - SIZE_SHRINK_PER_HIT));
        if (step != 0) {
            int direction = Integer.signum(step);
            step += direction * STEP_GROWTH_PER_HIT;
        }
    }

    /**
     * Returns true if {@code bird} overlaps the mouse this tick and the
     * mouse has not already been counted as hit this round.
     */
    public boolean isHitBy(Bird bird) {
        if (justGotHit) return false;
        double reach = getModelSize() / 3.0 + bird.getModelSize() / 3.0;
        boolean overlap = getCurrentCoordinate().distance(bird.getCurrentCoordinate()) <= reach;
        if (overlap) {
            justGotHit = true;
            return true;
        }
        return false;
    }

    /** Advances oscillation by one tick. Pure physics; drawing happens in {@link #show()}. */
    @Override
    public void move() {
        if (hp > 0) {
            moveBackAndForth();
        }
    }

    private void moveBackAndForth() {
        if (Math.abs(movingDistance) >= MAX_RANGE) {
            step *= -1;
        }
        movingDistance += step;
        setCurrentCoordinate(
            (int) (getInitialCoordinate().getX() + movingDistance),
            (int) getInitialCoordinate().getY()
        );
    }

    @Override
    public void show() {
        String[] frames = getModelPath();
        if (frames.length == 0) return;
        int idx = Math.min(Math.max(hp, 0), frames.length - 1);
        String frame = frames[idx];
        if (frame == null) return;
        StdDraw.picture(
            getCurrentCoordinate().getX(),
            getCurrentCoordinate().getY(),
            frame,
            getModelSize(),
            getModelSize()
        );
    }

    /** Renders the HP bar; no-op when the heart icon asset is missing. */
    public void renderHpBar(int worldWidth, int worldHeight) {
        if (HEART_ICON == null) return;
        int spacing = 110;
        int firstX = worldWidth / 2 - 100;
        int y = worldHeight / 2 - 100;
        for (int i = 0; i < hp; i++) {
            StdDraw.picture(firstX - i * spacing, y, HEART_ICON, 100, 100);
        }
    }

    public int getHP() {
        return hp;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void setJustGotHit(boolean justGotHit) {
        this.justGotHit = justGotHit;
    }
}
