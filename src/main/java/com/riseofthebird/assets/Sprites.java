package com.riseofthebird.assets;

/**
 * Centralised constants for sprite asset paths.
 *
 * <p>Indexing is gameplay-coupled:
 * <ul>
 *     <li>Bird arrays: {@code [0]} regular form, {@code [1]} using-skill form.</li>
 *     <li>Mouse arrays: index = current HP, so {@code mouse[hp]} renders the
 *         sprite matching the boss's remaining health.</li>
 * </ul>
 */
public final class Sprites {

    private Sprites() {}

    public static final String[] THORD = {
        Assets.require("bird/thord/regular_form.png"),
        Assets.require("bird/thord/using_skill_form.png"),
    };

    /** Optional: PNGs may be absent, in which case Bulk renders as a no-op. */
    public static final String[] BULK = {
        Assets.optional("bird/bulk/regular_form.png"),
        Assets.optional("bird/bulk/using_skill_form.png"),
    };

    /** Indexed by current HP (0..3); index 0 falls back to the 1HP sprite. */
    public static final String[] MOUSELEFICENT = {
        Assets.require("mouse/mouseleficent/mouseleficent_1hp.png"),
        Assets.require("mouse/mouseleficent/mouseleficent_1hp.png"),
        Assets.require("mouse/mouseleficent/mouseleficent_2hp.png"),
        Assets.require("mouse/mouseleficent/mouseleficent_3hp.png"),
    };
}
