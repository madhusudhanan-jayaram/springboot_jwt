# JWT Demo (springboot_jwt)

This project is a small Spring Boot JWT demo. The repository issues JWTs at login (signed with HS256) and validates them for protected endpoints using a request filter.

Files added
- `docs/sequence.puml` — PlantUML sequence diagram describing the login and request flow.

Overview
- Signing algorithm: HS256 (HMAC using SHA-256).
- Key management: the app must use a stable HMAC secret (Base64-encoded, at least 32 bytes when decoded). If you leave the current code unchanged it generates a random key at startup, which will invalidate tokens across restarts.
- Password validation: happens during login via Spring Security's `AuthenticationManager` which delegates to your `UserDetailsService` (named `UserService`) and `PasswordEncoder` (BCrypt). The login flow compares the raw password supplied by the client with the stored encoded password using `PasswordEncoder.matches(...)`.

Quickstart (Windows)
1. Make sure you have a suitable JDK and Maven installed (or use the included wrapper `mvnw.cmd`).

2. Configure a stable Base64-encoded secret (recommended). Example using OpenSSL (if available):

```bash
# generate a 32-byte random secret and print Base64
openssl rand -base64 32
```

If you do not have OpenSSL, you can use PowerShell to generate a 32-byte secret (run in PowerShell, not cmd.exe):

```powershell
# PowerShell: generate 32 random bytes and print Base64
[Convert]::ToBase64String((1..32 | ForEach-Object {Get-Random -Maximum 256}) -as [byte[]])
```

3. Add the secret to `src/main/resources/application.properties` (create the property if missing):

```properties
jwt.secret=BASE64_ENCODED_32_BYTE_SECRET_HERE
```

Important: the decoded secret should be at least 32 bytes for HS256 when using io.jsonwebtoken Keys.hmacShaKeyFor(...).

4. Run the app (from project root, Windows cmd):

```cmd
mvnw.cmd spring-boot:run
:: or, if you have mvn installed
mvn spring-boot:run
```

Or build and run the jar:

```cmd
mvnw.cmd clean package
java -jar target\jwt-demo-0.0.1-SNAPSHOT.jar
```

Example requests

- Login (returns token):

```cmd
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d "{\"username\":\"user\",\"password\":\"pass\"}"
```

- Access protected resource (replace <token> with JWT received):

```cmd
curl -X GET http://localhost:8080/api/protected -H "Authorization: Bearer <token>"
```

How password validation works (short)
1. Client posts credentials to `/auth/login`.
2. `AuthController` calls `authenticationManager.authenticate(...)` with a `UsernamePasswordAuthenticationToken`.
3. Spring Security's authentication manager delegates to the configured `UserDetailsService` (`UserService`) to load the `UserDetails` (which contains the stored, encoded password).
4. The manager uses the configured `PasswordEncoder` (BCrypt) to compare the raw password to the stored encoded password via `passwordEncoder.matches(raw, encoded)`.
5. If the password matches, authentication succeeds and the controller generates and returns a JWT using `JwtUtil.generateToken(...)`.
6. Subsequent requests include the token and the `JwtFilter` validates the token's signature and expiration — no password is re-checked during token validation.

Diagram (rendering)
- To render `docs/sequence.puml` you can use PlantUML (desktop or online). Example with PlantUML jar:

```bash
# requires Java and plantuml.jar
java -jar plantuml.jar docs/sequence.puml
```

Or paste the PlantUML into https://www.planttext.com/ or https://www.plantuml.com/plantuml to render.

Notes and next steps
- Change `JwtUtil` to read a stable Base64 secret from `jwt.secret` and build the key with `Keys.hmacShaKeyFor(...)` so tokens remain valid across restarts.
- Consider rotating secrets safely and exposing secrets through environment variables or a secrets manager instead of committing them to source control.

If you want, I can:
- Update `JwtUtil` now to read `jwt.secret` from configuration and add safety checks.
- Add a small integration test that signs a token and validates it.

@startuml
title JWT authentication sequence (HS256)
participant Client
participant "AuthController" as Controller
participant "AuthenticationManager" as AuthMgr
participant "UserService / UserDetailsService" as UserSvc
participant "PasswordEncoder (BCrypt)" as PwdEnc
participant "JwtUtil (HS256)" as JwtUtil
participant "JwtFilter" as JwtFilter
participant "ProtectedResource" as Resource

Client -> Controller: POST /auth/login\n{ username, password }
Controller -> AuthMgr: authenticate(UsernamePasswordAuthenticationToken)
AuthMgr -> UserSvc: loadUserByUsername(username)
UserSvc --> AuthMgr: UserDetails(username, password=encodedPassword)
AuthMgr -> PwdEnc: matches(rawPassword, encodedPassword)
PwdEnc --> AuthMgr: true / false
AuthMgr --> Controller: Authentication(success) / exception
Controller -> JwtUtil: generateToken(username)
JwtUtil --> Controller: JWT (header: {"alg":"HS256"}, signed with SECRET_KEY)
Controller --> Client: 200 OK \n{ token }

== Accessing protected endpoint ==
Client -> Resource: GET /api/protected\nAuthorization: Bearer <token>
Resource -> JwtFilter: filterRequest
JwtFilter -> JwtUtil: extractUsername(token)\n(parseClaimsJws -> verifies signature with SECRET_KEY)
JwtUtil --> JwtFilter: username, claims
JwtFilter -> SecurityContext: set Authentication
JwtFilter -> Resource: forward request
Resource --> Client: 200 OK

@enduml

