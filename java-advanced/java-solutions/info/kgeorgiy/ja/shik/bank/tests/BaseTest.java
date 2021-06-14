package info.kgeorgiy.ja.shik.bank.tests;

import info.kgeorgiy.ja.shik.bank.Bank;
import info.kgeorgiy.ja.shik.bank.RemoteBank;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class BaseTest {
    protected static final int BANK_PORT = 8888;
    protected static Bank bank;
    protected static Registry registry;

    @BeforeClass
    public static void initRegistry() {
        try {
            registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        } catch (RemoteException e) {
            System.err.println("Cannot create registry: " + e.getMessage());
        }
    }

    @Before
    public void initBank() {
        bank = new RemoteBank(BANK_PORT);
        try {
            UnicastRemoteObject.exportObject(bank, BANK_PORT);
            Naming.rebind("//localhost/bank", bank);
        } catch (final RemoteException e) {
            System.err.println("Cannot export object: " + e.getMessage());
        } catch (final MalformedURLException e) {
            System.err.println("Malformed URL:" + e.getMessage());
        }
    }


    @After
    public void closeBank() {
        try {
            bank.unExportAll();
            UnicastRemoteObject.unexportObject(bank, true);
            Naming.unbind("//localhost/bank");
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            System.err.println("Cannot close bank: " + e.getMessage());
        }
    }

    @AfterClass
    public static void closeRegistry() {
        try {
            UnicastRemoteObject.unexportObject(registry, true);
        } catch (NoSuchObjectException e) {
            System.err.println("Cannot close registry: " + e.getMessage());
        }
    }
}
