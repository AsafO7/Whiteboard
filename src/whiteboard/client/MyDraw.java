package whiteboard.client;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public abstract class MyDraw {

    private final javafx.scene.paint.Color color;

    public MyDraw(Color c) {
        this.color = c;
    }

    /* This function draws a shape depending on the class using it. */
    public abstract void Draw(GraphicsContext g);

    //public void setColor(Color c) { this.color = c; }

    public javafx.scene.paint.Color getColor() { return this.color; }
}
