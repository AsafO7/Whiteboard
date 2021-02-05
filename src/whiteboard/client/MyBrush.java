package whiteboard.client;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;

public class MyBrush extends MyDraw {

    //private boolean hasPathBegun = false;
    private final boolean fill;
    private final ArrayList<Double> xPoints = new ArrayList<>();
    private final ArrayList<Double> yPoints = new ArrayList<>();

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

    public ArrayList<Double> getXPoints() {
        return xPoints;
    }

    public ArrayList<Double> getYPoints() {
        return yPoints;
    }

    public boolean getFill() { return this.fill; }

//    @Override
//    public void Draw(GraphicsContext g) {
//        g.setStroke(this.getColor());
//        if(!hasPathBegun) {
//            g.beginPath();
//            g.lineTo(this.getX1(), this.getY1());
//            hasPathBegun = true;
//        }
//        g.lineTo(this.getX2(), this.getY2());
//        g.stroke();
//        g.stroke
//    }

    //TODO: potentially install a 3rd party library for efficiency.
    private double[] convertDouble(ArrayList<Double> arr) {
        double[] p = new double[arr.size()];
        for(int i = 0; i < p.length; i++) {
            p[i] = arr.get(i);
        }
        return p;
    }

    @Override
    public void Draw(GraphicsContext g) {
        /* you can loop but it's not efficient */
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
