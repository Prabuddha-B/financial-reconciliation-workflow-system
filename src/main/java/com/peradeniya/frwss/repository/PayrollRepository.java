package com.frwss.system.repository;

import com.frwss.system.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRepository
        extends JpaRepository<Payroll,String> {
}