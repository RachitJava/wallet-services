package com.wallet.service;

import com.wallet.dto.WalletOperationRequest;
import com.wallet.dto.WalletResponse;
import com.wallet.exception.InsufficientFundsException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.model.OperationType;
import com.wallet.model.Wallet;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {
    
    @Mock
    private WalletRepository walletRepository;
    
    @InjectMocks
    private WalletService walletService;
    
    @Test
    void processOperation_Deposit_NewWallet_Success() {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, OperationType.DEPOSIT, new BigDecimal("100.00")
        );
        
        Wallet newWallet = new Wallet(walletId);
        Wallet savedWallet = new Wallet(walletId);
        savedWallet.setBalance(new BigDecimal("100.00"));
        
        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet, savedWallet);
        
        WalletResponse response = walletService.processOperation(request);
        
        assertThat(response.getWalletId()).isEqualTo(walletId);
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        verify(walletRepository, times(2)).save(any(Wallet.class));
    }
    
    @Test
    void processOperation_Deposit_ExistingWallet_Success() {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, OperationType.DEPOSIT, new BigDecimal("50.00")
        );
        
        Wallet existingWallet = new Wallet(walletId);
        existingWallet.setBalance(new BigDecimal("100.00"));
        
        Wallet updatedWallet = new Wallet(walletId);
        updatedWallet.setBalance(new BigDecimal("150.00"));
        
        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(updatedWallet);
        
        WalletResponse response = walletService.processOperation(request);
        
        assertThat(response.getWalletId()).isEqualTo(walletId);
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }
    
    @Test
    void processOperation_Withdraw_Success() {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, OperationType.WITHDRAW, new BigDecimal("30.00")
        );
        
        Wallet existingWallet = new Wallet(walletId);
        existingWallet.setBalance(new BigDecimal("100.00"));
        
        Wallet updatedWallet = new Wallet(walletId);
        updatedWallet.setBalance(new BigDecimal("70.00"));
        
        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(updatedWallet);
        
        WalletResponse response = walletService.processOperation(request);
        
        assertThat(response.getWalletId()).isEqualTo(walletId);
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
    }
    
    @Test
    void processOperation_Withdraw_InsufficientFunds_ThrowsException() {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, OperationType.WITHDRAW, new BigDecimal("150.00")
        );
        
        Wallet existingWallet = new Wallet(walletId);
        existingWallet.setBalance(new BigDecimal("100.00"));
        
        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(existingWallet));
        
        assertThatThrownBy(() -> walletService.processOperation(request))
            .isInstanceOf(InsufficientFundsException.class)
            .hasMessageContaining("Insufficient funds");
        
        verify(walletRepository, never()).save(any(Wallet.class));
    }
    
    @Test
    void processOperation_Withdraw_ExactBalance_Success() {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, OperationType.WITHDRAW, new BigDecimal("100.00")
        );
        
        Wallet existingWallet = new Wallet(walletId);
        existingWallet.setBalance(new BigDecimal("100.00"));
        
        Wallet updatedWallet = new Wallet(walletId);
        updatedWallet.setBalance(BigDecimal.ZERO);
        
        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(updatedWallet);
        
        WalletResponse response = walletService.processOperation(request);
        
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
    
    @Test
    void getWalletBalance_Success() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId);
        wallet.setBalance(new BigDecimal("100.00"));
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        
        WalletResponse response = walletService.getWalletBalance(walletId);
        
        assertThat(response.getWalletId()).isEqualTo(walletId);
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }
    
    @Test
    void getWalletBalance_WalletNotFound_ThrowsException() {
        UUID walletId = UUID.randomUUID();
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> walletService.getWalletBalance(walletId))
            .isInstanceOf(WalletNotFoundException.class)
            .hasMessageContaining("Wallet not found");
    }
}

