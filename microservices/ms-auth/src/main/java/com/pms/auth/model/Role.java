package com.pms.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Spring Security convention requires the ROLE_ prefix. Values: ROLE_HEALTHCARE_ADMIN,
   * ROLE_DOCTOR, ROLE_PATIENT
   */
  @Column(nullable = false, unique = true)
  private String name;
}
