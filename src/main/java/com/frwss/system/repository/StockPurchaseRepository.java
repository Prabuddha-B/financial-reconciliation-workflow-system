package com.frwss.system.repository;

import com.frwss.system.model.StockPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockPurchaseRepository extends JpaRepository<StockPurchase, String> {
}
