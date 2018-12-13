import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


import javafx.embed.swing.SwingFXUtils;


import com.jhlabs.image.InvertFilter;
import com.jhlabs.image.GrayFilter;
import com.jhlabs.image.SolarizeFilter;
import com.jhlabs.image.GrayscaleFilter;
import javafx.stage.WindowEvent;

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
    ListView<String> imagePaths = new ListView<>();
    ComboBox<ImgMod> filterList = new ComboBox<>();
    Label threadmaxlabel = new Label("Max Processing Threads");
    ComboBox<Integer> maxthreads = new ComboBox<>();
    Label imgmaxlabel = new Label("Max Images in Memory");
    ComboBox<Integer> maxmemimgs = new ComboBox<>();
    ImageView iv = new ImageView();
    iv.setFitWidth(500);
    iv.setPreserveRatio(true);

    List<File> selection = new ArrayList<>();

    window = primaryStage;
    window.setTitle("ICS 499 - Concurrent Batch Photo Editor");

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

    maxthreads.setItems(FXCollections.observableArrayList(1, 2, 3, 4));
    maxthreads.getSelectionModel().selectLast();

    maxmemimgs.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    maxmemimgs.getSelectionModel().selectLast();

    // On chooseImageButton click, select files
    chooseImageButton.setOnAction(e -> {
      setFiles(imagePaths, selection);
      Platform.runLater(() -> {
        imagePaths.getSelectionModel().selectLast();
        displayImage(imagePaths.getSelectionModel().getSelectedItem(), iv);
        startJobButton.setDisable(false);
      });
    });

    // Start job on selected files
    startJobButton.setOnAction(e -> {
      Thread jobThread = new Thread(new filterJob(filterList.getValue(), selection,
                                                  maxthreads.getValue(), maxmemimgs.getValue()));
      jobThread.start();
    });

    imagePaths.setOnMouseClicked(e -> {
        Platform.runLater(() -> {
          displayImage(imagePaths.getSelectionModel().getSelectedItem(), iv);
        });
    });

    window.setOnCloseRequest(new EventHandler<WindowEvent>() {
      @Override
      public void handle(WindowEvent event) {
        Platform.exit();
        System.exit(0);
      }
    });


    VBox layout = new VBox();
    layout.getChildren().add(filterList);
    HBox maxThreadBox = new HBox();
    maxThreadBox.getChildren().addAll(threadmaxlabel, maxthreads);
    layout.getChildren().add(maxThreadBox);
    HBox maxMemImgBox = new HBox();
    maxMemImgBox.getChildren().addAll(imgmaxlabel, maxmemimgs);
    layout.getChildren().add(maxMemImgBox);
    layout.getChildren().add(chooseImageButton);
    layout.getChildren().add(startJobButton);
    layout.getChildren().add(imagePaths);
    layout.getChildren().add(iv);
    Scene scene = new Scene(layout, 500, 750);
    window.setScene(scene);
    window.show();

  }

  private void setFiles(ListView<String> imPaths, List<File> selectedFiles) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open Image File");
    fileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("Image Files", "*.jpg"));

    List<File> tempFiles = fileChooser.showOpenMultipleDialog(window);
    tempFiles.forEach(file -> {
      selectedFiles.add(file);
      Platform.runLater(() -> imPaths.getItems().add(file.getPath()));
    });

  }

  private void displayImage(String imPath, ImageView iv) {
    File file = new File(imPath);
    Image img = null;
    try {
      img = new Image(file.toURI().toURL().toString());
    } catch (MalformedURLException urlex) {
      System.out.println("URL to file conversion failed");
    }
    resizeImageViewport(img, iv);
    iv.setImage(img);
  }

  private void resizeImageViewport (Image img, ImageView iv) {
      if (img.getWidth() > img.getHeight()) {
          iv.setFitWidth(500);
      } else {
          iv.setFitHeight(400);
      }
  }

  private class filterJob implements Runnable {

    private ImgMod filterContainer;
    private List<File> files;
    private ListView<String> outImagePaths;
    private ImageView jobIV;
    private int maxpThreads;
    private int maxmemimgs;

    private Semaphore rMutex;
    private Semaphore wMutex;

    private Semaphore rAvailable;
    private Semaphore rFull;
    private Queue<Image> rBuffer;

    private Semaphore wAvailable;
    private Semaphore wFull;
    private Queue<BufferedImage> wBuffer;

    ExecutorService pool;

    filterJob(ImgMod fc, List<File> files, int maxThreads, int maxImages) {

      this.filterContainer = fc;
      this.files = files;
      this.outImagePaths = new ListView<>();
      this.jobIV = new ImageView();
      this.maxpThreads = maxThreads;
      this.maxmemimgs = maxImages;

      this.rMutex = new Semaphore(1);
      this.wMutex = new Semaphore(1);
      this.rAvailable = new Semaphore(maxmemimgs);
      this.rFull = new Semaphore(0);
      this.rBuffer = new LinkedList<>();
      this.wAvailable = new Semaphore(0);
      this.wFull = new Semaphore(maxmemimgs);
      this.wBuffer = new LinkedList<>();

      this.pool = Executors.newFixedThreadPool(maxThreads+2);


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
          displayImage(outImagePaths.getSelectionModel().getSelectedItem(), jobIV);
      });

      jobWindow.setOnCloseRequest(new EventHandler<WindowEvent>() {
        @Override
        public void handle(WindowEvent event) {
          pool.shutdown();
          try {
            pool.awaitTermination(3, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            System.out.println("Pool shutdown await was interrupted before countdown complete.");
          }
          pool.shutdownNow();
        }
      });
    }

    @Override
    public void run () {

      pool.execute(new imageProducer(files, rBuffer, rAvailable, rFull, rMutex));

      for (int t=0; t<maxpThreads; t++) {
        pool.execute(
            new imageProcessor(rBuffer, wBuffer, filterContainer,
                               rAvailable, rFull, rMutex,
                               wAvailable, wFull, wMutex)
        );
      }

      pool.execute(new imageWriter(wBuffer, filterContainer, outImagePaths, jobIV,
                                   wAvailable, wFull, wMutex));

      pool.shutdown();
//      files.forEach(file -> {
//
//        // Load the image from file
//        Image image = null;
//        try {
//            image = new Image(file.toURI().toURL().toString());
//        } catch (MalformedURLException x) {
//            System.out.println("To URL failed");
//        }
//
//        // Process the image
//        BufferedImage img = filterContainer.getFilter().filter(SwingFXUtils.fromFXImage(image, null), null);
//
//        // Add path to the display
//        String fmt = "jpg";
//        String imgname = null;
//        try {
//            URL imgurl = new URL(image.getUrl());
//            imgname = imgurl.getPath();
//            File f = new File(imgname);
//            imgname = f.getName();
//        } catch (MalformedURLException urlex) {
//            System.out.println("URL to imgname conversion failed");
//        }
//        File imgFilepath = new File("./testimg/filtered/" + filterContainer.toString()+ "_" + imgname);
//        Platform.runLater(() -> outImagePaths.getItems().add(imgFilepath.getPath()));
//
//
//        // Write the image back to a file
//        try {
//            ImageIO.write(img, fmt, imgFilepath);
//        } catch (IOException ioex) {
//            System.out.println("Image file write failed" + imgname);
//        }
//
//        Platform.runLater(() -> {
//          resizeImageViewport(SwingFXUtils.toFXImage(img, null), jobIV);
//          jobIV.setImage(SwingFXUtils.toFXImage(img, null));
//        });
//
//      });
    }

    private class imageProducer implements Runnable {
      private Semaphore rAvailable;
      private Semaphore rFull;
      private Semaphore rMutex;
      private Queue<Image> outQueue;
      private Queue<File> taskList;

      imageProducer(List<File> inFiles, Queue<Image> outImages, Semaphore ra, Semaphore rf, Semaphore rm) {

        this.rAvailable = ra;
        this.rFull = rf;
        this.rMutex = rm;
        this.outQueue = outImages;
        this.taskList = new LinkedList<>();

        try{
          rMutex.acquire();
        } catch (InterruptedException e) {
          System.out.println("Mutex acquire on loading taskList failed");
        }
        for (int i=0; i<inFiles.size(); i++) {this.taskList.offer(inFiles.get(i));}
        rMutex.release();

        System.out.println("imageProducer thread initialized.");
      }

      @Override
      public void run() {
        File tempFile;
        while(taskList.size() > 0) {
          try {
            rAvailable.acquire();
            rMutex.acquire();
          } catch (InterruptedException e) {
            System.out.println("Read available or mutex semaphore acquire failed in imageProducer while adding element to queue");
          }
          tempFile = taskList.poll();

          Image tempImage = null;
          try {
            tempImage = new Image(tempFile.toURI().toURL().toString());
          } catch (MalformedURLException x) {
            System.out.println("To URL failed");
          }
          outQueue.offer(tempImage);

          rMutex.release();
          rFull.release();

          System.out.println("Converted file to Image and loaded into memory. rA = " + rAvailable.availablePermits() + " rF = " + rFull.availablePermits());
        }
      }
    } // End Image Producer

    private class imageProcessor implements Runnable {

      private Queue<Image> toProcess;
      private Queue<BufferedImage> imgProcessed;
      private ImgMod filterContainer;

      private Semaphore rAvailable;
      private Semaphore rFull;
      private Semaphore rMutex;

      private Semaphore wAvailable;
      private Semaphore wFull;
      private Semaphore wMutex;

      imageProcessor(Queue<Image> inQueue, Queue<BufferedImage> outQueue, ImgMod fc,
                     Semaphore ra, Semaphore rf, Semaphore rm,
                     Semaphore wa, Semaphore wf, Semaphore wm) {

        this.toProcess = inQueue;
        this.imgProcessed = outQueue;
        this.filterContainer = fc;

        this.rAvailable = ra;
        this.rFull = rf;
        this.rMutex = rm;

        this.wAvailable = wa;
        this.wFull = wf;
        this.wMutex = wm;

        System.out.println("imageProcessor thread Initialized.");
      }

      @Override
      public void run() {

        while(true) {
          System.out.println("imageProcessor trying to consume from toProcess queue.");
          try {
            rFull.acquire();
            rMutex.acquire();
          } catch (InterruptedException e) {
            System.out.println("rFull or rMutex acquire failed in processing thread");
          }
          Image tempImage = toProcess.poll();
          System.out.println("imageProcessing thread polled the toProcess queue.");
          rMutex.release();
          rAvailable.release();

          BufferedImage img = filterContainer.getFilter().filter(SwingFXUtils.fromFXImage(tempImage, null), null);

          try {
            wFull.acquire();
            wMutex.acquire();
          } catch (InterruptedException e) {
            System.out.println("rFull or rMutex acquire failed in processing thread");
          }
          imgProcessed.offer(img);
          System.out.println("Image processing thread offered an image to the imgProcessed queue.");
          wMutex.release();
          wAvailable.release();

        }

      }
    } // End of imageProcessor

    private class imageWriter implements Runnable {

      private Queue<BufferedImage> toWrite;
      private ImgMod filterContainer;
      private ListView<String> outImagePaths;
      private ImageView jobIV;
      private Semaphore wAvailable;
      private Semaphore wFull;
      private Semaphore wMutex;

      private int counter;

      imageWriter(Queue<BufferedImage> inQueue, ImgMod fc,
                  ListView<String> pathsView, ImageView iv,
                  Semaphore wa, Semaphore wf, Semaphore wm) {
        this.toWrite = inQueue;
        this.filterContainer = fc;
        this.jobIV = iv;
        this.outImagePaths = pathsView;
        this.wAvailable = wa;
        this.wFull = wf;
        this.wMutex = wm;
        this.counter = 0;

        System.out.println("imageWriter thread initialized.");
      }

      @Override
      public void run() {
        while(true) {
          try {
            wAvailable.acquire();
            wMutex.acquire();
          } catch (InterruptedException e) {
            System.out.println("Acquiring semaphore for wAvailable or wMutex failed in imageWriter thread.");
          }
          BufferedImage img = toWrite.poll();
          counter++;
          System.out.println("imageWriter thread acquired image " + counter + " to export.");
          wMutex.release();
          wFull.release();

          String fmt = "jpg";
          String imgname = filterContainer.toString()+ "_jobOutput_" + counter + ".jpg";
          File imgFilepath = new File("./testimg/filtered/" + imgname);
          Platform.runLater(() -> outImagePaths.getItems().add(imgFilepath.getPath()));


          // Write the image back to a file
          try {
            ImageIO.write(img, fmt, imgFilepath);
          } catch (IOException ioex) {
            System.out.println("Image file write failed" + imgname);
          }

          Platform.runLater(() -> {
            resizeImageViewport(SwingFXUtils.toFXImage(img, null), jobIV);
            jobIV.setImage(SwingFXUtils.toFXImage(img, null));
          });
        }
      }
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
