# OWASP API Hardening Journey

This branch starts from the intentionally vulnerable lab and layers the ten requested fixes one commit at a time. The table below is updated after every step so reviewers can see which mitigations are already present.

| Requirement                         | Status                     | Notes                                                                             |
| ----------------------------------- | -------------------------- | --------------------------------------------------------------------------------- |
| 1. Hash passwords & add signup flow | ✅ Implemented in commit 1 | BCrypt replaces plaintext storage, a signup endpoint provisions new users safely. |
| 2–10                                | ⏳ Pending                 | Will be added in later commits.                                                   |

## Running the Application

```powershell
mvn spring-boot:run
```

The app still runs on port 8080. Seeded credentials now use BCrypt hashes:

```powershell
curl -s -X POST http://localhost:8080/api/auth/login `
	-H "Content-Type: application/json" `
	-d '{"username":"alice","password":"alice123"}'
```

## Verification Commands (Fix 1)

- Passwords are no longer readable from the database because they are stored using BCrypt.
- New users can sign up via:

  ```powershell
  curl -s -X POST http://localhost:8080/api/auth/signup `
  	-H "Content-Type: application/json" `
  	-d '{"username":"charlie","password":"charliePass1","email":"charlie@example.com"}'
  ```

  The response returns the created user without exposing the hashed password.

Further sections documenting fixes 2–10 will be appended as those commits land.
