package main.View;

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
import main.Controller.AppController;
import main.Model.*;

import java.util.*;

/**
 * Panneau de visualisation du graphe du reseau electrique
 */
public class NetworkGraphPane extends Pane {

    // Reference vers le cerveau de l'application pour recuperer les donnees du
    // reseau
    private AppController controller;

    // La zone de dessin (la toile) ou tout sera affiche
    private final Canvas canvas;

    // L'outil (le pinceau) qui permet de dessiner des formes sur la toile
    private final GraphicsContext gc;

    // Le decalage horizontal de la camera (pour se deplacer a gauche ou a droite)
    private double offsetX = 0;

    // Le decalage vertical de la camera (pour se deplacer en haut ou en bas)
    private double offsetY = 0;

    // Le niveau de grossissement actuel de la vue (1.0 = normal)
    private double zoom = 1.0;

    // La limite minimale pour dezoomer (voir de loin)
    private final double MIN_ZOOM = 0.3;

    // La limite maximale pour zoomer (voir de pres)
    private final double MAX_ZOOM = 3.0;

    // Memoire des positions exactes de chaque generateur sur le plan
    private final Map<String, Point2D> positionsGenerateurs = new HashMap<>();

    // Memoire des positions exactes de chaque maison sur le plan
    private final Map<String, Point2D> positionsMaisons = new HashMap<>();

    // Memorise ou la souris a clique pour la derniere fois
    private Point2D dernierClic;

    // Le nom de l'objet (maison ou generateur) actuellement sous la souris ou en
    // cours de deplacement
    private String elementSelectionne = null;

    // Permet de savoir si l'objet selectionne est un generateur (vrai) ou une
    // maison (faux)
    private boolean estGenerateur = false;

    // Indique si l'utilisateur est en train de glisser la souris pour deplacer
    // quelque chose
    private boolean dragEnCours = false;

    // Indique si le programme attend que l'utilisateur clique quelque part pour
    // poser un objet
    private boolean modeAttenteClic = false;

    // Une fonction a executer une fois que l'utilisateur a clique pour donner la
    // position
    private java.util.function.Consumer<Point2D> callbackPosition;

    // Indique si l'objet qu'on veut placer est un generateur
    private boolean estGenerateurEnPlacement = false;

    // La taille standard des ronds et des carres dessines
    private static final double NODE_SIZE = 50;

    // Initialise le panneau, cree la toile de dessin et configure les actions de la
    // souris
    public NetworkGraphPane(AppController controller) {
        this.controller = controller;

        canvas = new Canvas();
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());
        gc = canvas.getGraphicsContext2D();

        getChildren().add(canvas);

        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        canvas.setOnScroll(this::handleScroll);
        canvas.setOnMouseMoved(this::handleMouseMoved);

        canvas.widthProperty().addListener((obs, old, val) -> dessiner());
        canvas.heightProperty().addListener((obs, old, val) -> dessiner());
    }

    // Permet de changer le controleur lie a ce panneau
    public void setController(AppController controller) {
        this.controller = controller;
    }

    // Prepare le panneau pour que le prochain clic serve a definir la position d'un
    // objet
    public void attendreClicPourPosition(java.util.function.Consumer<Point2D> callback, boolean estGen) {
        this.modeAttenteClic = true;
        this.callbackPosition = callback;
        this.estGenerateurEnPlacement = estGen;
        canvas.setCursor(javafx.scene.Cursor.CROSSHAIR);
    }

    // Enregistre manuellement la position d'un generateur donne
    public void setPositionGenerateur(String nom, Point2D pos) {
        positionsGenerateurs.put(nom, pos);
        dessiner();
    }

    // Enregistre manuellement la position d'une maison donnee
    public void setPositionMaison(String nom, Point2D pos) {
        positionsMaisons.put(nom, pos);
        dessiner();
    }

    // Force la mise a jour de l'affichage, en s'assurant que cela se fait au bon
    // moment
    public void rafraichir() {
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

    // Remet la vue a zero (zoom normal, centre a l'origine)
    public void recentrerVue() {
        offsetX = 0;
        offsetY = 0;
        zoom = 1.0;
        dessiner();
    }

    // Donne une position automatique par defaut aux objets qui n'en ont pas encore
    private void calculerPositionsManquantes() {
        Reseau reseau = controller.getReseau();

        double largeur = canvas.getWidth();
        double hauteur = canvas.getHeight();

        double margeHaut = 80;
        double margeBas = 80;
        double hauteurUtile = hauteur - margeHaut - margeBas;

        double espacementMin = 120;

        double xGenerateurs = largeur * 0.25;
        double xMaisons = largeur * 0.75;

        List<String> gensSansPosition = new ArrayList<>();
        for (String nom : reseau.getGenerateurs().keySet()) {
            if (!positionsGenerateurs.containsKey(nom)) {
                gensSansPosition.add(nom);
            }
        }

        if (!gensSansPosition.isEmpty()) {
            int nbGen = gensSansPosition.size();
            double espacementSouhaite = Math.max(espacementMin, hauteurUtile / (nbGen + 1));

            if (nbGen > 1) {
                espacementSouhaite = Math.max(espacementMin, hauteurUtile / (nbGen - 1));
            }

            for (int i = 0; i < nbGen; i++) {
                double y;
                if (nbGen == 1) {
                    y = hauteur / 2;
                } else {
                    y = margeHaut + (i * espacementSouhaite);
                }
                positionsGenerateurs.put(gensSansPosition.get(i), new Point2D(xGenerateurs, y));
            }
        }

        List<String> maisonsSansPosition = new ArrayList<>();
        for (String nom : reseau.getMaisons().keySet()) {
            if (!positionsMaisons.containsKey(nom)) {
                maisonsSansPosition.add(nom);
            }
        }

        if (!maisonsSansPosition.isEmpty()) {
            int nbMaisons = maisonsSansPosition.size();
            double espacementSouhaite = Math.max(espacementMin, hauteurUtile / (nbMaisons + 1));

            if (nbMaisons > 1) {
                espacementSouhaite = Math.max(espacementMin, hauteurUtile / (nbMaisons - 1));
            }

            for (int i = 0; i < nbMaisons; i++) {
                double y;
                if (nbMaisons == 1) {
                    y = hauteur / 2;
                } else {
                    y = margeHaut + (i * espacementSouhaite);
                }
                positionsMaisons.put(maisonsSansPosition.get(i), new Point2D(xMaisons, y));
            }
        }
    }

    // Efface tout et redessine l'ensemble du reseau (fond, grille, liens, objets,
    // legende)
    private void dessiner() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.web("#ecf0f1"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        dessinerGrille();

        dessinerConnexions();

        dessinerGenerateurs();
        dessinerMaisons();

        dessinerLegende();
    }

    // Trace une grille legere en fond pour aider a se reperer
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

    // Parcourt toutes les connexions et dessine les cables electriques
    private void dessinerConnexions() {
        Reseau reseau = controller.getReseau();
        Map<String, List<String>> connexions = reseau.getConnexions();

        for (Map.Entry<String, List<String>> entry : connexions.entrySet()) {
            String nomMaison = entry.getKey();
            Point2D posMaison = positionsMaisons.get(nomMaison);

            if (posMaison == null)
                continue;

            for (String nomGen : entry.getValue()) {
                Point2D posGen = positionsGenerateurs.get(nomGen);
                if (posGen == null)
                    continue;

                Point2D p1 = transformerPoint(posGen);
                Point2D p2 = transformerPoint(posMaison);

                Generateur gen = reseau.getGenerateurs().get(nomGen);
                int charge = calculerChargeGenerateur(reseau, nomGen);

                if (charge > gen.getCapaciteMax()) {
                    gc.setStroke(Color.web("#e74c3c"));
                    gc.setLineWidth(3.5 * zoom);
                } else {
                    gc.setStroke(Color.web("#3498db", 0.7));
                    gc.setLineWidth(2.5 * zoom);
                }

                dessinerLigneAvecFleche(p1, p2);
            }
        }
    }

    // Trace une ligne avec une fleche au milieu pour indiquer la direction
    private void dessinerLigneAvecFleche(Point2D debut, Point2D fin) {
        double startX = debut.getX();
        double startY = debut.getY();
        double endX = fin.getX();
        double endY = fin.getY();

        gc.strokeLine(startX, startY, endX, endY);

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

    // Dessine tous les generateurs (ronds) avec leurs informations
    private void dessinerGenerateurs() {
        Reseau reseau = controller.getReseau();

        List<Map.Entry<String, Generateur>> generateurs = new ArrayList<>(reseau.getGenerateurs().entrySet());
        generateurs.sort((e1, e2) -> {
            Point2D p1 = positionsGenerateurs.get(e1.getKey());
            Point2D p2 = positionsGenerateurs.get(e2.getKey());
            if (p1 == null || p2 == null)
                return 0;
            return Double.compare(p1.getY(), p2.getY());
        });

        for (Map.Entry<String, Generateur> entry : generateurs) {
            String nom = entry.getKey();
            Generateur gen = entry.getValue();
            Point2D posReelle = positionsGenerateurs.get(nom);

            if (posReelle == null)
                continue;

            Point2D pos = transformerPoint(posReelle);
            double x = pos.getX();
            double y = pos.getY();
            double taille = NODE_SIZE * zoom;

            int charge = calculerChargeGenerateur(reseau, nom);
            boolean surcharge = charge > gen.getCapaciteMax();

            gc.setFill(Color.web("#000000", 0.2));
            gc.fillOval(x - taille / 2 + 3, y - taille / 2 + 3, taille, taille);

            Color couleurPrincipale = surcharge ? Color.web("#e74c3c") : Color.web("#f39c12");
            gc.setFill(couleurPrincipale);
            gc.fillOval(x - taille / 2, y - taille / 2, taille, taille);

            gc.setStroke(nom.equals(elementSelectionne) && estGenerateur ? Color.web("#2c3e50") : Color.web("#d68910"));
            gc.setLineWidth(3 * zoom);
            gc.strokeOval(x - taille / 2, y - taille / 2, taille, taille);

            gc.setFill(Color.web("#2c3e50"));
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 14 * zoom));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(nom, x, y - taille / 2 - 12 * zoom);

            String capacite = String.format("%d/%d kW", charge, gen.getCapaciteMax());
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12 * zoom));
            gc.setFill(surcharge ? Color.web("#c0392b") : Color.web("#27ae60"));
            gc.fillText(capacite, x, y + taille / 2 + 22 * zoom);

            int nbConnexions = 0;
            for (List<String> gens : reseau.getConnexions().values()) {
                if (gens.contains(nom))
                    nbConnexions++;
            }
            if (nbConnexions > 0) {
                double badgeX = x + taille / 2 - 8 * zoom;
                double badgeY = y - taille / 2 + 8 * zoom;
                double badgeSize = 18 * zoom;

                gc.setFill(Color.web("#e74c3c"));
                gc.fillOval(badgeX - badgeSize / 2, badgeY - badgeSize / 2, badgeSize, badgeSize);

                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 10 * zoom));
                gc.fillText(String.valueOf(nbConnexions), badgeX, badgeY + 3 * zoom);
            }
        }
    }

    // Dessine toutes les maisons (carres) avec leurs informations
    private void dessinerMaisons() {
        Reseau reseau = controller.getReseau();

        List<Map.Entry<String, Maison>> maisons = new ArrayList<>(reseau.getMaisons().entrySet());
        maisons.sort((e1, e2) -> {
            Point2D p1 = positionsMaisons.get(e1.getKey());
            Point2D p2 = positionsMaisons.get(e2.getKey());
            if (p1 == null || p2 == null)
                return 0;
            return Double.compare(p1.getY(), p2.getY());
        });

        for (Map.Entry<String, Maison> entry : maisons) {
            String nom = entry.getKey();
            Maison maison = entry.getValue();
            Point2D posReelle = positionsMaisons.get(nom);

            if (posReelle == null)
                continue;

            Point2D pos = transformerPoint(posReelle);
            double x = pos.getX();
            double y = pos.getY();
            double taille = NODE_SIZE * zoom;

            boolean connectee = reseau.connexionExistePourMaison(nom);

            gc.setFill(Color.web("#000000", 0.2));
            gc.fillRect(x - taille / 2 + 3, y - taille / 2 + 3, taille, taille);

            Color couleurPrincipale = getCouleurMaison(maison.getConsommation());
            gc.setFill(couleurPrincipale);
            gc.fillRect(x - taille / 2, y - taille / 2, taille, taille);

            Color couleurBordure = nom.equals(elementSelectionne) && !estGenerateur ? Color.web("#2c3e50")
                    : (connectee ? Color.web("#16a085") : Color.web("#e67e22"));
            gc.setStroke(couleurBordure);
            gc.setLineWidth(3 * zoom);
            gc.strokeRect(x - taille / 2, y - taille / 2, taille, taille);

            gc.setFill(Color.web("#2c3e50"));
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 13 * zoom));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(nom, x, y - taille / 2 - 12 * zoom);

            String conso = maison.getConsommationKw() + " kW";
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 11 * zoom));
            gc.setFill(Color.web("#7f8c8d"));
            gc.fillText(conso, x, y + taille / 2 + 22 * zoom);

            if (!connectee) {
                double indicateurX = x + taille / 2 - 8 * zoom;
                double indicateurY = y - taille / 2 + 8 * zoom;
                double indicateurSize = 18 * zoom;

                gc.setFill(Color.web("#e67e22"));
                gc.fillOval(indicateurX - indicateurSize / 2, indicateurY - indicateurSize / 2,
                        indicateurSize, indicateurSize);

                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 14 * zoom));
                gc.fillText("!", indicateurX, indicateurY + 4 * zoom);
            }
        }
    }

    // Retourne la couleur associee a un niveau de consommation
    private Color getCouleurMaison(Consommation conso) {
        switch (conso) {
            case BASSE:
                return Color.web("#2ecc71");
            case NORMAL:
                return Color.web("#3498db");
            case FORTE:
                return Color.web("#9b59b6");
            default:
                return Color.GRAY;
        }
    }

    // Affiche la legende explicative sur le canvas
    private void dessinerLegende() {
        double x = 20;
        double y = 20;
        double largeur = 200;
        double hauteur = 120;

        gc.setFill(Color.web("#ffffff", 0.9));
        gc.fillRoundRect(x, y, largeur, hauteur, 10, 10);
        gc.setStroke(Color.web("#bdc3c7"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, largeur, hauteur, 10, 10);

        gc.setFill(Color.web("#2c3e50"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Legende", x + 10, y + 25);

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

    // Gere le clic de la souris (selection ou placement)
    private void handleMousePressed(MouseEvent e) {
        dernierClic = new Point2D(e.getX(), e.getY());

        if (modeAttenteClic && callbackPosition != null) {
            Point2D posReel = inverserTransformation(dernierClic);
            callbackPosition.accept(posReel);

            modeAttenteClic = false;
            callbackPosition = null;
            canvas.setCursor(javafx.scene.Cursor.DEFAULT);
            return;
        }

        elementSelectionne = trouverElementSousPointeur(dernierClic);

        if (elementSelectionne != null) {
            dragEnCours = true;
        }

        dessiner();
    }

    // Gere le deplacement de la souris (drag de la vue ou d'un element)
    private void handleMouseDragged(MouseEvent e) {
        if (dragEnCours && elementSelectionne != null) {
            Point2D nouvellePos = inverserTransformation(new Point2D(e.getX(), e.getY()));

            if (estGenerateur) {
                positionsGenerateurs.put(elementSelectionne, nouvellePos);
            } else {
                positionsMaisons.put(elementSelectionne, nouvellePos);
            }

            dessiner();
        } else if (dernierClic != null && !modeAttenteClic) {
            offsetX += e.getX() - dernierClic.getX();
            offsetY += e.getY() - dernierClic.getY();
            dernierClic = new Point2D(e.getX(), e.getY());
            dessiner();
        }
    }

    // Gere le relachement du bouton de la souris (fin du drag)
    private void handleMouseReleased(MouseEvent e) {
        dragEnCours = false;
        dernierClic = null;
    }

    // Gere le zoom avec la molette de la souris
    private void handleScroll(ScrollEvent e) {
        double facteur = e.getDeltaY() > 0 ? 1.1 : 0.9;
        double nouveauZoom = zoom * facteur;

        if (nouveauZoom >= MIN_ZOOM && nouveauZoom <= MAX_ZOOM) {
            double mouseX = e.getX();
            double mouseY = e.getY();

            offsetX = mouseX - (mouseX - offsetX) * facteur;
            offsetY = mouseY - (mouseY - offsetY) * facteur;

            zoom = nouveauZoom;
            dessiner();
        }
    }

    // Change le curseur selon ce qui se trouve sous la souris
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

    // Identifie quel element se trouve sous une position donnee
    private String trouverElementSousPointeur(Point2D pointeur) {
        Point2D posInversee = inverserTransformation(pointeur);
        double taille = NODE_SIZE / 2;

        for (Map.Entry<String, Point2D> entry : positionsGenerateurs.entrySet()) {
            Point2D pos = entry.getValue();
            if (pos.distance(posInversee) < taille) {
                estGenerateur = true;
                return entry.getKey();
            }
        }

        for (Map.Entry<String, Point2D> entry : positionsMaisons.entrySet()) {
            Point2D pos = entry.getValue();
            if (pos.distance(posInversee) < taille) {
                estGenerateur = false;
                return entry.getKey();
            }
        }

        return null;
    }

    // Convertit des coordonnees reelles en coordonnees ecran (avec zoom et
    // decalage)
    private Point2D transformerPoint(Point2D point) {
        if (point == null)
            return null;
        return new Point2D(
                point.getX() * zoom + offsetX,
                point.getY() * zoom + offsetY);
    }

    // Convertit des coordonnees ecran en coordonnees reelles (inverse du zoom et
    // decalage)
    private Point2D inverserTransformation(Point2D point) {
        return new Point2D(
                (point.getX() - offsetX) / zoom,
                (point.getY() - offsetY) / zoom);
    }

    // Calcule la charge actuelle d'un generateur en sommant la consommation de ses
    // maisons connectees
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