package fr.joeseb.explorer;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class RadialMenu extends Pane {

  private static final double RADIUS = 150;
  // Légère augmentation du centre pour laisser la place au texte sur 2 lignes
  private static final double CENTER_RADIUS = 55;
  private static final int MAX_ITEMS_PER_PAGE = 8;

  private final List<Shape> slices = new ArrayList<>();
  private Circle centerCircle;
  private Shape nextButton;
  private final List<String> allElements;
  private int currentPage = 0;
  private double anglePerSlice;
  private int currentIndex = -2; // -2: Rien, -1: Retour, -3: Suivant, 0+: Slices

  public RadialMenu(List<String> elements) {
    this.allElements = elements;
    this.setPrefSize(400, 400);
    drawMenu();
  }

  private void drawMenu() {
    this.getChildren().clear();
    slices.clear();

    int startIdx = currentPage * MAX_ITEMS_PER_PAGE;
    List<String> currentElements = new ArrayList<>();
    boolean hasMore = allElements.size() > (startIdx + MAX_ITEMS_PER_PAGE);
    int itemsToShow = Math.min(MAX_ITEMS_PER_PAGE, allElements.size() - startIdx);

    for (int i = 0; i < itemsToShow; i++) {
      currentElements.add(allElements.get(startIdx + i));
    }

    int numberOfSlices = currentElements.size();
    this.anglePerSlice = 360.0 / numberOfSlices;

    for (int i = 0; i < numberOfSlices; i++) {
      double startAngle = i * anglePerSlice;
      double endAngle = (i + 1) * anglePerSlice;

      // L'écart entre le centre et les parts est géré ici (CENTER_RADIUS + 30)
      Shape slice = createDonutSlice(200, 200, CENTER_RADIUS + 30, RADIUS, startAngle, endAngle);
      slice.setFill(Color.web("#2c3e50", 0.8));
      slice.setStroke(Color.WHITE);
      slice.setStrokeWidth(1);
      slices.add(slice);
      this.getChildren().add(slice);

      // --- CENTRAGE DU TEXTE DES PARTS ---
      Text text = new Text(currentElements.get(i) + "\n(" + (i + 1) + ")");
      text.setFill(Color.WHITE);
      text.setTextAlignment(TextAlignment.CENTER);
      text.setTextOrigin(
          VPos.CENTER); // <-- FIX: Le point (X,Y) est maintenant le vrai centre du texte
      text.setMouseTransparent(true);
      this.getChildren().add(text);

      double middleAngle = Math.toRadians(startAngle + anglePerSlice / 2);
      double textRadius = (RADIUS + (CENTER_RADIUS + 30)) / 2; // Milieu de la tranche

      double tx =
          200 + textRadius * Math.cos(middleAngle) - (text.getLayoutBounds().getWidth() / 2);
      double ty = 200 + textRadius * Math.sin(middleAngle);

      text.setX(tx);
      text.setY(ty);
    }

    // --- BOUTON CENTRAL (RETOUR / PRÉCÉDENT - TOUCHE 0) ---
    centerCircle = new Circle(200, 200, CENTER_RADIUS);
    centerCircle.setFill(Color.web("#7f8c8d", 0.8));
    centerCircle.setStroke(Color.WHITE);
    centerCircle.setStrokeWidth(2);
    this.getChildren().add(centerCircle);

    String centerLabel = (currentPage > 0) ? "PRÉCÉD.\n(0)" : "RETOUR\n(0)";
    Text backText = new Text(centerLabel);
    backText.setFill(Color.WHITE);
    backText.setTextAlignment(TextAlignment.CENTER);
    backText.setTextOrigin(VPos.CENTER); // <-- FIX
    backText.setFont(Font.font(11)); // Police un poil plus petite pour que ça rentre parfaitement
    backText.setMouseTransparent(true);
    this.getChildren().add(backText);

    double bBackW = backText.getLayoutBounds().getWidth();
    backText.setX(200 - (bBackW / 2));

    // Si y a le bouton "Suivant", on remonte le texte "Retour" dans la moitié haute
    if (hasMore) {
      backText.setY(200 - (CENTER_RADIUS / 2) + 2); // +2 pour ajustement optique
    } else {
      backText.setY(200); // S'il est tout seul, il est parfaitement au milieu
    }

    // --- BOUTON SUIVANT (DEMI-CERCLE - TOUCHE 9) ---
    if (hasMore) {
      nextButton = createSemiCircle(200, 200, CENTER_RADIUS);
      nextButton.setFill(Color.web("#27ae60", 0.9));
      nextButton.setStroke(Color.WHITE);
      nextButton.setStrokeWidth(1);
      this.getChildren().add(nextButton);

      Text nextTxt = new Text("SUIV.\n(9)");
      nextTxt.setFill(Color.WHITE);
      nextTxt.setTextAlignment(TextAlignment.CENTER);
      nextTxt.setTextOrigin(VPos.CENTER); // <-- FIX
      nextTxt.setFont(Font.font(11));
      nextTxt.setMouseTransparent(true);
      this.getChildren().add(nextTxt);

      double bNextW = nextTxt.getLayoutBounds().getWidth();
      nextTxt.setX(200 - (bNextW / 2));
      nextTxt.setY(200 + (CENTER_RADIUS / 2) - 2); // Placé dans la moitié basse
    } else {
      nextButton = null;
    }

    setupMouseEvents();
    currentIndex = -2;
  }

  private Shape createDonutSlice(
      double centerX,
      double centerY,
      double innerRadius,
      double outerRadius,
      double startAngle,
      double endAngle) {
    double startRad = Math.toRadians(startAngle);
    double endRad = Math.toRadians(endAngle);
    Path path = new Path();
    path.getElements()
        .add(
            new MoveTo(
                centerX + outerRadius * Math.cos(startRad),
                centerY + outerRadius * Math.sin(startRad)));
    path.getElements()
        .add(
            new ArcTo(
                outerRadius,
                outerRadius,
                0,
                centerX + outerRadius * Math.cos(endRad),
                centerY + outerRadius * Math.sin(endRad),
                false,
                true));
    path.getElements()
        .add(
            new LineTo(
                centerX + innerRadius * Math.cos(endRad),
                centerY + innerRadius * Math.sin(endRad)));
    path.getElements()
        .add(
            new ArcTo(
                innerRadius,
                innerRadius,
                0,
                centerX + innerRadius * Math.cos(startRad),
                centerY + innerRadius * Math.sin(startRad),
                false,
                false));
    path.getElements().add(new ClosePath());
    return path;
  }

  private Shape createSemiCircle(double centerX, double centerY, double radius) {
    Path path = new Path();
    path.getElements().add(new MoveTo(centerX - radius, centerY));
    path.getElements().add(new LineTo(centerX + radius, centerY));
    path.getElements().add(new ArcTo(radius, radius, 0, centerX - radius, centerY, false, true));
    path.getElements().add(new ClosePath());
    return path;
  }

  private void setupMouseEvents() {
    this.setOnMouseMoved(
        event -> {
          double dx = event.getX() - 200;
          double dy = event.getY() - 200;
          double distance = Math.sqrt(dx * dx + dy * dy);

          if (distance < CENTER_RADIUS) {
            if (nextButton != null && dy > 0) {
              highlightSlice(-3); // Suivant
            } else {
              highlightSlice(-1); // Retour
            }
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

    // Reset
    if (currentIndex == -1) centerCircle.setFill(Color.web("#7f8c8d", 0.8));
    else if (currentIndex == -3 && nextButton != null)
      nextButton.setFill(Color.web("#27ae60", 0.9));
    else if (currentIndex >= 0 && currentIndex < slices.size())
      slices.get(currentIndex).setFill(Color.web("#2c3e50", 0.8));

    // Active
    if (index == -1) centerCircle.setFill(Color.web("#c0392b", 0.9));
    else if (index == -3 && nextButton != null) nextButton.setFill(Color.web("#2ecc71", 1.0));
    else if (index >= 0 && index < slices.size())
      slices.get(index).setFill(Color.web("#3498db", 0.9));

    currentIndex = index;
  }

  public void executeAction() {
    if (currentIndex == -1) {
      if (currentPage > 0) {
        currentPage--;
        drawMenu();
      } else {
        System.out.println("Action : Retour au niveau parent");
      }
    } else if (currentIndex == -3) {
      currentPage++;
      drawMenu();
    } else if (currentIndex >= 0 && currentIndex < slices.size()) {
      int actualIndex = (currentPage * MAX_ITEMS_PER_PAGE) + currentIndex;
      System.out.println("Ouverture de : " + allElements.get(actualIndex));
    }
  }
}
