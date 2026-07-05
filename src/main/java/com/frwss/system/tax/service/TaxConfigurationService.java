package com.frwss.system.tax.service;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.security.SecurityContextAdapter;
import com.frwss.system.tax.model.TaxConfiguration;
import com.frwss.system.tax.repository.TaxConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TaxConfigurationService {

    private final TaxConfigurationRepository taxConfigurationRepository;
    private final AuditService auditService;
    private final SecurityContextAdapter securityContextAdapter;

    public TaxConfigurationService(
            TaxConfigurationRepository taxConfigurationRepository,
            AuditService auditService,
            SecurityContextAdapter securityContextAdapter
    ) {
        this.taxConfigurationRepository = taxConfigurationRepository;
        this.auditService = auditService;
        this.securityContextAdapter = securityContextAdapter;
    }

    @Transactional(readOnly = true)
    public List<TaxConfiguration> getAllConfigurations() {
        return taxConfigurationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TaxConfiguration getConfiguration(Long id) {
        return taxConfigurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax configuration not found"));
    }

    @Transactional
    public void updateTaxRate(Long id, BigDecimal taxRate, String description) {

        TaxConfiguration configuration = getConfiguration(id);

        configuration.setTaxRate(taxRate);
        configuration.setDescription(description);

        taxConfigurationRepository.save(configuration);

        auditService.log(
                AuditActionType.MANUAL_MODIFICATION,
                "TaxConfiguration",
                id.toString(),
                securityContextAdapter.getCurrentUsername(),
                "Updated tax configuration for "
                        + configuration.getModule()
                        + " - "
                        + configuration.getCategory()
        );
    }
}