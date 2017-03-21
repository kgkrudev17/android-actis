package kgk.actis.data;

public class DataModuleFactory {

    public static AppDataStorage provideAppDataStorage() {
        return new StorageSharedPreferences();
    }
}
