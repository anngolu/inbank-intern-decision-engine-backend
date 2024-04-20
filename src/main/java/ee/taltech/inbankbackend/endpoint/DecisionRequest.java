package ee.taltech.inbankbackend.endpoint;

import ee.taltech.inbankbackend.common.Country;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Holds the request data of the REST endpoint
 */
@Getter
@AllArgsConstructor
public class DecisionRequest {
    private String personalCode;
    private Long loanAmount;
    private int loanPeriod;
    private Country countryCode;
}
