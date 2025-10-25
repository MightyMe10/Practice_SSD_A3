package edu.nu.owaspapivulnlab.web;

import edu.nu.owaspapivulnlab.dto.CreateUserRequest;
import edu.nu.owaspapivulnlab.dto.UserResponse;
import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.repo.AppUserRepository;
import edu.nu.owaspapivulnlab.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AppUserRepository users;
    private final UserService userService;

    public UserController(AppUserRepository users, UserService userService) {
        this.users = users;
        this.userService = userService;
    }

    // VULNERABILITY(API1: BOLA/IDOR) - no ownership check, any authenticated OR anonymous GET (due to SecurityConfig) can fetch any user
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        AppUser user = users.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return toResponse(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody CreateUserRequest request) {
        AppUser saved = userService.register(request);
        return toResponse(saved);
    }

    // VULNERABILITY(API9: Improper Inventory + API8 Injection style): naive 'search' that can be abused for enumeration
    @GetMapping("/search")
    public List<UserResponse> search(@RequestParam String q) {
        return users.search(q).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // VULNERABILITY(API3: Excessive Data Exposure) - returns all users including sensitive fields
    @GetMapping
    public List<UserResponse> list() {
        return users.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // VULNERABILITY(API5: Broken Function Level Authorization) - allows regular users to delete anyone
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        users.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "deleted");
        return ResponseEntity.ok(response);
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
    }
}
