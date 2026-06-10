package com.frwss.system.repository;

import com.frwss.system.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, String> {

    Optional<Payroll> findFirstByReferenceNoAndSalaryAndPaymentDate(
            String referenceNo,
            BigDecimal salary,
            LocalDate paymentDate
    );
}
