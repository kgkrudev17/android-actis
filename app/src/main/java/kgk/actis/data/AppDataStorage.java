package kgk.actis.data;

public interface AppDataStorage {

    void saveLogin(String login);

    String loadLogin();

    void savePassword(String password);

    String loadPassword();
}
