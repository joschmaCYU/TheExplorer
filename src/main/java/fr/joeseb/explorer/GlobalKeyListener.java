package fr.joeseb.explorer;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import javafx.application.Platform;
import javafx.stage.Stage;

public class GlobalKeyListener implements NativeKeyListener {

  private final Stage stage;

  public GlobalKeyListener(Stage stage) {
    this.stage = stage;
  }

  @Override
  public void nativeKeyPressed(NativeKeyEvent e) {
    // Détection de la touche "Espace" (Code 57)
    // Vous pouvez combiner avec NativeKeyEvent.VC_ALT etc.
    if (e.getKeyCode() == NativeKeyEvent.VC_SPACE) {

      // JavaFX exige que les modifications de fenêtre soient sur son "Thread"
      Platform.runLater(
          () -> {
            if (stage.isShowing()) {
              stage.hide();
              System.out.println("Roue cachée.");
            } else {
              // Optionnel : Placer la fenêtre sous la souris de l'utilisateur
              stage.show();
              stage.toFront();
              System.out.println("Roue affichée !");
            }
          });
    }
  }
}
