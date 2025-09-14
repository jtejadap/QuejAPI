package com.quejapp.quejapi.dto;

import java.util.Date;

import com.quejapp.quejapi.model.Complaint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintResponse {
    private String id;    
    private Date recievedDate;
    private Integer type;   
    private String description; 
    private String location; 
    private Integer status;
    private String user;
    private String response;

    public static ComplaintResponse mapComplaint(Complaint complaint){
        return ComplaintResponse.builder()
            .id(complaint.getId())
            .description(complaint.getDescription())               
            .recievedDate(complaint.getRecievedDate())
            .type(complaint.getType())  
            .description(complaint.getDescription())
            .status(complaint.getStatus())
            .user(complaint.getUser())
            .response(complaint.getResponse())
            .build();
    }
}
