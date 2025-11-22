package Controller;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.List;

import Model.Reseau;
import Model.FileSyntaxException;
import Model.Consommation;
import Model.Generateur;
import Model.Maison;

public class FileLoader {
  // private static String regex =
  // "^(?:generateur\\([A-Za-z][A-Za-z0-9]*,\\d+\\)\\.|" +
  // "maison\\([A-Za-z][A-Za-z0-9]*,(?:NORMAL|BASSE|FORTE)\\)\\.|" +
  // "connexion\\([A-Za-z][A-Za-z0-9]*,[A-Za-z][A-Za-z0-9]*\\)\\.)$";

  // Regex générique pour capturer la structure : type(arg1, arg2).
  private static String regex = "^([a-z]+)\\(([a-zA-Z0-9]+),([a-zA-Z0-9]+)\\)\\.$";

  private static final Pattern LINE_PATTERN = Pattern
      .compile(regex);

  public static Reseau chargerReseau(String cheminFichier)
      throws FileNotFoundException, IOException, FileSyntaxException {
    File fichier = new File(cheminFichier);
    if (!fichier.exists()) {
      throw new FileNotFoundException("Le fichier spécifié est introuvable : " + cheminFichier);
    }
    Reseau reseau = new Reseau();
    Map<String, Generateur> generateurs = new HashMap<>();
    Map<String, Maison> maisons = new HashMap<>();

    int sectionEncours = 0; // pour savoir dans quel stade de la lecture est-on actuellement :
                            // 0:Generateurs, 1:Maisons, 2: Connexions.

    try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
      String ligne;
      int numeroLigne = 0;

      while ((ligne = br.readLine()) != null) {
        numeroLigne++;
        ligne = ligne.trim();

        if (ligne.isEmpty())
          continue; // saut-de-ligne

        Matcher matcher = LINE_PATTERN.matcher(ligne);

        if (!matcher.matches()) {
          // We throw the custom exception with the specific line number
          throw new FileSyntaxException(
              "Format incorrect ou parenthèse/point manquant",
              numeroLigne,
              ligne);
        }

        String type = matcher.group(1);
        String arg1 = matcher.group(2);
        String arg2 = matcher.group(3);

        switch (type) {
          case "generateur":
            if (sectionEncours > 1)
              throw new FileSyntaxException(
                  "Les générateurs doivent être définis en premier.", numeroLigne, ligne);
            sectionEncours = 1;

            int capacity;
            try {
              capacity = Integer.parseInt(arg2);
            } catch (NumberFormatException e) {
              throw new FileSyntaxException("La capacité doit être un entier.", numeroLigne, ligne);
            }

            Generateur gen = new Generateur(arg1, capacity);
            reseau.addOrUpdateGenerateur(gen);
            generateurs.put(arg1, gen);
            break;
          case "maison":
            if (sectionEncours > 2)
              throw new FileSyntaxException(
                  "Les maisons doivent être définies après les générateurs et avant les connexions.", numeroLigne,
                  ligne);
            sectionEncours = 2;

            // Création de la maison (arg1 = nom, arg2 = type de consommation)
            try {
              // valide que arg2 est NORMAL, BASSE, ou FORTE
              Consommation conso = Consommation.valueOf(arg2);
              Maison maison = new Maison(arg1, conso);
              reseau.addOrUpdateMaison(maison);
              maisons.put(arg1, maison);
            } catch (IllegalArgumentException e) {
              throw new FileSyntaxException("Type de consommation inconnu (Attendu: BASSE, NORMAL, FORTE)", numeroLigne,
                  ligne);
            }
            break;
          case "connexion":
            if (sectionEncours < 2)
              throw new FileSyntaxException(
                  "Les connexions doivent être définies après les générateurs et les maisons.", numeroLigne, ligne);
            sectionEncours = 3;

            // C'est ici que se fait la validation "au vol" avec le numéro de ligne
            parseEtValiderConnexion(reseau, arg1, arg2, numeroLigne, ligne);

          default:
            throw new FileSyntaxException(
                "Premier mot de la ligne (le type) est inconnu (unexpected), expects \"maison\" \"generateur\", or \"connexion\" ",
                numeroLigne, ligne);
        }
      }
    } catch (IOException e) {
      throw new IOException("Erreur de lecture de ligne du fichier : " + fichier.toString(), e.getCause());
    }

    // --- Validation Finale (Post-Lecture) ---
    // Certaines erreurs ne peuvent pas être détectées ligne par ligne (ex: Maison
    // sans aucune connexion).
    // On les vérifie ici. Si une erreur est trouvée, on ne peut pas donner de ligne
    // précise (d'où line=0).
    validerCompletude(reseau);

    return reseau;
  }

  /**
   * Valide la connexion spécifiquement pour la règle "Une maison = un générateur
   * unique".
   */
  private static void parseEtValiderConnexion(Reseau reseau, String arg1, String arg2, int line, String content)
      throws FileSyntaxException {
    String nomMaison = null;
    String nomGen = null;

    // 1. Identification : Qui est la maison ? Qui est le générateur ?
    // (La partie 1 permet l'ordre "M G" ou "G M") [cite: 227]
    if (reseau.maisonExiste(arg1) && reseau.generateurExiste(arg2)) {
      nomMaison = arg1;
      nomGen = arg2;
    } else if (reseau.maisonExiste(arg2) && reseau.generateurExiste(arg1)) {
      nomMaison = arg2;
      nomGen = arg1;
    } else {
      // ERREUR : Élément introuvable
      // "Il faut bien entendu que le générateur et la maison aient été créés au
      // préalable"
      throw new FileSyntaxException(
          "Connexion impossible : l'un des éléments (" + arg1 + ", " + arg2 + ") n'a pas été défini plus haut.",
          line, content);
    }

    // 2. VALIDATION SÉMANTIQUE (Règle d'unicité)
    // [cite: 204] "Une maison doit obligatoirement être raccordée à un unique
    // générateur."
    if (reseau.connexionExistePourMaison(nomMaison)) {
      // Note: Il faut ajouter une petite méthode utilitaire dans Reseau ou vérifier
      // la map directement
      // Si la map des connexions contient déjà une clé pour cette maison, c'est une
      // erreur.
      List<String> gensConnectes = reseau.getConnexions().get(nomMaison);
      String dejaConnecteA = gensConnectes.get(0);

      throw new FileSyntaxException(
          "ERREUR LOGIQUE : La maison '" + nomMaison + "' est déjà connectée au générateur '" + dejaConnecteA + "'. "
              + "Impossible de la connecter aussi à '" + nomGen + "'.",
          line, content);
    }

    // Si tout est valide, on crée la connexion
    reseau.creerConnexion(nomMaison, nomGen);
  }

  /**
   * Vérifications globales qui ne peuvent se faire qu'à la fin du fichier.
   */
  private static void validerCompletude(Reseau reseau) throws Exception {
    // Vérifier qu'il y a au moins 1 maison et 1 générateur [cite: 202]
    if (reseau.getMaisons().isEmpty() || reseau.getGenerateurs().isEmpty()) {
      throw new Exception("Le réseau doit contenir au moins une maison et un générateur.");
    }

    // Vérifier les maisons orphelines (définies mais sans connexion)
    for (String nomMaison : reseau.getMaisons().keySet()) {
      if (!reseau.getConnexions().containsKey(nomMaison) || reseau.getConnexions().get(nomMaison).isEmpty()) {
        throw new Exception(
            "Configuration incomplète : La maison '" + nomMaison + "' n'est connectée à aucun générateur.");
      }
    }

    List<String> erreursGlobales = reseau.validerConfiguration();
    if (!erreursGlobales.isEmpty()) {
      throw new Exception("Erreur globale de capacité : " + erreursGlobales.get(0));
    }
  }

}
