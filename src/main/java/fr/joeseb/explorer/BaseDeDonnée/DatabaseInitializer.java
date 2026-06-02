package fr.joeseb.explorer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

  public static void initialize() {
    Connection conn = Database.getConnection();
    if (conn == null) return;

    try {
      // Exécuter le schéma SQL (création sécurisée avec IF NOT EXISTS)
      executeSqlScript(conn, "schema.sql");
      System.out.println("Vérification et application du schéma SQL réussies.");

      // Note : L'utilisateur et les tags sont maintenant gérés de façon dynamique
      // par ElementRepository lors du lancement et de la modification !

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
        if (!line.trim().startsWith("--") && !line.trim().isEmpty()) {
          sql.append(line).append("\n");
        }
      }
    }
    try (Statement stmt = conn.createStatement()) {
      stmt.execute(sql.toString());
    }
  }
}
