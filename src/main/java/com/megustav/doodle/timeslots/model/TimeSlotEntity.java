package com.megustav.doodle.timeslots.model;

import com.megustav.doodle.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "time_slots")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class TimeSlotEntity extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    // Plain id rather than @ManyToOne: users and slots are separate aggregates and
    // nothing here ever needs to walk into a user.
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Setter
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Setter
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeSlotStatus status;

    public TimeSlotEntity(UUID userId, Instant startsAt, Instant endsAt, TimeSlotStatus status) {
        this.userId = userId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
    }
}
