package com.quejapp.quejapi.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.quejapp.quejapi.model.Complaint;

public interface ComplaintRepository extends MongoRepository<Complaint, String> {
    List<Complaint> findByUserOrderByRecievedDateDesc(String usuarioId);
    List<Complaint> findAllByOrderByRecievedDateDesc();
}
