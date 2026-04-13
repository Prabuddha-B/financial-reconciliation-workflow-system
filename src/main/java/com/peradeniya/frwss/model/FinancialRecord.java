package com.peradeniya.frwss.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "financial_records") // Links to your pgAdmin table
public class FinancialRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Date is required") // FR-REQ-1.4
    private LocalDate date;

    @NotNull(message = "Amount is required") // FR-REQ-1.4
    private Double amount;

    @NotBlank(message = "Reference ID is required") // FR-REQ-1.4
    private String referenceId;

    // Standard Getters and Setters are needed here

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
}
