package name.eraxillan.myfirstapplication;

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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class MyActivity extends AppCompatActivity {
    final private String MY_TAG = "MyApp";
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    //----------------------------------------------------------------------------------------------

    // Категория затрат
    class CostsCategory {
        String m_displayName;   // User-friendly название категории
        float m_userTotalCosts;     // Общие затраты пользователя (на все категории) за весь период

        float m_totalCosts;       // Сколько всего денег потрачено на данную категорию товаров/услуг
        float m_totalPercent;   // Какой % от общих расходов за период занимает указанная категория
        // User-friendly список получателей и общих затрат на них (например, McDonalds, 5400р)
        TreeMap<String, Float> m_targets;

        public CostsCategory(String displayName, float userTotalCosts) {
            m_displayName = displayName;
            m_userTotalCosts = userTotalCosts;

            m_targets = new TreeMap<>();
        }

        public void addTarget(String displayName, float costs) {
            if (!m_targets.containsKey(displayName)) m_targets.put(displayName, costs);
            else m_targets.put(displayName, m_targets.get(displayName) + costs);
        }

        public String getDisplayName() { return m_displayName; }

        public float getTotalCosts() {
            if (m_totalCosts > 0) return m_totalCosts;

            for (Map.Entry<String, Float> entry : m_targets.entrySet()) {
                m_totalCosts += entry.getValue();
            }
            m_totalPercent = (m_totalCosts / m_userTotalCosts)*100f;
            return m_totalCosts;
        }

        public float getTotalCostsPercent() {
            getTotalCosts();
            return m_totalPercent;
        }
    }

    public class CostsCategoryAdapter extends ArrayAdapter<CostsCategory> {
        public CostsCategoryAdapter(Context context, ArrayList<CostsCategory> users) {
            super(context, 0, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            CostsCategory cat = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listviewitem, parent, false);
            }

            // Lookup view for data population
            TextView tvCategoryName = (TextView) convertView.findViewById(R.id.tvCategoryName);
            TextView tvCategoryCosts = (TextView) convertView.findViewById(R.id.tvCategoryCosts);

            // Adjust the red background brightness according to the costs total percent
            int redOpacity = (int) (0xFF * (cat.getTotalCostsPercent() / 100f));
            convertView.setBackgroundColor(Color.argb(redOpacity, 0xFF, 0, 0));

            // Populate the data into the template view using the data object
            String percentStr = (int) cat.getTotalCostsPercent() == 0 ? "< 1" : String.valueOf((int) cat.getTotalCostsPercent());
            DecimalFormat df = (DecimalFormat) DecimalFormat.getCurrencyInstance(Locale.getDefault());

            tvCategoryName.setText(cat.getDisplayName());
            tvCategoryCosts.setText(df.format(cat.getTotalCosts()) + " : " + percentStr + "%");

            // Return the completed view to render on screen
            return convertView;
        }
    }

    //----------------------------------------------------------------------------------------------

    ArrayList<SberbankSms> m_sms = null;

    private float getTotalCosts(ArrayList<SberbankSms> smsList) {
        if (smsList == null) return 0f;

        float result = 0f;
        for (SberbankSms sms : smsList) result += sms.getSum();
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
        GregorianCalendar gregCalTo   = new GregorianCalendar(toYear,   toMonth,   toDay  );
        ArrayList<SberbankSms> smsRange = getSmsRange(sms, gregCalFrom.getTime(), gregCalTo.getTime());

        // Create the adapter to convert the array to views
        ArrayList<CostsCategory> categories = splitCostToCategories(smsRange);
        CostsCategoryAdapter adapter = new CostsCategoryAdapter(this, categories);

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
            DatePickerDialog tpd = new DatePickerDialog(this, m_startDateCallback, m_startYear, m_startMonth, m_startDay);
            return tpd;
        } else if (id == DIALOG_END_DATE) {
            DatePickerDialog tpd = new DatePickerDialog(this, m_endDateCallback, m_endYear, m_endMonth, m_endDay);
            return tpd;
        } else {
            Log.w(MY_TAG, "Unknown dialog requested");
        }
        return super.onCreateDialog(id);
    }

    //----------------------------------------------------------------------------------------------

    class SberbankSms {
        private String m_cardId;
        private Date m_dateTime;
        private AccountOperation m_operation;
        private float m_sum;
        private String m_target;
        private float m_balance;

        SberbankSms() {
            m_cardId = "";
            m_dateTime = new Date(0);
            m_operation = AccountOperation.INVALID;
            m_sum = 0;
            m_target = "";
            m_balance = 0;
        }

        public boolean isValid() {
            if (m_cardId.isEmpty()) return false;
            if (m_dateTime.getTime() <= 0) return false;
            if (m_operation == AccountOperation.INVALID) return false;
            if (m_sum <= 0) return false;
            if (m_target.isEmpty()) return false;
            if (m_balance <= 0) return false;

            return true;
        }

        public String getCardId() {
            return m_cardId;
        }
        public void setCardId(String aCardId) {
            // TODO: arg check
            m_cardId = aCardId;
        }

        public Date getDateTime() {
            return m_dateTime;
        }
        public void setDateTime(Date aDateTime) {
            // TODO: arg check
            m_dateTime = aDateTime;
        }

        public AccountOperation getOperation() {
            return m_operation;
        }
        public void setOperation(AccountOperation anOperation) {
            // TODO: arg check
            m_operation = anOperation;
        }

        public float getSum() {
            return m_sum;
        }
        public void setSum(float aSum) {
            // TODO: arg check
            m_sum = aSum;
        }

        public String getTarget() {
            return m_target;
        }
        public void setTarget(String aTarget) {
            // TODO: arg check
            m_target = aTarget;
        }

        public float getBalance() {
            return m_balance;
        }
        public void setBalance(float aBalance) {
            // TODO: arg check
            m_balance = aBalance;
        }
    }

    ArrayList<SberbankSms> readSberbankSms() {
        ArrayList<SberbankSms> result = new ArrayList<SberbankSms>();

        Cursor cursor;
        try {
            cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
        }
        catch (Exception exc)
        {
            Log.e(MY_TAG, exc.getMessage());
            return result;
        }

        if ( cursor != null && cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                int addressColumnIndex = cursor.getColumnIndex("address");
                if(addressColumnIndex < 0) continue;

                int senderAddress = cursor.getInt(addressColumnIndex);
                if (senderAddress != 900) continue;

                int bodyColumnIndex = cursor.getColumnIndex("body");
                if (bodyColumnIndex < 0) continue;
                String smsText = cursor.getString(bodyColumnIndex);

                SberbankSms sms = parseSberbankSms(smsText);
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

    ArrayList<SberbankSms> getSmsRange(ArrayList<SberbankSms> smsList, Date from, Date to) {
        ArrayList<SberbankSms> result = new ArrayList<>();
        for (SberbankSms sms : smsList) {
            if (sms.getDateTime().after(from) && sms.getDateTime().before(to)) result.add(sms);
        }
        return result;
    }

    // FIXME: move categories data to external XML-file and make it editable in GUI
    ArrayList<CostsCategory> splitCostToCategories(ArrayList<SberbankSms> smsList) {
        ArrayList<String> categoryStrings = new ArrayList<>();
        CostsCategory basicCommodities = new CostsCategory("Товары первой необходимости", getTotalCosts(smsList));
        CostsCategory medicamentCommodities = new CostsCategory("Лекарства", getTotalCosts(smsList));
        CostsCategory treatmentCommodities = new CostsCategory("Лечение", getTotalCosts(smsList));
        CostsCategory transportCommodities = new CostsCategory("Транспорт", getTotalCosts(smsList));
        CostsCategory communicationCommodities = new CostsCategory("Интернет и связь", getTotalCosts(smsList));
        CostsCategory clothingShoesCommodities = new CostsCategory("Одежда и обувь", getTotalCosts(smsList));
        CostsCategory nonEssentialsCommodities = new CostsCategory("Второстепенные товары", getTotalCosts(smsList));
        CostsCategory publicCateringCommodities = new CostsCategory("Общепит", getTotalCosts(smsList));
        CostsCategory remittances = new CostsCategory("Переводы, снятие наличных", getTotalCosts(smsList));
        CostsCategory services = new CostsCategory("Оплата различных услуг", getTotalCosts(smsList));

        for (SberbankSms sms : smsList) {
            if (sms.getOperation() == AccountOperation.PURCHASE) {
                // Товары первой необходимости
                if (sms.getTarget().startsWith("LUG DA POLE ")) {
                    basicCommodities.addTarget("Избёнка", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("VKUSVILL ")) {
                    basicCommodities.addTarget("ВкусВилл", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("ATAK ")) {
                    basicCommodities.addTarget("Атак", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("BILLA ")) {
                    basicCommodities.addTarget("Билла", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("MAGNIT ")) {
                    basicCommodities.addTarget("Магнит", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("PYATEROCHKA ")) {
                    basicCommodities.addTarget("Пятёрочка", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("POKUPAY ")) {
                    basicCommodities.addTarget("Покупай! 24ч", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("PEREKRESTOK ")) {
                    basicCommodities.addTarget("Перекрёсток", sms.getSum());
                    continue;
                }
                if (sms.getTarget().compareTo("DA") == 0) {
                    basicCommodities.addTarget("Да!", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("ENGELSA") || sms.getTarget().startsWith("VOSKRESENSK ENGELSA")) {
                    basicCommodities.addTarget("Куриный дом", sms.getSum());
                    continue;
                }
                if (sms.getTarget().compareTo("VOSKRESENSKHLEB") == 0) {
                    basicCommodities.addTarget("ВоскресенскХлеб", sms.getSum());
                    continue;
                }
                if (sms.getTarget().compareTo("OOO VITL") == 0) {
                    basicCommodities.addTarget("Мини-универсам во Владыкино", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("MYASNAYA TOCHKA")) {
                    basicCommodities.addTarget("Мясная точка", sms.getSum());
                    continue;
                }
                // FIXME: "Южный Двор" shop have no name in SMS text
                if (sms.getTarget().startsWith("ZHULEBINO Moskva, ul P")) {
                    basicCommodities.addTarget("Южный Двор", sms.getSum());
                    continue;
                }
                //----------------------------------------------------------------------------------
                // Лекарства
                if (sms.getTarget().compareTo("APTEKA RIGLA") == 0) {
                    medicamentCommodities.addTarget("Аптека Ригла", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("NEOFARM ")) {
                    medicamentCommodities.addTarget("Неофарм, Воскресенск", sms.getSum());
                    continue;
                }
                if (sms.getTarget().compareTo("APTEKA-PLUS") == 0) {
                    medicamentCommodities.addTarget("Аптека +", sms.getSum());
                    continue;
                }
                if (sms.getTarget().contains("APTEKA")) {
                    medicamentCommodities.addTarget("Аптека (другая)", sms.getSum());
                    continue;
                }
                //----------------------------------------------------------------------------------
                // Лечение
                if (sms.getTarget().compareTo("OOO ZUBR") == 0) {
                    treatmentCommodities.addTarget("Зубр, стоматология", sms.getSum());
                    continue;
                }
                if (sms.getTarget().compareTo("KLINIKA UROLOGII") == 0) {
                    treatmentCommodities.addTarget("Клиника урологии", sms.getSum());
                    continue;
                }
                //----------------------------------------------------------------------------------
                // Транспорт
                if (sms.getTarget().compareTo("WWW.RZD.RU") == 0) {
                    transportCommodities.addTarget("РЖД", sms.getSum());
                    continue;
                }
                //----------------------------------------------------------------------------------
                // Интернет и связь
                if (sms.getTarget().compareTo("NETBYNET.RU") == 0) {
                    communicationCommodities.addTarget("Netbynet", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("MEGAFON")) {
                    communicationCommodities.addTarget("Мегафон", sms.getSum());
                    continue;
                }
                if (sms.getTarget().compareTo("BEELINE") == 0) {
                    communicationCommodities.addTarget("Билайн", sms.getSum());
                    continue;
                }
                //----------------------------------------------------------------------------------
                // Одежда и обувь
                if (sms.getTarget().startsWith("RESERVED RE")) {
                    clothingShoesCommodities.addTarget("Reserved", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("\"NEXT\"")) {
                    clothingShoesCommodities.addTarget("Next", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("\"H&M")) {
                    clothingShoesCommodities.addTarget("H&M", sms.getSum());
                    continue;
                }
                //----------------------------------------------------------------------------------
                // Второстепенные товары
                if (sms.getTarget().startsWith("IKEA DOM")) {
                    nonEssentialsCommodities.addTarget("Икеа", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("DECATHLON")) {
                    nonEssentialsCommodities.addTarget("Декатлон", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("DNS")) {
                    nonEssentialsCommodities.addTarget("DNS", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("LETUAL")) {
                    nonEssentialsCommodities.addTarget("Ле'туаль", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("IL PERSONA")) {
                    nonEssentialsCommodities.addTarget("Persona Labs", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("CLOUD*LINGUALEO.COM")) {
                    nonEssentialsCommodities.addTarget("Ле'туаль", sms.getSum());
                    continue;
                }
                //----------------------------------------------------------------------------------
                // Кафе и рестораны
                if (sms.getTarget().startsWith("MCDONALDS")) {
                    publicCateringCommodities.addTarget("McDonalds", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("KFS") || sms.getTarget().startsWith("KFC")) {
                    publicCateringCommodities.addTarget("KFC", sms.getSum());
                    continue;
                }
                if (sms.getTarget().compareTo("IKEA DOM 4RESTAURANT") == 0) {
                    publicCateringCommodities.addTarget("Ресторан Икеа", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("MY-")) {
                    publicCateringCommodities.addTarget("Му-му", sms.getSum());
                    continue;
                }
                //----------------------------------------------------------------------------------
                // Unknown purchase category
                categoryStrings.add("PURCHASE: " + sms.getTarget());
            } else if (sms.getOperation() == AccountOperation.REMITTANCE) {
                if (sms.getTarget().startsWith("PEREVOD") || sms.getTarget().startsWith("SBOL")) {
                    remittances.addTarget("Денежные переводы", sms.getSum());
                    continue;
                }
                if (sms.getTarget().startsWith("ATM")) {
                    remittances.addTarget("Снятие наличных", sms.getSum());
                    continue;
                }

                // Unknown remittance category
                categoryStrings.add("REMITTANCE: " + sms.getTarget());
            } else if (sms.getOperation() == AccountOperation.CASH_WITHDRAWAL) {
                String smsTarget = sms.getTarget();
                Log.i(MY_TAG, smsTarget);

                if (sms.getTarget().startsWith("ATM")) {
                    remittances.addTarget("Снятие наличных", sms.getSum());
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

                if (sms.getTarget().startsWith("BEELINE")) {
                    communicationCommodities.addTarget("Билайн", sms.getSum());
                    continue;
                }

                if (sms.getTarget().startsWith("MEGAFON")) {
                    communicationCommodities.addTarget("Мегафон", sms.getSum());
                    continue;
                }

                if (sms.getTarget().startsWith("USLUGI")) {
                    services.addTarget("Другие услуги", sms.getSum());
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
        ArrayList<CostsCategory> categories = new ArrayList<>();
        categories.add(basicCommodities);
        categories.add(medicamentCommodities);
        categories.add(treatmentCommodities);
        categories.add(transportCommodities);
        categories.add(communicationCommodities);
        categories.add(clothingShoesCommodities);
        categories.add(nonEssentialsCommodities);
        categories.add(publicCateringCommodities);
        categories.add(remittances);
        categories.add(services);

        //------------------------------------------------------------------------------------------
        // Some assertions
        float totalCostsPercent = basicCommodities.getTotalCostsPercent()
                + medicamentCommodities.getTotalCostsPercent()
                + treatmentCommodities.getTotalCostsPercent()
                + transportCommodities.getTotalCostsPercent()
                + communicationCommodities.getTotalCostsPercent()
                + clothingShoesCommodities.getTotalCostsPercent()
                + nonEssentialsCommodities.getTotalCostsPercent()
                + publicCateringCommodities.getTotalCostsPercent()
                + remittances.getTotalCostsPercent()
                + services.getTotalCostsPercent();
        if (Math.abs(totalCostsPercent - 100) > 1.0f) {
            Log.e(MY_TAG, "Total sum of category costs part give " + totalCostsPercent + "% instead of 100");
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
        ArrayList<CostsCategory> categories = splitCostToCategories(m_sms);
        CostsCategoryAdapter adapter = new CostsCategoryAdapter(this, categories);
        // Attach the adapter to a ListView
        ListView listView = (ListView) findViewById(R.id.lvMain);
        if (listView != null) {
            listView.setAdapter(adapter);
        }

        Log.i(MY_TAG, "SMS data sorting done");
    }

    private enum AccountOperation { INVALID, PAYMENT_FOR_SERVICES, MOBILE_BANK_PAYMENT, PURCHASE, REMITTANCE, CASH_WITHDRAWAL }
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

    float parseSumInRoubles(String sumStr) {
        if(!sumStr.endsWith("р")) return (-1);
        sumStr = sumStr.substring(0, sumStr.length() - 1);
        float result;
        try {
            result = Float.parseFloat(sumStr);
        }
        catch (NumberFormatException exc) {
            Log.e(MY_TAG, "Unable to parse " + sumStr + " as roubles sum");
            return (-1);
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

    SberbankSms parseSberbankSms(String smsText) {
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
        float sumRub = parseSumInRoubles(smsFields[currentFieldIndex]);
        // FIXME: handle mobile bank payment and "ОТКАЗ" here
        if (sumRub < 0) {
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
        float balanceRub = parseSumInRoubles(smsFields[currentFieldIndex]);
        if (balanceRub < 0) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        // API Level 23 introduces new Runtime Permissions system:
        // each app now needs to check permission each time it need it, not the one time during install
        if(ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }

        // We already have the permission to read SMS, so do it now
        processData();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    // Now we can read SMS Inbox dir
                    processData();
                } else {
                    // Permission Denied
                    Toast.makeText(this, "READ_SMS Permission was denied by the User", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
