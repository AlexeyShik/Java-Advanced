package info.kgeorgiy.ja.shik.bank;

import java.rmi.RemoteException;

public class LocalAccount extends AbstractAccount {

    public LocalAccount(final Account account) throws RemoteException {
        super(account.getId(), account.getAmount());
    }
}
