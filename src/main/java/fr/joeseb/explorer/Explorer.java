package fr.joeseb.explorer;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Explorer extends Application {

  private RadialMenu menu;
  // 1. La pile stocke maintenant les éléments complets
  private final Stack<ExplorerElement> parentStack = new Stack<>();
  private ExplorerElement currentDirectory = null;

  @Override
  public void start(Stage primaryStage) {
    Database.getConnection();
    DatabaseInitializer.initialize();

    List<ExplorerElement> elements = fetchElementsFromDb(null);

    menu = new RadialMenu(elements, false);
    menu.setOnElementSelectedListener(
        new RadialMenu.OnElementSelectedListener() {
          @Override
          public void onElementSelected(ExplorerElement element) {
            if ("Dossier".equalsIgnoreCase(element.getType())) {
              navigateTo(element);
            } else {
              openFile(element);
            }
          }

          @Override
          public void onBackSelected() {
            navigateBack();
          }
        });

    StackPane root = new StackPane(menu);
    root.setStyle("-fx-background-color: transparent;");

    Scene scene = new Scene(root, 400, 400);
    scene.setFill(Color.TRANSPARENT);

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
              break;
            case DIGIT0:
            case NUMPAD0:
              menu.highlightSlice(-1);
              menu.executeAction(); // Ferme instantanément à la racine
              break;
            case ENTER:
              menu.executeAction();
              break;
            case ESCAPE:
              primaryStage.hide();
              break;
          }
        });

    primaryStage.initStyle(StageStyle.TRANSPARENT);
    primaryStage.setScene(scene);
    primaryStage.setAlwaysOnTop(true);

    setupGlobalShortcut(primaryStage);
    Platform.setImplicitExit(false);

    System.out.println("🚀 Explorateur prêt. Appuyez sur ESPACE pour afficher la roue.");
  }

  private void openFile(ExplorerElement element) {
    System.out.println("Ouverture de : " + element.getName() + " (" + element.getPath() + ")");
    try {
      File file = new File(element.getPath());
      if (file.exists()) {
        getHostServices().showDocument(file.toURI().toString());
      } else {
        getHostServices().showDocument(element.getPath());
      }
    } catch (Exception e) {
      System.err.println("Erreur lors de l'ouverture du fichier : " + e.getMessage());
    }
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
      System.out.println("Fermeture de la roue.");
    }
  }

  // --- LE COEUR DU MOTEUR HYBRIDE ---
  private void updateMenu() {
    List<ExplorerElement> elements;

    if (currentDirectory == null) {
      // 1. On est à la racine de la base de données
      elements = fetchElementsFromDb(null);
    } else {
      File realFolder = new File(currentDirectory.getPath());
      System.out.println(
          "🔍 Recherche du dossier : "
              + realFolder.getAbsolutePath()
              + " | Existe sur le PC ? "
              + realFolder.exists());
      if (realFolder.exists() && realFolder.isDirectory()) {
        // 2. Le dossier existe physiquement sur le disque dur ! On lit le PC de l'utilisateur.
        elements = fetchElementsFromDisk(realFolder);
      } else {
        // 3. C'est un dossier purement virtuel dans la BD (ex: "Mes Applications")
        elements = fetchElementsFromDb(currentDirectory.getId());
      }
    }

    menu.setElements(elements, !parentStack.isEmpty());
  }

  // LECTURE DU DISQUE PHYSIQUE
  private List<ExplorerElement> fetchElementsFromDisk(File folder) {
    List<ExplorerElement> elements = new ArrayList<>();
    File[] files = folder.listFiles();

    if (files != null) {
      for (File file : files) {
        if (!file.isHidden()) { // Ignore les fichiers cachés du système
          String type = file.isDirectory() ? "Dossier" : "Fichier";
          // Le chemin réel devient à la fois l'ID et l'emplacement
          elements.add(
              new ExplorerElement(
                  file.getAbsolutePath(), file.getName(), type, file.getAbsolutePath()));
        }
      }
    }
    return elements;
  }

  // LECTURE DE LA BASE DE DONNÉES
  private List<ExplorerElement> fetchElementsFromDb(String parentId) {
    List<ExplorerElement> elements = new ArrayList<>();
    String sql =
        (parentId == null)
            ? "SELECT id_element, nom_element, type_element, emplacement FROM Element WHERE"
                + " id_parent IS NULL ORDER BY date_creation"
            : "SELECT id_element, nom_element, type_element, emplacement FROM Element WHERE"
                + " id_parent = ? ORDER BY date_creation";

    try (PreparedStatement pstmt = Database.getConnection().prepareStatement(sql)) {
      if (parentId != null) pstmt.setString(1, parentId);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          // On remplace le tag <HOME> par le vrai chemin du PC de l'utilisateur
          String rawPath = rs.getString("emplacement");
          if (rawPath != null && rawPath.contains("<HOME>")) {
            rawPath =
                rawPath
                    .replace("<HOME>", System.getProperty("user.home"))
                    .replace("/", File.separator);
          }

          elements.add(
              new ExplorerElement(
                  rs.getString("id_element"),
                  rs.getString("nom_element"),
                  rs.getString("type_element"),
                  rawPath));
        }
      }
    } catch (Exception e) {
      System.err.println("Erreur BD : " + e.getMessage());
    }
    return elements;
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
