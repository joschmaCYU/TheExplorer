package fr.joeseb.explorer;

public class ExplorerElement {
  private final String id;
  private final String name;
  private final String type;

  private final String path;

  public ExplorerElement(String id, String name, String type, String path) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.path = path;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getPath() {
    return path;
  }

  @Override
  public String toString() {
    return name;
  }
}
