package name.eraxillan.financial_advisor;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    final private String MY_TAG = "SBRF Financial Advisor";
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    final private String m_currentLanguage = "ru";

    ArrayList<SberbankSms> m_sms = null;

    //----------------------------------------------------------------------------------------------
    public class PaymentCategoryAdapter extends ArrayAdapter<PaymentCategory> {
        BigDecimal m_totalCharges;

        public PaymentCategoryAdapter(Context context, ArrayList<PaymentCategory> users, BigDecimal totalCharges) {
            super(context, 0, users);

            m_totalCharges = totalCharges;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            PaymentCategory cat = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listviewitem, parent, false);
            }

            // Lookup view for data population
            TextView tvCategoryName = (TextView) convertView.findViewById(R.id.tvCategoryName);
            TextView tvCategoryCosts = (TextView) convertView.findViewById(R.id.tvCategoryCosts);

            // Adjust the red background brightness according to the costs total percent
            int redOpacity = (int) (0xFF * (cat.getCategoryChargesPercent(m_totalCharges) / 100f));
            convertView.setBackgroundColor(Color.argb(redOpacity, 0xFF, 0, 0));

            // Populate the data into the template view using the data object
            // FIXME: replace m_sms with current range
            String percentStr = (int) cat.getCategoryChargesPercent(m_totalCharges) == 0
                    ? "< 1" :
                    String.valueOf((int) cat.getCategoryChargesPercent(m_totalCharges));
            DecimalFormat df = (DecimalFormat) DecimalFormat.getCurrencyInstance(Locale.getDefault());

            tvCategoryName.setText(cat.getName(m_currentLanguage));
            tvCategoryCosts.setText(df.format(cat.getCategoryCharges() ) + " : " + percentStr + "%");

            // Return the completed view to render on screen
            return convertView;
        }
    }

    //----------------------------------------------------------------------------------------------

    private BigDecimal getTotalCosts(ArrayList<SberbankSms> smsList) {
        if (smsList == null) return BigDecimal.ZERO;

        BigDecimal result = BigDecimal.ZERO;
        for (SberbankSms sms : smsList) result = result.add(sms.getSum());
        return result;
    }

    //----------------------------------------------------------------------------------------------
    int DIALOG_START_DATE = 1;
    int m_startYear = -1;
    int m_startMonth = -1;
    int m_startDay = -1;
    int DIALOG_END_DATE = 2;
    int m_endYear = -1;
    int m_endMonth = -1;
    int m_endDay = -1;

    boolean startDateValid() {
        return (m_startYear > 0) && (m_startMonth >= 0) && (m_startDay > 0);
    }

    boolean endDateValid() {
        return (m_endYear > 0) && (m_endMonth >= 0) && (m_endDay > 0);
    }

    GregorianCalendar minStartDate(ArrayList<SberbankSms> sms) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(sms.get(sms.size() - 1).getDateTime());
        return cal;
    }

    GregorianCalendar maxEndDate(ArrayList<SberbankSms> sms) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(sms.get(0).getDateTime());
        return cal;
    }

    void reloadSmsData(ArrayList<SberbankSms> sms, int fromYear, int fromMonth, int fromDay, int toYear, int toMonth, int toDay) {
        // NOTE: month number in Java stdlib is zero-based
        GregorianCalendar gregCalFrom = new GregorianCalendar(fromYear, fromMonth, fromDay);
        GregorianCalendar gregCalTo = new GregorianCalendar(toYear, toMonth, toDay);
        ArrayList<SberbankSms> smsRange = SberbankSmsParser.getSmsRange(sms, gregCalFrom.getTime(), gregCalTo.getTime());

        // Create the adapter to convert the array to views
        ArrayList<PaymentCategory> categories = splitCostToCategories(smsRange);
        PaymentCategoryAdapter adapter = new PaymentCategoryAdapter(this, categories, getTotalCosts(smsRange));

        // Attach the adapter to a ListView
        ListView listView = (ListView) findViewById(R.id.lvMain);
        if (listView != null) {
            listView.setAdapter(adapter);
        }
    }

    DatePickerDialog.OnDateSetListener m_startDateCallback = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            m_startYear = year;
            m_startMonth = monthOfYear;
            m_startDay = dayOfMonth;

            GregorianCalendar calStart = new GregorianCalendar(m_startYear, m_startMonth, m_startDay);
            DateFormat df = DateFormat.getDateInstance(
                    DateFormat.SHORT,
                    Locale.getDefault());
            TextView tvDate = (TextView) findViewById(R.id.tvStartDate);
            if (tvDate != null) {
                tvDate.setText(df.format(calStart.getTime()));
            }

            reloadSmsData(m_sms, m_startYear, m_startMonth, m_startDay, m_endYear, m_endMonth, m_endDay);
        }
    };

    DatePickerDialog.OnDateSetListener m_endDateCallback = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            m_endYear = year;
            m_endMonth = monthOfYear;
            m_endDay = dayOfMonth;

            GregorianCalendar calEnd = new GregorianCalendar(m_endYear, m_endMonth, m_endDay);
            DateFormat df = DateFormat.getDateInstance(
                    DateFormat.SHORT,
                    Locale.getDefault());
            TextView tvDate = (TextView) findViewById(R.id.tvEndDate);
            if (tvDate != null) {
                tvDate.setText(df.format(calEnd.getTime()));
            }

            reloadSmsData(m_sms, m_startYear, m_startMonth, m_startDay, m_endYear, m_endMonth, m_endDay);
        }
    };

    public void onStartDateClick(View view) {
        showDialog(DIALOG_START_DATE);
    }

    public void onEndDateClick(View view) {
        showDialog(DIALOG_END_DATE);
    }

    protected Dialog onCreateDialog(int id) {
        if (!startDateValid()) {
            GregorianCalendar cal = minStartDate(m_sms);
            m_startYear = cal.get(Calendar.YEAR);
            m_startMonth = cal.get(Calendar.MONTH);
            m_startDay = cal.get(Calendar.DAY_OF_MONTH);
        }

        if (!endDateValid()) {
            GregorianCalendar cal = maxEndDate(m_sms);
            m_endYear = cal.get(Calendar.YEAR);
            m_endMonth = cal.get(Calendar.MONTH);
            m_endDay = cal.get(Calendar.DAY_OF_MONTH);
        }

        if (id == DIALOG_START_DATE) {
            return new DatePickerDialog(this, m_startDateCallback, m_startYear, m_startMonth, m_startDay);
        } else if (id == DIALOG_END_DATE) {
            return new DatePickerDialog(this, m_endDateCallback, m_endYear, m_endMonth, m_endDay);
        } else {
            Log.w(MY_TAG, "Unknown dialog requested");
        }
        return super.onCreateDialog(id);
    }

    //----------------------------------------------------------------------------------------------

    ArrayList<SberbankSms> readSberbankSms() {
        ArrayList<SberbankSms> result = new ArrayList<>();

        Cursor cursor;
        try {
            cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
        }
        catch (Exception exc)
        {
            Log.e(MY_TAG, exc.getMessage());
            return result;
        }

        SberbankSmsParser parser = new SberbankSmsParser();
        if ( cursor != null && cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                int addressColumnIndex = cursor.getColumnIndex("address");
                if(addressColumnIndex < 0) continue;

                int senderAddress = cursor.getInt(addressColumnIndex);
                if (senderAddress != 900) continue;

                int bodyColumnIndex = cursor.getColumnIndex("body");
                if (bodyColumnIndex < 0) continue;
                String smsText = cursor.getString(bodyColumnIndex);

                SberbankSms sms = parser.parseSberbankSms(smsText);
                if (sms.isValid()) {
                    result.add(sms);
                }
                else {
                    Log.w(MY_TAG, "Skipping invalid Sberbank SMS");
                }

            } while (cursor.moveToNext());
        } else {
            // empty box, no SMS
            Log.w(MY_TAG, "SMS Inbox is empty!");
        }

        Log.w(MY_TAG, "DONE!");
        return result;
    }

    // FIXME: make categories XML file editable in GUI
    ArrayList<PaymentCategory> splitCostToCategories(ArrayList<SberbankSms> smsList) {
        CategoryXmlParser xmlParser = new CategoryXmlParser();
        ArrayList<PaymentCategory> categories = xmlParser.loadCategoriesFromFile("sbrf_categories.xml");
        if (categories == null) {
            Log.e(MY_TAG, "Unable to load commodity categories from specified XML");
            return null;
        }

        ArrayList<String> categoryStrings = new ArrayList<>();

        PaymentCategoryItem remittanceCategoryItem = new PaymentCategoryItem();
        remittanceCategoryItem.addName("ru", "Денежные переводы");
        remittanceCategoryItem.addName("en", "Remittances");

        PaymentCategoryItem cashWithdrawalCategoryItem = new PaymentCategoryItem();
        cashWithdrawalCategoryItem.addName("ru", "Получение наличных");
        cashWithdrawalCategoryItem.addName("en", "Cash withdrawal");

        PaymentCategory remittancesCategory = new PaymentCategory();
        remittancesCategory.addName("ru", "Переводы и снятие наличных");
        remittancesCategory.addName("en", "Remittances and cash withdrawal");
        remittancesCategory.addItem(remittanceCategoryItem);
        remittancesCategory.addItem(cashWithdrawalCategoryItem);

        PaymentCategory servicesCategory = new PaymentCategory();
        servicesCategory.addName("ru", "Оплата услуг");
        servicesCategory.addName("en", "Payment for services");

        PaymentCategoryItem serviceCategoryItem = new PaymentCategoryItem();
        servicesCategory.addItem(serviceCategoryItem);

        for (SberbankSms sms : smsList) {
            if (sms.getOperation() == AccountOperation.PURCHASE) {
                boolean categoryRecognized = false;
                for (PaymentCategory category : categories) {
                    if (categoryRecognized) break;

                    for (PaymentCategoryItem categoryItem : category.targetFilters()) {
                        if (categoryRecognized) break;

                        for (Pair<CategoryFilterType, String> filter : categoryItem.getFilters()) {
                            if (categoryRecognized) break;

                            switch (filter.first) {
                                case STARTS_WITH: {
                                    if (sms.getTarget().startsWith(filter.second)) {
                                        categoryItem.addOperation(sms.getSum());
                                        categoryRecognized = true;
                                        break;
                                    }
                                    break;
                                }
                                case EQUALS: {
                                    if (sms.getTarget().compareTo(filter.second) == 0) {
                                        categoryItem.addOperation(sms.getSum());
                                        categoryRecognized = true;
                                        break;
                                    }
                                    break;
                                }
                                default: {
                                    Log.w(MY_TAG, "Skipping invalid category filter");
                                    break;
                                }
                            }
                        }
                    }
                }
                //----------------------------------------------------------------------------------
                // FIXME: Unknown purchase category
                if (!categoryRecognized) {
                    categoryStrings.add("PURCHASE: " + sms.getTarget());
                } else continue;
            } else if (sms.getOperation() == AccountOperation.REMITTANCE) {
                if (sms.getTarget().startsWith("PEREVOD") || sms.getTarget().startsWith("SBOL")) {
                    remittanceCategoryItem.addOperation(sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("ATM")) {
                    cashWithdrawalCategoryItem.addOperation(sms.getSum());
                    continue;
                }

                // FIXME: Unknown remittance category
                categoryStrings.add("REMITTANCE: " + sms.getTarget());
            } else if (sms.getOperation() == AccountOperation.CASH_WITHDRAWAL) {
                String smsTarget = sms.getTarget();
                Log.i(MY_TAG, smsTarget);

                if (sms.getTarget().startsWith("ATM")) {
                    cashWithdrawalCategoryItem.addOperation(sms.getSum());
                    continue;
                }

                // Unknown cash withdrawal operation
                categoryStrings.add("CASH: " + smsTarget);
            } else if (sms.getOperation() == AccountOperation.MOBILE_BANK_PAYMENT) {
                String smsTarget = sms.getTarget();
                Log.i(MY_TAG, smsTarget);

                // Unknown mobile bank payment operation
                categoryStrings.add("MOBILE BANK: " + smsTarget);
            } else if (sms.getOperation() == AccountOperation.PAYMENT_FOR_SERVICES) {
                String smsTarget = sms.getTarget();
                Log.i(MY_TAG, smsTarget);

                if (sms.getTarget().startsWith("USLUGI")) {
                    serviceCategoryItem.addOperation(sms.getSum());
                    continue;
                }

                // FIXME: find the appropriate category and add there instead of general one
                if (sms.getTarget().startsWith("BEELINE")) {
                    serviceCategoryItem.addOperation(sms.getSum());
                    continue;
                }

                if (sms.getTarget().startsWith("MEGAFON")) {
                    serviceCategoryItem.addOperation(sms.getSum());
                    continue;
                }

                // Unknown service operation
                categoryStrings.add("USLUGI: " + sms.getTarget());
                continue;
            }

            // Unknown category
            categoryStrings.add(sms.getTarget());
        }

        // Construct the data source
        categories.add(remittancesCategory);
        categories.add(servicesCategory);

        //------------------------------------------------------------------------------------------
        // Some assertions: all category charges percent must be equal to 100%
        float totalCategoriesChargePercent = 0.0f;
        for (PaymentCategory cat : categories) {
            totalCategoriesChargePercent += cat.getCategoryChargesPercent(getTotalCosts(smsList));
        }
        if (Math.abs(totalCategoriesChargePercent - 100) > 1.0f) {
            Log.e(MY_TAG, "Total sum of user-defined category costs part give " + totalCategoriesChargePercent + "% instead of 100%");
        }
        if (categoryStrings.size() > 0) {
            Log.e(MY_TAG, "Unknown categories detected:");
            for (String cat : categoryStrings) Log.e(MY_TAG, cat);
            Log.e(MY_TAG, "----------------------------");
        }
        //------------------------------------------------------------------------------------------

        return categories;
    }

    ArrayList<String> parseCreditCardNames() {
        HashSet<String> creditCardNames = new HashSet<>();
        for (SberbankSms sms : m_sms )  creditCardNames.add(sms.getCardId());
        ArrayList<String> creditCardNameList = new ArrayList<>();
        for (String cardId : creditCardNames) creditCardNameList.add(cardId);
        return creditCardNameList;
    }

    void processData() {
        if (m_sms != null) return;

        Log.i(MY_TAG, "Loading SMS data...");
        m_sms = readSberbankSms();
        Log.i(MY_TAG, "SMS data was successfully loaded, " + m_sms.size() + " SMS found");

        Log.i(MY_TAG, "Sorting SMS data by target...");

        // Fill the credit card list
        ArrayList<String> creditCardNameList = parseCreditCardNames();
        ArrayAdapter<String> cardIdAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, creditCardNameList);
        Spinner cmbCards = (Spinner) findViewById(R.id.cmbCards);
        if (cmbCards != null) {
            cmbCards.setAdapter(cardIdAdapter);
        }

        // Create the adapter to convert the array to views
        ArrayList<PaymentCategory> categories = splitCostToCategories(m_sms);
        PaymentCategoryAdapter adapter = new PaymentCategoryAdapter(this, categories, getTotalCosts(m_sms));
        // Attach the adapter to a ListView
        ListView listView = (ListView) findViewById(R.id.lvMain);
        if (listView != null) {
            listView.setAdapter(adapter);
        }

        Log.i(MY_TAG, "SMS data sorting done");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        // API Level 23 introduces new Runtime Permissions system:
        // each app now needs to check permission each time it need it, not the one time during install
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") != PackageManager.PERMISSION_GRANTED
         || ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_SMS", "android.permission.READ_EXTERNAL_STORAGE"}, REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }

        // We already have the permission to read SMS, so do it now
        processData();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    // Now we can read SMS Inbox dir
                    processData();
                } else {
                    // Permission Denied
                    Toast.makeText(this, "READ_SMS Permission was denied by the User", Toast.LENGTH_SHORT).show();
                }
                break;
            default: super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
