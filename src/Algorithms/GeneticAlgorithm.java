package Algorithms;

import Model.Reseau;
import java.util.*;

public class GeneticAlgorithm {
    private final Reseau reseau;
    private final List<String> nomsMaisons;
    private final List<String> nomsGenerateurs;
    private final Random random;

    public GeneticAlgorithm(Reseau reseau) {
        this.reseau = reseau;
        // On cache les listes de noms pour optimiser les tirages aléatoires
        this.nomsMaisons = new ArrayList<>(reseau.getMaisons().keySet());
        this.nomsGenerateurs = new ArrayList<>(reseau.getGenerateurs().keySet());
        this.random = new Random();
    }

    /**
     * @param populationSize Taille de la population (ex: 50)
     * @param generations Nombre de générations (ex: 100)
     * @param mutationRate Taux de mutation (ex: 0.1 pour 10%)
     */
    public void solve(int populationSize, int generations, double mutationRate) {
        // 1 - Initialisation de la population
        List<Map<String, String>> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(genererSolutionAleatoire());
        }

        // 2 - Boucle d'evolution
        for (int i = 0; i < generations; i++) {
            // Trier la population par score (fitness) croissant (plus petit cout = meilleur)
            population.sort(Comparator.comparingDouble(this::calculerFitness));

            // Selection de l'elite (50% meilleurs)
            int tailleElite = populationSize / 2;
            List<Map<String, String>> nouvelleGeneration = new ArrayList<>(population.subList(0, tailleElite));

            // Reproduction pour remplir le reste de la population
            while (nouvelleGeneration.size() < populationSize) {
                // Tournoi ou choix aléatoire parmi l'élite
                Map<String, String> parent1 = nouvelleGeneration.get(random.nextInt(tailleElite));
                Map<String, String> parent2 = nouvelleGeneration.get(random.nextInt(tailleElite));

                Map<String, String> enfant = croisement(parent1, parent2);

                // Mutation eventuelle
                if (random.nextDouble() < mutationRate) {
                    mutation(enfant);
                }

                nouvelleGeneration.add(enfant);
            }

            population = nouvelleGeneration;
        }

        // 3 - Application finale de la meilleure solution trouvee
        population.sort(Comparator.comparingDouble(this::calculerFitness));
        Map<String, String> meilleureSolution = population.get(0);

        // On applique définitivement la meilleure configuration au réseau
        appliquerSolutionAuReseau(meilleureSolution);
    }

    // --- Methodes de l'Algorithme Genetique ---
    // Genere un "chromosome" (une configuration complete aleatoire)
    private Map<String, String> genererSolutionAleatoire() {
        Map<String, String> solution = new HashMap<>();
        for (String maison : nomsMaisons) {
            String generateur = nomsGenerateurs.get(random.nextInt(nomsGenerateurs.size()));
            solution.put(maison, generateur);
        }
        return solution;
    }

    // Calcule le score (le cout) d'une solution en l'appliquant temporairement au reseau
    private double calculerFitness(Map<String, String> solution) {
        appliquerSolutionAuReseau(solution);
        return ReseauAlgorithms.calculerCout(reseau)[0]; // Retourne le cout total
    }

    // Applique une map de configuration (Maison -> Generateur) a l'objet Reseau
    private void appliquerSolutionAuReseau(Map<String, String> solution) {
        for (Map.Entry<String, String> entry : solution.entrySet()) {
            reseau.setConnexionUnique(entry.getKey(), entry.getValue());
        }
    }

    // Croisement uniforme : pour chaque maison, on prend le generateur soit du parent 1, soit du parent 2
    private Map<String, String> croisement(Map<String, String> parent1, Map<String, String> parent2) {
        Map<String, String> enfant = new HashMap<>();
        for (String maison : nomsMaisons) {
            if (random.nextBoolean()) {
                enfant.put(maison, parent1.get(maison));
            } else {
                enfant.put(maison, parent2.get(maison));
            }
        }
        return enfant;
    }

    // Mutation : modifie le generateur d'une maison au hasard
    private void mutation(Map<String, String> solution) {
        String maisonAleatoire = nomsMaisons.get(random.nextInt(nomsMaisons.size()));
        String nouveauGenerateur = nomsGenerateurs.get(random.nextInt(nomsGenerateurs.size()));
        solution.put(maisonAleatoire, nouveauGenerateur);
    }
}