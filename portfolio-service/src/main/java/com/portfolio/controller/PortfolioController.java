package com.portfolio.controller;

import com.portfolio.service.PortfolioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioRepository portfolioRepository;

    public PortfolioController(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getPortfolio(Authentication authentication) {
        String username = authentication.getName(); // Extracted from JWT subject
        List<Map<String, Object>> portfolio = portfolioRepository.getUserPortfolio(username);
        return ResponseEntity.ok(portfolio);
    }
}
