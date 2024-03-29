package kgk.actis.util;


import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import de.greenrobot.event.EventBus;
import kgk.actis.R;
import kgk.actis.dispatcher.Dispatcher;
import kgk.actis.model.Device;
import kgk.actis.model.product.ProductType;
import kgk.actis.networking.VolleyHttpClient;
import kgk.actis.stores.ActisStore;
import kgk.actis.stores.T5Store;
import kgk.actis.stores.T6Store;

/**
 * Приложение разработано на базе многослойной архитектуры Flux, реализующий однонаправленный поток данных, как показано на схеме
 *
 * - Для реализации модуля диспетчера (Dispatcher) используется стандартная шина событий (Event Bus). Сам диспетчер является по сути оберткой вокруг шины
 *
 * - Коммуникация между слоями осуществляется с помощью простых DTO, созданием и распеределением которых занимается модуль Action Creator
 *
 * - Вся бизнес-логика инкапсулирована в оперативном хранилище (Actis Store), Предполагается, что последующее расширение приложения для поддержки других типов устройств будет происходить путем добавления новых модулей Store
 *
 * - Кроме того, Store одновременно реализует шаблон ViewModel предоставляя все необходимые поля данных, которые необходимо отбразать на экране, Оповещение модуля View осуществляется так же при помощи объектов Action (DTO)
 */

public class AppController extends Application {

    private static final String TAG = AppController.class.getSimpleName();
    private static final String APPLICATION_PREFERENCES = "application_preferences";

    public static final String ACTIS_DEVICE_NAME = "Actis";
    // TODO Delete test code
    public static final String TEST_GENERATOR_DEVICE_NAME = "Test Generator";

    public static final String ACTIS_DEVICE_TYPE = "Actis";
    public static final String T6_DEVICE_TYPE = "T6";
    public static final String T5_DEVICE_TYPE = "T5";
    public static final String GENERATOR_DEVICE_TYPE = "Генератор";

    public static String currentUserLogin;

    private static AppController instance;

    private Device activeDevice;
    private long activeDeviceId;
    private String activeDeviceType;
    private String activeDeviceModel;
    private ProductType activeProductType;
    private Handler uiHandler;

    private boolean demoMode;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        uiHandler = new Handler(Looper.getMainLooper());
        VolleyHttpClient.getInstance(this);
        registerSignalStore();
    }

    private void registerSignalStore() {
        Dispatcher dispatcher = Dispatcher.getInstance(EventBus.getDefault());

        ActisStore actisStore = ActisStore.getInstance(dispatcher);
        T6Store t6Store = T6Store.getInstance(dispatcher);
        T5Store t5Store = T5Store.getInstance(dispatcher);

        dispatcher.register(actisStore);
        dispatcher.register(t6Store);
        dispatcher.register(t5Store);
    }

    public static synchronized AppController getInstance() {
        return instance;
    }

    public long getActiveDeviceId() {
        return activeDeviceId;
    }

    public void setActiveDeviceId(long activeDeviceId) {
        this.activeDeviceId = activeDeviceId;
    }

    public String getActiveDeviceType() {
        return activeDeviceType;
    }

    public void setActiveDeviceType(String activeDeviceType) {
        this.activeDeviceType = activeDeviceType;
    }

    public String getActiveDeviceModel() {
        return activeDeviceModel;
    }

    public void setActiveDeviceModel(String activeDeviceModel) {
        this.activeDeviceModel = activeDeviceModel;
    }

    public ProductType getActiveProductType() {
        return activeProductType;
    }

    public void setActiveProductType(ProductType activeProductType) {
        this.activeProductType = activeProductType;
    }

    public Device getActiveDevice() {
        return activeDevice;
    }

    public void setActiveDevice(Device activeDevice) {
        this.activeDevice = activeDevice;
    }

    /** Проверка наличия на устройстве интернет-соединения */
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    public boolean isDemoMode() {
        return demoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    public static void saveBooleanValueToSharedPreferences(String key, boolean value) {
        SharedPreferences sharedPreferences = instance.getSharedPreferences(APPLICATION_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean(key, value);
        editor.apply();
    }

    public static boolean loadBooleanValueFromSharedPreferences(String key) {
        SharedPreferences sharedPreferences = instance.getSharedPreferences(APPLICATION_PREFERENCES, MODE_PRIVATE);
        return sharedPreferences.getBoolean(key, false);
    }

    public static void saveLongValueToSharedPreferences(String key, long value) {
        SharedPreferences sharedPreferences = instance.getSharedPreferences(APPLICATION_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putLong(key, value);
        editor.apply();
    }

    public static long loadLongValueFromSharedPreferences(String key) {
        SharedPreferences sharedPreferences = instance.getSharedPreferences(APPLICATION_PREFERENCES, MODE_PRIVATE);
        return sharedPreferences.getLong(key, 0);
    }

    public static void saveStringValueToSharedPreferences(String key, String value) {
        SharedPreferences sharedPreferences = instance.getSharedPreferences(APPLICATION_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(key, value);
        editor.apply();
    }

    public static String loadStringValueFromSharedPreferences(String key) {
        SharedPreferences sharedPreferences = instance.getSharedPreferences(APPLICATION_PREFERENCES, MODE_PRIVATE);
        return sharedPreferences.getString(key, "default");
    }

    /** Получить литеру направления в зависимости от градусного значения */
    public static String getDirectionLetterFromDegrees(int degrees) {
        if (degrees > 337 && degrees <= 22) {
            return AppController.getInstance().getString(R.string.direction_north);
        } else if (degrees > 22 && degrees <= 67) {
            return AppController.getInstance().getString(R.string.direction_north_east);
        } else if (degrees > 67 && degrees <= 112) {
            return AppController.getInstance().getString(R.string.direction_east);
        } else if (degrees > 112 && degrees <= 157) {
            return AppController.getInstance().getString(R.string.direction_south_east);
        } else if (degrees > 157 && degrees <= 202) {
            return AppController.getInstance().getString(R.string.direction_south);
        } else if (degrees > 202 && degrees <= 247) {
            return AppController.getInstance().getString(R.string.direction_south_west);
        } else if (degrees > 247 && degrees <= 292) {
            return AppController.getInstance().getString(R.string.direction_west);
        } else if (degrees > 292 && degrees <= 337) {
            return AppController.getInstance().getString(R.string.direction_north_west);
        }

        return AppController.getInstance().getString(R.string.direction_north);
    }

    public static String generateDeviceLabel(Device device) {
        if (device.getMark().equals(device.getStateNumber())) {
            return device.getCivilModel() + " " + device.getId();
        }
        return device.getCivilModel() + " " + device.getStateNumber() +
                " " + device.getId();
    }

    public static boolean isBelowLollopop() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isTablet() {
        return instance.getResources().getBoolean(R.bool.isTablet);
    }

    ////

    public static void runOnUiThread(Runnable runnable) {
        instance.uiHandler.post(runnable);
    }
}
