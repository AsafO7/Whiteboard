package whiteboard.client;/* This class represents a text box you can put on a canvas. */

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class TextBox extends MyDraw {
    private final double x, y;
    private final String text;
    public TextBox(double x, double y, Color c, String t) {
        super(c,1);
        this.x = x;
        this.y = y;
        this.text = t;
    }

    public void Draw(GraphicsContext g) {
        g.setLineWidth(getThickness());
        g.setFont(Font.font(20));
        g.setFill(this.getColor());
        g.setStroke(this.getColor());
        g.strokeText(text, this.x, this.y);
    }
}