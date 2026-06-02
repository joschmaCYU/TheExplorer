package fr.joeseb.explorer;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javafx.application.HostServices;

public class DiskManager {

  private final HostServices hostServices;

  public DiskManager(HostServices hostServices) {
    this.hostServices = hostServices;
  }

  public List<ExplorerElement> fetchElementsFromDisk(File folder) {
    List<ExplorerElement> elements = new ArrayList<>();
    File[] files = folder.listFiles();
    if (files != null) {
      for (File file : files) {
        if (!file.isHidden()) {
          String type = file.isDirectory() ? "Dossier" : "Fichier";
          String icon = file.isDirectory() ? "default_folder.png" : "default_file.png";
          elements.add(
              new ExplorerElement(
                  file.getAbsolutePath(), file.getName(), type, file.getAbsolutePath(), icon));
        }
      }
    }
    return elements;
  }

  public void openFile(ExplorerElement element) {
    // On lance l'ouverture dans un Thread totalement séparé pour protéger la roue JavaFX
    Thread openThread =
        new Thread(
            () -> {
              try {
                String path = element.getPath();
                File file = new File(path);
                boolean exists = file.exists();

                String target = exists ? file.getAbsolutePath() : path;
                String os = System.getProperty("os.name").toLowerCase();

                if (os.contains("nix")
                    || os.contains("nux")
                    || os.contains("aix")
                    || os.contains("mac")) {
                  String command = os.contains("mac") ? "open" : "xdg-open";
                  ProcessBuilder pb = new ProcessBuilder(command, target);

                  // CORRECTIF CRITIQUE : On détache totalement les flux du processus enfant
                  // pour qu'il ne fasse pas crasher la machine virtuelle Java
                  pb.redirectErrorStream(true);
                  pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);

                  Process process = pb.start();
                } else {
                  if (Desktop.isDesktopSupported()) {
                    if (exists) Desktop.getDesktop().open(file);
                    else Desktop.getDesktop().browse(new URI(target));
                  } else {
                    System.out.println(
                        "[DEBUG] 5b. Desktop non supporté, fallback sur HostServices.");
                    hostServices.showDocument(exists ? file.toURI().toString() : target);
                  }
                }
              } catch (Exception e) {
                System.err.println(
                    "[DEBUG ERROR] Une exception Java a été interceptée : " + e.getMessage());
                e.printStackTrace();
              }
            });

    // On indique à Java que ce thread ne doit pas empêcher l'application de se fermer
    openThread.setDaemon(true);
    openThread.start();
  }

  public void launchApp(String execPath) {
    System.out.println("[DEBUG] Lancement de l'application : " + execPath);
    new Thread(
            () -> {
              try {
                // On lance l'exécutable directement
                ProcessBuilder pb = new ProcessBuilder(execPath);

                // On détache les flux pour ne pas figer l'explorateur Java
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.start();

              } catch (Exception e) {
                System.err.println(
                    "[DEBUG ERROR] Impossible de lancer l'application : " + e.getMessage());
              }
            })
        .start();
  }
}
