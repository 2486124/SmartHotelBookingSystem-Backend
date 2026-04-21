package com.cts.project.shbs.repository;

import com.cts.project.shbs.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Finds a user by their email for login authentication
    Optional<User> findByEmail(String email);
    
    // Checks if an email is already registered to prevent duplicates
    Boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
     List<User> searchByKeyword(@Param("keyword") String keyword);
}