package com.bank.ledgerservice.controller;

import com.bank.ledgerservice.dto.CommitHoldRequest;
import com.bank.ledgerservice.service.HoldService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/holds")
public class HoldController {

    private final HoldService holdService;

    public HoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    @PostMapping("/commit")
    public ResponseEntity<?> commitHold(@RequestBody CommitHoldRequest request) {
        holdService.commitHold(request.getHoldId(), request.getDestinationAccountId());
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Hold committed successfully"
        ));
    }
}