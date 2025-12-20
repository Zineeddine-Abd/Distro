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
import Exceptions.InvalidNameException;
import Exceptions.ReseauInvalideException;
import Model.Consommation;
import Model.Generateur;
import Model.Maison;
import Model.Reseau;

import java.util.HashMap;
import java.util.List;

// Cette classe regroupe toutes les fonctions liees a la gestion des fichiers (lecture et ecriture).
// Elle permet de charger un reseau depuis un fichier texte et de sauvegarder un reseau existant.
public class FileAlgorithms {

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

        // Permet de suivre ou on en est dans le fichier pour imposer l'ordre :
        // 1: D'abord les Generateurs, 2: Ensuite les Maisons, 3: Enfin les Connexions.
        int sectionEncours = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            int numeroLigne = 0;

            while ((ligne = br.readLine()) != null) {
                numeroLigne++;
                ligne = ligne.trim();

                if (ligne.isEmpty())
                    continue; // On ignore les lignes vides

                Matcher matcher = LINE_PATTERN.matcher(ligne);

                if (!matcher.matches()) {
                    // Si la ligne ne ressemble pas a "type(arg1,arg2).", on arrete tout avec une
                    // erreur precise
                    throw new FileSyntaxException(
                            "Format incorrect ou parenthese/point manquant",
                            numeroLigne,
                            ligne);
                }

                // On recupere les 3 morceaux de la ligne : le type (ex: generateur) et les deux
                // arguments
                String type = matcher.group(1);
                String arg1 = matcher.group(2);
                String arg2 = matcher.group(3);

                switch (type) {
                    case "generateur":
                        // Verifie qu'on n'a pas deja commence a definir des maisons ou connexions
                        if (sectionEncours > 1)
                            throw new FileSyntaxException(
                                    "Les generateurs doivent etre definis en premier.", numeroLigne, ligne);
                        try {
                            int capacite = Integer.parseInt(arg2);
                            if (capacite <= 0) {
                                throw new FileSyntaxException(
                                        "La capacite doit etre un entier strictement positif.",
                                        numeroLigne,
                                        ligne);
                            }

                            Generateur gen = new Generateur(arg1, capacite);
                            try {
                                reseau.addOrUpdateGenerateur(gen);
                            } catch (IllegalArgumentException e) {
                                throw new FileSyntaxException(e.getMessage(), numeroLigne, ligne);
                            }
                            generateurs.put(arg1, gen);

                            // On ajoute sa capacite au total disponible
                            sommeCapacitesGenerateurs += capacite;
                            sectionEncours = 1;

                        } catch (NumberFormatException e) {
                            throw new FileSyntaxException("La capacite doit etre un entier.", numeroLigne, ligne);
                        }
                        break;
                    case "maison":
                        // Verifie qu'on est bien apres les generateurs et avant les connexions
                        if (sectionEncours > 2)
                            throw new FileSyntaxException(
                                    "Les maisons doivent etre definies apres les generateurs et avant les connexions.",
                                    numeroLigne,
                                    ligne);

                        try {
                            Consommation conso = Consommation.fromString(arg2);
                            Maison maison = new Maison(arg1, conso);
                            // On ajoute la consommation de cette maison au total demande
                            sommeBesoinsMaisons += conso.getValeurKw();
                            if (generateurs.isEmpty()) {
                                throw new FileSyntaxException(
                                        "AUCUN GENERATEUR DEFINI: le reseau doit contenir au moins un generateur.",
                                        numeroLigne,
                                        ligne);
                            }
                            // Verification immediate : est-ce que la demande depasse deja l'offre ?
                            if (sommeBesoinsMaisons > sommeCapacitesGenerateurs) {
                                throw new FileSyntaxException(
                                        "CAPACITE GLOBALE DEPASSEE : L'ajout de la maison '" + arg1 + "' ("
                                                + conso.getValeurKw() + "kW) " +
                                                "porte la demande totale a " + sommeBesoinsMaisons
                                                + "kW, ce qui depasse la capacite totale des generateurs (" +
                                                sommeCapacitesGenerateurs + "kW).",
                                        numeroLigne,
                                        ligne);
                            }
                            reseau.addOrUpdateMaison(maison);
                            maisons.put(arg1, maison);
                            sectionEncours = 2;

                        } catch (IllegalArgumentException e) {
                            throw new FileSyntaxException(
                                    "Type de consommation invalide/inattendu (Attendu: BASSE, NORMAL, FORTE)",
                                    numeroLigne, ligne);
                        }

                        break;
                    case "connexion":
                        // Verifie qu'on a fini de definir toutes les maisons et generateurs
                        if (sectionEncours < 2)
                            throw new FileSyntaxException(
                                    "Les connexions doivent etre definies apres les generateurs et les maisons.",
                                    numeroLigne, ligne);

                        if (reseau.getMaisons().isEmpty()) {
                            throw new FileSyntaxException(
                                    "Aucune maison definie dans le fichier (section 'maison' manquante).",
                                    numeroLigne,
                                    ligne);
                        }

                        // Traite la connexion et verifie qu'elle est valide (pas de doublon, elements
                        // existants)
                        parseEtValiderConnexion(reseau, arg1, arg2, numeroLigne, ligne);
                        sectionEncours = 3;
                        break;
                    default:
                        System.out.println("type: " + type);
                        throw new FileSyntaxException(
                                "Premier mot de la ligne (le type) est inconnu, le nom de la ligne doit etre \"maison\" \"generateur\", ou \"connexion\", or on trouve: "
                                        + type,
                                numeroLigne, ligne);
                }
            }
            if (reseau.getConnexions().isEmpty()) {
                throw new FileSyntaxException(
                        "Aucune connexion definie dans le fichier (section 'connexion' manquante).",
                        numeroLigne, ligne);
            }
        } catch (IOException e) {
            throw new IOException("Erreur de lecture de ligne du fichier : " + fichier.toString(), e.getCause());
        }

        // Une fois le fichier lu en entier, on fait les verifications globales
        // (ex: est-ce que toutes les maisons sont bien connectees ?)
        validerCompletude(reseau);

        return reseau;
    }

    // Analyse une ligne de connexion. Identifie qui est la maison et qui est le
    // generateur,
    // puis verifie qu'on n'essaie pas de connecter une maison qui a deja un
    // generateur.
    private static void parseEtValiderConnexion(Reseau reseau, String arg1, String arg2, int line, String content)
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

        // Verifie la regle d'unicite : une maison ne peut avoir qu'un seul generateur
        // check also if it's not the same generateur again, if it is the same
        // generateur add a warning...
        if (reseau.connexionExistePourMaison(nomMaison)) {
            List<String> gensConnectes = reseau.getConnexions().get(nomMaison);
            String dejaConnecteA = gensConnectes.get(0); // length==1
            // repeating connexion found this exception is also raised...
            throw new FileSyntaxException(
                    "ERREUR LOGIQUE : La maison '" + nomMaison + "' est deja connectee au generateur '" + dejaConnecteA
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
    public static void sauvegarderReseau(Reseau reseau, String cheminFichier) throws IOException, InvalidNameException {

        verifierNomsSansEspaces(reseau);

        // 1- Verification et ajustement du chemin (Logique intelligente du 2ème
        // fichier)
        File fichierCible = new File(cheminFichier);

        // Si le fichier n'a pas de parent (c'est juste un nom de fichier sans dossier,
        // ex: "save.txt")
        if (fichierCible.getParent() == null) {
            System.out.println("Aucun dossier specifie dans le chemin, Sauvegarde automatique dans le dossier 'instances'");
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