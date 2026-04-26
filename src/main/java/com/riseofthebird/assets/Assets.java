package com.riseofthebird.assets;

import java.net.URL;

/**
 * Resolves asset names to classpath-relative paths that
 * {@link edu.princeton.cs.introcs.StdDraw} can load via its built-in
 * {@code getResource} fallback.
 */
public final class Assets {

    private static final String ROOT = "/assets/";

    private Assets() {}

    /** Returns the classpath path for the asset, or throws if it is missing. */
    public static String require(String relativePath) {
        String path = ROOT + relativePath;
        URL url = Assets.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("Missing required asset on classpath: " + path);
        }
        return path;
    }

    /** Returns the classpath path for the asset, or {@code null} if it is missing. */
    public static String optional(String relativePath) {
        String path = ROOT + relativePath;
        URL url = Assets.class.getResource(path);
        return (url == null) ? null : path;
    }
}
