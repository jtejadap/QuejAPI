package com.quejapp.quejapi.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import com.quejapp.quejapi.model.Profile;
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
    private final WekaService wekaService;

    public Complaint createComplaintForUser(Complaint complaint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mail = auth.getName();
        System.out.println("🔐 Nickname extraído del token: " + mail);

        User user = usersRepo.findByEmail(mail).orElseThrow(()-> {
            System.out.println("❌ Usuario no encontrado en la base de datos.");
            return new RuntimeException("Usuario no encontrado");
        });
      
        return complaintsRepo.save(initializeComplaint(complaint, user));
    }

    private Complaint initializeComplaint(Complaint complaint, User user) {
        complaint.setStatus(0);
        complaint.setUser(user.getId());
        complaint.setUserProfile(Profile.builder()
            .id(user.getId())
            .name(user.getFirstname())
            .lastname(user.getLastname())
            .email(user.getEmail())
            .build()
        );
        complaint.setReference(buildReferenceField(complaint.getType()));
        complaint.setRecievedDate(new java.util.Date());
        complaint.setTraceability(new ArrayList<Trace>());        
        complaint.addTrace(Trace.builder()
            .date(new java.util.Date())
            .status("PQRS Radicada")
            .performedBy(user.getEmail())            
            .build()
        );
        complaint.setPrediction(makePredition(complaint));
        System.out.println("✅ Queja inicializada con referencia: " + complaint.getReference());
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

    private String makePredition(Complaint complaint){
        String tipo = transformTypeToString(complaint.getType());
        String canal = "Web";
        String dia = transformDayToString(complaint.getRecievedDate());
        String mes = transformMonthToString(complaint.getRecievedDate());
        try {
            return wekaService.predict(tipo, canal, dia, mes, true);
        } catch (Exception e) {
            System.out.println("❌ Error al hacer la predicción: " + e.getMessage());
            return "Desconocido";
        }        
    }

    private String transformTypeToString(Integer type) {
        String[] types = {"Peticion", "Queja", "Reclamo", "Sugerencia", "Felicitacion"};
        if(type == null || type > types.length) {
            return "Peticion";
        }
        return types[type];
    }

    private String transformDayToString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("u"); // 'u' gives day
        int day = Integer.parseInt(sdf.format(date));
        String[] days = {"lunes", "martes", "miercoles", "jueves", "viernes"};
        if( day < 1 || day > days.length) {
            return "lunes";
        }
        if(day > 5){
            return "viernes";
        } 
        return days[day - 1];
    }

    private String transformMonthToString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("M"); // 'M' gives month
        int month = Integer.parseInt(sdf.format(date));
        String[] months = {"enero", "febrero", "marzo", "abril", "mayo", "junio", 
                           "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
        if(month < 1 || month > months.length) {
            return "enero";
        }
        return months[month - 1];
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
