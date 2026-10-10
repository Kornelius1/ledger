package com.bank.ledgerservice.repository;

import com.bank.ledgerservice.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {}