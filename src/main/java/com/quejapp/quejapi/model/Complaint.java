package com.quejapp.quejapi.model;

import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("quejas")
public class Complaint {
    @Id
    private String id; 
    private String reference;   
    private Date recievedDate;
    private Integer type; 
    private Integer category;
    private String subject;  
    private String description;
    private Integer status;
    private String user; 
    private Profile userProfile;     
    
    private String response; 
    private Date responseDate;
    private String department;
    private String employee;
    private Profile employeeProfile;
    private Integer daysToResolve;
    private Integer daysfromExpiration;
    private List<Trace> traceability;

    public void addTrace(Trace trace) {
        this.traceability.add(trace);        
    }
}
