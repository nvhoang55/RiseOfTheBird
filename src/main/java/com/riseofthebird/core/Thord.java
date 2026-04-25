package com.riseofthebird.core;

import com.riseofthebird.assets.Sprites;
import java.util.List;

/**
 * Thord: a fast bird whose skill grants a one-time velocity boost and hurls
 * a {@link Lightning} bolt in the direction he is currently flying.
 */
public final class Thord extends Bird {

    private static final int SPAWN_X = -700;
    private static final int SPAWN_Y = -300;
    private static final int INITIAL_SIZE = 200;
    private static final double VELOCITY_BOOST = 50;

    private final Lightning lightning;

    public Thord() {
        super(SPAWN_X, SPAWN_Y, Sprites.THORD, INITIAL_SIZE);
        this.lightning = new Lightning();
    }

    @Override
    public int useSkill(List<Mouse> mice) {
        if (!isSkillActivated()) {
            markSkillActivated();
            setVelocity(getVelocity() + VELOCITY_BOOST);
            lightning.setInitialAngle(getCurrentAngle());
            lightning.setCurrentPosition(getCurrentCoordinate());
        }

        if (lightning.hasStriked()) {
            return 0;
        }
        return lightning.dart(mice) ? 1 : 0;
    }

    @Override
    public void resetForRound() {
        super.resetForRound();
        lightning.reset();
    }

    @Override
    public void drawSkillEffect() {
        lightning.show();
    }

    public Lightning getLightning() {
        return lightning;
    }
}
