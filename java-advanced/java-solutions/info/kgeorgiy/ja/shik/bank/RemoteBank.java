package info.kgeorgiy.ja.shik.bank;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteBank implements Bank {
    private final int port;
    private final Map<String, Person> persons;

    /**
     * Conctructs instance of {@code RemoteBank} with specified {@code port}
     * @param port port for working with rmi
     */
    public RemoteBank(final int port) {
        this.port = port;
        persons = new ConcurrentHashMap<>();
    }

    @Override
    public Account createAccount(final Person person, final String id) throws RemoteException {
        final String passport = person.getPassport();
        final Person remotePerson = persons.get(passport);
        if (remotePerson == null) {
            return null;
        }
        final Account account = new RemoteAccount(passport, id);
        if (remotePerson.getAccounts().putIfAbsent(id, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return remotePerson.getAccounts().get(id);
        }
    }

    @Override
    public Account getAccount(final Person person, final String accountId) throws RemoteException {
        return persons.get(person.getPassport()).getAccounts().get(accountId);
    }

    @Override
    public Person createPerson(final String firstName, final String lastName, final String passport) throws RemoteException {
        return createPerson(new RemotePerson(firstName, lastName, passport));
    }

    @Override
    public Person createPerson(final Person person) throws RemoteException {
        final String passport = person.getPassport();
        if (persons.putIfAbsent(passport, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return getPerson(passport);
        }
    }

    private Person getPerson(final String passport) {
        return persons.get(passport);
    }

    @Override
    public Person getLocalPerson(final String passport) throws RemoteException {
        final Person person = getPerson(passport);
        return new LocalPerson(person.getFirstName(), person.getLastName(), passport,
                copyAccounts(persons.get(passport).getAccounts()));
    }

    @Override
    public Person getRemotePerson(final String passport) {
        return getPerson(passport);
    }

    private <T extends Remote> void unExport(final T value) {
        try {
            UnicastRemoteObject.unexportObject(value, true);
        } catch (final NoSuchObjectException ignored) {
            //  no operations, impossible state
        }
    }

    @Override
    public void unExportAll() throws RemoteException {
        for (final Person person : persons.values()) {
            for (final Account account : person.getAccounts().values()) {
                unExport(account);
            }
            unExport(person);
        }
    }

    private Map<String, Account> copyAccounts(final Map<String, Account> accounts) {
        final Map<String, Account> copy = new HashMap<>();
        for (final Map.Entry<String, Account> entry : accounts.entrySet()) {
            try {
                copy.put(entry.getKey(), new LocalAccount(entry.getValue()));
            } catch (RemoteException ignored) {
                //  no operations
            }
        }
        return copy;
    }
}
