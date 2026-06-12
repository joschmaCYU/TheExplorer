package fr.joeseb.explorer;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ElementRepository {

  public String syncSystemUser() {
    String osUser = System.getProperty("user.name");
    try {
      // Création dynamique de l'utilisateur OS dans la base de données
      String sqlUser =
          "INSERT INTO Utilisateur (id_user, nom, prenom, mail) VALUES (?, ?, 'OS_User', ?) ON"
              + " CONFLICT DO NOTHING";
      try (PreparedStatement p = Database.getConnection().prepareStatement(sqlUser)) {
        p.setString(1, osUser);
        p.setString(2, osUser);
        p.setString(3, osUser + "@local");
        p.executeUpdate();
      }
      // Création dynamique d'un groupe pour cet utilisateur
      String groupId = "G_" + osUser;
      String sqlGroup =
          "INSERT INTO Groupe (id_groupe, nom_groupe, date_creation) VALUES (?, ?, CURRENT_DATE) ON"
              + " CONFLICT DO NOTHING";
      try (PreparedStatement p = Database.getConnection().prepareStatement(sqlGroup)) {
        p.setString(1, groupId);
        p.setString(2, "Groupe de " + osUser);
        p.executeUpdate();
      }
      // Liaison de l'utilisateur à son groupe
      String sqlLink =
          "INSERT INTO Membre_Groupe (id_user, id_groupe, role_user, date_rejoint) VALUES (?, ?,"
              + " 'Admin', CURRENT_DATE) ON CONFLICT DO NOTHING";
      try (PreparedStatement p = Database.getConnection().prepareStatement(sqlLink)) {
        p.setString(1, osUser);
        p.setString(2, groupId);
        p.executeUpdate();
      }
    } catch (Exception e) {
      System.err.println("Erreur Sync User : " + e.getMessage());
    }
    return osUser;
  }

  public List<ExplorerElement> fetchElements(String parentId) {
    List<ExplorerElement> elements = new ArrayList<>();
    String sql =
        (parentId == null)
            ? "SELECT e.id_element, e.nom_element, e.type_element, e.emplacement, COALESCE(d.icone,"
                + " f.icone, 'default.png') AS icone FROM Element e LEFT JOIN Dossier d ON"
                + " e.id_element = d.id_element LEFT JOIN Fichier f ON e.id_element ="
                + " f.id_element WHERE e.id_parent IS NULL ORDER BY e.date_creation"
            : "SELECT e.id_element, e.nom_element, e.type_element, e.emplacement, COALESCE(d.icone,"
                + " f.icone, 'default.png') AS icone FROM Element e LEFT JOIN Dossier d ON"
                + " e.id_element = d.id_element LEFT JOIN Fichier f ON e.id_element ="
                + " f.id_element WHERE e.id_parent = ? ORDER BY e.date_creation";

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
                  rawPath,
                  rs.getString("icone")));
        }
      }
    } catch (Exception e) {
      System.err.println("Erreur BD (Lecture) : " + e.getMessage());
    }
    return elements;
  }

  public void insertElementWithId(
      String id, String name, String type, String path, String parentId, String ownerId) {
    String sql =
        "INSERT INTO Element (id_element, nom_element, type_element, emplacement, id_proprietaire,"
            + " id_parent) VALUES (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement pstmt = Database.getConnection().prepareStatement(sql)) {
      pstmt.setString(1, id);
      pstmt.setString(2, name);
      pstmt.setString(3, type);
      pstmt.setString(4, path);
      pstmt.setString(5, ownerId);
      pstmt.setString(6, parentId);
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

  // --- GESTION DES TAGS ---

  public String getElementTagsAsString(String elementId) {
    StringBuilder tags = new StringBuilder();
    String sql =
        "SELECT t.nom_tag FROM Tag t JOIN Element_Tag et ON t.id_tag = et.id_tag WHERE"
            + " et.id_element = ?";
    try (PreparedStatement pt = Database.getConnection().prepareStatement(sql)) {
      pt.setString(1, elementId);
      try (ResultSet rs = pt.executeQuery()) {
        while (rs.next()) {
          if (tags.length() > 0) tags.append(", ");
          tags.append(rs.getString("nom_tag"));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return tags.toString();
  }

  public void updateElementTags(
      String elementId,
      String elementName,
      String elementType,
      String elementPath,
      String tagsString) {
    try {
      // SÉCURITÉ CRITIQUE : On s'assure que l'élément existe dans la base de données
      // S'il s'agit d'un vrai fichier physique, il sera enregistré ici pour respecter la clé
      // étrangère.
      String sqlEnsure =
          "INSERT INTO Element (id_element, nom_element, type_element, emplacement,"
              + " id_proprietaire) VALUES (?, ?, ?, ?, 'U01') ON CONFLICT (id_element) DO NOTHING";
      try (PreparedStatement p = Database.getConnection().prepareStatement(sqlEnsure)) {
        p.setString(1, elementId);
        p.setString(2, elementName);
        p.setString(3, elementType);
        p.setString(4, elementPath);
        p.executeUpdate();
      }

      // On nettoie les anciens tags pour cet élément
      try (PreparedStatement pstmt =
          Database.getConnection()
              .prepareStatement("DELETE FROM Element_Tag WHERE id_element = ?")) {
        pstmt.setString(1, elementId);
        pstmt.executeUpdate();
      }
      if (tagsString == null || tagsString.trim().isEmpty()) return;

      // On traite chaque nouveau tag
      String[] tags = tagsString.split(",");
      for (String tag : tags) {
        String tagName = tag.trim();
        if (tagName.isEmpty()) continue;
        // On génère un ID unique et propre pour le Tag
        String tagId = "T_" + tagName.toUpperCase().replaceAll("\\s+", "_");

        // On s'assure que le tag existe
        String sqlTag =
            "INSERT INTO Tag (id_tag, nom_tag, icone_tag) VALUES (?, ?, 'tag_default.png') ON"
                + " CONFLICT DO NOTHING";
        try (PreparedStatement pt = Database.getConnection().prepareStatement(sqlTag)) {
          pt.setString(1, tagId);
          pt.setString(2, tagName);
          pt.executeUpdate();
        }
        // On lie le tag à l'élément
        String sqlLink =
            "INSERT INTO Element_Tag (id_element, id_tag) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement pt = Database.getConnection().prepareStatement(sqlLink)) {
          pt.setString(1, elementId);
          pt.setString(2, tagId);
          pt.executeUpdate();
        }
      }
    } catch (Exception e) {
      System.err.println("Erreur Tag : " + e.getMessage());
    }
  }

  public void logHistory(String actionType, String elementId, String userId) {
    // Note : le ::enum_action est vital pour forcer le typage dans PostgreSQL
    String sql =
        "INSERT INTO Historique (id_historique, type_action, id_element, id_user) VALUES (?,"
            + " ?::enum_action, ?, ?) ON CONFLICT DO NOTHING";
    try (PreparedStatement pt = Database.getConnection().prepareStatement(sql)) {
      pt.setString(1, UUID.randomUUID().toString().substring(0, 8));
      pt.setString(2, actionType);
      pt.setString(3, elementId);
      pt.setString(4, userId);
      pt.executeUpdate();
    } catch (Exception e) {
      System.err.println("Erreur Historique : " + e.getMessage());
    }
  }

  public List<ExplorerElement> getAllTags() {
    List<ExplorerElement> elements = new ArrayList<>();
    String sql = "SELECT id_tag, nom_tag, icone_tag FROM Tag ORDER BY nom_tag";
    try (PreparedStatement pt = Database.getConnection().prepareStatement(sql);
        ResultSet rs = pt.executeQuery()) {
      while (rs.next()) {
        // On transforme virtuellement le Tag en ExplorerElement pour l'afficher dans la roue
        elements.add(
            new ExplorerElement(
                rs.getString("id_tag"),
                rs.getString("nom_tag"),
                "Tag",
                "",
                rs.getString("icone_tag")));
      }
    } catch (Exception e) {
      System.err.println("Erreur lecture Tags : " + e.getMessage());
    }
    return elements;
  }

  // Récupère tous les éléments (fichiers/dossiers) liés à un tag précis
  public List<ExplorerElement> getElementsByTag(String tagId) {
    List<ExplorerElement> elements = new ArrayList<>();

    String sql =
        "SELECT e.id_element, e.nom_element, e.type_element, e.emplacement, COALESCE(d.icone,"
            + " f.icone, 'default.png') AS icone FROM Element e JOIN Element_Tag et ON e.id_element"
            + " = et.id_element LEFT JOIN Dossier d ON e.id_element = d.id_element LEFT JOIN"
            + " Fichier f ON e.id_element = f.id_element WHERE et.id_tag = ? ORDER BY"
            + " e.nom_element";

    try (PreparedStatement pt = Database.getConnection().prepareStatement(sql)) {
      pt.setString(1, tagId);
      try (ResultSet rs = pt.executeQuery()) {
        while (rs.next()) {
          String rawPath = rs.getString("emplacement");
          if (rawPath != null && rawPath.contains("<HOME>")) {
            rawPath =
                rawPath
                    .replace("<HOME>", System.getProperty("user.home"))
                    .replace("/", java.io.File.separator);
          }

          elements.add(
              new ExplorerElement(
                  rs.getString("id_element"),
                  rs.getString("nom_element"),
                  rs.getString("type_element"),
                  rawPath,
                  rs.getString("icone")));
        }
      }
    } catch (Exception e) {
      System.err.println("Erreur lecture Elements par Tag : " + e.getMessage());
    }
    return elements;
  }

  public void deleteTag(String tagId) {
    try {
      // On supprime le Tag de TOUS les fichiers qui le possèdent
      try (PreparedStatement p =
          Database.getConnection().prepareStatement("DELETE FROM Element_Tag WHERE id_tag = ?")) {
        p.setString(1, tagId);
        p.executeUpdate();
      }
      // On supprime le Tag lui-même
      try (PreparedStatement p =
          Database.getConnection().prepareStatement("DELETE FROM Tag WHERE id_tag = ?")) {
        p.setString(1, tagId);
        p.executeUpdate();
      }
    } catch (Exception e) {
      System.err.println("Erreur suppression Tag : " + e.getMessage());
    }
  }

  public void insertTag(String tagId, String tagName, String icon) {
    String sql =
        "INSERT INTO Tag (id_tag, nom_tag, icone_tag) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";
    try (PreparedStatement pt = Database.getConnection().prepareStatement(sql)) {
      pt.setString(1, tagId);
      pt.setString(2, tagName);
      pt.setString(3, icon);
      pt.executeUpdate();
    } catch (Exception e) {
      System.err.println("Erreur création Tag : " + e.getMessage());
    }
  }

  public void updateTag(String tagId, String newName) {
    String sql = "UPDATE Tag SET nom_tag = ? WHERE id_tag = ?";
    try (PreparedStatement p = Database.getConnection().prepareStatement(sql)) {
      p.setString(1, newName);
      p.setString(2, tagId);
      p.executeUpdate();
    } catch (Exception e) {
      System.err.println("Erreur modification Tag : " + e.getMessage());
    }
  }

  public String getAppExecPath(String elementId) {
    String sql =
        "SELECT a.chemin_exec FROM Dossier d JOIN Application a ON d.id_app = a.id_app WHERE"
            + " d.id_element = ?";
    try (PreparedStatement pt = Database.getConnection().prepareStatement(sql)) {
      pt.setString(1, elementId);
      try (ResultSet rs = pt.executeQuery()) {
        if (rs.next()) {
          return rs.getString("chemin_exec"); // Retourne le chemin (ex: C:\...\code.exe)
        }
      }
    } catch (Exception e) {
      System.err.println("Erreur lecture chemin application : " + e.getMessage());
    }
    return null; // Si ce n'est pas une app, on retourne null
  }

  public void insertApplication(String appId, String name, String execPath) {
    String sql =
        "INSERT INTO Application (id_app, nom_app, chemin_exec, version_app) VALUES (?, ?, ?,"
            + " '1.0') ON CONFLICT DO NOTHING";
    try (PreparedStatement pt = Database.getConnection().prepareStatement(sql)) {
      pt.setString(1, appId);
      pt.setString(2, name);
      pt.setString(3, execPath);
      pt.executeUpdate();
    } catch (Exception e) {
      System.err.println("Erreur création App : " + e.getMessage());
    }
  }

  public void insertAppShortcut(
      String elementId, String name, String parentId, String appId, String ownerId) {
    // On crée l'élément virtuel (type 'Dossier')
    String sql1 =
        "INSERT INTO Element (id_element, nom_element, type_element, emplacement, id_proprietaire,"
            + " id_parent) VALUES (?, ?, 'Dossier', '/app_shortcut', ?, ?)";
    try (PreparedStatement pt = Database.getConnection().prepareStatement(sql1)) {
      pt.setString(1, elementId);
      pt.setString(2, name);
      pt.setString(3, ownerId);
      pt.setString(4, parentId);
      pt.executeUpdate();
    } catch (Exception e) {
      System.err.println("Erreur création raccourci App : " + e.getMessage());
    }

    // On le lie à l'application dans la table Dossier avec une icône par défaut
    String sql2 =
        "INSERT INTO Dossier (id_element, icone, id_app) VALUES (?, 'folder_apps.png', ?)";
    try (PreparedStatement pt = Database.getConnection().prepareStatement(sql2)) {
      pt.setString(1, elementId);
      pt.setString(2, appId);
      pt.executeUpdate();
    } catch (Exception e) {
      System.err.println("Erreur liaison Dossier-App : " + e.getMessage());
    }
  }

  public String getElementDetails(String id) {
    String sql =
        "SELECT e.date_creation, e.date_modification, "
            + "u.nom || ' ' || u.prenom AS proprietaire "
            + "FROM Element e "
            + "JOIN Utilisateur u ON e.id_proprietaire = u.id_user "
            + "WHERE e.id_element = ?";
    StringBuilder sb = new StringBuilder();
    try (PreparedStatement pt = Database.getConnection().prepareStatement(sql)) {
      pt.setString(1, id);
      try (ResultSet rs = pt.executeQuery()) {
        if (rs.next()) {
          sb.append("Date de création : ").append(rs.getDate("date_creation")).append("\n");
          sb.append("Dernière modification : ")
              .append(rs.getDate("date_modification"))
              .append("\n");
          sb.append("Propriétaire : ").append(rs.getString("proprietaire")).append("\n");
        }
      }
    } catch (Exception e) {
      System.err.println("Erreur détails : " + e.getMessage());
    }
    return sb.toString();
  }
}
