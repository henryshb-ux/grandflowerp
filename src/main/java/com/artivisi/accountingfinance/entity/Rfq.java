package com.artivisi.accountingfinance.entity;

import com.artivisi.accountingfinance.enums.RfqStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rfq")
@Getter @Setter @NoArgsConstructor
public class Rfq extends BaseEntity {

    @NotBlank
    @Size(max = 50)
    @Column(name = "rfq_number", nullable = false, unique = true, length = 50)
    private String rfqNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_client", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_project")
    private Project project;

    @NotNull
    @Column(name = "rfq_date", nullable = false)
    private LocalDate rfqDate;

    @Column(name = "response_date")
    private LocalDate responseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RfqStatus status = RfqStatus.OPEN;

    @Size(max = 500)
    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<RfqLine> lines = new ArrayList<>();

    // ── helpers ──────────────────────────────────────────────
    public boolean isOpen()       { return status == RfqStatus.OPEN; }
    public boolean isQuoted()     { return status == RfqStatus.QUOTED; }
    public boolean isCancelled()  { return status == RfqStatus.CANCELLED; }

    public String getClientLabel() {
        if (client == null) return "";
        return client.getCode() + " - " + client.getName();
    }
}
