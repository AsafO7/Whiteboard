import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/* This class represents a Rounded Rectangle. */

public class MyRoundRect extends MyComplexShape{

    private final int arcHeight;
    private final int arcWidth;

    public MyRoundRect(double x1, double y1, double w, double h, Color c, boolean fill, int arcW, int arcH) {
        super(x1,y1,w,h,c,fill); // Retrieving complex shapes' traits.

        /* These two traits will determine how rounded the rectangle will be. */
        this.arcWidth = arcW;
        this.arcHeight = arcH;
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
        if(this.toFill()) {
            g.fillRoundRect(x1, y1, x2 - x1, y2 - y1, this.getArcWidth(), this.getArcHeight());
        }
        else {
            g.strokeRoundRect(x1, y1, x2 - x1, y2 - y1, this.getArcWidth(), this.getArcHeight());
        }
    }

    public int getArcWidth() {
        return this.arcWidth;
    }

    public int getArcHeight() {
        return this.arcHeight;
    }
}
