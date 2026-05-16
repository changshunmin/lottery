package com.lottery.repository;

import com.lottery.model.LotteryResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotteryResultRepository extends JpaRepository<LotteryResult, Long> {
}