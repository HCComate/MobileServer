package com.semse.mobile_server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String role; // TECHNICIAN / ADMIN 등

    private String shiftStatus; // ON_DUTY / OFF_DUTY

    private String workStatus;  // IDLE / BUSY

    @ElementCollection
    @CollectionTable(name = "user_assigned_devices",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "device_id")
    private List<String> assignedDevices;
}