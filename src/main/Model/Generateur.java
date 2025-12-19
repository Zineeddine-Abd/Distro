package main.Model;

import java.util.Objects;

// Representation d'un generateur dans le reseau.

public class Generateur {
    private final String nom;
    private int capaciteMax;

    public Generateur(String nom, int capaciteMax) {
        this.nom = nom;
        this.capaciteMax = capaciteMax;
    }

    public String getNom() {
        return nom;
    }

    public int getCapaciteMax() {
        return capaciteMax;
    }

    public void setCapaciteMax(int capaciteMax) {
        this.capaciteMax = capaciteMax;
    }

    @Override
    public String toString() {
        return String.format("%s (Capacite: %d kW)", nom, capaciteMax);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Generateur that = (Generateur) o;
        return Objects.equals(nom, that.nom);
    }
}