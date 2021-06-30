package com.bouchov.clipboard.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import static com.bouchov.tools.Bytes.bytesToHex;
import static com.bouchov.tools.Bytes.hexToBytes;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 17:12
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class Password {
    private static final Random rnd = new Random();
    private static final int VERSION = 1;

    private static final int DIGEST_SIZE = 44;
    private static final byte[] SALT = new byte[]{
            (byte)0xc9, (byte)0xc8, (byte)0xd5, (byte)0xca,
            (byte)0xd7, (byte)0xc1, (byte)0xcd, (byte)0xd7,
            (byte)0xd3, (byte)0xc5, (byte)0xcd, (byte)0xce,
            (byte)0xc1, (byte)0xc8, (byte)0xd5, (byte)0xca,
    };
    private static final int[] ITERATIONS_BY_VERSION = new int[] {0, 512};
    private static final String PBKDF2_WITH_HMAC_SHA512 = "PBKDF2WithHmacSHA512";
    private static final int KEY_LENGTH = 512;

    public static final Password UNMATCHABLE = new Password(null, null, 0) {
        @Override
        public int version() {
            return 0;
        }
    };

    protected final String salt;
    protected final String hash;
    @JsonIgnore
    protected final int version;

    private Password(String salt, String hash, int version) {
        this.salt = salt;
        this.hash = hash;
        this.version = version;
    }

    public static Password create(String password) {
        if (password == null) {
            return UNMATCHABLE;
        }
        String digest = digest(password);
        return createForDigest(digest);
    }

    public static Password createForDigest(String digest) {
        byte[] salt = new byte[16];
        rnd.nextBytes(salt);
        String hashHex = generateSecret(digest, salt, VERSION);
        return new Password(bytesToHex(salt), hashHex, VERSION);
    }

    public static Password toPassword(String json) {
        return toPassword(VERSION, json);
    }

    public String toJson()
            throws JsonProcessingException {
        if (this == UNMATCHABLE) {
            return null;
        } else {
            return new ObjectMapper().writeValueAsString(this);
        }
    }

    private static Password toPassword(int version, String json) {
        if (version == 0 || json == null) {
            return UNMATCHABLE;
        }
        try {
            TypeReference<HashMap<String, String>> typeRef = new TypeReference<>() {};
            Map<String, String> map = new ObjectMapper().readValue(json, typeRef);

            return new Password(map.get("salt"), map.get("hash"), version);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public int version() {
        return version;
    }

    public static boolean isEqual(Password password, String word) {
        if (password == null || password == UNMATCHABLE || word == null) {
            return false;
        }
        return password.isEqual(word);
    }

    protected boolean isEqual(String word) {
        return Objects.equals(hash,
                generateSecret(digest(word), hexToBytes(salt), version));
    }

    public String getSalt() {
        return salt;
    }

    public String getHash() {
        return hash;
    }

    private static String generateSecret(String digest, byte[] salt, int version) {
        try {
            PBEKeySpec keySpec = new PBEKeySpec(digest.toCharArray(), salt, ITERATIONS_BY_VERSION[version], KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_WITH_HMAC_SHA512);
            byte[] hash = skf.generateSecret(keySpec).getEncoded();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unknown algorithm: " + PBKDF2_WITH_HMAC_SHA512, e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("PBEKey specification is inappropriate for secret-key factory", e);
        }
    }

    private static String digest(String password) {
        String digest;
        try {
            digest = digest("SHA-256", DIGEST_SIZE, password);
        } catch (Exception e) {
            throw new RuntimeException("error creating digest", e);
        }
        return digest;
    }

    private static String digest(String algorithm, int padTo, String input) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("unknown digest algorithm: " + algorithm, e);
        }
        md.update(input.getBytes(StandardCharsets.UTF_8));
        md.update(SALT);

        String base64 = Base64.getEncoder().encodeToString(md.digest());
        StringBuilder result = new StringBuilder(base64);
        while (result.length() < padTo) {
            result.insert(0, '0');
        }
        if (result.length() > padTo) {
            return result.substring(0, padTo);
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Password password = (Password) o;
        return version == password.version &&
                Objects.equals(salt, password.salt) &&
                Objects.equals(hash, password.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(salt, hash, version);
    }

    @Override
    public String toString() {
        return "[Password" +
                " salt=" + (salt == null ? null : '\'' + salt + '\'') +
                ", hash=" + (hash == null ? null : '\'' + hash + '\'') +
                ", version=" + version +
                ']';
    }
}
