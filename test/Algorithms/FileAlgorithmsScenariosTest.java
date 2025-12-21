package test.Algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Algorithms.FileAlgorithms;
import Exceptions.InvalidFileExtensionException;
import Exceptions.InvalidNameException;
import Exceptions.ReseauInvalideException;
import Exceptions.ReseauInvalideSyntaxException;
import Model.Consommation;
import Model.Generateur;
import Model.Maison;
import Model.Reseau;

class FileAlgorithmsScenariosTest {

  @TempDir
  Path tempDir;

  @Test
  void load_emptyFile_throwsAggregatedSyntaxError() throws Exception {
    Path file = tempDir.resolve("empty.txt");
    Files.writeString(file, "\n\n");

    ReseauInvalideSyntaxException ex = assertThrows(ReseauInvalideSyntaxException.class,
        () -> FileAlgorithms.chargerReseau(file.toString()));
    assertTrue(ex.getMessage().contains("FICHIER INVALIDE"));
  }

  @Test
  void load_generateurOnly_throwsAggregatedSyntaxErrors() throws Exception {
    Path file = write("generateur(G1,50).", "generateur(G2,30).");

    ReseauInvalideSyntaxException ex = assertThrows(ReseauInvalideSyntaxException.class,
        () -> FileAlgorithms.chargerReseau(file.toString()));
    assertTrue(ex.getMessage().contains("AUCUNE MAISON DEFINIE"));
    assertTrue(ex.getMessage().contains("AUCUNE CONNEXION"));
  }

  @Test
  void load_maisonOnly_throwsAggregatedSyntaxErrors() throws Exception {
    Path file = write("maison(M1,BASSE).", "maison(M2,NORMAL).");

    ReseauInvalideSyntaxException ex = assertThrows(ReseauInvalideSyntaxException.class,
        () -> FileAlgorithms.chargerReseau(file.toString()));
    assertTrue(ex.getMessage().contains("AUCUN GENERATEUR DEFINI"));
    assertTrue(ex.getMessage().contains("AUCUNE CONNEXION"));
  }

  @Test
  void load_generateursEtMaisonsSansConnexions_throwsAggregatedSyntaxErrors() throws Exception {
    Path file = write("generateur(G1,50).", "maison(M1,BASSE).", "maison(M2,NORMAL).");

    ReseauInvalideSyntaxException ex = assertThrows(ReseauInvalideSyntaxException.class,
        () -> FileAlgorithms.chargerReseau(file.toString()));
    assertTrue(ex.getMessage().contains("AUCUNE CONNEXION"));
  }

  @Test
  void load_connexionWithUndefinedNode_throwsAggregatedSyntaxErrors() throws Exception {
    Path file = write("generateur(G1,50).", "maison(M1,BASSE).", "connexion(G1,M2).");

    ReseauInvalideSyntaxException ex = assertThrows(ReseauInvalideSyntaxException.class,
        () -> FileAlgorithms.chargerReseau(file.toString()));
    assertTrue(ex.getMessage().contains("Connexion impossible"));
  }

  @Test
  void load_connexionsEtMaisonsOnly_throwsAggregatedSyntaxErrors() throws Exception {
    // Maisons + connexions, mais aucun generateur defini.
    Path file = write(
        "maison(M1,BASSE).",
        "connexion(G1,M1).");

    ReseauInvalideSyntaxException ex = assertThrows(ReseauInvalideSyntaxException.class,
        () -> FileAlgorithms.chargerReseau(file.toString()));
    assertTrue(ex.getMessage().contains("AUCUN GENERATEUR DEFINI") || ex.getMessage().contains("Aucun generateur"));
    assertTrue(ex.getMessage().contains("Connexion impossible")
        || ex.getMessage().contains("Les maisons doivent")
        || ex.getMessage().contains("Les connexions doivent"));
  }

  @Test
  void load_connexionsEtGenerateursOnly_throwsAggregatedSyntaxErrors() throws Exception {
    // Generateurs + connexions, mais aucune maison definie.
    Path file = write(
        "generateur(G1,50).",
        "connexion(G1,M1).");

    ReseauInvalideSyntaxException ex = assertThrows(ReseauInvalideSyntaxException.class,
        () -> FileAlgorithms.chargerReseau(file.toString()));
    assertTrue(ex.getMessage().contains("AUCUNE MAISON DEFINIE") || ex.getMessage().contains("Aucune maison"));
    assertTrue(ex.getMessage().contains("Connexion impossible")
        || ex.getMessage().contains("Les connexions doivent"));
  }

  @Test
  void load_allowsConnexionMaisonGenerateurEitherOrder() throws Exception {
    Path file = write("generateur(G1,50).", "generateur(G2,40).", "maison(M1,BASSE).", "maison(M2,NORMAL).",
        "connexion(G1,M1).", "connexion(M2,G2).");

    Reseau reseau = FileAlgorithms.chargerReseau(file.toString());
    assertTrue(reseau.connexionExiste("M1", "G1"));
    assertTrue(reseau.connexionExiste("M2", "G2"));
  }

  @Test
  void load_redeclaredGenerateurAndMaison_collectsWarningsOnlyOnSuccess() throws Exception {
    Path file = write(
        "generateur(G1,50).",
        "generateur(G1,60).",
        "maison(M1,BASSE).",
        "maison(M1,FORTE).",
        "connexion(G1,M1).");

    Reseau reseau = FileAlgorithms.chargerReseau(file.toString());
    assertEquals(60, reseau.getGenerateurs().get("G1").getCapaciteMax());
    assertEquals(Consommation.FORTE, reseau.getMaisons().get("M1").getConsommation());

    List<String> warnings = FileAlgorithms.consumeLastLoadWarnings();
    assertEquals(2, warnings.size());
    assertTrue(warnings.get(0).contains("redeclare"));

    // Consume resets
    assertTrue(FileAlgorithms.consumeLastLoadWarnings().isEmpty());
  }

  @Test
  void load_duplicateAndReverseConnexion_collectsWarningsAndIgnoresDuplicates() throws Exception {
    Path file = write(
        "generateur(G1,50).",
        "maison(M1,BASSE).",
        "connexion(G1,M1).",
        "connexion(G1,M1).",
        "connexion(M1,G1).");

    Reseau reseau = FileAlgorithms.chargerReseau(file.toString());
    assertTrue(reseau.connexionExiste("M1", "G1"));

    List<String> warnings = FileAlgorithms.consumeLastLoadWarnings();
    // The first connexion is valid; next two are duplicates (same + reversed)
    assertEquals(2, warnings.size());
    assertTrue(warnings.get(0).contains("connexion"));
    assertTrue(warnings.get(0).contains("redeclare"));
  }

  @Test
  void load_maisonConnectedToTwoGenerateurs_throwsAggregatedSyntaxError() throws Exception {
    Path file = write(
        "generateur(G1,50).",
        "generateur(G2,50).",
        "maison(M1,BASSE).",
        "connexion(G1,M1).",
        "connexion(G2,M1).");

    ReseauInvalideSyntaxException ex = assertThrows(ReseauInvalideSyntaxException.class,
        () -> FileAlgorithms.chargerReseau(file.toString()));
    assertTrue(ex.getMessage().contains("deja connectee") || ex.getMessage().contains("Impossible de la connecter"));
  }

  @Test
  void load_globalValidationFailure_doesNotExposeWarnings() throws Exception {
    // Parsing OK (warning), mais validation globale KO (maison deconnectee)
    Path file = write(
        "generateur(G1,50).",
        "generateur(G1,60).", // warning redeclare
        "maison(M1,BASSE).",
        "maison(M2,BASSE).",
        "connexion(G1,M1)." // M2 reste deconnectee => ReseauInvalideException
    );

    assertThrows(ReseauInvalideException.class, () -> FileAlgorithms.chargerReseau(file.toString()));
    assertTrue(FileAlgorithms.consumeLastLoadWarnings().isEmpty());
  }

  @Test
  void load_failure_doesNotExposeWarnings() throws Exception {
    Path file = write(
        "generateur(G1,50).",
        "maison(M1,LOW).", // invalid conso -> aggregated syntax error
        "connexion(G1,M1).");

    assertThrows(ReseauInvalideSyntaxException.class, () -> FileAlgorithms.chargerReseau(file.toString()));
    assertTrue(FileAlgorithms.consumeLastLoadWarnings().isEmpty());
  }

  @Test
  void load_nameCanStartWithNumber() throws Exception {
    Path file = write(
        "generateur(G1,50).",
        "maison(1M,BASSE).",
        "connexion(G1,1M).");

    Reseau reseau = FileAlgorithms.chargerReseau(file.toString());
    assertTrue(reseau.maisonExiste("1M"));
    assertTrue(reseau.connexionExiste("1M", "G1"));
  }

  @Test
  void load_rejectsNonTxtExtension() throws Exception {
    assertThrows(InvalidFileExtensionException.class, () -> FileAlgorithms.chargerReseau("reseau.csv"));
  }

  @Test
  void load_appendsTxtWhenMissing_andLoadsFile() throws Exception {
    Path file = write(
        "generateur(G1,50).",
        "maison(M1,BASSE).",
        "connexion(G1,M1).");

    // Remove the .txt suffix before calling chargerReseau
    String fullPath = file.toString();
    assertTrue(fullPath.endsWith(".txt"));
    String withoutExt = fullPath.substring(0, fullPath.length() - 4);

    Reseau reseau = FileAlgorithms.chargerReseau(withoutExt);
    assertTrue(reseau.connexionExiste("M1", "G1"));
  }

  @Test
  void save_appendsTxtWhenMissing_andWritesFile() throws Exception {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateGenerateur(new Generateur("G1", 60));
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.BASSE));
    reseau.setConnexionUnique("M1", "G1");

    Path base = tempDir.resolve("out");
    FileAlgorithms.sauvegarderReseau(reseau, base.toString());

    Path outTxt = tempDir.resolve("out.txt");
    assertTrue(Files.exists(outTxt));
    assertFalse(Files.exists(base));
  }

  @Test
  void save_rejectsNonTxtExtension() throws Exception {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateGenerateur(new Generateur("G1", 60));
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.BASSE));
    reseau.setConnexionUnique("M1", "G1");

    assertThrows(InvalidFileExtensionException.class,
        () -> FileAlgorithms.sauvegarderReseau(reseau, tempDir.resolve("out.csv").toString()));
  }

  @Test
  void save_rejectsNamesWithSpaces() throws Exception {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateGenerateur(new Generateur("G 1", 60));
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.BASSE));
    reseau.setConnexionUnique("M1", "G 1");

    assertThrows(InvalidNameException.class,
        () -> FileAlgorithms.sauvegarderReseau(reseau, tempDir.resolve("out").toString()));
  }

  @Test
  void save_toDirectoryPath_throwsIOException() throws Exception {
    Reseau reseau = new Reseau();
    reseau.addOrUpdateGenerateur(new Generateur("G1", 60));
    reseau.addOrUpdateMaison(new Maison("M1", Consommation.BASSE));
    reseau.setConnexionUnique("M1", "G1");

    Path dir = tempDir.resolve("target.txt");

    // Create an actual directory at a path that already ends with .txt
    // (so FileAlgorithms.normalizeTxtPathForSave won't change it).
    Files.createDirectory(dir);

    // Try writing to a directory path (should fail as a file target)
    assertThrows(IOException.class, () -> FileAlgorithms.sauvegarderReseau(reseau, dir.toString()));
  }

  @Test
  void multiSyntaxErrors_areAggregatedIntoOneException() throws Exception {
    Path file = write(
        "generateur(G1,0).",
        "maison(M1,LOW).",
        "connexion(G1,M2).");

    ReseauInvalideSyntaxException ex = assertThrows(ReseauInvalideSyntaxException.class,
        () -> FileAlgorithms.chargerReseau(file.toString()));

    assertTrue(ex.getErreurs().size() >= 3);

    // Expect multiple error lines in message
    String msg = ex.getMessage();
    assertTrue(msg.contains("erreur(s)"));
    assertTrue(msg.contains("ligne 1") || msg.contains("ligne 2") || msg.contains("ligne 3")
        || msg.contains("Ligne 1") || msg.contains("Ligne 2") || msg.contains("Ligne 3"));
    assertTrue(msg.contains("La capacite doit etre un entier strictement positif")
        || msg.contains("La capacite doit etre"));
    assertTrue(msg.contains("Type de consommation invalide") || msg.contains("consommation"));
    assertTrue(msg.contains("Connexion impossible"));
  }

  private Path write(String... lines) throws IOException {
    Path file = tempDir.resolve("reseau-" + System.nanoTime() + ".txt");
    Files.write(file, List.of(lines));
    return file;
  }
}
