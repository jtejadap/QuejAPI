package com.quejapp.quejapi.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintUpdate {
    private String id;
    private String usermail;
    private Integer status;
    private String response;
}
