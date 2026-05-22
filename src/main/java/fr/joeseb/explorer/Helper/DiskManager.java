package fr.joeseb.explorer;

import java.io.File;
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
          elements.add(
              new ExplorerElement(
                  file.getAbsolutePath(), file.getName(), type, file.getAbsolutePath()));
        }
      }
    }
    return elements;
  }

  public void openFile(ExplorerElement element) {
    try {
      File file = new File(element.getPath());
      if (file.exists()) {
        hostServices.showDocument(file.toURI().toString());
      } else {
        hostServices.showDocument(element.getPath());
      }
    } catch (Exception e) {
      System.err.println("Erreur lors de l'ouverture : " + e.getMessage());
    }
  }
}
