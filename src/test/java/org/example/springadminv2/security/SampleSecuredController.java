package org.example.springadminv2.security;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleSecuredController {

    @GetMapping("/test/menu001")
    @PreAuthorize("hasAuthority('SAMPLE:R')")
    public ResponseEntity<String> read() {
        return ResponseEntity.ok("read");
    }

    @PostMapping("/test/menu001")
    @PreAuthorize("hasAuthority('SAMPLE:W')")
    public ResponseEntity<String> create() {
        return ResponseEntity.ok("created");
    }

    @PutMapping("/test/menu001")
    @PreAuthorize("hasAuthority('SAMPLE:W')")
    public ResponseEntity<String> update() {
        return ResponseEntity.ok("updated");
    }

    @DeleteMapping("/test/menu001")
    @PreAuthorize("hasAuthority('SAMPLE:W')")
    public ResponseEntity<String> delete() {
        return ResponseEntity.ok("deleted");
    }

    @GetMapping("/test/cross-resource")
    @PreAuthorize("hasAuthority('WASINSTANCE:R')")
    public ResponseEntity<String> crossResourceRead() {
        return ResponseEntity.ok("cross-resource-read");
    }

    @PostMapping("/test/cross-resource")
    @PreAuthorize("hasAuthority('WASINSTANCE:W')")
    public ResponseEntity<String> crossResourceWrite() {
        return ResponseEntity.ok("cross-resource-write");
    }
}
