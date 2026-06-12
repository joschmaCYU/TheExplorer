package fr.joeseb.explorer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class DialogHelper {

  private final ElementRepository repository;
  private final HistoryManager historyManager;

  public DialogHelper(ElementRepository repository, HistoryManager historyManager) {
    this.repository = repository;
    this.historyManager = historyManager;
  }

  public void showAddDialog(Stage owner, String currentDirectoryId, Runnable onComplete) {
    List<String> choices = new ArrayList<>();
    choices.add("Dossier");
    choices.add("Fichier");
    choices.add("Texte");
    choices.add("Application");

    ChoiceDialog<String> dialog = new ChoiceDialog<>("Fichier", choices);
    dialog.initOwner(owner);
    dialog.setTitle("Ajouter un element");
    dialog.setHeaderText("Que voulez-vous ajouter ?");
    dialog
        .showAndWait()
        .ifPresent(
            choice -> {
              String newId = UUID.randomUUID().toString().substring(0, 8);

              if ("Dossier".equals(choice)) {
                TextInputDialog textDialog = new TextInputDialog("Nouveau Dossier");
                textDialog.initOwner(owner);
                textDialog.setHeaderText("Nom du dossier :");
                textDialog
                    .showAndWait()
                    .ifPresent(
                        name -> {
                          if (!name.trim().isEmpty()) {
                            historyManager.executeAction(
                                historyManager
                                .new CreateCommand(
                                    newId,
                                    name.trim(),
                                    "Dossier",
                                    "/virtual/" + name.trim(),
                                    currentDirectoryId));
                          }
                        });

              } else if ("Fichier".equals(choice)) {
                FileChooser fileChooser = new FileChooser();
                File file = fileChooser.showOpenDialog(owner);
                if (file != null) {
                  historyManager.executeAction(
                      historyManager
                      .new CreateCommand(
                          newId,
                          file.getName(),
                          "Fichier",
                          file.getAbsolutePath(),
                          currentDirectoryId));
                }

              } else if ("Texte".equals(choice)) {
                Dialog<ButtonType> textDialog = new Dialog<>();
                textDialog.initOwner(owner);
                textDialog.setTitle("Nouveau Texte");
                textDialog.setHeaderText("Ajouter un extrait de texte");

                ButtonType saveBtn = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
                textDialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.setPadding(new Insets(20, 150, 10, 10));

                TextField nameField = new TextField("Nouveau texte");
                TextField contentField = new TextField();

                grid.add(new Label("Titre :"), 0, 0);
                grid.add(nameField, 1, 0);
                grid.add(new Label("Texte :"), 0, 1);
                grid.add(contentField, 1, 1);

                textDialog.getDialogPane().setContent(grid);
                Platform.runLater(nameField::requestFocus);

                textDialog
                    .showAndWait()
                    .ifPresent(
                        res -> {
                          if (res == saveBtn) {
                            String title = nameField.getText().trim();
                            String content = contentField.getText().trim();
                            if (!title.isEmpty() && !content.isEmpty()) {
                              historyManager.executeAction(
                                  historyManager
                                  .new CreateCommand(
                                      newId, title, "Texte", content, currentDirectoryId));
                            }
                          }
                        });
              } else if ("Application".equals(choice)) {
                // On demande à l'utilisateur de pointer vers l'exécutable (.exe, .sh,
                // binaire...)
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Sélectionner l'exécutable de l'application");
                File file = fileChooser.showOpenDialog(owner);

                if (file != null) {
                  // On lui demande comment il veut appeler ce raccourci (on pré-remplit avec le
                  // nom du fichier)
                  String defaultName = file.getName().replace(".exe", "");
                  TextInputDialog nameDialog = new TextInputDialog(defaultName);
                  nameDialog.initOwner(owner);
                  nameDialog.setHeaderText("Nom de l'application :");

                  nameDialog
                      .showAndWait()
                      .ifPresent(
                          name -> {
                            if (!name.trim().isEmpty()) {
                              // On génère un ID d'application et on l'envoie à l'historique
                              String appId =
                                  "APP_"
                                      + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
                              historyManager.executeAction(
                                  historyManager
                                  .new CreateAppCommand(
                                      newId,
                                      name.trim(),
                                      appId,
                                      file.getAbsolutePath(),
                                      currentDirectoryId));
                            }
                          });
                }
              }
              onComplete.run();
            });
  }

  public void showEditDialog(Stage owner, ExplorerElement el, Runnable onComplete) {
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle("Modifier l'element");
    dialog.setHeaderText("Modification de : " + el.getName());

    ButtonType saveBtn = new ButtonType("Sauvegarder", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));

    TextField nameField = new TextField(el.getName());
    TextField pathField = new TextField(el.getPath());

    // Récupération dynamique des tags existants
    TextField tagsField = new TextField(repository.getElementTagsAsString(el.getId()));

    grid.add(new Label("Nom :"), 0, 0);
    grid.add(nameField, 1, 0);

    if ("Texte".equals(el.getType())) {
      grid.add(new Label("Contenu :"), 0, 1);
    } else {
      grid.add(new Label("Emplacement :"), 0, 1);
    }
    grid.add(pathField, 1, 1);

    grid.add(new Label("Tags (séparés par ,) :"), 0, 2);
    grid.add(tagsField, 1, 2);

    dialog.getDialogPane().setContent(grid);

    dialog
        .showAndWait()
        .ifPresent(
            result -> {
              if (result == saveBtn) {
                String newName = nameField.getText().trim();
                String newPath = pathField.getText().trim();

                if (newName.isEmpty() || ("Texte".equals(el.getType()) && newPath.isEmpty()))
                  return;

                File targetFile = new File(el.getId());
                if (targetFile.exists() && targetFile.isAbsolute()) {
                  File newFile = new File(targetFile.getParent(), newName);
                  targetFile.renameTo(newFile);
                } else {
                  // On passe par l'historique pour le renommage en DB
                  historyManager.executeAction(
                      historyManager
                      .new RenameCommand(
                          el.getId(), el.getName(), el.getPath(), newName, newPath, el.getType()));
                }

                // Mise à jour des Tags
                repository.updateElementTags(
                    el.getId(), el.getName(), el.getType(), el.getPath(), tagsField.getText());

                onComplete.run();
              }
            });
  }

  public void showDeleteConfirmation(Stage owner, ExplorerElement el, Runnable onConfirm) {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.initOwner(owner);
    alert.setTitle("Confirmation");
    alert.setHeaderText("Supprimer : " + el.getName());
    alert.setContentText("Êtes-vous sûr de vouloir supprimer cet élément ?");

    alert
        .showAndWait()
        .ifPresent(
            response -> {
              if (response == ButtonType.OK) onConfirm.run();
            });
  }

  public void showProperties(Stage owner, ExplorerElement el) {
    Alert alert = new Alert(AlertType.INFORMATION);
    alert.initOwner(owner);
    alert.setTitle("Propriétés");
    alert.setHeaderText(el.getName() + " (" + el.getType() + ")");

    StringBuilder details = new StringBuilder();
    details.append("Emplacement : ").append(el.getPath()).append("\n\n");

    File f = new File(el.getPath());
    if (f.exists() && f.isAbsolute()) {
      details.append("--- Fichier Système ---\n");
      long sizeKo = f.length() / 1024;
      details.append("Taille : ").append(sizeKo > 0 ? sizeKo : 1).append(" Ko\n");
      details.append("Lecture autorisée : ").append(f.canRead() ? "Oui" : "Non").append("\n");
      details.append("Écriture autorisée : ").append(f.canWrite() ? "Oui" : "Non").append("\n");
      details.append("Exécution autorisée : ").append(f.canExecute() ? "Oui" : "Non").append("\n");
      details.append("Modifié le : ").append(new java.util.Date(f.lastModified())).append("\n");
    } else {
      details.append("--- Élément Virtuel ---\n");
      details.append(repository.getElementDetails(el.getId()));
      String tags = repository.getElementTagsAsString(el.getId());
      if (!tags.isEmpty()) {
        details.append("Tags : ").append(tags).append("\n");
      }
    }

    alert.setContentText(details.toString());
    alert.showAndWait();
  }
}
