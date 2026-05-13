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

  public interface OnElementSelectedListener {
    void onElementSelected(ExplorerElement element);

    void onBackSelected();
  }

  private static final double RADIUS = 150;
  private static final double CENTER_RADIUS = 55;
  private static final int MAX_ITEMS_PER_PAGE = 8;

  private final List<Shape> slices = new ArrayList<>();
  private Circle centerCircle;
  private Shape nextButton;
  private List<ExplorerElement> allElements;
  private int currentPage = 0;
  private double anglePerSlice;
  private int currentIndex = -2;
  private OnElementSelectedListener listener;

  private boolean canGoBack = false;

  public RadialMenu(List<ExplorerElement> elements, boolean canGoBack) {
    this.allElements = elements;
    this.canGoBack = canGoBack;
    this.setPrefSize(400, 400);
    drawMenu();
  }

  public void setOnElementSelectedListener(OnElementSelectedListener listener) {
    this.listener = listener;
  }

  public void setElements(List<ExplorerElement> elements, boolean canGoBack) {
    this.allElements = elements;
    this.canGoBack = canGoBack;
    this.currentPage = 0;
    drawMenu();
  }

  private void drawMenu() {
    this.getChildren().clear();
    slices.clear();

    int startIdx = currentPage * MAX_ITEMS_PER_PAGE;
    List<ExplorerElement> currentElements = new ArrayList<>();
    boolean hasMore = allElements.size() > (startIdx + MAX_ITEMS_PER_PAGE);
    int itemsToShow = Math.min(MAX_ITEMS_PER_PAGE, allElements.size() - startIdx);

    for (int i = 0; i < itemsToShow; i++) {
      currentElements.add(allElements.get(startIdx + i));
    }

    int numberOfSlices = Math.max(1, currentElements.size());
    this.anglePerSlice = 360.0 / numberOfSlices;

    for (int i = 0; i < currentElements.size(); i++) {
      double startAngle = i * anglePerSlice;
      double endAngle = (i + 1) * anglePerSlice;

      Shape slice = createDonutSlice(200, 200, CENTER_RADIUS + 30, RADIUS, startAngle, endAngle);
      slice.setFill(Color.web("#2c3e50", 0.8));
      slice.setStroke(Color.WHITE);
      slice.setStrokeWidth(1);
      slices.add(slice);
      this.getChildren().add(slice);

      Text text = new Text(currentElements.get(i).getName() + "\n(" + (i + 1) + ")");
      text.setFill(Color.WHITE);
      text.setTextAlignment(TextAlignment.CENTER);
      text.setTextOrigin(VPos.CENTER);
      text.setMouseTransparent(true);
      this.getChildren().add(text);

      double middleAngle = Math.toRadians(startAngle + anglePerSlice / 2);
      double textRadius = (RADIUS + (CENTER_RADIUS + 30)) / 2;

      double tx =
          200 + textRadius * Math.cos(middleAngle) - (text.getLayoutBounds().getWidth() / 2);
      double ty = 200 + textRadius * Math.sin(middleAngle);

      text.setX(tx);
      text.setY(ty);
    }

    // --- BOUTON CENTRAL AVEC FERMETURE ---
    centerCircle = new Circle(200, 200, CENTER_RADIUS);
    centerCircle.setStroke(Color.WHITE);
    centerCircle.setStrokeWidth(2);
    centerCircle.setFill(Color.web("#7f8c8d", 0.8)); // Toujours actif maintenant
    this.getChildren().add(centerCircle);

    String centerLabel;
    if (currentPage > 0) {
      centerLabel = "PRÉCÉD.\n(0)";
    } else if (canGoBack) {
      centerLabel = "RETOUR\n(0)";
    } else {
      centerLabel = "FERMER\n(0)"; // NOUVEAU
    }

    Text backText = new Text(centerLabel);
    backText.setFill(Color.WHITE);
    backText.setTextAlignment(TextAlignment.CENTER);
    backText.setTextOrigin(VPos.CENTER);
    backText.setFont(Font.font(11));
    backText.setMouseTransparent(true);
    this.getChildren().add(backText);

    double bBackW = backText.getLayoutBounds().getWidth();
    backText.setX(200 - (bBackW / 2));

    if (hasMore) {
      backText.setY(200 - (CENTER_RADIUS / 2) + 2);
    } else {
      backText.setY(200);
    }

    // --- BOUTON SUIVANT ---
    if (hasMore) {
      nextButton = createSemiCircle(200, 200, CENTER_RADIUS);
      nextButton.setFill(Color.web("#27ae60", 0.9));
      nextButton.setStroke(Color.WHITE);
      nextButton.setStrokeWidth(1);
      this.getChildren().add(nextButton);

      Text nextTxt = new Text("SUIV.\n(9)");
      nextTxt.setFill(Color.WHITE);
      nextTxt.setTextAlignment(TextAlignment.CENTER);
      nextTxt.setTextOrigin(VPos.CENTER);
      nextTxt.setFont(Font.font(11));
      nextTxt.setMouseTransparent(true);
      this.getChildren().add(nextTxt);

      double bNextW = nextTxt.getLayoutBounds().getWidth();
      nextTxt.setX(200 - (bNextW / 2));
      nextTxt.setY(200 + (CENTER_RADIUS / 2) - 2);
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

    if (Math.abs(endAngle - startAngle) >= 360) {
      endAngle = startAngle + 359.99;
    }
    double startRad = Math.toRadians(startAngle);
    double endRad = Math.toRadians(endAngle);
    boolean largeArc = Math.abs(endAngle - startAngle) > 180.0;

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
                largeArc,
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
                largeArc,
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
              highlightSlice(-1); // Le bouton central est TOUJOURS actif maintenant
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
    if (index == -1)
      centerCircle.setFill(
          Color.web("#c0392b", 0.9)); // Rouge pour signifier une action critique (Fermer ou Retour)
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
        // Appelle la méthode onBackSelected dans tous les cas !
        if (listener != null) listener.onBackSelected();
      }
    } else if (currentIndex == -3) {
      currentPage++;
      drawMenu();
    } else if (currentIndex >= 0 && currentIndex < slices.size()) {
      int actualIndex = (currentPage * MAX_ITEMS_PER_PAGE) + currentIndex;
      if (listener != null) listener.onElementSelected(allElements.get(actualIndex));
    }
  }
}
