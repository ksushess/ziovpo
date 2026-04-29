package com.example.photoprintapplication1.binary;

import java.util.HexFormat;

public final class BinarySerializationUtils {

    private BinarySerializationUtils() {
    }

    public static byte[] hexToBytes(String hex) {
        String normalized = (hex == null ? "" : hex.trim());
        if (normalized.isEmpty()) {
            return new byte[0];
        }
        return HexFormat.of().parseHex(normalized);
    }
}
