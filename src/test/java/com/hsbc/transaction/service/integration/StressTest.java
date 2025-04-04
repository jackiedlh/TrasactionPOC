package com.hsbc.transaction.service.integration;

import com.hsbc.transaction.model.TransactionFilter;
import com.hsbc.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class StressTest {

    @Autowired
    TransactionService transactionService;
    
    @Test
    void performanceUnderLoad() {
        int numberOfThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        // Simulate concurrent requests
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    // Perform operations
                    TransactionFilter filter = null;
                    transactionService.queryTransactions(filter, 0, 10);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            assertTrue(latch.await(30, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}