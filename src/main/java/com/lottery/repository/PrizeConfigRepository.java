package com.lottery.repository;

import com.lottery.model.PrizeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrizeConfigRepository extends JpaRepository<PrizeConfig, Long> {
    List<PrizeConfig> findAllByOrderBySortOrderAsc();
}
