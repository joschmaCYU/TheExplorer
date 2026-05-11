package fr.joeseb.explorer;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class RadialMenu extends Pane {

  private static final double RADIUS = 150;
  private static final double CENTER_RADIUS = 50;
  private static final int MAX_ITEMS_PER_PAGE = 9;

  private final List<Shape> slices = new ArrayList<>();
  private Circle centerCircle;
  private final List<String> allElements;
  private int currentPage = 0;
  private double anglePerSlice;
  private int currentIndex = -2; // -2 = Rien, -1 = Centre, 0+ = Slices

  public RadialMenu(List<String> elements) {
    this.allElements = elements;
    this.setPrefSize(400, 400);
    drawMenu();
  }

  private void drawMenu() {
    this.getChildren().clear();
    slices.clear();

    int startIdx = currentPage * (MAX_ITEMS_PER_PAGE - 1);
    List<String> currentElements = new ArrayList<>();

    boolean hasMore = allElements.size() > (startIdx + MAX_ITEMS_PER_PAGE);
    int itemsToShow = hasMore ? MAX_ITEMS_PER_PAGE : Math.min(MAX_ITEMS_PER_PAGE, allElements.size() - startIdx);

    for (int i = 0; i < itemsToShow; i++) {
      if (hasMore && i == MAX_ITEMS_PER_PAGE - 1) {
        currentElements.add("SUIVANT...");
      } else {
        currentElements.add(allElements.get(startIdx + i));
      }
    }

    int numberOfSlices = currentElements.size();
    this.anglePerSlice = 360.0 / numberOfSlices;

    for (int i = 0; i < numberOfSlices; i++) {
      double startAngle = i * anglePerSlice;
      double endAngle = (i + 1) * anglePerSlice;
      
      Shape slice = createDonutSlice(200, 200, CENTER_RADIUS + 5, RADIUS, startAngle, endAngle);
      slice.setFill(Color.web("#2c3e50", 0.8));
      slice.setStroke(Color.WHITE);
      slice.setStrokeWidth(1);
      slices.add(slice);

      // Calcul de la position du texte (au milieu de la part)
      double middleAngle = Math.toRadians(startAngle + anglePerSlice / 2);
      double textRadius = (RADIUS + CENTER_RADIUS) / 2;
      double tx = 200 + textRadius * Math.cos(middleAngle);
      double ty = 200 + textRadius * Math.sin(middleAngle);

      Text text = new Text(currentElements.get(i) + "\n(" + (i + 1) + ")");
      text.setFill(Color.WHITE);
      text.setTextAlignment(TextAlignment.CENTER);
      text.setX(tx - 30);
      text.setY(ty + 5);
      text.setMouseTransparent(true); // Pour que le texte ne gêne pas le survol de la souris

      this.getChildren().addAll(slice, text);
    }

    // --- BOUTON CENTRAL (RETOUR / PRÉCÉDENT) ---
    centerCircle = new Circle(200, 200, CENTER_RADIUS);
    centerCircle.setFill(Color.web("#7f8c8d", 0.8));
    centerCircle.setStroke(Color.WHITE);
    centerCircle.setStrokeWidth(2);

    String centerLabel = (currentPage > 0) ? "PRÉCÉDENT\n(0)" : "RETOUR\n(0)";
    Text backText = new Text(centerLabel);
    backText.setFill(Color.WHITE);
    backText.setTextAlignment(TextAlignment.CENTER);
    backText.setX(200 - 35);
    backText.setY(200 + 5);
    backText.setMouseTransparent(true);

    this.getChildren().addAll(centerCircle, backText);

    setupMouseEvents();
    currentIndex = -2;
  }

  private Shape createDonutSlice(double centerX, double centerY, double innerRadius, double outerRadius, double startAngle, double endAngle) {
    double startRad = Math.toRadians(startAngle);
    double endRad = Math.toRadians(endAngle);

    Path path = new Path();
    
    // Extérieur départ
    double x1 = centerX + outerRadius * Math.cos(startRad);
    double y1 = centerY + outerRadius * Math.sin(startRad);
    
    // Extérieur fin
    double x2 = centerX + outerRadius * Math.cos(endRad);
    double y2 = centerY + outerRadius * Math.sin(endRad);
    
    // Intérieur fin
    double x3 = centerX + innerRadius * Math.cos(endRad);
    double y3 = centerY + innerRadius * Math.sin(endRad);
    
    // Intérieur départ
    double x4 = centerX + innerRadius * Math.cos(startRad);
    double y4 = centerY + innerRadius * Math.sin(startRad);

    path.getElements().add(new MoveTo(x1, y1));
    path.getElements().add(new ArcTo(outerRadius, outerRadius, 0, x2, y2, false, true));
    path.getElements().add(new LineTo(x3, y3));
    path.getElements().add(new ArcTo(innerRadius, innerRadius, 0, x4, y4, false, false));
    path.getElements().add(new ClosePath());

    return path;
  }

  private void setupMouseEvents() {
    this.setOnMouseMoved(event -> {
      double dx = event.getX() - 200;
      double dy = event.getY() - 200;
      double distance = Math.sqrt(dx * dx + dy * dy);

      if (distance < CENTER_RADIUS) {
        highlightSlice(-1);
        return;
      }

      if (distance > RADIUS) {
        highlightSlice(-2);
        return;
      }

      double angle = Math.toDegrees(Math.atan2(dy, dx));
      if (angle < 0) angle += 360;

      int targetIndex = (int) (angle / anglePerSlice);
      highlightSlice(targetIndex);
    });

    this.setOnMouseClicked(event -> executeAction());
  }

  public void highlightSlice(int index) {
    if (index == currentIndex) return;

    // Réinitialisation
    if (currentIndex == -1) {
      centerCircle.setFill(Color.web("#7f8c8d", 0.8));
    } else if (currentIndex >= 0 && currentIndex < slices.size()) {
      slices.get(currentIndex).setFill(Color.web("#2c3e50", 0.8));
    }

    // Activation
    if (index == -1) {
      centerCircle.setFill(Color.web("#c0392b", 0.9));
    } else if (index >= 0 && index < slices.size()) {
      slices.get(index).setFill(Color.web("#3498db", 0.9));
    }
    
    currentIndex = index;
  }

  public void executeAction() {
    if (currentIndex == -1) {
      if (currentPage > 0) {
        currentPage--;
        drawMenu();
      } else {
        System.out.println("Action : Retour au dossier parent");
      }
    } else if (currentIndex >= 0 && currentIndex < slices.size()) {
      int startIdx = currentPage * (MAX_ITEMS_PER_PAGE - 1);
      boolean hasMore = allElements.size() > (startIdx + MAX_ITEMS_PER_PAGE);

      if (hasMore && currentIndex == MAX_ITEMS_PER_PAGE - 1) {
        currentPage++;
        drawMenu();
      } else {
        int actualIndex = startIdx + currentIndex;
        System.out.println("Ouverture de : " + allElements.get(actualIndex));
      }
    }
  }
}
