package com.megustav.doodle.users.model;

import com.megustav.doodle.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class UserEntity extends BaseEntity {

    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("UTC");

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 64)
    private ZoneId timezone = DEFAULT_TIMEZONE;

    public UserEntity(String name, String email, ZoneId timezone) {
        this.name = name;
        this.email = email;
        this.timezone = Objects.requireNonNullElse(timezone, DEFAULT_TIMEZONE);
    }

}