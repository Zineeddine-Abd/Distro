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
import java.util.ArrayList;

import Exceptions.FileSyntaxException;
import Exceptions.InvalidFileExtensionException;
import Exceptions.InvalidNameException;
import Exceptions.ReseauInvalideException;
import Exceptions.ReseauInvalideSyntaxException;
import Model.Consommation;
import Model.Generateur;
import Model.Maison;
import Model.Reseau;

import java.util.HashMap;
import java.util.List;

// Cette classe regroupe toutes les fonctions liees a la gestion des fichiers (lecture et ecriture).
// Elle permet de charger un reseau depuis un fichier texte et de sauvegarder un reseau existant.
public class FileAlgorithms {

    // Avertissements (non bloquants) detectes lors du dernier chargement REUSSI et
    // VALIDE.
    // Ils ne doivent pas etre affiches si le chargement/validation echoue.
    private static List<String> lastLoadWarnings = List.of();

    /**
     * Retourne les avertissements du dernier chargement reussi, puis reinitialise
     * la liste.
     * Si le chargement a echoue, la liste est vide.
     */
    public static List<String> consumeLastLoadWarnings() {
        List<String> result = lastLoadWarnings;
        lastLoadWarnings = List.of();
        return result;
    }

    private static void setLastLoadWarnings(List<String> warnings) {
        lastLoadWarnings = List.copyOf(warnings);
    }

    public static String normalizeTxtPathForLoad(String path) throws InvalidFileExtensionException {
        return normalizeTxtPath(path, true);
    }

    public static String normalizeTxtPathForSave(String path) throws InvalidFileExtensionException {
        return normalizeTxtPath(path, true);
    }

    private static String normalizeTxtPath(String path, boolean appendIfMissing) throws InvalidFileExtensionException {
        if (path == null) {
            throw new InvalidFileExtensionException("Chemin de fichier invalide (null). Le fichier doit etre un .txt");
        }
        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidFileExtensionException("Chemin de fichier invalide (vide). Le fichier doit etre un .txt");
        }

        int lastSlash = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        String fileName = (lastSlash >= 0) ? trimmed.substring(lastSlash + 1) : trimmed;

        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            if (appendIfMissing) {
                return trimmed + ".txt";
            }
            throw new InvalidFileExtensionException(
                    "Extension manquante : veuillez fournir un fichier .txt (ex: reseau.txt)");
        }

        String ext = fileName.substring(dot + 1);
        if (!ext.equalsIgnoreCase("txt")) {
            throw new InvalidFileExtensionException(
                    "Extension invalide : '" + ext + "'. Le fichier doit avoir l'extension .txt");
        }

        return trimmed;
    }

    // Une expression reguliere (regex) pour verifier que chaque ligne respecte le
    // format : mot(mot,mot).
    // Exemple valide : maison(M1,10).
    private static String regex = "^([a-z]+)\\(([a-zA-Z0-9]+),([a-zA-Z0-9]+)\\)\\.$";

    // Le modele pre-compile de la regex pour l'utiliser plus rapidement sur chaque
    // ligne
    private static final Pattern LINE_PATTERN = Pattern.compile(regex);

    // Variables temporaires pour verifier si la demande totale depasse la capacite
    // totale pendant la lecture du fichier
    private static int sommeCapacitesGenerateurs = 0;
    private static int sommeBesoinsMaisons = 0;

    // Lit un fichier texte ligne par ligne pour construire le reseau.
    // Verifie la syntaxe, l'ordre des definitions et les contraintes logiques au
    // fur et a mesure.
    public static Reseau chargerReseau(String cheminFichier)
            throws FileNotFoundException, IOException, FileSyntaxException, ReseauInvalideException,
            ReseauInvalideSyntaxException, InvalidFileExtensionException {
        // Par defaut (si exception), pas d'avertissements a afficher.
        lastLoadWarnings = List.of();

        cheminFichier = normalizeTxtPathForLoad(cheminFichier);

        File fichier = new File(cheminFichier);
        if (!fichier.exists()) {
            throw new FileNotFoundException("Le fichier specifie est introuvable : " + cheminFichier);
        }
        Reseau reseau = new Reseau();
        sommeCapacitesGenerateurs = 0;
        sommeBesoinsMaisons = 0;

        Map<String, Generateur> generateurs = new HashMap<>();
        Map<String, Maison> maisons = new HashMap<>();

        // Permet de suivre ou on en est dans le fichier pour imposer l'ordre :
        // 1: D'abord les Generateurs, 2: Ensuite les Maisons, 3: Enfin les Connexions.
        int sectionEncours = 0;

        // Avertissements (non fatals) a montrer uniquement si le reseau charge est
        // valide.
        List<String> warnings = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            int numeroLigne = 0;
            List<FileSyntaxException> syntaxErrors = new ArrayList<>();

            while ((ligne = br.readLine()) != null) {
                numeroLigne++;
                ligne = ligne.trim();

                if (ligne.isEmpty())
                    continue; // On ignore les lignes vides

                Matcher matcher = LINE_PATTERN.matcher(ligne);

                if (!matcher.matches()) {
                    // Si la ligne ne ressemble pas a "type(arg1,arg2).", on arrete tout avec une
                    // erreur precise
                    // System.out.println(new FileSyntaxException(
                    // "Format incorrect ou parenthese/point manquant",
                    // numeroLigne,
                    // ligne).getMessage());
                    syntaxErrors.add(new FileSyntaxException(
                            "Format incorrect ou parenthese/point manquant",
                            numeroLigne,
                            ligne));
                    continue; // skip to the next line on file
                }

                // On recupere les 3 morceaux de la ligne : le type (ex: generateur) et les deux
                // arguments
                String type = matcher.group(1);
                String arg1 = matcher.group(2);
                String arg2 = matcher.group(3);

                switch (type) {
                    case "generateur":
                        // Verifie qu'on n'a pas deja commence a definir des maisons ou connexions
                        if (sectionEncours > 1)// a deja validé des choses qui se trouvent apres les generateurs
                            syntaxErrors.add(new FileSyntaxException(
                                    "Les generateurs doivent etre definis en premier.", numeroLigne, ligne));
                        try {
                            int capacite = Integer.parseInt(arg2);
                            if (capacite <= 0) {
                                syntaxErrors.add(new FileSyntaxException(
                                        "La capacite doit etre un entier strictement positif.",
                                        numeroLigne,
                                        ligne));
                                break; // all the breaks here directly skip to the next line on file
                            }

                            boolean genExisted = reseau.generateurExiste(arg1);
                            if (genExisted) {
                                int oldCapacite = reseau.getGenerateurs().get(arg1).getCapaciteMax();
                                warnings.add("Ligne " + numeroLigne + " : le generateur '" + arg1
                                        + "' est redeclare. Capacite mise a jour de " + oldCapacite + " a "
                                        + capacite + " kW.");
                                sommeCapacitesGenerateurs -= oldCapacite;
                            }

                            Generateur gen = new Generateur(arg1, capacite);
                            try {
                                reseau.addOrUpdateGenerateur(gen);
                            } catch (IllegalArgumentException e) {
                                syntaxErrors.add(new FileSyntaxException(e.getMessage(), numeroLigne, ligne));
                                // Si on avait soustrait l'ancienne capacite, on l'annule
                                if (genExisted) {
                                    sommeCapacitesGenerateurs += reseau.getGenerateurs().get(arg1).getCapaciteMax();
                                }
                                break;
                            }
                            generateurs.put(arg1, gen);
                            // On ajoute sa capacite au total disponible
                            sommeCapacitesGenerateurs += capacite;
                            sectionEncours = 1;

                        } catch (NumberFormatException e) {
                            syntaxErrors.add(
                                    new FileSyntaxException("La capacite doit etre un entier.", numeroLigne, ligne));
                            break;
                        }
                        break;
                    case "maison":
                        // Verifie qu'on est bien apres les generateurs et avant les connexions
                        if (sectionEncours > 2 || sectionEncours == 0) { // starts directly with maison or comes
                                                                         // directly from the cnx section
                            syntaxErrors.add(new FileSyntaxException(
                                    "Les maisons doivent etre definies apres les generateurs et avant les connexions.",
                                    numeroLigne,
                                    ligne));
                        }
                        // sectionEnCours ==1 (gen) or ==2 (maison) :
                        try {// conso invalid verif try block
                            Consommation consoMaison = Consommation.fromString(arg2);
                            Maison maison = new Maison(arg1, consoMaison);
                            boolean maisonExisted = reseau.maisonExiste(arg1);
                            if (maisonExisted) {
                                int ancienneConsoMaisonKw = reseau.getMaisons().get(arg1).getConsommationKw();
                                warnings.add("Ligne " + numeroLigne + " : la maison '" + arg1
                                        + "' est redeclaree. Consommation de la maison '" + arg1
                                        + "' mise a jour de " + ancienneConsoMaisonKw
                                        + " kW a "
                                        + consoMaison.getValeurKw() + " kW.");
                                sommeBesoinsMaisons -= ancienneConsoMaisonKw;
                            }
                            // On ajoute la consommation de cette maison au total demande
                            sommeBesoinsMaisons += consoMaison.getValeurKw();
                            // Verification immediate : est-ce que la demande depasse deja l'offre ?
                            if (sommeBesoinsMaisons > sommeCapacitesGenerateurs && generateurs.size() > 0) {
                                syntaxErrors.add(new FileSyntaxException(
                                        "ERREUR LOGIQUE LORS DU PARSING : CAPACITE GLOBALE DEPASSEE : L'ajout de la maison '" + arg1 + "' ("
                                                + consoMaison.getValeurKw() + "kW) " +
                                                "porte la demande totale a " + sommeBesoinsMaisons
                                                + "kW, ce qui depasse la capacite totale des generateurs (" +
                                                sommeCapacitesGenerateurs + "kW).",
                                        numeroLigne,
                                        ligne));

                                // Annule l'impact de la tentative pour eviter les effets de bord
                                sommeBesoinsMaisons -= consoMaison.getValeurKw();
                                if (maisonExisted) {
                                    sommeBesoinsMaisons += reseau.getMaisons().get(arg1).getConsommationKw();
                                }
                                break;
                            }
                            reseau.addOrUpdateMaison(maison);
                            maisons.put(arg1, maison);
                            sectionEncours = 2;

                        } catch (IllegalArgumentException e) {
                            syntaxErrors.add(new FileSyntaxException(
                                    "Type de consommation invalide/inattendu (Attendu: BASSE, NORMAL, FORTE)",
                                    numeroLigne, ligne));
                        }

                        break;
                    case "connexion":
                        // Verifie qu'on a fini de definir toutes les maisons et generateurs
                        if (sectionEncours == 1 || sectionEncours == 0) { // direct jump to connexion section from
                                                                          // nowhere or from gen section
                            syntaxErrors.add(new FileSyntaxException(
                                    "Les connexions doivent etre definies apres les generateurs et les maisons.",
                                    numeroLigne, ligne));
                        }

                        // if (reseau.getMaisons().isEmpty()) {
                        // syntaxErrors.add(new FileSyntaxException(
                        // "Aucune maison definie dans le fichier (section 'maison' manquante).",
                        // numeroLigne,
                        // ligne));
                        // break;
                        // }
                        // if (reseau.getGenerateurs().isEmpty()) {
                        // syntaxErrors.add(new FileSyntaxException(
                        // "Aucun generateur defini dans le fichier (section 'generateur' manquante).",
                        // numeroLigne,
                        // ligne));
                        // break;
                        // }

                        // Traite la connexion et verifie qu'elle est valide (pas de doublon, elements
                        // existants)
                        try {
                            parseEtValiderConnexion(reseau, arg1, arg2, numeroLigne, ligne, warnings);
                        } catch (FileSyntaxException e) {
                            syntaxErrors.add(e);
                        }
                        sectionEncours = 3;
                        break;
                    default:
                        // System.out.println("type: " + type + " ligne: " + ligne);
                        syntaxErrors.add(new FileSyntaxException(
                                "Premier mot de la ligne (le type) est inconnu, le nom de la ligne doit etre \"maison\" \"generateur\", ou \"connexion\", or on trouve: "
                                        + type,
                                numeroLigne, ligne));
                }
            }
            if (numeroLigne == 0) {
                syntaxErrors.add(new FileSyntaxException(
                        "FICHIER VIDE : le fichier d'entree ne contient rien.",
                        0,
                        ""));
            } else if (sectionEncours == 0) {
                syntaxErrors.add(new FileSyntaxException(
                        "FICHIER INVALIDE : le fichier d'entree ne contient aucune definition valide, ni generateur, ni maison, ni connexion.",
                        0,
                        ""));

            } else {
                if (maisons.isEmpty()) {
                    syntaxErrors.add(new FileSyntaxException(
                            "ERREUR LOGIQUE LORS DU PARSING : AUCUNE MAISON DEFINIE: le reseau doit contenir au moins une maison.",
                            numeroLigne,
                            ligne));
                }
                if (generateurs.isEmpty()) {
                    syntaxErrors.add(new FileSyntaxException(
                            "ERREUR LOGIQUE LORS DU PARSING : AUCUN GENERATEUR DEFINI: le reseau doit contenir au moins un generateur.",
                            numeroLigne,
                            ligne));
                }
                if (reseau.getConnexions().isEmpty()) {
                    if (sectionEncours == 3) {
                        // we have already added an error for that case
                        syntaxErrors.add(new FileSyntaxException(
                                "ERREUR LOGIQUE LORS DU PARSING : AUCUNE CONNEXION VALIDE N'A ETE DEFINIE dans le fichier (section 'connexion' entierement invalide).",
                                numeroLigne, ligne));
                    } else
                        syntaxErrors.add(new FileSyntaxException(
                                "ERREUR LOGIQUE LORS DU PARSING : AUCUNE CONNEXION DEFINIE dans le fichier (section 'connexion' manquante).",
                                numeroLigne, ligne));
                }
            }
            // Si on a rencontre des erreurs de syntaxe, on les remonte toutes ensemble
            if (!syntaxErrors.isEmpty()) {
                throw new ReseauInvalideSyntaxException(syntaxErrors);
            }
        } catch (IOException e) {
            throw new IOException("Erreur de lecture de ligne du fichier : " + fichier.toString(), e.getCause());
        }

        // Une fois le fichier lu en entier, on fait les verifications globales
        // (ex: est-ce que toutes les maisons sont bien connectees ?)
        validerCompletude(reseau);

        // Chargement + validation reussis : on expose les avertissements au caller.
        setLastLoadWarnings(warnings);

        return reseau;
    }

    // Analyse une ligne de connexion. Identifie qui est la maison et qui est le
    // generateur,
    // puis verifie qu'on n'essaie pas de connecter une maison qui a deja un
    // generateur.
    private static void parseEtValiderConnexion(Reseau reseau, String arg1, String arg2, int line, String content,
            List<String> warnings)
            throws FileSyntaxException {
        String nomMaison = null;
        String nomGen = null;

        // On essaie de deviner qui est qui, car le format autorise connexion(M,G) ou
        // connexion(G,M)
        if (reseau.maisonExiste(arg1) && reseau.generateurExiste(arg2)) {
            nomMaison = arg1;
            nomGen = arg2;
        } else if (reseau.maisonExiste(arg2) && reseau.generateurExiste(arg1)) {
            nomMaison = arg2;
            nomGen = arg1;
        } else {
            // Si l'un des noms n'existe pas, on arrete tout
            throw new FileSyntaxException(
                    "Connexion impossible : l'un des elements (" + arg1 + ", " + arg2
                            + ") n'a pas ete defini plus haut.",
                    line, content);
        }

        // Connexion deja declaree (meme paire, y compris si l'utilisateur inverse les
        // arguments)
        if (reseau.connexionExiste(nomMaison, nomGen)) {
            warnings.add("Ligne " + line + " : la connexion ('" + nomMaison + "', '" + nomGen
                    + "') est redeclaree. Elle est ignoree.");
            return;
        }

        // Verifie la regle d'unicite : une maison ne peut avoir qu'un seul generateur
        if (reseau.connexionExistePourMaison(nomMaison)) {
            List<String> gensConnectes = reseau.getConnexions().get(nomMaison);
            String dejaConnecteA = gensConnectes.get(0); // length==1
            throw new FileSyntaxException(
                    "ERREUR LOGIQUE : La maison '" + nomMaison + "' est deja connectee au generateur '"
                            + dejaConnecteA
                            + "'. "
                            + "Impossible de la connecter aussi a '" + nomGen + "'.",
                    line, content);
        }

        reseau.creerConnexion(nomMaison, nomGen);
    }

    // Effectue les dernieres validations sur le reseau complet.
    // Si des problemes subsistent (ex: maison sans electricite), on lance une
    // exception avec la liste des erreurs.
    private static void validerCompletude(Reseau reseau) throws ReseauInvalideException {

        List<String> problemes = reseau.validerConfiguration();
        if (!problemes.isEmpty()) {
            throw new ReseauInvalideException(problemes);
        }

    }

    // Ecrit l'etat actuel du reseau dans un fichier texte, en respectant le format
    // demande.
    public static void sauvegarderReseau(Reseau reseau, String cheminFichier)
            throws IOException, InvalidNameException, InvalidFileExtensionException {

        cheminFichier = normalizeTxtPathForSave(cheminFichier);

        verifierNomsSansEspaces(reseau);

        // 1- Verification et ajustement du chemin (Logique intelligente du 2ème
        // fichier)
        File fichierCible = new File(cheminFichier);

        // Si le fichier n'a pas de parent (c'est juste un nom de fichier sans dossier,
        // ex: "save.txt")
        if (fichierCible.getParent() == null) {
            System.out.println(
                    "Aucun dossier specifie dans le chemin, Sauvegarde automatique dans le dossier 'instances'");
            // On force le chemin vers le dossier "instances"
            File dossierInstances = new File("instances");

            // Sécurité : On crée le dossier s'il n'existe pas
            if (!dossierInstances.exists()) {
                dossierInstances.mkdir();
            }

            // On recompose le chemin : instances/nomFichier
            fichierCible = new File(dossierInstances, cheminFichier);
        }

        // 2- Ecriture (On utilise fichierCible au lieu de la String cheminFichier)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fichierCible))) {

            // A- On ecrit d'abord tous les generateurs
            for (Generateur gen : reseau.getGenerateurs().values()) {
                writer.write(String.format("generateur(%s,%d).", gen.getNom(), gen.getCapaciteMax()));
                writer.newLine();
            }

            // B- Ensuite toutes les maisons
            for (Maison maison : reseau.getMaisons().values()) {
                writer.write(String.format("maison(%s,%s).", maison.getNom(), maison.getConsommation().name()));
                writer.newLine();
            }

            // C- Enfin toutes les connexions
            for (Map.Entry<String, List<String>> entry : reseau.getConnexions().entrySet()) {
                String nomMaison = entry.getKey();
                for (String nomGen : entry.getValue()) {
                    writer.write(String.format("connexion(%s,%s).", nomGen, nomMaison));
                    writer.newLine();
                }
            }
        }

        // message de confirmation dans la console
        System.out.println("Reseau sauvegarde avec succes dans : " + fichierCible.getPath());
    }

    private static void verifierNomsSansEspaces(Reseau reseau) throws InvalidNameException {
        for (Generateur gen : reseau.getGenerateurs().values()) {
            verifierNomSansEspaces(gen.getNom(), "generateur");
        }
        for (Maison maison : reseau.getMaisons().values()) {
            verifierNomSansEspaces(maison.getNom(), "maison");
        }
        for (Map.Entry<String, List<String>> entry : reseau.getConnexions().entrySet()) {
            verifierNomSansEspaces(entry.getKey(), "maison");
            for (String nomGen : entry.getValue()) {
                verifierNomSansEspaces(nomGen, "generateur");
            }
        }
    }

    private static void verifierNomSansEspaces(String nom, String type) throws InvalidNameException {
        if (nom == null || nom.trim().isEmpty()) {
            throw new InvalidNameException("Nom invalide (" + type + ") : vide.");
        }
        if (nom.matches(".*\\s+.*")) {
            throw new InvalidNameException(
                    "Nom invalide (" + type + ") : '" + nom
                            + "' contient un espace. Veuillez utiliser un nom sans espaces.");
        }
    }
}