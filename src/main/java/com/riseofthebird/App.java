package com.riseofthebird;

import com.riseofthebird.data.Bird;
import com.riseofthebird.data.Bulk;
import com.riseofthebird.data.Thord;
import com.riseofthebird.runtime.GameLoop;
import java.util.ArrayList;
import java.util.List;

/** Application entry point. */
public final class App {

    private static final List<BirdKind> DEFAULT_ROSTER = List.of(
        BirdKind.THORD,
        BirdKind.THORD,
        BirdKind.BULK,
        BirdKind.BULK
    );

    private App() {}

    public static void main(String[] args) {
        List<BirdKind> kinds = parseRoster(System.getProperty("roster"));
        if (kinds.isEmpty()) {
            kinds = DEFAULT_ROSTER;
        }

        List<Bird> roster = new ArrayList<>(kinds.size());
        for (BirdKind kind : kinds) {
            roster.add(kind.spawn());
        }

        new GameLoop(roster).run();
    }

    /**
     * Parses a comma-separated list of {@link BirdKind} names from the
     * {@code -Droster=...} system property. Unknown names are skipped with a
     * warning to stderr; null/blank input yields an empty list.
     */
    private static List<BirdKind> parseRoster(String spec) {
        if (spec == null || spec.isBlank()) {
            return List.of();
        }
        List<BirdKind> out = new ArrayList<>();
        for (String token : spec.split(",")) {
            String name = token.trim().toUpperCase();
            if (name.isEmpty()) continue;
            try {
                out.add(BirdKind.valueOf(name));
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown bird character in roster: " + token);
            }
        }
        return out;
    }

    /**
     * Roster-spec enum: maps a string name from the {@code -Droster=...}
     * argument to a freshly-spawned concrete {@link Bird}. Lives here at the
     * boundary rather than in {@code data/} because it is purely a parser
     * concern - the rest of the game operates on {@link Bird} values.
     */
    private enum BirdKind {
        THORD {
            @Override
            Bird spawn() {
                return Thord.atSpawn();
            }
        },
        BULK {
            @Override
            Bird spawn() {
                return Bulk.atSpawn();
            }
        };

        abstract Bird spawn();
    }
}
