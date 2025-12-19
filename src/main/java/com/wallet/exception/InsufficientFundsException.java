package com.wallet.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(UUID walletId, BigDecimal balance, BigDecimal amount) {
        super(String.format("Insufficient funds in wallet %s. Current balance: %s, Requested amount: %s", 
            walletId, balance, amount));
    }
}

