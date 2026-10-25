package app.core;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {
  private static final int ITERATIONS = 210_000;

  private Passwords() {}

  public static String hash(char[] password) {
    if (password.length < 12 || password.length > 128) {
      throw new IllegalArgumentException("Password baru harus 12 sampai 128 karakter.");
    }
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return "pbkdf2-sha256$"
        + ITERATIONS
        + "$"
        + encode(salt)
        + "$"
        + encode(derive(password, salt, ITERATIONS));
  }

  public static boolean verify(char[] password, String encoded) {
    if (encoded == null || password.length > 128) return false;
    try {
      String[] pieces = encoded.split("\\$");
      if (pieces.length != 4 || !pieces[0].equals("pbkdf2-sha256")) return false;
      int rounds = Integer.parseInt(pieces[1]);
      if (rounds < 100_000 || rounds > 1_000_000) return false;
      byte[] salt = Base64.getDecoder().decode(pieces[2]);
      byte[] expected = Base64.getDecoder().decode(pieces[3]);
      return salt.length == 16
          && expected.length == 32
          && MessageDigest.isEqual(expected, derive(password, salt, rounds));
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  private static String encode(byte[] value) {
    return Base64.getEncoder().encodeToString(value);
  }

  private static byte[] derive(char[] password, byte[] salt, int rounds) {
    PBEKeySpec spec = new PBEKeySpec(password, salt, rounds, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("PBKDF2 tidak tersedia.", ex);
    } finally {
      spec.clearPassword();
    }
  }
}
