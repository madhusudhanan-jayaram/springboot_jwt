package com.example.jwt.controller;

import com.example.jwt.model.AuthRequest;
import com.example.jwt.model.AuthResponse;
import com.example.jwt.util.JwtUtil;
import com.example.jwt.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            /*
                With SSO, you do not manually validate username and password. Instead:
                The user authenticates via your SSO provider (e.g., OAuth2, SAML).
                Spring Security handles the SSO login flow.
                On successful SSO authentication, a custom success handler generates a JWT for your app.
                The JWT is sent to the client for API access.

                @Autowired
                    private JwtUtil jwtUtil;

                    @Override
                    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                        Authentication authentication) throws IOException {
                        String username = authentication.getName();
                        String token = jwtUtil.generateToken(username);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"token\": \"" + token + "\"}");
                    }
                }
             */
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(request.getUsername());
        return new AuthResponse(token);
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello, authenticated user!";
    }
}

