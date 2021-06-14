package info.kgeorgiy.ja.shik.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Objects;

public final class Client {

    private Client() {}

    /**
     * Creates {@link Person} if he doesn't already exists in {@link Bank}, otherwise checks his data.
     * Creates {@link Account} for this {@code Person}, if {@code Account} with specified id doesn't already exists.
     * Adds given amount of money on this {@code Account}
     *
     * @param args <ul>
     *                 <li>
     *                     firstName - first name of a {@code Person}
     *                 </li>
     *                 <li>
     *                     lastName - last name of a {@code Person}
     *                 </li>
     *                 <li>
     *                     passport - passport of a {@code Person}
     *                 </li>
     *                 <li>
     *                     account id - identifier for {@code Person}'s {@code Account}
     *                 </li>
     *                 <li>
     *                     amount of money to change - amount for money to be added for {@code Account} with identifier {@code id}
     *                 </li>
     *             </ul>
     */
    public static void main(final String... args) throws RemoteException {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Usage: Client <firstName> <lastName> <passport> <account id> <amount of money to change>,"
            + " arguments should not be null");
            return;
        }
        int i = 0;
        final String firstName = args[i++];
        final String lastName = args[i++];
        final String passport = args[i++];
        final String accountId = args[i++];
        final int amount;
        try {
            amount = Integer.parseInt(args[i]);
        } catch (NumberFormatException e) {
            System.err.println("Amount of money should be integral number");
            return;
        }

        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (final NotBoundException e) {
            System.err.println("Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            System.err.println("Bank URL is invalid");
            return;
        }

        Person person = bank.getRemotePerson(passport);
        if (person == null) {
            System.out.println("Creating person");
            person = bank.createPerson(firstName, lastName, passport);
        } else {
            System.out.println("Person already exists");
            if (!Objects.equals(person.getFirstName(), firstName) || !Objects.equals(person.getLastName(), lastName)) {
                System.out.println("Incorrect person data in request");
            } else {
                System.out.println("Person data are correct");
            }
        }

        Account account = bank.getAccount(person, accountId);
        if (account == null) {
            System.out.println("Creating account");
            account = bank.createAccount(person, accountId);
        } else {
            System.out.println("Account already exists");
        }
        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        account.setAmount(account.getAmount() + amount);
        System.out.println("Money: " + account.getAmount() + "\n");
    }
}
