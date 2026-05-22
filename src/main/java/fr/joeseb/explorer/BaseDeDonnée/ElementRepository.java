package fr.joeseb.explorer;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ElementRepository {

  public List<ExplorerElement> fetchElements(String parentId) {
    List<ExplorerElement> elements = new ArrayList<>();
    String sql =
        (parentId == null)
            ? "SELECT id_element, nom_element, type_element, emplacement FROM Element WHERE"
                  + " id_parent IS NULL ORDER BY date_creation"
            : "SELECT id_element, nom_element, type_element, emplacement FROM Element WHERE"
                  + " id_parent = ? ORDER BY date_creation";

    try (PreparedStatement pstmt = Database.getConnection().prepareStatement(sql)) {
      if (parentId != null) pstmt.setString(1, parentId);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          String rawPath = rs.getString("emplacement");
          if (rawPath != null && rawPath.contains("<HOME>")) {
            rawPath =
                rawPath
                    .replace("<HOME>", System.getProperty("user.home"))
                    .replace("/", File.separator);
          }
          elements.add(
              new ExplorerElement(
                  rs.getString("id_element"),
                  rs.getString("nom_element"),
                  rs.getString("type_element"),
                  rawPath));
        }
      }
    } catch (Exception e) {
      System.err.println("Erreur BD (Lecture) : " + e.getMessage());
    }
    return elements;
  }

  public void insertElement(String name, String type, String path, String parentId) {
    String id = UUID.randomUUID().toString().substring(0, 8);
    String sql =
        "INSERT INTO Element (id_element, nom_element, type_element, emplacement, id_proprietaire,"
            + " id_parent) VALUES (?, ?, ?, ?, 'U01', ?)";
    try (PreparedStatement pstmt = Database.getConnection().prepareStatement(sql)) {
      pstmt.setString(1, id);
      pstmt.setString(2, name);
      pstmt.setString(3, type);
      pstmt.setString(4, path);
      pstmt.setString(5, parentId);
      pstmt.executeUpdate();
    } catch (Exception e) {
      System.err.println("Erreur BD (Insertion) : " + e.getMessage());
    }
  }

  public void updateElement(String id, String newName, String newPath) {
    String sql = "UPDATE Element SET nom_element = ?, emplacement = ? WHERE id_element = ?";
    try (PreparedStatement pstmt = Database.getConnection().prepareStatement(sql)) {
      pstmt.setString(1, newName);
      pstmt.setString(2, newPath);
      pstmt.setString(3, id);
      pstmt.executeUpdate();
    } catch (Exception e) {
      System.err.println("Erreur BD (Mise à jour) : " + e.getMessage());
    }
  }

  public void deleteElement(String id) {
    String sql = "DELETE FROM Element WHERE id_element = ?";
    try (PreparedStatement pstmt = Database.getConnection().prepareStatement(sql)) {
      pstmt.setString(1, id);
      pstmt.executeUpdate();
    } catch (Exception e) {
      System.err.println("Erreur BD (Suppression) : " + e.getMessage());
    }
  }
}
