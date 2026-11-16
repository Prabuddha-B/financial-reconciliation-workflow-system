// -------------------------------------------------------------------------------
//       Manages tax configurations and records administrative changes
// -------------------------------------------------------------------------------
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
import com.frwss.system.tax.exception.DuplicateTaxConfigurationException;

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

    /**
     * Retrieves all tax configurations, including active and inactive records.
     */
    @Transactional(readOnly = true)
    public List<TaxConfiguration> getAllConfigurations() {
        return taxConfigurationRepository.findAll();
    }

    /**
     * Retrieves a tax configuration by its ID.
     * @param id unique identifier of the tax configuration
     * @return the matching tax configuration
     */
    @Transactional(readOnly = true)
    public TaxConfiguration getConfiguration(Long id) {
        return taxConfigurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax configuration not found"));
    }

    /**
     * Updates the tax rate and description of an existing configuration
     * and records the modification in the audit log.
     */
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

    /**
     * Creates a new active tax configuration after checking for an existing
     * active configuration with the same module and category.
     */
    @Transactional
    public TaxConfiguration createConfiguration(TaxConfiguration configuration) {

        if (taxConfigurationRepository.existsByModuleAndCategoryAndIsActiveTrue(
                configuration.getModule(),
                configuration.getCategory())) {

            throw new DuplicateTaxConfigurationException(
                    "An active tax configuration already exists for this module and category."
            );
        }

        configuration.setIsActive(true);
        TaxConfiguration savedConfiguration = taxConfigurationRepository.save(configuration);

        auditService.log(
                AuditActionType.MANUAL_MODIFICATION,
                "TaxConfiguration",
                savedConfiguration.getId().toString(),
                securityContextAdapter.getCurrentUsername(),
                "Created tax configuration for "
                        + savedConfiguration.getModule()
                        + " - "
                        + savedConfiguration.getCategory()
        );

        return savedConfiguration;
    }

    /**
     * Deactivates an existing tax configuration without deleting its record
     * and records the change in the audit log.
     */
    @Transactional
    public void deactivateConfiguration(Long id) {

        TaxConfiguration configuration = getConfiguration(id);

        configuration.setIsActive(false);

        taxConfigurationRepository.save(configuration);

        auditService.log(
                AuditActionType.MANUAL_MODIFICATION,
                "TaxConfiguration",
                id.toString(),
                securityContextAdapter.getCurrentUsername(),
                "Deactivated tax configuration for "
                        + configuration.getModule()
                        + " - "
                        + configuration.getCategory()
        );
    }

    /**
     * Reactivates an inactive tax configuration after checking that no other
     * active configuration exists for the same module and category.
     */
    @Transactional
    public void activateConfiguration(Long id) {

        TaxConfiguration configuration = getConfiguration(id);

        if (taxConfigurationRepository.existsByModuleAndCategoryAndIsActiveTrue(
                configuration.getModule(),
                configuration.getCategory())) {

            throw new DuplicateTaxConfigurationException(
                    "Another active tax configuration already exists for this module and category."
            );
        }

        configuration.setIsActive(true);

        taxConfigurationRepository.save(configuration);

        auditService.log(
                AuditActionType.MANUAL_MODIFICATION,
                "TaxConfiguration",
                id.toString(),
                securityContextAdapter.getCurrentUsername(),
                "Activated tax configuration for "
                        + configuration.getModule()
                        + " - "
                        + configuration.getCategory()
        );
    }
}