package whiteboard.client;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public abstract class MyDraw {

    private final javafx.scene.paint.Color color;
    private final double thickness;

    public MyDraw(Color c, double thickness) {
        this.color = c;
        this.thickness = thickness;
    }

    /* This function draws a shape depending on the class using it. */
    public abstract void Draw(GraphicsContext g);

    public javafx.scene.paint.Color getColor() { return this.color; }

    public double getThickness() { return thickness; }
}
