package info.kgeorgiy.ja.shik.bank.tests;

import info.kgeorgiy.ja.shik.bank.Account;
import info.kgeorgiy.ja.shik.bank.Person;
import info.kgeorgiy.ja.shik.bank.RemoteAccount;
import info.kgeorgiy.ja.shik.bank.RemotePerson;
import org.junit.Assert;
import org.junit.Test;

import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AdvancedBankTest extends BaseTest {
    private static final int SMALL_DIM = 10;
    private static final int BIG_DIM = 100;

    private void checkPersonAccount(final Person person, final String accountName) throws RemoteException {
        final Account account = bank.createAccount(person, accountName);
        Assert.assertNotNull(account);
        Assert.assertEquals(0, account.getAmount());
        account.setAmount(100);
        Assert.assertEquals(100, bank.getAccount(person, accountName).getAmount());
        account.addAmount(1000);
        Assert.assertEquals(1100, bank.getAccount(person, accountName).getAmount());
        Assert.assertEquals(1100, account.getAmount());
    }

    @Test
    public void test_01_createManyAccounts() throws RemoteException {
        final Person ALEXEY = new RemotePerson("Alexey", "Shik", "alexeyshik");
        Person person = bank.createPerson(ALEXEY);
        for (int i = 0; i < BIG_DIM; ++i) {
            checkPersonAccount(person, Integer.toString(i));
        }
    }

    @Test
    public void test_02_createManyPersons() throws RemoteException {
        for (int i = 0; i < BIG_DIM; ++i) {
            Person person = new RemotePerson("A" + i, "B" + i, "C" + i);
            person = bank.createPerson(person);
            for (int j = 0; j < SMALL_DIM; ++j) {
                checkPersonAccount(person, "D" + j);
            }
        }
    }

    private void checkPersonFields(final Person person1, final Person person2) throws RemoteException {
        Assert.assertEquals(person1.getFirstName(), person2.getFirstName());
        Assert.assertEquals(person1.getLastName(), person2.getLastName());
    }

    @Test
    public void test_03_findByPassportRemote() throws RemoteException {
        final Person ALEXEY = new RemotePerson("Alexey", "Shik", "alexeyshik");
        final Person KGEORGIY = new RemotePerson("Georgiy", "Korneev", "kgeorgiy");
        final Person ANDREW = new RemotePerson("Andrew", "Stankevich", "andrewzta");
        bank.createPerson(ALEXEY);
        checkPersonFields(ALEXEY, bank.getRemotePerson(ALEXEY.getPassport()));
        bank.createPerson(KGEORGIY);
        checkPersonFields(ALEXEY, bank.getRemotePerson(ALEXEY.getPassport()));
        checkPersonFields(KGEORGIY, bank.getRemotePerson(KGEORGIY.getPassport()));
        bank.createPerson(ANDREW);
        checkPersonFields(ALEXEY, bank.getRemotePerson(ALEXEY.getPassport()));
        checkPersonFields(KGEORGIY, bank.getRemotePerson(KGEORGIY.getPassport()));
        checkPersonFields(ANDREW, bank.getRemotePerson(ANDREW.getPassport()));

        final Person remoteAlexey = bank.getRemotePerson(ALEXEY.getPassport());
        final Account account = bank.createAccount(remoteAlexey, "A");
        checkPersonAccount(remoteAlexey, "A");

        final Person remoteAlexey2 = bank.getRemotePerson(ALEXEY.getPassport());
        final Account account2 = bank.createAccount(remoteAlexey2, "A");
        Assert.assertEquals(account.getAmount(), account2.getAmount());

        account2.addAmount(5);
        Assert.assertEquals(account.getAmount(), account2.getAmount());
        Assert.assertEquals(account2.getAmount(), bank.getAccount(ALEXEY, "A").getAmount());

        final Account account1 = new RemoteAccount(ALEXEY.getPassport(), "B");
        remoteAlexey.getAccounts().put("B", account1);
        account1.setAmount(123);
        Assert.assertEquals(123, bank.getAccount(remoteAlexey2, "B").getAmount());
    }

    @Test
    public void test_04_findByPassportLocal() throws RemoteException {
        final Person ALEXEY = new RemotePerson("Alexey", "Shik", "alexeyshik");
        final Person KGEORGIY = new RemotePerson("Georgiy", "Korneev", "kgeorgiy");
        final Person ANDREW = new RemotePerson("Andrew", "Stankevich", "andrewzta");
        bank.createPerson(ALEXEY);
        checkPersonFields(ALEXEY, bank.getLocalPerson(ALEXEY.getPassport()));
        bank.createPerson(KGEORGIY);
        checkPersonFields(ALEXEY, bank.getLocalPerson(ALEXEY.getPassport()));
        checkPersonFields(KGEORGIY, bank.getLocalPerson(KGEORGIY.getPassport()));
        bank.createPerson(ANDREW);
        checkPersonFields(ALEXEY, bank.getLocalPerson(ALEXEY.getPassport()));
        checkPersonFields(KGEORGIY, bank.getLocalPerson(KGEORGIY.getPassport()));
        checkPersonFields(ANDREW, bank.getLocalPerson(ANDREW.getPassport()));

        final Account account = bank.createAccount(ALEXEY, "A");
        final Person localAlexey = bank.getLocalPerson(ALEXEY.getPassport());
        account.setAmount(100);
        final Account localAccount = localAlexey.getAccount("A");
        Assert.assertEquals(0, localAccount.getAmount());
        Assert.assertEquals(100, bank.getAccount(ALEXEY, "A").getAmount());

        bank.createAccount(ALEXEY, "B");
        Assert.assertNull(localAlexey.getAccount("B"));

        final Person remoteAlexey = bank.getRemotePerson(ALEXEY.getPassport());
        final Account remoteAccount = remoteAlexey.getAccount("A");
        Assert.assertEquals(100, remoteAccount.getAmount());
        remoteAccount.addAmount(1000);
        Assert.assertEquals(1100, remoteAccount.getAmount());
        Assert.assertEquals(1100, bank.getAccount(ALEXEY, "A").getAmount());
        Assert.assertEquals(0, localAccount.getAmount());

        localAccount.setAmount(50);
        Assert.assertEquals(50, localAccount.getAmount());
        Assert.assertEquals(1100, remoteAccount.getAmount());
        Assert.assertEquals(1100, bank.getAccount(ALEXEY, "A").getAmount());
    }

    @Test
    public void test_05_getAccounts() throws RemoteException {
        final Person ALEXEY = new RemotePerson("Alexey", "Shik", "alexeyshik");
        final Person person = bank.createPerson(ALEXEY);
        for (int i = 0; i < BIG_DIM; ++i) {
            bank.createAccount(person, "A" + i);
        }
        for (int i = 0; i < BIG_DIM; ++i) {
            Assert.assertNotNull(bank.getAccount(person, "A" + i));
        }
    }

    private void shutdownAndAwaitTermination(final ExecutorService service) {
        service.shutdownNow();
        try {
            if (!service.awaitTermination(1, TimeUnit.MINUTES)) {
                System.err.println("Service did not terminate");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void test_06_parallelPersons() {
        final ExecutorService service = Executors.newFixedThreadPool(SMALL_DIM);
        for (int i = 0; i < BIG_DIM; ++i) {
            final int finalI = i;
            service.submit(() -> {
                Person person = new RemotePerson("A" + finalI, "B" + finalI, "C" + finalI);
                try {
                    person = bank.createPerson(person);
                    bank.createAccount(person, "D" + finalI);
                    checkPersonAccount(person, "D" + finalI);
                } catch (final RemoteException e) {
                    e.printStackTrace();
                }
            });
        }
        shutdownAndAwaitTermination(service);
    }

    @Test
    public void test_07_parallelAccounts() throws RemoteException {
        final ExecutorService service = Executors.newFixedThreadPool(SMALL_DIM);
        Person person = new RemotePerson("A", "B", "C");
        final Person finalPerson = bank.createPerson(person);
        for (int i = 0; i < BIG_DIM; ++i) {
            final int finalI = i;
            service.submit(() -> {
                try {
                    bank.createAccount(finalPerson, "D" + finalI);
                    checkPersonAccount(finalPerson, "D" + finalI);
                } catch (final RemoteException e) {
                    e.printStackTrace();
                }
            });
        }
        shutdownAndAwaitTermination(service);
    }

    @Test
    public void test_08_parallelSingleAccount() throws RemoteException {
        final ExecutorService service = Executors.newFixedThreadPool(SMALL_DIM);
        final Person person = new RemotePerson("A", "B", "C");
        final Person finalPerson = bank.createPerson(person);
        bank.createAccount(finalPerson, "D");
        for (int i = 0; i < BIG_DIM; ++i) {
            final int finalI = i;
            service.submit(() -> {
                try {
                    final Person remotePerson = bank.getRemotePerson(person.getPassport());
                    final int was = remotePerson.getAccount("D").getAmount();
                    remotePerson.getAccount("D").addAmount(finalI);
                    Assert.assertEquals(was + finalI, bank.getAccount(person, "D").getAmount());
                    bank.getAccount(person, "D").addAmount(finalI);
                    final Person localPerson = bank.getLocalPerson(person.getPassport());
                    Assert.assertEquals(was + 2 * finalI, localPerson.getAccount("D").getAmount());
                    Assert.assertEquals(was + 2 * finalI, remotePerson.getAccount("D").getAmount());
                } catch (final RemoteException e) {
                    e.printStackTrace();
                }
            });
        }
        shutdownAndAwaitTermination(service);
    }

    @Test
    public void test_09_arabic() throws RemoteException {
        final Person ARABIC_KGEORGIY = new RemotePerson("كجورجي", "كجورجيكجورجي", "1كجورجي1");
        final Person person = bank.createPerson(ARABIC_KGEORGIY);
        bank.createAccount(person, "1كجورجي");
        checkPersonAccount(person, "1كجورجي");
        Assert.assertEquals(ARABIC_KGEORGIY.getFirstName(), person.getFirstName());
        Assert.assertEquals(ARABIC_KGEORGIY.getLastName(), person.getLastName());
    }
}
