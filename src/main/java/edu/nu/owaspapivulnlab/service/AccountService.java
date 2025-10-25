package edu.nu.owaspapivulnlab.service;

import edu.nu.owaspapivulnlab.dto.AccountResponse;
import edu.nu.owaspapivulnlab.model.Account;
import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.repo.AccountRepository;
import edu.nu.owaspapivulnlab.service.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private static final BigDecimal MAX_TRANSFER_AMOUNT = new BigDecimal("10000.00");

    private final AccountRepository accounts;
    private final RateLimiterService rateLimiter;

    public AccountService(AccountRepository accounts, RateLimiterService rateLimiter) {
        this.accounts = accounts;
        this.rateLimiter = rateLimiter;
    }

    @Transactional(readOnly = true)
    public AccountResponse balance(Long id, AppUser currentUser) {
        Account account = accounts.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        enforceOwnershipOrAdmin(account, currentUser);
        return toResponse(account);
    }

    @Transactional
    public AccountResponse transfer(Long id, BigDecimal amount, AppUser currentUser) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (amount.compareTo(MAX_TRANSFER_AMOUNT) > 0) {
            throw new IllegalArgumentException("Transfer amount exceeds the maximum allowed");
        }
        rateLimiter.checkTransferForUser(currentUser.getId());
        Account account = accounts.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        enforceOwnership(account, currentUser);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for transfer");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accounts.save(account);
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> mine(AppUser currentUser) {
        return accounts.findByOwnerUserId(currentUser.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void enforceOwnershipOrAdmin(Account account, AppUser currentUser) {
        if (currentUser.isAdmin()) {
            return;
        }
        enforceOwnership(account, currentUser);
    }

    private void enforceOwnership(Account account, AppUser currentUser) {
        if (!account.getOwnerUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Account does not belong to current user");
        }
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getIban(), account.getBalance());
    }
}
