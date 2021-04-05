package whiteboard.client;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/* This class represents a text box you can put on a canvas. */

public class TextBox extends MyDraw {
    private final double x, y;
    private final String text;
    public TextBox(double x, double y, Color c, String t) {
        super(c,1);
        this.x = x;
        this.y = y;
        this.text = t;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getText() {
        return text;
    }

    public void Draw(GraphicsContext g) {
        g.setLineWidth(getThickness());
        g.setFont(Font.font(20));
        g.setFill(this.getColor());
        g.setStroke(this.getColor());
        g.strokeText(text, this.x, this.y);
    }
}
