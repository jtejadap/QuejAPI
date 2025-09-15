package com.quejapp.quejapi.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.quejapp.quejapi.dto.ComplaintResponse;
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

    public List<ComplaintResponse> getAllComplaints() {
        /*/
        return complaintsRepo.findAllByOrderByRecievedDateDesc()
                .stream()
                .map(ComplaintResponse::mapComplaint)
                .toList();
        /*/
        return null;
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
