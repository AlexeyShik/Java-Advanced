package info.kgeorgiy.ja.shik.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface Person extends Remote {

    /** Returns first name of this Person */
    String getFirstName() throws RemoteException;

    /** Returns last name of this Person */
    String getLastName() throws RemoteException;

    /** Returns passport of this Person */
    String getPassport() throws RemoteException;

    /** Returns accounts of this Person */
    Map<String, Account> getAccounts() throws RemoteException;

    /** Returns account of this Person with specified {@code id} */
    Account getAccount(final String id) throws RemoteException;
}
