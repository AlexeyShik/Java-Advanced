package info.kgeorgiy.ja.shik.bank;

import java.util.Map;

public class LocalPerson extends AbstractPerson {

    public LocalPerson(final String firstName, final String lastName, final String passport,
                       final Map<String, Account> accounts) {
        super(firstName, lastName, passport, accounts);
    }
}
