package com.frwss.system.tax.repository;

import com.frwss.system.tax.model.TaxConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxConfigurationRepository extends JpaRepository<TaxConfiguration, Long> {


//     Find an active tax configuration by module and category.
    @Query("""
            SELECT t
            FROM TaxConfiguration t
            WHERE t.module = :module
              AND t.category = :category
              AND t.isActive = true
            """)
    Optional<TaxConfiguration> findActiveByModuleAndCategory(
            @Param("module") String module,
            @Param("category") String category
    );


//     Find all active configurations for a module.
    @Query("""
            SELECT t
            FROM TaxConfiguration t
            WHERE t.module = :module
              AND t.isActive = true
            """)
    List<TaxConfiguration> findActiveByModule(
            @Param("module") String module
    );

//    Find every active configuration.
    @Query("""
            SELECT t
            FROM TaxConfiguration t
            WHERE t.isActive = true
            """)
    List<TaxConfiguration> findAllActive();

//    Check whether a configuration already exists.
    boolean existsByModuleAndCategoryAndIsActiveTrue(
            String module,
            String category
    );
}