package com.section.front.commerce.dto;
import com.section.common.commerce.entity.FrontOrderClaimType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record FrontOrderClaimRequest(@NotNull FrontOrderClaimType claimType, @NotBlank @Size(max = 1000) String reason) {}
