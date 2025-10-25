package edu.nu.owaspapivulnlab.dto;

import java.math.BigDecimal;

public class AccountResponse {
    private final Long id;
    private final String iban;
    private final BigDecimal balance;

    public AccountResponse(Long id, String iban, BigDecimal balance) {
        this.id = id;
        this.iban = iban;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public String getIban() {
        return iban;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
