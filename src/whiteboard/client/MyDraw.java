package whiteboard.client;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.io.Serializable;

public abstract class MyDraw implements Serializable {

    private final Color color;
    private final double thickness;

    public MyDraw(Color c, double thickness) {
        this.color = c;
        this.thickness = thickness;
    }

    /* This function draws a shape depending on the class using it. */
    public abstract void Draw(GraphicsContext g);

    public Color getColor() { return this.color; }

    public double getThickness() { return thickness; }
}
