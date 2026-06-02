package fr.joeseb.explorer;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import java.io.File;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
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
import javafx.scene.input.KeyCode;
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

  private ElementRepository repository;
  private DiskManager diskManager;
  private DialogHelper dialogHelper;
  private HistoryManager historyManager;

  @Override
  public void start(Stage primaryStage) {
    Database.getConnection();
    DatabaseInitializer.initialize();

    repository = new ElementRepository();

    // NOUVEAU : Récupère l'utilisateur OS et initialise l'historique
    String currentUser = repository.syncSystemUser();
    historyManager = new HistoryManager(repository, currentUser);

    diskManager = new DiskManager(getHostServices());
    dialogHelper = new DialogHelper(repository, historyManager);

    menu = new RadialMenu(repository.fetchElements(null), false);

    menu.setMenuListener(
        new MenuListener() {
          @Override
          public void onElementSelected(ExplorerElement element) {
            if ("Dossier".equalsIgnoreCase(element.getType())
                || "Tag".equalsIgnoreCase(element.getType())) {
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

    System.out.println("Explorateur pret pour l'utilisateur : " + currentUser);
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

          if (dist > 150 && !hitButton) primaryStage.hide();
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
              String id = UUID.randomUUID().toString().substring(0, 8);
              historyManager.executeAction(
                  historyManager
                  .new CreateCommand(
                      id, file.getName(), type, file.getAbsolutePath(), getCurrentDirId()));
            }
            success = true;
          } else if (db.hasString()) {
            String id = UUID.randomUUID().toString().substring(0, 8);
            historyManager.executeAction(
                historyManager
                .new CreateCommand(id, "Texte_Copie", "Texte", db.getString(), getCurrentDirId()));
            success = true;
          }
          event.setDropCompleted(success);
          event.consume();
          updateMenu();
        });

    scene.setOnKeyPressed(
        event -> {
          // NOUVEAU : Raccourcis d'Historique (Undo / Redo)
          if (event.isControlDown() && event.getCode() == KeyCode.Z) {
            historyManager.undo();
            updateMenu();
            return;
          }
          if (event.isControlDown() && event.getCode() == KeyCode.Y) {
            historyManager.redo();
            updateMenu();
            return;
          }

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
      dialogHelper.showDeleteConfirmation(
          primaryStage,
          el,
          () -> {
            historyManager.executeAction(historyManager.new DeleteCommand(el, getCurrentDirId()));
            updateMenu();
          });
    }
  }

  private void updateMenu() {
    List<ExplorerElement> elements;
    if (currentDirectory == null) {
      elements = repository.fetchElements(null);
    } else if ("E_TAG".equals(currentDirectory.getId())) {
      // 2. L'utilisateur a cliqué sur le dossier "Tags" : on affiche tous les tags
      elements = repository.getAllTags();

    } else if ("Tag".equalsIgnoreCase(currentDirectory.getType())) {
      // 3. L'utilisateur a cliqué sur un tag précis (ex: "ttest") : on affiche le contenu
      elements = repository.getElementsByTag(currentDirectory.getId());

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
