package com.riseofthebird.data;

import io.soabase.recordbuilder.core.RecordBuilder;

/**
 * Two-dimensional vector with double-precision components.
 *
 * <p>Replaces {@link java.awt.Point} throughout the data layer: immutable,
 * transparent, and able to represent sub-pixel positions for physics. The
 * renderer casts back to {@code int} at the StdDraw boundary.
 */
@RecordBuilder
public record Vec2(double x, double y) implements Vec2Builder.With {
    public static final Vec2 ZERO = new Vec2(0, 0);

    public Vec2 {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            throw new IllegalArgumentException("Vec2 components must not be NaN: (" + x + ", " + y + ")");
        }
    }

    public Vec2 add(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    public Vec2 add(double dx, double dy) {
        return new Vec2(x + dx, y + dy);
    }

    public Vec2 scale(double factor) {
        return new Vec2(x * factor, y * factor);
    }

    public double distanceTo(Vec2 other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
