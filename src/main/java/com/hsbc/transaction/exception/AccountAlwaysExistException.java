package com.hsbc.transaction.exception;

public class AccountAlwaysExistException extends RuntimeException {
    public AccountAlwaysExistException(String s) {
        super(s);
    }
}
