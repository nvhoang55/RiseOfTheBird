package com.riseofthebird.game;

import com.riseofthebird.core.Bird;
import com.riseofthebird.core.Bulk;
import com.riseofthebird.core.Thord;
import java.util.function.Supplier;

/**
 * Playable bird characters. Each constant carries a factory for its
 * concrete {@link Bird} subclass, so adding a character is compiler-enforced.
 */
public enum BirdCharacter {
    THORD(Thord::new),
    BULK(Bulk::new);

    private final Supplier<Bird> factory;

    BirdCharacter(Supplier<Bird> factory) {
        this.factory = factory;
    }

    public Bird create() {
        return factory.get();
    }
}
