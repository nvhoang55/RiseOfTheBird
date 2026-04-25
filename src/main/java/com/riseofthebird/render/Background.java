package com.riseofthebird.render;

import com.riseofthebird.assets.Assets;
import edu.princeton.cs.introcs.StdDraw;

/**
 * Owns the canvas dimensions and renders the static background image.
 *
 * <p>The canvas coordinate system is centred at the origin: the visible
 * region is {@code [-width/2, width/2] x [-height/2, height/2]}.
 */
public final class Background {

    private static final String BACKGROUND_PATH = Assets.require("background/ragnarok_background.jpg");

    private final int width;
    private final int height;

    public Background(int width, int height) {
        this.width = width;
        this.height = height;

        double displayScale = 0.6;
        StdDraw.setCanvasSize((int) (width * displayScale), (int) (height * displayScale));
        StdDraw.setXscale(-width / 2.0, width / 2.0);
        StdDraw.setYscale(-height / 2.0, height / 2.0);
    }

    /** Re-draws the background, clearing the previous frame's sprites. */
    public void clear() {
        StdDraw.picture(0, 0, BACKGROUND_PATH, width, height);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
