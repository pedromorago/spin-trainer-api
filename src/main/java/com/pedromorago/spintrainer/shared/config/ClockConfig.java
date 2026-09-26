package com.pedromorago.spintrainer.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj inyectable: los casos de uso sellan fechas (rangos guardados, intentos) y los tests fijan la hora. */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
