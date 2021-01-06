package whiteboard.client;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import whiteboard.client.MyComplexShape;

/* This class represents an Oval. */

public class MyOval extends MyComplexShape {

    public MyOval(double x1, double y1, double w, double h, Color c, double thickness, boolean fill) {
        super(x1,y1,w,h,c,thickness,fill); // Retrieving complex shapes' traits.
    }

    public void Draw(GraphicsContext g) {
		/* Making sure the function receives the lowest coordinates for the starting point,
		   as the width and/or height might be negative.*/
        double x1, y1, x2, y2;
        x1 = Math.min(this.getX1(), this.getX1() + this.getWidth());
        y1 = Math.min(this.getY1(), this.getY1() + this.getHeight());
        x2 = Math.max(this.getX1(), this.getX1() + this.getWidth());
        y2 = Math.max(this.getY1(), this.getY1() + this.getHeight());
        g.setFill(this.getColor());
        g.setStroke(this.getColor());
        g.setLineWidth(this.getThickness());
        if(this.toFill()) {
            g.fillOval(x1, y1, x2 - x1, y2 - y1);
        }
        else {
            g.strokeOval(x1, y1, x2 - x1, y2 - y1);
        }
    }
}
