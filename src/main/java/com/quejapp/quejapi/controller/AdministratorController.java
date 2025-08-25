package com.quejapp.quejapi.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quejapp.quejapi.dto.ComplaintResponse;
import com.quejapp.quejapi.dto.ComplaintUpdate;
import com.quejapp.quejapi.model.Complaint;
import com.quejapp.quejapi.service.AdministrationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/admin")
@RequiredArgsConstructor
public class AdministratorController {
    private final AdministrationService administrationService;

    @PostMapping("/home")
    public ResponseEntity<String> seyHello() {
        return ResponseEntity.ok("Your home is everywhere admin!");
    }

     // Get all complaints.     
    @GetMapping("/complaints")
    public List<ComplaintResponse> getAllComplaints() {
        return administrationService.getAllComplaints();
    }

    // Get a complaint by ID.    
    @GetMapping("/complaints/{id}")
    public ResponseEntity<Complaint> getProductById(@PathVariable String id) {
        Optional<Complaint> complaint = administrationService.getComplaintBy(id);
        return complaint.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update a complaint by ID.     
    @PutMapping("/complaints/{id}")
    public ResponseEntity<Complaint> updateProduct(@PathVariable Long id, @RequestBody ComplaintUpdate complaint) {
        Complaint update = administrationService.updateComplaint(complaint);
        return ResponseEntity.ok(update);
    }
}
