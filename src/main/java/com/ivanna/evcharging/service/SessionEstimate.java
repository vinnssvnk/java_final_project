package com.ivanna.evcharging.service;

import java.math.BigDecimal;
import java.time.Duration;

/** Result of estimating a session without booking it. */
public record SessionEstimate(Duration duration, double energyKwh, BigDecimal price) {
}
