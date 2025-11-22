package Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represente le reseau electrique : maisons, generateurs et connexions.
 * Gere toute la logique de calcul de cout.
 */

public class Reseau {
    // Justification d'utilisation de Map plutot que List dans la representation des
    // maisons et generateurs :
    // mieux pour recherche et aussi la mise a jour rapide
    // aussi pour garder l'unicite des noms
    private final Map<String, Maison> maisons = new HashMap<>();
    private final Map<String, Generateur> generateurs = new HashMap<>();
    // La map des connexions : chaque maison peut etre connectee a plusieurs
    // generateurs dans le premier menu
    // C'est pour ca qu'on utilise une List pour les generateurs
    // Aussi l'utilisation de String pour les noms permet de simplifier la recherche
    // Aussi pour eviter les problemes avec les objets
    private final Map<String, List<String>> connexions = new HashMap<>(); // {Key : nomMaison, Value :
                                                                          // List<nomGenerateur>}

    // --- Methodes pour la gestion du reseau ---
    // Verifie si une maison existe deja dans le reseau
    public boolean maisonExiste(String nom) {
        return maisons.containsKey(nom);
    }

    // Verifie si un generateur existe deja dans le reseau
    public boolean generateurExiste(String nom) {
        return generateurs.containsKey(nom);
    }

    // Ajoute ou met a jour une maison dans le reseau
    public void addOrUpdateMaison(Maison maison) {
        maisons.put(maison.getNom(), maison);
    }

    // Ajoute ou met a jour un generateur dans le reseau
    public void addOrUpdateGenerateur(Generateur generateur) {
        generateurs.put(generateur.getNom(), generateur);
    }

    // Ajoute une connexion. Si la maison a déjà des connexions, celle-ci est
    // ajoutée à la liste.
    public void creerConnexion(String nomMaison, String nomGenerateur) {
        // computeIfAbsent: Récupere la liste pour la maison, ou en cree une nouvelle si
        // elle n'existe pas.
        // .add() : Ajoute le générateur à cette liste.
        connexions.computeIfAbsent(nomMaison, k -> new ArrayList<>()).add(nomGenerateur);
    }

    // Supprime une connexion spécifique.
    public void supprimerConnexion(String nomMaison, String nomGenerateur) {
        List<String> gens = connexions.get(nomMaison);
        if (gens != null) {
            gens.remove(nomGenerateur); // Supprime uniquement ce generateur de la liste
            if (gens.isEmpty()) {
                connexions.remove(nomMaison); // Nettoie la map si la liste est vide
            }
        }
    }

    // Remplace toutes les connexions d'une maison par une nouvelle connexion
    // unique, Utilise pour la modification
    public void setConnexionUnique(String nomMaison, String nomGenerateur) {
        List<String> nouvelleListe = new ArrayList<>();
        nouvelleListe.add(nomGenerateur);
        connexions.put(nomMaison, nouvelleListe); // pour craser la liste précédente
    }

    // Verifie si une connexion specifique existe.
    public boolean connexionExiste(String nomMaison, String nomGenerateur) {
        List<String> gens = connexions.get(nomMaison);
        return gens != null && gens.contains(nomGenerateur);
    }

    /**
     * Vérifie simplement si la maison a déjà au moins une connexion enregistrée
     */
    public boolean connexionExistePourMaison(String nomMaison) {
        return connexions.containsKey(nomMaison) && !connexions.get(nomMaison).isEmpty();
    }

    // Valide la configuration du reseau et retourne une liste de problemes trouves.
    public List<String> validerConfiguration() {

        List<String> problemes = new ArrayList<>();

        // Vérifications de base
        if (maisons.isEmpty()) {
            problemes.add("Aucune maison definie, veuillez definir au moins une maison.");
        }
        if (generateurs.isEmpty()) {
            problemes.add("Aucun generateur definie, veuillez definir au moins un generateur.");
        }
        if (!problemes.isEmpty()) {
            return problemes;
        }

        // Vérification de la capacité
        int capaciteGenerateurs = 0;
        for (Generateur generateur : generateurs.values()) {
            capaciteGenerateurs += generateur.getCapaciteMax();
        }
        int chargesMaisons = 0;
        for (Maison maison : maisons.values()) {
            chargesMaisons += maison.getConsommationKw();
        }
        if (capaciteGenerateurs < chargesMaisons) {
            problemes.add("La capacite totale des generateurs (" + capaciteGenerateurs
                    + "kW) est insuffisante pour alimenter toutes les maisons (" + chargesMaisons + "kW).");
        }

        // Verification des connextions
        for (String nomMaison : maisons.keySet()) {
            List<String> gens = connexions.get(nomMaison);

            // Cas 1 : "pas de connexion"
            if (gens == null || gens.isEmpty()) {
                problemes.add(nomMaison + " (pas de connexion)");
            }
            // Cas 2 : "trop de connexions (maison connectee a plusieurs generateurs)"
            else if (gens.size() > 1) {
                String genList = String.join(", ", gens); // Construit "G1, G2"
                problemes.add(nomMaison + " (trop de connexions: " + genList + ")");
            }
            // Cas 3 (gens.size() == 1) cas valide, on ne fait rien
        }

        return problemes;
    }

    // --- Getters ---
    public Map<String, Maison> getMaisons() {
        return maisons;
    }

    public Map<String, Generateur> getGenerateurs() {
        return generateurs;
    }

    public Map<String, List<String>> getConnexions() {
        return connexions;
    }

    // --- Methodes de calcul du cout ---

    /**
     * Le calcul de coût ne fonctionne QUE sur un réseau valide.
     * On suppose qu'il n'est appelé toujours que lorsque la configuration est
     * valide.
     * Cette méthode ne comptera la charge que pour les maisons ayant UNE SEULE
     * connexion.
     */

    // Calcule le cout total du reseau en fonction de la dispersion et de la
    // surcharge.
    // Retourne un tableau de double : [coutTotal, dispersion, surcharge]
    public double[] calculerCout() {
        if (generateurs.isEmpty())
            return new double[] { 0, 0, 0 };

        Map<String, Integer> charges = calculerCharges();
        Map<String, Double> tauxUtilisation = calculerTauxUtilisation(charges);
        double moyenneTaux = tauxUtilisation.values().stream().mapToDouble(d -> d).average().orElse(0.0);

        double dispersion = calculerDispersion(tauxUtilisation, moyenneTaux);
        double surcharge = calculerSurcharge(charges);

        final int LAMBDA = 10;
        double coutTotal = dispersion + LAMBDA * surcharge;

        return new double[] { coutTotal, dispersion, surcharge };
    }

    // Calcule la charge totale pour chaque générateur (la somme des capacite des
    // masions connectés).
    private Map<String, Integer> calculerCharges() {
        Map<String, Integer> charges = new HashMap<>();
        generateurs.keySet().forEach(nom -> charges.put(nom, 0)); // Initialise toutes les charges a 0

        for (Map.Entry<String, List<String>> entry : connexions.entrySet()) {
            String nomMaison = entry.getKey();
            List<String> gens = entry.getValue();

            // NE COMPTER LA CHARGE QUE SI LA CONNEXION EST UNIQUE ET VALIDE
            if (gens.size() == 1) {
                String nomGenerateur = gens.get(0);
                Maison maison = maisons.get(nomMaison);

                // Verifie que la maison et le générateur existent toujours
                if (maison != null && charges.containsKey(nomGenerateur)) {
                    int consommation = maison.getConsommationKw();
                    charges.computeIfPresent(nomGenerateur, (k, v) -> v + consommation);
                }
            }
        }
        return charges;
    }

    // Calcule le taux d'utilisation pour chaque générateur.
    private Map<String, Double> calculerTauxUtilisation(Map<String, Integer> charges) {
        Map<String, Double> taux = new HashMap<>();
        for (Generateur gen : generateurs.values()) {
            int charge = charges.getOrDefault(gen.getNom(), 0);
            double tauxU = (gen.getCapaciteMax() == 0) ? 0 : (double) charge / gen.getCapaciteMax();
            taux.put(gen.getNom(), tauxU);
        }
        return taux;
    }

    // Calcule la dispersion des taux d'utilisation par rapport à la moyenne.
    private double calculerDispersion(Map<String, Double> tauxUtilisation, double moyenneTaux) {
        return tauxUtilisation.values().stream().mapToDouble(taux -> Math.abs(taux - moyenneTaux)).sum();
    }

    // Calcule la surcharge totale des generateurs.
    private double calculerSurcharge(Map<String, Integer> charges) {
        double surcharge = 0.0;
        for (Generateur gen : generateurs.values()) {
            int charge = charges.getOrDefault(gen.getNom(), 0);
            int capacite = gen.getCapaciteMax();
            if (charge > capacite) {
                surcharge += (double) (charge - capacite) / capacite;
            }
        }
        return surcharge;
    }
}