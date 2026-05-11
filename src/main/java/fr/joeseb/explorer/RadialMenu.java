package fr.joeseb.explorer;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Text;

public class RadialMenu extends Pane {

  private static final double RADIUS = 150;
  private final List<Arc> slices = new ArrayList<>();
  private final List<String> elements;
  private final double anglePerSlice;
  private int currentIndex = -1;

  public RadialMenu(List<String> elements) {
    this.elements = elements;
    int numberOfSlices = elements.size();
    this.anglePerSlice = 360.0 / numberOfSlices;

    // On crée un fond transparent pour capter la souris même en dehors de la roue
    this.setPrefSize(400, 400);

    for (int i = 0; i < numberOfSlices; i++) {
      // Dessin des parts (0 degrés = Est / Droite)
      // Dans JavaFX, les angles sont inversés, donc on utilise des valeurs négatives
      Arc slice = new Arc(200, 200, RADIUS, RADIUS, -i * anglePerSlice, -anglePerSlice);
      slice.setType(ArcType.ROUND);
      slice.setFill(Color.web("#2c3e50", 0.8)); // Couleur par défaut
      slice.setStroke(Color.WHITE);
      slice.setStrokeWidth(2);
      slices.add(slice);

      // Ajout du texte
      Text text = new Text(elements.get(i));
      text.setFill(Color.WHITE);
      double textAngle = Math.toRadians((i * anglePerSlice) + (anglePerSlice / 2));
      text.setX(200 + (RADIUS * 0.6 * Math.cos(textAngle)) - 20);
      text.setY(200 + (RADIUS * 0.6 * Math.sin(textAngle)));

      this.getChildren().addAll(slice, text);
    }

    // --- 1. GESTION DE LA SOURIS (DIRECTIONNELLE) ---
    this.setOnMouseMoved(
        event -> {
          double dx = event.getX() - 200; // 200 est le centre X
          double dy = event.getY() - 200; // 200 est le centre Y

          // "Zone morte" au centre de la roue (si la souris est à moins de 30 pixels du centre)
          if (Math.abs(dx) < 30 && Math.abs(dy) < 30) {
            return;
          }

          // Calcul mathématique de l'angle (de 0 à 360 degrés)
          double angle = Math.toDegrees(Math.atan2(dy, dx));
          if (angle < 0) {
            angle += 360;
          }

          // On déduit quelle case correspond à cet angle
          int targetIndex = (int) (angle / anglePerSlice);
          highlightSlice(targetIndex);
        });

    // Validation au clic (peu importe où se trouve la souris sur l'écran)
    this.setOnMouseClicked(event -> executeAction());
  }

  // Met en surbrillance visuelle une case spécifique
  public void highlightSlice(int index) {
    if (index == currentIndex || index < 0 || index >= slices.size()) return;

    // Réinitialise l'ancienne case
    if (currentIndex != -1) {
      slices.get(currentIndex).setFill(Color.web("#2c3e50", 0.8));
    }

    // Allume la nouvelle case
    slices.get(index).setFill(Color.web("#3498db", 0.9));
    currentIndex = index;
  }

  // Exécute l'action de la case actuellement sélectionnée
  public void executeAction() {
    if (currentIndex != -1) {
      System.out.println("📁 Ouverture de : " + elements.get(currentIndex));
      // C'est ici que vous ferez votre requête SQL SELECT
    }
  }
}
