package fr.joeseb.explorer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;

public class DatabaseInitializer {

  public static void initialize() {
    Connection conn = Database.getConnection();
    if (conn == null) return;

    try {
      // 1. Exécuter le schéma SQL
      executeSqlScript(conn, "schema2.sql");
      System.out.println("Schéma SQL appliqué avec succès.");

      // 2. Créer un utilisateur par défaut
      String userId = "user-default";
      insertUser(conn, userId, "Utilisateur", "Principal", "admin@explorer.local");

      // 3. Créer les Tags de base
      insertTag(conn, "tag-fav", "Favori", "star");
      insertTag(conn, "tag-work", "Travail", "briefcase");
      insertTag(conn, "tag-perso", "Personnel", "user");

      // 4. Créer les dossiers de base dans la table Element/Dossier
      // Ces éléments serviront de points d'entrée "virtuels" ou réels
      String[] baseFolders = {
        "Home",
        "Favoris",
        "Téléchargements",
        "Documents",
        "Vidéos",
        "Images",
        "Desktop",
        "Corbeille",
        "Récents"
      };
      for (String folder : baseFolders) {
        String folderId = UUID.randomUUID().toString();
        insertBaseElement(conn, folderId, folder, "FICHIER_SYSTEME", userId);
      }

      System.out.println("Données initiales insérées.");

    } catch (Exception e) {
      System.err.println("Erreur lors de l'initialisation : " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void executeSqlScript(Connection conn, String filePath) throws Exception {
    StringBuilder sql = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = br.readLine()) != null) {
        sql.append(line).append("\n");
      }
    }
    try (Statement stmt = conn.createStatement()) {
      stmt.execute(sql.toString());
    }
  }

  private static void insertUser(Connection conn, String id, String nom, String prenom, String mail)
      throws Exception {
    String sql =
        "INSERT INTO Utilisateur (id_user, nom, prenom, mail) VALUES (?, ?, ?, ?) ON CONFLICT DO"
            + " NOTHING";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, id);
      pstmt.setString(2, nom);
      pstmt.setString(3, prenom);
      pstmt.setString(4, mail);
      pstmt.executeUpdate();
    }
  }

  private static void insertTag(Connection conn, String id, String nom, String icone)
      throws Exception {
    String sql =
        "INSERT INTO Tag (id_tag, nom_tag, icone_tag) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, id);
      pstmt.setString(2, nom);
      pstmt.setString(3, icone);
      pstmt.executeUpdate();
    }
  }

  private static void insertBaseElement(
      Connection conn, String id, String nom, String path, String ownerId) throws Exception {
    // On insère dans Element
    String sqlElem =
        "INSERT INTO Element (id_element, nom_element, emplacement, id_proprietaire, type_element)"
            + " VALUES (?, ?, ?, ?, 'DOSSIER')";
    try (PreparedStatement pstmt = conn.prepareStatement(sqlElem)) {
      pstmt.setString(1, id);
      pstmt.setString(2, nom);
      pstmt.setString(3, path);
      pstmt.setString(4, ownerId);
      pstmt.executeUpdate();
    }
    // On insère dans Dossier
    String sqlDossier = "INSERT INTO Dossier (id_element, icone) VALUES (?, ?)";
    try (PreparedStatement pstmt = conn.prepareStatement(sqlDossier)) {
      pstmt.setString(1, id);
      pstmt.setString(2, "folder-icon");
      pstmt.executeUpdate();
    }
  }
}
