package name.eraxillan.financial_advisor;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.TreeMap;

public class PaymentCategory {
    //------------------------------------------------------------------------------------------
    // Static XML data
    // language -> name
    TreeMap<String, String> m_names;
    ArrayList<PaymentCategoryItem> m_items;
    //------------------------------------------------------------------------------------------
    // Runtime data
    BigDecimal m_categoryCharges;
//        float      m_categoryChargesPercent;
    //------------------------------------------------------------------------------------------

    public PaymentCategory() {
        m_names = new TreeMap<>();
        m_items = new ArrayList<>();

        m_categoryCharges = BigDecimal.ZERO;
    }

    public void addName(String language, String name) {
        m_names.put(language, name);
    }

    public void addItem(PaymentCategoryItem item) {
        m_items.add(item);
    }

    public String getName(String language) { return m_names.get(language); }
    public ArrayList<PaymentCategoryItem> targetFilters() { return m_items; }

    public BigDecimal getCategoryCharges() {
        return m_categoryCharges;
    }
    public float getCategoryChargesPercent(BigDecimal totalCharges) {
        if (m_categoryCharges.compareTo(BigDecimal.ZERO) == 0) {
            for (PaymentCategoryItem pci : m_items) {
                m_categoryCharges = m_categoryCharges.add(pci.getCharges());
            }
        }

        return (m_categoryCharges.divide(totalCharges, 2, RoundingMode.HALF_DOWN).floatValue()) * 100f;
    }
}
