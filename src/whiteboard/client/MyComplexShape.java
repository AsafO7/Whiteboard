package whiteboard.client;

import javafx.scene.paint.Color;

/* This class represents complex shapes(Ovals, Rectangles and Rounded Rectangles).
 	Along with the basic traits of every shape, complex shapes are also represented by:
 	Width, Height and whether it's filled or not. */

public abstract class MyComplexShape extends MyShape {
    private double width, height;
    private final boolean isFull;

    public MyComplexShape(double x1, double y1, double w, double h, Color c, boolean fill) {
        super(x1,y1,c); // Retrieving basic traits.
        this.width = w;
        this.height = h;
        this.isFull = fill;
    }

    public double getWidth() {
        return this.width;
    }

    public double getHeight() {
        return this.height;
    }

    public void setWidth(double w) {
        this.width = w;
    }

    public void setHeight(double h) {
        this.height = h;
    }

    public boolean toFill() { return this.isFull; }
}
