package com.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.dto.WalletOperationRequest;
import com.wallet.dto.WalletResponse;
import com.wallet.exception.InsufficientFundsException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.model.OperationType;
import com.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class WalletControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private WalletService walletService;
    
    @Test
    void processWalletOperation_Deposit_Success() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, OperationType.DEPOSIT, new BigDecimal("100.00")
        );
        WalletResponse response = new WalletResponse(walletId, new BigDecimal("100.00"));
        
        when(walletService.processOperation(any())).thenReturn(response);
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(100.00));
    }
    
    @Test
    void processWalletOperation_Withdraw_Success() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, OperationType.WITHDRAW, new BigDecimal("50.00")
        );
        WalletResponse response = new WalletResponse(walletId, new BigDecimal("50.00"));
        
        when(walletService.processOperation(any())).thenReturn(response);
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(50.00));
    }
    
    @Test
    void processWalletOperation_InsufficientFunds_ReturnsBadRequest() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, OperationType.WITHDRAW, new BigDecimal("200.00")
        );
        
        when(walletService.processOperation(any()))
            .thenThrow(new InsufficientFundsException(walletId, BigDecimal.ZERO, new BigDecimal("200.00")));
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void processWalletOperation_InvalidJSON_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
    
    @Test
    void processWalletOperation_MissingWalletId_ReturnsBadRequest() throws Exception {
        String requestJson = "{\"operationType\":\"DEPOSIT\",\"amount\":100}";
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
    
    @Test
    void processWalletOperation_NegativeAmount_ReturnsBadRequest() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, OperationType.DEPOSIT, new BigDecimal("-100.00")
        );
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
    
    @Test
    void processWalletOperation_ZeroAmount_ReturnsBadRequest() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, OperationType.DEPOSIT, BigDecimal.ZERO
        );
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
    
    @Test
    void getWalletBalance_Success() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletResponse response = new WalletResponse(walletId, new BigDecimal("100.00"));
        
        when(walletService.getWalletBalance(walletId)).thenReturn(response);
        
        mockMvc.perform(get("/api/v1/wallets/" + walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(100.00));
    }
    
    @Test
    void getWalletBalance_WalletNotFound_ReturnsNotFound() throws Exception {
        UUID walletId = UUID.randomUUID();
        
        when(walletService.getWalletBalance(walletId))
            .thenThrow(new WalletNotFoundException(walletId));
        
        mockMvc.perform(get("/api/v1/wallets/" + walletId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void getWalletBalance_InvalidUUID_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/invalid-uuid"))
                .andExpect(status().isBadRequest());
    }
}

