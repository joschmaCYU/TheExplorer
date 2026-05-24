package fr.joeseb.explorer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

  public DialogHelper(ElementRepository repository) {
    this.repository = repository;
  }

  public void showDeleteConfirmation(Stage owner, ExplorerElement el, Runnable onConfirm) {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.initOwner(owner);
    alert.setTitle("Confirmation de suppression");
    alert.setHeaderText("Supprimer : " + el.getName());
    alert.setContentText(
        "Êtes-vous sûr de vouloir supprimer cet élément ? Cette action est irréversible.");

    // On attend la réponse de l'utilisateur
    alert
        .showAndWait()
        .ifPresent(
            response -> {
              if (response == ButtonType.OK) {
                onConfirm.run(); // Exécute la suppression si on clique sur OK
              }
            });
  }

  public void showAddDialog(Stage owner, String currentDirectoryId, Runnable onComplete) {
    List<String> choices = new ArrayList<>();
    choices.add("Dossier");
    choices.add("Fichier");
    choices.add("Texte");

    ChoiceDialog<String> dialog = new ChoiceDialog<>("Fichier", choices);
    dialog.initOwner(owner);
    dialog.setTitle("Ajouter un element");
    dialog.setHeaderText("Que voulez-vous ajouter ?");
    dialog
        .showAndWait()
        .ifPresent(
            choice -> {
              if ("Dossier".equals(choice)) {
                TextInputDialog textDialog = new TextInputDialog("Nouveau Dossier");
                textDialog.initOwner(owner);
                textDialog.setHeaderText("Nom du dossier :");
                textDialog
                    .showAndWait()
                    .ifPresent(
                        name -> {
                          if (!name.trim().isEmpty()) {
                            repository.insertElement(
                                name.trim(),
                                "Dossier",
                                "/virtual/" + name.trim(),
                                currentDirectoryId);
                          }
                        });

              } else if ("Fichier".equals(choice)) {
                FileChooser fileChooser = new FileChooser();
                File file = fileChooser.showOpenDialog(owner);
                if (file != null) {
                  repository.insertElement(
                      file.getName(), "Fichier", file.getAbsolutePath(), currentDirectoryId);
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
                              repository.insertElement(title, "Texte", content, currentDirectoryId);
                            }
                          }
                        });
              }
              onComplete.run(); // Rafraîchit le menu après l'action
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

    grid.add(new Label("Nom :"), 0, 0);
    grid.add(nameField, 1, 0);

    if ("Texte".equals(el.getType())) {
      grid.add(new Label("Contenu :"), 0, 1);
    } else {
      grid.add(new Label("Emplacement :"), 0, 1);
    }
    grid.add(pathField, 1, 1);

    dialog.getDialogPane().setContent(grid);

    dialog
        .showAndWait()
        .ifPresent(
            result -> {
              if (result == saveBtn) {
                String newName = nameField.getText().trim();
                String newPath = pathField.getText().trim();

                if (newName.isEmpty() || ("Texte".equals(el.getType()) && newPath.isEmpty())) {
                  return;
                }

                File targetFile = new File(el.getId());
                if (targetFile.exists() && targetFile.isAbsolute()) {
                  File newFile = new File(targetFile.getParent(), newName);
                  targetFile.renameTo(newFile);
                } else {
                  repository.updateElement(el.getId(), newName, newPath);
                }
                onComplete.run();
              }
            });
  }
}
