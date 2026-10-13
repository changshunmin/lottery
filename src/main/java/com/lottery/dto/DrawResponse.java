package com.lottery.dto;

import java.time.LocalDateTime;

public class DrawResponse {

    private Long id;
    private String name;
    private String icon;
    private int index;
    private String userName;
    private LocalDateTime createdAt;

    public DrawResponse() {}

    public DrawResponse(Long id, String name, String icon, int index, String userName, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.index = index;
        this.userName = userName;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public int getIndex() { return index; }
    public String getUserName() { return userName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
