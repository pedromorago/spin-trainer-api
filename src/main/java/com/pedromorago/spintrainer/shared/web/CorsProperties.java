package com.pedromorago.spintrainer.shared.web;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Orígenes del frontend que pueden llamar a la API desde el navegador (Vite en local y el dominio de Vercel). */
@ConfigurationProperties("spin-trainer.cors")
public record CorsProperties(
        @DefaultValue("http://localhost:5173") List<String> allowedOrigins) {}
