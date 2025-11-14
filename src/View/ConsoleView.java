package View;

import Controller.AppController;
import Model.Generateur;
import Model.Maison;
import Model.Reseau;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;


// Gere tous les affichages et les saisies de la console.
public class ConsoleView {
    private final AppController controller;
    private final Scanner scanner;

    public ConsoleView(AppController controller) {
        this.controller = controller;
        this.scanner = new Scanner(System.in);
    }

    // Demarrage la boucle principale de l'application.
    public void start() {
        gererMenuConfiguration();
    }

    // --- Gestion des menus ---
    // Menu 1 : Configuration du reseau
    private void gererMenuConfiguration() {
        boolean configurationTerminee = false;
        while (!configurationTerminee) {
            afficherMenuPrincipal();
            String choix = scanner.nextLine();
            switch (choix) {
                case "1":
                    traiterAjoutGenerateur();
                    break;
                case "2":
                    traiterAjoutMaison();
                    break;
                case "3":
                    traiterAjoutConnexion();
                    break;
                case "4":
                    traiterSuppressionConnexion();
                    break;
                case "5":
                    List<String> problemes = controller.validerConfigurationReseau();
                    if (problemes.isEmpty()) {
                        afficherMessage("Configuration terminee et validee");
                        configurationTerminee = true;
                        gererMenuAnalyse();
                    } else {
                        afficherProblemesConfiguration(problemes);
                    }
                    break;
                default:
                    afficherErreur("Choix invalide. Veuillez reessayer.");
            }
        }
    }

    //Menu 2 : Analyse du reseau
    private void gererMenuAnalyse() {
        boolean quitter = false;
        while (!quitter) {
            afficherMenuAnalyse();
            String choix = scanner.nextLine();
            switch (choix) {
                case "1":
                    double[] couts = controller.calculerCoutReseau();
                    afficherCout(couts[0], couts[1], couts[2]);
                    break;
                case "2":
                    traiterModificationConnexion();
                    break;
                case "3":
                    afficherReseau(controller.getReseau());
                    break;
                case "4":
                    quitter = true;
                    System.out.println("Programme termine");
                    break;
                default:
                    afficherErreur("Choix invalide. Veuillez reessayer.");
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
        int capacite = Integer.parseInt(entrees[1]);

        boolean existed = controller.addGenerateur(nom, capacite);
        if (existed) {
            afficherAvertissement("Le generateur " + nom + " a ete mis à jour avec succes.");
        } else {
            afficherMessage("Generateur " + nom + " ajoutee");
        }
    }

    // Ajout d'une maison
    private void traiterAjoutMaison() {
        System.out.print("Entrez le nom et le type de consommation (BASSE, NORMALE, FORTE) : ");
        String[] entrees = scanner.nextLine().split(" ");
        if (entrees.length != 2) {
            afficherErreur("Format incorrect");
            return;
        }
        String nom = entrees[0];
        try {
            boolean existed = controller.addMaison(nom, entrees[1]);
            if (existed) {
                afficherAvertissement("La maison " + nom + " a ete mise à jour avec succes.");
            } else {
                afficherMessage("Maison " + nom + " ajoutee.");
            }
        } catch (IllegalArgumentException e) {
            afficherErreur(e.getMessage());
        }
    }

    // Ajout d'une connexion entre une maison et un generateur
    private void traiterAjoutConnexion() {
        System.out.print("Entrez le nom de la maison et du generateur à connecter (ex: M1 G1) : ");
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
            afficherErreur("La maison ou le generateur specifie n'existe pas.");
            return;
        }

        controller.addConnexion(nomMaison, nomGenerateur);
        afficherMessage("Connexion cree entre " + nomMaison + " et " + nomGenerateur + ".");
    }

    // Suppression d'une connexion entre une maison et un generateur
    private void traiterSuppressionConnexion() {
        System.out.print("Entrez le nom de la maison et du generateur à deconnecter (ex: M1 G1) : ");
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
            afficherErreur("La maison ou le generateur specifie n'existe pas.");
            return;
        }

        if (!reseau.connexionExiste(nomMaison, nomGenerateur)) {
            afficherErreur("La connexion entre " + nomMaison + " et " + nomGenerateur + " n'existe pas.");
            return;
        }

        controller.supprimerConnexion(nomMaison, nomGenerateur);
        afficherMessage("Connexion supprimee entre " + nomMaison + " et " + nomGenerateur + ".");
    }

    // Modification d'une connexion existante
    private void traiterModificationConnexion() {
        Reseau reseau = controller.getReseau();

        System.out.print("Veuillez saisir la connexion à modifier (ex: M1 G1): ");
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
            afficherErreur("La maison ou le generateur specifie n'existe pas.");
            return;
        }

        if (!reseau.connexionExiste(nomMaisonAncienne, nomGenAncien)) {
            afficherErreur("La connexion '" + ancienne[0] + " " + ancienne[1] + "' n'existe pas");
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
            afficherErreur("La maison ou le generateur specifie n'existe pas.");
            return;
        }

        if (!nomMaisonAncienne.equals(nomMaisonNouvelle) || !reseau.generateurExiste(nomGenNouveau)) {
            afficherErreur("Saisie invalide (la maison doit etre la meme, et le nouveau generateur doit exister)");
            return;
        }

        controller.modifierConnexion(nomMaisonNouvelle, nomGenNouveau);

        afficherMessage(
                "Connexion pour " + nomMaisonNouvelle + " modifiee de " + nomGenAncien + " à " + nomGenNouveau + ".");
    }

    // --- Methodes d'affichage ---7
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

    // Affichage du menu d'analyse
    public void afficherMenuAnalyse() {
        System.out.println("\n--- MENU D'ANALYSE ---");
        System.out.println("1) Calculer le cout du reseau electrique actuel");
        System.out.println("2) Modifier une connexion");
        System.out.println("3) Afficher le reseau");
        System.out.println("4) Fin");
        System.out.print("Votre choix : ");
    }

    // --- Messages d'information ---
    public void afficherMessage(String message) {
        System.out.println("INFO: " + message);
    }

    public void afficherAvertissement(String message) {
        System.out.println("AVERTISSEMENT: " + message);
    }

    public void afficherErreur(String message) {
        System.out.println("ERREUR: " + message);
    }

    public void afficherCout(double coutTotal, double dispersion, double surcharge) {
        System.out.println("\n--- RESULTAT DU CALCUL DE COUT ---");
        System.out.printf("Dispersion (Disp(S))     : %.4f\n", dispersion);
        System.out.printf("Surcharge (Surcharge(S)) : %.4f\n", surcharge);
        System.out.printf("Cout total (Cout(S))     : %.4f\n", coutTotal);
    }

    public void afficherProblemesConfiguration(List<String> problemes) {
        afficherErreur("La configuration du reseau est incomplete.");
        System.out.println("Problemes detectes :");
        for (String probleme : problemes) {
            System.out.println("- " + probleme);
        }
    }

    public void afficherReseau(Reseau reseau) {
        System.out.println("\n--- ETAT ACTUEL DU RESEAU ELECTRIQUE ---");

        if (reseau.getGenerateurs().isEmpty()) {
            System.out.println("Aucun generateur dans le reseau.");
        } else {
            System.out.println("\n>> Genérateurs et connexions :");

            // Recrée la map inversée (Générateur -> Liste de Maisons) manuellement
            Map<String, List<String>> maisonsParGenerateur = new HashMap<>();
            for (String nomGenerateur : reseau.getGenerateurs().keySet()) {
                maisonsParGenerateur.put(nomGenerateur, new ArrayList<>());
            }

            for (Map.Entry<String, List<String>> entry : reseau.getConnexions().entrySet()) {
                String nomMaison = entry.getKey();
                List<String> generateursConnectes = entry.getValue();

                // Ajoute la maison à la liste de CHACUN de ses générateurs
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
                            System.out.printf("  -> Connecte à %s (%d kW)\n", maison.getNom(), maison.getConsommationKw());
                        }
                    }
                }
            }
        }

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
                    statut = "connectee à " + gens.get(0);
                } else {
                    statut = "connectee à PLUSIEURS: " + String.join(", ", gens);
                }

                System.out.printf("- %s (%s kW) - %s\n", maison.getNom(), maison.getConsommation().name(), statut);
            }
        }
        System.out.println("------------------------------------------");
    }
}
