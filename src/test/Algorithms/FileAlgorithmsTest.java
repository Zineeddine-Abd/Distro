package test.Algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import main.Exceptions.FileSyntaxException;
import main.Exceptions.ReseauInvalideException;
import main.Model.Consommation;
import main.Model.Reseau;
import main.Algorithms.FileAlgorithms;

class FileAlgorithmsTest {

  @TempDir
  Path tempDir;

  @Test
  void happyPath_parsesNetworkCorrectly() throws Exception {
    Path file = write("generateur(G1,50).",
        "generateur(G2,40).",
        "maison(M1,BASSE).",
        "maison(M2,NORMAL).",
        "connexion(G1,M1).",
        "connexion(M2,G2).");

    Reseau reseau = FileAlgorithms.chargerReseau(file.toString());

    assertEquals(2, reseau.getGenerateurs().size());
    assertEquals(2, reseau.getMaisons().size());
    assertTrue(reseau.connexionExiste("M1", "G1"));
    assertTrue(reseau.connexionExiste("M2", "G2"));
  }

  @Test
  void syntax_missingTrailingDot_throws() throws Exception {
    Path file = write("generateur(G1,50)");
    assertThrows(FileSyntaxException.class, () -> FileAlgorithms.chargerReseau(file.toString()));
  }

  @Test
  void syntax_badKeyword_throws() throws Exception {
    Path file = write("bad(G1,10).", "maison(M1,BASSE).", "connexion(G1,M1).");
    assertThrows(FileSyntaxException.class, () -> FileAlgorithms.chargerReseau(file.toString()));
  }

  @Test
  void syntax_wrongOrderConnexionBeforeMaisons_throws() throws Exception {
    Path file = write("generateur(G1,50).", "connexion(G1,M1).", "maison(M1,BASSE).");
    assertThrows(FileSyntaxException.class, () -> FileAlgorithms.chargerReseau(file.toString()));
  }

  @Test
  void syntax_wrongArity_throws() throws Exception {
    Path file = write("maison(M1).", "generateur(G1,50).", "connexion(G1,M1).");
    assertThrows(FileSyntaxException.class, () -> FileAlgorithms.chargerReseau(file.toString()));
  }

  @Test
  void syntax_negativeCapacity_throws() throws Exception {
    Path file = write("generateur(G1,-5).", "maison(M1,BASSE).", "connexion(G1,M1).");
    assertThrows(FileSyntaxException.class, () -> FileAlgorithms.chargerReseau(file.toString()));
  }

  @Test
  void syntax_invalidConsumption_throws() throws Exception {
    Path file = write("generateur(G1,50).", "maison(M1,LOW).", "connexion(G1,M1).");
    assertThrows(FileSyntaxException.class, () -> FileAlgorithms.chargerReseau(file.toString()));
  }

  @Test
  void logical_connectionToUndefinedNode_throws() throws Exception {
    Path file = write("generateur(G1,50).", "connexion(G1,M1).");
    assertThrows(FileSyntaxException.class, () -> FileAlgorithms.chargerReseau(file.toString()));
  }

  @Test
  void logical_duplicateConnectionSameHouse_throws() throws Exception {
    Path file = write("generateur(G1,50).", "generateur(G2,50).", "maison(M1,BASSE).",
        "connexion(G1,M1).", "connexion(G2,M1).");
    assertThrows(FileSyntaxException.class, () -> FileAlgorithms.chargerReseau(file.toString()));
  }

  @Test
  void globalValidation_unconnectedHouse_throwsReseauInvalide() throws Exception {
    Path file = write("generateur(G1,50).", "maison(M1,BASSE).");
    assertThrows(ReseauInvalideException.class, () -> FileAlgorithms.chargerReseau(file.toString()));
  }

  @Test
  void capacityOverflowDuringParse_throwsFileSyntaxException() throws Exception {
    Path file = write("generateur(G1,10).", "maison(M1,FORTE).", "connexion(G1,M1).");
    FileSyntaxException ex = assertThrows(FileSyntaxException.class,
        () -> FileAlgorithms.chargerReseau(file.toString()));
    assertTrue(ex.getMessage().contains("CAPACITE GLOBALE DEPASSEE"));
  }

  @Test
  void veryLongNames_areParsed() throws Exception {
    String longName = "MaisonTresLongue1234567890ABCDE";
    Path file = write("generateur(G1,50).", "maison(" + longName + ",BASSE).", "connexion(G1," + longName + ").");
    Reseau reseau = FileAlgorithms.chargerReseau(file.toString());
    assertTrue(reseau.maisonExiste(longName));
    assertTrue(reseau.connexionExiste(longName, "G1"));
  }

  @Test
  void sauvegardePuisChargement_preserveStructure() throws Exception {
    Reseau reseau = new Reseau();
    reseau.setLambda(7); // ne doit pas etre sauvegarde
    reseau.addOrUpdateGenerateur(new main.Model.Generateur("G1", 60));
    reseau.addOrUpdateGenerateur(new main.Model.Generateur("G2", 40));
    reseau.addOrUpdateMaison(new main.Model.Maison("M1", Consommation.BASSE));
    reseau.addOrUpdateMaison(new main.Model.Maison("M2", Consommation.FORTE));
    reseau.setConnexionUnique("M1", "G1");
    reseau.setConnexionUnique("M2", "G2");

    Path saveFile = tempDir.resolve("roundtrip.txt");
    FileAlgorithms.sauvegarderReseau(reseau, saveFile.toString());

    Reseau reloaded = FileAlgorithms.chargerReseau(saveFile.toString());

    assertEquals(2, reloaded.getGenerateurs().size());
    assertEquals(2, reloaded.getMaisons().size());
    assertTrue(reloaded.connexionExiste("M1", "G1"));
    assertTrue(reloaded.connexionExiste("M2", "G2"));
    assertEquals(main.Model.Constants.LAMBDA, reloaded.getLambda());
  }

  private Path write(String... lines) throws IOException {
    Path file = tempDir.resolve("reseau-" + System.nanoTime() + ".txt");
    Files.write(file, List.of(lines));
    return file;
  }
}
