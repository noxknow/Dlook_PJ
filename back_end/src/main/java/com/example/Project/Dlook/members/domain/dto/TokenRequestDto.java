package com.example.Project.Dlook.members.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenRequestDto {

    private String accessToken;
    private String refreshToken;
}
