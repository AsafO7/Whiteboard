import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/* This class represents a single Line. */

public class MyLine extends MyShape{
    private double x2, y2;

    public MyLine(double x1, double y1, double x2, double y2, Color c) {
        super(x1,y1,c); // Retrieving shapes' basic traits.
        this.x2 = x2;
        this.y2 = y2;
    }

    public void setX2(double x) {
        this.x2 = x;
    }

    public void setY2(double y) {
        this.y2 = y;
    }

    public double getX2() { return x2; }

    public double getY2() { return y2; }

    public void Draw(GraphicsContext g) {
        g.setStroke(this.getColor());
        g.strokeLine(this.getX1(), this.getY1(), x2, y2);
    }
}
