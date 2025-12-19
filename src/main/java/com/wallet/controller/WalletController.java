package com.wallet.controller;

import com.wallet.dto.WalletOperationRequest;
import com.wallet.dto.WalletResponse;
import com.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class WalletController {
    
    private final WalletService walletService;
    
    @PostMapping("/wallet")
    public ResponseEntity<WalletResponse> processWalletOperation(
            @Valid @RequestBody WalletOperationRequest request) {
        log.info("Received wallet operation request: {}", request);
        
        WalletResponse response = walletService.processOperation(request);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<WalletResponse> getWalletBalance(@PathVariable UUID walletId) {
        log.info("Received request to get balance for wallet: {}", walletId);
        
        WalletResponse response = walletService.getWalletBalance(walletId);
        
        return ResponseEntity.ok(response);
    }
}

