package Controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import Exceptions.InvalidNameException;
import Model.Consommation;
import Model.Generateur;
import Model.Maison;
import Model.Reseau;

class AppControllerTest {

  private static final double DELTA = 1e-6;

  @Test
  void addMaison_rejectsInvalidType() {
    AppController controller = new AppController(new Reseau());
    assertThrows(IllegalArgumentException.class, () -> controller.addMaison("M1", "UNKNOWN"));
  }

  @Test
  void addGenerateurAndMaison_returnExistedFlag() {
    AppController controller = new AppController(new Reseau());

    assertFalse(controller.addGenerateur("G1", 10));
    assertTrue(controller.addGenerateur("G1", 20)); // update

    assertFalse(controller.addMaison("M1", "BASSE"));
    assertTrue(controller.addMaison("M1", "FORTE")); // update
  }

  @Test
  void addGenerateur_rejectsNonAlphanumericName() {
    AppController controller = new AppController(new Reseau());
    assertThrows(InvalidNameException.class, () -> controller.addGenerateur("G_1", 10));
    assertThrows(InvalidNameException.class, () -> controller.addGenerateur("G 1", 10));
    assertThrows(InvalidNameException.class, () -> controller.addGenerateur("", 10));
  }

  @Test
  void addMaison_rejectsNonAlphanumericName() {
    AppController controller = new AppController(new Reseau());
    assertThrows(InvalidNameException.class, () -> controller.addMaison("M-1", "BASSE"));
    assertThrows(InvalidNameException.class, () -> controller.addMaison("M 1", "BASSE"));
    assertThrows(InvalidNameException.class, () -> controller.addMaison("  ", "BASSE"));
  }

  @Test
  void calculerCoutReseau_delegatesToAlgorithms() {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateGenerateur(new Generateur("G1", 50));
    reseau.addOrUpdateGenerateur(new Generateur("G2", 50));
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.FORTE));
    reseau.addOrUpdateMaison(new Maison("M2", Consommation.BASSE));
    reseau.setConnexionUnique("M1", "G1");
    reseau.setConnexionUnique("M2", "G2");

    AppController controller = new AppController(reseau);

    assertArrayEquals(reseau.calculerCout(), controller.calculerCoutReseau(), DELTA);
  }
}