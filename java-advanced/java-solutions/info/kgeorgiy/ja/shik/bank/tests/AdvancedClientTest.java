package info.kgeorgiy.ja.shik.bank.tests;

import info.kgeorgiy.ja.shik.bank.Client;
import info.kgeorgiy.ja.shik.bank.Person;
import info.kgeorgiy.ja.shik.bank.RemotePerson;
import org.junit.Assert;
import org.junit.Test;

import java.rmi.RemoteException;

// :NOTE: does not pass on Linux. 1.5 minuses.
// If you find evidence that it is an issue with Linux and not your code,
// I'll remove those minuses. yep
public class AdvancedClientTest extends BaseTest {

    @Test
    public void test_01_singleAccount() throws RemoteException {
        final Person ALEXEY = new RemotePerson("Alexey", "Shik", "alexeyshik");
        for (int i = 0; i < 10; ++i) {
            callMain(ALEXEY, "A", "10");
        }
        checkAccount(100, ALEXEY, "A");
    }

    @Test
    public void test_02_incorrectArguments() throws RemoteException {
        Client.main((String[]) null);
        Client.main("");
        Client.main("Мало", "Аргументов", "1", "2");
        Client.main("Ok", "Okay", "okey", "ok", null);
        Client.main("Ok", "Okay", "okey", "ok", "Not a number");
    }

    @Test
    public void test_03_manyAccounts() throws RemoteException {
        final Person ALEXEY = new RemotePerson("Alexey", "Shik", "alexeyshik");
        callMain(ALEXEY, "A", "10");
        callMain(ALEXEY, "B", "100");
        callMain(ALEXEY, "C", "-10");
        checkAccount(10, ALEXEY, "A");
        checkAccount(100, ALEXEY, "B");
        checkAccount(-10, ALEXEY, "C");
        callMain(ALEXEY, "A", "10");
        callMain(ALEXEY, "B", "100");
        callMain(ALEXEY, "C", "-10");
        checkAccount(20, ALEXEY, "A");
        checkAccount(200, ALEXEY, "B");
        checkAccount(-20, ALEXEY, "C");
    }

    private static void callMain(final Person person, final String account, final String amount) throws RemoteException{
        Client.main(person.getFirstName(), person.getLastName(), person.getPassport(), account, amount);
    }

    private static void checkAccount(final int expected, final Person person, final String account) throws RemoteException {
        Assert.assertEquals(expected, bank.getAccount(person, account).getAmount());
    }
}
