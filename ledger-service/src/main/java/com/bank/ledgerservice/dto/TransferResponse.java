package com.bank.ledgerservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponse(
    String status,
    UUID transactionId,
    String idempotencyKey,
    Long sourceAccountId,
    Long destinationAccountId,
    BigDecimal amount
) {}