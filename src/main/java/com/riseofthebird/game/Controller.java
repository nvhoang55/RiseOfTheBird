package com.riseofthebird.game;

import com.riseofthebird.assets.Assets;
import com.riseofthebird.core.Bird;
import com.riseofthebird.core.Mouse;
import com.riseofthebird.render.Background;
import edu.princeton.cs.introcs.StdDraw;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-round UI state for angle aiming, power charging, and skill activation,
 * plus the rendering of the on-screen indicators that go with each.
 *
 * <p>Tick-driven: the {@link GameLoop} advances the controller once per tick
 * during the appropriate phase. None of the sub-controllers hold static
 * state, so a fresh {@code Controller} is the natural reset.
 */
public final class Controller {

    private final Background background;
    private final Power power = new Power();
    private final Angle angle = new Angle();
    private final Skill skill = new Skill();

    public Controller(Background background) {
        this.background = background;
    }

    public Power power() {
        return power;
    }

    public Angle angle() {
        return angle;
    }

    public Skill skill() {
        return skill;
    }

    public void reset() {
        power.reset();
        angle.reset();
        skill.reset();
    }

    // =====================================================================
    // Power
    // =====================================================================

    public final class Power {

        private static final int BAR_HEIGHT = 70;
        private static final int VELOCITY_MULTIPLIER = 30;

        /** Frame paths a.png ... k.png. Index in array == velocity tier. */
        private final List<String> frames = loadFrames();

        private int currentFrame = 0;
        private double currentVelocity = 0;
        private int direction = 1;

        private static List<String> loadFrames() {
            List<String> out = new ArrayList<>();
            for (char c = 'a'; c <= 'k'; c++) {
                String path = Assets.optional("controller/power/" + c + ".png");
                if (path != null) out.add(path);
            }
            return out;
        }

        public void reset() {
            currentFrame = 0;
            currentVelocity = 0;
            direction = 1;
        }

        public void tick() {
            if (frames.isEmpty()) return;
            // Bounce off either end of the bar.
            if (currentFrame <= 0) direction = 1;
            else if (currentFrame >= frames.size() - 1) direction = -1;
            currentFrame += direction;
            currentVelocity = currentFrame * VELOCITY_MULTIPLIER;
        }

        public void render() {
            if (frames.isEmpty()) return;
            StdDraw.picture(
                0,
                -background.getHeight() / 2.0 + 50,
                frames.get(currentFrame),
                background.getWidth(),
                BAR_HEIGHT
            );
        }

        public void applyTo(Bird bird) {
            bird.setVelocity(currentVelocity);
        }

        public double currentVelocity() {
            return currentVelocity;
        }
    }

    // =====================================================================
    // Angle
    // =====================================================================

    public final class Angle {

        private static final int MAX_ANGLE = 80;
        private static final int ANGLE_STEP = 3;
        private static final int ARROW_WIDTH = 150;
        private static final int ARROW_HEIGHT = 150;
        private static final int ARROW_GAP = 150;

        private final String arrowPath = Assets.optional("controller/angle/arrow.png");

        private double currentAngle = 0;
        private int direction = 1;

        public void reset() {
            currentAngle = 0;
            direction = 1;
        }

        public void tick() {
            if (currentAngle >= MAX_ANGLE || currentAngle < 0) {
                direction *= -1;
            }
            currentAngle += ANGLE_STEP * direction;
        }

        public void applyTo(Bird bird) {
            bird.setCurrentAngle(currentAngle);
        }

        public void render(Bird bird) {
            if (arrowPath == null) return;
            double distance = ARROW_WIDTH / 2.0 + ARROW_GAP;
            double cx = bird.getInitialCoordinate().getX();
            double cy = bird.getInitialCoordinate().getY();
            double arrowX = cx + distance * Math.cos(Math.toRadians(currentAngle));
            double arrowY = cy + distance * Math.sin(Math.toRadians(currentAngle));
            StdDraw.picture(arrowX, arrowY, arrowPath, ARROW_WIDTH, ARROW_HEIGHT, currentAngle);
        }

        public double currentAngle() {
            return currentAngle;
        }
    }

    // =====================================================================
    // Skill
    // =====================================================================

    public final class Skill {

        private boolean activated = false;

        public void reset() {
            activated = false;
        }

        public boolean isActivated() {
            return activated;
        }

        public void activate() {
            activated = true;
        }

        /**
         * Drives the bird's per-tick skill behaviour while active.
         *
         * @return fresh hits scored this tick (e.g. Thord's lightning), or 0
         *         when inactive or the skill deals no direct damage
         */
        public int applyTo(Bird bird, List<Mouse> mice) {
            if (!activated) return 0;
            bird.changeForm();
            return bird.useSkill(mice);
        }
    }
}
