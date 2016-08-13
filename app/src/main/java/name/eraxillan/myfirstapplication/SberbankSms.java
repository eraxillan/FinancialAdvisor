package name.eraxillan.myfirstapplication;


import java.math.BigDecimal;
import java.util.Date;

class SberbankSms {
    private String m_cardId;
    private Date m_dateTime;
    private AccountOperation m_operation;
    private BigDecimal m_sum;
    private String m_target;
    private BigDecimal m_balance;

    SberbankSms() {
        m_cardId = "";
        m_dateTime = new Date(0);
        m_operation = AccountOperation.INVALID;
        m_sum = BigDecimal.ZERO;
        m_target = "";
        m_balance = BigDecimal.ZERO;
    }

    public boolean isValid() {
        if (m_cardId.isEmpty()) return false;
        if (m_dateTime.getTime() <= 0) return false;
        if (m_operation == AccountOperation.INVALID) return false;
        if (m_sum.compareTo(BigDecimal.ZERO) <= 0) return false;
        if (m_target.isEmpty()) return false;
        if (m_balance.compareTo(BigDecimal.ZERO) <= 0) return false;

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

    public BigDecimal getSum() {
        return m_sum;
    }
    public void setSum(BigDecimal aSum) {
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

    public BigDecimal getBalance() {
        return m_balance;
    }
    public void setBalance(BigDecimal aBalance) {
        // TODO: arg check
        m_balance = aBalance;
    }
}
