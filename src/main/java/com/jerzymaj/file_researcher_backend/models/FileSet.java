package com.jerzymaj.file_researcher_backend.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class FileSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime creationDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotBlank
    @Column(nullable = false)
    private String description;

    @Email
    @NotBlank
    @Column(nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileSetStatus status;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "file_set_files",
            joinColumns = @JoinColumn(name = "file_set_id"),
            inverseJoinColumns = @JoinColumn(name = "file_entry_id")
    )
    @Builder.Default
    private List<FileEntry> files = new ArrayList<>();
}
