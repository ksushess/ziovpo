package com.example.photoprintapplication1;

import com.example.photoprintapplication1.binary.BinaryMultipartPayload;
import com.example.photoprintapplication1.models.MalwareSignature;
import com.example.photoprintapplication1.models.SignatureStatus;
import com.example.photoprintapplication1.repository.MalwareSignatureRepository;
import com.example.photoprintapplication1.service.BinarySignatureExportService;
import com.example.photoprintapplication1.service.SignatureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BinarySignatureExportServiceTest {

    @Test
    void exportFullShouldBuildMultipartPayloadWithManifestAndData() throws Exception {
        MalwareSignatureRepository repository = mock(MalwareSignatureRepository.class);
        SignatureService signatureService = mock(SignatureService.class);

        MalwareSignature signature = new MalwareSignature();
        signature.setId(UUID.randomUUID());
        signature.setThreatName("Trojan.Demo");
        signature.setFirstBytesHex("4D5A9000");
        signature.setRemainderHashHex("A1B2C3D4");
        signature.setRemainderLength(42L);
        signature.setFileType("EXE");
        signature.setOffsetStart(0L);
        signature.setOffsetEnd(64L);
        signature.setUpdatedAt(Instant.parse("2026-04-29T12:00:00Z"));
        signature.setStatus(SignatureStatus.ACTUAL);
        signature.setDigitalSignatureBase64(Base64.getEncoder().encodeToString(new byte[]{9, 8, 7}));

        when(repository.findByStatusOrderByUpdatedAtDesc(SignatureStatus.ACTUAL)).thenReturn(List.of(signature));
        when(signatureService.signBytes(any())).thenReturn(new byte[]{1, 2, 3, 4});

        BinarySignatureExportService service = new BinarySignatureExportService(repository, signatureService, "TEST", 1);

        BinaryMultipartPayload payload = service.exportFull();

        assertThat(payload.manifestBytes()).isNotEmpty();
        assertThat(payload.dataBytes()).isNotEmpty();

        DataInputStream dataReader = new DataInputStream(new ByteArrayInputStream(payload.dataBytes()));
        assertThat(readString(dataReader)).isEqualTo("DB-TEST");
        assertThat(dataReader.readUnsignedShort()).isEqualTo(1);
        assertThat(readU32(dataReader)).isEqualTo(1);
        assertThat(readString(dataReader)).isEqualTo("Trojan.Demo");
        assertThat(readByteArray(dataReader)).containsExactly((byte) 0x4D, (byte) 0x5A, (byte) 0x90, (byte) 0x00);
        assertThat(readByteArray(dataReader)).containsExactly((byte) 0xA1, (byte) 0xB2, (byte) 0xC3, (byte) 0xD4);
        assertThat(dataReader.readLong()).isEqualTo(42L);
        assertThat(readString(dataReader)).isEqualTo("EXE");
        assertThat(dataReader.readLong()).isEqualTo(0L);
        assertThat(dataReader.readLong()).isEqualTo(64L);

        DataInputStream manifestReader = new DataInputStream(new ByteArrayInputStream(payload.manifestBytes()));
        assertThat(readString(manifestReader)).isEqualTo("MF-TEST");
        assertThat(manifestReader.readUnsignedShort()).isEqualTo(1);
        assertThat(manifestReader.readUnsignedByte()).isEqualTo(1);

        long generatedAt = manifestReader.readLong();
        assertThat(generatedAt).isGreaterThan(0L);

        assertThat(manifestReader.readLong()).isEqualTo(-1L);
        assertThat(readU32(manifestReader)).isEqualTo(1);

        byte[] sha256FromManifest = new byte[32];
        manifestReader.readFully(sha256FromManifest);
        assertThat(sha256FromManifest).isEqualTo(MessageDigest.getInstance("SHA-256").digest(payload.dataBytes()));

        assertThat(readUuid(manifestReader)).isEqualTo(signature.getId());
        assertThat(manifestReader.readUnsignedByte()).isEqualTo(1);
        assertThat(manifestReader.readLong()).isEqualTo(signature.getUpdatedAt().toEpochMilli());
        assertThat(readU32(manifestReader)).isEqualTo(0L);

        long dataLength = readU32(manifestReader);
        assertThat(dataLength).isGreaterThan(0L);

        long recordSignatureLength = readU32(manifestReader);
        assertThat(recordSignatureLength).isEqualTo(3L);
        byte[] recordSignatureBytes = new byte[(int) recordSignatureLength];
        manifestReader.readFully(recordSignatureBytes);
        assertThat(recordSignatureBytes).containsExactly(9, 8, 7);

        long manifestSignatureLength = readU32(manifestReader);
        assertThat(manifestSignatureLength).isEqualTo(4L);
        byte[] manifestSignature = new byte[(int) manifestSignatureLength];
        manifestReader.readFully(manifestSignature);
        assertThat(manifestSignature).containsExactly(1, 2, 3, 4);

        verify(signatureService).signBytes(any());
    }

    @Test
    void exportIncrementShouldRequireSince() {
        MalwareSignatureRepository repository = mock(MalwareSignatureRepository.class);
        SignatureService signatureService = mock(SignatureService.class);

        BinarySignatureExportService service = new BinarySignatureExportService(repository, signatureService, "TEST", 1);

        assertThatThrownBy(() -> service.exportIncrement(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("since");
    }

    @Test
    void exportIncrementShouldEncodeDeletedStatusAsTwo() throws Exception {
        MalwareSignatureRepository repository = mock(MalwareSignatureRepository.class);
        SignatureService signatureService = mock(SignatureService.class);

        MalwareSignature signature = new MalwareSignature();
        signature.setId(UUID.randomUUID());
        signature.setThreatName("Deleted.Demo");
        signature.setFirstBytesHex("AA55");
        signature.setRemainderHashHex("BB66");
        signature.setRemainderLength(1L);
        signature.setFileType("BIN");
        signature.setOffsetStart(1L);
        signature.setOffsetEnd(2L);
        signature.setUpdatedAt(Instant.parse("2026-04-29T12:00:00Z"));
        signature.setStatus(SignatureStatus.DELETED);
        signature.setDigitalSignatureBase64(Base64.getEncoder().encodeToString(new byte[]{5}));

        Instant since = Instant.parse("2026-04-29T00:00:00Z");
        when(repository.findByUpdatedAtAfterOrderByUpdatedAtDesc(since)).thenReturn(List.of(signature));
        when(signatureService.signBytes(any())).thenReturn(new byte[]{3});

        BinarySignatureExportService service = new BinarySignatureExportService(repository, signatureService, "TEST", 1);

        BinaryMultipartPayload payload = service.exportIncrement(since);

        DataInputStream manifestReader = new DataInputStream(new ByteArrayInputStream(payload.manifestBytes()));
        readString(manifestReader);
        manifestReader.readUnsignedShort();
        assertThat(manifestReader.readUnsignedByte()).isEqualTo(2);
        manifestReader.readLong();
        assertThat(manifestReader.readLong()).isEqualTo(since.toEpochMilli());
        readU32(manifestReader);
        byte[] skipSha = new byte[32];
        manifestReader.readFully(skipSha);
        readUuid(manifestReader);

        assertThat(manifestReader.readUnsignedByte()).isEqualTo(2);
    }

    private static long readU32(DataInputStream in) throws Exception {
        return Integer.toUnsignedLong(in.readInt());
    }

    private static String readString(DataInputStream in) throws Exception {
        int length = (int) readU32(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] readByteArray(DataInputStream in) throws Exception {
        int length = (int) readU32(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return bytes;
    }

    private static UUID readUuid(DataInputStream in) throws Exception {
        long msb = in.readLong();
        long lsb = in.readLong();
        return new UUID(msb, lsb);
    }
}
