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

  Stage window;
  Button imageJobButton;
  BufferedImageOp filter;
  ComboBox<ImgMod> filterList = new ComboBox<>();
  ListView imageList;
  ImageView iv;
  FileChooser fileChooser;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    window = primaryStage;
    window.setTitle("ICS 499 - Concurrent Photo Editor");

    // Set up imageJobButton
    imageJobButton = new Button("Choose jpg files");
    // On imageJobButton click, select files and start filter job
    imageJobButton.setOnAction(e -> filterJob());

    // Combo box receives image filter objects that contain name and filter
    filterList.setItems(FXCollections.observableArrayList(
        new ImgMod("Invert Filter", new InvertFilter()),
        new ImgMod("Gray Filter", new GrayFilter()),
        new ImgMod("Solarize Filter", new SolarizeFilter()),
        new ImgMod("Grayscale Filter", new GrayscaleFilter())
    ));
    //Chooses first filter as default.
    filterList.getSelectionModel().selectFirst();
    filter = filterList.getValue().getFilter();

    // Switches filter automatically every time image is selected.
    filterList.setOnAction(e -> setFilter(filterList.getValue().getFilter()));

    imageList = new ListView();
    iv = new ImageView();
    fileChooser = new FileChooser();
    fileChooser.setTitle("Open Image File");
    fileChooser.getExtensionFilters().addAll(
        new ExtensionFilter("Image Files", "*.jpg"));




    VBox layout = new VBox();
    layout.getChildren().add(filterList);
    layout.getChildren().add(imageJobButton);
    layout.getChildren().add(imageList);
    layout.getChildren().add(iv);
    Scene scene = new Scene(layout, 1000, 800);
    window.setScene(scene);
    window.show();



  }

  private void setFilter (BufferedImageOp newFilter) {
    filter = newFilter;
  }

  private void filterJob() {
      imageJobButton.setDisable(true);
      List<Image> images = new ArrayList<>();
      imageList.getItems().clear();
      List<File> selectedFiles = fileChooser.showOpenMultipleDialog(window);

      if (selectedFiles != null) {
        selectedFiles.forEach(file -> {
          imageList.getItems().add(file.toString());
          try {
            images.add(new Image(file.toURI().toURL().toString()));
          } catch (MalformedURLException x) {
            System.out.println("To URL failed");
          }
        });

        images.forEach(image -> { //SwingFXUtils.toFXImage(
          String imgname = null;
          BufferedImage img = filter.filter(SwingFXUtils.fromFXImage(image, null), null); // , null);
          String fmt = "jpg";
          try {
            URL imgurl = new URL(image.getUrl());
            imgname = imgurl.getPath();
            File f = new File(imgname);
            imgname = f.getName();
          } catch (MalformedURLException urlex) {
            System.out.println("URL to imgname conversion failed");
          }
          File imgFilepath = new File("./testimg/filtered/filtered_"+ imgname);
          try {
            ImageIO.write(img, fmt, imgFilepath);
          } catch (IOException ioex) {
            System.out.println("Image file write failed" + imgname);
          }

        });


        iv.setImage(images.get(images.size()-1));
        iv.setFitWidth(500);
        iv.setPreserveRatio(true);
      }
    imageJobButton.setDisable(false);
  }


  // Nested class to create an object that can display a filter name and store a filter for the combo box.
  public class ImgMod {
    private String name;
    private BufferedImageOp filter;

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return this.name;
    }

    public BufferedImageOp getFilter() {
      return filter;
    }

    public ImgMod (String name, BufferedImageOp filter) {
      this.name = name;
      this.filter = filter;
    }
  }
}
