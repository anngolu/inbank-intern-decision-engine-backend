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
            // same as
//            if (isFemale) {
//                averageLifetimeYears = AgeConstants.ESTONIAN_AVERAGE_FEMALE_LIFETIME_YEARS;
//            } else {
//                averageLifetimeYears = AgeConstants.ESTONIAN_AVERAGE_MALE_LIFETIME_YEARS;
//            }
        } else if (countryCode == Country.LV) {
            age = latvianPersonalCodeParser.getAge(personalCode);
            averageLifetimeYears = AgeConstants.LATVIAN_AVERAGE_MALE_LIFETIME_YEARS;
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
        //ToDo: at this point all checks must pass
    }

    private static void validateLifetimeNotExceedingLoanPeriod(short averageLifetimeYears, Period age, int expectedLoanPayOffYears) throws AgeRestrictionException {
        if (averageLifetimeYears <= age.getYears()) {
            throw new AgeRestrictionException("Your age exceeds the current Estonian expected lifetime");
        }

        if (averageLifetimeYears <= expectedLoanPayOffYears) {
            throw new AgeRestrictionException("Your age plus specified loan period exceeds expected "
                    + averageLifetimeYears
                    + " years life time in Estonia. Try to request smaller loan period");
        }
    }

    //    public boolean isFemale(String personalCode, PersonalCodeParsers personalCodeParser) {
//        personalCodeParser.getGender();
//        return
//    }
//    public boolean checkIfPeriodNegative(Period periodBetweenLifetime) {
//        if (periodBetweenLifetime.isNegative()) {
//            //ToAsk: try catch
//            try {
//                changePeriodToPositive(periodBetweenLifetime);
//            } catch (AgeRestrictionException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        return periodBetweenLifetime.isNegative();
//    }
//    public boolean isYearNegative(Period periodBetweenLifetime) {
//        return periodBetweenLifetime.getYears() < 0;
//    }
//
//    public boolean isMonthNegative(Period periodBetweenLifetime) {
//        return periodBetweenLifetime.getMonths() < 0;
//    }
//
//    public boolean isDayNegative(Period periodBetweenLifetime) {
//        return periodBetweenLifetime.getDays() < 0;
//    }
//
//    public void changePeriodToPositive(Period periodBetweenLifetime) throws AgeRestrictionException {
//        if (isYearNegative(periodBetweenLifetime)) {
//            throw new AgeRestrictionException("Your age exceeds the current Estonian" +
//                    " expected lifetime");
//        }
//        if (isMonthNegative(periodBetweenLifetime)) {
//            int positiveMonth = periodBetweenLifetime.getMonths() * -1;
//            periodBetweenLifetime = periodBetweenLifetime.withMonths(positiveMonth);
//        }
//        if (isDayNegative(periodBetweenLifetime)) {
//            int positiveDay = periodBetweenLifetime.getDays() * -1;
//            periodBetweenLifetime = periodBetweenLifetime.withDays(positiveDay);
//        }
//    }
//
//    //check if customer is young enough to pay the loan
//    public void calculateLoanPeriod(int loanPeriod, Period periodBetweenLifetime) throws AgeRestrictionException {
//        toTotalMonthsFromDays(periodBetweenLifetime);
//        toTotalMonthsFromYears(periodBetweenLifetime);
//        int totalMonths = periodBetweenLifetime.getMonths();
//        if (totalMonths > 0) {
//            if (totalMonths - loanPeriod < 0) {
//                throw new AgeRestrictionException("Your age is too close to the current Estonian " +
//                        "expected lifetime. Try to request smaller loan period");
//            }
//        }
//        throw new AgeRestrictionException("Your age exceeds the current Estonian" +
//                " expected lifetime");
//
//
//    }
//
//    public void toTotalMonthsFromYears(Period periodBetweenLifetime) {
//        long totalMonthsFromYears = periodBetweenLifetime.toTotalMonths();
//        periodBetweenLifetime = periodBetweenLifetime.withMonths((int) totalMonthsFromYears);
//    }
//
//    public void toTotalMonthsFromDays(Period periodBetweenLifetime) {
//        int days = periodBetweenLifetime.getDays();
//        int daysInYear = 365;
//        if (days >= daysInYear) {
//            int monthsFromDays = days % daysInYear;
//            int totalMonth = monthsFromDays + periodBetweenLifetime.getMonths();
//            periodBetweenLifetime = periodBetweenLifetime.withMonths(totalMonth);
//        }
//    }
}
