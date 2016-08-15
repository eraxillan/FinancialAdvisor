package name.eraxillan.financial_advisor;

import android.util.Log;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

enum AccountOperation { INVALID, PAYMENT_FOR_SERVICES, MOBILE_BANK_PAYMENT, PURCHASE, REMITTANCE, CASH_WITHDRAWAL }

public class SberbankSmsParser {
    final private String MY_TAG = "SBRF Financial Advisor";

    final private String PAYMENT_FOR_SERVICES_OPERATION = "оплата услуг";
    final private String MOBILE_BANK_PAYMENT_OPERATION = "оплата Мобильного банка";
    final private String PURCHASE_OPERATION = "покупка";
    final private String REMITTANCE_OPERATION = "списание";
    final private String CASH_WITHDRAWAL_OPERATION = "выдача наличных";

    final private String BALANCE_STRING = "Баланс:";
    // оплата услуг:
    // ECMC9705 17.12.15 22:21 оплата услуг 6500р Баланс: 40722.95р
    // выдача наличных:
    // ECMC9705 21.12.15 19:30 выдача наличных 3000р ATM 10402536 Баланс: 37239.05р
    // оплата Мобильного банка:
    // ECMC9705 28.03.16 оплата Мобильного банка за 28/03/2016-27/04/2016 60р Баланс: 52147.33р

    AccountOperation parseOperationType(String aSmsText) {
        if (aSmsText.contains(PAYMENT_FOR_SERVICES_OPERATION)) return AccountOperation.PAYMENT_FOR_SERVICES;
        if (aSmsText.contains(MOBILE_BANK_PAYMENT_OPERATION)) return AccountOperation.MOBILE_BANK_PAYMENT;
        if (aSmsText.contains(PURCHASE_OPERATION)) return AccountOperation.PURCHASE;
        if (aSmsText.contains(REMITTANCE_OPERATION)) return AccountOperation.REMITTANCE;
        if (aSmsText.contains(CASH_WITHDRAWAL_OPERATION)) return AccountOperation.CASH_WITHDRAWAL;
        return AccountOperation.INVALID;
    }

    int parseOperationWordCount(String aSmsText) {
        if (aSmsText.contains(PAYMENT_FOR_SERVICES_OPERATION)) return 2;
        if (aSmsText.contains(MOBILE_BANK_PAYMENT_OPERATION)) return 3;
        if (aSmsText.contains(PURCHASE_OPERATION)) return 1;
        if (aSmsText.contains(REMITTANCE_OPERATION)) return 1;
        if (aSmsText.contains(CASH_WITHDRAWAL_OPERATION)) return 2;
        return (-1);
    }

    BigDecimal parseSumInRoubles(String sumStr) {
        if(!sumStr.endsWith("р")) return BigDecimal.ONE.negate();
        sumStr = sumStr.substring(0, sumStr.length() - 1);
        BigDecimal result;
        try {
            result = new BigDecimal(sumStr);
        }
        catch (NumberFormatException exc) {
            Log.e(MY_TAG, "Unable to parse " + sumStr + " as roubles sum");
            return BigDecimal.ONE.negate();
        }
        return result;
    }

    Date parseDateTime(String aDateTimeStr) {
        DateFormat formatter = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT,
                Locale.getDefault());
        Date result;
        try {
            result = formatter.parse(aDateTimeStr);
        }
        catch (ParseException exc) {
            Log.e(MY_TAG, "Unable to parse date from the string " + aDateTimeStr);
            return null;
        }
        return result;
    }

    String parseSberbankCard(String smsText) {
        // <credit card id> <short date> <short time> <action string> <sum in roubles> <shop> <current balance string>
        String result = "";

        // NOTE: all of the fields except the <shop> has no spaces
        String[] smsFields = smsText.split("\\s+");
        if (smsFields.length < 5) {
            Log.w(MY_TAG, "Invalid Sberbank SMS");
            return result;
        }

        // Parse credit card operation
        AccountOperation cardOperation = parseOperationType(smsText);
        if (cardOperation == AccountOperation.INVALID) {
            Log.w(MY_TAG, "Invalid account operation in Sberbank SMS (1)");
            return result;
        }

        // Read the credit ch
        result = smsFields[0];
        result = result.trim();
        return result;
    }

    public SberbankSms parseSberbankSms(String smsText) {
        SberbankSms result = new SberbankSms();

        // Sberbank SMS text example:
        // ECMC9705 07.09.15 11:31 покупка 659р PEREKRESTOK VLADYKINO Баланс: 5444.65р
        // ECMC9705 08.04.16 22:41 списание 27160р Баланс: 104170.09р
        // So, the format is:
        // <credit card id> <short date> <short time> <action string> <sum in roubles> <shop> <current balance string>

        // NOTE: all of the fields except the <shop> has no spaces
        String[] smsFields = smsText.split("\\s+");
        if (smsFields.length < 5) {
            Log.w(MY_TAG, "Invalid Sberbank SMS");
            return result;
        }

        int currentFieldIndex = 0;

        // Parse credit card operation
        AccountOperation cardOperation = parseOperationType(smsText);
        if (cardOperation == AccountOperation.INVALID) {
            Log.w(MY_TAG, "Invalid account operation in Sberbank SMS (1)");
            return result;
        }
        // FIXME: implement all operations
        if (cardOperation == AccountOperation.MOBILE_BANK_PAYMENT) return result;
        int operationWordCount = parseOperationWordCount(smsText);
        if (operationWordCount < 1) {
            Log.w(MY_TAG, "Invalid account operation in Sberbank SMS (2)");
            return result;
        }

        // Skip all credit cards except the specified one
        String creditCardId = smsFields[currentFieldIndex];
        creditCardId = creditCardId.trim();
        currentFieldIndex++;

        // TODO: Parse spend operation date and time
        // 1, 2 items
        String dateTimeStr = smsFields[currentFieldIndex] + " ";
        currentFieldIndex++;
        dateTimeStr += smsFields[currentFieldIndex];
        currentFieldIndex++;
        Date dateTime = parseDateTime(dateTimeStr);
        currentFieldIndex += operationWordCount;

        // Parse spent sum (required)
        // TODO: only roubles currency is currently supported
        BigDecimal sumRub = parseSumInRoubles(smsFields[currentFieldIndex]);
        // FIXME: handle mobile bank payment and "ОТКАЗ" here
        if (sumRub.compareTo(BigDecimal.ZERO) <= 0) {
            Log.w(MY_TAG, "Invalid sum in Sberbank SMS");
            return result;
        }
        currentFieldIndex++;

        // Parse target (optional)
        String target = "";
        for ( ; currentFieldIndex < smsFields.length; currentFieldIndex++) {
            if (smsFields[currentFieldIndex].compareTo(BALANCE_STRING) == 0) break;

            target += smsFields[currentFieldIndex];
            target += " ";
        }
        target = target.trim();
        // Skip balance string
        currentFieldIndex++;
        //if (target.isEmpty()) return result;
        // FIXME: костыль
        if (target.isEmpty() && cardOperation == AccountOperation.REMITTANCE) target = "PEREVOD";
        if (target.isEmpty()) {
            switch (cardOperation) {
                case REMITTANCE: target = "PEREVOD"; break;
                case PAYMENT_FOR_SERVICES: target = "USLUGI"; break;
                default:
                    Log.w(MY_TAG, "Excuse me what the fuck");
            }
        }

        // Parse balance
        BigDecimal balanceRub = parseSumInRoubles(smsFields[currentFieldIndex]);
        if (balanceRub.compareTo(BigDecimal.ZERO) <= 0) {
            Log.w(MY_TAG, "Invalid balance in Sberbank SMS");
            return result;
        }
        currentFieldIndex++;

        result.setOperation(cardOperation);
        result.setCardId(creditCardId);
        result.setDateTime(dateTime);
        result.setSum(sumRub);
        result.setTarget(target);
        result.setBalance(balanceRub);
        return result;
    }

    public static ArrayList<SberbankSms> getSmsRange(ArrayList<SberbankSms> smsList, Date from, Date to) {
        ArrayList<SberbankSms> result = new ArrayList<>();
        for (SberbankSms sms : smsList) {
            if (sms.getDateTime().after(from) && sms.getDateTime().before(to)) result.add(sms);
        }
        return result;
    }
}
