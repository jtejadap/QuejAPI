package com.quejapp.quejapi.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quejapp.quejapi.dto.ComplaintResponse;
import com.quejapp.quejapi.model.Complaint;
import com.quejapp.quejapi.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/home")
    public ResponseEntity<String> seyHello() {
        return ResponseEntity.ok("Your home is here user!");
    }

     // Create a new complaint.
    @PostMapping("/complaint")
    public ResponseEntity<Complaint> saveComplaint(@RequestBody Complaint complaint) {
        Complaint newComplaint = userService.createComplaintForUser(complaint);
        return ResponseEntity.ok(newComplaint);
    }

    // Get all user complaints.     
    @GetMapping("/complaints")
    public List<ComplaintResponse> getAllComplaints() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getAllComplaintsBy(email);
    }

    // Get a product by ID.    
    @GetMapping("/complaints/{id}")
    public ResponseEntity<ComplaintResponse> getComplaintById(@PathVariable String id) {
        ComplaintResponse complaint = userService.getComplaintBy(id);
        return ResponseEntity.ok(complaint);
    }
}
