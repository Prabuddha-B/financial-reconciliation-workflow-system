// -------------------------------------------------------------------------------
//       Provides database access operations for tax configurations
// -------------------------------------------------------------------------------
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


    /**
     * Retrieves an active tax configuration matching the specified module
     * and category.
     */
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


    /**
     * Retrieves all active tax configurations associated with a specified module.
     */
    @Query("""
            SELECT t
            FROM TaxConfiguration t
            WHERE t.module = :module
              AND t.isActive = true
            """)
    List<TaxConfiguration> findActiveByModule(
            @Param("module") String module
    );

    /**
     * Retrieves every active tax configuration in the system.
     */
    @Query("""
            SELECT t
            FROM TaxConfiguration t
            WHERE t.isActive = true
            """)
    List<TaxConfiguration> findAllActive();

    /**
     * Checks whether an active configuration already exists for the given
     * module and category.
     */
    boolean existsByModuleAndCategoryAndIsActiveTrue(
            String module,
            String category
    );

    /**
     * Retrieves distinct module names from active tax configurations
     * in alphabetical order.
     */
    @Query("""
       SELECT DISTINCT t.module
       FROM TaxConfiguration t
       WHERE t.isActive = true
       ORDER BY t.module
       """)
    List<String> findAllActiveModules();


    /**
     * Retrieves distinct category names from active tax configurations
     * in alphabetical order.
     */
    @Query("""
       SELECT DISTINCT t.category
       FROM TaxConfiguration t
       WHERE t.isActive = true
       ORDER BY t.category
       """)
    List<String> findAllActiveCategories();

}