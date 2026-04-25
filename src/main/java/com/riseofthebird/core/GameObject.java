package com.riseofthebird.core;

import java.awt.Point;

/**
 * Base class for anything that lives in the world and is drawn on the canvas.
 *
 * <p>Subclasses implement {@link #move()} to advance per-tick physics and
 * {@link #show()} to render the current frame. Frame-array semantics are
 * subclass-specific - see {@link com.riseofthebird.assets.Sprites}.
 */
public abstract class GameObject {

    private Point initialCoordinate;
    private Point currentCoordinate;
    private final String[] modelPath;
    private int modelSize;

    protected GameObject(int x, int y, String[] modelPath, int modelSize) {
        this(new Point(x, y), modelPath, modelSize);
    }

    protected GameObject(Point initialCoordinate, String[] modelPath, int modelSize) {
        this.initialCoordinate = new Point(initialCoordinate);
        this.currentCoordinate = new Point(initialCoordinate);
        this.modelPath = modelPath;
        this.modelSize = modelSize;
    }

    public abstract void move();

    public abstract void show();

    public Point getInitialCoordinate() {
        return initialCoordinate;
    }

    public void setInitialCoordinate(Point initialCoordinate) {
        this.initialCoordinate = new Point(initialCoordinate);
    }

    public Point getCurrentCoordinate() {
        return currentCoordinate;
    }

    public void setCurrentCoordinate(int x, int y) {
        this.currentCoordinate = new Point(x, y);
    }

    public String[] getModelPath() {
        return modelPath;
    }

    public int getModelSize() {
        return modelSize;
    }

    public void setModelSize(int modelSize) {
        this.modelSize = modelSize;
    }
}
