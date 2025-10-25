package edu.nu.owaspapivulnlab.web;

import edu.nu.owaspapivulnlab.dto.CreateUserRequest;
import edu.nu.owaspapivulnlab.dto.LoginRequest;
import edu.nu.owaspapivulnlab.dto.TokenResponse;
import edu.nu.owaspapivulnlab.dto.UserResponse;
import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.service.JwtService;
import edu.nu.owaspapivulnlab.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtService jwt;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, JwtService jwt, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwt = jwt;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        AppUser user = userService.findByUsername(request.getUsername()).orElse(null);
        if (user != null && passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole());
            claims.put("isAdmin", user.isAdmin()); // VULN: trusts client-side role later
            String token = jwt.issue(user.getUsername(), claims);
            return ResponseEntity.ok(new TokenResponse(token));
        }
        Map<String, String> error = new HashMap<>();
        error.put("error", "invalid credentials");
        return ResponseEntity.status(401).body(error);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody CreateUserRequest request) {
        AppUser created = userService.register(request);
        return new UserResponse(created.getId(), created.getUsername(), created.getEmail());
    }
}
