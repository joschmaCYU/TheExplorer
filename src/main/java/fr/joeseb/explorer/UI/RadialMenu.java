package fr.joeseb.explorer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
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

  private static final double RADIUS = 185;
  private static final double CENTER_RADIUS = 75;
  private static final int MAX_ITEMS_PER_PAGE = 8;

  private final List<Shape> slices = new ArrayList<>();
  private Circle centerCircle;
  private Shape nextButton;
  private List<ExplorerElement> allElements;
  private int currentPage = 0;
  private double anglePerSlice;
  private int currentIndex = -2;
  private MenuListener listener; // Utilisation de la nouvelle interface externe

  private boolean canGoBack = false;
  private Color baseCenterColor;
  private Color hoverCenterColor;

  public RadialMenu(List<ExplorerElement> elements, boolean canGoBack) {
    this.allElements = elements;
    this.canGoBack = canGoBack;
    this.setPrefSize(400, 400);
    drawMenu();
  }

  public void setMenuListener(MenuListener listener) {
    this.listener = listener;
  }

  public void setElements(List<ExplorerElement> elements, boolean canGoBack) {
    this.allElements = elements;
    this.canGoBack = canGoBack;
    this.currentPage = 0;
    drawMenu();
  }

  public ExplorerElement getHighlightedElement() {
    if (currentIndex >= 0 && currentIndex < slices.size()) {
      int actualIndex = (currentPage * MAX_ITEMS_PER_PAGE) + currentIndex;
      return allElements.get(actualIndex);
    }
    return null;
  }

  private void drawMenu() {
    this.getChildren().clear();
    slices.clear();

    int startIdx = currentPage * MAX_ITEMS_PER_PAGE;
    List<ExplorerElement> currentElements = new ArrayList<>();
    boolean hasMore = allElements.size() > (startIdx + MAX_ITEMS_PER_PAGE);
    int itemsToShow = Math.min(MAX_ITEMS_PER_PAGE, allElements.size() - startIdx);

    int totalPages = (int) Math.ceil((double) allElements.size() / MAX_ITEMS_PER_PAGE);
    if (totalPages == 0) totalPages = 1;

    for (int i = 0; i < itemsToShow; i++) {
      currentElements.add(allElements.get(startIdx + i));
    }

    if (currentElements.isEmpty()) {
      Shape emptySlice = createDonutSlice(200, 200, CENTER_RADIUS + 30, RADIUS, 0, 359.99);
      emptySlice.setFill(Color.web("#34495e", 0.4));
      emptySlice.setStroke(Color.WHITE);
      emptySlice.setStrokeWidth(1);
      this.getChildren().add(emptySlice);

      Text emptyText = new Text("Paneau vide\nGlissez vos elements ici");
      emptyText.setFill(Color.WHITE);
      emptyText.setTextAlignment(TextAlignment.CENTER);
      emptyText.setTextOrigin(VPos.CENTER);
      emptyText.setMouseTransparent(true);
      emptyText.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 11));
      this.getChildren().add(emptyText);

      emptyText.setX(200 - emptyText.getLayoutBounds().getWidth() / 2);
      emptyText.setY(200 + CENTER_RADIUS + 30);
    } else {
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

        String originalName = currentElements.get(i).getName();
        double middleAngle = Math.toRadians(startAngle + anglePerSlice / 2);

        // Calcul dynamique de la place disponible selon l'angle.
        // absCos vaut 1 sur la gauche/droite (espace minimum) et 0 en haut/bas (espace maximum).
        double absCos = Math.abs(Math.cos(middleAngle));

        // Limites à ajuster manuellement selon la taille en pixels de ton interface
        int minChars = 10; // Limite pour les cases latérales (restreint par l'épaisseur)
        int maxChars = 22; // Limite pour les cases verticales (largeur de l'arc)

        // Interpolation linéaire entre le minimum et le maximum
        int dynamicMaxLength = (int) (minChars + (maxChars - minChars) * (1.0 - absCos));

        String truncatedName = formatAppleStyle(originalName, dynamicMaxLength);

        Text text = new Text(truncatedName + "\n(" + (i + 1) + ")");
        text.setFill(Color.WHITE);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setTextOrigin(VPos.CENTER);
        text.setMouseTransparent(true);

        double textRadius = (RADIUS + (CENTER_RADIUS + 30)) / 2;

        double tx = 200 + textRadius * Math.cos(middleAngle);
        double ty = 200 + textRadius * Math.sin(middleAngle);

        // --- DESSIN DE L'ICÔNE ---
        String iconName = currentElements.get(i).getIcon();
        boolean hasIcon = false;

        if (iconName != null && !iconName.isEmpty()) {
          try {
            // On vérifie que le fichier existe bien avant de le dessiner
            java.io.InputStream stream = getClass().getResourceAsStream("/icons/" + iconName);
            if (stream != null) {
              javafx.scene.image.Image img = new javafx.scene.image.Image(stream);
              javafx.scene.image.ImageView iconView = new javafx.scene.image.ImageView(img);
              iconView.setFitWidth(24);
              iconView.setFitHeight(24);
              iconView.setMouseTransparent(true);

              // On centre l'icône et on la remonte un peu
              iconView.setX(tx - 12);
              iconView.setY(ty - 22);
              this.getChildren().add(iconView);
              hasIcon = true;
            }
          } catch (Exception e) {
            System.err.println("Icône introuvable : " + iconName);
          }
        }

        // --- DESSIN DU TEXTE ---
        text.setX(tx - (text.getLayoutBounds().getWidth() / 2));

        // Si l'icône est présente, on descend le texte pour faire de la place.
        // Sinon, on le place parfaitement au centre (tx, ty).
        if (hasIcon) {
          text.setY(ty + 16);
        } else {
          text.setY(ty);
        }

        this.getChildren().add(text);
      }
    }

    centerCircle = new Circle(200, 200, CENTER_RADIUS);
    centerCircle.setStroke(Color.WHITE);
    centerCircle.setStrokeWidth(2);
    this.getChildren().add(centerCircle);

    String centerLabel;
    if (currentPage > 0) {
      centerLabel = "PRECED.\n(0)";
      baseCenterColor = Color.web("#e67e22", 0.8);
      hoverCenterColor = Color.web("#d35400", 0.9);
    } else if (canGoBack) {
      centerLabel = "RETOUR\n(0)";
      baseCenterColor = Color.web("#e74c3c", 0.8);
      hoverCenterColor = Color.web("#c0392b", 0.9);
    } else {
      centerLabel = "FERMER\n(0)";
      baseCenterColor = Color.web("#7f8c8d", 0.8);
      hoverCenterColor = Color.web("#c0392b", 0.9);
    }

    centerCircle.setFill(baseCenterColor);

    Text backText = new Text(centerLabel);
    backText.setFill(Color.WHITE);
    backText.setTextAlignment(TextAlignment.CENTER);
    backText.setTextOrigin(VPos.CENTER);
    backText.setFont(Font.font(11));
    backText.setMouseTransparent(true);
    this.getChildren().add(backText);

    double bBackW = backText.getLayoutBounds().getWidth();
    backText.setX(200 - (bBackW / 2));
    backText.setY(hasMore ? 200 - (CENTER_RADIUS / 2) + 2 : 200);

    if (totalPages > 1) {
      Text pageIndicator = new Text(String.valueOf(currentPage + 1) + " / " + totalPages);
      pageIndicator.setFill(Color.web("#bdc3c7"));
      pageIndicator.setTextAlignment(TextAlignment.CENTER);
      pageIndicator.setTextOrigin(VPos.CENTER);
      pageIndicator.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 10));
      pageIndicator.setMouseTransparent(true);
      this.getChildren().add(pageIndicator);

      double pIndW = pageIndicator.getLayoutBounds().getWidth();
      pageIndicator.setX(200 - (pIndW / 2));
      // CORRECTION : On place l'indicateur à Y=212 (sous la ligne) au lieu de 200
      pageIndicator.setY(hasMore ? 200 - 10 : 200 + (CENTER_RADIUS / 2) - 5);
    }

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
      nextTxt.setFont(Font.font(12));
      nextTxt.setMouseTransparent(true);
      this.getChildren().add(nextTxt);

      double bNextW = nextTxt.getLayoutBounds().getWidth();
      nextTxt.setX(200 - (bNextW / 2));
      nextTxt.setY(200 + (CENTER_RADIUS / 2));
    } else {
      nextButton = null;
    }

    setupMouseEvents();
    currentIndex = -2;
  }

  private Shape createDonutSlice(double cx, double cy, double ir, double or, double sa, double ea) {
    if (Math.abs(ea - sa) >= 360) ea = sa + 359.99;
    double startRad = Math.toRadians(sa);
    double endRad = Math.toRadians(ea);
    boolean largeArc = Math.abs(ea - sa) > 180.0;
    Path path = new Path();
    path.getElements().add(new MoveTo(cx + or * Math.cos(startRad), cy + or * Math.sin(startRad)));
    path.getElements()
        .add(
            new ArcTo(
                or, or, 0, cx + or * Math.cos(endRad), cy + or * Math.sin(endRad), largeArc, true));
    path.getElements().add(new LineTo(cx + ir * Math.cos(endRad), cy + ir * Math.sin(endRad)));
    path.getElements()
        .add(
            new ArcTo(
                ir,
                ir,
                0,
                cx + ir * Math.cos(startRad),
                cy + ir * Math.sin(startRad),
                largeArc,
                false));
    path.getElements().add(new ClosePath());
    return path;
  }

  private Shape createSemiCircle(double cx, double cy, double r) {
    Path path = new Path();
    path.getElements().add(new MoveTo(cx - r, cy));
    path.getElements().add(new LineTo(cx + r, cy));
    path.getElements().add(new ArcTo(r, r, 0, cx - r, cy, false, true));
    path.getElements().add(new ClosePath());
    return path;
  }

  private void setupMouseEvents() {
    this.setOnMouseMoved(
        event -> {
          double dx = event.getX() - 200;
          double dy = event.getY() - 200;
          double dist = Math.sqrt(dx * dx + dy * dy);
          if (dist < CENTER_RADIUS) {
            if (nextButton != null && dy > 0) highlightSlice(-3);
            else highlightSlice(-1);
            return;
          }
          if (dist > RADIUS || slices.isEmpty()) {
            highlightSlice(-2);
            return;
          }
          double angle = Math.toDegrees(Math.atan2(dy, dx));
          if (angle < 0) angle += 360;
          highlightSlice((int) (angle / anglePerSlice));
        });

    this.setOnMouseClicked(
        event -> {
          if (event.getButton() == MouseButton.SECONDARY) {
            ExplorerElement el = getHighlightedElement();
            if (el != null && listener != null) {
              listener.onElementEdit(el);
            }
          } else {
            executeAction();
          }
        });

    this.setOnDragDetected(
        event -> {
          ExplorerElement el = getHighlightedElement();
          if (el != null) {
            Dragboard db = this.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();

            if ("Texte".equalsIgnoreCase(el.getType())) {
              content.putString(el.getPath());
            } else {
              File f = new File(el.getPath());
              if (f.exists()) {
                content.putFiles(List.of(f));
              }
            }
            db.setContent(content);
            event.consume();
          }
        });
  }

  public void highlightSlice(int index) {
    if (index == currentIndex) return;
    if (index == -3 && nextButton == null) return;

    if (currentIndex == -1) centerCircle.setFill(baseCenterColor);
    else if (currentIndex == -3 && nextButton != null)
      nextButton.setFill(Color.web("#27ae60", 0.9));
    else if (currentIndex >= 0 && currentIndex < slices.size())
      slices.get(currentIndex).setFill(Color.web("#2c3e50", 0.8));

    if (index == -1) centerCircle.setFill(hoverCenterColor);
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
      } else if (listener != null) listener.onBackSelected();
    } else if (currentIndex == -3 && nextButton != null) {
      currentPage++;
      drawMenu();
    } else if (currentIndex >= 0 && currentIndex < slices.size()) {
      int actualIndex = (currentPage * MAX_ITEMS_PER_PAGE) + currentIndex;
      if (listener != null) listener.onElementSelected(allElements.get(actualIndex));
    }
  }

  private String formatAppleStyle(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) {
      return text;
    }
    // On calcule combien de caractères garder au début et à la fin (en soustrayant les 3 points)
    int keepFront = (maxLength - 3) / 2 + 1; // On privilégie un caractère de plus au début
    int keepBack = (maxLength - 3) - keepFront;

    return text.substring(0, keepFront) + "..." + text.substring(text.length() - keepBack);
  }
}
