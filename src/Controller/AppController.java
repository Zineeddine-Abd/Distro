package Controller;

import Model.*;
import Algorithms.ReseauAlgorithms;
import Algorithms.FileAlgorithms;

import java.io.IOException;
import java.util.List;

/**
 * Il contient toute la logique applicative
 * agit comme un intermidiaire entre la Vue (Affichage) et le Modele (calcule et
 * logique metier/stockage).
 */

public class AppController {
    private Reseau reseau;

    public AppController(Reseau reseau) {
        this.reseau = reseau;
    }

    // Loads a network from file and replaces the current model
    public void chargerReseauDepuisFichier(String chemin) throws Exception {
        // Delegate logic to the static persistence class
        this.reseau = FileAlgorithms.chargerReseau(chemin);
    }

    // Saves current network
    public void sauvegarderReseau(String chemin) throws IOException {
        FileAlgorithms.sauvegarderReseau(this.reseau, chemin);
    }

    // --- Actions declenchees par le View ---
    // Ajoute ou met a jour un generateur dans le reseau
    // Retourne true si l'element existait deja (mis a jour), false sinon (ajout)
    public boolean addGenerateur(String nom, int capacite) {
        boolean existed = reseau.generateurExiste(nom);
        reseau.addOrUpdateGenerateur(new Generateur(nom, capacite));
        return existed;
    }

    // Ajoute ou met a jour une maison dans le reseau
    // Retourne true si l'element existait deja (mis a jour), false sinon (ajout)
    public boolean addMaison(String nom, String type) throws IllegalArgumentException {
        Consommation conso = Consommation.fromString(type);
        boolean existed = reseau.maisonExiste(nom);
        reseau.addOrUpdateMaison(new Maison(nom, conso));
        return existed;
    }

    // Ajoute une connexion entre une maison et un generateur
    public void addConnexion(String nomMaison, String nomGenerateur) {
        reseau.creerConnexion(nomMaison, nomGenerateur);
    }

    // Supprime une connexion entre une maison et un generateur
    public void supprimerConnexion(String nomMaison, String nomGenerateur) {
        reseau.supprimerConnexion(nomMaison, nomGenerateur);
    }

    // Modifie une connexion pour qu'une maison soit connectee a un nouveau
    // generateur
    public void modifierConnexion(String nomMaison, String nomNouveauGenerateur) {
        reseau.setConnexionUnique(nomMaison, nomNouveauGenerateur);
    }

    // Valide la configuration du reseau et retourne une liste de messages d'erreur
    // s'il y en a
    public List<String> validerConfigurationReseau() {
        return reseau.validerConfiguration();
    }

    // Calcule et retourne le cout du reseau sous forme de tableau de doubles
    public double[] calculerCoutReseau() {
        return ReseauAlgorithms.calculerCout(reseau);
    }

    // --- Getter pour que la Vue puisse acceder aux reseau ---
    public Reseau getReseau() {
        return reseau;
    }
}