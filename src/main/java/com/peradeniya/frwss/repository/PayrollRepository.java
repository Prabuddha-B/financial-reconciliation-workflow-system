package com.peradeniya.frwss.repository;

import com.peradeniya.frwss.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, String> {
}
