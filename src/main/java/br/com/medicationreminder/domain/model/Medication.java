package br.com.medicationreminder.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "tb_medications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private String user;

    @Column(nullable = false)
    private String name;

    @ElementCollection
    @CollectionTable(
            name = "medication_scheduled_times",
            joinColumns = @JoinColumn(name = "medication_id")
    )
    @Column(name = "scheduled_time", nullable = false)
    private List<LocalTime> scheduleTimes;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "medication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReminderSession> reminderSessions;
}
