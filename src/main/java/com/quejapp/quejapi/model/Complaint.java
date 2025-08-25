package com.quejapp.quejapi.model;

import java.util.Date;

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
    private Date recievedDate;
    private Integer type;   
    private String description; 
    private String location; 
    private Integer status;
    private String user;      
    
    private String response; 
    private Date responseDate;
    private String employee;
    
    

}
