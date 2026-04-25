package com.riseofthebird.core;

import com.riseofthebird.assets.Sprites;
import java.util.List;

/**
 * Bulk: a heavyweight bird whose skill grows its hitbox and increases gravity
 * mid-flight, causing a steep "stomp" descent.
 */
public final class Bulk extends Bird {

    private static final int SPAWN_X = -700;
    private static final int SPAWN_Y = -300;
    private static final int INITIAL_SIZE = 170;

    private static final int SIZE_GROWTH_PER_TICK = 20;
    private static final double GRAVITY_GROWTH_PER_TICK = 0.1;

    public Bulk() {
        super(SPAWN_X, SPAWN_Y, Sprites.BULK, INITIAL_SIZE);
    }

    @Override
    public int useSkill(List<Mouse> mice) {
        setModelSize(getModelSize() + SIZE_GROWTH_PER_TICK);
        setGravity(getGravity() + GRAVITY_GROWTH_PER_TICK);
        markSkillActivated();
        return 0;
    }
}
