package com.hsbc.transaction.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@Configuration
@EnableCaching
@Profile("!test")
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("transactions", "accounts");
    }


    @Bean
    public PlatformTransactionManager transactionManager() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() throws TransactionException {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
                // No-op for in-memory operations
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
                // No-op for in-memory operations
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
                // No-op for in-memory operations
            }
        };
    }
} 