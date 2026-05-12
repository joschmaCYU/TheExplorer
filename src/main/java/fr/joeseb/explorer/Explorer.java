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
  private final Stack<String> parentStack = new Stack<>();
  private String currentParentId = null;

  @Override
  public void start(Stage primaryStage) {
    // 1. Initialisation silencieuse de la base de données et des données de base
    Database.getConnection();
    DatabaseInitializer.initialize();

    // 2. Récupération des dossiers racine
    List<ExplorerElement> elements = fetchElementsFromDb(null);

    // 3. Création du menu radial
    menu = new RadialMenu(elements);
    menu.setOnElementSelectedListener(
        new RadialMenu.OnElementSelectedListener() {
          @Override
          public void onElementSelected(ExplorerElement element) {
            if ("Dossier".equals(element.getType())) {
              navigateTo(element.getId());
            } else {
              openFile(element);
            }
          }

          @Override
          public void onBackSelected() {
            navigateBack();
          }
        });

    // 4. Configuration de l'interface (Transparence totale)
    StackPane root = new StackPane(menu);
    root.setStyle("-fx-background-color: transparent;");

    Scene scene = new Scene(root, 400, 400);
    scene.setFill(Color.TRANSPARENT);

    // Gestion des touches (1-8 pour les parts, 0 pour retour, 9 pour suivant, Enter pour valider)
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
              break; // Suivant
            case DIGIT0:
            case NUMPAD0:
              menu.highlightSlice(-1);
              break; // Retour
            case ENTER:
              menu.executeAction();
              break;
            case ESCAPE:
              primaryStage.hide();
              break;
          }
        });

    // Paramètres de la fenêtre système
    primaryStage.initStyle(StageStyle.TRANSPARENT);
    primaryStage.setScene(scene);
    primaryStage.setAlwaysOnTop(true);

    // 5. Configuration de JNativeHook (Raccourci global)
    setupGlobalShortcut(primaryStage);

    // Empêche l'application de s'arrêter quand on ferme la fenêtre
    Platform.setImplicitExit(false);

    System.out.println("Explorateur prêt. Appuyez sur ESPACE pour afficher la roue.");
  }

  private void openFile(ExplorerElement element) {
    System.out.println("Ouverture de : " + element.getName() + " (" + element.getPath() + ")");
    try {
      File file = new File(element.getPath());
      if (file.exists()) {
        getHostServices().showDocument(file.toURI().toString());
      } else {
        // Tentative d'ouverture via le système si le chemin n'est pas un fichier local valide
        // (Certains chemins dans la DB sont peut-être virtuels ou spécifiques)
        getHostServices().showDocument(element.getPath());
      }
    } catch (Exception e) {
      System.err.println("Erreur lors de l'ouverture du fichier : " + e.getMessage());
    }
  }

  private void navigateTo(String parentId) {
    parentStack.push(currentParentId);
    currentParentId = parentId;
    updateMenu();
  }

  private void navigateBack() {
    if (!parentStack.isEmpty()) {
      currentParentId = parentStack.pop();
      updateMenu();
    } else {
      System.out.println("Déjà à la racine");
    }
  }

  private void updateMenu() {
    List<ExplorerElement> elements = fetchElementsFromDb(currentParentId);
    menu.setElements(elements);
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

  private List<ExplorerElement> fetchElementsFromDb(String parentId) {
    List<ExplorerElement> elements = new ArrayList<>();
    String sql;
    if (parentId == null) {
      sql =
          "SELECT id_element, nom_element, type_element, emplacement FROM Element WHERE id_parent"
              + " IS NULL ORDER BY date_creation";
    } else {
      sql =
          "SELECT id_element, nom_element, type_element, emplacement FROM Element WHERE id_parent"
              + " = ? ORDER BY date_creation";
    }

    try (PreparedStatement pstmt = Database.getConnection().prepareStatement(sql)) {
      if (parentId != null) {
        pstmt.setString(1, parentId);
      }
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          elements.add(
              new ExplorerElement(
                  rs.getString("id_element"),
                  rs.getString("nom_element"),
                  rs.getString("type_element"),
                  rs.getString("emplacement")));
        }
      }
    } catch (Exception e) {
      System.err.println("Erreur lors de la récupération des éléments : " + e.getMessage());
    }
    for (ExplorerElement element : elements) {
      System.out.println(element.getName());
    }
    return elements;
  }

  public static void main(String[] args) {
    launch(args);
  }
}
