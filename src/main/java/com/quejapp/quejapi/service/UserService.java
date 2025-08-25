package com.quejapp.quejapi.service;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.quejapp.quejapi.dto.ComplaintResponse;
import com.quejapp.quejapi.model.Complaint;
import com.quejapp.quejapi.model.User;
import com.quejapp.quejapi.repository.ComplaintRepository;
import com.quejapp.quejapi.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final ComplaintRepository complaintsRepo;
    private final UserRepository usersRepo;     

    
    public List<ComplaintResponse> getAllComplaintsBy(String email) {
        User user = usersRepo.findByEmail(email).orElseThrow(()-> new RuntimeException("Usuario no encontrado"));
        List<Complaint> complaints = complaintsRepo.findByUserOrderByRecievedDateDesc(user.getId());
        return complaints.stream().map(ComplaintResponse::mapComplaint).toList();
    }


    public Complaint createComplaintForUser(Complaint complaint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mail = auth.getName();
        System.out.println("🔐 Nickname extraído del token: " + mail);

        User user = usersRepo.findByEmail(mail).orElseThrow(()-> {
            System.out.println("❌ Usuario no encontrado en la base de datos.");
            return new RuntimeException("Usuario no encontrado");
        });

        complaint.setStatus(1);
        complaint.setUser(user.getId());
        return complaintsRepo.save(complaint);
    }


    public ComplaintResponse getComplaintBy(String id) {
        Complaint complaint = complaintsRepo.findById(id).orElseThrow(()-> {
            System.out.println("❌ Queja no encontrada con ID: " + id);
            return new RuntimeException("Queja no encontrada con ID: " + id);
        });

        // Log de la queja
        System.out.println("📄 Queja encontrada: " + complaint.getDescription());
        System.out.println("👤 Usuario ID asociado: " + complaint.getUser());

        User user = usersRepo.findById(complaint.getUser()).orElseThrow(()-> {
           System.out.println("⚠️ Usuario no encontrado para ID: " + complaint.getUser());
            return new RuntimeException("Usuario no encontrado");
        });

        System.out.println("✅ Usuario encontrado: " + user.getFirstname() + " " + user.getLastname());

        return ComplaintResponse.builder()
            .id(complaint.getId())
            .description(complaint.getDescription())               
            .recievedDate(complaint.getRecievedDate())
            .type(complaint.getType())  
            .description(complaint.getDescription())
            .location(complaint.getLocation())
            .status(complaint.getStatus())
            .user(complaint.getUser())
            .response(complaint.getResponse())
            .build();
    }
}
