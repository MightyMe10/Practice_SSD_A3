# OWASP API Hardening Journey

This branch starts from the intentionally vulnerable lab and layers the ten requested fixes one commit at a time. The table below is updated after every step so reviewers can see which mitigations are already present.

| Requirement                         | Status                     | Notes                                                                                 |
| ----------------------------------- | -------------------------- | ------------------------------------------------------------------------------------- |
| 1. Hash passwords & add signup flow | ✅ Implemented in commit 1 | BCrypt replaces plaintext storage, a signup endpoint provisions new users safely.     |
| 2. Enforce authentication defaults  | ✅ Implemented in commit 2 | All API routes now require auth (except login/signup/health) and return JSON 401/403. |
| 3–10                                | ⏳ Pending                 | Will be added in later commits.                                                       |

## Running the Application

```powershell
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
```

The samples below assume port 9090—adjust if you run on a different port.

Seeded credentials now use BCrypt hashes:

```powershell
curl.exe -s -X POST http://localhost:9090/api/auth/login `
	-H "Content-Type: application/json" `
	-d "{\"username\":\"alice\",\"password\":\"alice123\"}"
```

## Verification Commands (Fix 1)

- Passwords are no longer readable from the database because they are stored using BCrypt.
- New users can sign up via:

  ```powershell
  curl.exe -s -X POST http://localhost:9090/api/auth/signup `
  	-H "Content-Type: application/json" `
  	-d "{\"username\":\"charlie\",\"password\":\"charliePass1\",\"email\":\"charlie@example.com\"}"
  ```

  The response returns the created user without exposing the hashed password.

## Verification Commands (Fix 2)

- Unauthenticated requests now fail fast:

  ```powershell
  curl.exe http://localhost:9090/api/accounts/mine
  # -> {"error":"unauthorized"}
  ```

- Authenticated requests still succeed:

  ```powershell
  $token = curl.exe -s -X POST http://localhost:9090/api/auth/login `
    -H "Content-Type: application/json" `
    -d "{\"username\":\"alice\",\"password\":\"alice123\"}" | ConvertFrom-Json
  curl.exe -H "Authorization: Bearer $($token.token)" http://localhost:9090/api/accounts/mine
  ```

Further sections documenting fixes 3–10 will be appended as those commits land.
