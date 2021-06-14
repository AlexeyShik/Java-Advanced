package info.kgeorgiy.ja.shik.bank;

import java.io.Serializable;

public abstract class AbstractAccount implements Account, Serializable {
    private final String id;
    private int amount;

    protected AbstractAccount(final String personId, final String accountId) {
        this.id = personId + ":" + accountId;
        amount = 0;
    }

    protected AbstractAccount(final String id, final int amount) {
        this.id = id;
        this.amount = amount;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public void setAmount(final int amount) {
        this.amount = amount;
    }

    @Override
    public void addAmount(final int amount) {
        this.amount += amount;
    }
}
