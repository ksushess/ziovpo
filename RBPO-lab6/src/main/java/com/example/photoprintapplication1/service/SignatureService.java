package com.example.photoprintapplication1.service;

import com.example.photoprintapplication1.dto.Ticket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

@Service
public class SignatureService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SignatureService(
            @Value("${signature.private-key-path}") String privateKeyPath,
            @Value("${signature.keystore-password}") String keystorePassword,
            @Value("${signature.alias}") String alias) throws Exception {
        // Загрузка ключей из keystore (того же, что для HTTPS, или отдельного)
        java.security.KeyStore keystore = java.security.KeyStore.getInstance("PKCS12");
        try (java.io.FileInputStream fis = new java.io.FileInputStream(privateKeyPath)) {
            keystore.load(fis, keystorePassword.toCharArray());
            java.security.KeyStore.PrivateKeyEntry entry = (java.security.KeyStore.PrivateKeyEntry) keystore.getEntry(alias, new java.security.KeyStore.PasswordProtection(keystorePassword.toCharArray()));
            this.privateKey = entry.getPrivateKey();
            this.publicKey = entry.getCertificate().getPublicKey();
        }
    }

    // Подпись Ticket
    public String signTicket(Ticket ticket) throws Exception {
        String ticketJson = objectMapper.writeValueAsString(ticket);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(ticketJson.getBytes(StandardCharsets.UTF_8));

        byte[] sigBytes = signature.sign();
        return Base64.getEncoder().encodeToString(sigBytes);
    }

    // Проверка подписи (для тестов или клиента)
    public boolean verifyTicket(Ticket ticket, String signatureBase64) throws Exception {
        String ticketJson = objectMapper.writeValueAsString(ticket);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(ticketJson.getBytes(StandardCharsets.UTF_8));

        byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
        return signature.verify(sigBytes);
    }
}