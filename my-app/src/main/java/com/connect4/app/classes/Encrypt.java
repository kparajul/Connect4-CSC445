package com.connect4.app.classes;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class Encrypt {
    private SecretKey aesKey;
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 256;

    // Constructor for new random key
    public Encrypt() throws Exception {
        this.aesKey = generateKey();
    }

    // Constructor from existing key (e.g., received from Raft log)
    public Encrypt(byte[] keyBytes) {
        this.aesKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    // Encrypts plaintext and returns Base64 encoded IV + ciphertext
    public String encrypt(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        byte[] ivBytes = new byte[16];
        new SecureRandom().nextBytes(ivBytes); // generate random IV
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes());

        // Prepend IV and encode
        byte[] ivAndEncrypted = new byte[ivBytes.length + encrypted.length];
        System.arraycopy(ivBytes, 0, ivAndEncrypted, 0, ivBytes.length);
        System.arraycopy(encrypted, 0, ivAndEncrypted, ivBytes.length, encrypted.length);
        return Base64.getEncoder().encodeToString(ivAndEncrypted);
    }

    // Decrypts Base64 encoded IV + ciphertext
    public String decrypt(String base64Ciphertext) throws Exception {
        byte[] ivAndEncrypted = Base64.getDecoder().decode(base64Ciphertext);
        byte[] ivBytes = new byte[16];
        byte[] ciphertext = new byte[ivAndEncrypted.length - 16];

        System.arraycopy(ivAndEncrypted, 0, ivBytes, 0, 16);
        System.arraycopy(ivAndEncrypted, 16, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, iv);
        byte[] decrypted = cipher.doFinal(ciphertext);
        return new String(decrypted);
    }

    // Returns the raw AES key for replication via Raft
    public byte[] getKeyBytes() {
        return aesKey.getEncoded();
    }

    // Generates a new AES-256 key
    private SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE);
        return keyGen.generateKey();
    }

}