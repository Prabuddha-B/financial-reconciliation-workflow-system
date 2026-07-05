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

    public TaxEstimationService(
            TaxConfigurationRepository taxConfigurationRepository, AuditService auditService, SecurityContextAdapter securityContextAdapter
    ) {
        this.taxConfigurationRepository = taxConfigurationRepository;
        this.auditService = auditService;
        this.securityContextAdapter = securityContextAdapter;
    }

    @Transactional
    public TaxEstimationResponseDto estimateTax(TaxEstimationRequestDto request) {

        TaxEstimationResponseDto response = new TaxEstimationResponseDto();

        try {

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

            BigDecimal amount = request.getAmount();

            BigDecimal taxRate = configuration.getTaxRate();

            BigDecimal estimatedTax =
                    amount.multiply(taxRate)
                            .divide(
                                    BigDecimal.valueOf(100),
                                    2,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal total =
                    amount.add(estimatedTax);

            response.setModule(request.getModule());
            response.setCategory(request.getCategory());
            response.setOriginalAmount(amount);
            response.setTaxRate(taxRate);
            response.setEstimatedTax(estimatedTax);
            response.setTotalWithTax(total);

            String currentUser = securityContextAdapter.getCurrentUsername();

            response.setEstimatedBy(currentUser);

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

    @Transactional
    public List<TaxConfiguration> getAllActiveConfigurations() {

        return taxConfigurationRepository.findAllActive();
    }

    @Transactional
    public boolean hasTaxConfiguration(String module, String category) {

        return taxConfigurationRepository
                .existsByModuleAndCategoryAndIsActiveTrue(
                        module,
                        category
                );
    }

}