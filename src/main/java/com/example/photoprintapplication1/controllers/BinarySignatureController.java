package com.example.photoprintapplication1.controllers;

import com.example.photoprintapplication1.binary.BinaryMultipartPayload;
import com.example.photoprintapplication1.dto.MalwareSignatureIdsRequest;
import com.example.photoprintapplication1.exception.BadRequestException;
import com.example.photoprintapplication1.service.BinarySignatureExportService;
import com.example.photoprintapplication1.service.MultipartMixedResponseFactory;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/binary/signatures")
public class BinarySignatureController {

    private final BinarySignatureExportService binarySignatureExportService;
    private final MultipartMixedResponseFactory multipartMixedResponseFactory;

    public BinarySignatureController(
            BinarySignatureExportService binarySignatureExportService,
            MultipartMixedResponseFactory multipartMixedResponseFactory
    ) {
        this.binarySignatureExportService = binarySignatureExportService;
        this.multipartMixedResponseFactory = multipartMixedResponseFactory;
    }

    @GetMapping(value = "/full", produces = MediaType.MULTIPART_MIXED_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<MultiValueMap<String, Object>> getFull() {
        BinaryMultipartPayload payload = binarySignatureExportService.exportFull();
        return multipartMixedResponseFactory.build(payload);
    }

    @GetMapping(value = "/increment", produces = MediaType.MULTIPART_MIXED_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<MultiValueMap<String, Object>> getIncrement(@RequestParam("since") String since) {
        Instant parsedSince;
        try {
            parsedSince = Instant.parse(since);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid 'since' format. Use ISO-8601, for example: 2026-04-29T10:15:30Z");
        }

        BinaryMultipartPayload payload = binarySignatureExportService.exportIncrement(parsedSince);
        return multipartMixedResponseFactory.build(payload);
    }

    @PostMapping(value = "/by-ids", produces = MediaType.MULTIPART_MIXED_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<MultiValueMap<String, Object>> getByIds(@RequestBody @Valid MalwareSignatureIdsRequest request) {
        BinaryMultipartPayload payload = binarySignatureExportService.exportByIds(request.getIds());
        return multipartMixedResponseFactory.build(payload);
    }
}
