package fr.vrlc.poolmanagerapp;

import java.text.ParseException;
import java.util.TimeZone;

import com.jjoe64.graphview.series.DataPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by rlalanne on 23/05/2017.
 */

public class GraphSeriesGenerator {
    public static DataPoint[] toLineGraphSeries(JSONObject jsonObject) {
        // {"history":[{"celsius":21.562,"timestamp":"2017-05-23 15:00:02"},{"celsius":21.562,"timestamp":"2017-05-23 14:45:02"},{"celsius":21.5,"timestamp":"2017-05-23 14:30:02"},{"celsius":21.5,"timestamp":"2017-05-23 14:15:01"},{"celsius":21.437,"timestamp":"2017-05-23 14:00:02"},{"celsius":21.437,"timestamp":"2017-05-23 13:45:02"},{"celsius":21.437,"timestamp":"2017-05-23 13:30:02"},{"celsius":21.375,"timestamp":"2017-05-23 13:15:02"}]}
        DataPoint[] dataPoints = null;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            JSONArray a = jsonObject.getJSONArray("history");
            dataPoints = new DataPoint[a.length()];
            for (int i = 0; i < a.length(); ++i) {
                JSONObject o = a.getJSONObject(a.length() - 1 - i);
                Date d = dateFormat.parse(o.getString("timestamp"));
                double t = o.getDouble("celsius");
                dataPoints[i] = new DataPoint(d, t);
            }
        } catch (JSONException ex) {
        } catch (ParseException ex) {
        }
        return dataPoints;
    }

    public DataPoint[] convert(char[] downloadedData) {
        DataPoint[] res = null;
        try {
            String s = new String(downloadedData);
            JSONObject jsonObject = new JSONObject(s);
            res = toLineGraphSeries(jsonObject);
        } catch (JSONException ex) {
            res = null;
        }
        return res;
    }
}
