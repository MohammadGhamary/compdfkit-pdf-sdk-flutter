package com.compdfkit.flutter.compdfkit_flutter.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import android.util.Base64;


public class TextDecryptionUtils {

    private static final byte[] psBytes = {
            99, 81, 57, 65, 126, 62, 56, 43,
            75, 58, 79, 127, 108, 59, 106, 122
    };

    private static String getPassword() {
        byte[] decodedBytes = new byte[psBytes.length];

        for (int i = 0; i < psBytes.length; i++) {
            decodedBytes[i] = (byte)(psBytes[i] ^ 8);
        }

        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    // Flutter-like utf8ToHex
    private static String utf8ToHex(String input) {
        StringBuilder sb = new StringBuilder();
        for (byte b : input.getBytes(StandardCharsets.UTF_8)) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Add zero padding to make 32 hex chars (i.e. 16 bytes after utf8)
    private static String utf8ToHex(String input, boolean havePadding) {
        String hex = utf8ToHex(input);
        if (havePadding) {
            while (hex.length() < 32) hex += "0";
            return hex.substring(0, 32);
        }
        return hex;
    }

    private static byte[] toUtf8BytesOfHex(String s) {
        return utf8ToHex(s).getBytes(StandardCharsets.UTF_8);
    }

    public static String decrypt(String base64CipherText) throws Exception {
        try {
            String key = getPassword();
            byte[] keyBytes = toUtf8BytesOfHex(key); // Same as utf8.encode(utf8ToHex(key)) in Flutter
            byte[] ivBytes = utf8ToHex(key.substring(0, 4), true).getBytes(StandardCharsets.UTF_8);

            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);

            byte[] cipherBytes = Base64.decode(base64CipherText, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(cipherBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8).trim();
        }catch (Exception e){
            return "";
        }
    }
}
