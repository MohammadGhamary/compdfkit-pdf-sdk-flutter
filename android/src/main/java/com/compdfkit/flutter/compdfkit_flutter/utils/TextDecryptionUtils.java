package com.compdfkit.flutter.compdfkit_flutter.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import android.util.Base64;

public class TextDecryptionUtils {

    private static final byte[] psBytes = new byte[]{
            99, 81, 57, 65, 126, 62, 56, 43,
            75, 58, 79, 127, 108, 59, 106, 122
    };

    public static String encryptTextWithDefaultPassword(String text) {
        return encryptText(getPassword(), text);
    }

    public static String encryptText(String key, String text) {
        try {
            String keyString = utf8ToHex(key, false);
            byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);

            String ivString = utf8ToHex(key.substring(0, 4), true);
            byte[] ivBytes = ivString.getBytes(StandardCharsets.UTF_8);

            if (keyBytes.length != 32) {
                throw new IllegalArgumentException("Key must be 256 bits (32 bytes).");
            }

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encryptedBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);

        } catch (Exception e) {
            return null;
        }
    }

    public static String decryptTextWithDefaultPassword(String cipherText) {
        try {
            String key = getPassword();
            String keyString = utf8ToHex(key, false);
            String ivString = utf8ToHex(key.substring(0, 4), true);

            byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);
            byte[] ivBytes = ivString.getBytes(StandardCharsets.UTF_8);
            byte[] cipherBytes = Base64.decode(cipherText, Base64.DEFAULT);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(cipherBytes);
            ByteArrayInputStream byteStream = new ByteArrayInputStream(decryptedBytes);
            BufferedReader reader = new BufferedReader(new InputStreamReader(byteStream, StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            return result.toString();

        } catch (Exception e) {
            return null;
        }
    }

    private static String utf8ToHex(String str, boolean havePadding) {
        StringBuilder hexResult = new StringBuilder();

        for (char ch : str.toCharArray()) {
            byte[] utf8Bytes = String.valueOf(ch).getBytes(StandardCharsets.UTF_8);
            for (byte b : utf8Bytes) {
                String hex = String.format("%02x", b);
                if (havePadding) {
                    hex = "00" + hex;
                }
                hexResult.append(hex);
            }
        }

        return hexResult.toString();
    }

    private static String getPassword() {
        byte[] bytes = new byte[psBytes.length];
        for (int i = 0; i < psBytes.length; i++) {
            bytes[i] = (byte) (psBytes[i] ^ 8);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
