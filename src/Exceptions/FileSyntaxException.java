package Exceptions;

/**
 * Exception levee lorsqu'une erreur de syntaxe est detectee dans un fichier
 * d'entree.
 * Elle permet de savoir exactement quelle ligne du fichier texte pose probleme.
 */
public class FileSyntaxException extends Exception {

    // Le numero de la ligne dans le fichier qui contient l'erreur
    private final int numeroLigne;

    // Le texte exact de la ligne qui a provoque l'erreur (pour l'afficher a
    // l'utilisateur)
    private final String contenuLigne;

    // Cree une nouvelle exception en assemblant un message clair avec le numero de
    // ligne et son contenu
    public FileSyntaxException(String message, int numeroLigne, String contenuLigne) {
        super((contenuLigne != null && !contenuLigne.isEmpty() ? "Erreur à la ligne " + numeroLigne + ": "
                : "") + message
                + (contenuLigne != null && !contenuLigne.isEmpty()
                        ? " (Contenu de la ligne: \"" + contenuLigne + "\")"
                        : ""));
        this.numeroLigne = numeroLigne;
        this.contenuLigne = contenuLigne;
    }

    // Permet de recuperer le numero de la ligne fautive
    public int getNumeroLigne() {
        return numeroLigne;
    }

    // Permet de recuperer le texte brut de la ligne fautive
    public String getContenuLigne() {
        return contenuLigne;
    }
}