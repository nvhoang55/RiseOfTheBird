package com.riseofthebird.data;

/**
 * A playable bird projectile, modelled as a sum type over its concrete kinds.
 *
 * <p>Every bird carries a {@link BirdState} component (its in-flight physics
 * and visual state); kind-specific extras live on the implementing record
 * (e.g. {@link Thord} carries a {@link Lightning} bolt; {@link Bulk} does
 * not). This is the canonical "make illegal states unrepresentable" win:
 * the type system structurally forbids a {@code Bulk} from owning a
 * lightning bolt, so the loop never has to check.
 *
 * <p>Operations on birds are not virtual methods on this interface; they
 * live in {@link com.riseofthebird.logic.Birds} and dispatch via exhaustive
 * {@code switch} expressions over the sealed hierarchy. Adding a new bird
 * kind therefore requires touching every such switch - the compiler will
 * refuse to forget one.
 */
public sealed interface Bird permits Thord, Bulk {

    /** Shared physics and visual state every bird carries. */
    BirdState state();

    /** Returns this bird with its {@link BirdState} replaced. */
    Bird withState(BirdState newState);
}
