package com.airguard.server.security;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES Security Service — אחראי על:
 * 1. הצפנה/פענוח AES-128 CBC
 * 2. הוספת timestamp לכל חבילה
 * 3. בדיקת "טריות" — חלון זמן של 2 שניות
 */
@Service
public class AesEncryptionService {

    // מפתח AES 128-bit (16 בתים) — ב-production יש להעביר ל-application.properties
    private static final String SECRET_KEY = "AirGuard1234Key!"; // 16 תווים = 128 bit
    private static final String INIT_VECTOR = "AirGuardIV123456"; // 16 תווים = 128 bit IV

    // חלון הזמן המקסימלי (שניות) לפני שהודעה נחשבת ישנה
    private static final long MAX_TIMESTAMP_AGE_SECONDS = 2;

    /**
     * מצפין טקסט + מוסיף timestamp אוטומטית.
     * הפורמט: Base64( IV + timestamp(8 bytes) + encrypted_payload )
     */
    public String encryptWithTimestamp(String plainText) throws Exception {
        long timestamp = Instant.now().getEpochSecond();

        // בונים את ה-payload: timestamp (8 בתים) + הטקסט עצמו
        byte[] textBytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] timestampBytes = longToBytes(timestamp);
        byte[] payload = new byte[8 + textBytes.length];
        System.arraycopy(timestampBytes, 0, payload, 0, 8);
        System.arraycopy(textBytes, 0, payload, 8, textBytes.length);

        // מצפינים
        IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
        byte[] encrypted = cipher.doFinal(payload);

        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * מפענח הודעה מוצפנת + בודק timestamp.
     * זורק חריגה אם:
     * - הפענוח נכשל (מקור לא מורשה)
     * - ה-timestamp ישן מ-2 שניות (ניסיון replay attack)
     *
     * @return הטקסט המפוענח ללא ה-timestamp
     */
    public String decryptAndValidate(String encryptedBase64) throws Exception {
        byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);

        IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);

        byte[] decrypted;
        try {
            decrypted = cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new SecurityException("AES Decryption failed — unauthorized source or corrupted data.");
        }

        // מוציאים את ה-timestamp (8 בתים ראשונים)
        long messageTimestamp = bytesToLong(Arrays.copyOfRange(decrypted, 0, 8));
        long now = Instant.now().getEpochSecond();
        long age = now - messageTimestamp;

        if (age > MAX_TIMESTAMP_AGE_SECONDS || age < 0) {
            throw new SecurityException(
                    "Timestamp validation failed — message age: " + age + "s (max: "
                            + MAX_TIMESTAMP_AGE_SECONDS + "s). Possible replay attack.");
        }

        // מחזירים רק את ה-payload האמיתי (ללא ה-timestamp)
        return new String(Arrays.copyOfRange(decrypted, 8, decrypted.length), StandardCharsets.UTF_8);
    }

    // ================= Helper Methods =================

    private byte[] longToBytes(long value) {
        return ByteBuffer.allocate(8).putLong(value).array();
    }

    private long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }
}