/* This class represents a text box you can put on a canvas. */

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class TextBox extends MyDraw {
    private double x, y;
    private Color c;
    private String text;
    public TextBox(double x, double y, Color c, String t) {
        super(c);
        this.x = x;
        this.y = y;
        this.text = t;
    }

    public void Draw(GraphicsContext g) {
        g.strokeText(text, this.x, this.y);
    }
}
