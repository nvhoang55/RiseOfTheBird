package com.riseofthebird.game;

import com.riseofthebird.core.Bird;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ordered queue of birds for the current run. Exposes the "current bird" the
 * {@link GameLoop} interacts with and advances through the roster as each
 * round ends.
 */
public final class BirdManager {

    private final List<Bird> birds = new ArrayList<>();
    private int currentIndex = 0;

    public BirdManager(BirdCharacter[] roster) {
        for (BirdCharacter character : roster) {
            birds.add(character.create());
        }
    }

    /** The bird currently being launched, or empty once the roster is exhausted. */
    public Optional<Bird> current() {
        if (currentIndex >= birds.size()) return Optional.empty();
        return Optional.of(birds.get(currentIndex));
    }

    public boolean hasMore() {
        return currentIndex < birds.size();
    }

    /** Advances to the next bird. Safe to call after the last bird. */
    public void advance() {
        if (currentIndex < birds.size()) {
            currentIndex++;
        }
    }

    /** Read-only view of every bird in this run, in launch order. */
    public List<Bird> all() {
        return List.copyOf(birds);
    }

    public int currentIndex() {
        return currentIndex;
    }

    public int size() {
        return birds.size();
    }
}
