package com.example.photoprintapplication1.service;

import com.example.photoprintapplication1.dto.Ticket;
import com.example.photoprintapplication1.dto.MalwareSignatureSigningPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;

@Service
public class SignatureService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SignatureService(
            @Value("${signature.keystore.path}") Resource keystoreResource,
            @Value("${signature.keystore.password}") String keystorePassword,
            @Value("${signature.key.alias}") String alias,
            @Value("${signature.public-key:}") String publicKeyBase64) {

        try {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            try (var is = keystoreResource.getInputStream()) {
                keystore.load(is, keystorePassword.toCharArray());
            }

            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keystore.getEntry(
                    alias, new KeyStore.PasswordProtection(keystorePassword.toCharArray())
            );

            this.privateKey = entry.getPrivateKey();

            if (publicKeyBase64 != null && !publicKeyBase64.isBlank()) {
                this.publicKey = tryReadPublicKeyOrFallback(publicKeyBase64, keystore, alias);
            } else {
                Certificate cert = keystore.getCertificate(alias);
                this.publicKey = cert.getPublicKey();
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SignatureService", e);
        }
    }

    private PublicKey tryReadPublicKeyOrFallback(String publicKeyValue, KeyStore keystore, String alias) throws Exception {
        try {
            return readPublicKeyFromProperty(publicKeyValue);
        } catch (Exception ex) {
            Certificate cert = keystore.getCertificate(alias);
            if (cert == null) {
                throw ex;
            }
            return cert.getPublicKey();
        }
    }

    private PublicKey readPublicKeyFromProperty(String publicKeyValue) throws Exception {
        String normalized = publicKeyValue
                .trim()
                .replace("\\\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\\\r", "")
                .replace("\r", "");

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        Certificate cert;

        if (normalized.contains("-----BEGIN CERTIFICATE-----")) {
            cert = certFactory.generateCertificate(
                    new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8))
            );
        } else {
            String cleanedKey = normalized.replaceAll("\\s", "");
            byte[] certBytes = Base64.getDecoder().decode(cleanedKey);
            cert = certFactory.generateCertificate(new ByteArrayInputStream(certBytes));
        }

        return cert.getPublicKey();
    }

    public String signTicket(Ticket ticket) throws Exception {
        return signObject(ticket);
    }

    public String signMalwareSignature(MalwareSignatureSigningPayload payload) throws Exception {
        return signObject(payload);
    }

    public boolean verifyTicket(Ticket ticket, String signatureBase64) throws Exception {
        String json = objectMapper.writeValueAsString(ticket);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(json.getBytes(StandardCharsets.UTF_8));

        byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
        return signature.verify(sigBytes);
    }

    private String signObject(Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(json.getBytes(StandardCharsets.UTF_8));
        byte[] signed = signature.sign();
        return Base64.getEncoder().encodeToString(signed);
    }
}
