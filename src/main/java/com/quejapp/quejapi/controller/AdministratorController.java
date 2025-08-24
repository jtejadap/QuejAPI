package com.quejapp.quejapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/admin")
public class AdministratorController {
    @PostMapping("/home")
    public ResponseEntity<String> seyHello() {
        return ResponseEntity.ok("Your home is everywhere admin!");
    }
}
