package com.quejapp.quejapi.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.quejapp.quejapi.model.Complaint;

public interface ComplaintRepository extends MongoRepository<Complaint, String> {
    Page<Complaint> findByUserOrderByRecievedDateDesc(String usuarioId, Pageable pageable);
    Page<Complaint> findAllByOrderByRecievedDateDesc(Pageable pageable);
    @Query("{ $and: [ " +
           "  { $or: [ " +
           "    { 'reference': { $regex: ?0, $options: 'i' } }, " +
           "    { 'subject': { $regex: ?0, $options: 'i' } } " +
           "  ] }, " +
           "  { 'status': ?1 }, " +
           "  { 'user': ?2 } " +
           "] }")
    Page<Complaint> findByReferenceOrSubjectContainingIgnoreCaseAndStatus(
        String searchTerm, Integer status, String user,  Pageable pageable);
    
    // Search by reference or subject without status filter
   @Query("{ $and: [ " +
           "  { $or: [ " +
           "    { 'reference': { $regex: ?0, $options: 'i' } }, " +
           "    { 'subject': { $regex: ?0, $options: 'i' } } " +
           "  ] }, " +          
           "  { 'user': ?1 } " +
           "] }")
    Page<Complaint> findByReferenceOrSubjectContainingIgnoreCase(
        String searchTerm, String user, Pageable pageable);
    
    // Filter by status only
    @Query("{ $and: [ " +          
           "  { 'status': ?0 }, " +
           "  { 'user': ?1 } " +
           "] }")
    Page<Complaint> findByStatus(Integer status, String user, Pageable pageable);

}
