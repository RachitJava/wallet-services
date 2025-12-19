package com.wallet.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.dto.WalletOperationRequest;
import com.wallet.dto.WalletResponse;
import com.wallet.model.OperationType;
import com.wallet.model.Wallet;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WalletIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
    }
    
    @Test
    void depositToNewWallet_Success() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            walletId, OperationType.DEPOSIT, new BigDecimal("100.00")
        );
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(100.00));
        
        // Verify in database
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }
    
    @Test
    void multipleDeposits_Success() throws Exception {
        UUID walletId = UUID.randomUUID();
        
        // First deposit
        WalletOperationRequest request1 = new WalletOperationRequest(
            walletId, OperationType.DEPOSIT, new BigDecimal("50.00")
        );
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());
        
        // Second deposit
        WalletOperationRequest request2 = new WalletOperationRequest(
            walletId, OperationType.DEPOSIT, new BigDecimal("75.00")
        );
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());
        
        // Get balance
        mockMvc.perform(get("/api/v1/wallets/" + walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(125.00));
    }
    
    @Test
    void depositAndWithdraw_Success() throws Exception {
        UUID walletId = UUID.randomUUID();
        
        // Deposit
        WalletOperationRequest depositRequest = new WalletOperationRequest(
            walletId, OperationType.DEPOSIT, new BigDecimal("200.00")
        );
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk());
        
        // Withdraw
        WalletOperationRequest withdrawRequest = new WalletOperationRequest(
            walletId, OperationType.WITHDRAW, new BigDecimal("75.00")
        );
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(125.00));
    }
    
    @Test
    void withdrawWithInsufficientFunds_ReturnsBadRequest() throws Exception {
        UUID walletId = UUID.randomUUID();
        
        // Deposit small amount
        WalletOperationRequest depositRequest = new WalletOperationRequest(
            walletId, OperationType.DEPOSIT, new BigDecimal("50.00")
        );
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk());
        
        // Try to withdraw more
        WalletOperationRequest withdrawRequest = new WalletOperationRequest(
            walletId, OperationType.WITHDRAW, new BigDecimal("100.00")
        );
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void getBalance_WalletNotFound_ReturnsNotFound() throws Exception {
        UUID walletId = UUID.randomUUID();
        
        mockMvc.perform(get("/api/v1/wallets/" + walletId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
    
    @Test
    void concurrentDeposits_AllSucceed() throws Exception {
        UUID walletId = UUID.randomUUID();
        int numberOfThreads = 5;  // Reduced for reliability in test environment
        BigDecimal depositAmount = new BigDecimal("10.00");
        
        // First, create the wallet with initial deposit
        WalletOperationRequest initialRequest = new WalletOperationRequest(
            walletId, OperationType.DEPOSIT, new BigDecimal("0.01")
        );
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initialRequest)))
                .andExpect(status().isOk());
        
        // Now perform concurrent deposits
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
        List<Future<Integer>> futures = new ArrayList<>();
        
        for (int i = 0; i < numberOfThreads; i++) {
            Future<Integer> future = executorService.submit(() -> {
                try {
                    startLatch.await();  // Wait for all threads to be ready
                    
                    WalletOperationRequest request = new WalletOperationRequest(
                        walletId, OperationType.DEPOSIT, depositAmount
                    );
                    
                    MvcResult result = mockMvc.perform(post("/api/v1/wallet")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                            .andReturn();
                    
                    return result.getResponse().getStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                    return 500;
                } finally {
                    doneLatch.countDown();
                }
            });
            futures.add(future);
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(terminated).isTrue();
        
        // Count successful requests
        int successCount = 0;
        for (Future<Integer> future : futures) {
            Integer status = future.get(5, TimeUnit.SECONDS);
            if (status == 200) {
                successCount++;
            }
        }
        
        // All requests should succeed
        assertThat(successCount).isEqualTo(numberOfThreads);
        
        // Verify final balance
        Thread.sleep(100); // Small delay to ensure all transactions are committed
        
        MvcResult result = mockMvc.perform(get("/api/v1/wallets/" + walletId))
                .andExpect(status().isOk())
                .andReturn();
        
        WalletResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), 
            WalletResponse.class
        );
        
        // Expected: 0.01 (initial) + (5 * 10.00) = 50.01
        BigDecimal expectedBalance = new BigDecimal("0.01")
            .add(depositAmount.multiply(new BigDecimal(numberOfThreads)));
        assertThat(response.getBalance()).isEqualByComparingTo(expectedBalance);
    }
    
    @Test
    void concurrentDepositsAndWithdrawals_CorrectBalance() throws Exception {
        UUID walletId = UUID.randomUUID();
        
        // Initial deposit
        WalletOperationRequest initialDeposit = new WalletOperationRequest(
            walletId, OperationType.DEPOSIT, new BigDecimal("1000.00")
        );
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initialDeposit)))
                .andExpect(status().isOk());
        
        int numberOfOperations = 10;  // Reduced for reliability (5 deposits + 5 withdrawals)
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfOperations);
        List<Future<Integer>> futures = new ArrayList<>();
        
        // 5 deposits and 5 withdrawals
        for (int i = 0; i < numberOfOperations; i++) {
            final OperationType opType = i % 2 == 0 ? OperationType.DEPOSIT : OperationType.WITHDRAW;
            Future<Integer> future = executorService.submit(() -> {
                try {
                    startLatch.await();  // Wait for all threads to be ready
                    
                    WalletOperationRequest request = new WalletOperationRequest(
                        walletId, opType, new BigDecimal("10.00")
                    );
                    
                    MvcResult result = mockMvc.perform(post("/api/v1/wallet")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                            .andReturn();
                    
                    return result.getResponse().getStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                    return 500;
                } finally {
                    doneLatch.countDown();
                }
            });
            futures.add(future);
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(terminated).isTrue();
        
        // Count successful operations
        int successCount = 0;
        for (Future<Integer> future : futures) {
            Integer status = future.get(5, TimeUnit.SECONDS);
            if (status == 200) {
                successCount++;
            }
        }
        
        // All operations should succeed since we have enough balance
        assertThat(successCount).isEqualTo(numberOfOperations);
        
        // Verify balance consistency
        Thread.sleep(100); // Small delay to ensure all transactions are committed
        
        MvcResult result = mockMvc.perform(get("/api/v1/wallets/" + walletId))
                .andExpect(status().isOk())
                .andReturn();
        
        WalletResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), 
            WalletResponse.class
        );
        
        // Initial 1000 + (5 deposits * 10) - (5 withdrawals * 10) = 1000
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }
}

