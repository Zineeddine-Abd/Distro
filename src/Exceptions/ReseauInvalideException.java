package Exceptions;

import java.util.List;

/**
 * Exception levee lorsqu'une erreur de configuration logique est detectee dans le reseau.
 * Par exemple : une maison non connectee ou connectee a plusieurs generateurs.
 */
public class ReseauInvalideException extends Exception {

    // Stocke la liste de tous les problemes trouves (car il peut y en avoir plusieurs a la fois)
    private final List<String> erreurs;

    // Constructeur simple pour quand il n'y a qu'une seule erreur precise
    public ReseauInvalideException(String message) {
        super(message);
        this.erreurs = List.of(message);
    }

    // Constructeur principal utilise quand la validation renvoie une liste de problemes
    public ReseauInvalideException(List<String> erreurs) {
        super("Le reseau contient " + erreurs.size() + " erreur(s) de configuration logique.");
        this.erreurs = erreurs;
    }

    // Permet de recuperer la liste brute des erreurs
    public List<String> getErreurs() {
        return erreurs;
    }

    // Modifie le message par defaut de l'exception pour afficher la liste des erreurs proprement, ligne par ligne
    @Override
    public String getMessage() {
        // On concatene les erreurs pour un affichage propre
        if (erreurs.size() == 1) {
            return erreurs.get(0);
        }
        return super.getMessage() + " : \n" + //
                "- " + String.join("\n- ", erreurs);
    }
}