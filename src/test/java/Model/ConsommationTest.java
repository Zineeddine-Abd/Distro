package test.java.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import main.Model.Consommation;

class ConsommationTest {

  @ParameterizedTest
  @CsvSource({
      "BASSE, BASSE",
      "basse, BASSE",
      "BasSe, BASSE",
      "NORMAL, NORMAL",
      "Normal, NORMAL",
      "FORTE, FORTE",
      "fOrTe, FORTE"
  })
  void fromString_parsesKnownValues(String input, Consommation expected) {
    assertEquals(expected, Consommation.fromString(input));
  }

  @ParameterizedTest
  @ValueSource(strings = { "", " ", "low", "HAUTE", "123" })
  void fromString_unknownValueThrows(String input) {
    assertThrows(IllegalArgumentException.class, () -> Consommation.fromString(input));
  }
}
