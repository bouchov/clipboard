package com.bouchov.tools;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Alexandre Y. Bouchov
 * Date: 30.06.2021
 * Time: 17:20
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class Bytes {
    public static final byte ZERO = (byte) 0;
    public static final byte ONE = (byte) 1;
    public static final byte TWO = (byte) 2;

    public static final String START_ZIP = "\\x1f8b08";
    public static final String START_OBJ = "\\xaced00";

    private Bytes() {
    }

    public static byte[] toBytes(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        return baos.toByteArray();
    }

    public static byte[] toZBytes(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStream zstream = new GZIPOutputStream(baos)) {
            zstream.write(toBytes(obj));
        }
        return baos.toByteArray();
    }

    public static <O> O fromBytes(byte[] bytes)
            throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (O) ois.readObject();
    }

    public static <O> O fromHex(String hex)
            throws IOException, ClassNotFoundException {
        if (hex.startsWith(START_OBJ)) {
            return fromBytes(hexToBytes(hex.substring(2)));
        } else if (hex.startsWith(START_ZIP)) {
            try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(hexToBytes(hex.substring(2))))) {
                try (ObjectInputStream ois = new ObjectInputStream(gz)) {
                    return (O) ois.readObject();
                }
            }
        }
        throw new IllegalArgumentException("invalid string: " + hex.substring(0,
                Math.min(START_ZIP.length(), hex.length())));
    }

    private static final char[] HEX_DIGITS = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    public static String bytesToHex(byte[] bytes) {
        if (bytes.length == 0) {
            return "";
        }
        return bytesToHex(bytes, 0, bytes.length);
    }

    public static String bytesToHex(byte[] bytes, int off, int len) {
        if (bytes == null) {
            return "null";
        }
        int length = bytes.length;
        if (len == 0 || length == 0 || length <= off) {
            return "";
        }

        StringBuilder sb = new StringBuilder(length * 2);
        for (int i = off, n = Math.min(off + len, length); i < n; i++) {
            sb.append(HEX_DIGITS[(bytes[i] >> 4) & 0xF]);
            sb.append(HEX_DIGITS[bytes[i] & 0xF]);
        }
        return sb.toString();
    }

    public static String byteToHex(byte b) {
        return String.valueOf(HEX_DIGITS[(b >> 4) & 0xF]) + HEX_DIGITS[b & 0xF];
    }

    public static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException(hex);
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0, len = hex.length(); i < len; i += 2) {
            char c1 = hex.charAt(i);
            byte b1;
            if (c1 >= '0' && c1 <= '9') {
                b1 = (byte) ((c1 - '0') << 4);
            } else if (c1 >= 'a' && c1 <= 'f') {
                b1 = (byte) ((c1 - 'a' + 10) << 4);
            } else {
                throw new IllegalArgumentException("'" + c1  + "'" + " pos " + i + " in '" + hex + "'");
            }
            char c2 = hex.charAt(i + 1);
            byte b2;
            if (c2 >= '0' && c2 <= '9') {
                b2 = (byte) ((c2 - '0') & 0x0F);
            } else if (c2 >= 'a' && c2 <= 'f') {
                b2 = (byte) ((c2 - 'a' + 10) & 0x0F);
            } else {
                throw new IllegalArgumentException(hex);
            }
            bytes[i / 2] = (byte) (b1 | b2);
        }
        return bytes;
    }

    public static String toPostgresByteaString(byte[] data) {
        return toPostgresByteaString3(data);
    }

    public static String toPostgresByteaString0(byte[] data) {
        if (data == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(data.length);
        for (byte b : data) {
            char c = (char) (b & 0x00FF);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c <= 31 || c >= 127) {
                sb.append('\\').append(String.format("%03o", (int) c));
            } else {
                sb.append(c);
            }
        }
        //postgres: decode(arg, 'escape');
        return sb.toString();
    }

    public static String toPostgresByteaString1(byte[] data) {
        if (data == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(data.length);
        for (byte b : data) {
            char c = (char) (b & 0x00FF);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c <= 31 || c >= 127) {
                sb.append('\\');
                String octal = Integer.toOctalString((int) c);
                int l = octal.length();
                if (l == 1) {
                    sb.append("00");
                } else if (l == 2) {
                    sb.append("0");
                }
                sb.append(octal);
            } else {
                sb.append(c);
            }
        }
        //postgres: decode(arg, 'escape');
        return sb.toString();
    }

    public static String toPostgresByteaString2(byte[] data) {
        if (data == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(data.length);
        for (byte b : data) {
            char c = (char) (b & 0x00FF);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c <= 31 || c >= 127) {
                sb.append('\\');
                String octal = Integer.toOctalString((int) c);
                switch (octal.length()) {
                    case 1 : sb.append("00"); break;
                    case 2 : sb.append("0"); break;
                }
                sb.append(octal);
            } else {
                sb.append(c);
            }
        }
        //postgres: decode(arg, 'escape');
        return sb.toString();
    }

    public static String toPostgresByteaString3(byte[] data) {
        if (data == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(data.length);
        for (byte b : data) {
            char c = (char) (b & 0x00FF);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c <= 31 || c >= 127) {
                sb.append('\\');
                toOctalString(sb, (int) c);
            } else {
                sb.append(c);
            }
        }
        //postgres: decode(arg, 'escape');
        return sb.toString();
    }

    private static void toOctalString(StringBuilder sb, int i) {
        int radix = 8;
        int mask = radix - 1;
        sb.append(digits[(i >>> 6) & mask]);
        sb.append(digits[(i >>> 3) & mask]);
        sb.append(digits[i & mask]);
//        int radix = 8;
//        int mask = radix - 1;
//        char c3 = digits[i & mask];
//        i >>>= 3;
//        char c2 = digits[i & mask];
//        i >>>= 3;
//        char c1 = digits[i & mask];
//        sb.append(c1);
//        sb.append(c2);
//        sb.append(c3);
    }

    private final static char[] digits = {'0' , '1' , '2' , '3' , '4' , '5' , '6' , '7'};
}