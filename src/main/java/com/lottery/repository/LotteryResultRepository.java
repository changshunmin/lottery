package com.lottery.repository;

import com.lottery.model.LotteryResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LotteryResultRepository extends JpaRepository<LotteryResult, Long> {

    @Modifying
    @Query("UPDATE LotteryResult r SET r.claimedBy = :claimedBy, r.claimedAt = :claimedAt " +
            "WHERE r.id = :id AND r.claimedBy IS NULL")
    int claimIfUnclaimed(@Param("id") Long id,
                         @Param("claimedBy") String claimedBy,
                         @Param("claimedAt") LocalDateTime claimedAt);

    // prize/user 为空时不过滤；奖品名按 "用户名 - 奖品名" 存储约定做前后缀匹配
    @Query("SELECT r FROM LotteryResult r WHERE " +
            "(:prize IS NULL OR r.prize LIKE CONCAT('% - ', :prize)) AND " +
            "(:user IS NULL OR r.prize LIKE CONCAT(:user, ' - %')) AND " +
            "(:start IS NULL OR r.createdAt >= :start) AND " +
            "(:end IS NULL OR r.createdAt <= :end)")
    Page<LotteryResult> searchPaged(@Param("prize") String prize,
                                    @Param("user") String user,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    Pageable pageable);
}
