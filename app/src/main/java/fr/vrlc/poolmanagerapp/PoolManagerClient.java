package fr.vrlc.poolmanagerapp;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

/**
 * Created by rlalanne on 27/05/2017.
 */

public class PoolManagerClient {
    private static final String BASE_URL = "http://home.vrlc.fr:8080/";
    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void setState(String service, boolean state, JsonHttpResponseHandler responseHandler) {
        RequestParams params = new RequestParams();
        params.put("service", service);
        params.put("state", state);
        client.get(getAbsoluteUrl("poolSetState"), params, responseHandler);
    }

    public static void readStates(JsonHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl("poolReadStates"), null, responseHandler);
    }
    public static void getTemperature(JsonHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl("poolTemp"), null, responseHandler);
    }
    public static void getTemperatureHistory(int hoursBack, JsonHttpResponseHandler responseHandler) {
        RequestParams params = new RequestParams();
        params.put("hours", hoursBack);
        client.get(getAbsoluteUrl("poolTempHistory"), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
