package Model;

import java.util.Objects;

public class Maison {
    private final String nom;
    private Consommation consommation;

    public Maison(String nom, Consommation consommation) {
        this.nom = nom;
        this.consommation = consommation;
    }

    public String getNom() { return nom; }
    public Consommation getConsommation() { return consommation; }
    public int getConsommationKw() { return consommation.getValeurKw(); }
    public void setConsommation(Consommation consommation) { this.consommation = consommation; }

    @Override
    public String toString() { return String.format("%s (%d kW)", nom, getConsommationKw()); }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Maison maison = (Maison) o;
        return Objects.equals(nom, maison.nom);
    }

}
