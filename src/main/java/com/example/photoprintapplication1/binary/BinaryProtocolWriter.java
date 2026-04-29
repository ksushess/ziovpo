package com.example.photoprintapplication1.binary;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class BinaryProtocolWriter {

    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

    public BinaryProtocolWriter writeU8(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("u8 out of range: " + value);
        }

        try {
            out.writeByte(value);
            return this;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write u8", e);
        }
    }

    public BinaryProtocolWriter writeU16(int value) {
        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException("u16 out of range: " + value);
        }

        try {
            out.writeShort(value);
            return this;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write u16", e);
        }
    }

    public BinaryProtocolWriter writeU32(long value) {
        if (value < 0 || value > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("u32 out of range: " + value);
        }

        try {
            out.writeInt((int) value);
            return this;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write u32", e);
        }
    }

    public BinaryProtocolWriter writeI64(long value) {
        try {
            out.writeLong(value);
            return this;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write i64", e);
        }
    }

    public BinaryProtocolWriter writeUuid(UUID value) {
        Objects.requireNonNull(value, "uuid is required");
        return writeI64(value.getMostSignificantBits())
                .writeI64(value.getLeastSignificantBits());
    }

    public BinaryProtocolWriter writeBytes(byte[] value) {
        try {
            out.write(value);
            return this;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write bytes", e);
        }
    }

    public BinaryProtocolWriter writeUtf8String(String value) {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        return writeU32(bytes.length).writeBytes(bytes);
    }

    public BinaryProtocolWriter writeByteArray(byte[] value) {
        byte[] bytes = value == null ? new byte[0] : value;
        return writeU32(bytes.length).writeBytes(bytes);
    }

    public byte[] toByteArray() {
        return byteArrayOutputStream.toByteArray();
    }

    public int size() {
        return byteArrayOutputStream.size();
    }
}
