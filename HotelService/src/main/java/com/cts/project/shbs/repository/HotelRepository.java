package com.cts.project.shbs.repository;

import com.cts.project.shbs.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    // Custom query to only fetch hotels that the Admin has approved
    List<Hotel> findByApprovalTrue();
    
    Optional<Hotel> findByManagerId(Long managerId);
    
 // Public Search: Must be approved. Name and Location are optional.
    @Query("SELECT h FROM Hotel h WHERE h.approval = true " +
           "AND (:name IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:location IS NULL OR LOWER(h.location) LIKE LOWER(CONCAT('%', :location, '%')))")
    List<Hotel> searchApprovedHotels(@Param("name") String name, @Param("location") String location);
    
    // Admin Search: Ignores approval status. Name and Location are optional.
    @Query("SELECT h FROM Hotel h WHERE " +
           "(:name IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:location IS NULL OR LOWER(h.location) LIKE LOWER(CONCAT('%', :location, '%')))")
    List<Hotel> searchAllHotelsForAdmin(@Param("name") String name, @Param("location") String location);
}