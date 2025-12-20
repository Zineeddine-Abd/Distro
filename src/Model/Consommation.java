package Model;

// Représente les catégories de consommation d'une maison.
// Pour la garantie de la securité et la cohérence des données, on utilise une énumération.
// Pour la centralisation et la scalabilité.

public enum Consommation {
    BASSE(10),
    NORMAL(20),
    FORTE(40);

    private final int valeurKw;

    Consommation(int valeurKw) {
        this.valeurKw = valeurKw;
    }

    public int getValeurKw() {
        return valeurKw;
    }

    public static Consommation fromString(String texte) throws IllegalArgumentException {

        for (Consommation c : Consommation.values()) {
            if (c.name().equalsIgnoreCase(texte)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Type de consommation inconnu : " + texte);
    }
}