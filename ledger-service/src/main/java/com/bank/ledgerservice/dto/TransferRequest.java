package com.bank.ledgerservice.dto;

import java.math.BigDecimal;

public record TransferRequest(
    Long sourceAccountId,
    Long destinationAccountId,
    BigDecimal amount,
    String idempotencyKey
) {}