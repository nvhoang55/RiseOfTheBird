package com.riseofthebird.data;

/**
 * Visual form a bird is currently rendered in.
 *
 * <p>Maps directly to the sprite-array index used by {@link com.riseofthebird.assets.Sprites}:
 * {@link #REGULAR} is frame {@code 0}, {@link #USING_SKILL} is frame {@code 1}.
 */
public enum Form {
    REGULAR,
    USING_SKILL
}
