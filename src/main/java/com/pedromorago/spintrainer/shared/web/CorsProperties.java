package com.pedromorago.spintrainer.shared.web;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Frontend origins that can call the API from the browser (Vite locally and the Vercel domain). */
@ConfigurationProperties("spin-trainer.cors")
public record CorsProperties(
        @DefaultValue("http://localhost:5173") List<String> allowedOrigins) {}
