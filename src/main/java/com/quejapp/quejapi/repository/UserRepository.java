package com.quejapp.quejapi.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.quejapp.quejapi.model.User;

public interface UserRepository extends MongoRepository<User,String>{
    Optional<User> findByEmail(String email);
}
