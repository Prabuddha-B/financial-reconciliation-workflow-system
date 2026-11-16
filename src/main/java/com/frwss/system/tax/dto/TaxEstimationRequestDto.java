// -------------------------------------------------------------------------------
//       Transfers input data required for tax estimation requests
// -------------------------------------------------------------------------------
package com.frwss.system.tax.dto;

import java.math.BigDecimal;

public class TaxEstimationRequestDto {

    /** Identifies the system module associated with the tax estimation. */
    private String module;

    /** Identifies the tax category within the selected module. */
    private String category;

    /** Represents the original monetary amount used for tax estimation. */
    private BigDecimal amount;

//    Getters and Setters
    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}