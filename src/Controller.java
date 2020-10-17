import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;

/**
 * JavaFX controller for the main interface.
 */
public class Controller {
    short[][][] cthead; //medical data
    short min;
    short max;

    WritableImage imageZ;
    WritableImage imageY;
    WritableImage imageX;


    @FXML
    Button button_MIP;
    @FXML
    Button button_Reset;
    @FXML
    Button button_ThumbZ;
    @FXML
    Button button_ThumbY;
    @FXML
    Button button_ThumbX;
    @FXML
    Slider slice_Z;
    @FXML
    Slider slice_Y;
    @FXML
    Slider slice_X;
    @FXML
    Slider scale_Z;
    @FXML
    Slider scale_Y;
    @FXML
    Slider scale_X;
    @FXML
    ImageView view_Z;
    @FXML
    ImageView view_Y;
    @FXML
    ImageView view_X;


    /**
     * Initialises the main window.
     *
     * @throws IOException if data can't be read in
     */
    @FXML
    private void initialize() throws IOException {
        ReadData();
        setupWindow();
        imageZ = new WritableImage(256, 256);
        view_Z.setImage(imageZ);

        imageY = new WritableImage(256, 113);
        view_Y.setImage(imageY);

        imageX = new WritableImage(256, 256);
        view_X.setImage(imageX);

    }


    /**
     * Read and store all data from CThead.
     *
     * @throws IOException if data can't be read from CThead.
     */
    public void ReadData() throws IOException {
        File file = new File("CThead");
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

        min = Short.MAX_VALUE;
        max = Short.MIN_VALUE; // set to extreme values
        short read; // value read in
        int b1;
        int b2; // data is wrong Endian (check wikipedia) for Java so we need to
        // swap the bytes
        // around

        cthead = new short[113][256][256];

        // loop through the data reading it in
        for (int k = 0; k < 113; k++) {
            for (int j = 0; j < 256; j++) {
                for (int i = 0; i < 256; i++) {
                    // because the Endianess is wrong, it needs to be read byte
                    // at a time and
                    // swapped
                    b1 = ((int) in.readByte()) & 0xff; // the 0xff is because
                    // Java does not have
                    // unsigned types
                    b2 = ((int) in.readByte()) & 0xff; // the 0xff is because
                    // Java does not have
                    // unsigned types
                    read = (short) ((b2 << 8) | b1); // and swizzle the bytes
                    // around
                    if (read < min)
                        min = read;
                    if (read > max)
                        max = read;
                    cthead[k][j][i] = read;
                }
            }
        }
        System.out.println(min + " " + max);


        in.close();
    }

    /**
     * Assign actions to all interactive features in window
     */
    public void setupWindow() {
        slice_X.setOnMouseDragged(e -> sliderX(imageX, slice_X.valueProperty().intValue()));
        slice_Z.setOnMouseDragged(e -> sliderZ(imageZ, slice_Z.valueProperty().intValue()));
        slice_Y.setOnMouseDragged(e -> sliderY(imageY, slice_Y.valueProperty().intValue()));

        scale_Z.setOnMouseDragged(e -> view_Z.setImage(resizeBi(scale_Z.valueProperty().doubleValue(), imageZ)));
        scale_Y.setOnMouseDragged(e -> view_Y.setImage(resizeBi(scale_Y.valueProperty().doubleValue(), imageY)));
        scale_X.setOnMouseDragged(e -> view_X.setImage(resizeBi(scale_X.valueProperty().doubleValue(), imageX)));

        button_MIP.setOnMouseClicked(e -> MIP());
        button_Reset.setOnMouseClicked(e -> reset());
        button_ThumbZ.setOnMouseClicked(e -> {
            try {
                handleButton_Thumb(getZThumbnails(113, 256, 256));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        button_ThumbY.setOnMouseClicked(e -> {
            try {
                handleButton_Thumb(getYThumbnails(256, 256, 113));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        button_ThumbX.setOnMouseClicked(e -> {
            try {
                handleButton_Thumb(getXThumbnails(256, 256, 113));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

    }

    /**
     * Reset window to default.
     */
    public void reset() {
        slice_Z.adjustValue(0);
       sliderZ(imageZ, 0);
        slice_Y.adjustValue(0);
        sliderY(imageY, 0);
        slice_X.adjustValue(0);
        sliderX(imageX, 0);

        scale_Z.adjustValue(1);
        view_Z.setImage(resize(1, imageZ));
        scale_Y.adjustValue(1);
        view_Y.setImage(resize(1, imageY));
        scale_X.adjustValue(1);
        view_X.setImage(resize(1, imageX));
    }

    /**
     * Calculates image in Z axis and updates the view to hold the image.
     *
     * @param image input image on the Z axis
     * @param zInput slice number in the Z axis
     */
    public void sliderZ(WritableImage image, int zInput) {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();

        PixelWriter image_writer = image.getPixelWriter();

        float col;
        short datum;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                datum = cthead[zInput][y][x];

                col = (((float) datum - (float) min) / ((float) (max - min)));

                for (int c = 0; c < 3; c++) {
                    image_writer.setColor(x, y, Color.color(col, col, col, 1.0));

                }
            }
        }
        System.out.println(zInput);
        view_Z.setImage(resizeBi(scale_Z.valueProperty().doubleValue(), imageZ));
    }

    /**
     * Calculates image in X axis and updates the view to hold the image.
     *
     * @param image input image on the X axis
     * @param xInput slice number in the X axis
     */
    public void sliderX(WritableImage image, int xInput) {
        PixelWriter image_writer = image.getPixelWriter();

        float col;
        short datum;
        for (int z = 0; z < 113; z++) {
            for (int y = 0; y < 256; y++) {

                datum = cthead[z][y][xInput];

                col = (((float) datum - (float) min) / ((float) (max - min)));

                image_writer.setColor(y, z, Color.color(col, col, col, 1.0));

            }
        }
        System.out.println(xInput);
        view_X.setImage(resizeBi(scale_X.valueProperty().doubleValue(), imageX));
    }

    /**
     * Calculates image in Y axis and updates the view to hold the image.
     *
     * @param image input image on the Y axis
     * @param yInput slice number in the Y axis
     */
    public void sliderY(WritableImage image, int yInput) {

        PixelWriter image_writer = image.getPixelWriter();

        float col;
        short datum;

        for (int z = 0; z < 113; z++) {
            for (int x = 0; x < 256; x++) {

                datum = cthead[z][yInput][x];
                col = (((float) datum - (float) min) / ((float) (max - min)));
                image_writer.setColor(x, z, Color.color(col, col, col, 1.0));

            }
        }
        System.out.println(yInput);
        view_Y.setImage(resizeBi(scale_Y.valueProperty().doubleValue(), imageY));
    }

    /**
     * Performs maximum intensity projection on all axes
     */
    public void MIP() {
        reset();
        MIPZ(imageZ);
        MIPY(imageY);
        MIPX(imageX);
    }

    /**
     * Performs maximum intensity projection in the Z axis
     *
     * @param image in the Z axis
     */
    public void MIPZ(WritableImage image) {
        int h = (int) image.getHeight();
        int w = (int) image.getWidth();
        view_Z.setImage(imageZ);
        scale_Z.adjustValue(1);
        slice_Z.adjustValue(0);

        PixelWriter image_writer = image.getPixelWriter();
        short maxdatum;
        short datum;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                maxdatum = (short) -max;
                for (int z = 0; z < 113; z++) {
                    datum = cthead[z][y][x];

                    if (maxdatum < datum) {
                        maxdatum = datum;
                    }

                    float col = (((float) maxdatum - (float) min) / ((float) (max - min)));
                        image_writer.setColor(x, y, Color.color(col, col, col, 1.0));

                }
            }
        }
    }

    /**
     * Performs maximum intensity projection in the Y axis
     *
     * @param image in the Y axis
     */
    public void MIPY(WritableImage image) {
        PixelWriter image_writer = image.getPixelWriter();
        short maxdatum;
        short datum;

        for (int z = 0; z < 113; z++) {
            for (int x = 0; x < 256; x++) {
                maxdatum = (short) -max;
                for (int y = 0; y < 256; y++) {

                    datum = cthead[z][y][x];

                    if(maxdatum < datum) {
                        maxdatum = datum;
                    }

                    float col = (((float) maxdatum - (float) min) / ((float) (max - min)));
                        image_writer.setColor(x, z, Color.color(col, col, col, 1.0));


                }
            }
        }
        view_Y.setImage(imageY);
    }

    /**
     * Performs maximum intensity projection in the X axis
     *
     * @param image in the X axis
     */
    public void MIPX(WritableImage image) {
        int h = (int) image.getHeight();
        int w = (int) image.getWidth();

        PixelWriter image_writer = image.getPixelWriter();
        short maxdatum;
        short datum;

        for (int z = 0; z < 113; z++) {
            for (int y = 0; y < h; y++) {
                maxdatum = (short) -max;
                for (int x = 0; x < w; x++) {
                    datum = cthead[z][y][x];

                    if (maxdatum < datum) {
                        maxdatum = datum;
                    }

                    float col = (((float) maxdatum - (float) min) / ((float) (max - min)));
                        image_writer.setColor(y, z, Color.color(col, col, col, 1.0));
                }
            }
        }
        view_X.setImage(imageX);
    }


    /**
     * Performs nearest neighbour resize algorithm on an image
     *
     * @param sizeInput factor to increase resolution of original image
     * @param image to be resized
     * @return The resized image
     */
    public WritableImage resize(double sizeInput, WritableImage image) {
        int h = (int) image.getHeight();
        int w = (int) image.getWidth();
        float newH = (float)(h * sizeInput);
        float newW = (float)(w * sizeInput);

        PixelReader reader = image.getPixelReader();


        WritableImage newImage = new WritableImage((int) newW, (int) newH);
        PixelWriter image_writer = newImage.getPixelWriter();

        for (int j = 0; j < newH - 1; j++) {
            for (int i = 0; i < newW - 1; i++) {
                float x = i * (w / newW);
                float y = j * (h / newH);

                Color col = reader.getColor((int) Math.floor(x), (int) Math.floor(y));
                image_writer.setColor(i, j, col);

            }
        }
        return newImage;
    }


    /**
     * Performs bilinear interpolation resize algorithm on an image
     *
     * @param sizeInput factor to increase resolution of original image
     * @param image to be resized
     * @return The resized image
     */
    public static WritableImage resizeBi(double sizeInput, WritableImage image) {
        int h = (int) image.getHeight();
        int w = (int) image.getWidth();
        float newH = (float)(h * sizeInput);
        float newW = (float)(w * sizeInput);

        PixelReader reader = image.getPixelReader();

        WritableImage newImage = new WritableImage((int) newW, (int) newH);
        PixelWriter image_writer = newImage.getPixelWriter();

        for (int j = 0; j < newH - 1; j++) {
            for (int i = 0; i < newW - 1; i++) {

                double oldX = i/sizeInput;
                int x1 = (int) Math.floor(oldX);
                int x2 = x1 + 1;

                double oldY = j/sizeInput;
                int y1 = (int) Math.floor(oldY);
                int y2 = y1 + 1;

                Color col;

                if (y1 >= h - 1 || x1 >= w - 1) {
                    col = reader.getColor((int) Math.floor(oldX), (int) Math.floor(oldY));
                    image_writer.setColor(i, j, col);

                } else {

                    Color bottomLeft = reader.getColor(x1, y1);
                    Color bottomRight = reader.getColor(x1, y2);
                    Color topRight = reader.getColor(x2, y2);
                    Color topLeft = reader.getColor(x1, y2);

                    double V1 = bottomLeft.getRed() + (bottomRight.getRed() - bottomLeft.getRed()) * ((oldX - x1) / (x2 - x1));
                    double V2 = topLeft.getRed() + (topRight.getRed() - topLeft.getRed()) * ((oldX - x1) / (x2 - x1));

                    double colVal = V1 + (V2 - V1) * ((oldY - y1) / (y2 - y1));

                    image_writer.setColor(i, j, Color.color(colVal, colVal, colVal, 1.0));
                }

            }
        }
        return newImage;
    }

    /**
     * Gets all image slices of Z axis in CThead.
     *
     * @param totalSlices number of slices in the scan's Z axis
     * @param width image width in Z axis
     * @param height image height in Z axis
     * @return array of all image slices in the Z axis
     */
    public WritableImage[] getZThumbnails(int totalSlices, int width, int height) {
        WritableImage[] thumbnails = new WritableImage[totalSlices];

        WritableImage image;

        for (int z = 0; z < totalSlices; z ++){
            image = new WritableImage(width, height);
            PixelWriter image_writer = image.getPixelWriter();

            float col;
            short datum;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    datum = cthead[z][y][x];
                    col = (((float) datum - (float) min) / ((float) (max - min)));
                    image_writer.setColor(x, y, Color.color(col, col, col, 1.0));

                }
            }
            thumbnails[z] = image;
        }
        return thumbnails;
    }

    /**
     * Gets all image slices of Y axis in CThead.
     *
     * @param totalSlices number of slices in the scan's Y axis
     * @param width image width in Y axis
     * @param height image height in Y axis
     * @return array of all image slices in the Y axis
     */
    public WritableImage[] getYThumbnails(int totalSlices, int width, int height) {
        WritableImage[] thumbnails = new WritableImage[totalSlices];
        WritableImage image;

        for (int y = 0; y < totalSlices; y++) {
            image = new WritableImage(width, height);
            PixelWriter image_writer = image.getPixelWriter();

            float col;
            short datum;

            for (int z = 0; z < height; z++) {
                for (int x = 0; x < width; x++) {
                    datum = cthead[z][y][x];
                    col = (((float) datum - (float) min) / ((float) (max - min)));
                    image_writer.setColor(x, z, Color.color(col, col, col, 1.0));

                }
            }
            thumbnails[y] = image;
        }
        return thumbnails;
    }

    /**
     * Gets all image slices of X axis in CThead.
     *
     * @param totalSlices number of slices in the scan's X axis
     * @param width image width in X axis
     * @param height image height in X axis
     * @return array of all image slices in the X axis
     */
    public WritableImage[] getXThumbnails(int totalSlices, int width, int height) {
        WritableImage[] thumbnails = new WritableImage[totalSlices];
        WritableImage image;

        for (int x = 0; x < totalSlices; x ++) {
            image = new WritableImage(width, height);
            PixelWriter image_writer = image.getPixelWriter();

            float col;
            short datum;
            for (int z = 0; z < height; z++) {
                for (int y = 0; y < width; y++) {

                    datum = cthead[z][y][x];
                    col = (((float) datum - (float) min) / ((float) (max - min)));
                    image_writer.setColor(y, z, Color.color(col, col, col, 1.0));

                }
            }
            thumbnails[x] = image;
        }
        return thumbnails;
    }


    /**
     * Opens a new window to display thumbnails.
     *
     * @param thumbnails array of images to be displayed on new page
     * @throws IOException if unable to open the thumbnails fxml file.
     */
    @FXML
    private void handleButton_Thumb(WritableImage[] thumbnails) throws IOException {
        Thumbnails.thumbnails = thumbnails;

        // Create a FXML loader for loading the thumbnails FXML file
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().
                getResource("thumbnails.fxml"));

        BorderPane root = fxmlLoader.load();
        Scene scene = new Scene(root);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Mitchell James 981988");

        //must be closed to interact with previous page
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.show();
    }


}
