package org.example.springadminv2.security;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/menu001")
public class SampleSecuredController {

    @GetMapping
    @PreAuthorize("hasAuthority('MENU001:R')")
    public ResponseEntity<String> read() {
        return ResponseEntity.ok("read");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MENU001:W')")
    public ResponseEntity<String> create() {
        return ResponseEntity.ok("created");
    }

    @PutMapping
    @PreAuthorize("hasAuthority('MENU001:W')")
    public ResponseEntity<String> update() {
        return ResponseEntity.ok("updated");
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('MENU001:W')")
    public ResponseEntity<String> delete() {
        return ResponseEntity.ok("deleted");
    }
}
