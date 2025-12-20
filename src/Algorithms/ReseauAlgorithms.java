package Algorithms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Model.Generateur;
import Model.Maison;
import Model.Reseau;

/**
 * Contient les algorithmes de calcul statiques pour le reseau (Cout, Surcharge,
 * Dispersion)
 */
public class ReseauAlgorithms {

    // Calcule le score global du reseau (le cout). Plus ce chiffre est bas, plus le
    // reseau est efficace.
    // Elle renvoie trois valeurs : le cout total, la dispersion et la surcharge.
    public static double[] calculerCout(Reseau reseau) {
        if (reseau.getGenerateurs().isEmpty())
            return new double[] { 0, 0, 0 };

        Map<String, Integer> charges = calculerCharges(reseau);
        Map<String, Double> tauxUtilisation = calculerTauxUtilisation(reseau, charges);

        double moyenneTaux = tauxUtilisation.values().stream()
                .mapToDouble(d -> d).average().orElse(0.0);

        double dispersion = calculerDispersion(tauxUtilisation, moyenneTaux);
        double surcharge = calculerSurcharge(reseau, charges);

        double coutTotal = dispersion + reseau.getLambda() * surcharge;

        return new double[] { coutTotal, dispersion, surcharge };
    }

    // Calcule la charge electrique totale demandee a chaque generateur.
    // Elle additionne la consommation de toutes les maisons connectees a ce
    // generateur.
    private static Map<String, Integer> calculerCharges(Reseau reseau) {
        Map<String, Integer> charges = new HashMap<>();
        reseau.getGenerateurs().keySet().forEach(nom -> charges.put(nom, 0));

        for (Map.Entry<String, List<String>> entry : reseau.getConnexions().entrySet()) {
            String nomMaison = entry.getKey();
            List<String> gens = entry.getValue();

            if (gens.size() == 1) {
                String nomGenerateur = gens.get(0);
                Maison maison = reseau.getMaisons().get(nomMaison);

                if (maison != null && charges.containsKey(nomGenerateur)) {
                    int consommation = maison.getConsommationKw();
                    charges.computeIfPresent(nomGenerateur, (k, v) -> v + consommation);
                }
            }
        }
        return charges;
    }

    // Calcule le pourcentage d'utilisation de chaque generateur.
    // C'est le rapport entre ce qu'il produit actuellement et sa capacite maximale.
    private static Map<String, Double> calculerTauxUtilisation(Reseau reseau, Map<String, Integer> charges) {
        Map<String, Double> taux = new HashMap<>();
        for (Generateur gen : reseau.getGenerateurs().values()) {
            int charge = charges.getOrDefault(gen.getNom(), 0);
            double tauxU = (gen.getCapaciteMax() == 0) ? 0 : (double) charge / gen.getCapaciteMax();
            taux.put(gen.getNom(), tauxU);
        }
        return taux;
    }

    // Mesure a quel point la charge est mal repartie entre les generateurs.
    // Si tout le monde travaille de facon equitable, ce chiffre est bas (ce qui est
    // bien).
    private static double calculerDispersion(Map<String, Double> tauxUtilisation, double moyenneTaux) {
        return tauxUtilisation.values().stream()
                .mapToDouble(taux -> Math.abs(taux - moyenneTaux)).sum();
    }

    // Calcule la penalite si des generateurs produisent plus que leur maximum.
    // Si un generateur est en surchauffe, ce chiffre augmente fortement le cout
    // total.
    private static double calculerSurcharge(Reseau reseau, Map<String, Integer> charges) {
        double surcharge = 0.0;
        for (Generateur gen : reseau.getGenerateurs().values()) {
            int charge = charges.getOrDefault(gen.getNom(), 0);
            int capacite = gen.getCapaciteMax();
            if (charge > capacite) {
                surcharge += (double) (charge - capacite) / capacite;
            }
        }
        return surcharge;
    }
}