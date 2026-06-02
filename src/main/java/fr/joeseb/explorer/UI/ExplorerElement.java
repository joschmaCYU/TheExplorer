package fr.joeseb.explorer;

public class ExplorerElement {
  private String id;
  private String name;
  private String type;
  private String path;
  private String icon;

  public ExplorerElement(String id, String name, String type, String path, String icon) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.path = path;
    this.icon = icon;
  }

  // Getters
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

  public String getIcon() {
    return icon;
  }
}
