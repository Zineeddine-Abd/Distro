package Algorithms;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Exceptions.FileSyntaxException;
import Exceptions.ReseauInvalideException;

import java.util.HashMap;
import java.util.List;

import Model.Reseau;
import Model.Consommation;
import Model.Generateur;
import Model.Maison;

public class FileAlgorithms {

    // Regex generique pour capturer la structure : type(arg1, arg2).
    private static String regex = "^([a-z]+)\\(([a-zA-Z0-9]+),([a-zA-Z0-9]+)\\)\\.$";

    private static final Pattern LINE_PATTERN = Pattern
            .compile(regex);

    private static int sommeCapacitesGenerateurs = 0;
    private static int sommeBesoinsMaisons = 0;

    public static Reseau chargerReseau(String cheminFichier)
            throws FileNotFoundException, IOException, FileSyntaxException, ReseauInvalideException {
        File fichier = new File(cheminFichier);
        if (!fichier.exists()) {
            throw new FileNotFoundException("Le fichier specifie est introuvable : " + cheminFichier);
        }
        Reseau reseau = new Reseau();
        sommeCapacitesGenerateurs = 0;
        sommeBesoinsMaisons = 0;

        Map<String, Generateur> generateurs = new HashMap<>();
        Map<String, Maison> maisons = new HashMap<>();

        int sectionEncours = 0; // pour savoir dans quel stade de la lecture est-on actuellement :
        // 1:Generateurs, 2:Maisons, 3: Connexions.

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
                            "Format incorrect ou parenthese/point manquant",
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
                                    "Les generateurs doivent etre definis en premier.", numeroLigne, ligne);
                        sectionEncours = 1;
                        try {
                            int capacite = Integer.parseInt(arg2);
                            if (capacite < 0)
                                throw new NumberFormatException();

                            Generateur gen = new Generateur(arg1, capacite);
                            reseau.addOrUpdateGenerateur(gen);
                            generateurs.put(arg1, gen);

                            // MISE A JOUR DE LA CAPACITE TOTALE
                            sommeCapacitesGenerateurs += capacite;

                        } catch (NumberFormatException e) {
                            throw new FileSyntaxException("La capacite doit etre un entier.", numeroLigne, ligne);
                        }
                        break;
                    case "maison":
                        if (sectionEncours > 2)
                            throw new FileSyntaxException(
                                    "Les maisons doivent etre definies apres les generateurs et avant les connexions.", numeroLigne,
                                    ligne);
                        sectionEncours = 2;

                        try {
                            Consommation conso = Consommation.fromString(arg2);
                            Maison maison = new Maison(arg1, conso);
                            reseau.addOrUpdateMaison(maison);
                            maisons.put(arg1, maison);

                            // 1. On ajoute la consommation de cette nouvelle maison au cumul
                            sommeBesoinsMaisons += conso.getValeurKw();

                            // 2. VERIFICATION IMMEDIATE : A-t-on depasse le plafond ?
                            // La contrainte globale est : Somme(Demandes) <= Somme(Capacites)
                            if (sommeBesoinsMaisons > sommeCapacitesGenerateurs) {
                                throw new FileSyntaxException(
                                        "CAPACITE GLOBALE DEPASSEE : L'ajout de la maison '" + arg1 + "' (" + conso.getValeurKw() + "kW) " +
                                                "porte la demande totale a " + sommeBesoinsMaisons
                                                + "kW, ce qui depasse la capacite totale des generateurs (" +
                                                sommeCapacitesGenerateurs + "kW).",
                                        numeroLigne,
                                        ligne);
                            }

                        } catch (IllegalArgumentException e) {
                            throw new FileSyntaxException("Type de consommation invalide/inattendu (Attendu: BASSE, NORMAL, FORTE)",
                                    numeroLigne, ligne);
                        }

                        break;
                    case "connexion":
                        if (sectionEncours < 2)
                            throw new FileSyntaxException(
                                    "Les connexions doivent etre definies apres les generateurs et les maisons.", numeroLigne, ligne);
                        sectionEncours = 3;

                        // C'est ici que se fait la validation "au vol" avec le numero de ligne
                        parseEtValiderConnexion(reseau, arg1, arg2, numeroLigne, ligne);
                        break;
                    default:
                        System.out.println("type: " + type);
                        throw new FileSyntaxException(
                                "Premier mot de la ligne (le type) est inconnu (unexpected), expected \"maison\" \"generateur\", or \"connexion\", found: "
                                        + type,
                                numeroLigne, ligne);
                }
            }
        } catch (IOException e) {
            throw new IOException("Erreur de lecture de ligne du fichier : " + fichier.toString(), e.getCause());
        }

        // --- Validation Finale (Post-Lecture) ---
        // Certaines erreurs ne peuvent pas etre detectees ligne par ligne (ex: Maison
        // sans aucune connexion).
        // On les verifie ici. Si une erreur est trouvee, on ne peut pas donner de ligne
        // precise (d'ou line=0).
        validerCompletude(reseau);

        return reseau;
    }

    /**
     * Valide la connexion specifiquement pour la regle "Une maison = un generateur
     * unique".
     */
    private static void parseEtValiderConnexion(Reseau reseau, String arg1, String arg2, int line, String content)
            throws FileSyntaxException {
        String nomMaison = null;
        String nomGen = null;

        // 1. Identification : Qui est la maison ? Qui est le generateur ?
        // (La partie 1 permet l'ordre "M G" ou "G M") [cite: 227]
        if (reseau.maisonExiste(arg1) && reseau.generateurExiste(arg2)) {
            nomMaison = arg1;
            nomGen = arg2;
        } else if (reseau.maisonExiste(arg2) && reseau.generateurExiste(arg1)) {
            nomMaison = arg2;
            nomGen = arg1;
        } else {
            // ERREUR : Element introuvable
            // "Il faut bien entendu que le generateur et la maison aient ete crees au
            // prealable"
            throw new FileSyntaxException(
                    "Connexion impossible : l'un des elements (" + arg1 + ", " + arg2 + ") n'a pas ete defini plus haut.",
                    line, content);
        }

        // 2. VALIDATION SEMANTIQUE (Regle d'unicite)
        // [cite: 204] "Une maison doit obligatoirement etre raccordee a un unique
        // generateur."
        if (reseau.connexionExistePourMaison(nomMaison)) {
            // Note: Il faut ajouter une petite methode utilitaire dans Reseau ou verifier
            // la map directement
            // Si la map des connexions contient deja une cle pour cette maison, c'est une
            // erreur.
            List<String> gensConnectes = reseau.getConnexions().get(nomMaison);
            String dejaConnecteA = gensConnectes.get(0);

            throw new FileSyntaxException(
                    "ERREUR LOGIQUE : La maison '" + nomMaison + "' est deja connectee au generateur '" + dejaConnecteA + "'. "
                            + "Impossible de la connecter aussi a '" + nomGen + "'.",
                    line, content);
        }

        // Si tout est valide, on cree la connexion
        reseau.creerConnexion(nomMaison, nomGen);
    }

    /**
     * Verifications globales qui ne peuvent se faire qu'a la fin du fichier.
     */
    private static void validerCompletude(Reseau reseau) throws ReseauInvalideException {

        List<String> problemes = reseau.validerConfiguration();
        if (!problemes.isEmpty()) {
            // On lance (cree) l'exception avec TOUTE la liste des problemes
            throw new ReseauInvalideException(problemes);
        }

    }

    /**
     * Saves the current network configuration to a file. [cite: 84, 85]
     */
    public static void sauvegarderReseau(Reseau reseau, String cheminFichier) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cheminFichier))) {

            // 1. Write Generators
            for (Generateur gen : reseau.getGenerateurs().values()) {
                writer.write(String.format("generateur(%s,%d).", gen.getNom(), gen.getCapaciteMax()));
                writer.newLine();
            }

            // 2. Write Maisons
            for (Maison maison : reseau.getMaisons().values()) {
                writer.write(String.format("maison(%s,%s).", maison.getNom(), maison.getConsommation().name()));
                writer.newLine();
            }

            // 3. Write Connexions
            // We iterate over the map to reconstruct connexions
            for (Map.Entry<String, List<String>> entry : reseau.getConnexions().entrySet()) {
                String nomMaison = entry.getKey();
                for (String nomGen : entry.getValue()) {
                    writer.write(String.format("connexion(%s,%s).", nomGen, nomMaison));
                    writer.newLine();
                }
            }
        }
    }
}
