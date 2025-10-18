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
    private final Map<String, Maison> maisons = new HashMap<>();
    private final Map<String, Generateur> generateurs = new HashMap<>();
    private final Map<String, String> connexions = new HashMap<>(); // Key : nomMaison, Value : nomGenerateur

    // --- Methodes pour la gestion du reseau ---
    public boolean maisonExiste(String nom) { return maisons.containsKey(nom); }
    public boolean generateurExiste(String nom) { return generateurs.containsKey(nom); }
    public void addOrUpdateMaison(Maison maison) { maisons.put(maison.getNom(), maison); }
    public void addOrUpdateGenerateur(Generateur generateur) { generateurs.put(generateur.getNom(), generateur); }
    public void creerConnexion(String nomMaison, String nomGenerateur) { connexions.put(nomMaison, nomGenerateur); }
    public String getConnexionPourMaison(String nomMaison) { return connexions.get(nomMaison); }
    public boolean connexionExiste(String nomMaison, String nomGenerateur) { return nomGenerateur.equals(connexions.get(nomMaison)); }

    public List<String> validerConfiguration() {

        List<String> problemes = new ArrayList<>();
        for (String nomMaison : maisons.keySet()) {
            if (!connexions.containsKey(nomMaison)) {
                problemes.add(nomMaison + " (n'est pas connectée)");
            }
        }

        if(connexions.isEmpty()) {
            problemes.add("Aucune connexion defenie, veuillez definir au moins une connexion.");
        }

        return problemes;
    }

    // --- Getters ---
    public Map<String, Maison> getMaisons() { return maisons; }
    public Map<String, Generateur> getGenerateurs() { return generateurs; }
    public Map<String, String> getConnexions() { return connexions; }

    // --- Methodes de calcul du cout ---
    public double[] calculerCout() {

        if (generateurs.isEmpty()) return new double[]{0, 0, 0};

        Map<String, Integer> charges = calculerCharges();
        Map<String, Double> tauxUtilisation = calculerTauxUtilisation(charges);
        double moyenneTaux = tauxUtilisation.values().stream().mapToDouble(d -> d).average().orElse(0.0);

        double dispersion = calculerDispersion(tauxUtilisation, moyenneTaux);
        double surcharge = calculerSurcharge(charges);

        final int LAMBDA = 10;
        double coutTotal = dispersion + LAMBDA * surcharge;

        return new double[]{coutTotal, dispersion, surcharge};
    }

    private Map<String, Integer> calculerCharges() {
        Map<String, Integer> charges = new HashMap<>();
        generateurs.keySet().forEach(nom -> charges.put(nom, 0));
        for (Map.Entry<String, String> connexion : connexions.entrySet()) {
            int consommation = maisons.get(connexion.getKey()).getConsommationKw();
            charges.computeIfPresent(connexion.getValue(), (k, v) -> v + consommation);
        }
        return charges;
    }

    private Map<String, Double> calculerTauxUtilisation(Map<String, Integer> charges) {
        Map<String, Double> taux = new HashMap<>();
        for (Generateur gen : generateurs.values()) {
            int charge = charges.getOrDefault(gen.getNom(), 0);
            double tauxU = (gen.getCapaciteMax() == 0) ? 0 : (double) charge / gen.getCapaciteMax();
            taux.put(gen.getNom(), tauxU);
        }
        return taux;
    }

    private double calculerDispersion(Map<String, Double> tauxUtilisation, double moyenneTaux) {
        return tauxUtilisation.values().stream().mapToDouble(taux -> Math.abs(taux - moyenneTaux)).sum();
    }

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
