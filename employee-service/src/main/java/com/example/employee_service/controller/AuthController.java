package com.example.employee_service.controller;

import com.example.employee_service.entity.User;
import com.example.employee_service.repository.UserRepository;
import com.example.employee_service.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder encoder;
    @Autowired private AuthenticationManager authManager;
    @Autowired private JwtService jwtService;

    // POST /auth/register — il nuovo utente è sempre USER
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> body) {
        if (userRepo.findByUsername(body.get("username")).isPresent())
            return ResponseEntity.badRequest().body("Username già esistente");

        User user = new User();
        user.setUsername(body.get("username"));
        user.setPassword(encoder.encode(body.get("password")));
        user.setRuolo("USER"); // sempre USER alla registrazione
        userRepo.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("Utente registrato!");
    }

    // POST /auth/login — restituisce token + ruolo
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        body.get("username"), body.get("password")));

        User user = userRepo.findByUsername(body.get("username")).orElseThrow();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "ruolo", user.getRuolo(),
                "username", user.getUsername()
        ));
    }

    // GET /auth/utenti — solo ADMIN: lista tutti gli utenti
    @GetMapping("/utenti")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getUtenti() {
        List<Map<String, Object>> utenti = userRepo.findAll().stream()
                .map(u -> Map.of(
                        "id", (Object) u.getId(),
                        "username", u.getUsername(),
                        "ruolo", u.getRuolo()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(utenti);
    }

    // PATCH /auth/promuovi/{id} — solo ADMIN: promuove utente ad ADMIN
    @PatchMapping("/promuovi/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> promuovi(@PathVariable int id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
        user.setRuolo("ADMIN");
        userRepo.save(user);
        return ResponseEntity.ok("Utente " + user.getUsername() + " promosso ad ADMIN");
    }

    // PATCH /auth/retrocedi/{id} — solo ADMIN: retrocede utente a USER
    @PatchMapping("/retrocedi/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> retrocedi(@PathVariable int id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
        user.setRuolo("USER");
        userRepo.save(user);
        return ResponseEntity.ok("Utente " + user.getUsername() + " retrocesso a USER");
    }
}
