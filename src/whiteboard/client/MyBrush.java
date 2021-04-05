package whiteboard.client;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;

/* This class represents a freely drawn drawing. */

public class MyBrush extends MyDraw {

    private final boolean fill;
    private ArrayList<Double> xPoints = new ArrayList<>();
    private ArrayList<Double> yPoints = new ArrayList<>();

    public MyBrush(double x, double y, Color c, double thickness, boolean fill) {
        super(c, thickness);
        this.fill = fill;
        this.xPoints.add(x);
        this.yPoints.add(y);
    }

    public void addPoint(double x, double y) {
        this.xPoints.add(x);
        this.yPoints.add(y);
    }

    public boolean isFill() {
        return this.fill;
    }

    public ArrayList<Double> getXPoints() {
        return xPoints;
    }

    public ArrayList<Double> getYPoints() {
        return yPoints;
    }

    public void setXPoints(ArrayList<Double> pathX) {
        this.xPoints = pathX;
    }

    public void setYPoints(ArrayList<Double> pathY) {
        this.yPoints = pathY;
    }

    private double[] convertDouble(ArrayList<Double> arr) {
        double[] p = new double[arr.size()];
        for(int i = 0; i < p.length; i++) {
            p[i] = arr.get(i);
        }
        return p;
    }

    @Override
    public void Draw(GraphicsContext g) {
        g.setLineWidth(getThickness());
        g.setStroke(this.getColor());
        g.setFill(this.getColor());
        if (this.fill) {
            g.fillPolygon(convertDouble(xPoints), convertDouble(yPoints), this.xPoints.size());
        }
        else {
            g.strokePolyline(convertDouble(xPoints), convertDouble(yPoints), this.xPoints.size());
        }
    }
}
