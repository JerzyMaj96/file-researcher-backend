package com.jerzymaj.file_researcher_backend.models;

import com.jerzymaj.file_researcher_backend.models.enum_classes.SendStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zip_archive_id", nullable = false)
    private ZipArchive zipArchive;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime sendAttemptDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SendStatus status;

    private String errorMessage;

    @Email
    @Column(nullable = false)
    private String sentToEmail;

}
