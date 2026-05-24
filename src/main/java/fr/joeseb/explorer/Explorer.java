package fr.joeseb.explorer;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import java.io.File;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Explorer extends Application {

  private RadialMenu menu;
  private final Stack<ExplorerElement> parentStack = new Stack<>();
  private ExplorerElement currentDirectory = null;

  // Services indépendants
  private ElementRepository repository;
  private DiskManager diskManager;
  private DialogHelper dialogHelper;

  @Override
  public void start(Stage primaryStage) {
    Database.getConnection();
    DatabaseInitializer.initialize();

    // Initialisation des services
    repository = new ElementRepository();
    diskManager = new DiskManager(getHostServices());
    dialogHelper = new DialogHelper(repository);

    menu = new RadialMenu(repository.fetchElements(null), false);

    // Configuration du listener découpé
    menu.setMenuListener(
        new MenuListener() {
          @Override
          public void onElementSelected(ExplorerElement element) {
            if ("Dossier".equalsIgnoreCase(element.getType())) {
              navigateTo(element);
            } else if ("Texte".equalsIgnoreCase(element.getType())) {
              Clipboard clipboard = Clipboard.getSystemClipboard();
              ClipboardContent content = new ClipboardContent();
              content.putString(element.getPath());
              clipboard.setContent(content);
              System.out.println("Texte copie !");
            } else {
              diskManager.openFile(element);
            }
          }

          @Override
          public void onElementEdit(ExplorerElement element) {
            dialogHelper.showEditDialog(primaryStage, element, () -> updateMenu());
          }

          @Override
          public void onBackSelected() {
            navigateBack();
          }
        });

    setupTopUI(primaryStage);

    StackPane root = new StackPane(layoutUI(primaryStage));
    root.setStyle("-fx-background-color: transparent;");

    Scene scene = new Scene(root, 450, 450);
    scene.setFill(Color.TRANSPARENT);

    setupSceneEvents(scene, primaryStage);
    setupGlobalShortcut(primaryStage);

    primaryStage.initStyle(StageStyle.TRANSPARENT);
    primaryStage.setScene(scene);
    primaryStage.setAlwaysOnTop(true);
    Platform.setImplicitExit(false);

    System.out.println("Explorateur pret.");
  }

  private VBox layoutUI(Stage primaryStage) {
    Button btnAdd = new Button("+");
    Button btnEdit = new Button("*");
    Button btnRemove = new Button("-");

    btnAdd.setFocusTraversable(false);
    btnEdit.setFocusTraversable(false);
    btnRemove.setFocusTraversable(false);

    btnAdd.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
    btnEdit.setStyle("-fx-background-color: #ff6f00; -fx-text-fill: white; -fx-font-weight: bold;");
    btnRemove.setStyle(
        "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

    btnAdd.setOnAction(
        e -> dialogHelper.showAddDialog(primaryStage, getCurrentDirId(), this::updateMenu));
    btnEdit.setOnAction(
        e -> {
          ExplorerElement elToEdit = menu.getHighlightedElement();
          if (elToEdit != null)
            dialogHelper.showEditDialog(primaryStage, elToEdit, this::updateMenu);
        });
    btnRemove.setOnAction(e -> handleDelete(primaryStage));

    HBox btnBox = new HBox(10, btnAdd, btnEdit, btnRemove);
    btnBox.setAlignment(Pos.CENTER);
    btnBox.setPadding(new Insets(10));
    btnBox.setId("btnBox");

    VBox layout = new VBox(btnBox, menu);
    layout.setAlignment(Pos.CENTER);
    layout.setStyle("-fx-background-color: transparent;");

    return layout;
  }

  private void setupTopUI(Stage primaryStage) {}

  private void setupSceneEvents(Scene scene, Stage primaryStage) {
    scene.addEventFilter(
        MouseEvent.MOUSE_CLICKED,
        event -> {
          Point2D menuPt = menu.sceneToLocal(event.getSceneX(), event.getSceneY());
          double dist =
              Math.sqrt(Math.pow(menuPt.getX() - 200, 2) + Math.pow(menuPt.getY() - 200, 2));

          HBox btnBox = (HBox) scene.lookup("#btnBox");
          boolean hitButton = false;
          if (btnBox != null) {
            Point2D btnPt = btnBox.sceneToLocal(event.getSceneX(), event.getSceneY());
            hitButton = btnBox.getBoundsInLocal().contains(btnPt);
          }

          if (dist > 150 && !hitButton) {
            primaryStage.hide();
          }
        });

    scene.setOnDragOver(
        event -> {
          if (event.getDragboard().hasFiles() || event.getDragboard().hasString()) {
            event.acceptTransferModes(TransferMode.COPY);
          }
          event.consume();
        });

    scene.setOnDragDropped(
        event -> {
          Dragboard db = event.getDragboard();
          boolean success = false;
          if (db.hasFiles()) {
            for (File file : db.getFiles()) {
              String type = file.isDirectory() ? "Dossier" : "Fichier";
              repository.insertElement(
                  file.getName(), type, file.getAbsolutePath(), getCurrentDirId());
            }
            success = true;
          } else if (db.hasString()) {
            repository.insertElement("Texte_Copie", "Texte", db.getString(), getCurrentDirId());
            success = true;
          }
          event.setDropCompleted(success);
          event.consume();
          updateMenu();
        });

    scene.setOnKeyPressed(
        event -> {
          switch (event.getCode()) {
            case DIGIT1:
            case NUMPAD1:
              menu.highlightSlice(0);
              break;
            case DIGIT2:
            case NUMPAD2:
              menu.highlightSlice(1);
              break;
            case DIGIT3:
            case NUMPAD3:
              menu.highlightSlice(2);
              break;
            case DIGIT4:
            case NUMPAD4:
              menu.highlightSlice(3);
              break;
            case DIGIT5:
            case NUMPAD5:
              menu.highlightSlice(4);
              break;
            case DIGIT6:
            case NUMPAD6:
              menu.highlightSlice(5);
              break;
            case DIGIT7:
            case NUMPAD7:
              menu.highlightSlice(6);
              break;
            case DIGIT8:
            case NUMPAD8:
              menu.highlightSlice(7);
              break;
            case DIGIT9:
            case NUMPAD9:
              menu.highlightSlice(-3);
              menu.executeAction();
              break;
            case DIGIT0:
            case NUMPAD0:
              menu.highlightSlice(-1);
              menu.executeAction();
              break;
            case MULTIPLY:
            case ASTERISK:
              ExplorerElement elToEdit = menu.getHighlightedElement();
              if (elToEdit != null)
                dialogHelper.showEditDialog(primaryStage, elToEdit, this::updateMenu);
              break;
            case ENTER:
              menu.executeAction();
              break;
            case ESCAPE:
              primaryStage.hide();
              break;
            case ADD:
            case PLUS:
              dialogHelper.showAddDialog(primaryStage, getCurrentDirId(), this::updateMenu);
              break;
            case SUBTRACT:
            case MINUS:
              handleDelete(primaryStage);
              break;
          }
        });
  }

  private void navigateTo(ExplorerElement element) {
    parentStack.push(currentDirectory);
    currentDirectory = element;
    updateMenu();
  }

  private void navigateBack() {
    if (!parentStack.isEmpty()) {
      currentDirectory = parentStack.pop();
      updateMenu();
    } else {
      ((Stage) menu.getScene().getWindow()).hide();
    }
  }

  private void handleDelete(Stage primaryStage) {
    ExplorerElement el = menu.getHighlightedElement();
    if (el != null) {
      // On appelle la boîte de dialogue de confirmation
      dialogHelper.showDeleteConfirmation(
          primaryStage,
          el,
          () -> {
            repository.deleteElement(el.getId());
            updateMenu();
          });
    }
  }

  private void updateMenu() {
    List<ExplorerElement> elements;
    if (currentDirectory == null) {
      elements = repository.fetchElements(null);
    } else {
      File realFolder = new File(currentDirectory.getPath());
      if (realFolder.exists() && realFolder.isDirectory()) {
        elements = diskManager.fetchElementsFromDisk(realFolder);
      } else {
        elements = repository.fetchElements(currentDirectory.getId());
      }
    }
    menu.setElements(elements, !parentStack.isEmpty());
  }

  private String getCurrentDirId() {
    return currentDirectory == null ? null : currentDirectory.getId();
  }

  private void setupGlobalShortcut(Stage stage) {
    try {
      Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
      logger.setLevel(Level.OFF);
      logger.setUseParentHandlers(false);
      GlobalScreen.registerNativeHook();
      GlobalScreen.addNativeKeyListener(new GlobalKeyListener(stage));
    } catch (NativeHookException ex) {
      System.err.println("Erreur JNativeHook : " + ex.getMessage());
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}
