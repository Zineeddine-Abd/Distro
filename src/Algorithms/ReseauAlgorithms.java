package Algorithms;

import Model.Reseau;
import Model.Generateur;
import Model.Maison;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contient les algorithmes de calcul statiques pour le reseau (Cout, Surcharge, Dispersion)
 */
public class ReseauAlgorithms {

    // Methode statique pour calculer le coût
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

    private static Map<String, Double> calculerTauxUtilisation(Reseau reseau, Map<String, Integer> charges) {
        Map<String, Double> taux = new HashMap<>();
        for (Generateur gen : reseau.getGenerateurs().values()) {
            int charge = charges.getOrDefault(gen.getNom(), 0);
            double tauxU = (gen.getCapaciteMax() == 0) ? 0 : (double) charge / gen.getCapaciteMax();
            taux.put(gen.getNom(), tauxU);
        }
        return taux;
    }

    private static double calculerDispersion(Map<String, Double> tauxUtilisation, double moyenneTaux) {
        return tauxUtilisation.values().stream()
                .mapToDouble(taux -> Math.abs(taux - moyenneTaux)).sum();
    }

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