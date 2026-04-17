package com.peradeniya.frwss.repository;

import com.peradeniya.frwss.model.StockPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockPurchaseRepository extends JpaRepository<StockPurchase, String> {
}
