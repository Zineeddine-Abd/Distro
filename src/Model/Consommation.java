package Model;

/**
 * Représente les catégories de consommation d'une maison.
 **/
public enum Consommation {
    BASSE(10),
    NORMALE(20),
    FORTE(40);

    private final int valeurKw;

    Consommation(int valeurKw) {
        this.valeurKw = valeurKw;
    }

    public int getValeurKw() {
        return valeurKw;
    }

    public static Consommation fromString(String texte) {

        for (Consommation c : Consommation.values()) {
            if (c.name().equalsIgnoreCase(texte)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Type de consommation inconnu : " + texte);
    }
}