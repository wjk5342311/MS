package com.example.musicapi.common.dto;

import lombok.Data;

@Data
public class CommonContextDto {

    private String account;

    private String roles;

    private String name;

    private String avatar;

    private String token;

    private String active;
}
