package View;

import Controller.AppController;
import Model.*;
import Algorithms.GeneticAlgorithm;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Interface graphique principale pour le réseau électrique
 * Utilise JavaFX pour une représentation visuelle interactive
 */
public class GraphicalView extends Application {
    private AppController controller;
    private NetworkGraphPane graphPane;
    private Label statusLabel;
    private Label coutLabel;
    private Stage primaryStage;

    // Labels d'information mis à jour dynamiquement
    private Label infoGenerateurs;
    private Label infoMaisons;
    private Label infoConnexions;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // Initialisation du contrôleur avec un réseau vide
        Reseau reseau = new Reseau();
        controller = new AppController(reseau);

        // Vérifier les paramètres de ligne de commande
        Parameters params = getParameters();
        List<String> args = params.getRaw();

        if (!args.isEmpty()) {
            // Mode fichier
            chargerReseauDepuisFichier(args.get(0), args.size() > 1 ? args.get(1) : "10");
        }

        // Configuration de l'interface
        BorderPane root = new BorderPane();
        root.setTop(creerMenuBar());
        root.setCenter(creerCentrePane());
        root.setBottom(creerBarreStatut());
        root.setRight(creerPanneauControle());

        Scene scene = new Scene(root, 1400, 900);

        // Charger le CSS avec gestion d'erreur
        try {
            String css = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.out.println("Attention: Fichier styles.css non trouvé, utilisation du style par défaut");
        }

        stage.setTitle("Réseau Électrique - Gestion et Optimisation");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        // Rafraîchir l'affichage initial
        rafraichirGraphe();
        rafraichirInfos();
    }

    private MenuBar creerMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: #34495e;");

        // Menu Fichier
        Menu menuFichier = new Menu("Fichier");
        menuFichier.setStyle("-fx-text-fill: white;");

        MenuItem itemCharger = new MenuItem("Charger depuis fichier...");
        MenuItem itemSauvegarder = new MenuItem("Sauvegarder...");
        MenuItem itemNouveau = new MenuItem("Nouveau réseau");
        MenuItem itemQuitter = new MenuItem("Quitter");

        itemCharger.setOnAction(e -> ouvrirDialogueChargement());
        itemSauvegarder.setOnAction(e -> ouvrirDialogueSauvegarde());
        itemNouveau.setOnAction(e -> nouveauReseau());
        itemQuitter.setOnAction(e -> primaryStage.close());

        menuFichier.getItems().addAll(itemCharger, itemSauvegarder,
                new SeparatorMenuItem(), itemNouveau,
                new SeparatorMenuItem(), itemQuitter);

        // Menu Édition
        Menu menuEdition = new Menu("Édition");
        menuEdition.setStyle("-fx-text-fill: white;");

        MenuItem itemAjouterGen = new MenuItem("Ajouter un générateur");
        MenuItem itemAjouterMaison = new MenuItem("Ajouter une maison");
        MenuItem itemAjouterConnexion = new MenuItem("Ajouter une connexion");
        MenuItem itemSupprimerConnexion = new MenuItem("Supprimer une connexion");
        MenuItem itemModifierConnexion = new MenuItem("Modifier une connexion");

        itemAjouterGen.setOnAction(e -> ajouterGenerateurAvecPosition());
        itemAjouterMaison.setOnAction(e -> ajouterMaisonAvecPosition());
        itemAjouterConnexion.setOnAction(e -> creerConnexion());
        itemSupprimerConnexion.setOnAction(e -> supprimerConnexion());
        itemModifierConnexion.setOnAction(e -> modifierConnexion());

        menuEdition.getItems().addAll(itemAjouterGen, itemAjouterMaison,
                new SeparatorMenuItem(),
                itemAjouterConnexion, itemSupprimerConnexion, itemModifierConnexion);

        // Menu Analyse
        Menu menuAnalyse = new Menu("Analyse");
        menuAnalyse.setStyle("-fx-text-fill: white;");

        MenuItem itemCalculerCout = new MenuItem("Calculer le coût");
        MenuItem itemOptimiser = new MenuItem("Optimisation automatique");
        MenuItem itemValider = new MenuItem("Valider la configuration");
        MenuItem itemAfficher = new MenuItem("Afficher le réseau (détails)");

        itemCalculerCout.setOnAction(e -> calculerEtAfficherCout());
        itemOptimiser.setOnAction(e -> lancerOptimisation());
        itemValider.setOnAction(e -> validerConfiguration());
        itemAfficher.setOnAction(e -> afficherDetailsReseau());

        menuAnalyse.getItems().addAll(itemCalculerCout, itemOptimiser,
                new SeparatorMenuItem(), itemValider, itemAfficher);

        // Menu Aide
        Menu menuAide = new Menu("Aide");
        menuAide.setStyle("-fx-text-fill: white;");

        MenuItem itemAPropos = new MenuItem("À propos");
        MenuItem itemGuide = new MenuItem("Guide d'utilisation");

        itemAPropos.setOnAction(e -> afficherAPropos());
        itemGuide.setOnAction(e -> afficherGuide());

        menuAide.getItems().addAll(itemGuide, itemAPropos);

        menuBar.getMenus().addAll(menuFichier, menuEdition, menuAnalyse, menuAide);

        // Forcer le style des labels de menu
        menuBar.getMenus().forEach(menu -> {
            menu.setStyle("-fx-text-fill: white;");
        });

        return menuBar;
    }

    private Pane creerCentrePane() {
        graphPane = new NetworkGraphPane(controller);
        graphPane.setStyle("-fx-background-color: #f5f5f5;");
        return graphPane;
    }

    private VBox creerPanneauControle() {
        VBox panneau = new VBox(15);
        panneau.setPadding(new Insets(20));
        panneau.setStyle("-fx-background-color: #2c3e50; -fx-min-width: 280px;");

        Label titre = new Label("Panneau de Contrôle");
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Section Actions rapides
        Label lblActions = new Label("Actions Rapides");
        lblActions.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        Button btnAjouterGen = creerBoutonStyleControle("➕ Ajouter Générateur");
        btnAjouterGen.setOnAction(e -> ajouterGenerateurAvecPosition());

        Button btnAjouterMaison = creerBoutonStyleControle("🏠 Ajouter Maison");
        btnAjouterMaison.setOnAction(e -> ajouterMaisonAvecPosition());

        Button btnConnexion = creerBoutonStyleControle("🔗 Créer Connexion");
        btnConnexion.setOnAction(e -> creerConnexion());

        Separator sep1 = new Separator();

        // Section Analyse
        Label lblAnalyse = new Label("Analyse");
        lblAnalyse.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        Button btnCalculer = creerBoutonStyleControle("📊 Calculer Coût");
        btnCalculer.setOnAction(e -> calculerEtAfficherCout());

        Button btnOptimiser = creerBoutonStyleControle("🚀 Optimiser");
        btnOptimiser.setOnAction(e -> lancerOptimisation());

        Button btnValider = creerBoutonStyleControle("✓ Valider");
        btnValider.setOnAction(e -> validerConfiguration());

        Separator sep2 = new Separator();

        // Section Vue
        Label lblVue = new Label("Affichage");
        lblVue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        Button btnRecentrer = creerBoutonStyleControle("🎯 Recentrer");
        btnRecentrer.setOnAction(e -> graphPane.recentrerVue());

        Button btnRafraichir = creerBoutonStyleControle("🔄 Rafraîchir");
        btnRafraichir.setOnAction(e -> {
            rafraichirGraphe();
            rafraichirInfos();
        });

        // Informations réseau
        Separator sep3 = new Separator();
        Label lblInfo = new Label("Informations");
        lblInfo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        VBox infoBox = new VBox(5);
        infoBox.setStyle("-fx-background-color: #34495e; -fx-padding: 10; -fx-background-radius: 5;");

        infoGenerateurs = new Label("Générateurs: 0");
        infoMaisons = new Label("Maisons: 0");
        infoConnexions = new Label("Connexions: 0");

        infoGenerateurs.setStyle("-fx-text-fill: #ecf0f1;");
        infoMaisons.setStyle("-fx-text-fill: #ecf0f1;");
        infoConnexions.setStyle("-fx-text-fill: #ecf0f1;");

        infoBox.getChildren().addAll(infoGenerateurs, infoMaisons, infoConnexions);

        panneau.getChildren().addAll(
                titre, new Separator(),
                lblActions, btnAjouterGen, btnAjouterMaison, btnConnexion,
                sep1, lblAnalyse, btnCalculer, btnOptimiser, btnValider,
                sep2, lblVue, btnRecentrer, btnRafraichir,
                sep3, lblInfo, infoBox
        );

        return panneau;
    }

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

    private HBox creerBarreStatut() {
        HBox barre = new HBox(20);
        barre.setPadding(new Insets(10));
        barre.setStyle("-fx-background-color: #34495e;");

        statusLabel = new Label("Prêt");
        statusLabel.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        coutLabel = new Label("Coût: Non calculé");
        coutLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 12px;");

        barre.getChildren().addAll(statusLabel, spacer, coutLabel);
        return barre;
    }

    // Méthodes d'action avec toutes les contraintes

    private void chargerReseauDepuisFichier(String chemin, String lambdaStr) {
        try {
            int lambda = Integer.parseInt(lambdaStr);
            if (lambda <= 0) {
                afficherErreur("Erreur Lambda", "Lambda doit être un entier positif.");
                return;
            }
            controller.chargerReseauDepuisFichier(chemin);
            controller.getReseau().setLambda(lambda);
            rafraichirGraphe();
            rafraichirInfos();
            statusLabel.setText("Fichier chargé: " + chemin + " (λ=" + lambda + ")");
        } catch (NumberFormatException e) {
            afficherErreur("Erreur Lambda", "La valeur de lambda doit être un entier.");
        } catch (Exception e) {
            afficherErreur("Erreur de chargement", e.getMessage());
        }
    }

    private void ouvrirDialogueChargement() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Charger un réseau");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers texte", "*.txt")
        );

        File fichier = fileChooser.showOpenDialog(primaryStage);
        if (fichier != null) {
            TextInputDialog dialog = new TextInputDialog("10");
            dialog.setTitle("Paramètre Lambda");
            dialog.setHeaderText("Valeur de pénalisation");
            dialog.setContentText("Lambda (λ):");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(lambda -> chargerReseauDepuisFichier(fichier.getAbsolutePath(), lambda));
        }
    }

    private void ouvrirDialogueSauvegarde() {
        // Valider avant de sauvegarder
        List<String> problemes = controller.validerConfigurationReseau();
        if (!problemes.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.WARNING);
            confirm.setTitle("Configuration invalide");
            confirm.setHeaderText("Le réseau contient des erreurs");
            confirm.setContentText("Voulez-vous quand même sauvegarder?\n\nErreurs:\n" +
                    String.join("\n", problemes));
            confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() == ButtonType.NO) {
                return;
            }
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder le réseau");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers texte", "*.txt")
        );

        File fichier = fileChooser.showSaveDialog(primaryStage);
        if (fichier != null) {
            try {
                controller.sauvegarderReseau(fichier.getAbsolutePath());
                statusLabel.setText("Sauvegarde réussie: " + fichier.getName());
                afficherInfo("Sauvegarde réussie", "Le réseau a été sauvegardé avec succès.");
            } catch (Exception e) {
                afficherErreur("Erreur de sauvegarde", e.getMessage());
            }
        }
    }

    private void nouveauReseau() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Nouveau réseau");
        alert.setHeaderText("Créer un nouveau réseau?");
        alert.setContentText("Toutes les données non sauvegardées seront perdues.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Reseau nouveauReseau = new Reseau();
            controller = new AppController(nouveauReseau);
            graphPane.setController(controller);
            rafraichirGraphe();
            rafraichirInfos();
            coutLabel.setText("Coût: Non calculé");
            statusLabel.setText("Nouveau réseau créé");
        }
    }

    private void ajouterGenerateurAvecPosition() {
        Dialog<Generateur> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un générateur");
        dialog.setHeaderText("Nouveau générateur - Cliquez sur le graphe pour placer");

        ButtonType btnAjouter = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAjouter, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nomField = new TextField();
        nomField.setPromptText("ex: gen1");
        TextField capaciteField = new TextField();
        capaciteField.setPromptText("ex: 60");

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Capacité (kW):"), 0, 1);
        grid.add(capaciteField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAjouter) {
                try {
                    String nom = nomField.getText().trim();
                    if (nom.isEmpty()) {
                        afficherErreur("Erreur", "Le nom ne peut pas être vide.");
                        return null;
                    }

                    int capacite = Integer.parseInt(capaciteField.getText().trim());
                    if (capacite <= 0) {
                        afficherErreur("Erreur", "La capacité doit être un nombre positif.");
                        return null;
                    }

                    return new Generateur(nom, capacite);
                } catch (NumberFormatException e) {
                    afficherErreur("Erreur", "La capacité doit être un nombre entier valide.");
                    return null;
                }
            }
            return null;
        });

        Optional<Generateur> result = dialog.showAndWait();
        result.ifPresent(gen -> {
            // Demander la position
            statusLabel.setText("Cliquez sur le graphe pour placer le générateur " + gen.getNom());
            graphPane.attendreClicPourPosition(pos -> {
                boolean existed = controller.addGenerateur(gen.getNom(), gen.getCapaciteMax());
                graphPane.setPositionGenerateur(gen.getNom(), pos);
                rafraichirGraphe();
                rafraichirInfos();
                statusLabel.setText(existed ? "Générateur mis à jour: " + gen.getNom()
                        : "Générateur ajouté: " + gen.getNom());
            }, true);
        });
    }

    private void ajouterMaisonAvecPosition() {
        Dialog<Maison> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une maison");
        dialog.setHeaderText("Nouvelle maison - Cliquez sur le graphe pour placer");

        ButtonType btnAjouter = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAjouter, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nomField = new TextField();
        nomField.setPromptText("ex: maison1");

        ComboBox<String> consoCombo = new ComboBox<>();
        consoCombo.getItems().addAll("BASSE", "NORMAL", "FORTE");
        consoCombo.setValue("NORMAL");

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Consommation:"), 0, 1);
        grid.add(consoCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAjouter) {
                String nom = nomField.getText().trim();
                if (nom.isEmpty()) {
                    afficherErreur("Erreur", "Le nom ne peut pas être vide.");
                    return null;
                }

                Consommation conso = Consommation.fromString(consoCombo.getValue());
                return new Maison(nom, conso);
            }
            return null;
        });

        Optional<Maison> result = dialog.showAndWait();
        result.ifPresent(maison -> {
            // Demander la position
            statusLabel.setText("Cliquez sur le graphe pour placer la maison " + maison.getNom());
            graphPane.attendreClicPourPosition(pos -> {
                try {
                    boolean existed = controller.addMaison(maison.getNom(), maison.getConsommation().name());
                    graphPane.setPositionMaison(maison.getNom(), pos);
                    rafraichirGraphe();
                    rafraichirInfos();
                    statusLabel.setText(existed ? "Maison mise à jour: " + maison.getNom()
                            : "Maison ajoutée: " + maison.getNom());
                } catch (IllegalArgumentException e) {
                    afficherErreur("Erreur", e.getMessage());
                }
            }, false);
        });
    }

    private void creerConnexion() {
        Reseau reseau = controller.getReseau();

        if (reseau.getMaisons().isEmpty() || reseau.getGenerateurs().isEmpty()) {
            afficherErreur("Connexion impossible",
                    "Il faut au moins une maison et un générateur.");
            return;
        }

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Créer une connexion");
        dialog.setHeaderText("Connecter une maison à un générateur");

        ButtonType btnCreer = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnCreer, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> maisonCombo = new ComboBox<>();
        maisonCombo.getItems().addAll(reseau.getMaisons().keySet());

        ComboBox<String> genCombo = new ComboBox<>();
        genCombo.getItems().addAll(reseau.getGenerateurs().keySet());

        grid.add(new Label("Maison:"), 0, 0);
        grid.add(maisonCombo, 1, 0);
        grid.add(new Label("Générateur:"), 0, 1);
        grid.add(genCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnCreer) {
                if (maisonCombo.getValue() == null || genCombo.getValue() == null) {
                    afficherErreur("Erreur", "Veuillez sélectionner une maison et un générateur.");
                    return null;
                }
                return new String[]{maisonCombo.getValue(), genCombo.getValue()};
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String nomMaison = data[0];
            String nomGen = data[1];

            // CONTRAINTE: Vérifier si la maison est déjà connectée
            if (reseau.connexionExistePourMaison(nomMaison)) {
                List<String> gensConnectes = reseau.getConnexions().get(nomMaison);
                String dejaConnecteA = gensConnectes.get(0);

                Alert confirm = new Alert(Alert.AlertType.WARNING);
                confirm.setTitle("Maison déjà connectée");
                confirm.setHeaderText("La maison '" + nomMaison + "' est déjà connectée au générateur '" + dejaConnecteA + "'.");
                confirm.setContentText("Une maison ne peut être connectée qu'à un seul générateur.\n\n" +
                        "Voulez-vous remplacer la connexion existante?");
                confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

                Optional<ButtonType> confirmResult = confirm.showAndWait();
                if (confirmResult.isPresent() && confirmResult.get() == ButtonType.YES) {
                    controller.modifierConnexion(nomMaison, nomGen);
                    rafraichirGraphe();
                    rafraichirInfos();
                    statusLabel.setText("Connexion modifiée: " + nomMaison + " → " + nomGen);
                }
            } else {
                controller.addConnexion(nomMaison, nomGen);
                rafraichirGraphe();
                rafraichirInfos();
                statusLabel.setText("Connexion créée: " + nomMaison + " ↔ " + nomGen);
            }
        });
    }

    private void supprimerConnexion() {
        Reseau reseau = controller.getReseau();

        if (reseau.getConnexions().isEmpty()) {
            afficherErreur("Aucune connexion", "Il n'y a aucune connexion à supprimer.");
            return;
        }

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Supprimer une connexion");
        dialog.setHeaderText("Sélectionnez la connexion à supprimer");

        ButtonType btnSupprimer = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSupprimer, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> maisonCombo = new ComboBox<>();
        maisonCombo.getItems().addAll(reseau.getConnexions().keySet());

        ComboBox<String> genCombo = new ComboBox<>();

        maisonCombo.setOnAction(e -> {
            String maison = maisonCombo.getValue();
            if (maison != null) {
                genCombo.getItems().clear();
                genCombo.getItems().addAll(reseau.getConnexions().get(maison));
            }
        });

        grid.add(new Label("Maison:"), 0, 0);
        grid.add(maisonCombo, 1, 0);
        grid.add(new Label("Générateur:"), 0, 1);
        grid.add(genCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSupprimer) {
                if (maisonCombo.getValue() == null || genCombo.getValue() == null) {
                    afficherErreur("Erreur", "Veuillez sélectionner une connexion complète.");
                    return null;
                }
                return new String[]{maisonCombo.getValue(), genCombo.getValue()};
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String nomMaison = data[0];
            String nomGen = data[1];

            // CONTRAINTE: Vérifier que la connexion existe
            if (!reseau.connexionExiste(nomMaison, nomGen)) {
                afficherErreur("Connexion inexistante",
                        "La connexion entre " + nomMaison + " et " + nomGen + " n'existe pas.");
                return;
            }

            controller.supprimerConnexion(nomMaison, nomGen);
            rafraichirGraphe();
            rafraichirInfos();
            statusLabel.setText("Connexion supprimée: " + nomMaison + " ↔ " + nomGen);
        });
    }

    private void modifierConnexion() {
        Reseau reseau = controller.getReseau();

        if (reseau.getConnexions().isEmpty()) {
            afficherErreur("Aucune connexion", "Il n'y a aucune connexion à modifier.");
            return;
        }

        // Dialogue en deux étapes: ancienne connexion puis nouvelle
        Dialog<String[]> dialog1 = new Dialog<>();
        dialog1.setTitle("Modifier une connexion - Étape 1");
        dialog1.setHeaderText("Sélectionnez la connexion à modifier");

        ButtonType btnSuivant = new ButtonType("Suivant", ButtonBar.ButtonData.OK_DONE);
        dialog1.getDialogPane().getButtonTypes().addAll(btnSuivant, ButtonType.CANCEL);

        GridPane grid1 = new GridPane();
        grid1.setHgap(10);
        grid1.setVgap(10);
        grid1.setPadding(new Insets(20));

        ComboBox<String> maisonCombo1 = new ComboBox<>();
        maisonCombo1.getItems().addAll(reseau.getConnexions().keySet());

        ComboBox<String> genCombo1 = new ComboBox<>();

        maisonCombo1.setOnAction(e -> {
            String maison = maisonCombo1.getValue();
            if (maison != null) {
                genCombo1.getItems().clear();
                genCombo1.getItems().addAll(reseau.getConnexions().get(maison));
            }
        });

        grid1.add(new Label("Maison:"), 0, 0);
        grid1.add(maisonCombo1, 1, 0);
        grid1.add(new Label("Générateur actuel:"), 0, 1);
        grid1.add(genCombo1, 1, 1);

        dialog1.getDialogPane().setContent(grid1);

        dialog1.setResultConverter(dialogButton -> {
            if (dialogButton == btnSuivant) {
                if (maisonCombo1.getValue() == null || genCombo1.getValue() == null) {
                    afficherErreur("Erreur", "Veuillez sélectionner une connexion complète.");
                    return null;
                }
                return new String[]{maisonCombo1.getValue(), genCombo1.getValue()};
            }
            return null;
        });

        Optional<String[]> result1 = dialog1.showAndWait();
        result1.ifPresent(anciennes -> {
            String nomMaison = anciennes[0];
            String ancienGen = anciennes[1];

            // Deuxième dialogue: nouveau générateur
            Dialog<String> dialog2 = new Dialog<>();
            dialog2.setTitle("Modifier une connexion - Étape 2");
            dialog2.setHeaderText("Nouvelle connexion pour " + nomMaison);

            ButtonType btnModifier = new ButtonType("Modifier", ButtonBar.ButtonData.OK_DONE);
            dialog2.getDialogPane().getButtonTypes().addAll(btnModifier, ButtonType.CANCEL);

            GridPane grid2 = new GridPane();
            grid2.setHgap(10);
            grid2.setVgap(10);
            grid2.setPadding(new Insets(20));

            Label infoLabel = new Label("Maison: " + nomMaison);
            infoLabel.setStyle("-fx-font-weight: bold;");

            ComboBox<String> genCombo2 = new ComboBox<>();
            genCombo2.getItems().addAll(reseau.getGenerateurs().keySet());

            grid2.add(infoLabel, 0, 0, 2, 1);
            grid2.add(new Label("Nouveau générateur:"), 0, 1);
            grid2.add(genCombo2, 1, 1);

            dialog2.getDialogPane().setContent(grid2);

            dialog2.setResultConverter(dialogButton -> {
                if (dialogButton == btnModifier) {
                    if (genCombo2.getValue() == null) {
                        afficherErreur("Erreur", "Veuillez sélectionner un générateur.");
                        return null;
                    }
                    return genCombo2.getValue();
                }
                return null;
            });

            Optional<String> result2 = dialog2.showAndWait();
            result2.ifPresent(nouveauGen -> {
                // CONTRAINTE: La maison doit rester la même
                if (!reseau.generateurExiste(nouveauGen)) {
                    afficherErreur("Erreur", "Le générateur sélectionné n'existe pas.");
                    return;
                }

                controller.modifierConnexion(nomMaison, nouveauGen);
                rafraichirGraphe();
                rafraichirInfos();
                statusLabel.setText("Connexion modifiée: " + nomMaison + " : " + ancienGen + " → " + nouveauGen);
            });
        });
    }

    private void calculerEtAfficherCout() {
        // CONTRAINTE: Le réseau doit être valide
        List<String> problemes = controller.validerConfigurationReseau();
        if (!problemes.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Configuration invalide");
            alert.setHeaderText("Impossible de calculer le coût");
            alert.setContentText("Le réseau contient des erreurs:\n\n" +
                    String.join("\n", problemes));
            alert.showAndWait();
            return;
        }

        double[] couts = controller.calculerCoutReseau();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Résultat du calcul");
        alert.setHeaderText("Coût du réseau électrique");

        String contenu = String.format(
                "Dispersion (Disp(S)): %.4f\n" +
                        "Surcharge (Surcharge(S)): %.4f\n" +
                        "Coût total (Cout(S)): %.4f\n\n" +
                        "λ (lambda): %d",
                couts[1], couts[2], couts[0], controller.getReseau().getLambda()
        );

        alert.setContentText(contenu);
        alert.showAndWait();

        coutLabel.setText(String.format("Coût: %.4f", couts[0]));
        statusLabel.setText("Coût calculé avec succès");
    }

    private void lancerOptimisation() {
        // CONTRAINTE: Vérifier que le réseau a des éléments
        Reseau reseau = controller.getReseau();
        if (reseau.getMaisons().isEmpty() || reseau.getGenerateurs().isEmpty()) {
            afficherErreur("Optimisation impossible",
                    "Le réseau doit contenir au moins un générateur et une maison.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Optimisation automatique");
        confirmDialog.setHeaderText("Lancer l'algorithme génétique?");
        confirmDialog.setContentText("Cette opération peut prendre quelques secondes.\n" +
                "L'algorithme modifiera les connexions pour minimiser le coût.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            statusLabel.setText("Optimisation en cours...");

            // Exécuter en arrière-plan
            new Thread(() -> {
                long debut = System.currentTimeMillis();
                double[] avant = controller.calculerCoutReseau();

                GeneticAlgorithm solver = new GeneticAlgorithm(controller.getReseau());
                solver.solve(50, 1000, 0.1);

                double[] apres = controller.calculerCoutReseau();
                long duree = System.currentTimeMillis() - debut;

                javafx.application.Platform.runLater(() -> {
                    rafraichirGraphe();
                    rafraichirInfos();
                    coutLabel.setText(String.format("Coût: %.4f", apres[0]));

                    Alert resultDialog = new Alert(Alert.AlertType.INFORMATION);
                    resultDialog.setTitle("Optimisation terminée");
                    resultDialog.setHeaderText("Résultats de l'optimisation");

                    double amelioration = avant[0] > 0 ? ((avant[0] - apres[0]) / avant[0]) * 100 : 0;

                    resultDialog.setContentText(String.format(
                            "Coût avant: %.4f\n" +
                                    "Coût après: %.4f\n" +
                                    "Amélioration: %.2f%%\n" +
                                    "Durée: %d ms",
                            avant[0], apres[0], amelioration, duree
                    ));
                    resultDialog.showAndWait();

                    statusLabel.setText("Optimisation terminée");
                });
            }).start();
        }
    }

    private void validerConfiguration() {
        List<String> problemes = controller.validerConfigurationReseau();

        if (problemes.isEmpty()) {
            afficherInfo("Configuration valide", "✓ Le réseau est correctement configuré.\n\n" +
                    "Toutes les maisons sont connectées à un unique générateur.");
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Configuration invalide");
            alert.setHeaderText("Problèmes détectés (" + problemes.size() + "):");
            alert.setContentText(String.join("\n", problemes));
            alert.showAndWait();
        }
    }

    private void afficherDetailsReseau() {
        Reseau reseau = controller.getReseau();
        StringBuilder sb = new StringBuilder();

        sb.append("=== RÉSEAU ÉLECTRIQUE ===\n\n");

        sb.append("GÉNÉRATEURS (" + reseau.getGenerateurs().size() + "):\n");
        for (Generateur gen : reseau.getGenerateurs().values()) {
            int charge = calculerChargeGenerateur(reseau, gen.getNom());
            sb.append(String.format("  • %s: %d/%d kW", gen.getNom(), charge, gen.getCapaciteMax()));
            if (charge > gen.getCapaciteMax()) {
                sb.append(" ⚠ SURCHARGE");
            }
            sb.append("\n");
        }

        sb.append("\nMAISONS (" + reseau.getMaisons().size() + "):\n");
        for (Maison maison : reseau.getMaisons().values()) {
            sb.append(String.format("  • %s: %s (%d kW)",
                    maison.getNom(), maison.getConsommation(), maison.getConsommationKw()));

            List<String> gens = reseau.getConnexions().get(maison.getNom());
            if (gens == null || gens.isEmpty()) {
                sb.append(" → NON CONNECTÉE");
            } else if (gens.size() == 1) {
                sb.append(" → " + gens.get(0));
            } else {
                sb.append(" → ERREUR: " + gens.size() + " connexions");
            }
            sb.append("\n");
        }

        sb.append("\nSTATISTIQUES:\n");
        int capaciteTotale = reseau.getGenerateurs().values().stream()
                .mapToInt(Generateur::getCapaciteMax).sum();
        int demandeTotale = reseau.getMaisons().values().stream()
                .mapToInt(Maison::getConsommationKw).sum();
        int nbConnexions = reseau.getConnexions().values().stream()
                .mapToInt(List::size).sum();

        sb.append(String.format("  • Capacité totale: %d kW\n", capaciteTotale));
        sb.append(String.format("  • Demande totale: %d kW\n", demandeTotale));
        sb.append(String.format("  • Connexions: %d\n", nbConnexions));
        sb.append(String.format("  • Lambda (λ): %d\n", reseau.getLambda()));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du réseau");
        alert.setHeaderText(null);

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private int calculerChargeGenerateur(Reseau reseau, String nomGen) {
        int charge = 0;
        for (java.util.Map.Entry<String, List<String>> entry : reseau.getConnexions().entrySet()) {
            if (entry.getValue().contains(nomGen)) {
                Maison maison = reseau.getMaisons().get(entry.getKey());
                if (maison != null) {
                    charge += maison.getConsommationKw();
                }
            }
        }
        return charge;
    }

    private void afficherAPropos() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("À propos");
        alert.setHeaderText("Réseau Électrique - Version 2.0");
        alert.setContentText(
                "Projet de Programmation Avancée\n" +
                        "Université Paris Cité\n" +
                        "Licence 3 Informatique\n\n" +
                        "Gestion et optimisation de réseaux électriques\n" +
                        "avec algorithme génétique.\n\n" +
                        "Architecture: MVC (Model-View-Controller)\n" +
                        "Interface: JavaFX"
        );
        alert.showAndWait();
    }

    private void afficherGuide() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Guide d'utilisation");
        alert.setHeaderText("Comment utiliser l'application");
        alert.setContentText(
                "CRÉATION:\n" +
                        "1. Ajoutez des générateurs (Menu ou bouton)\n" +
                        "2. Cliquez sur le graphe pour les placer\n" +
                        "3. Ajoutez des maisons\n" +
                        "4. Créez des connexions\n\n" +
                        "ANALYSE:\n" +
                        "• Validez la configuration (chaque maison = 1 générateur)\n" +
                        "• Calculez le coût\n" +
                        "• Optimisez avec l'algorithme génétique\n\n" +
                        "NAVIGATION:\n" +
                        "• Molette: Zoom\n" +
                        "• Glisser fond: Déplacer la vue\n" +
                        "• Glisser nœud: Déplacer un élément\n\n" +
                        "CONTRAINTES:\n" +
                        "• Une maison = exactement un générateur\n" +
                        "• Capacité totale ≥ Demande totale"
        );
        alert.showAndWait();
    }

    private void rafraichirGraphe() {
        graphPane.rafraichir();
    }

    private void rafraichirInfos() {
        Reseau reseau = controller.getReseau();
        infoGenerateurs.setText("Générateurs: " + reseau.getGenerateurs().size());
        infoMaisons.setText("Maisons: " + reseau.getMaisons().size());

        int nbConnexions = reseau.getConnexions().values().stream()
                .mapToInt(List::size).sum();
        infoConnexions.setText("Connexions: " + nbConnexions);
    }

    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherInfo(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}