package com.example.photoprintapplication1.service;

import com.example.photoprintapplication1.binary.BinaryExportType;
import com.example.photoprintapplication1.binary.BinaryMultipartPayload;
import com.example.photoprintapplication1.binary.BinaryProtocolWriter;
import com.example.photoprintapplication1.binary.BinarySerializationUtils;
import com.example.photoprintapplication1.exception.BadRequestException;
import com.example.photoprintapplication1.models.MalwareSignature;
import com.example.photoprintapplication1.models.SignatureStatus;
import com.example.photoprintapplication1.repository.MalwareSignatureRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class BinarySignatureExportService {

    private final MalwareSignatureRepository signatureRepository;
    private final SignatureService signatureService;
    private final String manifestMagic;
    private final String dataMagic;
    private final int protocolVersion;

    public BinarySignatureExportService(
            MalwareSignatureRepository signatureRepository,
            SignatureService signatureService,
            @Value("${binary.magic.surname:BOCHK}") String studentSurname,
            @Value("${binary.protocol.version:1}") int protocolVersion
    ) {
        this.signatureRepository = signatureRepository;
        this.signatureService = signatureService;
        this.protocolVersion = protocolVersion;

        String normalizedSurname = (studentSurname == null || studentSurname.isBlank())
                ? "BOCHK"
                : studentSurname.trim().toUpperCase();

        this.manifestMagic = "MF-" + normalizedSurname;
        this.dataMagic = "DB-" + normalizedSurname;
    }

    @Transactional(readOnly = true)
    public BinaryMultipartPayload exportFull() {
        List<MalwareSignature> signatures = signatureRepository.findByStatusOrderByUpdatedAtDesc(SignatureStatus.ACTUAL);
        return buildPayload(signatures, BinaryExportType.FULL, null);
    }

    @Transactional(readOnly = true)
    public BinaryMultipartPayload exportIncrement(Instant since) {
        if (since == null) {
            throw new BadRequestException("Parameter 'since' is required");
        }

        List<MalwareSignature> signatures = signatureRepository.findByUpdatedAtAfterOrderByUpdatedAtDesc(since);
        return buildPayload(signatures, BinaryExportType.INCREMENT, since);
    }

    @Transactional(readOnly = true)
    public BinaryMultipartPayload exportByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("ids must not be empty");
        }

        List<MalwareSignature> signatures = signatureRepository.findByIdIn(ids)
                .stream()
                .sorted(Comparator.comparing(MalwareSignature::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return buildPayload(signatures, BinaryExportType.BY_IDS, null);
    }

    private BinaryMultipartPayload buildPayload(List<MalwareSignature> signatures, BinaryExportType exportType, Instant since) {
        List<SerializedDataRecord> serializedRecords = new ArrayList<>();

        long currentOffset = 0;
        for (MalwareSignature signature : signatures) {
            byte[] recordBytes = serializeDataRecord(signature);
            byte[] recordSignatureBytes = decodeRecordSignature(signature.getDigitalSignatureBase64());

            serializedRecords.add(new SerializedDataRecord(
                    signature,
                    currentOffset,
                    recordBytes.length,
                    recordBytes,
                    recordSignatureBytes
            ));

            currentOffset += recordBytes.length;
        }

        byte[] dataFile = buildDataFile(serializedRecords);
        byte[] manifestFile = buildManifestFile(serializedRecords, dataFile, exportType, since);

        return new BinaryMultipartPayload(manifestFile, dataFile);
    }

    private byte[] buildDataFile(List<SerializedDataRecord> serializedRecords) {
        BinaryProtocolWriter writer = new BinaryProtocolWriter();

        writer.writeUtf8String(dataMagic)
                .writeU16(protocolVersion)
                .writeU32(serializedRecords.size());

        for (SerializedDataRecord record : serializedRecords) {
            writer.writeBytes(record.recordBytes());
        }

        return writer.toByteArray();
    }

    private byte[] buildManifestFile(
            List<SerializedDataRecord> serializedRecords,
            byte[] dataFile,
            BinaryExportType exportType,
            Instant since
    ) {
        byte[] dataSha256 = sha256(dataFile);

        BinaryProtocolWriter unsignedManifestWriter = new BinaryProtocolWriter();
        unsignedManifestWriter.writeUtf8String(manifestMagic)
                .writeU16(protocolVersion)
                .writeU8(exportType.getCode())
                .writeI64(Instant.now().toEpochMilli())
                .writeI64(since == null ? -1L : since.toEpochMilli())
                .writeU32(serializedRecords.size())
                .writeBytes(dataSha256);

        for (SerializedDataRecord record : serializedRecords) {
            unsignedManifestWriter
                    .writeUuid(record.signature().getId())
                    .writeU8(statusToCode(record.signature().getStatus()))
                    .writeI64(record.signature().getUpdatedAt() == null ? -1L : record.signature().getUpdatedAt().toEpochMilli())
                    .writeU32(record.dataOffset())
                    .writeU32(record.dataLength())
                    .writeU32(record.recordSignatureBytes().length)
                    .writeBytes(record.recordSignatureBytes());
        }

        byte[] unsignedManifest = unsignedManifestWriter.toByteArray();
        byte[] manifestSignatureBytes = signManifest(unsignedManifest);

        BinaryProtocolWriter finalManifestWriter = new BinaryProtocolWriter();
        finalManifestWriter.writeBytes(unsignedManifest)
                .writeU32(manifestSignatureBytes.length)
                .writeBytes(manifestSignatureBytes);

        return finalManifestWriter.toByteArray();
    }

    private byte[] signManifest(byte[] unsignedManifest) {
        try {
            return signatureService.signBytes(unsignedManifest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign manifest", e);
        }
    }

    private byte[] serializeDataRecord(MalwareSignature signature) {
        BinaryProtocolWriter writer = new BinaryProtocolWriter();

        writer.writeUtf8String(signature.getThreatName())
                .writeByteArray(BinarySerializationUtils.hexToBytes(signature.getFirstBytesHex()))
                .writeByteArray(BinarySerializationUtils.hexToBytes(signature.getRemainderHashHex()))
                .writeI64(signature.getRemainderLength())
                .writeUtf8String(signature.getFileType())
                .writeI64(signature.getOffsetStart())
                .writeI64(signature.getOffsetEnd());

        return writer.toByteArray();
    }

    private byte[] decodeRecordSignature(String signatureBase64) {
        if (signatureBase64 == null || signatureBase64.isBlank()) {
            return new byte[0];
        }

        try {
            return Base64.getDecoder().decode(signatureBase64);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Stored digitalSignatureBase64 is invalid for one of signatures", e);
        }
    }

    private int statusToCode(SignatureStatus status) {
        if (status == SignatureStatus.DELETED) {
            return 2;
        }
        return 1;
    }

    private byte[] sha256(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate data SHA-256", e);
        }
    }

    private record SerializedDataRecord(
            MalwareSignature signature,
            long dataOffset,
            int dataLength,
            byte[] recordBytes,
            byte[] recordSignatureBytes
    ) {
    }
}
