package name.eraxillan.financial_advisor;

import android.support.v4.util.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.TreeMap;

enum CategoryFilterType { INVALID, STARTS_WITH, EQUALS }

// Category sub-item (e.g. "McDonald's" for "Cafe and restaurants" category)
public class PaymentCategoryItem {
    // Static XML data -------------------------------------------------------------------------
    // language -> name
    TreeMap<String, String> m_names;
    // <filter_type, target>
    ArrayList<Pair<CategoryFilterType, String>> m_targetFilters;
    //------------------------------------------------------------------------------------------
    // Runtime data
    BigDecimal m_charges;
    //------------------------------------------------------------------------------------------

    public PaymentCategoryItem() {
        m_names = new TreeMap<>();
        m_targetFilters = new ArrayList<>();

        m_charges = BigDecimal.ZERO;
    }

    public void addName(String language, String name) {
        m_names.put(language, name);
    }

    public void addFilter(Pair<CategoryFilterType, String> filter) {
        m_targetFilters.add(filter);
    }

    public String getName(String language) { return m_names.get(language); }
    public ArrayList<Pair<CategoryFilterType, String>> getFilters() {
        return m_targetFilters;
    }
    public BigDecimal getCharges() { return m_charges; }

    public void addOperation(BigDecimal sum) {
        m_charges = m_charges.add(sum);
    }
}
