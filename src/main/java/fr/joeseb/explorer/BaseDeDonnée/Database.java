package fr.joeseb.explorer;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class Database {

  private static Connection connection;

  public static Connection getConnection() {
    if (connection == null) {
      try {
        // On charge le fichier de configuration
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("db.properties")) {
          props.load(input);
        }

        // On récupère les valeurs
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        // On se connecte
        connection = DriverManager.getConnection(url, user, password);
        System.out.println("Connexion à la base de données réussie !");

      } catch (Exception e) {
        System.err.println("Erreur de connexion : " + e.getMessage());
      }
    }
    return connection;
  }
}
