package View;

import Exceptions.ReseauInvalideException;
import Exceptions.ReseauInvalideSyntaxException;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import static Model.Constants.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

import Algorithms.GeneticAlgorithm;
import Algorithms.FileAlgorithms;
import Controller.AppController;
import Exceptions.InvalidFileExtensionException;
import Exceptions.InvalidNameException;
import Model.*;

/**
 * Interface graphique principale de l'application.
 * C'est ici que l'on construit la fenetre, les menus et que l'on gere les
 * interactions
 * de l'utilisateur (clics sur les boutons, boites de dialogue, etc.).
 */
public class GraphicalView extends Application {

    // Le cerveau de l'application qui fait le lien avec les donnees
    private AppController controller;

    // La zone de dessin ou s'affiche le reseau
    private NetworkGraphPane graphPane;

    // Les etiquettes de texte pour afficher des messages en bas ou a droite
    private Label statusLabel;
    private Label coutLabel;

    // La fenetre principale
    private Stage primaryStage;

    // Permet de savoir dans quelle etape on se trouve :
    // 1 = Construction manuelle
    // 2 = Analyse et calculs
    // 3 = Mode fichier charge
    private int menuActuel = 1;

    // Etiquettes pour afficher les statistiques dans le panneau de droite
    private Label infoGenerateurs;
    private Label infoMaisons;
    private Label infoConnexions;
    private Label infoMenu;

    // Reference vers le panneau d'actions de droite pour pouvoir le desactiver
    private VBox actionsBox;

    // Point d'entree de l'application JavaFX. C'est la premiere methode appelee.
    // Elle prepare la fenetre, charge le CSS et initialise l'affichage.
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        Parameters params = getParameters();
        List<String> args = params.getRaw();

        Reseau reseau = new Reseau();
        controller = new AppController(reseau);
        menuActuel = 1;

        BorderPane root = new BorderPane();
        root.setTop(creerBarreTitre());
        root.setCenter(creerCentrePane());
        root.setBottom(creerBarreStatut());
        root.setRight(creerPanneauControle());

        Scene scene = new Scene(root, 1400, 900);

        try {
            String css = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            // Style par defaut
        }

        stage.setTitle("Reseau Electrique - Projet Programmation Avancee");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        if (!args.isEmpty()) {
            String cheminFichier = args.get(0);
            String lambdaStr = args.size() > 1 ? args.get(1) : String.valueOf(LAMBDA);
            ;

            javafx.application.Platform.runLater(() -> {
                if (chargerReseauDepuisFichier(cheminFichier, lambdaStr)) {
                    menuActuel = 3;
                    mettreAJourMenuInfo();

                    if (actionsBox != null) {
                        mettreAJourActions(actionsBox);
                    }
                    rafraichirCoutLabel();
                } else {
                    // Echec du chargement, on quitte l'application
                    javafx.application.Platform.exit();
                }
            });
        } else {
            // Pas de fichier en argument, on reste en mode construction manuelle
            rafraichirGraphe();
            rafraichirInfos();
            mettreAJourMenuInfo();
        }
    }

    // Cree la barre foncee tout en haut avec le titre du projet
    private VBox creerBarreTitre() {
        VBox barre = new VBox(5);
        barre.setStyle("-fx-background-color: #34495e; -fx-padding: 15;");

        Label titre = new Label("DISTRO - RESEAU DE DISTRIBUTION D'ELECTRICITE");
        titre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        infoMenu = new Label();
        infoMenu.setStyle("-fx-font-size: 13px; -fx-text-fill: #ecf0f1;");

        barre.getChildren().addAll(titre, infoMenu);
        return barre;
    }

    // Initialise la zone centrale qui contiendra le dessin du reseau
    private Pane creerCentrePane() {
        graphPane = new NetworkGraphPane(controller);
        graphPane.setStyle("-fx-background-color: #f5f5f5;");
        return graphPane;
    }

    // Construit le panneau vertical a droite qui contient les boutons d'action
    // et les statistiques (nombre de maisons, etc.)
    private VBox creerPanneauControle() {
        VBox panneau = new VBox(15);
        panneau.setPadding(new Insets(20));
        panneau.setStyle("-fx-background-color: #2c3e50; -fx-min-width: 300px;");

        Label titre = new Label("Panneau de Controle");
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Informations reseau
        VBox infoBox = new VBox(5);
        infoBox.setStyle("-fx-background-color: #34495e; -fx-padding: 10; -fx-background-radius: 5;");

        Label lblInfo = new Label("Informations Reseau");
        lblInfo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        infoGenerateurs = new Label("Generateurs: 0");
        infoMaisons = new Label("Maisons: 0");
        infoConnexions = new Label("Connexions: 0");

        infoGenerateurs.setStyle("-fx-text-fill: #ecf0f1;");
        infoMaisons.setStyle("-fx-text-fill: #ecf0f1;");
        infoConnexions.setStyle("-fx-text-fill: #ecf0f1;");

        infoBox.getChildren().addAll(lblInfo, new Separator(),
                infoGenerateurs, infoMaisons, infoConnexions);

        Separator sep1 = new Separator();

        // Section Actions
        actionsBox = new VBox(10);
        actionsBox.setId("actionsBox");

        mettreAJourActions(actionsBox);

        panneau.getChildren().addAll(
                titre, new Separator(),
                infoBox, sep1,
                actionsBox);

        return panneau;
    }

    // Met a jour le texte en haut pour dire a l'utilisateur dans quel menu il se
    // trouve
    private void mettreAJourMenuInfo() {
        switch (menuActuel) {
            case 1:
                infoMenu.setText("MENU 1 - Configuration du Reseau (Construction Manuelle)");
                break;
            case 2:
                infoMenu.setText("MENU 2 - Analyse du Reseau");
                break;
            case 3:
                // infoMenu.setText("MENU 3 - Reseau charge ou valide");
                infoMenu.setText("MENU 3 - Menu d'Optimisation et de Sauvegarde");
                break;
        }
    }

    // Change les boutons disponibles a droite selon l'etape en cours (Menu 1, 2 ou
    // 3)
    private void mettreAJourActions(VBox actionsBox) {
        actionsBox.getChildren().clear();

        Label lblActions = new Label("Actions Disponibles");
        lblActions.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");
        actionsBox.getChildren().add(lblActions);

        if (menuActuel == 1) {
            // MENU 1 : Construction
            Button btn1 = creerBoutonStyleControle("1) Ajouter Generateur");
            btn1.setOnAction(e -> ajouterGenerateur());

            Button btn2 = creerBoutonStyleControle("2) Ajouter Maison");
            btn2.setOnAction(e -> ajouterMaison());

            Button btn3 = creerBoutonStyleControle("3) Ajouter Connexion");
            btn3.setOnAction(e -> ajouterConnexion());

            Button btn4 = creerBoutonStyleControle("4) Supprimer Connexion");
            btn4.setOnAction(e -> supprimerConnexion());

            Button btn5 = new Button("5) Fin (Valider et passer au Menu 2)");
            btn5.setMaxWidth(Double.MAX_VALUE);
            btn5.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                    "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;");

            btn5.setOnMouseEntered(e -> btn5.setStyle("-fx-background-color: #219150; -fx-text-fill: white; " +
                    "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;"));
            btn5.setOnMouseExited(e -> btn5.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                    "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;"));

            btn5.setOnAction(e -> finMenu1());

            actionsBox.getChildren().addAll(btn1, btn2, btn3, btn4, new Separator(), btn5);

        } else if (menuActuel == 2) {
            // MENU 2 : Analyse
            Button btn1 = creerBoutonStyleControle("1) Calculer le Cout");
            btn1.setOnAction(e -> calculerCout());

            Button btn2 = creerBoutonStyleControle("2) Modifier une Connexion");
            btn2.setOnAction(e -> modifierConnexion());

            Button btn3 = creerBoutonStyleControle("3) Afficher le Reseau");
            btn3.setOnAction(e -> afficherReseau());

            Button btn4 = new Button("4) Fin (Quitter)");
            btn4.setMaxWidth(Double.MAX_VALUE);
            btn4.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                    "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;");

            btn4.setOnMouseEntered(e -> btn4.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; " +
                    "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;"));
            btn4.setOnMouseExited(e -> btn4.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                    "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;"));

            btn4.setOnAction(e -> confirmerQuitter());

            actionsBox.getChildren().addAll(btn1, btn2, btn3, new Separator(), btn4);

        } else if (menuActuel == 3) {
            // MENU 3 : Fichier charge
            Button btn0 = creerBoutonStyleControle("Afficher le Cout Actuel en Detail");
            btn0.setOnAction(e -> calculerCout());

            Button btn1 = creerBoutonStyleControle("1) Resolution Automatique");
            btn1.setOnAction(e -> resolutionAutomatique());

            Button btn2 = creerBoutonStyleControle("2) Sauvegarder Solution");
            btn2.setOnAction(e -> sauvegarderSolution());

            Button btn3 = new Button("3) Fin (Quitter)");
            btn3.setMaxWidth(Double.MAX_VALUE);
            btn3.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                    "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;");

            btn3.setOnMouseEntered(e -> btn3.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; " +
                    "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;"));
            btn3.setOnMouseExited(e -> btn3.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                    "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;"));

            btn3.setOnAction(e -> confirmerQuitter());

            actionsBox.getChildren().addAll(btn0, new Separator(), btn1, btn2, new Separator(), btn3);
        }
    }

    // MENU 1 : CONSTRUCTION -----------------------------------------------------

    // Ouvre une petite fenêtre pour demander les infos d'un nouveau generateur
    // Si valide, demande a l'utilisateur de cliquer sur le graphe pour le placer
    private void ajouterGenerateur() {
        Dialog<Generateur> dialog = new Dialog<>();
        dialog.setTitle("Menu 1 - Option 1");
        dialog.setHeaderText("Ajouter un Generateur");

        ButtonType btnAjouter = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAjouter, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nomField = new TextField();
        nomField.setPromptText("ex: G1");
        TextField capaciteField = new TextField();
        capaciteField.setPromptText("ex: 60");

        grid.add(new Label("Nom du generateur:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Capacite maximale (kW):"), 0, 1);
        grid.add(capaciteField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAjouter) {
                try {
                    String nom = nomField.getText().trim();
                    validerNomAlphanumerique(nom, "generateur");

                    int capacite = Integer.parseInt(capaciteField.getText().trim());
                    if (capacite <= 0) {
                        afficherErreur("Erreur", "La capacite doit etre un nombre positif.");
                        return null;
                    }

                    return new Generateur(nom, capacite);
                } catch (InvalidNameException e) {
                    afficherErreur("Erreur", e.getMessage());
                    return null;
                } catch (NumberFormatException e) {
                    afficherErreur("Erreur", "La capacite doit etre un nombre entier valide.");
                    return null;
                }
            }
            return null;
        });

        Optional<Generateur> result = dialog.showAndWait();
        result.ifPresent(gen -> {
            if (controller.getReseau().generateurExiste(gen.getNom())) {
                try {
                    controller.addGenerateur(gen.getNom(), gen.getCapaciteMax());
                    rafraichirGraphe();
                    rafraichirInfos();
                } catch (InvalidNameException e) {
                    afficherErreur("Nom invalide", e.getMessage());
                    return;
                }

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Modification Generateur");
                alert.setHeaderText("Generateur existant");
                alert.setContentText("Le generateur '" + gen.getNom() + "' existe deja.\n"
                        + "Sa capacite a ete mise a jour a " + gen.getCapaciteMax() + " kW.");
                alert.showAndWait();

                statusLabel.setText("AVERTISSEMENT: Le generateur " + gen.getNom() + " a ete mis a jour.");
            } else {
                statusLabel.setText("Cliquez sur le graphe pour placer le generateur " + gen.getNom());
                setActionsDisabled(true);
                graphPane.attendreClicPourPosition(pos -> {
                    try {
                        controller.addGenerateur(gen.getNom(), gen.getCapaciteMax());
                        graphPane.setPositionGenerateur(gen.getNom(), pos);
                        rafraichirGraphe();
                        rafraichirInfos();
                        setActionsDisabled(false);
                        statusLabel.setText("INFO: Generateur " + gen.getNom() + " ajoute.");
                    } catch (InvalidNameException e) {
                        setActionsDisabled(false);
                        afficherErreur("Nom invalide", e.getMessage());
                        statusLabel.setText("ERREUR: " + e.getMessage());
                    }
                }, true);
            }
        });
    }

    // Ouvre une fenetre pour creer une maison (Nom et Type de consommation)
    // Puis demande de la placer sur le graphe
    private void ajouterMaison() {
        Dialog<Maison> dialog = new Dialog<>();
        dialog.setTitle("Menu 1 - Option 2");
        dialog.setHeaderText("Ajouter une Maison");

        ButtonType btnAjouter = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAjouter, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nomField = new TextField();
        nomField.setPromptText("ex: M1");

        ComboBox<String> consoCombo = new ComboBox<>();
        consoCombo.getItems().addAll("BASSE", "NORMAL", "FORTE");
        consoCombo.setValue("NORMAL");

        grid.add(new Label("Nom de la maison:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Type de consommation:"), 0, 1);
        grid.add(consoCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAjouter) {
                String nom = nomField.getText().trim();
                try {
                    validerNomAlphanumerique(nom, "maison");
                } catch (InvalidNameException e) {
                    afficherErreur("Erreur", e.getMessage());
                    return null;
                }

                Consommation conso = Consommation.fromString(consoCombo.getValue());
                return new Maison(nom, conso);
            }
            return null;
        });

        Optional<Maison> result = dialog.showAndWait();
        result.ifPresent(maison -> {
            try {
                if (controller.getReseau().maisonExiste(maison.getNom())) {
                    controller.addMaison(maison.getNom(), maison.getConsommation().name());
                    rafraichirGraphe();
                    rafraichirInfos();

                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Modification Maison");
                    alert.setHeaderText("Maison existante");
                    alert.setContentText("La maison '" + maison.getNom() + "' existe deja.\n"
                            + "Sa consommation a ete mise a jour (" + maison.getConsommation().name() + ").");
                    alert.showAndWait();

                    statusLabel.setText("AVERTISSEMENT: La maison " + maison.getNom() + " a ete mise a jour.");
                } else {
                    statusLabel.setText("Cliquez sur le graphe pour placer la maison " + maison.getNom());
                    setActionsDisabled(true);
                    graphPane.attendreClicPourPosition(pos -> {
                        try {
                            controller.addMaison(maison.getNom(), maison.getConsommation().name());
                            graphPane.setPositionMaison(maison.getNom(), pos);
                            rafraichirGraphe();
                            rafraichirInfos();
                            setActionsDisabled(false);
                            statusLabel.setText("INFO: Maison " + maison.getNom() + " ajoutee.");
                        } catch (IllegalArgumentException e) {
                            setActionsDisabled(false);
                            afficherErreur("Erreur", e.getMessage());
                            statusLabel.setText("ERREUR: " + e.getMessage());
                        }
                    }, false);
                }
            } catch (IllegalArgumentException e) {
                afficherErreur("Erreur", e.getMessage());
            }
        });
    }

    // Permet de relier une maison a un generateur via une boite de dialogue
    private void ajouterConnexion() {
        Reseau reseau = controller.getReseau();

        if (reseau.getMaisons().isEmpty() || reseau.getGenerateurs().isEmpty()) {
            afficherErreur("Connexion impossible",
                    "Il faut au moins une maison et un generateur.\n" +
                            "Veuillez d'abord creer des elements (options 1 et 2).");
            return;
        }

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Menu 1 - Option 3");
        dialog.setHeaderText("Creer une Connexion");

        ButtonType btnCreer = new ButtonType("Creer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnCreer, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> maisonCombo = new ComboBox<>();
        maisonCombo.getItems().addAll(reseau.getMaisons().keySet());
        maisonCombo.setPromptText("Selectionner une maison");

        ComboBox<String> genCombo = new ComboBox<>();
        genCombo.getItems().addAll(reseau.getGenerateurs().keySet());
        genCombo.setPromptText("Selectionner un generateur");

        grid.add(new Label("Maison:"), 0, 0);
        grid.add(maisonCombo, 1, 0);
        grid.add(new Label("Generateur:"), 0, 1);
        grid.add(genCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnCreer) {
                if (maisonCombo.getValue() == null || genCombo.getValue() == null) {
                    afficherErreur("Erreur", "Veuillez selectionner une maison ET un generateur.");
                    return null;
                }
                return new String[] { maisonCombo.getValue(), genCombo.getValue() };
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String nomMaison = data[0];
            String nomGen = data[1];

            if (reseau.connexionExiste(nomMaison, nomGen)) {
                afficherErreur("Connexion Existante",
                        "La connexion entre '" + nomMaison + "' et '" + nomGen + "' existe deja.");
                return;
            }

            if (reseau.connexionExistePourMaison(nomMaison)) {
                List<String> gensConnectes = reseau.getConnexions().get(nomMaison);
                String dejaConnecteA = String.join(", ", gensConnectes);

                Alert warning = new Alert(Alert.AlertType.WARNING);
                warning.setTitle("Attention - Connexions Multiples");
                warning.setHeaderText("La maison '" + nomMaison + "' est deja connectee");
                warning.setContentText(
                        "La maison '" + nomMaison + "' est deja connectee a: " + dejaConnecteA + "\n\n" +
                                "RAPPEL: Une maison doit etre connectee a UN SEUL generateur.\n\n" +
                                "Vous pouvez continuer a ajouter cette connexion, mais elle sera\n" +
                                "consideree comme INVALIDE lors de la validation (option 5).\n\n" +
                                "Voulez-vous quand meme ajouter cette connexion ?");
                warning.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

                Optional<ButtonType> choice = warning.showAndWait();
                if (choice.isEmpty() || choice.get() == ButtonType.NO) {
                    return;
                }
            }

            controller.addConnexion(nomMaison, nomGen);
            rafraichirGraphe();
            rafraichirInfos();

            if (reseau.getConnexions().get(nomMaison).size() > 1) {
                statusLabel.setText("AVERTISSEMENT: Connexion creee mais INVALIDE: " +
                        nomMaison + " <-> " + nomGen + " (connexions multiples)");
            } else {
                statusLabel.setText("INFO: Connexion creee: " + nomMaison + " <-> " + nomGen);
            }
        });
    }

    // Permet de choisir une connexion existante pour la supprimer
    private void supprimerConnexion() {
        Reseau reseau = controller.getReseau();

        if (reseau.getConnexions().isEmpty()) {
            afficherErreur("Aucune connexion",
                    "Il n'y a aucune connexion a supprimer.\n" +
                            "Creez d'abord des connexions avec l'option 3.");
            return;
        }

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Menu 1 - Option 4");
        dialog.setHeaderText("Supprimer une Connexion");

        ButtonType btnSupprimer = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSupprimer, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> maisonCombo = new ComboBox<>();
        maisonCombo.getItems().addAll(reseau.getConnexions().keySet());
        maisonCombo.setPromptText("Selectionner une maison");

        ComboBox<String> genCombo = new ComboBox<>();
        genCombo.setPromptText("Selectionner un generateur");

        maisonCombo.setOnAction(e -> {
            String maison = maisonCombo.getValue();
            if (maison != null) {
                genCombo.getItems().clear();
                genCombo.getItems().addAll(reseau.getConnexions().get(maison));
                if (!genCombo.getItems().isEmpty()) {
                    genCombo.setValue(genCombo.getItems().get(0));
                }
            }
        });

        grid.add(new Label("Maison:"), 0, 0);
        grid.add(maisonCombo, 1, 0);
        grid.add(new Label("Generateur connecte:"), 0, 1);
        grid.add(genCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSupprimer) {
                if (maisonCombo.getValue() == null || genCombo.getValue() == null) {
                    afficherErreur("Erreur", "Veuillez selectionner une connexion complete.");
                    return null;
                }
                return new String[] { maisonCombo.getValue(), genCombo.getValue() };
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String nomMaison = data[0];
            String nomGen = data[1];

            if (!reseau.connexionExiste(nomMaison, nomGen)) {
                afficherErreur("Connexion inexistante",
                        "La connexion entre " + nomMaison + " et " + nomGen + " n'existe pas.");
                return;
            }

            controller.supprimerConnexion(nomMaison, nomGen);
            rafraichirGraphe();
            rafraichirInfos();
            statusLabel.setText("INFO: Connexion supprimee: " + nomMaison + " <-> " + nomGen);
        });
    }

    // Verifie si le reseau est valide (pas d'erreurs) avant de passer a l'etape
    // d'analyse
    private void finMenu1() {
        List<String> problemes = controller.validerConfigurationReseau();

        if (!problemes.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Configuration Invalide");
            alert.setHeaderText("Impossible de passer au Menu 2");

            String message = "Le reseau contient des erreurs:\n\n" +
                    String.join("\n", problemes) + "\n\n" +
                    "Veuillez corriger ces problemes avant de continuer.";
            TextArea textArea = new TextArea(message);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Fin de la Configuration");
        confirm.setHeaderText("Configuration Terminee et Validee");
        confirm.setContentText("- Toutes les maisons sont connectees a un unique generateur.\n" +
                "- Le reseau respecte toutes les contraintes.\n\n" +
                "Passer au Menu 2 (Analyse) ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Bascule directement vers le menu 3 (meme interface que le chargement fichier)
            menuActuel = 3;
            mettreAJourMenuInfo();

            if (actionsBox != null) {
                mettreAJourActions(actionsBox);
            }

            calculerCout();
            statusLabel.setText("Configuration validee. Menu 3 actif.");
        }
    }

    // MENU 2 : ANALYSE -----------------------------------------------------------

    // Calcule et affiche le cout total, la dispersion et la surcharge du reseau
    private void calculerCout() {
        List<String> problemes = controller.validerConfigurationReseau();
        if (!problemes.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Configuration Invalide");
            alert.setHeaderText("Impossible de calculer le cout");

            String message = "Le reseau contient des erreurs:\n\n" +
                    String.join("\n", problemes);
            TextArea textArea = new TextArea(message);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();
            return;
        }

        double[] couts = controller.calculerCoutReseau();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(menuActuel == 2 ? "Menu 2 - Option 1" : "Menu 3 - Cout Actuel");
        alert.setHeaderText("Resultat du Calcul de Cout");

        String contenu = String.format(
                "--- RESULTAT DU CALCUL DE COUT ---\n\n" +
                        "Dispersion (Disp(S)):     %.4f\n" +
                        "Surcharge (Surcharge(S)): %.4f\n" +
                        "Cout total (Cout(S)):     %.4f\n\n" +
                        "Parametre lambda: %d",
                couts[1], couts[2], couts[0], controller.getReseau().getLambda());

        alert.setContentText(contenu);
        alert.showAndWait();

        coutLabel.setText(String.format("Cout: %.4f", couts[0]));
        statusLabel.setText("Cout calcule avec succes.");
    }

    // Met a jour l'etiquette de cout sans ouvrir de pop-up
    private void rafraichirCoutLabel() {
        List<String> problemes = controller.validerConfigurationReseau();
        if (!problemes.isEmpty()) {
            coutLabel.setText("Cout: Configuration invalide!");
            return;
        }

        double[] couts = controller.calculerCoutReseau();
        coutLabel.setText(String.format("Cout: %.4f", couts[0]));
    }

    // Permet de changer le generateur d'une maison en deux etapes
    private void modifierConnexion() {
        Reseau reseau = controller.getReseau();

        if (reseau.getConnexions().isEmpty()) {
            afficherErreur("Aucune connexion",
                    "Il n'y a aucune connexion a modifier.");
            return;
        }

        Dialog<String[]> dialog1 = new Dialog<>();
        dialog1.setTitle("Menu 2 - Option 2 (Etape 1/2)");
        dialog1.setHeaderText("Modifier une Connexion\n\nEtape 1: Selectionnez la connexion a modifier");

        ButtonType btnSuivant = new ButtonType("Suivant", ButtonBar.ButtonData.OK_DONE);
        dialog1.getDialogPane().getButtonTypes().addAll(btnSuivant, ButtonType.CANCEL);

        GridPane grid1 = new GridPane();
        grid1.setHgap(10);
        grid1.setVgap(10);
        grid1.setPadding(new Insets(20));

        ComboBox<String> maisonCombo1 = new ComboBox<>();
        maisonCombo1.getItems().addAll(reseau.getConnexions().keySet());
        maisonCombo1.setPromptText("Selectionner la maison");

        ComboBox<String> genCombo1 = new ComboBox<>();
        genCombo1.setPromptText("Generateur actuel");

        maisonCombo1.setOnAction(e -> {
            String maison = maisonCombo1.getValue();
            if (maison != null) {
                genCombo1.getItems().clear();
                genCombo1.getItems().addAll(reseau.getConnexions().get(maison));
                if (!genCombo1.getItems().isEmpty()) {
                    genCombo1.setValue(genCombo1.getItems().get(0));
                }
            }
        });

        grid1.add(new Label("Maison:"), 0, 0);
        grid1.add(maisonCombo1, 1, 0);
        grid1.add(new Label("Generateur actuel:"), 0, 1);
        grid1.add(genCombo1, 1, 1);

        dialog1.getDialogPane().setContent(grid1);

        dialog1.setResultConverter(dialogButton -> {
            if (dialogButton == btnSuivant) {
                if (maisonCombo1.getValue() == null || genCombo1.getValue() == null) {
                    afficherErreur("Erreur", "Veuillez selectionner une connexion complete.");
                    return null;
                }
                return new String[] { maisonCombo1.getValue(), genCombo1.getValue() };
            }
            return null;
        });

        Optional<String[]> result1 = dialog1.showAndWait();
        result1.ifPresent(anciennes -> {
            String nomMaison = anciennes[0];
            String ancienGen = anciennes[1];

            Dialog<String> dialog2 = new Dialog<>();
            dialog2.setTitle("Menu 2 - Option 2 (Etape 2/2)");
            dialog2.setHeaderText("Modifier une Connexion\n\nEtape 2: Nouvelle connexion pour " + nomMaison);

            ButtonType btnModifier = new ButtonType("Modifier", ButtonBar.ButtonData.OK_DONE);
            dialog2.getDialogPane().getButtonTypes().addAll(btnModifier, ButtonType.CANCEL);

            GridPane grid2 = new GridPane();
            grid2.setHgap(10);
            grid2.setVgap(10);
            grid2.setPadding(new Insets(20));

            Label infoLabel = new Label("Maison: " + nomMaison + "\nGenerateur actuel: " + ancienGen);
            infoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            ComboBox<String> genCombo2 = new ComboBox<>();
            genCombo2.getItems().addAll(reseau.getGenerateurs().keySet());
            genCombo2.setPromptText("Selectionner le nouveau generateur");

            grid2.add(infoLabel, 0, 0, 2, 1);
            grid2.add(new Label("Nouveau generateur:"), 0, 1);
            grid2.add(genCombo2, 1, 1);

            dialog2.getDialogPane().setContent(grid2);

            dialog2.setResultConverter(dialogButton -> {
                if (dialogButton == btnModifier) {
                    if (genCombo2.getValue() == null) {
                        afficherErreur("Erreur", "Veuillez selectionner un generateur.");
                        return null;
                    }
                    return genCombo2.getValue();
                }
                return null;
            });

            Optional<String> result2 = dialog2.showAndWait();
            result2.ifPresent(nouveauGen -> {
                if (!reseau.generateurExiste(nouveauGen)) {
                    afficherErreur("Erreur", "Le generateur selectionne n'existe pas.");
                    return;
                }

                controller.modifierConnexion(nomMaison, nouveauGen);
                rafraichirGraphe();
                rafraichirInfos();
                statusLabel.setText("INFO: Connexion modifiee: " + nomMaison + " : " + ancienGen + " -> " + nouveauGen);
            });
        });
    }

    // Affiche une grande fenetre de texte avec le detail de tout le reseau
    private void afficherReseau() {
        Reseau reseau = controller.getReseau();
        StringBuilder sb = new StringBuilder();

        sb.append("=== ETAT ACTUEL DU RESEAU ELECTRIQUE ===\n\n");

        sb.append(">> GENERATEURS (" + reseau.getGenerateurs().size() + ") :\n");
        if (reseau.getGenerateurs().isEmpty()) {
            sb.append("  Aucun generateur defini.\n");
        } else {
            for (Generateur gen : reseau.getGenerateurs().values()) {
                int charge = calculerChargeGenerateur(reseau, gen.getNom());
                sb.append(String.format("  - %s (Capacite: %d kW)\n", gen.getNom(), gen.getCapaciteMax()));
                sb.append(String.format("    Charge actuelle: %d kW", charge));
                if (charge > gen.getCapaciteMax()) {
                    sb.append(" ! SURCHARGE!");
                }
                sb.append("\n");

                int nbConnectees = 0;
                for (var entry : reseau.getConnexions().entrySet()) {
                    if (entry.getValue().contains(gen.getNom())) {
                        if (nbConnectees == 0) {
                            sb.append("    Maisons connectees:\n");
                        }
                        Maison m = reseau.getMaisons().get(entry.getKey());
                        sb.append(String.format("      -> %s (%d kW)\n", entry.getKey(), m.getConsommationKw()));
                        nbConnectees++;
                    }
                }
                if (nbConnectees == 0) {
                    sb.append("    Aucune maison connectee.\n");
                }
                sb.append("\n");
            }
        }

        sb.append(">> MAISONS (" + reseau.getMaisons().size() + ") :\n");
        if (reseau.getMaisons().isEmpty()) {
            sb.append("  Aucune maison definie.\n");
        } else {
            for (Maison maison : reseau.getMaisons().values()) {
                List<String> gens = reseau.getConnexions().get(maison.getNom());
                String statut;
                if (gens == null || gens.isEmpty()) {
                    statut = "NON CONNECTEE";
                } else if (gens.size() == 1) {
                    statut = "Connectee a " + gens.get(0);
                } else {
                    statut = "ERREUR: Connectee a PLUSIEURS generateurs: " + String.join(", ", gens);
                }

                sb.append(String.format("  - %s (%s, %d kW) - %s\n",
                        maison.getNom(),
                        maison.getConsommation().name(),
                        maison.getConsommationKw(),
                        statut));
            }
        }

        sb.append("\n>> STATISTIQUES :\n");
        int capaciteTotale = reseau.getGenerateurs().values().stream()
                .mapToInt(Generateur::getCapaciteMax).sum();
        int demandeTotale = reseau.getMaisons().values().stream()
                .mapToInt(Maison::getConsommationKw).sum();
        int nbConnexions = reseau.getConnexions().values().stream()
                .mapToInt(List::size).sum();

        sb.append(String.format("  Capacite totale: %d kW\n", capaciteTotale));
        sb.append(String.format("  Demande totale: %d kW\n", demandeTotale));
        sb.append(String.format("  Connexions: %d\n", nbConnexions));
        sb.append(String.format("  Lambda: %d\n", reseau.getLambda()));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Menu 2 - Option 3");
        alert.setHeaderText("Affichage du Reseau");

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setPrefSize(600, 500);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();

        statusLabel.setText("Affichage du reseau termine.");
    }

    // MENU 3 : FICHIER CHARGE
    // -------------------------------------------------------------

    // Tente de lire un fichier texte pour construire le reseau automatiquement
    private boolean chargerReseauDepuisFichier(String chemin, String lambdaStr) {
        try {
            int lambda = Integer.parseInt(lambdaStr);
            if (lambda <= 0) {
                afficherErreur("Erreur Lambda", "Lambda doit etre un entier positif.");
                return false;
            }

            String cheminNormalise = FileAlgorithms.normalizeTxtPathForLoad(chemin);

            controller.chargerReseauDepuisFichier(cheminNormalise);
            controller.getReseau().setLambda(lambda);

            List<String> warnings = FileAlgorithms.consumeLastLoadWarnings();

            javafx.application.Platform.runLater(() -> {
                rafraichirGraphe();
                rafraichirInfos();

                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Fichier Charge");
                info.setHeaderText("Reseau charge avec succes");
                info.setContentText("Fichier: " + cheminNormalise + "\nLambda: " + lambda + "\n\n" +
                        "Le Menu 3 est maintenant actif.");
                info.showAndWait();

                if (!warnings.isEmpty()) {
                    Alert warn = new Alert(Alert.AlertType.WARNING);
                    warn.setTitle("Avertissements de Chargement");
                    warn.setHeaderText("Le reseau est valide, mais des avertissements ont ete detectes");

                    TextArea textArea = new TextArea(String.join("\n", warnings));
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    textArea.setPrefSize(600, 400);
                    warn.getDialogPane().setContent(textArea);

                    warn.showAndWait();
                }

                statusLabel.setText(
                        "Fichier charge: " + new File(cheminNormalise).getName() + " (lambda=" + lambda + ")");
            });

            return true;

        } catch (NumberFormatException e) {
            afficherErreur("Erreur Lambda", "La valeur de lambda doit etre un entier.");
            return false;
            // Extension de fichier incorrecte
        } catch (InvalidFileExtensionException e) {
            afficherErreur("Extension invalide", e.getMessage());
            return false;
        } catch (ReseauInvalideSyntaxException e) {
            // la syntaxe
            afficherErreur("Fichier Incorrect (Syntaxe)", e.getMessage());
            return false;
        } catch (ReseauInvalideException e) {
            // la logique
            afficherErreur("Reseau Incohérent (Logique)", e.getMessage());
            return false;
        } catch (FileNotFoundException e) {
            // fichier non trouvé
            afficherErreur("Fichier Introuvable", "Le fichier n'existe pas ou le chemin est incorrect :\n" + chemin);
            return false;
        } catch (Exception e) {
            afficherErreur("Erreur Inattendue",
                    "Une erreur technique est survenue :\n\n" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Lance l'algorithme d'optimisation pour trouver une meilleure configuration
    // Cette operation s'execute en arriere-plan pour ne pas bloquer l'interface
    private void resolutionAutomatique() {
        Reseau reseau = controller.getReseau();

        List<String> problemes = controller.validerConfigurationReseau();
        if (!problemes.isEmpty()) {
            afficherErreur("Reseau Invalide",
                    "Impossible de lancer l'optimisation.\n\n" +
                            "Erreurs:\n" + String.join("\n", problemes));
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Menu 3 - Option 1");
        confirmDialog.setHeaderText("Resolution Automatique");
        confirmDialog.setContentText("Lancer l'algorithme genetique d'optimisation?\n\n" +
                "Attention: Cette operation peut prendre quelques secondes.\n" +
                "L'algorithme modifiera les connexions pour minimiser le cout.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            statusLabel.setText("Optimisation en cours...");

            new Thread(() -> {
                long debut = System.currentTimeMillis();
                double[] avant = controller.calculerCoutReseau();

                try {
                    GeneticAlgorithm solver = new GeneticAlgorithm(controller.getReseau());
                    solver.solve();
                } catch (RuntimeException e) {
                    javafx.application.Platform.runLater(() -> {
                        afficherErreur("Erreur d'Optimisation",
                                "L'optimisation a echoue:\n\n" + e.getMessage());
                        statusLabel.setText("Erreur durant l'optimisation.");
                    });
                    return;
                }

                double[] apres = controller.calculerCoutReseau();
                long duree = System.currentTimeMillis() - debut;

                javafx.application.Platform.runLater(() -> {
                    rafraichirGraphe();
                    rafraichirInfos();
                    coutLabel.setText(String.format("Cout: %.4f", apres[0]));

                    Alert resultDialog = new Alert(Alert.AlertType.INFORMATION);
                    resultDialog.setTitle("Optimisation Terminee");
                    resultDialog.setHeaderText("Resultats de la Resolution Automatique");

                    double amelioration = avant[0] > 0 ? ((avant[0] - apres[0]) / avant[0]) * 100 : 0;

                    resultDialog.setContentText(String.format(
                            "Cout avant optimisation: %.4f\n" +
                                    "Cout apres optimisation: %.4f\n\n" +
                                    "Amelioration: %.2f%%\n" +
                                    "Temps d'execution: %d ms\n\n" +
                                    "Les connexions ont ete modifiees pour minimiser le cout.",
                            avant[0], apres[0], amelioration, duree));
                    resultDialog.showAndWait();

                    statusLabel.setText("Optimisation terminee avec succes.");
                });
            }).start();
        }
    }

    // Ecrit la configuration actuelle dans un fichier texte choisi par
    // l'utilisateur
    private void sauvegarderSolution() {
        List<String> problemes = controller.validerConfigurationReseau();
        if (!problemes.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.WARNING);
            confirm.setTitle("Configuration Invalide");
            confirm.setHeaderText("Le reseau contient des erreurs");

            String message = "Voulez-vous quand meme sauvegarder?\n\n" +
                    "Erreurs:\n" + String.join("\n", problemes);
            TextArea textArea = new TextArea(message);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            confirm.getDialogPane().setContent(textArea);
            confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() == ButtonType.NO) {
                return;
            }
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Menu 3 - Option 2 : Sauvegarder la Solution");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers texte", "*.txt"));
        fileChooser.setInitialFileName("solution.txt");

        File fichier = fileChooser.showSaveDialog(primaryStage);
        if (fichier != null) {
            try {
                controller.sauvegarderReseau(fichier.getAbsolutePath());

                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Sauvegarde Reussie");
                info.setHeaderText("Solution sauvegardee");
                info.setContentText("Le reseau a ete sauvegarde avec succes dans:\n" +
                        fichier.getAbsolutePath());
                info.showAndWait();

                statusLabel.setText("Sauvegarde reussie: " + fichier.getName());
            } catch (InvalidNameException e) {
                afficherErreur("Nom invalide",
                        "Impossible de sauvegarder car un nom contient un espace.\n\n" + e.getMessage());
            } catch (Exception e) {
                afficherErreur("Erreur de Sauvegarde",
                        "Impossible de sauvegarder:\n\n" + e.getMessage());
            }
        }
    }

    private void validerNomAlphanumerique(String nom, String type) throws InvalidNameException {
        if (nom == null || nom.trim().isEmpty()) {
            throw new InvalidNameException("Le nom ne peut pas etre vide.");
        }
        if (!nom.matches("[a-zA-Z0-9]+")) {
            throw new InvalidNameException(
                    "Le nom du " + type
                            + " doit etre strictement alphanumerique (lettres et chiffres uniquement, sans espaces ni caracteres speciaux).");
        }
    }

    // UTILITAIRES -----------------------------------------------------------

    // Active ou desactive tous les boutons du panneau d'actions
    private void setActionsDisabled(boolean disabled) {
        if (actionsBox == null)
            return;
        for (Node node : actionsBox.getChildren()) {
            if (node instanceof Button) {
                node.setDisable(disabled);
            }
        }
    }

    // Cree la barre du bas avec le statut et le cout
    private HBox creerBarreStatut() {
        HBox barre = new HBox(20);
        barre.setPadding(new Insets(10));
        barre.setStyle("-fx-background-color: #34495e;");

        statusLabel = new Label("Pret");
        statusLabel.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        coutLabel = new Label("Cout: Non calcule");
        coutLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 12px;");

        barre.getChildren().addAll(statusLabel, spacer, coutLabel);
        return barre;
    }

    // Helper pour creer un bouton bleu
    private Button creerBoutonStyleControle(String texte) {
        Button btn = new Button(texte);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;"));
        return btn;
    }

    // Demande confirmation avant de fermer l'application (peut etre on ajoute un
    // auto-save ici (hors de projet))
    private void confirmerQuitter() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quitter");
        alert.setHeaderText("Voulez-vous vraiment quitter?");
        alert.setContentText("Toutes les donnees non sauvegardees seront perdues.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            primaryStage.close();
        }
    }

    // Redessine le graphe
    private void rafraichirGraphe() {
        graphPane.rafraichir();
    }

    // Met a jour les compteurs (Nombre de maisons, generateurs, connexions)
    private void rafraichirInfos() {
        Reseau reseau = controller.getReseau();
        infoGenerateurs.setText("Generateurs: " + reseau.getGenerateurs().size());
        infoMaisons.setText("Maisons: " + reseau.getMaisons().size());

        int nbConnexions = reseau.getConnexions().values().stream()
                .mapToInt(List::size).sum();
        infoConnexions.setText("Connexions: " + nbConnexions);
    }

    // Calcule la charge d'un generateur donne
    private int calculerChargeGenerateur(Reseau reseau, String nomGen) {
        int charge = 0;
        for (var entry : reseau.getConnexions().entrySet()) {
            if (entry.getValue().contains(nomGen)) {
                Maison maison = reseau.getMaisons().get(entry.getKey());
                if (maison != null) {
                    charge += maison.getConsommationKw();
                }
            }
        }
        return charge;
    }

    // Affiche une boite de dialogue d'erreur standard
    private void afficherErreur(String titre, String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        // Mettre un HeaderText pour separer l'icône du contenu
        alert.setHeaderText("Une erreur est survenue :");

        TextArea textArea = new TextArea(message);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        // force une taille pour le TextArea
        textArea.setPrefSize(600, 200);

        // Assurer que le composant prend toute la place disponible
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        // Placer le TextArea dans le dialog pane
        alert.getDialogPane().setContent(textArea);

        // On rend la fenetre redimensionnable au cas ou le texte est tres long
        alert.setResizable(true);

        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}