package com.example.photoprintapplication1.service;

import com.example.photoprintapplication1.binary.BinaryMultipartPayload;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
public class MultipartMixedResponseFactory {

    public ResponseEntity<MultiValueMap<String, Object>> build(BinaryMultipartPayload payload) {
        LinkedMultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("manifest", buildBinaryPart("manifest.bin", payload.manifestBytes()));
        parts.add("data", buildBinaryPart("data.bin", payload.dataBytes()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("multipart/mixed"));

        return new ResponseEntity<>(parts, headers, HttpStatus.OK);
    }

    private HttpEntity<ByteArrayResource> buildBinaryPart(String filename, byte[] bytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(bytes.length);

        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        return new HttpEntity<>(resource, headers);
    }
}
