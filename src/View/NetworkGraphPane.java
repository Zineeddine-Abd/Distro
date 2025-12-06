package View;

import Controller.AppController;
import Model.*;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.*;

/**
 * Panneau de visualisation du graphe du réseau électrique
 * Permet l'interaction: zoom, déplacement, sélection
 */
public class NetworkGraphPane extends Pane {
    private AppController controller;
    private final Canvas canvas;
    private final GraphicsContext gc;

    // État de la vue
    private double offsetX = 0;
    private double offsetY = 0;
    private double zoom = 1.0;
    private final double MIN_ZOOM = 0.3;
    private final double MAX_ZOOM = 3.0;

    // Positions des nœuds
    private final Map<String, Point2D> positionsGenerateurs = new HashMap<>();
    private final Map<String, Point2D> positionsMaisons = new HashMap<>();

    // Interaction
    private Point2D dernierClic;
    private String elementSelectionne = null;
    private boolean estGenerateur = false;
    private boolean dragEnCours = false;

    // Mode placement
    private boolean modeAttenteClic = false;
    private java.util.function.Consumer<Point2D> callbackPosition;
    private boolean estGenerateurEnPlacement = false;

    // Paramètres visuels
    private static final double NODE_SIZE = 50;
    private static final double ICON_SIZE = 35;
    private static final double LABEL_OFFSET = 30;

    public NetworkGraphPane(AppController controller) {
        this.controller = controller;

        canvas = new Canvas();
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());
        gc = canvas.getGraphicsContext2D();

        getChildren().add(canvas);

        // Écouteurs d'événements
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        canvas.setOnScroll(this::handleScroll);
        canvas.setOnMouseMoved(this::handleMouseMoved);

        // Redessiner quand la taille change
        canvas.widthProperty().addListener((obs, old, val) -> dessiner());
        canvas.heightProperty().addListener((obs, old, val) -> dessiner());
    }

    // Méthode pour permettre de changer le contrôleur (nouveau réseau)
    public void setController(AppController controller) {
        this.controller = controller;
    }

    // Attendre un clic de l'utilisateur pour placer un élément
    public void attendreClicPourPosition(java.util.function.Consumer<Point2D> callback, boolean estGen) {
        this.modeAttenteClic = true;
        this.callbackPosition = callback;
        this.estGenerateurEnPlacement = estGen;
        canvas.setCursor(javafx.scene.Cursor.CROSSHAIR);
    }

    // Définir manuellement la position d'un générateur
    public void setPositionGenerateur(String nom, Point2D pos) {
        positionsGenerateurs.put(nom, pos);
    }

    // Définir manuellement la position d'une maison
    public void setPositionMaison(String nom, Point2D pos) {
        positionsMaisons.put(nom, pos);
    }

    public void rafraichir() {
        calculerPositions();
        dessiner();
    }

    public void recentrerVue() {
        offsetX = 0;
        offsetY = 0;
        zoom = 1.0;
        calculerPositions();
        dessiner();
    }

    private void calculerPositions() {
        positionsGenerateurs.clear();
        positionsMaisons.clear();

        Reseau reseau = controller.getReseau();
        List<String> generateurs = new ArrayList<>(reseau.getGenerateurs().keySet());
        List<String> maisons = new ArrayList<>(reseau.getMaisons().keySet());

        double centreX = canvas.getWidth() / 2;
        double centreY = canvas.getHeight() / 2;

        // Placement des générateurs en cercle à gauche
        int nbGen = generateurs.size();
        double rayonGen = Math.min(200, canvas.getHeight() / 4);
        double angleStepGen = nbGen > 1 ? 2 * Math.PI / nbGen : 0;

        for (int i = 0; i < nbGen; i++) {
            double angle = i * angleStepGen - Math.PI / 2;
            double x = centreX - 250 + Math.cos(angle) * rayonGen;
            double y = centreY + Math.sin(angle) * rayonGen;
            positionsGenerateurs.put(generateurs.get(i), new Point2D(x, y));
        }

        // Placement des maisons en cercle à droite
        int nbMaisons = maisons.size();
        double rayonMaison = Math.min(250, canvas.getHeight() / 3);
        double angleStepMaison = nbMaisons > 1 ? 2 * Math.PI / nbMaisons : 0;

        for (int i = 0; i < nbMaisons; i++) {
            double angle = i * angleStepMaison - Math.PI / 2;
            double x = centreX + 250 + Math.cos(angle) * rayonMaison;
            double y = centreY + Math.sin(angle) * rayonMaison;
            positionsMaisons.put(maisons.get(i), new Point2D(x, y));
        }
    }

    private void dessiner() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Fond
        gc.setFill(Color.web("#ecf0f1"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Grille
        dessinerGrille();

        // Connexions
        dessinerConnexions();

        // Nœuds
        dessinerGenerateurs();
        dessinerMaisons();

        // Légende
        dessinerLegende();
    }

    private void dessinerGrille() {
        gc.setStroke(Color.web("#bdc3c7", 0.3));
        gc.setLineWidth(1);

        double espacement = 50 * zoom;

        for (double x = (offsetX % espacement); x < canvas.getWidth(); x += espacement) {
            gc.strokeLine(x, 0, x, canvas.getHeight());
        }

        for (double y = (offsetY % espacement); y < canvas.getHeight(); y += espacement) {
            gc.strokeLine(0, y, canvas.getWidth(), y);
        }
    }

    private void dessinerConnexions() {
        Reseau reseau = controller.getReseau();
        Map<String, List<String>> connexions = reseau.getConnexions();

        gc.setLineWidth(2.5 * zoom);

        for (Map.Entry<String, List<String>> entry : connexions.entrySet()) {
            String nomMaison = entry.getKey();
            Point2D posMaison = positionsMaisons.get(nomMaison);

            if (posMaison == null) continue;

            for (String nomGen : entry.getValue()) {
                Point2D posGen = positionsGenerateurs.get(nomGen);
                if (posGen == null) continue;

                Point2D p1 = transformerPoint(posGen);
                Point2D p2 = transformerPoint(posMaison);

                // Vérifier la surcharge pour colorer la ligne
                Generateur gen = reseau.getGenerateurs().get(nomGen);
                int charge = calculerChargeGenerateur(reseau, nomGen);

                if (charge > gen.getCapaciteMax()) {
                    gc.setStroke(Color.web("#e74c3c")); // Rouge pour surcharge
                } else {
                    gc.setStroke(Color.web("#3498db", 0.6)); // Bleu normal
                }

                // Dessiner la ligne avec flèche
                dessinerLigneAvecFleche(p1, p2);
            }
        }
    }

    private void dessinerLigneAvecFleche(Point2D debut, Point2D fin) {
        gc.strokeLine(debut.getX(), debut.getY(), fin.getX(), fin.getY());

        // Dessiner une flèche au milieu
        double midX = (debut.getX() + fin.getX()) / 2;
        double midY = (debut.getY() + fin.getY()) / 2;

        double angle = Math.atan2(fin.getY() - debut.getY(), fin.getX() - debut.getX());
        double arrowLength = 12 * zoom;
        double arrowAngle = Math.PI / 6;

        double x1 = midX - arrowLength * Math.cos(angle - arrowAngle);
        double y1 = midY - arrowLength * Math.sin(angle - arrowAngle);
        double x2 = midX - arrowLength * Math.cos(angle + arrowAngle);
        double y2 = midY - arrowLength * Math.sin(angle + arrowAngle);

        gc.strokeLine(midX, midY, x1, y1);
        gc.strokeLine(midX, midY, x2, y2);
    }

    private void dessinerGenerateurs() {
        Reseau reseau = controller.getReseau();

        for (Map.Entry<String, Generateur> entry : reseau.getGenerateurs().entrySet()) {
            String nom = entry.getKey();
            Generateur gen = entry.getValue();
            Point2D pos = transformerPoint(positionsGenerateurs.get(nom));

            if (pos == null) continue;

            double x = pos.getX();
            double y = pos.getY();
            double taille = NODE_SIZE * zoom;

            // Calculer la charge
            int charge = calculerChargeGenerateur(reseau, nom);
            boolean surcharge = charge > gen.getCapaciteMax();

            // Ombre
            gc.setFill(Color.web("#000000", 0.2));
            gc.fillOval(x - taille/2 + 3, y - taille/2 + 3, taille, taille);

            // Cercle principal
            Color couleurPrincipale = surcharge ? Color.web("#e74c3c") : Color.web("#f39c12");
            gc.setFill(couleurPrincipale);
            gc.fillOval(x - taille/2, y - taille/2, taille, taille);

            // Bordure
            gc.setStroke(nom.equals(elementSelectionne) && estGenerateur ?
                    Color.web("#2c3e50") : Color.web("#d68910"));
            gc.setLineWidth(3 * zoom);
            gc.strokeOval(x - taille/2, y - taille/2, taille, taille);

            // Icône éclair
            dessinerEclair(x, y, ICON_SIZE * zoom, Color.WHITE);

            // Nom
            gc.setFill(Color.web("#2c3e50"));
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 13 * zoom));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(nom, x, y - taille/2 - 10 * zoom);

            // Capacité
            String capacite = String.format("%d/%d kW", charge, gen.getCapaciteMax());
            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 11 * zoom));
            gc.setFill(surcharge ? Color.web("#c0392b") : Color.web("#27ae60"));
            gc.fillText(capacite, x, y + taille/2 + 20 * zoom);
        }
    }

    private void dessinerMaisons() {
        Reseau reseau = controller.getReseau();

        for (Map.Entry<String, Maison> entry : reseau.getMaisons().entrySet()) {
            String nom = entry.getKey();
            Maison maison = entry.getValue();
            Point2D pos = transformerPoint(positionsMaisons.get(nom));

            if (pos == null) continue;

            double x = pos.getX();
            double y = pos.getY();
            double taille = NODE_SIZE * zoom;

            // Vérifier si connectée
            boolean connectee = reseau.connexionExistePourMaison(nom);

            // Ombre
            gc.setFill(Color.web("#000000", 0.2));
            gc.fillRect(x - taille/2 + 3, y - taille/2 + 3, taille, taille);

            // Rectangle principal
            Color couleurPrincipale = getCouleurMaison(maison.getConsommation());
            gc.setFill(couleurPrincipale);
            gc.fillRect(x - taille/2, y - taille/2, taille, taille);

            // Bordure
            Color couleurBordure = nom.equals(elementSelectionne) && !estGenerateur ?
                    Color.web("#2c3e50") :
                    (connectee ? Color.web("#16a085") : Color.web("#e67e22"));
            gc.setStroke(couleurBordure);
            gc.setLineWidth(3 * zoom);
            gc.strokeRect(x - taille/2, y - taille/2, taille, taille);

            // Icône maison
            dessinerMaison(x, y, ICON_SIZE * zoom, Color.WHITE);

            // Nom
            gc.setFill(Color.web("#2c3e50"));
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12 * zoom));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(nom, x, y - taille/2 - 10 * zoom);

            // Consommation
            String conso = maison.getConsommationKw() + " kW";
            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 11 * zoom));
            gc.setFill(Color.web("#7f8c8d"));
            gc.fillText(conso, x, y + taille/2 + 20 * zoom);
        }
    }

    private Color getCouleurMaison(Consommation conso) {
        switch (conso) {
            case BASSE: return Color.web("#2ecc71");
            case NORMAL: return Color.web("#3498db");
            case FORTE: return Color.web("#9b59b6");
            default: return Color.GRAY;
        }
    }

    private void dessinerEclair(double x, double y, double taille, Color couleur) {
        gc.setFill(couleur);
        gc.setStroke(couleur);
        gc.setLineWidth(2 * zoom);

        double[] xPoints = {
                x - taille/4, x, x - taille/6,
                x + taille/4, x, x + taille/6
        };
        double[] yPoints = {
                y - taille/3, y - taille/6, y,
                y + taille/3, y + taille/6, y
        };

        gc.fillPolygon(xPoints, yPoints, 6);
    }

    private void dessinerMaison(double x, double y, double taille, Color couleur) {
        gc.setFill(couleur);
        gc.setStroke(couleur);
        gc.setLineWidth(2 * zoom);

        // Base de la maison
        double baseWidth = taille * 0.7;
        double baseHeight = taille * 0.5;
        gc.fillRect(x - baseWidth/2, y - baseHeight/4, baseWidth, baseHeight);

        // Toit
        double[] xPoints = {x - baseWidth/2, x, x + baseWidth/2};
        double[] yPoints = {y - baseHeight/4, y - taille/2, y - baseHeight/4};
        gc.fillPolygon(xPoints, yPoints, 3);

        // Porte
        gc.setFill(Color.web("#34495e"));
        double porteWidth = taille * 0.2;
        double porteHeight = taille * 0.3;
        gc.fillRect(x - porteWidth/2, y + baseHeight/4 - porteHeight, porteWidth, porteHeight);
    }

    private void dessinerLegende() {
        double x = 20;
        double y = 20;
        double largeur = 200;
        double hauteur = 120;

        // Fond semi-transparent
        gc.setFill(Color.web("#ffffff", 0.9));
        gc.fillRoundRect(x, y, largeur, hauteur, 10, 10);
        gc.setStroke(Color.web("#bdc3c7"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, largeur, hauteur, 10, 10);

        // Titre
        gc.setFill(Color.web("#2c3e50"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Légende", x + 10, y + 25);

        // Items
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 11));

        // Générateur
        gc.setFill(Color.web("#f39c12"));
        gc.fillOval(x + 10, y + 40, 15, 15);
        gc.setFill(Color.web("#2c3e50"));
        gc.fillText("Générateur", x + 35, y + 52);

        // Maison BASSE
        gc.setFill(Color.web("#2ecc71"));
        gc.fillRect(x + 10, y + 60, 15, 15);
        gc.setFill(Color.web("#2c3e50"));
        gc.fillText("Maison (Basse)", x + 35, y + 72);

        // Maison NORMAL
        gc.setFill(Color.web("#3498db"));
        gc.fillRect(x + 10, y + 80, 15, 15);
        gc.setFill(Color.web("#2c3e50"));
        gc.fillText("Maison (Normale)", x + 35, y + 92);

        // Maison FORTE
        gc.setFill(Color.web("#9b59b6"));
        gc.fillRect(x + 10, y + 100, 15, 15);
        gc.setFill(Color.web("#2c3e50"));
        gc.fillText("Maison (Forte)", x + 35, y + 112);
    }

    // Gestion des événements

    private void handleMousePressed(MouseEvent e) {
        dernierClic = new Point2D(e.getX(), e.getY());

        // Mode placement: l'utilisateur clique pour placer
        if (modeAttenteClic && callbackPosition != null) {
            Point2D posReel = inverserTransformation(dernierClic);
            callbackPosition.accept(posReel);
            modeAttenteClic = false;
            callbackPosition = null;
            canvas.setCursor(javafx.scene.Cursor.DEFAULT);
            return;
        }

        // Vérifier si on clique sur un élément
        elementSelectionne = trouverElementSousPointeur(dernierClic);

        if (elementSelectionne != null) {
            dragEnCours = true;
        }

        dessiner();
    }

    private void handleMouseDragged(MouseEvent e) {
        if (dragEnCours && elementSelectionne != null) {
            // Déplacer l'élément sélectionné
            Point2D nouvellePos = inverserTransformation(new Point2D(e.getX(), e.getY()));

            if (estGenerateur) {
                positionsGenerateurs.put(elementSelectionne, nouvellePos);
            } else {
                positionsMaisons.put(elementSelectionne, nouvellePos);
            }

            dessiner();
        } else if (dernierClic != null) {
            // Déplacer la vue
            offsetX += e.getX() - dernierClic.getX();
            offsetY += e.getY() - dernierClic.getY();
            dernierClic = new Point2D(e.getX(), e.getY());
            dessiner();
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        dragEnCours = false;
        dernierClic = null;
    }

    private void handleScroll(ScrollEvent e) {
        double facteur = e.getDeltaY() > 0 ? 1.1 : 0.9;
        double nouveauZoom = zoom * facteur;

        if (nouveauZoom >= MIN_ZOOM && nouveauZoom <= MAX_ZOOM) {
            // Zoomer vers la position de la souris
            double mouseX = e.getX();
            double mouseY = e.getY();

            offsetX = mouseX - (mouseX - offsetX) * facteur;
            offsetY = mouseY - (mouseY - offsetY) * facteur;

            zoom = nouveauZoom;
            dessiner();
        }
    }

    private void handleMouseMoved(MouseEvent e) {
        // Mode placement: afficher le curseur approprié
        if (modeAttenteClic) {
            canvas.setCursor(javafx.scene.Cursor.CROSSHAIR);
            return;
        }

        // Changer le curseur si on survole un élément
        Point2D pos = new Point2D(e.getX(), e.getY());
        String element = trouverElementSousPointeur(pos);

        if (element != null) {
            canvas.setCursor(javafx.scene.Cursor.HAND);
        } else {
            canvas.setCursor(javafx.scene.Cursor.DEFAULT);
        }
    }

    private String trouverElementSousPointeur(Point2D pointeur) {
        Point2D posInversee = inverserTransformation(pointeur);
        double taille = NODE_SIZE / 2;

        // Vérifier les générateurs
        for (Map.Entry<String, Point2D> entry : positionsGenerateurs.entrySet()) {
            Point2D pos = entry.getValue();
            if (pos.distance(posInversee) < taille) {
                estGenerateur = true;
                return entry.getKey();
            }
        }

        // Vérifier les maisons
        for (Map.Entry<String, Point2D> entry : positionsMaisons.entrySet()) {
            Point2D pos = entry.getValue();
            if (pos.distance(posInversee) < taille) {
                estGenerateur = false;
                return entry.getKey();
            }
        }

        return null;
    }

    private Point2D transformerPoint(Point2D point) {
        if (point == null) return null;
        return new Point2D(
                point.getX() * zoom + offsetX,
                point.getY() * zoom + offsetY
        );
    }

    private Point2D inverserTransformation(Point2D point) {
        return new Point2D(
                (point.getX() - offsetX) / zoom,
                (point.getY() - offsetY) / zoom
        );
    }

    private int calculerChargeGenerateur(Reseau reseau, String nomGen) {
        int charge = 0;
        for (Map.Entry<String, List<String>> entry : reseau.getConnexions().entrySet()) {
            if (entry.getValue().contains(nomGen)) {
                Maison maison = reseau.getMaisons().get(entry.getKey());
                if (maison != null) {
                    charge += maison.getConsommationKw();
                }
            }
        }
        return charge;
    }
}