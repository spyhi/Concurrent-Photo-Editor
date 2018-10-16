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



  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {

    Stage window;
    Button chooseImageButton;
    Button startJobButton;
    List<Image> imageList = new ArrayList<>();
    ListView<String> imagePaths = new ListView<>();
    ComboBox<ImgMod> filterList = new ComboBox<>();
    ImageView iv = new ImageView();

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open Image File");
    fileChooser.getExtensionFilters().addAll(
        new ExtensionFilter("Image Files", "*.jpg"));

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
    ImgMod currentFilter = filterList.getValue();

    // Switches filter automatically every time image is selected.
    var lambdaGuard = new Object(){ImgMod temp = currentFilter; };
    filterList.setOnAction(e -> lambdaGuard.temp = filterList.getValue());

    // On chooseImageButton click, select files
    chooseImageButton.setOnAction(e -> {
      List<File> selection = fileChooser.showOpenMultipleDialog(window);
      setFiles(imagePaths, imageList, selection);
      iv.setFitWidth(500);
      iv.setPreserveRatio(true);
      startJobButton.setDisable(false);
    });

    // Start job on selected files
    startJobButton.setOnAction(e -> {
      filterJob(currentFilter, imageList);
      iv.setImage(imageList.get(imageList.size()-1));
    });

    imagePaths.setOnMouseClicked(e -> {
      iv.setImage(imageList.get(imagePaths.getSelectionModel().getSelectedIndex()));
    });


    VBox layout = new VBox();
    layout.getChildren().add(filterList);
    layout.getChildren().add(chooseImageButton);
    layout.getChildren().add(startJobButton);
    layout.getChildren().add(imagePaths);
    layout.getChildren().add(iv);
    Scene scene = new Scene(layout, 1000, 800);
    window.setScene(scene);
    window.show();

  }

  private void setFiles(ListView<String> imPaths, List<Image> images, List<File> selectedFiles) {

    if (selectedFiles != null) {
      images.clear();
      imPaths.getItems().clear();

      selectedFiles.forEach(file -> {
        imPaths.getItems().add(file.toString());
        try {
          images.add(new Image(file.toURI().toURL().toString()));
        } catch (MalformedURLException x) {
          System.out.println("To URL failed");
        }
      });
    }

  }

  private void filterJob(ImgMod filterContainer, List<Image> images) {

    List<Image> temp = new ArrayList<>();

    images.forEach(image -> {
      String imgname = null;
      BufferedImage img = filterContainer.getFilter().filter(SwingFXUtils.fromFXImage(image, null), null);
      temp.add(SwingFXUtils.toFXImage(img, null));
      String fmt = "jpg";
      try {
        URL imgurl = new URL(image.getUrl());
        imgname = imgurl.getPath();
        File f = new File(imgname);
        imgname = f.getName();
      } catch (MalformedURLException urlex) {
        System.out.println("URL to imgname conversion failed");
      }
      File imgFilepath = new File("./testimg/filtered/" + filterContainer.toString()+ "_" + imgname);
      try {
        ImageIO.write(img, fmt, imgFilepath);
      } catch (IOException ioex) {
        System.out.println("Image file write failed" + imgname);
      }
    });

    images.clear();
    temp.forEach(i -> images.add(i));
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
