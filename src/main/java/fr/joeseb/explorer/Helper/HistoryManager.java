package fr.joeseb.explorer;

import java.util.Stack;

public class HistoryManager {
  public interface Command {
    void execute();

    void undo();
  }

  private final Stack<Command> undoStack = new Stack<>();
  private final Stack<Command> redoStack = new Stack<>();
  private final ElementRepository repo;
  private final String currentUser;

  public HistoryManager(ElementRepository repo, String currentUser) {
    this.repo = repo;
    this.currentUser = currentUser;
  }

  public void executeAction(Command command) {
    command.execute();
    undoStack.push(command);
    redoStack.clear();
  }

  public void undo() {
    if (!undoStack.isEmpty()) {
      Command command = undoStack.pop();
      command.undo();
      redoStack.push(command);
      System.out.println("Action annulée (Undo)");
    }
  }

  public void redo() {
    if (!redoStack.isEmpty()) {
      Command command = redoStack.pop();
      command.execute();
      undoStack.push(command);
      System.out.println("Action refaite (Redo)");
    }
  }

  public class CreateCommand implements Command {
    private final String id, name, type, path, parentId;

    public CreateCommand(String id, String name, String type, String path, String parentId) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.path = path;
      this.parentId = parentId;
    }

    @Override
    public void execute() {
      repo.insertElementWithId(id, name, type, path, parentId, currentUser);
      repo.logHistory("Création", id, currentUser);
    }

    @Override
    public void undo() {
      repo.deleteElement(id);
      repo.logHistory("Suppression", id, currentUser);
    }
  }

  public class CreateAppCommand implements Command {
    private final String id, name, appId, execPath, parentId;

    public CreateAppCommand(
        String id, String name, String appId, String execPath, String parentId) {
      this.id = id;
      this.name = name;
      this.appId = appId;
      this.execPath = execPath;
      this.parentId = parentId;
    }

    @Override
    public void execute() {
      // On enregistre d'abord l'application, puis le raccourci
      repo.insertApplication(appId, name, execPath);
      repo.insertAppShortcut(id, name, parentId, appId, currentUser);
      repo.logHistory("Création", id, currentUser);
    }

    @Override
    public void undo() {
      // La suppression en cascade (ON DELETE CASCADE) détruira automatiquement
      // la ligne dans la table Dossier et Element_Tag
      repo.deleteElement(id);
      repo.logHistory("Suppression", id, currentUser);
    }
  }

  public class RenameCommand implements Command {
    private final String id, oldName, oldPath, newName, newPath, type;

    public RenameCommand(
        String id, String oldName, String oldPath, String newName, String newPath, String type) {
      this.id = id;
      this.oldName = oldName;
      this.oldPath = oldPath;
      this.newName = newName;
      this.newPath = newPath;
      this.type = type;
    }

    @Override
    public void execute() {
      if ("Tag".equalsIgnoreCase(type)) {
        repo.updateTag(id, newName); // SQL Spécifique aux Tags
      } else {
        repo.updateElement(id, newName, newPath);
        repo.logHistory("Modification", id, currentUser);
      }
    }

    @Override
    public void undo() {
      if ("Tag".equalsIgnoreCase(type)) {
        repo.updateTag(id, oldName);
      } else {
        repo.updateElement(id, oldName, oldPath);
        repo.logHistory("Modification", id, currentUser);
      }
    }
  }

  public class DeleteCommand implements Command {
    private final String id, name, type, path, parentId;

    public DeleteCommand(ExplorerElement el, String parentId) {
      this.id = el.getId();
      this.name = el.getName();
      this.type = el.getType();
      this.path = el.getPath();
      this.parentId = parentId;
    }

    @Override
    public void execute() {
      if ("Tag".equalsIgnoreCase(type)) {
        repo.deleteTag(id); // SQL Spécifique aux Tags (Détruit le tag et les liens)
      } else {
        repo.deleteElement(id);
        repo.logHistory("Suppression", id, currentUser);
      }
    }

    @Override
    public void undo() {
      if ("Tag".equalsIgnoreCase(type)) {
        repo.insertTag(id, name, "tag_default.png");
      } else {
        repo.insertElementWithId(id, name, type, path, parentId, currentUser);
        repo.logHistory("Création", id, currentUser);
      }
    }
  }
}
