package Algorithms;

import Model.Generateur;
import Model.Maison;
import Model.Reseau;

import java.util.*;
import java.util.concurrent.*;

public class GeneticAlgorithm {
    private final Reseau reseauOriginal;
    private final List<String> nomsMaisons;
    private final List<String> nomsGenerateurs;
    private final Random random;

    public GeneticAlgorithm(Reseau reseau) {
        this.reseauOriginal = reseau;
        this.nomsMaisons = new ArrayList<>(reseau.getMaisons().keySet());
        this.nomsGenerateurs = new ArrayList<>(reseau.getGenerateurs().keySet());
        this.random = new Random();
    }

    /**
     * Point d'entrée principal.
     * Calcule automatiquement les paramètres optimaux et lance l'éxécution en parallele.
     */
    public void solve(int populationSizeIgnored, int generationsIgnored, double mutationRateIgnored) {
        // 1- Calcul Dynamique des Paramètres
        int nbMaisons = nomsMaisons.size();

        // Minimum 50 individus, sinon 5x le nombre de maisons
        int dynamicPopSize = Math.max(20, nbMaisons * 5);

        // Minimum 1000 générations, sinon 100x le nombre de maisons
        int dynamicGenerations = Math.max(1000, nbMaisons * 100);

        double dynamicMutation = 0.1; // 10% est un standard qu'on a trouvé efficace

        // 2- Préparation du Multi-threading
        int nbThreads = Runtime.getRuntime().availableProcessors(); // Utilise tous les cœurs disponibles
        ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
        List<Future<ResultatEvolution>> futures = new ArrayList<>();
        System.out.printf("Lancement de la resolution automatique sur %d threads (Pop: %d, Gen: %d)...\n",
                nbThreads, dynamicPopSize, dynamicGenerations);

        // 3- Lancement de l'algo en paralléle
        for (int i = 0; i < nbThreads; i++) {
            Reseau reseauClone = clonerReseau(this.reseauOriginal);

            Callable<ResultatEvolution> task = () -> executerEvolution(reseauClone, dynamicPopSize, dynamicGenerations, dynamicMutation);
            futures.add(executor.submit(task));
        }

        // 4- Récupération et comparaison des resultats
        Map<String, String> meilleureSolutionGlobale = null;
        double meilleurCoutGlobal = Double.MAX_VALUE;

        try {
            for (Future<ResultatEvolution> f : futures) {
                ResultatEvolution res = f.get(); // Attend la fin du thread
                if (res.cout < meilleurCoutGlobal) {
                    meilleurCoutGlobal = res.cout;
                    meilleureSolutionGlobale = res.solution;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        // 5- Application finale sur le vrai reseau
        if (meilleureSolutionGlobale != null) {
            appliquerSolutionAuReseau(this.reseauOriginal, meilleureSolutionGlobale);
        }
    }

    /**
     * l'algorithme génétique (independant par thread)
     */
    private ResultatEvolution executerEvolution(Reseau reseauLocal, int populationSize, int generations, double mutationRate) {
        Random threadRandom = new Random();

        List<Map<String, String>> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(genererSolutionAleatoire(threadRandom));
        }

        for (int i = 0; i < generations; i++) {
            // Tri par fitness (Coût croissant)
            population.sort(Comparator.comparingDouble(sol -> calculerFitness(reseauLocal, sol)));

            // Elitisme : On garde les 50% meilleurs
            int tailleElite = populationSize / 2;
            List<Map<String, String>> nouvelleGeneration = new ArrayList<>(population.subList(0, tailleElite));

            // Reproduction :
            while (nouvelleGeneration.size() < populationSize) {
                Map<String, String> parent1 = nouvelleGeneration.get(threadRandom.nextInt(tailleElite));
                Map<String, String> parent2 = nouvelleGeneration.get(threadRandom.nextInt(tailleElite));

                Map<String, String> enfant = croisement(parent1, parent2, threadRandom);

                if (threadRandom.nextDouble() < mutationRate) {
                    mutation(enfant, threadRandom);
                }
                nouvelleGeneration.add(enfant);
            }
            population = nouvelleGeneration;
        }

        // Retourner le meilleur resultat de ce thread
        population.sort(Comparator.comparingDouble(sol -> calculerFitness(reseauLocal, sol)));
        Map<String, String> meilleur = population.get(0);
        double cout = calculerFitness(reseauLocal, meilleur);

        return new ResultatEvolution(meilleur, cout);
    }

    // --- Utils ---

    // Classe interne simple pour retourner le couple (Solution, Coût) depuis un thread
    private static class ResultatEvolution {
        Map<String, String> solution;
        double cout;
        public ResultatEvolution(Map<String, String> s, double c) { this.solution = s; this.cout = c; }
    }

    // Clone le réseau (Maisons et Générateurs) pour travailler en isolation.
    private Reseau clonerReseau(Reseau original) {
        Reseau clone = new Reseau();
        clone.setLambda(original.getLambda());

        for (Generateur g : original.getGenerateurs().values()) {
            clone.addOrUpdateGenerateur(new Generateur(g.getNom(), g.getCapaciteMax()));
        }

        for (Maison m : original.getMaisons().values()) {
            clone.addOrUpdateMaison(new Maison(m.getNom(), m.getConsommation()));
        }
        return clone;
    }

    // --- Utils Genetique ---

    private Map<String, String> genererSolutionAleatoire(Random rnd) {
        Map<String, String> solution = new HashMap<>();
        for (String maison : nomsMaisons) {
            String generateur = nomsGenerateurs.get(rnd.nextInt(nomsGenerateurs.size()));
            solution.put(maison, generateur);
        }
        return solution;
    }

    private double calculerFitness(Reseau r, Map<String, String> solution) {
        appliquerSolutionAuReseau(r, solution);
        return ReseauAlgorithms.calculerCout(r)[0];
    }

    private void appliquerSolutionAuReseau(Reseau r, Map<String, String> solution) {
        for (Map.Entry<String, String> entry : solution.entrySet()) {
            r.setConnexionUnique(entry.getKey(), entry.getValue());
        }
    }

    private Map<String, String> croisement(Map<String, String> p1, Map<String, String> p2, Random rnd) {
        Map<String, String> enfant = new HashMap<>();
        for (String maison : nomsMaisons) {
            enfant.put(maison, rnd.nextBoolean() ? p1.get(maison) : p2.get(maison));
        }
        return enfant;
    }

    private void mutation(Map<String, String> solution, Random rnd) {
        String maisonAleatoire = nomsMaisons.get(rnd.nextInt(nomsMaisons.size()));
        String nouveauGenerateur = nomsGenerateurs.get(rnd.nextInt(nomsGenerateurs.size()));
        solution.put(maisonAleatoire, nouveauGenerateur);
    }
}