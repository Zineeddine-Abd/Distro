package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ReseauTest {

  @Test
  void addOrUpdateMaisonAndGenerateur_replacesExistingEntries() {
    Reseau reseau = new Reseau();

    reseau.addOrUpdateMaison(new Maison("M1", Consommation.BASSE));
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.FORTE));

    assertEquals(1, reseau.getMaisons().size());
    assertEquals(Consommation.FORTE, reseau.getMaisons().get("M1").getConsommation());

    reseau.addOrUpdateGenerateur(new Generateur("G1", 10));
    reseau.addOrUpdateGenerateur(new Generateur("G1", 25));

    assertEquals(1, reseau.getGenerateurs().size());
    assertEquals(25, reseau.getGenerateurs().get("G1").getCapaciteMax());
  }

  @Test
  void addOrUpdateGenerateur_rejectsNonPositiveCapacity() {
    Reseau reseau = new Reseau();
    assertThrows(IllegalArgumentException.class, () -> reseau.addOrUpdateGenerateur(new Generateur("G1", 0)));
    assertThrows(IllegalArgumentException.class, () -> reseau.addOrUpdateGenerateur(new Generateur("G2", -5)));
  }

  @Test
  void connexionCrudOperations_andExistenceChecks() {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.BASSE));
    reseau.addOrUpdateGenerateur(new Generateur("G1", 50));
    reseau.addOrUpdateGenerateur(new Generateur("G2", 50));

    reseau.creerConnexion("M1", "G1");
    assertTrue(reseau.connexionExiste("M1", "G1"));
    assertTrue(reseau.connexionExistePourMaison("M1"));

    reseau.creerConnexion("M1", "G2");
    assertTrue(reseau.connexionExiste("M1", "G2"));
    assertEquals(2, reseau.getConnexions().get("M1").size());

    reseau.setConnexionUnique("M1", "G2");
    assertTrue(reseau.connexionExiste("M1", "G2"));
    assertFalse(reseau.connexionExiste("M1", "G1"));
    assertEquals(1, reseau.getConnexions().get("M1").size());

    reseau.supprimerConnexion("M1", "G2");
    assertFalse(reseau.connexionExistePourMaison("M1"));
    assertFalse(reseau.getConnexions().containsKey("M1"));
  }

  @Test
  void validerConfiguration_reportsMissingMaisonAndGenerateur() {
    Reseau reseau = new Reseau();
    List<String> problemes = reseau.validerConfiguration();

    assertTrue(problemes.stream().anyMatch(p -> p.startsWith("Aucune maison definie")));
    assertTrue(problemes.stream().anyMatch(p -> p.startsWith("Aucun generateur defini")));
  }

  @Test
  void validerConfiguration_reportsMissingConnection() {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.BASSE));
    reseau.addOrUpdateGenerateur(new Generateur("G1", 20));

    List<String> problemes = reseau.validerConfiguration();

    assertEquals(1, problemes.size());
    assertTrue(
        problemes.stream().anyMatch(p -> p.startsWith("Aucune connexion definie entre maisons et generateurs.")));
  }

  @Test
  void validerConfiguration_reportsCapacityInsufficient() {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.FORTE));
    reseau.addOrUpdateGenerateur(new Generateur("G1", 10));
    reseau.creerConnexion("M1", "G1");

    List<String> problemes = reseau.validerConfiguration();
    assertEquals(1, problemes.size());
    assertTrue(problemes.get(0).contains("insuffisante"));
  }

  @Test
  void validerConfiguration_reportsMultipleConnections() {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.BASSE));
    reseau.addOrUpdateGenerateur(new Generateur("G1", 30));
    reseau.addOrUpdateGenerateur(new Generateur("G2", 30));

    reseau.creerConnexion("M1", "G1");
    reseau.creerConnexion("M1", "G2");

    List<String> problemes = reseau.validerConfiguration();

    assertEquals(1, problemes.size());
    assertTrue(problemes.get(0).contains("trop de connexions"));
    assertTrue(problemes.get(0).contains("G1"));
    assertTrue(problemes.get(0).contains("G2"));
  }
}