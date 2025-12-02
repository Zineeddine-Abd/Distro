package Exceptions;

import java.util.List;

public class ReseauInvalideException extends Exception {

  private final List<String> erreurs;

  // Constructeur pour une seule erreur
  public ReseauInvalideException(String message) {
    super(message);
    this.erreurs = List.of(message);
  }

  // Constructeur pour une liste d'erreurs (venant de validerConfiguration)
  public ReseauInvalideException(List<String> erreurs) {
    super("Le réseau contient " + erreurs.size() + " erreur(s) de configuration logique.");
    this.erreurs = erreurs;
  }

  public List<String> getErreurs() {
    return erreurs;
  }

  @Override
  public String getMessage() {
    // On concatène les erreurs pour un affichage par défaut propre
    if (erreurs.size() == 1) {
      return erreurs.get(0);
    }
    return super.getMessage() + " : \n" + //
        "- " + String.join("\n- ", erreurs);
  }
}