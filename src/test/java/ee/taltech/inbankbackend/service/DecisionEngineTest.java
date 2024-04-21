package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.common.Gender;
import com.github.vladislavgoltjajev.personalcode.exception.PersonalCodeException;
import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeGenerator;
import com.github.vladislavgoltjajev.personalcode.locale.latvia.LatvianPersonalCodeGenerator;
import com.github.vladislavgoltjajev.personalcode.locale.lithuania.LithuanianPersonalCodeGenerator;
import ee.taltech.inbankbackend.common.Country;
import ee.taltech.inbankbackend.config.AgeConstants;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class DecisionEngineTest {

    @InjectMocks
    private DecisionEngine decisionEngine;

    private String debtorPersonalCode;
    private String segment1PersonalCode;
    private String segment2PersonalCode;
    private String segment3PersonalCode;

    @BeforeEach
    void setUp() {
        debtorPersonalCode = "37605030299";
        segment1PersonalCode = "50307172740";
        segment2PersonalCode = "38411266610";
        segment3PersonalCode = "35006069515";
    }

    @Test
    void testDebtorPersonalCode() {
        assertThrows(NoValidLoanException.class,
                () -> decisionEngine.calculateApprovedLoan(debtorPersonalCode, 4000L, 12, Country.EE));
    }

    @Test
    void testSegment1PersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException {
        Decision decision = decisionEngine.calculateApprovedLoan(segment1PersonalCode, 4000L, 12, Country.EE);
        assertEquals(2000, decision.getLoanAmount());
        assertEquals(20, decision.getLoanPeriod());
    }

    @Test
    void testSegment2PersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException {
        Decision decision = decisionEngine.calculateApprovedLoan(segment2PersonalCode, 4000L, 12, Country.LT);
        assertEquals(3600, decision.getLoanAmount());
        assertEquals(12, decision.getLoanPeriod());
    }

    @Test
    void testSegment3PersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException {
        Decision decision = decisionEngine.calculateApprovedLoan(segment3PersonalCode, 4000L, 12, Country.EE);
        assertEquals(10000, decision.getLoanAmount());
        assertEquals(12, decision.getLoanPeriod());
    }

    @Test
    void testInvalidPersonalCode() {
        String invalidPersonalCode = "12345678901";
        assertThrows(InvalidPersonalCodeException.class,
                () -> decisionEngine.calculateApprovedLoan(invalidPersonalCode, 4000L, 12, Country.EE));
    }

    @Test
    void testInvalidLoanAmount() {
        Long tooLowLoanAmount = DecisionEngineConstants.MINIMUM_LOAN_AMOUNT - 1L;
        Long tooHighLoanAmount = DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT + 1L;

        assertThrows(InvalidLoanAmountException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, tooLowLoanAmount, 12, Country.EE));

        assertThrows(InvalidLoanAmountException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, tooHighLoanAmount, 12, Country.LT));
    }

    @Test
    void testInvalidLoanPeriod() {
        int tooShortLoanPeriod = DecisionEngineConstants.MINIMUM_LOAN_PERIOD - 1;
        int tooLongLoanPeriod = DecisionEngineConstants.MAXIMUM_LOAN_PERIOD + 1;

        assertThrows(InvalidLoanPeriodException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, 4000L, tooShortLoanPeriod, Country.EE));

        assertThrows(InvalidLoanPeriodException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, 4000L, tooLongLoanPeriod, Country.EE));
    }

    @Test
    void testFindSuitableLoanPeriod() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException {
        Decision decision = decisionEngine.calculateApprovedLoan(segment2PersonalCode, 2000L, 12, Country.LT);
        assertEquals(3600, decision.getLoanAmount());
        assertEquals(12, decision.getLoanPeriod());
    }

    @Test
    void testNoValidLoanFound() {
        assertThrows(NoValidLoanException.class,
                () -> decisionEngine.calculateApprovedLoan(debtorPersonalCode, 10000L, 60, Country.EE));
    }

    @Test
    void testInvalidLatvianPersonalCodeShouldFail() {
        String invalidPersonalCode = new LithuanianPersonalCodeGenerator().generateRandomPersonalCode();
        assertThrows(InvalidPersonalCodeException.class,
                () -> decisionEngine.calculateApprovedLoan(invalidPersonalCode, 4000L, 12, Country.LV));
    }

    @Test
    void testInvalidLithuanianPersonalCodeShouldFail() {
        String invalidPersonalCode = new LatvianPersonalCodeGenerator().generateRandomPersonalCode();
        assertThrows(InvalidPersonalCodeException.class,
                () -> decisionEngine.calculateApprovedLoan(invalidPersonalCode, 4000L, 12, Country.LT));
    }

    @Test
    void testEstonianUnderageShouldFail() throws PersonalCodeException, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        // given
        String underagePersonalCode = new EstonianPersonalCodeGenerator().generatePersonalCode(Gender.FEMALE, LocalDate.now());
        // when
        Decision decision = decisionEngine.calculateApprovedLoan(underagePersonalCode, 10000L, 60, Country.EE);
        // then
        assertEquals( "Loans are not offered to people under age 18.", decision.getErrorMessage());
    }

    @Test
    void testLatvianUnderageShouldFail() throws PersonalCodeException, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        //ToDo: Update Latvian PersonalCode library to generate ages further that 2017 year
        String underagePersonalCode = new LatvianPersonalCodeGenerator().generateLegacyPersonalCode(LocalDate.of(2016,12,3));

        Decision decision = decisionEngine.calculateApprovedLoan(underagePersonalCode, 10000L, 60, Country.LV);

        assertEquals( "Loans are not offered to people under age 18.", decision.getErrorMessage());
    }

    @Test
    void testLithuanianUnderageShouldFail() throws PersonalCodeException, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        String underagePersonalCode = new LithuanianPersonalCodeGenerator().generatePersonalCode(Gender.FEMALE, LocalDate.now());

        Decision decision = decisionEngine.calculateApprovedLoan(underagePersonalCode, 10000L, 60, Country.LT);

        assertEquals( "Loans are not offered to people under age 18.", decision.getErrorMessage());
    }

    @Test
    void testEstonianFemaleLifeTimeShouldFail() throws PersonalCodeException, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        String exceededLifeTimePersonalCode = new EstonianPersonalCodeGenerator().generatePersonalCode(Gender.FEMALE, LocalDate.of(1920,1,1));

        Decision decision = decisionEngine.calculateApprovedLoan(exceededLifeTimePersonalCode, 10000L, 60, Country.EE);

        assertEquals( "Your age exceeds the current expected lifetime in your country", decision.getErrorMessage());
    }

    @Test
    void testEstonianMaleLifeTimeShouldFail() throws PersonalCodeException, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        String exceededLifeTimePersonalCode = new EstonianPersonalCodeGenerator().generatePersonalCode(Gender.MALE, LocalDate.of(1920,1,1));

        Decision decision = decisionEngine.calculateApprovedLoan(exceededLifeTimePersonalCode, 10000L, 60, Country.EE);

        assertEquals( "Your age exceeds the current expected lifetime in your country", decision.getErrorMessage());
    }

    @Test
    void testLatvianLifeTimeShouldFail() throws PersonalCodeException, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        String exceededLifeTimePersonalCode = new LatvianPersonalCodeGenerator().generateLegacyPersonalCode(LocalDate.of(1920,1,1));

        Decision decision = decisionEngine.calculateApprovedLoan(exceededLifeTimePersonalCode, 10000L, 60, Country.LV);

        assertEquals( "Your age exceeds the current expected lifetime in your country", decision.getErrorMessage());
    }

    @Test
    void testLithuanianFemaleLifeTimeShouldFail() throws PersonalCodeException, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        String exceededLifeTimePersonalCode = new LithuanianPersonalCodeGenerator().generatePersonalCode(Gender.FEMALE, LocalDate.of(1920,1,1));

        Decision decision = decisionEngine.calculateApprovedLoan(exceededLifeTimePersonalCode, 10000L, 60, Country.LT);

        assertEquals( "Your age exceeds the current expected lifetime in your country", decision.getErrorMessage());
    }

    @Test
    void testLithuanianMaleLifeTimeShouldFail() throws PersonalCodeException, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        String exceededLifeTimePersonalCode = new LithuanianPersonalCodeGenerator().generatePersonalCode(Gender.MALE, LocalDate.of(1920,1,1));

        Decision decision = decisionEngine.calculateApprovedLoan(exceededLifeTimePersonalCode, 10000L, 60, Country.LT);

        assertEquals( "Your age exceeds the current expected lifetime in your country", decision.getErrorMessage());
    }

    @Test
    void testEstonianFemaleLoanPeriodShouldFail() throws PersonalCodeException, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        String nearlyExceededLifeTimePersonalCode = new EstonianPersonalCodeGenerator().generatePersonalCode(Gender.FEMALE, LocalDate.now().minusYears(AgeConstants.ESTONIAN_AVERAGE_FEMALE_LIFETIME_YEARS).plusMonths(1));

        Decision decision = decisionEngine.calculateApprovedLoan(nearlyExceededLifeTimePersonalCode, 10000L, 60, Country.EE);

        assertEquals( "Your age plus specified loan period exceeds expected "
                 + AgeConstants.ESTONIAN_AVERAGE_FEMALE_LIFETIME_YEARS
                 + " years life time in your country. Try to request smaller loan period", decision.getErrorMessage());
    }

    @Test
    void testEstonianMaleLoanPeriodShouldFail() throws PersonalCodeException, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        String nearlyExceededLifeTimePersonalCode = new EstonianPersonalCodeGenerator().generatePersonalCode(Gender.MALE, LocalDate.now().minusYears(AgeConstants.ESTONIAN_AVERAGE_MALE_LIFETIME_YEARS).plusMonths(1));

        Decision decision = decisionEngine.calculateApprovedLoan(nearlyExceededLifeTimePersonalCode, 10000L, 60, Country.EE);

        assertEquals( "Your age plus specified loan period exceeds expected "
                + AgeConstants.ESTONIAN_AVERAGE_MALE_LIFETIME_YEARS
                + " years life time in your country. Try to request smaller loan period", decision.getErrorMessage());
    }

    @Test
    void testLatvianLoanPeriodShouldFail() throws PersonalCodeException, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        String nearlyExceededLifeTimePersonalCode = new LatvianPersonalCodeGenerator().generateLegacyPersonalCode(LocalDate.now().minusYears(AgeConstants.LATVIAN_AVERAGE_LIFETIME_YEARS).plusMonths(1));

        Decision decision = decisionEngine.calculateApprovedLoan(nearlyExceededLifeTimePersonalCode, 10000L, 60, Country.LV);

        assertEquals( "Your age plus specified loan period exceeds expected "
                + AgeConstants.LATVIAN_AVERAGE_LIFETIME_YEARS
                + " years life time in your country. Try to request smaller loan period", decision.getErrorMessage());
    }

    @Test
    void testLithuanianFemaleLoanPeriodShouldFail() throws PersonalCodeException, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        String nearlyExceededLifeTimePersonalCode = new LithuanianPersonalCodeGenerator().generatePersonalCode(Gender.FEMALE, LocalDate.now().minusYears(AgeConstants.LITHUANIAN_AVERAGE_FEMALE_LIFETIME_YEARS).plusMonths(1));

        Decision decision = decisionEngine.calculateApprovedLoan(nearlyExceededLifeTimePersonalCode, 10000L, 60, Country.LT);

        assertEquals( "Your age plus specified loan period exceeds expected "
                + AgeConstants.LITHUANIAN_AVERAGE_FEMALE_LIFETIME_YEARS
                + " years life time in your country. Try to request smaller loan period", decision.getErrorMessage());
    }
    @Test
    void testLithuanianMaleLoanPeriodShouldFail() throws PersonalCodeException, InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        String nearlyExceededLifeTimePersonalCode = new LithuanianPersonalCodeGenerator().generatePersonalCode(Gender.MALE, LocalDate.now().minusYears(AgeConstants.LITHUANIAN_AVERAGE_MALE_LIFETIME_YEARS).plusMonths(1));

        Decision decision = decisionEngine.calculateApprovedLoan(nearlyExceededLifeTimePersonalCode, 10000L, 60, Country.LT);

        assertEquals( "Your age plus specified loan period exceeds expected "
                + AgeConstants.LITHUANIAN_AVERAGE_MALE_LIFETIME_YEARS
                + " years life time in your country. Try to request smaller loan period", decision.getErrorMessage());
    }
}

