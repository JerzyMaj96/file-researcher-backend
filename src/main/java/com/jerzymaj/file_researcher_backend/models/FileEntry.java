package com.jerzymaj.file_researcher_backend.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class FileEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String originalName;

    @NotBlank
    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private Long size;

    @NotBlank
    @Size(max = 10)
    private String extension;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_set_id", nullable = false)
    private FileSet fileSet;
}
