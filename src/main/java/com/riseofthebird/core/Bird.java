package com.riseofthebird.core;

import edu.princeton.cs.introcs.StdDraw;
import java.util.List;

/**
 * Base for any bird projectile launched at the boss.
 *
 * <p>Travels along a parabolic trajectory parameterised by an initial
 * velocity, an angle, and a per-bird gravity constant. Has a regular form
 * (frame {@code 0}) and a using-skill form (frame {@code 1}). Subclasses
 * implement {@link #useSkill(List)} for character-specific behaviour.
 */
public abstract class Bird extends GameObject {

    private double gravity;
    private double initialGravity;
    private double time;
    private double velocity;
    private double currentAngle;
    private int formIndex;

    private boolean skillActivated;

    protected Bird(int x, int y, String[] model, int modelSize) {
        super(x, y, model, modelSize);
        this.gravity = 1.5;
        this.initialGravity = this.gravity;
        this.time = 0;
        this.velocity = 0;
        this.currentAngle = 0;
        this.formIndex = 0;
        this.skillActivated = false;
    }

    /**
     * Character-specific skill effect. Called every tick once the skill has
     * been activated by the player.
     *
     * @param mice the live boss list, available to skills that apply their
     *             own collision detection (e.g. independent projectiles)
     * @return how many fresh hits this skill scored on this tick. Birds whose
     *         skills do not damage mice directly should return {@code 0}.
     */
    public abstract int useSkill(List<Mouse> mice);

    /**
     * Advances the parabolic trajectory by one tick. Pure physics: does not
     * touch the canvas; drawing happens in {@link #show()}.
     */
    @Override
    public void move() {
        double vx = Math.cos(Math.toRadians(currentAngle)) * velocity;
        double vy = Math.sin(Math.toRadians(currentAngle)) * velocity - gravity * time;

        double x = vx * time + getInitialCoordinate().getX();
        double y = vy * time - 0.5 * gravity * time * time + getInitialCoordinate().getY();

        setCurrentCoordinate((int) x, (int) y);

        // Track the heading so the sprite rotates with the trajectory.
        if (vx != 0) {
            currentAngle = Math.toDegrees(Math.atan(vy / vx));
        }
        time += 0.5;
    }

    /**
     * Renders any skill-spawned visual effect that lives independently of
     * the bird itself. Default is a no-op; override for projectile-style skills.
     */
    public void drawSkillEffect() {}

    /**
     * Bounces the bird back when it hits a target: flips its angle, reverses
     * velocity, resets the trajectory clock, and slightly increases gravity.
     */
    public void knockBack() {
        currentAngle += 180;
        velocity *= -1;
        setInitialCoordinate(getCurrentCoordinate());
        time = 0;
        gravity += 0.2;
    }

    @Override
    public void show() {
        String frame = currentFrame();
        if (frame == null) return;
        StdDraw.picture(
            getCurrentCoordinate().getX(),
            getCurrentCoordinate().getY(),
            frame,
            getModelSize(),
            getModelSize(),
            currentAngle
        );
    }

    /** True once the bird has flown off the right edge or below the bottom of the world. */
    public boolean isOverreached(int worldWidth, int worldHeight) {
        double x = getCurrentCoordinate().getX();
        double y = getCurrentCoordinate().getY();
        return ((x - getModelSize()) >= worldWidth || (y - getModelSize()) <= -worldHeight);
    }

    /**
     * Resets per-round physics, form, and skill latches so this bird can be
     * launched again on replay without inheriting state from the previous run.
     */
    public void resetForRound() {
        this.time = 0;
        this.velocity = 0;
        this.currentAngle = 0;
        this.formIndex = 0;
        this.gravity = this.initialGravity;
        this.skillActivated = false;
        setCurrentCoordinate((int) getInitialCoordinate().getX(), (int) getInitialCoordinate().getY());
    }

    /** Switches to the using-skill sprite (frame {@code 1}) if one is available. */
    public void changeForm() {
        if (getModelPath().length > 1 && getModelPath()[1] != null) {
            this.formIndex = 1;
        }
    }

    private String currentFrame() {
        String[] frames = getModelPath();
        if (frames.length == 0) return null;
        int idx = Math.min(formIndex, frames.length - 1);
        return frames[idx];
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setCurrentAngle(double angle) {
        this.currentAngle = angle;
    }

    public double getCurrentAngle() {
        return currentAngle;
    }

    public double getGravity() {
        return gravity;
    }

    public void setGravity(double gravity) {
        this.gravity = gravity;
    }

    public boolean isSkillActivated() {
        return skillActivated;
    }

    public void markSkillActivated() {
        this.skillActivated = true;
    }
}
