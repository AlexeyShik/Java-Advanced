package info.kgeorgiy.ja.shik.bank;

public class RemoteAccount extends AbstractAccount {

    public RemoteAccount(final String personId, final String accountId) {
        super(personId, accountId);
    }

    @Override
    public synchronized void setAmount(final int amount) {
        super.setAmount(amount);
    }

    @Override
    public synchronized void addAmount(final int amount) {
        super.addAmount(amount);
    }

    @Override
    public synchronized int getAmount() {
        return super.getAmount();
    }
}
