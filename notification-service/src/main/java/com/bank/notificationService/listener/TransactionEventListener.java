package com.bank.notificationService.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventListener.class);

    @KafkaListener(topics = "transaction-settlement", groupId = "notification-group")
    public void handleTransactionEvent(String message) {
        log.info("============== AUDIT & NOTIFICATION ==============");
        log.info("Menerima event settlement dari Kafka: {}", message);
        log.info("Tindakan 1: Menyimpan data ke Immutable Audit Log...");
        log.info("Tindakan 2: Mengirimkan Push Notification / Email ke HP Nasabah...");
        log.info("==================================================");
    }
}