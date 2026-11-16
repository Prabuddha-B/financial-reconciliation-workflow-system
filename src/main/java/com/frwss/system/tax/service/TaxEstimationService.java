// -------------------------------------------------------------------------------
//       Calculates reference-only tax estimates using active tax configurations
// -------------------------------------------------------------------------------

package com.frwss.system.tax.service;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.security.SecurityContextAdapter;
import com.frwss.system.tax.dto.TaxEstimationRequestDto;
import com.frwss.system.tax.dto.TaxEstimationResponseDto;
import com.frwss.system.tax.model.TaxConfiguration;
import com.frwss.system.tax.repository.TaxConfigurationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaxEstimationService {

    private final TaxConfigurationRepository taxConfigurationRepository;
    private final AuditService auditService;
    private final SecurityContextAdapter securityContextAdapter;

    // Constructor
    public TaxEstimationService(
            TaxConfigurationRepository taxConfigurationRepository, AuditService auditService, SecurityContextAdapter securityContextAdapter) {
        this.taxConfigurationRepository = taxConfigurationRepository;
        this.auditService = auditService;
        this.securityContextAdapter = securityContextAdapter;
    }

    /**
     * Calculates the estimated tax and total amount using the active tax
     * configuration matching the requested module and category.
     * Records successful and failed estimation attempts in the audit log.
     *
     * @param request tax estimation input details
     * @return response containing the estimated tax calculation or error details
     */
    @Transactional
    public TaxEstimationResponseDto estimateTax(TaxEstimationRequestDto request) {

        // Initialize the response
        TaxEstimationResponseDto response = new TaxEstimationResponseDto();

        try {

            // Retrieve the applicable tax configuration
            TaxConfiguration configuration =
                    taxConfigurationRepository
                            .findActiveByModuleAndCategory(
                                    request.getModule(),
                                    request.getCategory()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "No active tax configuration found."
                                    ));

            // Extract the amount and tax rate
            BigDecimal amount = request.getAmount();
            BigDecimal taxRate = configuration.getTaxRate();

            // Calculate the estimated tax
            BigDecimal estimatedTax =
                    amount.multiply(taxRate).divide(
                                    BigDecimal.valueOf(100),
                                    2,
                                    RoundingMode.HALF_UP);

            // Calculate the total including tax
            BigDecimal total = amount.add(estimatedTax);

            // Populate the response DTO
            response.setModule(request.getModule());
            response.setCategory(request.getCategory());
            response.setOriginalAmount(amount);
            response.setTaxRate(taxRate);
            response.setEstimatedTax(estimatedTax);
            response.setTotalWithTax(total);

            // Identify the user
            String currentUser = securityContextAdapter.getCurrentUsername();

            response.setEstimatedBy(currentUser);

            // Record a successful estimation
            auditService.log(
                    AuditActionType.TAX_ESTIMATION,
                    "TaxEstimation",
                    request.getModule() + "-" + request.getCategory(),
                    currentUser,
                    "Reference-only tax estimation generated."
            );

            return response;

        } catch (Exception ex) {

            response.setModule(request.getModule());
            response.setCategory(request.getCategory());
            response.setOriginalAmount(request.getAmount());
            response.setTaxRate(BigDecimal.ZERO);
            response.setEstimatedTax(BigDecimal.ZERO);
            response.setTotalWithTax(request.getAmount());

            response.setEstimatedBy(securityContextAdapter.getCurrentUsername());

            response.setDisclaimer("ERROR : " + ex.getMessage());

            auditService.log(
                    AuditActionType.TAX_ESTIMATION_ERROR,
                    "TaxEstimation",
                    request.getModule() + "-" + request.getCategory(),
                    securityContextAdapter.getCurrentUsername(),
                    ex.getMessage()
            );

            return response;
        }
    }

    /**
     * Retrieves active tax rates for a module, using category names as map keys.
     *
     * @param module system module used to filter tax configurations
     * @return map containing category names and their corresponding tax rates
     */
    @Transactional
    public Map<String, BigDecimal> getTaxRatesByModule(String module) {

        Map<String, BigDecimal> taxRates = new HashMap<>();

        List<TaxConfiguration> configurations = taxConfigurationRepository.findActiveByModule(module);

        for (TaxConfiguration configuration : configurations) {
            taxRates.put(
                    configuration.getCategory(),
                    configuration.getTaxRate()
            );
        }

        return taxRates;
    }

    /**
     * Retrieves all currently active tax configurations.
     *
     * @return list of active tax configurations
     */
    @Transactional
    public List<TaxConfiguration> getAllActiveConfigurations() {

        return taxConfigurationRepository.findAllActive();
    }

    /**
     * Checks whether an active tax configuration exists for the specified
     * module and category.
     *
     * @param module system module
     * @param category tax category
     * @return true if an active configuration exists; otherwise false
     */
    @Transactional
    public boolean hasTaxConfiguration(String module, String category) {

        return taxConfigurationRepository
                .existsByModuleAndCategoryAndIsActiveTrue(
                        module,
                        category
                );
    }

    /**
     * Retrieves distinct module names associated with active tax configurations.
     *
     * @return list of available module names
     */
    @Transactional
    public List<String> getAvailableModules() {
        return taxConfigurationRepository.findAllActiveModules();
    }

    /**
     * Retrieves distinct category names associated with active tax configurations.
     *
     * @return list of available category names
     */
    @Transactional
    public List<String> getAvailableCategories() {
        return taxConfigurationRepository.findAllActiveCategories();
    }



}