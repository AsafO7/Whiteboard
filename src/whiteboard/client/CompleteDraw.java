package whiteboard.client;

import javafx.scene.canvas.GraphicsContext;

import java.io.Serializable;
import java.util.ArrayList;

public class CompleteDraw implements Serializable {

    private String color, shape, text;
    private double thickness, x1, y1, x2, y2;
    private boolean fill = false;
    private int arcW, arcH;
    private ArrayList<Double> xPoints, yPoints;
    //private GraphicsContext gc;

    public CompleteDraw(String color, double thickness, double x1, double y1, double x2, double y2, boolean fill, int arcW, int arcH,
                        ArrayList<Double> xPoints, ArrayList<Double> yPoints, /*GraphicsContext gc,*/ String shape, String text) {
        this.color = color;
        this.thickness = thickness;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.fill = fill;
        this.arcW = arcW;
        this.arcH = arcH;
        this.xPoints = xPoints;
        this.yPoints = yPoints;
        //this.gc = gc;
        this.shape = shape;
        this.text = text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public String getShape() {
        return shape;
    }

//    public GraphicsContext getGc() {
//        return gc;
//    }

    public String getColor() {
        return color;
    }

    public double getThickness() {
        return thickness;
    }

    public double getX1() {
        return x1;
    }

    public double getY1() {
        return y1;
    }

    public double getX2() {
        return x2;
    }

    public double getY2() {
        return y2;
    }

    public boolean isFill() {
        return fill;
    }

    public int getArcW() {
        return arcW;
    }

    public int getArcH() {
        return arcH;
    }

    public ArrayList<Double> getXPoints() {
        return xPoints;
    }

    public ArrayList<Double> getYPoints() {
        return yPoints;
    }
}
