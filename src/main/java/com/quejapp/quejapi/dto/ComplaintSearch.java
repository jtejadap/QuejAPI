package com.quejapp.quejapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintSearch {
    private String searchTerm;
    private Integer status;
    private String userId;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
