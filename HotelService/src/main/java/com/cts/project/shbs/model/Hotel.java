package com.cts.project.shbs.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "hotel")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hotelId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(unique = true, nullable = false)
    private Long managerId; 

    @Column(columnDefinition = "TEXT")
    private String amenities;

    private String imageUrl;

    private Double rating;

    // Defaults to false 
    @Column(nullable = false)
    private Boolean approval = false; 

    // One Hotel has Many Rooms
    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Room> rooms;
}
