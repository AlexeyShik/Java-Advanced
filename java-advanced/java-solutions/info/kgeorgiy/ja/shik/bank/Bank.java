package info.kgeorgiy.ja.shik.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    /**
     * Creates a new {@link Account} with specified identifier if it is not already exists.
     *
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(final Person person, final String id) throws RemoteException;

    /**
     * Returns account by identifier.
     *
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exists.
     */
    Account getAccount(final Person person, final String id) throws RemoteException;

    /**
     * Creates a new {@link Person} with specified data, if he doesn't already exists in {@link Bank}.
     *
     * @param firstName first name of a person
     * @param lastName last name of a person
     * @param passport passport of a person
     * @return created or existing person.
     */
    Person createPerson(final String firstName, final String lastName, final String passport) throws RemoteException;

    /**
     * Creates a new {@link Person} with specified data, if he doesn't already exists in {@link Bank}.
     *
     * @param person {@code Person}, which data needs to be stored
     * @return created or existing person
     */
    Person createPerson(final Person person) throws RemoteException;

    /**
     * Gets {@link LocalPerson} with specified passport
     *
     * @param passport person's passport
     * @return instance of {@code LocalPerson} with specified passport
     */
    Person getLocalPerson(final String passport) throws RemoteException;

    /**
     * Gets {@link RemotePerson} with specified passport
     *
     * @param passport person's passport
     * @return instance of {@code RemotePerson} with specified passport
     */
    Person getRemotePerson(final String passport) throws RemoteException;

    /**
     * UnExports all exported objects
     */
    void unExportAll() throws RemoteException;
}
