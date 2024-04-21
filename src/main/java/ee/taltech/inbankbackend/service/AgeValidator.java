package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.common.Gender;
import com.github.vladislavgoltjajev.personalcode.exception.PersonalCodeException;
import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeParser;
import com.github.vladislavgoltjajev.personalcode.locale.latvia.LatvianPersonalCodeParser;
import com.github.vladislavgoltjajev.personalcode.locale.lithuania.LithuanianPersonalCodeParser;
import ee.taltech.inbankbackend.common.Country;
import ee.taltech.inbankbackend.config.AgeConstants;
import ee.taltech.inbankbackend.exceptions.AgeRestrictionException;

import java.time.Period;
import java.util.Objects;

public class AgeValidator {

    private static final EstonianPersonalCodeParser estonianPersonalCodeParser = new EstonianPersonalCodeParser();
    private static final LatvianPersonalCodeParser latvianPersonalCodeParser = new LatvianPersonalCodeParser();
    private static final LithuanianPersonalCodeParser lithuanianPersonalCodeParser = new LithuanianPersonalCodeParser();


    public static void validate(String personalCode, Country countryCode, int loanPeriodMonths) throws AgeRestrictionException, PersonalCodeException {
        Objects.requireNonNull(personalCode);
        // parse personal code retrieving birthdate
        Period age;
        short averageLifetimeYears;
        if (countryCode == Country.LT) {
            age = lithuanianPersonalCodeParser.getAge(personalCode);
            boolean isFemale = lithuanianPersonalCodeParser.getGender(personalCode).equals(Gender.FEMALE);
            averageLifetimeYears = isFemale ? AgeConstants.LITHUANIAN_AVERAGE_FEMALE_LIFETIME_YEARS : AgeConstants.LITHUANIAN_AVERAGE_MALE_LIFETIME_YEARS;

        } else if (countryCode == Country.LV) {
            age = latvianPersonalCodeParser.getAge(personalCode);
            averageLifetimeYears = AgeConstants.LATVIAN_AVERAGE_LIFETIME_YEARS;
        } else {
            age = estonianPersonalCodeParser.getAge(personalCode);
            boolean isFemale = estonianPersonalCodeParser.getGender(personalCode).equals(Gender.FEMALE);
            averageLifetimeYears = isFemale ? AgeConstants.ESTONIAN_AVERAGE_FEMALE_LIFETIME_YEARS : AgeConstants.ESTONIAN_AVERAGE_MALE_LIFETIME_YEARS;
        }
        if (AgeConstants.UNDERAGE_PERIOD > age.getYears()) {
            throw new AgeRestrictionException("Loans are not offered to people under age 18.");
        }
        // calculate when person pays off the loan
        int expectedLoanPayOffYears = age.plusMonths(loanPeriodMonths).normalized().getYears();
        validateLifetimeNotExceedingLoanPeriod(averageLifetimeYears, age, expectedLoanPayOffYears);
    }

    private static void validateLifetimeNotExceedingLoanPeriod(short averageLifetimeYears, Period age, int expectedLoanPayOffYears) throws AgeRestrictionException {
        if (averageLifetimeYears <= age.getYears()) {
            throw new AgeRestrictionException("Your age exceeds the current expected lifetime in your country");
        }

        if (averageLifetimeYears <= expectedLoanPayOffYears) {
            throw new AgeRestrictionException("Your age plus specified loan period exceeds expected "
                    + averageLifetimeYears
                    + " years life time in your country. Try to request smaller loan period");
        }
    }
}
