package com.lottery.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class PrizeConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String icon;

    private Double probability;

    private Integer sortOrder;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public PrizeConfig() {}

    public PrizeConfig(String name, String icon, Double probability, Integer sortOrder) {
        this.name = name;
        this.icon = icon;
        this.probability = probability;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Double getProbability() { return probability; }
    public void setProbability(Double probability) { this.probability = probability; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
