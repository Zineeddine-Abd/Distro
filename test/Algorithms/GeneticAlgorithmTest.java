package test.Algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import Algorithms.GeneticAlgorithm;
import Model.Consommation;
import Model.Generateur;
import Model.Maison;
import Model.Reseau;

class GeneticAlgorithmTest {

  @Test
  void solve_connectsSingleMaisonToSingleGenerateur() {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateGenerateur(new Generateur("G1", 50));
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.BASSE));

    GeneticAlgorithm ga = new GeneticAlgorithm(reseau, new Random(1));
    ga.solve();

    assertTrue(reseau.connexionExiste("M1", "G1"));
  }

  @Test
  void solve_withEmptyNetworkThrows() {
    GeneticAlgorithm ga = new GeneticAlgorithm(new Reseau(), new Random(1));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, ga::solve);
  }

  @Test
  void solve_doesNotIncreaseCostWithDeterministicSeed() {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateGenerateur(new Generateur("G1", 40));
    reseau.addOrUpdateGenerateur(new Generateur("G2", 40));
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.FORTE)); // 40
    reseau.addOrUpdateMaison(new Maison("M2", Consommation.FORTE)); // 40

    // Mauvais repartition initiale : tout sur G1
    reseau.setConnexionUnique("M1", "G1");
    reseau.setConnexionUnique("M2", "G1");

    double coutAvant = reseau.calculerCout()[0];

    GeneticAlgorithm ga = new GeneticAlgorithm(reseau, new Random(1234));
    ga.solve();

    double coutApres = reseau.calculerCout()[0];
    assertFalse(Double.isNaN(coutApres));
    assertTrue(coutApres <= coutAvant + 1e-6);
  }
}
