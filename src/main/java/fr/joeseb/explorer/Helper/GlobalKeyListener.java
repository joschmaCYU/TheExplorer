package fr.joeseb.explorer;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import javafx.application.Platform;
import javafx.stage.Stage;

public class GlobalKeyListener implements NativeKeyListener {

  private final Stage primaryStage;

  public GlobalKeyListener(Stage primaryStage) {
    this.primaryStage = primaryStage;
  }

  @Override
  public void nativeKeyPressed(NativeKeyEvent e) {
    // Détecte la touche Windows Gauche ou Windows Droite
    if (e.getKeyCode() == NativeKeyEvent.VC_SPACE
        && (e.getModifiers() & NativeKeyEvent.META_MASK) != 0) {

      // JNativeHook tourne dans un thread séparé, on doit utiliser Platform.runLater
      // pour modifier l'interface JavaFX sans créer de crash.
      Platform.runLater(
          () -> {
            if (primaryStage.isShowing()) {
              primaryStage.hide();
            } else {
              primaryStage.show();
              primaryStage.requestFocus();
            }
          });
    }
  }
}
