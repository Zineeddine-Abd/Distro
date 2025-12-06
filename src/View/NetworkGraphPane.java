package View;

import Controller.AppController;
import Model.*;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.*;

/**
 * Panneau de visualisation du graphe du reseau electrique
 * CORRECTION: Le placement avec la souris fonctionne maintenant correctement
 */
public class NetworkGraphPane extends Pane {
    private AppController controller;
    private final Canvas canvas;
    private final GraphicsContext gc;

    // Etat de la vue
    private double offsetX = 0;
    private double offsetY = 0;
    private double zoom = 1.0;
    private final double MIN_ZOOM = 0.3;
    private final double MAX_ZOOM = 3.0;

    // Positions des noeuds (STOCKEES EN COORDONNEES REELLES, PAS TRANSFORMEES)
    private final Map<String, Point2D> positionsGenerateurs = new HashMap<>();
    private final Map<String, Point2D> positionsMaisons = new HashMap<>();

    // Interaction
    private Point2D dernierClic;
    private String elementSelectionne = null;
    private boolean estGenerateur = false;
    private boolean dragEnCours = false;

    // Mode placement - CORRECTION: Variables pour le placement manuel
    private boolean modeAttenteClic = false;
    private java.util.function.Consumer<Point2D> callbackPosition;
    private boolean estGenerateurEnPlacement = false;

    // Parametres visuels
    private static final double NODE_SIZE = 50;
    private static final double ICON_SIZE = 35;

    public NetworkGraphPane(AppController controller) {
        this.controller = controller;

        canvas = new Canvas();
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());
        gc = canvas.getGraphicsContext2D();

        getChildren().add(canvas);

        // Ecouteurs d'evenements
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        canvas.setOnScroll(this::handleScroll);
        canvas.setOnMouseMoved(this::handleMouseMoved);

        // Redessiner quand la taille change
        canvas.widthProperty().addListener((obs, old, val) -> dessiner());
        canvas.heightProperty().addListener((obs, old, val) -> dessiner());
    }

    public void setController(AppController controller) {
        this.controller = controller;
    }

    /**
     * CORRECTION MAJEURE: Cette methode active le mode placement
     * L'utilisateur clique sur le graphe pour placer l'element
     */
    public void attendreClicPourPosition(java.util.function.Consumer<Point2D> callback, boolean estGen) {
        this.modeAttenteClic = true;
        this.callbackPosition = callback;
        this.estGenerateurEnPlacement = estGen;
        canvas.setCursor(javafx.scene.Cursor.CROSSHAIR);

        // Message visuel pour l'utilisateur
        System.out.println("Mode placement actif. Cliquez sur le graphe pour placer l'element.");
    }

    /**
     * CORRECTION: Definir manuellement la position (en coordonnees REELLES)
     */
    public void setPositionGenerateur(String nom, Point2D pos) {
        positionsGenerateurs.put(nom, pos);
        dessiner();
    }

    public void setPositionMaison(String nom, Point2D pos) {
        positionsMaisons.put(nom, pos);
        dessiner();
    }

    public void rafraichir() {
        // CORRECTION: S'assurer qu'on est sur le thread JavaFX
        if (javafx.application.Platform.isFxApplicationThread()) {
            calculerPositionsManquantes();
            dessiner();
        } else {
            javafx.application.Platform.runLater(() -> {
                calculerPositionsManquantes();
                dessiner();
            });
        }
    }

    public void recentrerVue() {
        offsetX = 0;
        offsetY = 0;
        zoom = 1.0;
        dessiner();
    }

    /**
     * Positionnement en graphe bipartite (deux lignes verticales paralleles)
     * SANS labels de colonnes
     * AVEC espacement vertical augmente pour meilleure lisibilite
     */
    private void calculerPositionsManquantes() {
        Reseau reseau = controller.getReseau();

        double largeur = canvas.getWidth();
        double hauteur = canvas.getHeight();

        // Marges augmentees pour plus d'espace
        double margeHaut = 80;
        double margeBas = 80;
        double hauteurUtile = hauteur - margeHaut - margeBas;

        // Espacement minimum entre les noeuds (augmente pour meilleure lisibilite)
        double espacementMin = 120; // Espace minimum entre deux elements

        // Positions X fixes pour les deux colonnes
        double xGenerateurs = largeur * 0.25;  // 25% de la largeur (colonne gauche)
        double xMaisons = largeur * 0.75;       // 75% de la largeur (colonne droite)

        // === GENERATEURS (Colonne gauche) ===
        List<String> gensSansPosition = new ArrayList<>();
        for (String nom : reseau.getGenerateurs().keySet()) {
            if (!positionsGenerateurs.containsKey(nom)) {
                gensSansPosition.add(nom);
            }
        }

        if (!gensSansPosition.isEmpty()) {
            int nbGen = gensSansPosition.size();

            // Calculer l'espacement necessaire
            double espacementSouhaite = Math.max(espacementMin, hauteurUtile / (nbGen + 1));

            // Si on a beaucoup d'elements, utiliser tout l'espace disponible
            if (nbGen > 1) {
                espacementSouhaite = Math.max(espacementMin, hauteurUtile / (nbGen - 1));
            }

            for (int i = 0; i < nbGen; i++) {
                double y;
                if (nbGen == 1) {
                    // Un seul generateur : centrer verticalement
                    y = hauteur / 2;
                } else {
                    // Plusieurs generateurs : repartir avec espacement
                    y = margeHaut + (i * espacementSouhaite);
                }

                positionsGenerateurs.put(gensSansPosition.get(i), new Point2D(xGenerateurs, y));
            }
        }

        // === MAISONS (Colonne droite) ===
        List<String> maisonsSansPosition = new ArrayList<>();
        for (String nom : reseau.getMaisons().keySet()) {
            if (!positionsMaisons.containsKey(nom)) {
                maisonsSansPosition.add(nom);
            }
        }

        if (!maisonsSansPosition.isEmpty()) {
            int nbMaisons = maisonsSansPosition.size();

            // Calculer l'espacement necessaire
            double espacementSouhaite = Math.max(espacementMin, hauteurUtile / (nbMaisons + 1));

            // Si on a beaucoup d'elements, utiliser tout l'espace disponible
            if (nbMaisons > 1) {
                espacementSouhaite = Math.max(espacementMin, hauteurUtile / (nbMaisons - 1));
            }

            for (int i = 0; i < nbMaisons; i++) {
                double y;
                if (nbMaisons == 1) {
                    // Une seule maison : centrer verticalement
                    y = hauteur / 2;
                } else {
                    // Plusieurs maisons : repartir avec espacement
                    y = margeHaut + (i * espacementSouhaite);
                }

                positionsMaisons.put(maisonsSansPosition.get(i), new Point2D(xMaisons, y));
            }
        }
    }

    private void dessiner() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Fond
        gc.setFill(Color.web("#ecf0f1"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Grille
        dessinerGrille();

        // Instructions si en mode placement
        if (modeAttenteClic) {
            dessinerInstructionsPlacement();
        }

        // Connexions (SANS labels de colonnes)
        dessinerConnexions();

        // Noeuds
        dessinerGenerateurs();
        dessinerMaisons();

        // Legende
        dessinerLegende();
    }

    /**
     * CORRECTION: Afficher des instructions visuelles en mode placement
     */
    private void dessinerInstructionsPlacement() {
        gc.setFill(Color.web("#3498db", 0.9));
        gc.fillRoundRect(canvas.getWidth() / 2 - 200, 20, 400, 60, 10, 10);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("MODE PLACEMENT", canvas.getWidth() / 2, 45);

        String type = estGenerateurEnPlacement ? "generateur" : "maison";
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        gc.fillText("Cliquez sur le graphe pour placer le " + type, canvas.getWidth() / 2, 65);
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

        for (Map.Entry<String, List<String>> entry : connexions.entrySet()) {
            String nomMaison = entry.getKey();
            Point2D posMaison = positionsMaisons.get(nomMaison);

            if (posMaison == null) continue;

            for (String nomGen : entry.getValue()) {
                Point2D posGen = positionsGenerateurs.get(nomGen);
                if (posGen == null) continue;

                Point2D p1 = transformerPoint(posGen);
                Point2D p2 = transformerPoint(posMaison);

                // Verifier la surcharge pour colorer la ligne
                Generateur gen = reseau.getGenerateurs().get(nomGen);
                int charge = calculerChargeGenerateur(reseau, nomGen);

                if (charge > gen.getCapaciteMax()) {
                    gc.setStroke(Color.web("#e74c3c")); // Rouge surcharge
                    gc.setLineWidth(3.5 * zoom); // Plus epais pour attirer l'attention
                } else {
                    gc.setStroke(Color.web("#3498db", 0.7)); // Bleu normal
                    gc.setLineWidth(2.5 * zoom);
                }

                // Dessiner ligne droite avec fleche
                dessinerLigneAvecFleche(p1, p2);
            }
        }
    }

    /**
     * Dessine une ligne droite avec une fleche au milieu
     */
    private void dessinerLigneAvecFleche(Point2D debut, Point2D fin) {
        double startX = debut.getX();
        double startY = debut.getY();
        double endX = fin.getX();
        double endY = fin.getY();

        // Ligne droite
        gc.strokeLine(startX, startY, endX, endY);

        // Fleche au milieu
        double midX = (startX + endX) / 2;
        double midY = (startY + endY) / 2;

        double angle = Math.atan2(endY - startY, endX - startX);
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

        // Trier les generateurs par position Y pour afficher dans l'ordre
        List<Map.Entry<String, Generateur>> generateurs = new ArrayList<>(reseau.getGenerateurs().entrySet());
        generateurs.sort((e1, e2) -> {
            Point2D p1 = positionsGenerateurs.get(e1.getKey());
            Point2D p2 = positionsGenerateurs.get(e2.getKey());
            if (p1 == null || p2 == null) return 0;
            return Double.compare(p1.getY(), p2.getY());
        });

        for (Map.Entry<String, Generateur> entry : generateurs) {
            String nom = entry.getKey();
            Generateur gen = entry.getValue();
            Point2D posReelle = positionsGenerateurs.get(nom);

            if (posReelle == null) continue;

            Point2D pos = transformerPoint(posReelle);
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

            // Nom (au-dessus)
            gc.setFill(Color.web("#2c3e50"));
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 14 * zoom));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(nom, x, y - taille/2 - 12 * zoom);

            // Capacite (en dessous)
            String capacite = String.format("%d/%d kW", charge, gen.getCapaciteMax());
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12 * zoom));
            gc.setFill(surcharge ? Color.web("#c0392b") : Color.web("#27ae60"));
            gc.fillText(capacite, x, y + taille/2 + 22 * zoom);

            // Compteur de connexions (petit badge)
            int nbConnexions = 0;
            for (List<String> gens : reseau.getConnexions().values()) {
                if (gens.contains(nom)) nbConnexions++;
            }
            if (nbConnexions > 0) {
                double badgeX = x + taille/2 - 8 * zoom;
                double badgeY = y - taille/2 + 8 * zoom;
                double badgeSize = 18 * zoom;

                gc.setFill(Color.web("#e74c3c"));
                gc.fillOval(badgeX - badgeSize/2, badgeY - badgeSize/2, badgeSize, badgeSize);

                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 10 * zoom));
                gc.fillText(String.valueOf(nbConnexions), badgeX, badgeY + 3 * zoom);
            }
        }
    }

    private void dessinerMaisons() {
        Reseau reseau = controller.getReseau();

        // Trier les maisons par position Y pour afficher dans l'ordre
        List<Map.Entry<String, Maison>> maisons = new ArrayList<>(reseau.getMaisons().entrySet());
        maisons.sort((e1, e2) -> {
            Point2D p1 = positionsMaisons.get(e1.getKey());
            Point2D p2 = positionsMaisons.get(e2.getKey());
            if (p1 == null || p2 == null) return 0;
            return Double.compare(p1.getY(), p2.getY());
        });

        for (Map.Entry<String, Maison> entry : maisons) {
            String nom = entry.getKey();
            Maison maison = entry.getValue();
            Point2D posReelle = positionsMaisons.get(nom);

            if (posReelle == null) continue;

            Point2D pos = transformerPoint(posReelle);
            double x = pos.getX();
            double y = pos.getY();
            double taille = NODE_SIZE * zoom;

            // Verifier si connectee
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

            // Nom (au-dessus)
            gc.setFill(Color.web("#2c3e50"));
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 13 * zoom));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(nom, x, y - taille/2 - 12 * zoom);

            // Consommation (en dessous)
            String conso = maison.getConsommationKw() + " kW";
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 11 * zoom));
            gc.setFill(Color.web("#7f8c8d"));
            gc.fillText(conso, x, y + taille/2 + 22 * zoom);

            // Indicateur de connexion
            if (!connectee) {
                double indicateurX = x + taille/2 - 8 * zoom;
                double indicateurY = y - taille/2 + 8 * zoom;
                double indicateurSize = 18 * zoom;

                gc.setFill(Color.web("#e67e22"));
                gc.fillOval(indicateurX - indicateurSize/2, indicateurY - indicateurSize/2,
                        indicateurSize, indicateurSize);

                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 14 * zoom));
                gc.fillText("!", indicateurX, indicateurY + 4 * zoom);
            }
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

    private void dessinerLegende() {
        double x = 20;
        double y = 20;
        double largeur = 200;
        double hauteur = 120;

        // Fond
        gc.setFill(Color.web("#ffffff", 0.9));
        gc.fillRoundRect(x, y, largeur, hauteur, 10, 10);
        gc.setStroke(Color.web("#bdc3c7"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, largeur, hauteur, 10, 10);

        // Titre
        gc.setFill(Color.web("#2c3e50"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Legende", x + 10, y + 25);

        // Items
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 11));

        gc.setFill(Color.web("#f39c12"));
        gc.fillOval(x + 10, y + 40, 15, 15);
        gc.setFill(Color.web("#2c3e50"));
        gc.fillText("Generateur", x + 35, y + 52);

        gc.setFill(Color.web("#2ecc71"));
        gc.fillRect(x + 10, y + 60, 15, 15);
        gc.setFill(Color.web("#2c3e50"));
        gc.fillText("Maison (Basse)", x + 35, y + 72);

        gc.setFill(Color.web("#3498db"));
        gc.fillRect(x + 10, y + 80, 15, 15);
        gc.setFill(Color.web("#2c3e50"));
        gc.fillText("Maison (Normale)", x + 35, y + 92);

        gc.setFill(Color.web("#9b59b6"));
        gc.fillRect(x + 10, y + 100, 15, 15);
        gc.setFill(Color.web("#2c3e50"));
        gc.fillText("Maison (Forte)", x + 35, y + 112);
    }

    // ============ GESTION DES EVENEMENTS ============

    /**
     * CORRECTION CRITIQUE: Gestion du clic en mode placement
     */
    private void handleMousePressed(MouseEvent e) {
        dernierClic = new Point2D(e.getX(), e.getY());

        // MODE PLACEMENT: L'utilisateur clique pour placer l'element
        if (modeAttenteClic && callbackPosition != null) {
            // Convertir les coordonnees ecran en coordonnees reelles (monde)
            Point2D posReel = inverserTransformation(dernierClic);

            // Appeler le callback avec la position
            callbackPosition.accept(posReel);

            // Desactiver le mode placement
            modeAttenteClic = false;
            callbackPosition = null;
            canvas.setCursor(javafx.scene.Cursor.DEFAULT);

            System.out.println("Element place a: " + posReel);
            return;
        }

        // Mode normal: Selection d'un element
        elementSelectionne = trouverElementSousPointeur(dernierClic);

        if (elementSelectionne != null) {
            dragEnCours = true;
        }

        dessiner();
    }

    private void handleMouseDragged(MouseEvent e) {
        if (dragEnCours && elementSelectionne != null) {
            // Deplacer l'element selectionne
            Point2D nouvellePos = inverserTransformation(new Point2D(e.getX(), e.getY()));

            if (estGenerateur) {
                positionsGenerateurs.put(elementSelectionne, nouvellePos);
            } else {
                positionsMaisons.put(elementSelectionne, nouvellePos);
            }

            dessiner();
        } else if (dernierClic != null && !modeAttenteClic) {
            // Deplacer la vue
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
            // Zoomer vers la souris
            double mouseX = e.getX();
            double mouseY = e.getY();

            offsetX = mouseX - (mouseX - offsetX) * facteur;
            offsetY = mouseY - (mouseY - offsetY) * facteur;

            zoom = nouveauZoom;
            dessiner();
        }
    }

    private void handleMouseMoved(MouseEvent e) {
        if (modeAttenteClic) {
            canvas.setCursor(javafx.scene.Cursor.CROSSHAIR);
            return;
        }

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

        // Verifier generateurs
        for (Map.Entry<String, Point2D> entry : positionsGenerateurs.entrySet()) {
            Point2D pos = entry.getValue();
            if (pos.distance(posInversee) < taille) {
                estGenerateur = true;
                return entry.getKey();
            }
        }

        // Verifier maisons
        for (Map.Entry<String, Point2D> entry : positionsMaisons.entrySet()) {
            Point2D pos = entry.getValue();
            if (pos.distance(posInversee) < taille) {
                estGenerateur = false;
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * CORRECTION: Transformation des coordonnees reelles vers l'ecran
     */
    private Point2D transformerPoint(Point2D point) {
        if (point == null) return null;
        return new Point2D(
                point.getX() * zoom + offsetX,
                point.getY() * zoom + offsetY
        );
    }

    /**
     * CORRECTION: Transformation inverse (ecran vers coordonnees reelles)
     */
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