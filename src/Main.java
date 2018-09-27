import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javafx.embed.swing.SwingFXUtils;

import com.jhlabs.image.InvertFilter;

import javax.imageio.ImageIO;

public class Main extends Application {

  Stage window;
  Button button;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    window = primaryStage;
    window.setTitle("ICS 499 - Concurrent Photo Editor");


    button = new Button("Choose File");
    ListView imageList = new ListView();


    ImageView iv = new ImageView();
    InvertFilter filter = new InvertFilter();

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open Image File");
    fileChooser.getExtensionFilters().addAll(
        new ExtensionFilter("Image Files", "*.jpg"));

    button.setOnAction(e -> {
      List<Image> images = new ArrayList<>();
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
          BufferedImage img = filter.filter(SwingFXUtils.fromFXImage(image, null), null); // , null);
          String fmt = "jpg";
          File imgFilepath = new File("./testimg/filtered/"+image.toString()+".jpg");
          try {
            ImageIO.write(img, fmt, imgFilepath);
          } catch (IOException ioex) {
            System.out.println("Image file write failed");
          }

        });


        iv.setImage(images.get(1));
        iv.setFitWidth(1000);
        iv.setPreserveRatio(true);
      }
    });


    VBox layout = new VBox();
    layout.getChildren().add(button);
    layout.getChildren().add(imageList);
    layout.getChildren().add(iv);
    Scene scene = new Scene(layout, 1000, 800);
    window.setScene(scene);
    window.show();


  }
}
