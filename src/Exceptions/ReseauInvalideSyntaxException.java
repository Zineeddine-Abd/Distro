package Exceptions;

import java.util.List;

public class ReseauInvalideSyntaxException extends ReseauInvalideException {

  public ReseauInvalideSyntaxException(List<FileSyntaxException> syntaxErrors) {
    super("Le reseau contient " + syntaxErrors.size() + " erreur(s) de syntaxe dans le fichier d'entree.");
    // this.erreurs =
    // syntaxErrors.stream().map(FileSyntaxException::getMessage).toList();
    // System.out.println(syntaxErrors);
    // System.out.println(syntaxErrors.stream().map(FileSyntaxException::getMessage).toList().size());
    erreurs = syntaxErrors.stream().map(FileSyntaxException::getMessage).toList();
  }

}
