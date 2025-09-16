package com.quejapp.quejapi.service;

import java.util.Date;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.quejapp.quejapi.dto.ComplaintSearch;
import com.quejapp.quejapi.dto.ComplaintUpdate;
import com.quejapp.quejapi.model.Complaint;
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
        Optional<Complaint> complaint = complaintsRepo.findById(update.getId());

        if(complaint.isEmpty()){
            return new Complaint();
        }
        User employee = usersRepo.findByEmail(update.getUsermail()).orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));;

        Complaint complaintUpdated = complaint.get();
        complaintUpdated.setEmployee(employee.getId());
        complaintUpdated.setStatus(update.getStatus());
        complaintUpdated.setResponse(update.getResponse());
        complaintUpdated.setResponseDate(new Date());
        return complaintsRepo.save(complaintUpdated);
    }
}
