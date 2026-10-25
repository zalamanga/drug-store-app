package app.core;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DomainTest {
  @Test
  void saltedHashesDifferAndVerifyOnlyCorrectPassword() {
    String first = Passwords.hash(Fixture.PASSWORD.toCharArray()),
        second = Passwords.hash(Fixture.PASSWORD.toCharArray());
    assertNotEquals(first, second);
    assertTrue(Passwords.verify(Fixture.PASSWORD.toCharArray(), first));
    assertFalse(Passwords.verify("incorrect".toCharArray(), first));
    assertFalse(Passwords.verify(Fixture.PASSWORD.toCharArray(), "old-plaintext"));
    assertFalse(
        Passwords.verify(Fixture.PASSWORD.toCharArray(), "pbkdf2-sha256$999999999$bad$bad"));
  }

  @Test
  void shortPasswordsAndNegativeQuantitiesAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> Passwords.hash("short".toCharArray()));
    assertThrows(
        IllegalArgumentException.class, () -> new Cart.Line("OBT", "Demo", BigDecimal.ONE, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new Cart.Line("OBT", "Demo", new BigDecimal("-1"), 1));
  }

  @Test
  void cartMergesRowsAndUsesExactDecimalMoney() {
    Cart cart = new Cart();
    cart.add(new Cart.Line("OBT", "Demo", new BigDecimal("1250.50"), 2));
    cart.add(new Cart.Line("OBT", "Demo", new BigDecimal("1250.50"), 1));
    assertEquals(1, cart.lines().size());
    assertEquals(3, cart.lines().get(0).quantity());
    assertEquals(new BigDecimal("3751.50"), cart.total());
    cart.remove("OBT");
    assertTrue(cart.lines().isEmpty());
  }

  @Test
  void formRejectsInvalidPhoneAndMissingFields() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Entity.CUSTOMER.validate(
                Map.of(
                    "id_customer",
                    "CUS1",
                    "nama_customer",
                    "Demo",
                    "telp_customer",
                    "abc",
                    "status_customer",
                    "Aktif"),
                false));
    assertThrows(IllegalArgumentException.class, () -> Entity.OBAT.validate(Map.of(), false));
  }
}
