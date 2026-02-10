package io.github.rivon0507.courier.envoi.domain;

import io.github.rivon0507.courier.common.domain.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "envoi")
public class Envoi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reference;

    @Column(nullable = false)
    private String destinataire;

    @Column(nullable = false)
    private LocalDate dateEnvoi;

    @Nullable
    private String observation;

    @ManyToOne
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @OneToMany(mappedBy = "envoi", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<EnvoiPiece> pieces;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}