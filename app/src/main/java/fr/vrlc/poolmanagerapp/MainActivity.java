package fr.vrlc.poolmanagerapp;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.BaseSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_MESSAGE = "fr.vrlc.poolmanagerapp.MESSAGE";
    //public final static String ACTION_GRAPH_DOWNLOADED = "fr.vrlc.poolmanagerapp.GRAPH_DOWNLOADED";
    //public final static String ACTION_STATES_DOWNLOADED = "fr.vrlc.poolmanagerapp.STATES_DOWNLOADED";
    private final static int N_HOURS = 48;

    private GraphView mGraph;
    private BaseSeries mGraphSeries;
    private boolean mDownloadingGraph = false;
    private boolean mDownloadingStates = false;
    private final Handler mHandler = new Handler();
    private Runnable mRunnableGraph;
    private Runnable mRunnableStates;
    private Context mActivityContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mActivityContext = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //PoolTempService.startGetTemp(this);
        // Register alarms
        PoolTempService.registerAlarm(this);

        // Init switch states
        Switch sFiltration = (Switch) findViewById(R.id.switch_filtration);
        Switch sSurpresseur = (Switch) findViewById(R.id.switch_surpresseur);
        Switch sSpot = (Switch) findViewById(R.id.switch_spotlight);
        sFiltration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                Toast.makeText(mActivityContext, "Filtration changed", Toast.LENGTH_SHORT);
            }
        });
        sSurpresseur.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                Toast.makeText(mActivityContext, "Surpresseur changed", Toast.LENGTH_SHORT);
            }
        });
        sSpot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                PoolManagerClient.setState("spot", isChecked, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        // Ok
                        Toast.makeText(mActivityContext, "Spot changed", Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        // revert sSpot check state?
                        Toast.makeText(mActivityContext, "Spot not changed!", Toast.LENGTH_SHORT);
                    }
                });
            }
        });

        // Init the graph
        mGraph = (GraphView) findViewById(R.id.graph);
        mGraph.removeAllSeries();
        mGraphSeries = new LineGraphSeries();
        mGraph.addSeries(mGraphSeries);

        // as we use dates as labels, the human rounding to nice readable numbers
        // is not necessary
        mGraph.getGridLabelRenderer().setHumanRounding(true);

        // set date label formatter
        final DateAsXAxisLabelFormatter dateFormatter = new DateAsXAxisLabelFormatter(mGraph.getContext());
        mGraph.getGridLabelRenderer().setLabelFormatter(dateFormatter);
        mGraph.getGridLabelRenderer().setNumHorizontalLabels(7);
        //mGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);

        mGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    Date d = new Date();
                    d.setTime((long) value);
                    DateFormat df = new SimpleDateFormat("HH'h'mm");
                    return df.format(d);
                } else {
                    return super.formatLabel(value, isValueX);
                }
            }
        });

        // set manual x bounds to have nice steps
        Calendar calendar = Calendar.getInstance();
        mGraph.getViewport().setMaxX(calendar.getTime().getTime());
        calendar.add(Calendar.HOUR, -N_HOURS);
        mGraph.getViewport().setMinX(calendar.getTime().getTime());
        mGraph.getViewport().setXAxisBoundsManual(true);

        mGraph.getViewport().setMinY(17);
        mGraph.getViewport().setMaxY(28);
        mGraph.getViewport().setYAxisBoundsManual(true);

        mGraph.getViewport().setScrollable(true);
        mGraph.getViewport().setScalable(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // TODO Refresh button states
        if (!mDownloadingGraph) {
            mDownloadingGraph = true;
            PoolManagerClient.getTemperatureHistory(N_HOURS, new JsonHttpResponseHandler() {
                @Override
                public void onFinish() {
                    mDownloadingGraph = false;
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    runRefreshGraph(response);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    // reset graph?
                }
            });
            //mDownloadingGraph = true;
            //DownloadURI downloaderGraph = new DownloadURI(this, ACTION_GRAPH_DOWNLOADED);
            //downloaderGraph.execute("http://home.vrlc.fr:8080/poolTempHistory?hours=" + N_HOURS);
        }
        if (!mDownloadingStates) {
            mDownloadingStates = true;
            PoolManagerClient.readStates(new JsonHttpResponseHandler() {
                @Override
                public void onFinish() {
                    mDownloadingStates = false;
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    runSetSwitchStates(response);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    // Disable switches?
                }
            });
            //mDownloadingStates = true;
            //DownloadURI downloaderStates = new DownloadURI(this, ACTION_STATES_DOWNLOADED);
            //downloaderStates.execute("http://home.vrlc.fr:8080/poolReadStates");
        }


    }


    public void refreshGraph(DataPoint[] data) {
        // generate Dates
        mGraphSeries.resetData(data);

        //
        double maxValue = Math.ceil(mGraphSeries.getHighestValueY());    // here, you find your max value
        double minValue = Math.floor(mGraphSeries.getLowestValueY());    // here, you find your max value
        double interval = 0.5;

        // set manual bounds
        mGraph.getViewport().setMinY(minValue);
        mGraph.getViewport().setMaxY(maxValue);
        // indicate number of vertical labels
        mGraph.getGridLabelRenderer().setNumVerticalLabels(Double.valueOf((maxValue - minValue) / interval + 1).intValue());
        // now, it's ok, you should have a graph with integer labels
    }

    /**
     * Called when the user taps the Send button
     */
    public void sendMessage(View view) {
        //Intent intent = new Intent(this, DisplayMessageActivity.class);
        //EditText editText = (EditText) findViewById(R.id.editText);
        //String message = editText.getText().toString();
        //intent.putExtra(EXTRA_MESSAGE, message);
        //startActivity(intent);
    }

    private void runRefreshGraph(char[] data) {
        final DataPoint[] dataPoints = new GraphSeriesGenerator().convert(data);
        mRunnableGraph = new Runnable() {
            @Override
            public void run() {
                refreshGraph(dataPoints);
            }
        };
        mHandler.postDelayed(mRunnableGraph, 100);
    }

    private void runRefreshGraph(JSONObject jsonObject) {
        final DataPoint[] fdata = GraphSeriesGenerator.toLineGraphSeries(jsonObject);
        mRunnableGraph = new Runnable() {
            @Override
            public void run() {
                refreshGraph(fdata);
            }
        };
        mHandler.postDelayed(mRunnableGraph, 100);
    }

    private void runSetSwitchStates(JSONObject jsonObject) {
        final JSONObject json = jsonObject;
        mRunnableStates = new Runnable() {
            @Override
            public void run() {
                // Set states
                Switch sFiltration = (Switch) findViewById(R.id.switch_filtration);
                Switch sSurpresseur = (Switch) findViewById(R.id.switch_surpresseur);
                Switch sSpot = (Switch) findViewById(R.id.switch_spotlight);
                try {
                    sSpot.setChecked(json.getBoolean("spot"));
                    sSurpresseur.setChecked(json.getBoolean("surpresseur"));
                    sFiltration.setChecked(json.getBoolean("filtration"));
                } catch (JSONException ex) {
                    Toast.makeText(mActivityContext, "JSONException", Toast.LENGTH_SHORT);
                }
            }
        };
        mHandler.postDelayed(mRunnableStates, 100);
    }

    //@Override
    //public void onDownloadComplete(String action, char[] data) {
    //    if (action == ACTION_GRAPH_DOWNLOADED) {
    //        runRefreshGraph(data);
    //        mDownloadingGraph = false;
    //    }

    //    if (action == ACTION_STATES_DOWNLOADED) {
    //        try {
    //            runSetSwitchStates(new JSONObject(new String(data)));
    //            mDownloadingStates = false;
    //        } catch (JSONException ex) {
    //            // TODO
    //        }
    //    }
    //}


    //@Override
    //public void onDownloadFail(String action, Exception ex) {
    //    if (action == ACTION_STATES_DOWNLOADED) {
    //        mDownloadingStates = false;
    //    } else if (action == ACTION_GRAPH_DOWNLOADED) {
    //        mDownloadingGraph = false;
    //    }
    //}

    //@Override
    //public NetworkInfo getActiveNetworkInfo() {
    //    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    //    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    //    return networkInfo;
    //}
}
