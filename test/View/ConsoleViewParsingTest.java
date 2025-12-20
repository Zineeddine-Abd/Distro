// package test.View;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertThrows;

// import org.junit.jupiter.api.Test;

// import main.Model.Consommation;
// import main.View.ConsoleView;

// class ConsoleViewParsingTest {

// @Test
// void parsePositiveInt_acceptsPositive() {
// assertEquals(5, ConsoleView.parsePositiveInt("5"));
// }

// @Test
// void parsePositiveInt_rejectsZeroOrNegative() {
// assertThrows(IllegalArgumentException.class, () ->
// ConsoleView.parsePositiveInt("0"));
// assertThrows(IllegalArgumentException.class, () ->
// ConsoleView.parsePositiveInt("-3"));
// }

// @Test
// void parseLambda_usesPositiveRule() {
// assertThrows(IllegalArgumentException.class, () ->
// ConsoleView.parseLambda("-1"));
// }

// @Test
// void parseConsommationToken_acceptsKnownAndRejectsUnknown() {
// assertEquals(Consommation.BASSE,
// ConsoleView.parseConsommationToken("basse"));
// assertThrows(IllegalArgumentException.class, () ->
// ConsoleView.parseConsommationToken("LOW"));
// }

// @Test
// void parseMenuChoice_validatesRange() {
// assertEquals(2, ConsoleView.parseMenuChoice("2", 3));
// assertThrows(IllegalArgumentException.class, () ->
// ConsoleView.parseMenuChoice("0", 3));
// assertThrows(IllegalArgumentException.class, () ->
// ConsoleView.parseMenuChoice("5", 3));
// }
// }
