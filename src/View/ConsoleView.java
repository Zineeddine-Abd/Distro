package View;

import static Model.Constants.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import Algorithms.FileAlgorithms;
import Algorithms.GeneticAlgorithm;
import Controller.AppController;
import Exceptions.FileSyntaxException;
import Exceptions.InvalidFileExtensionException;
import Exceptions.ReseauInvalideException;
import Exceptions.ReseauInvalideSyntaxException;
import Model.Generateur;
import Model.Maison;
import Model.Reseau;

// Gere tous les affichages et les saisies de la console.

public class ConsoleView {
    // Reference au controller de l'application
    private final AppController controller;

    public static final String YELLOW = "\033[0;33m";

    // Scanner pour lire les entrees utilisateur
    private final Scanner scanner;

    // Constructeur
    public ConsoleView(AppController controller) {
        this.controller = controller;
        this.scanner = new Scanner(System.in);
    }

    // Démarrage la boucle principale de l'application.
    public void start(String[] args) {
        // Case 1: pas d'arguments -> Mode Manuel (Part 1)
        if (args.length == 0) {
            System.out.println("Mode manuel active (Aucun fichier fourni).");
            gererMenuConfiguration();
        }
        // Case 2: Avec arguments -> Mode fichier (Part 2)
        else {
            String cheminFichier = args[0];
            int lambda = LAMBDA;
            // Verifier si lambda est args[1]
            if (args.length >= 2) {
                try {
                    lambda = Integer.parseInt(args[1]);

                    // Validé que lambda est positif
                    if (lambda <= 0) {
                        System.err.println("ERREUR : La penalite (lambda) doit etre un entier positif.");
                        System.exit(1);
                    }

                } catch (NumberFormatException e) {
                    // gerer l'erreur de format de lambda
                    System.err.println(
                            "ERREUR : La valeur de penalite '" + args[1] + "' n est pas valide (entier attendu).");
                    System.exit(1);
                }
            } else {
                System.out.println(
                        "INFO : Pas de penalite specifiee, utilisation de la valeur par defaut (lu depuis le fichier des constantes Model/Constants.java) ("
                                + LAMBDA + ").");
            }

            try {
                cheminFichier = FileAlgorithms.normalizeTxtPathForLoad(cheminFichier);
                System.out.println("Chargement du fichier : " + cheminFichier + " ...");

                // 1- Charger et valider le reseau depuis le fichier
                Reseau reseau = FileAlgorithms.chargerReseau(cheminFichier);
                reseau.setLambda(lambda);
                AppController controller = new AppController(reseau);

                System.out.println("Fichier charge avec succes (Lambda = " + lambda + ").");

                List<String> warnings = FileAlgorithms.consumeLastLoadWarnings();
                if (!warnings.isEmpty()) {
                    afficherAvertissement("Avertissements detectes lors du chargement :");
                    for (String w : warnings) {
                        afficherAvertissement(w);
                    }
                }

                // 2- Declancher le menu de la partie 2
                gererMenuResolution(controller);

            } catch (InvalidFileExtensionException e) {
                System.err.println("ERREUR FATALE : " + e.getMessage());
                System.exit(1);
            } catch (ReseauInvalideSyntaxException e) {
                // Gerer les erreurs de syntaxe dans le fichier
                System.err.println("ERREUR FATALE : Syntaxe invalide dans le fichier d entree.");
                System.err.println(e.getMessage());
                System.exit(1);
            } catch (ReseauInvalideException | FileSyntaxException e) {
                // Gerer les erreurs de configuration logique du reseau
                System.err.println("ERREUR FATALE : Le reseau charge est invalide.");
                System.err.println(e.getMessage());
                System.exit(1);
            } catch (IOException e) {
                // Gerer les erreurs de chargement du fichier
                System.err.println("ERREUR FATALE : Impossible de charger le reseau.");
                System.err.println(e.getMessage());
                System.exit(1);
            } catch (Exception e) {
                System.err.println("Programme interrompu.");
                e.printStackTrace();
                System.exit(1);
            }

        }
    }

    // --- Gestion des menus ---
    // Menu 1 : Configuration du reseau
    private void gererMenuConfiguration() {
        boolean configurationTerminee = false;
        while (!configurationTerminee) {
            afficherMenuPrincipal();
            String input = scanner.nextLine();
            int choix;
            try {
                choix = Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                afficherErreur("Choix invalide. Veuillez saisir un entier (1 a 5).");
                continue;
            }

            if (choix < 1 || choix > 5) {
                afficherErreur("Choix invalide. Veuillez saisir un entier (1 a 5).");
                continue;
            }

            switch (choix) {
                case 1:
                    traiterAjoutGenerateur();
                    break;
                case 2:
                    traiterAjoutMaison();
                    break;
                case 3:
                    traiterAjoutConnexion();
                    break;
                case 4:
                    traiterSuppressionConnexion();
                    break;
                case 5:
                    List<String> problemes = controller.validerConfigurationReseau();
                    if (problemes.isEmpty()) {
                        afficherMessage("Configuration terminee et validee");
                        configurationTerminee = true;
                        // Bascule directement vers le menu de resolution
                        gererMenuResolution(controller);
                    } else {
                        afficherProblemesConfiguration(problemes);
                    }
                    break;
                default:
                    afficherErreur("Choix invalide. Veuillez reessayer.");
            }
        }
    }

    // Menu 2 : Analyse du reseau
    private void gererMenuAnalyse() {
        boolean quitter = false;
        while (!quitter) {
            afficherMenuAnalyse();
            String input = scanner.nextLine();
            int choix;
            try {
                choix = Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                afficherErreur("Choix invalide. Veuillez saisir un entier (1 a 4).");
                continue;
            }

            if (choix < 1 || choix > 4) {
                afficherErreur("Choix invalide. Veuillez saisir un entier (1 a 4).");
                continue;
            }

            switch (choix) {
                case 1:
                    double[] couts = controller.calculerCoutReseau();
                    afficherCout(couts[0], couts[1], couts[2]);
                    break;
                case 2:
                    traiterModificationConnexion();
                    break;
                case 3:
                    afficherReseau(controller.getReseau());
                    break;
                case 4:
                    quitter = true;
                    System.out.println("Programme terminee");
                    break;
                default:
                    afficherErreur("Choix invalide. Veuillez reessayer.");
            }
        }
    }

    // Menu 3 : Reseau charge depuis fichier (Partie 2)
    private void gererMenuResolution(AppController controller) {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;

            // Affiche l'etat et le cout courant avant toute action
            afficherReseauEtCout(controller);

            while (running) {
                afficherMenuPrincipalPartie2();

                String input = scanner.nextLine();
                int choix;
                try {
                    choix = Integer.parseInt(input.trim());
                } catch (NumberFormatException e) {
                    System.out.println("Choix invalide. Veuillez reessayer en saisissant 1, 2 ou 3.");
                    continue;
                }

                if (choix < 1 || choix > 3) {
                    System.out.println("Choix invalide. Veuillez reessayer en saisissant 1, 2 ou 3.");
                    continue;
                }

                switch (choix) {
                    // case "1":
                    // double[] couts_avant = controller.calculerCoutReseau();
                    // afficherCout(couts_avant[0], couts_avant[1], couts_avant[2]);
                    // break;
                    // case "2":
                    case 1:
                        long avant = System.currentTimeMillis();

                        try {
                            // Creation du solveur avec le reseau actuel
                            GeneticAlgorithm solver = new GeneticAlgorithm(controller.getReseau());

                            // Paramètres vont être calculé automatiquement par le solver
                            solver.solve();

                            // Affichage du resultat
                            afficherReseau(controller.getReseau());
                            double[] couts_apres = controller.calculerCoutReseau();

                            System.out.println("Optimisation terminee.");
                            afficherCout(couts_apres[0], couts_apres[1], couts_apres[2]);

                            long apres = System.currentTimeMillis();
                            System.out.println("Temps ecoule (ms) : " + (apres - avant));
                        } catch (IllegalStateException e) {
                            afficherErreur(e.getMessage());
                        }
                        break;
                    case 2:
                        System.out.print("Entrez le nom du fichier de sauvegarde : ");
                        String savePath = scanner.nextLine();
                        try {
                            controller.sauvegarderReseau(FileAlgorithms.normalizeTxtPathForSave(savePath));
                        } catch (Exception e) {
                            System.out.println("Erreur lors de la sauvegarde : " + e.getMessage());
                        }
                        break;
                    case 3:
                        running = false;
                        System.out.println("Programme terminee");
                        break;
                    default:
                        System.out.println("Choix invalide. Veuillez reessayer en saisissant 1, 2 ou 3.");
                }
            }
        }
    }

    // --- Logique de traitement des entrees utilisateur ---
    // Ajout d'un generateur
    private void traiterAjoutGenerateur() {
        System.out.print("Entrez le nom et la capacite du generateur (ex: G1 60) : ");
        String[] entrees = scanner.nextLine().split(" ");
        if (entrees.length != 2) {
            afficherErreur("Format incorrect");
            return;
        }
        String nom = entrees[0];
        int capacite;
        try {
            capacite = Integer.parseInt(entrees[1]);
            if (capacite <= 0) {
                afficherErreur("La capacite doit etre un entier strictement positif.");
                return;
            }
        } catch (NumberFormatException e) {
            afficherErreur("La capacite doit etre un entier valide.");
            return;
        }

        boolean existed = controller.addGenerateur(nom, capacite);
        if (existed) {
            afficherAvertissement("Le generateur " + nom + " a ete mis a jour avec succes.");
        } else {
            afficherMessage("Generateur " + nom + " ajoutee");
        }
    }

    // Ajout d'une maison
    private void traiterAjoutMaison() {
        System.out.print("Entrez le nom et le type de consommation (BASSE, NORMALE, FORTE) (ex: M1 BASSE) : ");
        String[] entrees = scanner.nextLine().split(" ");
        if (entrees.length != 2) {
            afficherErreur("Format incorrect");
            return;
        }
        String nom = entrees[0];
        try {
            boolean existed = controller.addMaison(nom, entrees[1]);
            if (existed) {
                afficherAvertissement("La maison " + nom + " a ete mise a jour avec succes.");
            } else {
                afficherMessage("Maison " + nom + " ajoutee.");
            }
        } catch (IllegalArgumentException e) {
            afficherErreur(e.getMessage());
        }
    }

    // Ajout d'une connexion entre une maison et un generateur
    private void traiterAjoutConnexion() {
        System.out.print("Entrez le nom de la maison et du generateur a connecter (ex: M1 G1) : ");
        String[] entrees = scanner.nextLine().split(" ");
        if (entrees.length != 2) {
            afficherErreur("Format incorrect");
            return;
        }
        Reseau reseau = controller.getReseau();
        String nom1 = entrees[0], nom2 = entrees[1];
        String nomMaison, nomGenerateur;

        if (reseau.maisonExiste(nom1) && reseau.generateurExiste(nom2)) {
            nomMaison = nom1;
            nomGenerateur = nom2;
        } else if (reseau.maisonExiste(nom2) && reseau.generateurExiste(nom1)) {
            nomMaison = nom2;
            nomGenerateur = nom1;
        } else {
            afficherErreur("La maison ou le generateur specifie n existe pas.");
            return;
        }

        controller.addConnexion(nomMaison, nomGenerateur);
        afficherMessage("Connexion cree entre " + nomMaison + " et " + nomGenerateur + ".");
    }

    // Suppression d'une connexion entre une maison et un generateur
    private void traiterSuppressionConnexion() {
        System.out.print("Entrez le nom de la maison et du generateur a deconnecter (ex: M1 G1) : ");
        String[] entrees = scanner.nextLine().split(" ");
        if (entrees.length != 2) {
            afficherErreur("Format incorrect");
            return;
        }
        Reseau reseau = controller.getReseau();
        String nom1 = entrees[0], nom2 = entrees[1];
        String nomMaison, nomGenerateur;

        if (reseau.maisonExiste(nom1) && reseau.generateurExiste(nom2)) {
            nomMaison = nom1;
            nomGenerateur = nom2;
        } else if (reseau.maisonExiste(nom2) && reseau.generateurExiste(nom1)) {
            nomMaison = nom2;
            nomGenerateur = nom1;
        } else {
            afficherErreur("La maison ou le generateur specifie n existe pas.");
            return;
        }

        if (!reseau.connexionExiste(nomMaison, nomGenerateur)) {
            afficherErreur("La connexion entre " + nomMaison + " et " + nomGenerateur + " n existe pas.");
            return;
        }

        controller.supprimerConnexion(nomMaison, nomGenerateur);
        afficherMessage("Connexion supprimee entre " + nomMaison + " et " + nomGenerateur + ".");
    }

    // Modification d'une connexion existante
    private void traiterModificationConnexion() {
        Reseau reseau = controller.getReseau();

        System.out.print("Veuillez saisir la connexion a modifier (ex: M1 G1): ");
        String[] ancienne = scanner.nextLine().split(" ");
        if (ancienne.length != 2) {
            afficherErreur("Format incorrect");
            return;
        }

        String nom1 = ancienne[0], nom2 = ancienne[1];
        String nomMaisonAncienne, nomGenAncien;

        if (reseau.maisonExiste(nom1) && reseau.generateurExiste(nom2)) {
            nomMaisonAncienne = nom1;
            nomGenAncien = nom2;
        } else if (reseau.maisonExiste(nom2) && reseau.generateurExiste(nom1)) {
            nomMaisonAncienne = nom2;
            nomGenAncien = nom1;
        } else {
            afficherErreur("La maison ou le generateur specifie n existe pas.");
            return;
        }

        if (!reseau.connexionExiste(nomMaisonAncienne, nomGenAncien)) {
            afficherErreur("La connexion '" + ancienne[0] + " " + ancienne[1] + "' n existe pas");
            return;
        }

        System.out.print("Veuillez saisir la nouvelle connexion (ex: M1 G2): ");
        String[] nouvelle = scanner.nextLine().split(" ");
        if (nouvelle.length != 2) {
            afficherErreur("Format incorrect");
            return;
        }

        nom1 = nouvelle[0];
        nom2 = nouvelle[1];

        String nomMaisonNouvelle, nomGenNouveau;

        if (reseau.maisonExiste(nom1) && reseau.generateurExiste(nom2)) {
            nomMaisonNouvelle = nom1;
            nomGenNouveau = nom2;
        } else if (reseau.maisonExiste(nom2) && reseau.generateurExiste(nom1)) {
            nomMaisonNouvelle = nom2;
            nomGenNouveau = nom1;
        } else {
            afficherErreur("La maison ou le generateur specifie n existe pas.");
            return;
        }

        if (!nomMaisonAncienne.equals(nomMaisonNouvelle) || !reseau.generateurExiste(nomGenNouveau)) {
            afficherErreur("Saisie invalide (la maison doit etre la meme, et le nouveau generateur doit exister)");
            return;
        }

        controller.modifierConnexion(nomMaisonNouvelle, nomGenNouveau);

        afficherMessage(
                "Connexion pour " + nomMaisonNouvelle + " modifiee de " + nomGenAncien + " a " + nomGenNouveau + ".");
    }

    // // --- Helpers testables (parsing) ---
    // public static int parsePositiveInt(String value) {
    // int parsed = Integer.parseInt(value.trim());
    // if (parsed <= 0) {
    // throw new IllegalArgumentException("La valeur doit etre un entier strictement
    // positif.");
    // }
    // return parsed;
    // }

    // public static int parseLambda(String value) {
    // return parsePositiveInt(value);
    // }

    // public static Consommation parseConsommationToken(String token) {
    // return Consommation.fromString(token);
    // }

    // public static int parseMenuChoice(String input, int maxOption) {
    // int choice = Integer.parseInt(input.trim());
    // if (choice < 1 || choice > maxOption) {
    // throw new IllegalArgumentException("Choix invalide.");
    // }
    // return choice;
    // }

    // --- Méthodes d affichage ---
    // Affichage du menu principal
    public void afficherMenuPrincipal() {
        System.out.println("\n--- MENU DE CONFIGURATION ---");
        System.out.println("1) Ajouter un generateur");
        System.out.println("2) Ajouter une maison");
        System.out.println("3) Ajouter une connexion");
        System.out.println("4) Supprimer une connexion existante");
        System.out.println("5) Fin de la configuration");
        System.out.print("Votre choix : ");
    }

    // Affichage du menu d analyse
    public void afficherMenuAnalyse() {
        System.out.println("\n--- MENU D ANALYSE ---");
        System.out.println("1) Calculer le cout du reseau electrique actuel");
        System.out.println("2) Modifier une connexion");
        System.out.println("3) Afficher le reseau");
        System.out.println("4) Fin");
        System.out.print("Votre choix : ");
    }

    // Affichage du menu principal de la partie 2
    public static void afficherMenuPrincipalPartie2() {
        System.out.println("\n--- MENU PARTIE 2 ---");
        // System.out.println("1) Calculer le cout du reseau electrique actuel");
        System.out.println("1) Resolution automatique");
        System.out.println("2) Sauvegarder la solution actuelle");
        System.out.println("3) Fin");
        System.out.print("Votre choix : ");
    }

    // --- Messages d information ---
    public void afficherMessage(String message) {
        System.out.println("INFO: " + message);
    }

    public void afficherAvertissement(String message) {
        System.out.println("AVERTISSEMENT: " + message);
    }

    public void afficherErreur(String message) {
        System.out.println("ERREUR: " + message);
    }

    // Affichage du cout total, de la dispersion et de la surcharge
    public void afficherCout(double coutTotal, double dispersion, double surcharge) {
        System.out.println("\n--- RESULTAT DU CALCUL DE COUT ---");
        System.out.printf("Dispersion (Disp(S))     : %.4f\n", dispersion);
        System.out.printf("Surcharge (Surcharge(S)) : %.4f\n", surcharge);
        System.out.printf("Cout total (Cout(S))     : %.4f\n", coutTotal);
    }

    // Affiche a la fois le reseau et son cout courant
    private void afficherReseauEtCout(AppController controller) {
        afficherReseau(controller.getReseau());
        double[] couts = controller.calculerCoutReseau();
        afficherCout(couts[0], couts[1], couts[2]);
    }

    // Affichage des problemes de configuration
    public void afficherProblemesConfiguration(List<String> problemes) {
        afficherErreur("La configuration du reseau est incomplete.");
        System.out.println("Problemes detectes :");
        for (String probleme : problemes) {
            System.out.println("- " + probleme);
        }
    }

    // Affichage de l état actuel du reseau
    public void afficherReseau(Reseau reseau) {
        System.out.println("\n--- ETAT ACTUEL DU RESEAU ELECTRIQUE ---");

        // Affichage des generateurs et de leurs connexions
        if (reseau.getGenerateurs().isEmpty()) {
            System.out.println("Aucun generateur dans le reseau.");
        } else {
            System.out.println("\n>> Generateurs et connexions :");

            // Recree la map inversee (Generateur -> Liste de Maisons) manuellement
            Map<String, List<String>> maisonsParGenerateur = new HashMap<>();
            for (String nomGenerateur : reseau.getGenerateurs().keySet()) {
                maisonsParGenerateur.put(nomGenerateur, new ArrayList<>());
            }

            for (Map.Entry<String, List<String>> entry : reseau.getConnexions().entrySet()) {
                String nomMaison = entry.getKey();
                List<String> generateursConnectes = entry.getValue();

                // Ajoute la maison a la liste de CHACUN de ses generateurs
                for (String nomGenerateur : generateursConnectes) {
                    if (maisonsParGenerateur.containsKey(nomGenerateur)) {
                        maisonsParGenerateur.get(nomGenerateur).add(nomMaison);
                    }
                }
            }

            for (Generateur gen : reseau.getGenerateurs().values()) {
                System.out.printf("- %s (Capacite: %d kW)\n", gen.getNom(), gen.getCapaciteMax());
                List<String> maisonsConnectees = maisonsParGenerateur.getOrDefault(gen.getNom(), new ArrayList<>());
                if (maisonsConnectees.isEmpty()) {
                    System.out.println("  -> Ne connecte aucune maison.");
                } else {
                    for (String nomMaison : maisonsConnectees) {
                        Maison maison = reseau.getMaisons().get(nomMaison);
                        if (maison != null) {
                            System.out.printf("  -> Connecte a %s (%d kW)\n", maison.getNom(),
                                    maison.getConsommationKw());
                        }
                    }
                }
            }
        }

        // Affichage des maisons et de leurs connexions
        if (reseau.getMaisons().isEmpty()) {
            System.out.println("\nAucune maison dans le reseau.");
        } else {
            System.out.println("\n>> Liste des maisons :");
            for (Maison maison : reseau.getMaisons().values()) {

                List<String> gens = reseau.getConnexions().get(maison.getNom());
                String statut;
                if (gens == null || gens.isEmpty()) {
                    statut = "non connectee";
                } else if (gens.size() == 1) {
                    statut = "connectee a " + gens.get(0);
                } else {
                    statut = "connectee a PLUSIEURS: " + String.join(", ", gens);
                }

                System.out.printf("- %s (%s kW) - %s\n", maison.getNom(), maison.getConsommation().name(), statut);
            }
        }
        System.out.println("----------------------------------------");
    }
}