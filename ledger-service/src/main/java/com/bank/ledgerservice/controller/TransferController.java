package com.bank.ledgerservice.controller;

import com.bank.ledgerservice.dto.TransferRequest;
import com.bank.ledgerservice.dto.TransferResponse;
import com.bank.ledgerservice.service.TransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> executeTransfer(@RequestBody TransferRequest request) {
        return ResponseEntity.ok(transferService.processTransfer(request));
    }
}