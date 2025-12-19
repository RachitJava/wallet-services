package com.wallet.service;

import com.wallet.dto.WalletOperationRequest;
import com.wallet.dto.WalletResponse;
import com.wallet.exception.InsufficientFundsException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.model.OperationType;
import com.wallet.model.Wallet;
import com.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {
    
    private final WalletRepository walletRepository;
    
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(
        retryFor = {org.springframework.dao.OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public WalletResponse processOperation(WalletOperationRequest request) {
        log.debug("Processing {} operation for wallet: {}, amount: {}", 
            request.getOperationType(), request.getWalletId(), request.getAmount());
        
        UUID walletId = request.getWalletId();
        
        // Get wallet with pessimistic lock to prevent concurrent modifications
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseGet(() -> {
                    // Create wallet if it doesn't exist
                    log.info("Creating new wallet: {}", walletId);
                    Wallet newWallet = new Wallet(walletId);
                    return walletRepository.save(newWallet);
                });
        
        BigDecimal currentBalance = wallet.getBalance();
        BigDecimal amount = request.getAmount();
        
        // Process operation based on type
        if (request.getOperationType() == OperationType.DEPOSIT) {
            wallet.setBalance(currentBalance.add(amount));
            log.debug("Deposited {} to wallet {}. New balance: {}", 
                amount, walletId, wallet.getBalance());
        } else if (request.getOperationType() == OperationType.WITHDRAW) {
            // Check if sufficient funds available
            if (currentBalance.compareTo(amount) < 0) {
                log.warn("Insufficient funds for wallet {}. Balance: {}, Requested: {}", 
                    walletId, currentBalance, amount);
                throw new InsufficientFundsException(walletId, currentBalance, amount);
            }
            wallet.setBalance(currentBalance.subtract(amount));
            log.debug("Withdrew {} from wallet {}. New balance: {}", 
                amount, walletId, wallet.getBalance());
        }
        
        // Save the updated wallet
        Wallet savedWallet = walletRepository.save(wallet);
        
        log.info("Operation {} completed successfully for wallet {}. Final balance: {}", 
            request.getOperationType(), walletId, savedWallet.getBalance());
        
        return new WalletResponse(savedWallet.getWalletId(), savedWallet.getBalance());
    }
    
    @Transactional(readOnly = true)
    public WalletResponse getWalletBalance(UUID walletId) {
        log.debug("Fetching balance for wallet: {}", walletId);
        
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> {
                    log.warn("Wallet not found: {}", walletId);
                    return new WalletNotFoundException(walletId);
                });
        
        log.debug("Balance for wallet {}: {}", walletId, wallet.getBalance());
        return new WalletResponse(wallet.getWalletId(), wallet.getBalance());
    }
}

