package com.pms.appointment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "pms.appointment")
public class AppointmentProperties {

  private int durationMinutes = 60;
}
