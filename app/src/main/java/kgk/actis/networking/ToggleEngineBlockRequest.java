package kgk.actis.networking;


import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

class ToggleEngineBlockRequest extends StringRequest {

    private String session;

    ////

    ToggleEngineBlockRequest(int method,
                         String url,
                         Response.Listener<String> listener,
                         Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
    }

    ////

    void setSession(String session) {
        this.session = session;
    }

    ////

    static String makeUrl(String deviceId) {
        return VolleyHttpClient.DOMAIN + VolleyHttpClient.API
                    + "cmdtoggleblockengine?device=" + deviceId;
    }

    //// REQUEST

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", session);
        return headers;
    }
}
