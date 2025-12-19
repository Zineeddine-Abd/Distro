package test.Algorithms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

import main.Model.Consommation;
import main.Model.Generateur;
import main.Model.Maison;
import main.Model.Reseau;
import main.Algorithms.ReseauAlgorithms;

class ReseauCostTest {

  private static final double DELTA = 1e-6;

  @Test
  void balancedNetwork_hasZeroDispersionAndNoSurcharge() {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateGenerateur(new Generateur("G1", 40));
    reseau.addOrUpdateGenerateur(new Generateur("G2", 40));
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.NORMAL));
    reseau.addOrUpdateMaison(new Maison("M2", Consommation.NORMAL));
    reseau.setConnexionUnique("M1", "G1");
    reseau.setConnexionUnique("M2", "G2");

    double[] expected = { 0.0, 0.0, 0.0 };

    assertArrayEquals(expected, reseau.calculerCout(), DELTA);
    assertArrayEquals(expected, ReseauAlgorithms.calculerCout(reseau), DELTA);
  }

  @Test
  void unbalancedWithoutSurcharge_producesDispersionOnly() {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateGenerateur(new Generateur("G1", 60));
    reseau.addOrUpdateGenerateur(new Generateur("G2", 60));
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.FORTE)); // 40
    reseau.addOrUpdateMaison(new Maison("M2", Consommation.BASSE)); // 10
    reseau.setConnexionUnique("M1", "G1");
    reseau.setConnexionUnique("M2", "G2");

    // charges: G1=40, G2=10 => taux: 0.6667 et 0.1667, dispersion = 0.5
    double[] expected = { 0.5, 0.5, 0.0 };

    assertArrayEquals(expected, reseau.calculerCout(), DELTA);
    assertArrayEquals(expected, ReseauAlgorithms.calculerCout(reseau), DELTA);
  }

  @Test
  void surchargeCase_combinesDispersionAndSurcharge() {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateGenerateur(new Generateur("G1", 50));
    reseau.addOrUpdateGenerateur(new Generateur("G2", 50));
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.FORTE)); // 40
    reseau.addOrUpdateMaison(new Maison("M2", Consommation.FORTE)); // 40
    reseau.setConnexionUnique("M1", "G1");
    reseau.setConnexionUnique("M2", "G1");

    // charges: G1=80, G2=0 => taux: 1.6 et 0; dispersion=1.6; surcharge=0.6
    double[] expected = { 7.6, 1.6, 0.6 }; // cout = dispersion + lambda(10)*surcharge

    assertArrayEquals(expected, reseau.calculerCout(), DELTA);
    assertArrayEquals(expected, ReseauAlgorithms.calculerCout(reseau), DELTA);
  }

  @Test
  void lambdaVariation_scalesSurchargeContribution() {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateGenerateur(new Generateur("G1", 50));
    reseau.addOrUpdateGenerateur(new Generateur("G2", 50));
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.FORTE));
    reseau.addOrUpdateMaison(new Maison("M2", Consommation.FORTE));
    reseau.setConnexionUnique("M1", "G1");
    reseau.setConnexionUnique("M2", "G1");
    reseau.setLambda(2); // surcharge=0.6, dispersion=1.6 => cout=1.6+(2*0.6)=2.8

    double[] expected = { 2.8, 1.6, 0.6 };

    assertArrayEquals(expected, reseau.calculerCout(), DELTA);
    assertArrayEquals(expected, ReseauAlgorithms.calculerCout(reseau), DELTA);
  }

  @Test
  void emptyGenerators_returnsZeros() {
    Reseau reseau = new Reseau();
    double[] expected = { 0.0, 0.0, 0.0 };

    assertArrayEquals(expected, reseau.calculerCout(), DELTA);
    assertArrayEquals(expected, ReseauAlgorithms.calculerCout(reseau), DELTA);
  }
}
