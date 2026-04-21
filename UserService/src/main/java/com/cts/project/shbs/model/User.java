package com.cts.project.shbs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "`User`")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Long userId;

    @Column(name = "Name", nullable = false, length = 255)
    private String name;

    @Column(name = "Email",
            nullable = false,
            unique = true,
            length = 255)
    private String email;

    @JsonIgnore
    @Column(name = "Password", nullable = false, length = 255)
    private String password;

    // Values: "ROLE_ADMIN", "ROLE_HOTEL_MANAGER", "ROLE_GUEST"
    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false, length = 50)
    private Role role;

    @Column(name = "ContactNumber", length = 20)
    private String contactNumber;
}