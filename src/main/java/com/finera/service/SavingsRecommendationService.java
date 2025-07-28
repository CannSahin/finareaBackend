package com.finera.service;

import com.finera.dto.SavingsRequestDto;
import com.finera.dto.SavingsResponseDto;
import com.finera.service.ai.AiProvider; // Enum import

public interface SavingsRecommendationService {

    SavingsResponseDto getRecommendations(SavingsRequestDto request);

}