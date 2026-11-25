package tn.iset.marina.notification_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Seuils d'alerte configurables (application.yml ou variables d'environnement). */
@ConfigurationProperties(prefix = "smart-marina.notifications.thresholds")
public record NotificationThresholds(BigDecimal electricityKwh, BigDecimal waterM3) {

    public BigDecimal forType(String type) {
        return switch (type) {
            case "ELECTRICITY" -> electricityKwh;
            case "WATER" -> waterM3;
            default -> null;
        };
    }
}
