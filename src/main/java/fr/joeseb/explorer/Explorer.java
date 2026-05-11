package fr.joeseb.explorer;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import java.util.Arrays;
import java.util.List;
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

  @Override
  public void start(Stage primaryStage) {
    // 1. Initialisation silencieuse de la base de données
    Database.getConnection();

    // 2. Création de la liste des dossiers (Exemple pour 8 directions)
    List<String> mesDossiers =
        Arrays.asList(
            "Images",
            "Vidéos",
            "Projets",
            "Documents",
            "Musique",
            "Téléchargements",
            "Bureau",
            "Corbeille",
            "Test1",
            "Test2",
            "Test3");

    // 3. Création du menu radial
    menu = new RadialMenu(mesDossiers);

    // 4. Configuration de l'interface (Transparence totale)
    StackPane root = new StackPane(menu);
    root.setStyle("-fx-background-color: transparent;");

    Scene scene = new Scene(root, 400, 400);
    scene.setFill(Color.TRANSPARENT);

    // --- GESTION DU PAVÉ NUMÉRIQUE ---
    scene.setOnKeyPressed(
        event -> {
          switch (event.getCode()) {
            case DIGIT0:
            case NUMPAD0:
              menu.highlightSlice(-1);
              break; // Retour
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

  private void setupGlobalShortcut(Stage stage) {
    // Désactive les logs inutiles de JNativeHook dans la console
    Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
    logger.setLevel(Level.OFF);

    try {
      GlobalScreen.registerNativeHook();
      GlobalScreen.addNativeKeyListener(new GlobalKeyListener(stage));
    } catch (NativeHookException ex) {
      System.err.println("Impossible d'activer le raccourci global : " + ex.getMessage());
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}
