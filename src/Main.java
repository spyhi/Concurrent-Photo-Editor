import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOError;
import java.net.MalformedURLException;
import javafx.embed.swing.SwingFXUtils;

import com.jhlabs.image.InvertFilter;

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
    ImageView iv = new ImageView();
    Label fileLabel = new Label("File Name Here");

    button.setOnAction(e -> {
      Image image;
      InvertFilter filter = new InvertFilter();
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Open Image File");
      fileChooser.getExtensionFilters().addAll(
          new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
      File selectedFile = fileChooser.showOpenDialog(window);

      if (selectedFile != null) {
        fileLabel.setText(selectedFile.getName());
        try {
          image = new Image(selectedFile.toURI().toURL().toString());
          image = SwingFXUtils.toFXImage(
              filter.filter(
                  SwingFXUtils.fromFXImage(
                      image, null), null), null);
          iv.setImage(image);
          iv.setFitWidth(250);
          iv.setPreserveRatio(true);
        } catch (MalformedURLException x) {
          System.out.println("To URL failed");
        }
      }
    });


    VBox layout = new VBox();
    layout.getChildren().add(button);
    layout.getChildren().add(fileLabel);
    layout.getChildren().add(iv);
    Scene scene = new Scene(layout, 300, 300);
    window.setScene(scene);
    window.show();


  }
}
