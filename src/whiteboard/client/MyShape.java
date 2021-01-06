package whiteboard.client;

import whiteboard.client.MyDraw;

/* This class represents the basic traits of a shape: a starting point and a color. */

public abstract class MyShape extends MyDraw {

    private double x1;
    private double y1;

    public MyShape(double x1, double y1, javafx.scene.paint.Color c, double thickness) {
        super(c, thickness);
        this.x1 = x1;
        this.y1 = y1;
    }

    public double getX1() {
        return this.x1;
    }

    public double getY1() {
        return this.y1;
    }

    public void setX1(double x1) { this.x1 = x1; }

    public void setY1(double y1) { this.y1 = y1; }
}
