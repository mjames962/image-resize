import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;

import java.io.IOException;

public class Thumbnails {

   @FXML
    GridPane gridPane;
   @FXML
   ImageView thumbView;

   public static WritableImage[] thumbnails;

    @FXML
    private void initialize() throws IOException {
        int col = 1;
        int row = 0;

        for(WritableImage image : thumbnails) {

            ImageView view = new ImageView(Controller.resizeBi(0.3, image));
            view.setOnMouseClicked(e -> {
                    thumbView.setImage(image);
            });

            if (col > 10) {
                col = 1;
                row++;
            }

            gridPane.add(view, col, row);

            col ++;

        }
    }
}
