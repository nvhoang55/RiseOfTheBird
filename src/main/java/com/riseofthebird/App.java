package com.riseofthebird;

import com.riseofthebird.data.Bird;
import com.riseofthebird.data.Bulk;
import com.riseofthebird.data.Thord;
import com.riseofthebird.runtime.GameLoop;
import java.util.ArrayList;
import java.util.List;

/** Application entry point. */
public final class App {

    private App() {}

    public static void main(String[] args) {
        List<Bird> roster = parseRoster(System.getProperty("roster"));
        if (roster.isEmpty()) {
            roster = defaultRoster();
        }
        new GameLoop(roster).run();
    }

    /** Canonical startup roster: two Thords followed by two Bulks. */
    private static List<Bird> defaultRoster() {
        return List.of(Thord.atSpawn(), Thord.atSpawn(), Bulk.atSpawn(), Bulk.atSpawn());
    }

    /**
     * Parses a comma-separated list of bird names from the {@code -Droster=...}
     * system property into a fresh roster. Unknown names are skipped with a
     * warning to stderr; null or blank input yields an empty list.
     */
    private static List<Bird> parseRoster(String spec) {
        if (spec == null || spec.isBlank()) return List.of();
        List<Bird> out = new ArrayList<>();
        for (String token : spec.split(",")) {
            String name = token.trim().toUpperCase();
            if (name.isEmpty()) continue;
            Bird bird = switch (name) {
                case "THORD" -> Thord.atSpawn();
                case "BULK" -> Bulk.atSpawn();
                default -> null;
            };
            if (bird != null) {
                out.add(bird);
            } else {
                System.err.println("Unknown bird character in roster: " + token);
            }
        }
        return out;
    }
}
