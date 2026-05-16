package com.lottery.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class LotteryResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String prize;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private String claimedBy;

    private LocalDateTime claimedAt;

    public LotteryResult() {}

    public LotteryResult(String prize) {
        this.prize = prize;
    }

    public Long getId() { return id; }
    public String getPrize() { return prize; }
    public void setPrize(String prize) { this.prize = prize; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getClaimedBy() { return claimedBy; }
    public void setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; }
}