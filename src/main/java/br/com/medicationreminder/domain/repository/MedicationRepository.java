package br.com.medicationreminder.domain.repository;

import br.com.medicationreminder.domain.model.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, UUID> {
    List<Medication> findByUserIdAndActiveTrue(UUID userId);

    @Query("""
        SELECT m FROM Medication m
        JOIN m.scheduledTimes st
        WHERE m.active = true
        AND st BETWEEN :start AND :end
    """)
    List<Medication> findActiveMedicationScheduleBetween(
            @Param("start") LocalTime start,
            @Param("end") LocalTime end
    );
}
