package fr.joeseb.explorer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
  private static final String URL = "jdbc:postgresql://localhost:5432/votre_base";
  private static final String USER = "postgres";
  private static final String PASS = "votre_mot_de_passe";

  private static Connection connection = null;

  public static Connection getConnection() {
    if (connection == null) {
      try {
        connection = DriverManager.getConnection(URL, USER, PASS);
        System.out.println("Connexion à la base de données réussie !");
      } catch (SQLException e) {
        System.err.println("Erreur de connexion à la base de données !");
        e.printStackTrace();
      }
    }
    return connection;
  }
}
