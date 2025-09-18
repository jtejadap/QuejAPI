package com.quejapp.quejapi.service;

import java.util.Date;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.quejapp.quejapi.dto.ComplaintSearch;
import com.quejapp.quejapi.dto.ComplaintUpdate;
import com.quejapp.quejapi.model.Complaint;
import com.quejapp.quejapi.model.Profile;
import com.quejapp.quejapi.model.Trace;
import com.quejapp.quejapi.model.User;
import com.quejapp.quejapi.repository.ComplaintRepository;
import com.quejapp.quejapi.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdministrationService {
    private final ComplaintRepository complaintsRepo;
    private final UserRepository usersRepo;
    
    public Page<Complaint> searchComplaints(ComplaintSearch request) {        
        Pageable pageable = createPageable(request);
        
        String searchTerm = request.getSearchTerm();
        Integer status = request.getStatus();
        // Case 1: Both search term and status provided
        if (StringUtils.hasText(searchTerm) && status != null) {
            return complaintsRepo.findByReferenceOrSubjectContainingIgnoreCaseAndStatus(
                searchTerm, status, pageable
            );
        }
        
        // Case 2: Only search term provided
        if (StringUtils.hasText(searchTerm)) {
            return complaintsRepo.findByReferenceOrSubjectContainingIgnoreCase(
                searchTerm, pageable);
        }
        
        // Case 3: Only status provided
        if (status != null) {
            return complaintsRepo.findByStatus(status,  pageable);
        }
        
        // Case 4: No filters, return all
        return complaintsRepo.findAllByOrderByRecievedDateDesc(pageable);
    }
    
    private Pageable createPageable(ComplaintSearch request) {
        Sort sort = Sort.by(
            "desc".equalsIgnoreCase(request.getSortDirection()) ? 
                Sort.Direction.DESC : Sort.Direction.ASC,
            request.getSortBy()
        );
        
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }


    public Optional<Complaint> getComplaintBy(String id){        
        return complaintsRepo.findById(id);
    }


    public Complaint updateComplaint(ComplaintUpdate update){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mail = auth.getName();
        System.out.println("🔐 Nickname extraído del token: " + mail);

        User employee = usersRepo.findByEmail(mail).orElseThrow(()-> {
            System.out.println("❌ Usuario no encontrado en la base de datos.");
            return new RuntimeException("Usuario no encontrado");
        });

        Optional<Complaint> complaintOptional = complaintsRepo.findById(update.getId());

        if(complaintOptional.isEmpty()){            
            throw new RuntimeException("Queja no encontrada");
        }

        Complaint complaint = complaintOptional.get();
        complaint = updateResponseFields(complaint, update, employee);
        complaint.setStatus(update.getStatus());        
        complaint.setUpdatedDate(new Date());
        return complaintsRepo.save(complaint);
    }

    private Complaint updateResponseFields(Complaint complaint, ComplaintUpdate update, User employee){
        if(!checkIfStatusChanged(complaint.getStatus(), update.getStatus())){
            return complaint;        
        }
        // New or In Process
        if (update.getStatus()>=0 && update.getStatus() < 2){ 
            complaint = cleanResponseFields(complaint);
            complaint = clearComplaintResponseMetrics(complaint);
            complaint = updateTreceability(complaint, "Estado cambiado a "+ getStatusName(update.getStatus()), employee.getEmail());
            return complaint;            
        }

        // Resolved or Closed
        complaint.setResponseDate(new Date());
        complaint.setResponse(update.getResponse());
        complaint.setEmployee(update.getId());
        complaint.setEmployeeProfile(Profile.builder()
            .id(employee.getId())
            .name(employee.getFirstname())
            .lastname(employee.getLastname())
            .email(employee.getEmail())
            .build());
        complaint.setDepartment(update.getDepartment());
        complaint = updateComplaintResponseMetrics(complaint);
        complaint = updateTreceability(complaint, "Radicado "+ getStatusName(update.getStatus()), employee.getEmail());
        return complaint;
        
    }

    private boolean checkIfStatusChanged(Integer status,Integer newStatus){
        if (status != null && newStatus != null && !status.equals(newStatus)) {
            return true;
        }
        return false;
    }

    private Complaint cleanResponseFields(Complaint complaint){
        complaint.setResponseDate(null);
        complaint.setResponse(null);
        complaint.setEmployee(null);
        complaint.setEmployeeProfile(null);
        complaint.setDepartment(null);
        return complaint;
    }

    private Complaint  updateComplaintResponseMetrics(Complaint complaint){
        complaint.setDaysfromExpiration(calculateDaysFromExpiration(complaint.getRecievedDate()));
        complaint.setDaysToResolve(calculateDaysToResolve(complaint.getRecievedDate(), complaint.getResponseDate()));
        return complaint;
    }

    private Integer calculateDaysFromExpiration(Date recievedDate){
        if(recievedDate == null) return null;
        Date currentDate = new Date();
        Integer daysElapsed = getDifferenceInDays(recievedDate, currentDate);
        int expirationPeriod = 15; //15 days to respond       
        return expirationPeriod - daysElapsed;
    }

    private Integer calculateDaysToResolve(Date recievedDate, Date responseDate){
        if(recievedDate == null || responseDate == null) return null;
        return getDifferenceInDays(recievedDate, responseDate);
    }

    private Integer getDifferenceInDays(Date start, Date end){
        long diffInMillies = Math.abs(end.getTime() - start.getTime());
        return (int) (diffInMillies / (1000 * 60 * 60 * 24));
    }

    private Complaint clearComplaintResponseMetrics(Complaint complaint){
        complaint.setDaysfromExpiration(null);
        complaint.setDaysToResolve(null);
        return complaint;
    }

    private Complaint updateTreceability(Complaint complaint, String action, String performedBy){
        complaint.addTrace(
            Trace.builder()
                .date(new Date())
                .status(action)
                .performedBy(performedBy)
                .build()
        );
        return complaint;
    }

    private String getStatusName(Integer status){
        return switch (status) {
            case 0 -> "Nuevo";
            case 1 -> "En Proceso";
            case 2 -> "Resuelto";
            case 3 -> "Cerrado";
            default -> "Desconocido";
        };
    }
   
}
