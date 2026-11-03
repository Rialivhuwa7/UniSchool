package co.za.unicard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {
    @Bean
    public Clock clock(@Value("${unischool.timezone:Africa/Johannesburg}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
