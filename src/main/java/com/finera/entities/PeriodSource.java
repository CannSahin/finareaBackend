package com.finera.entities;

import com.finera.entities.enums.SourceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "period_sources", schema = "public",
        uniqueConstraints = @UniqueConstraint(columnNames = {"period_id", "source_name"}))
public class PeriodSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "source_id", updatable = false, nullable = false)
    private UUID sourceId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private Period period;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Source type cannot be null")
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @NotBlank(message = "Source name cannot be blank")
    @Column(name = "source_name", nullable = false, length = 150)
    private String sourceName;

    @Column(name = "institution_name", length = 100)
    private String institutionName;

    @Column(name = "upload_timestamp")
    private OffsetDateTime uploadTimestamp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "periodSource", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Transaction> transactions;
}