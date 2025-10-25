# OWASP API Hardening Journey

This branch starts from the intentionally vulnerable lab and layers the ten requested fixes one commit at a time. The table below is updated after every step so reviewers can see which mitigations are already present.

| Requirement                         | Status                     | Notes                                                                                 |
| ----------------------------------- | -------------------------- | ------------------------------------------------------------------------------------- |
| 1. Hash passwords & add signup flow | ✅ Implemented in commit 1 | BCrypt replaces plaintext storage, a signup endpoint provisions new users safely.     |
| 2. Enforce authentication defaults  | ✅ Implemented in commit 2 | All API routes now require auth (except login/signup/health) and return JSON 401/403. |
| 3. Enforce account ownership        | ✅ Implemented in commit 3 | Service layer verifies ownership, BigDecimal balances prevent rounding exploits.      |
| 4. Stop excessive data exposure     | ✅ Implemented in commit 4 | User endpoints now respond with DTOs—no passwords, roles, or admin flags leak out.    |
| 5–10                                | ⏳ Pending                 | Will be added in later commits.                                                       |

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

## Verification Commands (Fix 3)

- Alice only sees her own accounts:

  ```powershell
  $aliceToken = (curl.exe -s -X POST http://localhost:9090/api/auth/login `
    -H "Content-Type: application/json" `
    -d "{\"username\":\"alice\",\"password\":\"alice123\"}" | ConvertFrom-Json).token
  curl.exe -s -H "Authorization: Bearer $aliceToken" http://localhost:9090/api/accounts/mine
  # -> [{"id":1,"iban":"PK00-ALICE","balance":1000.00}]
  ```

- Alice cannot read Bob's balance (403):

  ```powershell
  curl.exe -i -H "Authorization: Bearer $aliceToken" http://localhost:9090/api/accounts/2/balance
  # -> HTTP/1.1 403 Forbidden
  ```

- Admin Bob can inspect any account:

  ```powershell
  $bobToken = (curl.exe -s -X POST http://localhost:9090/api/auth/login `
    -H "Content-Type: application/json" `
    -d "{\"username\":\"bob\",\"password\":\"bob123\"}" | ConvertFrom-Json).token
  curl.exe -s -H "Authorization: Bearer $bobToken" http://localhost:9090/api/accounts/1/balance
  ```

## Verification Commands (Fix 4)

- User lookups no longer leak password hashes or admin flags:

  ```powershell
  curl.exe -s http://localhost:9090/api/users/1
  # -> {"id":1,"username":"alice","email":"alice@cydea.tech"}
  ```

- Bulk user listings are similarly trimmed to safe fields:

  ```powershell
  curl.exe -s http://localhost:9090/api/users | ConvertFrom-Json
  # objects only contain id/username/email even without auth checks yet
  ```

Further sections documenting fixes 5–10 will be appended as those commits land.
