import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


import javafx.embed.swing.SwingFXUtils;


import com.jhlabs.image.InvertFilter;
import com.jhlabs.image.GrayFilter;
import com.jhlabs.image.SolarizeFilter;
import com.jhlabs.image.GrayscaleFilter;

import javax.imageio.ImageIO;

public class Main extends Application {


    private Stage window;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {


        Button chooseImageButton;
        Button startJobButton;
        List<Image> imageList = new ArrayList<>();
        ListView<String> imagePaths = new ListView<>();
        ComboBox<ImgMod> filterList = new ComboBox<>();
        ImageView iv = new ImageView();


        List<File> selection = new ArrayList<>();

        window = primaryStage;
        window.setTitle("ICS 499 - Concurrent Photo Editor");

        // Set up buttons
        chooseImageButton = new Button("Choose jpg files");
        startJobButton = new Button("Start New Job");
        startJobButton.setDisable(true);

        // Combo box receives image filter objects that contain name and filter
        filterList.setItems(FXCollections.observableArrayList(
                new ImgMod("Invert Filter", new InvertFilter()),
                new ImgMod("Gray Filter", new GrayFilter()),
                new ImgMod("Solarize Filter", new SolarizeFilter()),
                new ImgMod("Grayscale Filter", new GrayscaleFilter())
        ));
        //Chooses first filter as default.
        filterList.getSelectionModel().selectFirst();

        // On chooseImageButton click, select files
        chooseImageButton.setOnAction(e -> {
//      List<File> selection = fileChooser.showOpenMultipleDialog(window);
            setFiles(imagePaths, imageList, selection);
            iv.setFitWidth(500);
            iv.setPreserveRatio(true);
            startJobButton.setDisable(false);
        });

        // Start job on selected files
        startJobButton.setOnAction(e -> {
            new filterJob(filterList.getValue(), selection).process();
            iv.setImage(imageList.get(imageList.size()-1));
        });

        imagePaths.setOnMouseClicked(e -> {
            iv.setImage(imageList.get(imagePaths.getSelectionModel().getSelectedIndex()));
            resizeImageViewport(imageList.get(imagePaths.getSelectionModel().getSelectedIndex()), iv);
        });


        VBox layout = new VBox();
        layout.getChildren().add(filterList);
        layout.getChildren().add(chooseImageButton);
        layout.getChildren().add(startJobButton);
        layout.getChildren().add(imagePaths);
        layout.getChildren().add(iv);
        Scene scene = new Scene(layout, 500, 750);
        window.setScene(scene);
        window.show();

    }

    private void setFiles(ListView<String> imPaths, List<Image> images, List<File> selectedFiles) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image File");
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("Image Files", "*.jpg"));

        selectedFiles = fileChooser.showOpenMultipleDialog(window);

    }

    private void resizeImageViewport (Image img, ImageView iv) {
        if (img.getWidth() > img.getHeight()) {
            iv.setFitWidth(500);
        } else {
            iv.setFitHeight(400);
        }
    }

    private class filterJob {

        private ImgMod filterContainer;
        private List<File> files;
        //    private List<Image> unfilteredImages;
//    private List<Image> filteredImages;
        private ListView<String> outImagePaths;
        private ImageView jobIV;

        filterJob(ImgMod fc, List<File> files) {

            this.filterContainer = fc;
            this.files = files;
//      this.filteredImages = new ArrayList<>();
            this.outImagePaths = new ListView<>();
            this.jobIV = new ImageView();

            Stage jobWindow = new Stage();
            VBox jobLayout = new VBox();
            Scene jobScene = new Scene(jobLayout, 500, 750);
            jobWindow.setScene(jobScene);
            jobWindow.setTitle(filterContainer.toString() + " Job");
            jobIV.setFitWidth(500);
            jobIV.setPreserveRatio(true);

            jobLayout.getChildren().add(outImagePaths);
            jobLayout.getChildren().add(jobIV);
            jobWindow.show();

            outImagePaths.setOnMouseClicked(e -> {
                File file = new File(outImagePaths.getSelectionModel().getSelectedItem());
                Image img = new Image(file.toURI().toURL().toString());
                jobIV.setImage(img);
                resizeImageViewport(img, jobIV);
            });
        }

        protected void process () {

            files.forEach(file -> {

                // Load the image from file
                Image image = null;
                try {
                    image = new Image(file.toURI().toURL().toString());
                } catch (MalformedURLException x) {
                    System.out.println("To URL failed");
                }

                // Process the image
                BufferedImage img = filterContainer.getFilter().filter(SwingFXUtils.fromFXImage(image, null), null);

                // Add path to the display
                String fmt = "jpg";
                String imgname;
                try {
                    URL imgurl = new URL(image.getUrl());
                    imgname = imgurl.getPath();
                    File f = new File(imgname);
                    imgname = f.getName();
                } catch (MalformedURLException urlex) {
                    System.out.println("URL to imgname conversion failed");
                }
                File imgFilepath = new File("./testimg/filtered/" + filterContainer.toString()+ "_" + imgname);
                outImagePaths.getItems().add(imgFilepath.getPath());


                // Write the image back to a file
                try {
                    ImageIO.write(img, fmt, imgFilepath);
                } catch (IOException ioex) {
                    System.out.println("Image file write failed" + imgname);
                }

                jobIV.setImage(SwingFXUtils.toFXImage(img, null));

            });

        }
    }

    // Nested class to create an object that can display a filter name and store a filter for the combo box.
    public class ImgMod {
        private String name;
        private BufferedImageOp filter;

        @Override
        public String toString() {
            return this.name;
        }

        BufferedImageOp getFilter() {
            return filter;
        }

        ImgMod (String name, BufferedImageOp filter) {
            this.name = name;
            this.filter = filter;
        }
    }
}
