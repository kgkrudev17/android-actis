package kgk.actis.networking;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import kgk.actis.R;
import kgk.actis.actions.Action;
import kgk.actis.actions.ActionCreator;
import kgk.actis.actions.HttpActions;
import kgk.actis.actions.event.ToggleSearchModeEvent;
import kgk.actis.database.ActisDatabaseDao;
import kgk.actis.dispatcher.Dispatcher;
import kgk.actis.model.Signal;
import kgk.actis.model.T5Packet;
import kgk.actis.model.T6Packet;
import kgk.actis.networking.event.BalanceResponseReceived;
import kgk.actis.networking.event.DownloadDataInProgressEvent;
import kgk.actis.networking.event.QueryRequestSuccessfulEvent;
import kgk.actis.networking.event.SearchModeStatusEvent;
import kgk.actis.networking.event.ValidatedCoordinatesReceivedEvent;
import kgk.actis.stores.DeviceStore;
import kgk.actis.stores.PacketsForDetailReportEvent;
import kgk.actis.util.AppController;
import kgk.actis.util.LastActionDateStorageForActis;
import kgk.actis.util.lbscoordinatesvalidator.ActisCoordinatesValidatorFromNetwork;
import kgk.actis.util.lbscoordinatesvalidator.LbsCoordinatesValidator;
import kgk.actis.view.actis.InformationFragment;
import kgk.actis.view.general.DeviceListActivity;
import kgk.actis.view.general.ProductActivity;
import kgk.actis.view.general.event.StartActivityEvent;

/**
 * Реализация менеджера http-запросов на базе библиотеки Google Volley
 */
public class VolleyHttpClient implements Response.ErrorListener {

    public interface WebSocketKeyRequestListener {

        void onWebSocketKeyReceived(String webSocketKey);
    }

    ////

    static final String DOMAIN = "http://api.trezub.ru/api2/";
    static final String API = "mobile/";

    private static final String TAG = VolleyHttpClient.class.getSimpleName();
    private static final String AUTHENTICATION_URL = DOMAIN + API + "authorize";
    private static final String DEVICE_LIST_URL = DOMAIN + API + "getdeviceslist?all=1"; //monitor.kgk-global.com
    private static final String GET_LAST_STATE_URL = DOMAIN + API + "getdevicestate";
    private static final String GET_LAST_SIGNALS_URL = DOMAIN + API + "getpackets";
    private static final String QUERY_BEACON_REQUEST_URL = DOMAIN + API + "cmdrequestinfo";
    private static final String TOGGLE_SEARCH_MODE_REQUEST_URL = DOMAIN + API + "cmdtogglefind";
    private static final String SETTINGS_REQUEST_URL = DOMAIN + API + "cmdsetsettingssms";
    // private static final String SETTINGS_REQUEST_URL = "http://api.trezub.ru/api2/beacon/cmdsetsettings";
    private static final String DETAIL_REPORT_URL = DOMAIN + "reports/getroute/";
    private static final String DETAIL_REPORT_URL_POST = DOMAIN + API + "detailedreport"; //"http://monitor.kgk-global.com/monitoring/reports/getdata";
    private static final String ACTIS_CONFIG_URL = DOMAIN + API + "getactisconfig";
    private static final String GET_USER_INFO_URL = DOMAIN + API + "getuserinfo";
    private static final String WEB_SOCKET_KEY_URL = DOMAIN + API + "getwebsocketkey"; //"http://requestb.in/1gmkqvu1";

    private static final String OPEN_CELL_ID_GET_URL = "http://opencellid.org/cell/get";
    private static final String OPEN_CELL_ID_API_KEY = "635c0e4c-afd3-4272-a0ac-66275f6a9c1b";

    private static final String YANDEX_LBS_LOCATION_REQUEST = "http://mobile.maps.yandex.net/cellid_location/";

    private static final int SOCKET_TIMEOUT_MS = 30000;
    private static final int SOCKET_MAX_RETRIES = 10;

    private static VolleyHttpClient instance;
    private Context context;
    private Dispatcher dispatcher;
    private RequestQueue requestQueue;

    private int routeReportResponseCounter;
    private WebSocketKeyRequestListener webSocketKeyRequestListener;

    static String phpSessId;

    ////

    private VolleyHttpClient(Context context) {
        this.context = context;
        requestQueue = Volley.newRequestQueue(context);
        dispatcher = Dispatcher.getInstance(EventBus.getDefault());
        dispatcher.register(this);
        dispatcher.register(DeviceStore.getInstance(dispatcher));

        Thread periodicLastStateQueryThread = new Thread(periodicLastStateQueryRunnable());
        periodicLastStateQueryThread.start();
    }

    public static synchronized VolleyHttpClient getInstance(Context context) {
        if (instance == null) {
            instance = new VolleyHttpClient(context);
        }

        return instance;
    }

    public void onEvent(Action action) {
        onAction(action);
    }

    public void onAction(Action action) {
        switch (action.getType()) {
            case HttpActions.AUTHENTICATION_REQUEST:
                String login = ((String) action.getData().get(ActionCreator.KEY_LOGIN));
                String password = ((String) action.getData().get(ActionCreator.KEY_PASSWORD));
                authenticationRequest(login, password);
                break;
            case HttpActions.QUERY_BEACON_REQUEST:
                queryBeaconRequest();
                break;
            case HttpActions.TOGGLE_SEARCH_MODE_REQUEST:
                boolean searchModeStatus = (boolean) action.getData().get(ActionCreator.KEY_SEARCH_MODE_STATUS);
                toggleSearchModeRequest(searchModeStatus);
                break;
            case HttpActions.SEND_SETTINGS_REQUEST:
                JSONObject settingsJson = (JSONObject) action.getData().get(ActionCreator.KEY_SETTINGS);
                settingsRequest(settingsJson);
                break;
            case HttpActions.SEND_GET_SETTINGS_REQUEST:
                getSettingsRequest();
                break;
            case HttpActions.GET_LAST_STATE_REQUEST:
                getLastStateRequest();
                break;
            case HttpActions.GET_LAST_SIGNALS_REQUEST:
                long fromDate = (long) action.getData().get(ActionCreator.KEY_FROM_DATE);
                long toDate = (long) action.getData().get(ActionCreator.KEY_TO_DATE);
                getLastSignalsRequest(fromDate, toDate);
                break;
            case HttpActions.LAST_STATE_FOR_DEVICE_REQUEST:
                lastStateForDeviceRequest();
                break;
            case HttpActions.GET_USER_INFO_REQUEST:
                getUserInfoRequest();
                break;
            case HttpActions.GENERATOR_SEND_MANUAL_MODE_COMMAND:
                sendManualModeRequestToGenerator();
                break;
            case HttpActions.GENERATOR_SEND_AUTO_MODE_COMMAND:
                sendAutoModeRequestToGenerator();
                break;
            case HttpActions.GENERATOR_SEND_EMERGENCY_STOP_COMMAND:
                sendEmergencyStopRequestToGenerator();
                break;
            case HttpActions.GENERATOR_SEND_SWITCH_ON_COMMAND:
                sendSwitchOnRequestToGenerator();
                break;
            case HttpActions.GENERATOR_SEND_SWITCH_OFF_COMMAND:
                sendSwitchOffRequestToGenerator();
                break;
            case HttpActions.ACTIS_COORDINATES_VALIDATION_REQUEST:
                long serverDate = (long) action.getData().get(ActionCreator.KEY_VALIDATION_SERVER_DATE);
                int mcc = (int) action.getData().get(ActionCreator.KEY_VALIDATION_MCC);
                int mnc = (int) action.getData().get(ActionCreator.KEY_VALIDATION_MNC);
                String cellIdHex = (String) action.getData().get(ActionCreator.KEY_VALIDATION_CELL_ID);
                String lacHex = (String) action.getData().get(ActionCreator.KEY_VALIDATION_LAC);
                yandexLbsLocationRequest(serverDate, mcc, mnc, cellIdHex, lacHex);
                break;
        }
    }

    public void sendBalanceRequest() {
        BalanceRequest request = new BalanceRequest(Request.Method.POST,
                "http://www.kgk-global.com/ru/pay?user_name=ru.dev17&user_password=123098",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {}
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {}
                });

        request.setPhpSessId(phpSessId);
        setRetryPolicy(request);
        requestQueue.add(request);
    }

    public void addRequest(Request request) {

        setRetryPolicy(request);
        requestQueue.add(request);
    }

    //// WEB SOCKET

    public void setWebSocketKeyRequestListener(WebSocketKeyRequestListener listener) {
        webSocketKeyRequestListener = listener;
    }

    public void removeWebSocketKeyRequestListener() {
        webSocketKeyRequestListener = null;
    }

    public void sendWebSocketKeyRequest() {
        WebSocketKeyRequest request = new WebSocketKeyRequest(
                Request.Method.GET,
                WEB_SOCKET_KEY_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        webSocketKeyRequestListener.onWebSocketKeyReceived(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {}
                }
        );

        request.setPhpSessId(phpSessId);
        setRetryPolicy(request);
        requestQueue.add(request);
    }

    ////

    /** Отправка запроса на авторизацию */
    private void authenticationRequest(final String login, final String password) {
        final AuthenticationRequest request = new AuthenticationRequest(Request.Method.POST,
                AUTHENTICATION_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        // TODO Delete test code
                        Log.d("DEBUG", response);

                        try {
                            JSONObject responseJson = new JSONObject(response);
                            processAuthenticationResponseJson(responseJson, login);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        StartActivityEvent event = new StartActivityEvent(DeviceListActivity.class);
                        event.setLoginSuccessful(false);
                        EventBus.getDefault().post(event);
                        Toast.makeText(context, context.getString(R.string.on_command_send_error_toast),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        request.setLogin(login);
        request.setPassword(password);

        setRetryPolicy(request);
        requestQueue.add(request);
    }

    /** Отправка запроса на получение списка устройств пользователя */
    private void deviceListRequest(String phpSessId) {
        DeviceListRequest request = new DeviceListRequest(Request.Method.GET,
                DEVICE_LIST_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        // TODO Delete test code
                        Log.d("DEBUG", response);

                        try {
                            JSONObject responseJson = new JSONObject(response);
                            processDeviceListResponseJson(responseJson);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                this);

        request.setPhpSessId(phpSessId);
        setRetryPolicy(request);
        requestQueue.add(request);
    }

    /** Отправка запроса на команду разового определения местоположения Actis */
    private void queryBeaconRequest() {
        LastActionDateStorageForActis.getInstance().save(Calendar.getInstance().getTime());
        QueryBeaconRequest request = new QueryBeaconRequest(Request.Method.GET,
                QueryBeaconRequest.makeUrl(QUERY_BEACON_REQUEST_URL),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responseJson = new JSONObject(response);
                            if (responseJson.getBoolean("status")) {
                                EventBus.getDefault().post(new QueryRequestSuccessfulEvent());

                                long deviceId = AppController.getInstance().getActiveDeviceId();

                                AppController.saveLongValueToSharedPreferences(deviceId + InformationFragment.KEY_QUERY_CONTROL_DATE,
                                        ActisDatabaseDao.getInstance(AppController.getInstance()).getLastSignalDate());

                                AppController.saveLongValueToSharedPreferences(deviceId + InformationFragment.KEY_QUERY_EXPIRE_DATE,
                                        Calendar.getInstance().getTimeInMillis() / 1000 + InformationFragment.COMMAND_EXPIRATION_PERIOD);

                            } else {
                                Toast.makeText(context, R.string.on_command_send_error_toast, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                this);

        request.setPhpSessId(phpSessId);
        setRetryPolicy(request);
        requestQueue.add(request);
    }

    /** Отправка запроса на получение последнего актуального местоположения */
    private void getLastStateRequest() {
        GetLastStateRequest request = new GetLastStateRequest(Request.Method.GET,
                GetLastStateRequest.makeUrl(GET_LAST_STATE_URL),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        processLastStateResponse(response);
                    }
                },
                this);

        request.setPhpSessId(phpSessId);
        setRetryPolicy(request);
        requestQueue.add(request);
    }

    /** Отправка запроса на отправку комманды на включение/выключение режима Поиск устройства Actis */
    private void toggleSearchModeRequest(boolean searchModeStatus) {
        LastActionDateStorageForActis.getInstance().save(Calendar.getInstance().getTime());
        ToggleSearchModeRequest request = new ToggleSearchModeRequest(Request.Method.GET,
                ToggleSearchModeRequest.makeUrl(TOGGLE_SEARCH_MODE_REQUEST_URL, searchModeStatus),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responseJson = new JSONObject(response);
                            ToggleSearchModeEvent event = new ToggleSearchModeEvent();
                            if (responseJson.getBoolean("status")) {
                                event.setResult(true);
                                EventBus.getDefault().post(event);
                            } else {
                                Toast.makeText(context, R.string.on_command_send_error_toast, Toast.LENGTH_SHORT).show();
                                event.setResult(false);
                                EventBus.getDefault().post(event);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                this);

        request.setPhpSessId(phpSessId);
        setRetryPolicy(request);
        requestQueue.add(request);
    }

    /** Отправка запроса на отправку настроек для устройства Actis */
    private void settingsRequest(JSONObject settingsJson) {
        LastActionDateStorageForActis.getInstance().save(Calendar.getInstance().getTime());

        try {
            settingsJson.put("id", AppController.getInstance().getActiveDeviceId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SettingsRequest request = new SettingsRequest(SETTINGS_REQUEST_URL,
                settingsJson,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("status")) {
                                Log.d(TAG, response.toString());
//                                Toast.makeText(context, R.string.on_command_send_toast, Toast.LENGTH_SHORT).show();
//                                AppController.saveBooleanValueToSharedPreferences(
//                                        SettingsFragment.KEY_IS_AWAITING_SETTINGS_CONFIRMATION, true);
//                                AppController.saveLongValueToSharedPreferences(
//                                        SettingsFragment.KEY_SETTINGS_CONFIRMATION_CONTROL_DATE,
//                                        Calendar.getInstance().getTimeInMillis() / 1000);
                            } else {
//                                Toast.makeText(context, R.string.on_command_send_error_toast, Toast.LENGTH_SHORT).show();
                                Log.d(TAG, response.toString());
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                this);

        request.setPhpSessId(phpSessId);
        setRetryPolicy(request);
        requestQueue.add(request);
    }

    /** Отправка запроса на получение текущих настроек устройства Actis */
    private void getSettingsRequest() {
        long deviceId = AppController.getInstance().getActiveDeviceId();

        GetSettingsRequest request = new GetSettingsRequest(ACTIS_CONFIG_URL + "?device=" + deviceId,
                new JSONObject(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        processGetSettingsRequest(response);
                    }
                },
                this);

        request.setPhpSessId(phpSessId);
        setRetryPolicy(request);
        requestQueue.add(request);
    }

    /** Отправка запроса на получение свежих сигналов за указанный период */
    private void getLastSignalsRequest(long fromDate, long toDate) {
        if (!AppController.getInstance().isNetworkAvailable()) {
            DownloadDataInProgressEvent event = new DownloadDataInProgressEvent();
            event.setStatus(DownloadDataStatus.noInternetConnection);
            EventBus.getDefault().post(event);
            return;
        }

        Log.d(TAG, "getLastSignalsRequest: " + GetLastSignalsRequest.makeUrl(GET_LAST_SIGNALS_URL, fromDate, toDate));

        GetLastSignalsRequest request = new GetLastSignalsRequest(Request.Method.POST,
                GetLastSignalsRequest.makeUrl(GET_LAST_SIGNALS_URL, fromDate, toDate),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        processLastSignalsResponse(response);
                    }
                },
                this);

        request.setPhpSessId(phpSessId);
        setRetryPolicy(request);
        requestQueue.add(request);

        DownloadDataInProgressEvent event = new DownloadDataInProgressEvent();
        event.setStatus(DownloadDataStatus.Started);
        EventBus.getDefault().post(event);
    }

    /** Отправка запроса на получение последнего актуального местоположения */
    private void lastStateForDeviceRequest() {
        if (!AppController.getInstance().isNetworkAvailable()) {
            DownloadDataInProgressEvent event = new DownloadDataInProgressEvent();
            event.setStatus(DownloadDataStatus.noInternetConnection);
            EventBus.getDefault().post(event);
            return;
        }

        GetLastStateRequest request = new GetLastStateRequest(Request.Method.GET,
                GetLastStateRequest.makeUrl(GET_LAST_STATE_URL),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        processLastStateResponse(response);
                    }
                },
                this);

        request.setPhpSessId(phpSessId);
        setRetryPolicy(request);
        requestQueue.add(request);

        DownloadDataInProgressEvent event = new DownloadDataInProgressEvent();
        event.setStatus(DownloadDataStatus.Started);
        EventBus.getDefault().post(event);
    }

    private void getUserInfoRequest() {
        GetUserInfoRequest request = new GetUserInfoRequest(GET_USER_INFO_URL,
                new JSONObject(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        processGetUserInfoResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {}
                });

        request.setPhpSessId(phpSessId);
        setRetryPolicy(request);
        requestQueue.add(request);
    }

    /** Обработка результатов запроса на авторизацию */
    private void processAuthenticationResponseJson(JSONObject responseJson,
                                                   String login) throws JSONException {
        if (responseJson.getBoolean("status")) {
            String[] parsedCookie = ((String) responseJson.get("Cookie")).split(";");
            phpSessId = parsedCookie[0];
            deviceListRequest(phpSessId);

            AppController.currentUserLogin = login;
        } else {
            StartActivityEvent event = new StartActivityEvent(DeviceListActivity.class);
            event.setLoginSuccessful(false);
            EventBus.getDefault().post(event);
            Toast.makeText(context, context.getString(R.string.wrong_login_or_password_label),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** Обработка результатов запроса на получение списка устройств пользователя */
    private void processDeviceListResponseJson(JSONObject responseJson) throws JSONException {
        if (responseJson.getBoolean("status")) {
            try {
                JSONArray devices = responseJson.getJSONArray("data");
                ActionCreator.getInstance(dispatcher).receiveDeviceListResponse(devices);
                StartActivityEvent event = new StartActivityEvent(DeviceListActivity.class);
                event.setLoginSuccessful(true);
                EventBus.getDefault().post(event);
            } catch (JSONException e) {
                Toast.makeText(AppController.getInstance().getApplicationContext(), R.string.no_registered_devices_toast, Toast.LENGTH_LONG).show();
                StartActivityEvent event = new StartActivityEvent(DeviceListActivity.class);
                event.setLoginSuccessful(false);
                EventBus.getDefault().post(event);
            }
        } else {
            Toast.makeText(AppController.getInstance().getApplicationContext(), R.string.server_error, Toast.LENGTH_LONG).show();
            StartActivityEvent event = new StartActivityEvent(DeviceListActivity.class);
            event.setLoginSuccessful(false);
            EventBus.getDefault().post(event);
        }
    }

    /** Обработка результатов запроса на получение свежих сигналов за указанный период */
    private void processLastSignalsResponse(String response) {
        Log.d(TAG, "processLastSignalsResponse: " + response);

        final DownloadDataInProgressEvent event = new DownloadDataInProgressEvent();
        try {
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.getBoolean("status")) {
                final JSONArray responseDataJson = responseJson.getJSONArray("data");

                if (responseDataJson.length() <= 0) {
                    event.setStatus(DownloadDataStatus.DeviceNotFound);
                    EventBus.getDefault().post(event);
                    return;
                }

                switch (AppController.getInstance().getActiveDeviceType()) {
                    case AppController.ACTIS_DEVICE_TYPE:

                        Thread workerThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                List<Signal> signals = new ArrayList<>();

                                try {
                                    for (int i = 0; i < responseDataJson.length(); i++) {
                                        signals.add(Signal.signalFromJson(responseDataJson.getJSONObject(i)));
                                    }

                                    LbsCoordinatesValidator validator = new ActisCoordinatesValidatorFromNetwork(signals);
                                    validator.validate();
                                } catch (JSONException je) {
                                    je.printStackTrace();
                                }

//                                Signal lastSignalFromList = null;
//                                for (int i = 0; i < responseDataJson.length(); i++) {
//                                    try {
//                                        JSONObject signalJson = responseDataJson.getJSONObject(i);
//                                        Signal signal = Signal.signalFromJson(signalJson);
//                                        ActisDatabaseDao.getInstance(AppController.getInstance()).insertSignal(signal);
//
//                                        if (i == responseDataJson.length() - 1) {
//                                            lastSignalFromList = signal;
//                                        }
//                                    } catch (JSONException je) {
//                                        je.printStackTrace();
//                                    }
//                                }
//
//                                if (lastSignalFromList != null) {
//                                    ActionCreator.getInstance(dispatcher).updateLastSignal(lastSignalFromList);
//                                }

                                event.setStatus(DownloadDataStatus.Success);
                                EventBus.getDefault().post(event);
                            }
                        });

                        workerThread.start();
                        break;
                    case AppController.T5_DEVICE_TYPE:
                        ArrayList<T5Packet> t5packets = new ArrayList<>();
                        for (int i = 0; i < responseDataJson.length(); i++) {
                            JSONObject signalJson = responseDataJson.getJSONObject(i);
                            T5Packet packet = T5Packet.fromJson(signalJson);
                            t5packets.add(packet);
                        }
                        ActionCreator.getInstance(dispatcher).transportPacketsForTrack(t5packets);
                        event.setStatus(DownloadDataStatus.Success);
                        EventBus.getDefault().post(event);
                        break;
                    case AppController.T6_DEVICE_TYPE:
                        ArrayList<T6Packet> t6packets = new ArrayList<>();
                        for (int i = 0; i < responseDataJson.length(); i++) {
                            JSONObject signalJson = responseDataJson.getJSONObject(i);
                            T6Packet packet = T6Packet.fromJson(signalJson);
                            t6packets.add(packet);
                        }
                        ActionCreator.getInstance(dispatcher).transportPacketsForTrack(t6packets);
                        event.setStatus(DownloadDataStatus.Success);
                        EventBus.getDefault().post(event);
                        break;
                }
            } else {
                event.setStatus(DownloadDataStatus.Success);
                EventBus.getDefault().post(event);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /** Обработка результатов запроса на получение последнего актуального местоположения */
    private void processLastStateResponse(String response) {
        if (AppController.getInstance().getActiveDeviceType() == null) {
            return;
        }

        DownloadDataInProgressEvent event = new DownloadDataInProgressEvent();
        try {
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.getBoolean("status")) {
                JSONObject jsonPacket = responseJson.getJSONObject("data");
                Object packet = null;
                switch (AppController.getInstance().getActiveDeviceType()) {
                    case AppController.T6_DEVICE_TYPE:
                        packet = T6Packet.fromJson(jsonPacket);
                        break;
                    case AppController.T5_DEVICE_TYPE:
                        packet = T5Packet.fromJson(jsonPacket);
                        break;
                }
                event.setStatus(DownloadDataStatus.Success);
                EventBus.getDefault().post(event);
                ActionCreator.getInstance(dispatcher).transportLastStatePacketToStore(packet);
            } else {
                event.setStatus(DownloadDataStatus.Error);
                EventBus.getDefault().post(event);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /** Обработка результатов запроса на получение детального отчета */
    private void processDetailReportResponse(JSONObject responseJson) {
        try {
            JSONObject allData = responseJson.getJSONObject(String.valueOf(AppController.getInstance().getActiveDeviceId()));
            JSONArray dataArray = allData.getJSONArray("rows");

            ArrayList<LatLng> coordinatesMovingForDetailReport = new ArrayList<>();
            ArrayList<LatLng> coordinatesStopsForDetailReport = new ArrayList<>();
            for (int i = 1; i < dataArray.length(); i++) {
                try {
                    JSONObject entry = dataArray.getJSONObject(i);
                    String eventType = entry.getString("event");

                    if (eventType.equals("Движение")) {
                        JSONArray movingArray = entry.getJSONArray("treck");

                        for (int j = 0; j < movingArray.length(); j++) {
                            JSONObject movingEntry = movingArray.getJSONObject(j);
                            coordinatesMovingForDetailReport.add(new LatLng(movingEntry.getDouble("lat"), movingEntry.getDouble("lng")));
                        }
                    } else if (eventType.equals("Стоянка")) {
                        JSONArray stopArray = entry.getJSONArray("treck");
                        coordinatesStopsForDetailReport.add(new LatLng(stopArray.getJSONObject(0).getDouble("lat"),
                                stopArray.getJSONObject(0).getDouble("lng")));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "JSON Exception at processDetailReportResponse()");
                }
            }

            DownloadDataInProgressEvent downloadEvent = new DownloadDataInProgressEvent();
            downloadEvent.setStatus(DownloadDataStatus.Success);
            EventBus.getDefault().post(downloadEvent);

            PacketsForDetailReportEvent event = new PacketsForDetailReportEvent();
            event.setCoordinatesForMoving(coordinatesMovingForDetailReport);
            event.setCoordinatesForStops(coordinatesStopsForDetailReport);
            EventBus.getDefault().post(event);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /** Обработка результатов запроса на получение текущих настроек устройства Actis */
    private void processGetSettingsRequest(JSONObject dataJson) {
        try {
            Boolean status = dataJson.getBoolean("status");
            if (status) {
                String settingsAsString = dataJson.getString("data");
                JSONObject settingsJson = new JSONObject(settingsAsString);

                EventBus.getDefault().post(new SearchModeStatusEvent(
                        settingsJson.getInt("track") == 1));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processGetUserInfoResponse(JSONObject response) {
        try {
            JSONObject data = response.getJSONObject("data");
            double balance = data.getDouble("balance");

            BalanceResponseReceived event = new BalanceResponseReceived();
            event.setResultCode(0);
            event.setBalance(Double.toString(balance));
            EventBus.getDefault().post(event);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /** Установка характеристик переотправки запросов */
    private void setRetryPolicy(Request request) {
        request.setRetryPolicy(new DefaultRetryPolicy(
                SOCKET_TIMEOUT_MS,
                SOCKET_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    /** Отправка запроса в OpenCellId на определение координат по базовым станциям */
    private void openCellIdRequest(final long serverDate, final int mcc, final int mnc, final String cellIdHex, final String lacHex) {
        OpenCellIdRequest request = new OpenCellIdRequest(Request.Method.GET,
                OpenCellIdRequest.makeUrl(OPEN_CELL_ID_GET_URL,
                        OPEN_CELL_ID_API_KEY,
                        mcc,
                        mnc,
                        Integer.parseInt(cellIdHex, 16),
                        Integer.parseInt(lacHex, 16)),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        parseOpenCellIdResponse(serverDate, response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        yandexLbsLocationRequest(serverDate, mcc, mnc, cellIdHex, lacHex);
                    }
                });

        setRetryPolicy(request);
        requestQueue.add(request);
    }

    /** Парсинг результатов запроса в OpenCellId на определение координат по базовым станциям */
    private void parseOpenCellIdResponse(long serverDate, String response) {
        try {
            JSONObject responseJson = new JSONObject(response);
            ValidatedCoordinatesReceivedEvent event = new ValidatedCoordinatesReceivedEvent();
            event.setServerDate(serverDate);
            event.setLatitude(responseJson.getDouble("lat"));
            event.setLongitude(responseJson.getDouble("lon"));
            EventBus.getDefault().post(event);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /** Отправка запроса в Yandex на определение координат по базовым станциям */
    private void yandexLbsLocationRequest(final long serverDate, int mcc, int mnc, String cellIdHex, String lacHex) {
        YandexLBSLocationRequest request = new YandexLBSLocationRequest(Request.Method.GET,
                YandexLBSLocationRequest.makeUrl(YANDEX_LBS_LOCATION_REQUEST,
                        mcc,
                        mnc,
                        Integer.parseInt(cellIdHex, 16),
                        Integer.parseInt(lacHex, 16)),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        parseYandexLbsResponse(serverDate, response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ValidatedCoordinatesReceivedEvent event = new ValidatedCoordinatesReceivedEvent();
                        event.setServerDate(serverDate);
                        event.setLatitude(0);
                        event.setLongitude(0);
                        EventBus.getDefault().post(event);
                    }
                });

        setRetryPolicy(request);
        requestQueue.add(request);
    }

    /** Парсинг результатов запроса в Yandex на определение координат по базовым станциям */
    private void parseYandexLbsResponse(final long serverDate, final String response) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    double latitude = 0;
                    double longitude = 0;

                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(true);
                    XmlPullParser parser = factory.newPullParser();
                    parser.setInput(new StringReader(response));
                    //parser.setInput(new ByteArrayInputStream(response.getBytes()), null);

                    long startTime = Calendar.getInstance().getTimeInMillis();

                    int xmlEvent = parser.getEventType();

                    while (xmlEvent != XmlPullParser.END_DOCUMENT) {
                        String name = parser.getName();

                        switch (xmlEvent) {
                            case XmlPullParser.START_TAG: break;
                            case XmlPullParser.END_TAG:
                                if (name.equals("coordinates")) {
                                    latitude = Double.parseDouble(parser.getAttributeValue(null, "latitude"));
                                    longitude = Double.parseDouble(parser.getAttributeValue(null, "longitude"));
                                }
                        }

                        if (Calendar.getInstance().getTimeInMillis() - startTime > 5000) {
                            break;
                        }

                        xmlEvent = parser.next();
                    }

                    ValidatedCoordinatesReceivedEvent event = new ValidatedCoordinatesReceivedEvent();
                    event.setServerDate(serverDate);
                    event.setLatitude(latitude);
                    event.setLongitude(longitude);
                    EventBus.getDefault().post(event);

                } catch (XmlPullParserException xppe) {
                    xppe.printStackTrace();

                    ValidatedCoordinatesReceivedEvent event = new ValidatedCoordinatesReceivedEvent();
                    event.setServerDate(serverDate);
                    event.setLatitude(0);
                    event.setLongitude(0);
                    EventBus.getDefault().post(event);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Toast.makeText(context, R.string.on_command_send_error_toast, Toast.LENGTH_SHORT).show();
    }

    private Runnable periodicLastStateQueryRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (AppController.getInstance().getActiveDeviceType() != null) {
                        if (AppController.getInstance().getActiveDeviceType().equals(AppController.T6_DEVICE_TYPE) ||
                                AppController.getInstance().getActiveDeviceType().equals(AppController.T5_DEVICE_TYPE)) {
                            try {
                                getLastStateRequest();
                                TimeUnit.SECONDS.sleep(30);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                TimeUnit.SECONDS.sleep(30);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        try {
                            TimeUnit.SECONDS.sleep(30);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }

    //// Methods for Generator

    private void sendManualModeRequestToGenerator() {}

    private void sendAutoModeRequestToGenerator() {}

    private void sendEmergencyStopRequestToGenerator() {}

    private void sendSwitchOnRequestToGenerator() {}

    private void sendSwitchOffRequestToGenerator() {}

    ////

    public class RequestCounter {

        private int count;

        ////

        public void increment() {
            count++;
        }
    }
}