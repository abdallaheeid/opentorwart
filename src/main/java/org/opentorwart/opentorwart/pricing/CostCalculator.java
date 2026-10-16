package org.opentorwart.opentorwart.pricing;

import org.opentorwart.opentorwart.config.PricingProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CostCalculator {

    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);

    private final PricingProperties props;

    public CostCalculator(PricingProperties props) {
        this.props = props;
    }

    /**
     * Cost in € for one request, or null if the model has no configured price.
     */
    public BigDecimal cost(String model, Integer promptTokens, Integer completionTokens) {
        if (props.models() == null || promptTokens == null || completionTokens == null) {
            return null;
        }
        PricingProperties.ModelPrice price = props.models().get(model);
        if (price == null) {
            return null;
        }

        BigDecimal promptCost = price.input().multiply(BigDecimal.valueOf(promptTokens));
        BigDecimal completionCost = price.output().multiply(BigDecimal.valueOf(completionTokens));
        return promptCost.add(completionCost).divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
    }
}