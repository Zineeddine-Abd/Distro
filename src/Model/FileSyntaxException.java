package Model;

public class FileSyntaxException extends Exception {
  private final int numeroLigne;
  private final String contenuLigne;

  public FileSyntaxException(String message, int numeroLigne, String contenuLigne) {
    super("Erreur à la ligne " + numeroLigne + ": " + message + " (Contenu: \"" + contenuLigne + "\")");
    this.numeroLigne = numeroLigne;
    this.contenuLigne = contenuLigne;
  }

  public int getNumeroLigne() {
    return numeroLigne;
  }

  public String getContenuLigne() {
    return contenuLigne;
  }
}