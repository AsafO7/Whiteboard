package whiteboard.client;

/* CSS code for styling JavaFX elements. */

public class CssLayouts {
    public static final String cssBorder = "-fx-border-color: black;\n" +
            "-fx-border-width: 3;\n" +
            "-fx-border-radius: 3;\n" +
            "-fx-background-color: white";

    public static final String cssBottomLayout = cssBorder + ";\n" +
            "-fx-background-color: #3399AA";

    public static final String cssBottomLayoutText = "-fx-font-weight: bold;\n" +
            "-fx-font-size: 14";

    public static final String cssTopLayout = cssBorder + ";\n" +
            "-fx-background-color:#3399AA;\n" +
            "-fx-alignment: center;\n" +
            "-fx-font-size: 25;\n" +
            "-fx-font-family: Comic Sans MS;\n" +
            "-fx-font-style: italic;\n" +
            "-fx-font-weight: bold";

    public static final String cssExplanationText = "-fx-font-weight: bold;\n" +
            "-fx-font-size: 16;\n" +
            "-fx-fill: #A040A0";

    public static final String cssChatText = "-fx-font-size: 14;\n" +
            "-fx-fill: #3B92D5";

    public static final String cssLeftMenu = cssBorder + ";\n-fx-font-weight: bold;\n" +
            "-fx-padding: 3 0 0 10;\n" +
            "-fx-font-size: 16";
    public static final String cssLobbiesStyle = cssBorder + ";\n-fx-font-size: 18;\n" +
            "-fx-font-weight: bold;\n" +
            "-fx-border-color: gray";

    public static final String cssBoardRightMenu = CssLayouts.cssBorder + ";\n-fx-padding: 6 0 0 6;";
}
