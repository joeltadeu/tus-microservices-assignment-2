package com.pms.appointment.repository;

import com.pms.appointment.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment,Long>, AppointmentRepositoryCustom {
    Optional<Appointment> findByIdAndPatientId(Long id, Long patientId);
}
