package com.quejapp.quejapi.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Random;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.quejapp.quejapi.dto.ComplaintSearch;
import com.quejapp.quejapi.model.Complaint;
import com.quejapp.quejapi.model.Trace;
import com.quejapp.quejapi.model.User;
import com.quejapp.quejapi.repository.ComplaintRepository;
import com.quejapp.quejapi.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final ComplaintRepository complaintsRepo;
    private final UserRepository usersRepo; 

    public Complaint createComplaintForUser(Complaint complaint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mail = auth.getName();
        System.out.println("🔐 Nickname extraído del token: " + mail);

        User user = usersRepo.findByEmail(mail).orElseThrow(()-> {
            System.out.println("❌ Usuario no encontrado en la base de datos.");
            return new RuntimeException("Usuario no encontrado");
        });
      
        return complaintsRepo.save(initializeComplaint(complaint, user.getId()));
    }

    private Complaint initializeComplaint(Complaint complaint, String user) {
        complaint.setStatus(0);
        complaint.setUser(user);
        complaint.setReference(buildReferenceField(complaint.getType()));
        complaint.setRecievedDate(new java.util.Date());
        complaint.setTraceability(new ArrayList<Trace>());
        complaint.addTrace(Trace.builder()
            .date(new java.util.Date())
            .status("PQRS Radicada")            
            .build()
        );
        return complaint;
    }

    private String buildReferenceField(Integer type) {
        String prefix = "TRC";
        String date = new SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String random = generateRandomString(6);
        return String.format("%s-%s-%s-%s", prefix, date, random, type);
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }


    public Complaint getComplaintBy(String id) {
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

        return complaint;
    }

    public Page<Complaint> searchComplaints(ComplaintSearch request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mail = auth.getName();
        System.out.println("🔐 Nickname extraído del token: " + mail);

        User user = usersRepo.findByEmail(mail).orElseThrow(()-> {
            System.out.println("❌ Usuario no encontrado en la base de datos.");
            return new RuntimeException("Usuario no encontrado");
        });
        request.setUserId(user.getId());
        Pageable pageable = createPageable(request);
        
        String searchTerm = request.getSearchTerm();
        Integer status = request.getStatus();
        // Case 1: Both search term and status provided
        if (StringUtils.hasText(searchTerm) && status != null) {
            return complaintsRepo.findByReferenceOrSubjectContainingIgnoreCaseAndStatus(
                searchTerm, status, user.getId(), pageable
            );
        }
        
        // Case 2: Only search term provided
        if (StringUtils.hasText(searchTerm)) {
            return complaintsRepo.findByReferenceOrSubjectContainingIgnoreCase(
                searchTerm, user.getId(), pageable);
        }
        
        // Case 3: Only status provided
        if (status != null) {
            return complaintsRepo.findByStatus(status, user.getId(), pageable);
        }
        
        // Case 4: No filters, return all
        return complaintsRepo.findByUserOrderByRecievedDateDesc(request.getUserId(), pageable);
    }
    
    private Pageable createPageable(ComplaintSearch request) {
        Sort sort = Sort.by(
            "desc".equalsIgnoreCase(request.getSortDirection()) ? 
                Sort.Direction.DESC : Sort.Direction.ASC,
            request.getSortBy()
        );
        
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}
