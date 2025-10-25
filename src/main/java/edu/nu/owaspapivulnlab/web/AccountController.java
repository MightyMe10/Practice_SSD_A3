package edu.nu.owaspapivulnlab.web;

import edu.nu.owaspapivulnlab.dto.AccountResponse;
import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.service.AccountService;
import edu.nu.owaspapivulnlab.service.CurrentUserService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Validated
public class AccountController {

    private final AccountService accountService;
    private final CurrentUserService currentUserService;

    public AccountController(AccountService accountService, CurrentUserService currentUserService) {
        this.accountService = accountService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{id}/balance")
    public AccountResponse balance(@PathVariable Long id) {
        AppUser current = currentUserService.requireCurrentUser();
        return accountService.balance(id, current);
    }

    @PostMapping("/{id}/transfer")
    public AccountResponse transfer(
            @PathVariable Long id,
            @RequestParam
            @NotNull
            @DecimalMin(value = "0.01")
            @DecimalMax(value = "10000.00")
            @Digits(integer = 8, fraction = 2) BigDecimal amount) {
        AppUser current = currentUserService.requireCurrentUser();
        return accountService.transfer(id, amount, current);
    }

    @GetMapping("/mine")
    public List<AccountResponse> mine() {
        AppUser current = currentUserService.requireCurrentUser();
        return accountService.mine(current);
    }
}
