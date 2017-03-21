package kgk.actis.networking;


import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import kgk.actis.util.AppController;

public class BlockEngine {

    public interface Listener {

        int NETWORK_ERROR = -10;
        int PARSE_ERROR = -20;
        int BLOCKING_NOT_AVAILABLE = -1;
        int BLOCKING_DEACTIVATED = 0;
        int BLOCKING_ACTIVATED = 1;
        int TOGGLE_BLOCK_ENGINE_SUCCESS = 2;

        ////

        void onBlockEngineFunctionAvailabilityReceived(int result);

        void onToggleBlockEngineStatusReceived(int result);
    }

    ////

    private VolleyHttpClient httpClient;
    private ArrayList<Listener> listeners;

    ////

    public BlockEngine() {
        httpClient = VolleyHttpClient.getInstance(AppController.getInstance());
        listeners = new ArrayList<>();
    }

    ////

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void requestBlockEngineFunctionAvailability(String deviceId) {
        String url = GetDeviceInfoRequest.makeUrl(deviceId);
        GetDeviceInfoRequest request = new GetDeviceInfoRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            int result;

                            JSONObject responseJson = new JSONObject(response);

                            boolean requestStatus = responseJson.getBoolean("status");

                            if (requestStatus) {
                                JSONObject dataJson = responseJson.getJSONObject("data");
                                switch (dataJson.getInt("blocking")) {
                                    case -1: result = Listener.BLOCKING_NOT_AVAILABLE; break;
                                    case 0: result = Listener.BLOCKING_DEACTIVATED; break;
                                    case 1: result = Listener.BLOCKING_ACTIVATED; break;
                                    default: result = Listener.NETWORK_ERROR;
                                }
                            } else {
                                result = Listener.NETWORK_ERROR;
                            }

                            for (Listener listener : listeners) {
                                listener.onBlockEngineFunctionAvailabilityReceived(result);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();

                            for (Listener listener : listeners) {
                                listener.onBlockEngineFunctionAvailabilityReceived(
                                        Listener.PARSE_ERROR);
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        for (Listener listener : listeners) {
                            listener.onBlockEngineFunctionAvailabilityReceived(
                                    Listener.NETWORK_ERROR);
                        }
                    }
                });

        request.setSession(VolleyHttpClient.phpSessId);
        httpClient.addRequest(request);
    }

    public void requestToggleBlockEngine(String deviceId) {
        String url = ToggleEngineBlockRequest.makeUrl(deviceId);
        ToggleEngineBlockRequest request = new ToggleEngineBlockRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            int result;

                            JSONObject responseJson = new JSONObject(response);

                            boolean requestStatus = responseJson.getBoolean("status");

                            if (requestStatus) {
                               result = Listener.TOGGLE_BLOCK_ENGINE_SUCCESS;
                            } else {
                                result = Listener.NETWORK_ERROR;
                            }

                            for (Listener listener : listeners) {
                                listener.onToggleBlockEngineStatusReceived(result);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();

                            for (Listener listener : listeners) {
                                listener.onToggleBlockEngineStatusReceived(
                                        Listener.PARSE_ERROR);
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        for (Listener listener : listeners) {
                            listener.onToggleBlockEngineStatusReceived(
                                    Listener.NETWORK_ERROR);
                        }
                    }
                });

        request.setSession(VolleyHttpClient.phpSessId);
        httpClient.addRequest(request);
    }
}
