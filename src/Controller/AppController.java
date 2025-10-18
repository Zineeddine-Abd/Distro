package Controller;

import Model.*;
import java.util.List;

/**
 * Il contient toute la logique applicative et agit comme un
 * intermidiaire entre la Vue (Affichage) et le Modele (calcule et logique metier/stockage).
 */
public class AppController {
    private final Reseau reseau;

    public AppController(Reseau reseau) {
        this.reseau = reseau;
    }

    // --- Actions declenchees par la Vue ---
    public boolean addGenerateur(String nom, int capacite) {
        boolean existed = reseau.generateurExiste(nom);
        reseau.addOrUpdateGenerateur(new Generateur(nom, capacite));
        return existed;
    }

    public boolean addMaison(String nom, String type) throws IllegalArgumentException {
        Consommation conso = Consommation.fromString(type);
        boolean existed = reseau.maisonExiste(nom);
        reseau.addOrUpdateMaison(new Maison(nom, conso));
        return existed;
    }

    public void addConnexion(String nomMaison, String nomGenerateur) {
        reseau.creerConnexion(nomMaison, nomGenerateur);
    }

    public void modifierConnexion(String nomMaison, String nomNouveauGenerateur) {
        reseau.creerConnexion(nomMaison, nomNouveauGenerateur);
    }

    public List<String> validerConfigurationReseau() {
        return reseau.validerConfiguration();
    }

    public double[] calculerCoutReseau() {
        return reseau.calculerCout();
    }

    // --- Getter pour que la Vue puisse accéder aux donnees ---
    public Reseau getReseau() {
        return reseau;
    }
}
