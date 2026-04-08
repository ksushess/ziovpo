package com.example.photoprintapplication1.service;

import com.example.photoprintapplication1.dto.Ticket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Base64;

@Service
public class SignatureService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SignatureService(
            @Value("${signature.private-key-path}") Resource keystoreResource,
            @Value("${signature.keystore-password}") String keystorePassword,
            @Value("${signature.alias}") String alias) {

        try {
            // Загрузка keystore
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            try (var inputStream = keystoreResource.getInputStream()) {
                keystore.load(inputStream, keystorePassword.toCharArray());
            }

            // Получаем приватный ключ
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keystore.getEntry(
                    alias,
                    new KeyStore.PasswordProtection(keystorePassword.toCharArray())
            );

            this.privateKey = entry.getPrivateKey();

            // Получаем сертификат и публичный ключ
            Certificate cert = keystore.getCertificate(alias);
            this.publicKey = cert.getPublicKey();

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SignatureService", e);
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

    // Проверка подписи
    public boolean verifyTicket(Ticket ticket, String signatureBase64) throws Exception {
        String ticketJson = objectMapper.writeValueAsString(ticket);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(ticketJson.getBytes(StandardCharsets.UTF_8));

        byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
        return signature.verify(sigBytes);
    }
}