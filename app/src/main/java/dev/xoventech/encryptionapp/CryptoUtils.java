package dev.xoventech.encryptionapp;

import android.util.Base64;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 10000;
    private static final int KEY_LENGTH = 256;

    public static String encrypt(String input, String secretKey) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        SecretKey aesKey = deriveKey(secretKey);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);

        byte[] encrypted = cipher.doFinal(input.getBytes("UTF-8"));
        byte[] combined = new byte[IV_LENGTH + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
        System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

        return Base64.encodeToString(combined, Base64.DEFAULT);
    }

    public static String decrypt(String encryptedInput, String secretKey) throws Exception {
        byte[] combined = Base64.decode(encryptedInput, Base64.DEFAULT);
        if (combined.length < IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data");
        }
        byte[] iv = new byte[IV_LENGTH];
        byte[] ciphertext = new byte[combined.length - IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
        System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        SecretKey aesKey = deriveKey(secretKey);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);

        byte[] decrypted = cipher.doFinal(ciphertext);
        return new String(decrypted, "UTF-8");
    }

    private static SecretKey deriveKey(String secretKey) throws Exception {
        byte[] salt = "fixed_salt_123456".getBytes("UTF-8");
        PBEKeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        byte[] key = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }
}