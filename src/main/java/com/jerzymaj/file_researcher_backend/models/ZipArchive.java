package com.jerzymaj.file_researcher_backend.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jerzymaj.file_researcher_backend.models.suplementary_classes.ZipArchiveStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)

public class ZipArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String archiveName;

    @NotBlank
    @Column(nullable = false)
    private String archivePath;

    @Column(nullable = false)
    private Long size;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @CreationTimestamp
    @Column(updatable = false)
    @ToString.Include
    private LocalDateTime creationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ZipArchiveStatus status;

    @Email
    @NotBlank
    @Column(nullable = false)
    private String recipientEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_set_id", nullable = false, updatable = false)
    private FileSet fileSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false, updatable = false)
    private User user;

    @OneToMany(mappedBy = "zipArchive", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SendHistory> sendHistoryList;

}
