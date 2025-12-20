package Algorithms;

import Model.Generateur;
import Model.Maison;
import Model.Reseau;

import java.util.*;
import java.util.concurrent.*;

// Cette classe est le cerveau de l'optimisation.
// Elle inspire de l'algorithme genetique (inspire de l'evolution naturelle) pour trouver
// automatiquement la meilleure facon de connecter les maisons aux generateurs.
// On a adapté l'algorithme genetique a notre probleme et pour assuré l'optimalité on a ajouté deux foncionalites :
// - Le multi-threading pour exploiter tous les coeurs du processeur et trouver une solution plus rapidement et proche de l'optimale en comparant les resultats.
// - Un calcul dynamique des parametres (taille de population, nombre de generations) en fonction de la taille du reseau a optimiser.
public class GeneticAlgorithm {

    // Le reseau sur lequel on travaille (celui qu'on veut optimiser)
    private final Reseau reseauOriginal;

    // Liste simple des noms des maisons pour pouvoir en choisir une au hasard
    // facilement
    private final List<String> nomsMaisons;

    // Liste simple des noms des generateurs pour les tirages aleatoires
    private final List<String> nomsGenerateurs;

    // Generateur de nombres aleatoires (injectable pour les tests - FONCTIONNALITÉ SOURCE A)
    private final Random random;

    // Prepare l'algorithme en recuperant les listes de maisons et de generateurs
    public GeneticAlgorithm(Reseau reseau) {
        this(reseau, new Random());
    }

    // Surcharge pour injecter un Random deterministe (utilise dans les tests pour
    // eviter le flakiness - FONCTIONNALITÉ SOURCE A)
    public GeneticAlgorithm(Reseau reseau, Random random) {
        this.reseauOriginal = reseau;
        this.nomsMaisons = new ArrayList<>(reseau.getMaisons().keySet());
        this.nomsGenerateurs = new ArrayList<>(reseau.getGenerateurs().keySet());
        this.random = (random == null) ? new Random() : random;
    }

    // C'est le point de depart. Cette methode calcule combien de temps l'algorithme doit tourner,
    // lance plusieurs recherches en parallele sur le processeur, et applique la meilleure solution trouvee a la fin.
    public void solve() {
        // Validation (SOURCE A)
        if (nomsGenerateurs.isEmpty() || nomsMaisons.isEmpty()) {
            throw new IllegalStateException(
                    "Le reseau doit contenir au moins un generateur et une maison avant l'optimisation.");
        }

        // 1- Calcul Dynamique des Paramètres (VALEURS SOURCE B)
        int nbMaisons = nomsMaisons.size();

        // On adapte la taille de la population selon la taille du reseau (minimum 50 solutions testees a la fois)
        int dynamicPopSize = Math.max(50, nbMaisons * 5);

        // On decide combien de fois on va ameliorer la solution (minimum 1000 cycles, plafonné à 20000)
        int dynamicGenerations = Math.min(20000, Math.max(1000, nbMaisons * 100));

        // Probabilite qu'une solution change un petit detail au hasard (10%)
        double dynamicMutation = 0.1;

        // 2- Préparation du Multi-threading
        // On regarde combien de coeurs a le processeur pour travailler plus vite
        int nbThreads = Runtime.getRuntime().availableProcessors();
        if (nbThreads < 1) nbThreads = 1; // Securite (SOURCE B)

        ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
        List<Future<ResultatEvolution>> futures = new ArrayList<>();
        System.out.printf("\nLancement de la resolution automatique sur %d threads (Pop: %d, Gen: %d)...\n",
                nbThreads, dynamicPopSize, dynamicGenerations);

        // 3- Lancement de l'algo en paralléle
        // On utilise la graine deterministe de SOURCE A pour la reproductibilité
        long baseSeed = random.nextLong();

        for (int i = 0; i < nbThreads; i++) {
            // Important : on clone le reseau pour que chaque thread travaille sur sa propre
            // copie sans gener les autres
            Reseau reseauClone = clonerReseau(this.reseauOriginal);

            // Utilise un Random derive pour garantir la reproductibilite par thread (LOGIQUE SOURCE A)
            Random threadRandom = new Random(baseSeed + i);

            Callable<ResultatEvolution> task = () -> executerEvolution(reseauClone, dynamicPopSize, dynamicGenerations,
                    dynamicMutation, threadRandom);
            futures.add(executor.submit(task));
        }

        // 4- Récupération et comparaison des resultats
        Map<String, String> meilleureSolutionGlobale = null;
        double meilleurCoutGlobal = Double.MAX_VALUE;

        try {
            // On recupere le resultat de chaque thread et on garde le meilleur de tous
            for (Future<ResultatEvolution> f : futures) {
                ResultatEvolution res = f.get(); // Attend la fin du thread
                if (res.cout < meilleurCoutGlobal) {
                    meilleurCoutGlobal = res.cout;
                    meilleureSolutionGlobale = res.solution;
                }
            }
        } catch (InterruptedException | ExecutionException | RuntimeException e) {
            // LOGIQUE SOURCE B : On ne plante pas, on loggue l'erreur et on passe au Plan B
            System.err.println(">> ERREUR CRITIQUE DANS LE MULTI-THREADING : " + e.getMessage());
            e.printStackTrace();
            meilleureSolutionGlobale = null; // Force le passage au plan B
        } finally {
            // On ferme proprement les outils de calcul parallele
            executor.shutdownNow(); // ShutdownNow est plus sûr pour forcer l'arrêt (SOURCE B)
        }

        // --- PLAN B : Mode Secours (FONCTIONNALITÉ SOURCE B) ---
        // Si le multi-threading a echoué ou n'a rien trouvé, on execute l'algo sur le thread principal
        if (meilleureSolutionGlobale == null) {
            System.out.println(">> Basculement vers le mode de secours (Single Thread)...");
            try {
                // On lance une seule evolution classique
                // Note : On passe 'this.random' pour respecter la logique de Source A
                ResultatEvolution resSecours = executerEvolution(clonerReseau(this.reseauOriginal), dynamicPopSize, dynamicGenerations, dynamicMutation, this.random);
                meilleureSolutionGlobale = resSecours.solution;
                System.out.println(">> Solution de secours trouvée.");
            } catch (Exception e) {
                System.err.println(">> ECHEC TOTAL : Même le mode de secours a échoué.");
                e.printStackTrace();
                return;
            }
        }

        // 5- Application finale sur le vrai reseau
        // On applique la solution gagnante au vrai reseau pour que l'utilisateur voie le resultat
        if (meilleureSolutionGlobale != null) {
            appliquerSolutionAuReseau(this.reseauOriginal, meilleureSolutionGlobale);
        }
    }

    // C'est le coeur de l'algorithme qui tourne sur chaque thread.
    // Il cree une population de solutions, les fait se reproduire et muter pour
    // trouver la meilleure configuration.
    // (Signature SOURCE A conservée : prend un Random en paramètre)
    private ResultatEvolution executerEvolution(Reseau reseauLocal, int populationSize, int generations,
                                                double mutationRate, Random threadRandom) {
        // Creation de la premiere generation completement au hasard
        List<Map<String, String>> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(genererSolutionAleatoire(threadRandom));
        }

        // Boucle d'evolution (on repete le processus 'generations' fois)
        for (int i = 0; i < generations; i++) {
            // On trie les solutions : les meilleures (cout le plus bas) en premier
            population.sort(Comparator.comparingDouble(sol -> calculerFitness(reseauLocal, sol)));

            // Elitisme : On ne garde que la moitie la plus performante (les parents)
            int tailleElite = populationSize / 2;
            List<Map<String, String>> nouvelleGeneration = new ArrayList<>(population.subList(0, tailleElite));

            // Reproduction : On cree des enfants jusqu'a remplir la population
            while (nouvelleGeneration.size() < populationSize) {
                // On choisit deux parents au hasard parmi l'elite
                Map<String, String> parent1 = nouvelleGeneration.get(threadRandom.nextInt(tailleElite));
                Map<String, String> parent2 = nouvelleGeneration.get(threadRandom.nextInt(tailleElite));

                // On cree un enfant en melangeant les parents
                Map<String, String> enfant = croisement(parent1, parent2, threadRandom);

                // Parfois, on applique une petite mutation (changement aleatoire) pour eviter
                // les optima locaux
                if (threadRandom.nextDouble() < mutationRate) {
                    mutation(enfant, threadRandom);
                }
                nouvelleGeneration.add(enfant);
            }
            // La nouvelle generation remplace l'ancienne
            population = nouvelleGeneration;
        }

        // A la fin, on trie une derniere fois pour trouver la meuilleure solution
        population.sort(Comparator.comparingDouble(sol -> calculerFitness(reseauLocal, sol)));
        Map<String, String> meilleur = population.get(0);
        double cout = calculerFitness(reseauLocal, meilleur);

        return new ResultatEvolution(meilleur, cout);
    }

    // --- Utils ---

    // Une petit conteneur pour transporter a la fois la solution (le plan de connexion) et son score (le cout)
    private static class ResultatEvolution {
        Map<String, String> solution;
        double cout;

        public ResultatEvolution(Map<String, String> s, double c) {
            this.solution = s;
            this.cout = c;
        }
    }

    // Cree une copie complete du reseau.
    // C'est indispensable pour que chaque thread puisse faire ses tests sans casser le reseau principal.
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

    // Cree une solution au hasard : connecte chaque maison a un generateur tire au sort
    private Map<String, String> genererSolutionAleatoire(Random rnd) {
        Map<String, String> solution = new HashMap<>();
        for (String maison : nomsMaisons) {
            String generateur = nomsGenerateurs.get(rnd.nextInt(nomsGenerateurs.size()));
            solution.put(maison, generateur);
        }
        return solution;
    }

    // Calcule le score d'une solution (le cout). Plus c'est bas, mieux c'est.
    // Pour ca, on applique temporairement la solution au reseau pour utiliser la methode de calcul existante.
    private double calculerFitness(Reseau r, Map<String, String> solution) {
        appliquerSolutionAuReseau(r, solution);
        return ReseauAlgorithms.calculerCout(r)[0];
    }

    // Prend un plan de connexion (Map) et modifie le reseau pour qu'il corresponde a ce plan
    private void appliquerSolutionAuReseau(Reseau r, Map<String, String> solution) {
        for (Map.Entry<String, String> entry : solution.entrySet()) {
            r.setConnexionUnique(entry.getKey(), entry.getValue());
        }
    }

    // Melange deux solutions parents pour creer un enfant.
    // Pour chaque maison, on garde soit la connexion du pere, soit celle de la mere.
    private Map<String, String> croisement(Map<String, String> p1, Map<String, String> p2, Random rnd) {
        Map<String, String> enfant = new HashMap<>();
        for (String maison : nomsMaisons) {
            enfant.put(maison, rnd.nextBoolean() ? p1.get(maison) : p2.get(maison));
        }
        return enfant;
    }

    // Change aleatoirement la connexion d'une maison.
    // Cela permet d'explorer de nouvelles solutions que les parents n'avaient pas.
    private void mutation(Map<String, String> solution, Random rnd) {
        String maisonAleatoire = nomsMaisons.get(rnd.nextInt(nomsMaisons.size()));
        String nouveauGenerateur = nomsGenerateurs.get(rnd.nextInt(nomsGenerateurs.size()));
        solution.put(maisonAleatoire, nouveauGenerateur);
    }
}