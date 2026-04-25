package com.riseofthebird;

import com.riseofthebird.game.BirdCharacter;
import com.riseofthebird.game.GameLoop;

/** Application entry point. */
public final class App {

    private static final BirdCharacter[] DEFAULT_ROSTER = {
        BirdCharacter.THORD,
        BirdCharacter.THORD,
        BirdCharacter.BULK,
        BirdCharacter.BULK,
    };

    private App() {}

    public static void main(String[] args) {
        BirdCharacter[] roster = parseRoster(System.getProperty("roster"));
        if (roster.length == 0) {
            roster = DEFAULT_ROSTER;
        }

        GameLoop loop = new GameLoop(roster);
        loop.run();
    }

    /**
     * Parses a comma-separated list of {@link BirdCharacter} names from the
     * {@code -Droster=...} system property. Unknown names are skipped with a
     * warning to stderr; null/blank input yields an empty array.
     */
    private static BirdCharacter[] parseRoster(String spec) {
        if (spec == null || spec.isBlank()) {
            return new BirdCharacter[0];
        }
        String[] tokens = spec.split(",");
        BirdCharacter[] parsed = new BirdCharacter[tokens.length];
        int n = 0;
        for (String token : tokens) {
            String name = token.trim().toUpperCase();
            if (name.isEmpty()) continue;
            try {
                parsed[n++] = BirdCharacter.valueOf(name);
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown bird character in roster: " + token);
            }
        }
        BirdCharacter[] trimmed = new BirdCharacter[n];
        System.arraycopy(parsed, 0, trimmed, 0, n);
        return trimmed;
    }
}
