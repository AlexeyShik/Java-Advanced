package info.kgeorgiy.ja.shik.bank;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractPerson implements Person, Serializable {
    private final String firstName;
    private final String lastName;
    private final String passport;
    private final Map<String, Account> accounts;

    protected AbstractPerson(final String firstName, final String lastName, final String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passport = password;
        accounts = new ConcurrentHashMap<>();
    }

    protected AbstractPerson(final String firstName, final String lastName, final String password,
                             final Map<String, Account> accounts) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passport = password;
        this.accounts = accounts;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getPassport() {
        return passport;
    }

    @Override
    public Map<String, Account> getAccounts() {
        return accounts;
    }

    @Override
    public Account getAccount(final String id) {
        return accounts.get(id);
    }
}
