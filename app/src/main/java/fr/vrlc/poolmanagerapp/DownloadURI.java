package fr.vrlc.poolmanagerapp;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by rlalanne on 24/05/2017.
 */
@Deprecated
public class DownloadURI extends AsyncTask<String, Integer, DownloadURI.Result> {

    /**
     * Wrapper class that serves as a union of a result value and an exception. When the
     * download task has completed, either the result value or exception can be a non-null
     * value. This allows you to pass exceptions to the UI thread that were thrown during
     * doInBackground().
     */

    public interface IDownloadURI {
        void onDownloadComplete(String action, char[] r);

        void onDownloadFail(String action, Exception ex);

        NetworkInfo getActiveNetworkInfo();
    }

    private IDownloadURI mCallback;
    private String mAction;

    public DownloadURI(IDownloadURI callback, String action) {
        mCallback = callback;
        mAction = action;
    }

    class Result {
        public char[] mResultValue;
        public Exception mException;

        public Result(char[] resultValue) {
            mResultValue = resultValue;
        }

        public Result(Exception exception) {
            mException = exception;
        }
    }

    /**
     * Cancel background network operation if we do not have network connectivity.
     */
    @Override
    protected void onPreExecute() {
        if (mCallback != null) {
            NetworkInfo networkInfo = mCallback.getActiveNetworkInfo();
            if (networkInfo == null
                    || !networkInfo.isConnected()
                    || (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                    && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                // If no connectivity, cancel task and update Callback with null data.
                mCallback.onDownloadFail(mAction, new Exception("No network"));
                cancel(true);
            }
        }
    }

    /**
     * Defines work to perform on the background thread.
     */
    @Override
    protected DownloadURI.Result doInBackground(String... urls) {
        DownloadURI.Result result = null;
        if (!isCancelled() && urls != null && urls.length > 0) {
            String urlString = urls[0];
            try {
                URL url = new URL(urlString);
                char[] res = downloadUrl(url);
                if (res != null) {
                    result = new DownloadURI.Result(res);
                } else {
                    throw new IOException("No response received.");
                }
            } catch (Exception e) {
                result = new DownloadURI.Result(e);
            }
        }
        return result;
    }

    /**
     * Send DownloadCallback a progress update.
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (values.length >= 2) {
            //mCallback.onProgressUpdate(values[0], values[1]);
        }
    }

    /**
     * Updates the DownloadCallback with the result.
     */
    @Override
    protected void onPostExecute(DownloadURI.Result result) {
        if (result != null && mCallback != null) {
            if (result.mException != null) {
                mCallback.onDownloadFail(mAction, result.mException);
            } else if (result.mResultValue != null) {
                mCallback.onDownloadComplete(mAction, result.mResultValue);
            }
        }
    }

    /**
     * Override to add special behavior for cancelled AsyncTask.
     */
    @Override
    protected void onCancelled(DownloadURI.Result result) {
    }

    /**
     * Given a URL, sets up a connection and gets the HTTP response body from the server.
     * If the network request is successful, it returns the response body in String form. Otherwise,
     * it will throw an IOException.
     */
    private char[] downloadUrl(URL url) throws IOException {
        InputStream stream = null;
        HttpURLConnection connection = null;
        char[] result = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.setReadTimeout(3000);
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.setConnectTimeout(3000);
            // For this use case, set HTTP method to GET.
            connection.setRequestMethod("GET");
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);
            // Open communications link (network traffic occurs here).
            connection.connect();
            publishProgress(DownloadCallback.Progress.CONNECT_SUCCESS);
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();
            publishProgress(DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS, 0);
            if (stream != null) {
                // Converts Stream to String with max length of 5000.
                result = readStream(stream, 5 * 1024 * 1024);
                publishProgress(DownloadCallback.Progress.PROCESS_INPUT_STREAM_SUCCESS, 0);
            }
        } finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                stream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    /**
     * Converts the contents of an InputStream to a String.
     */
    private char[] readStream(InputStream stream, int maxLength) throws IOException {
        char[] result = null;
        // Read InputStream using the UTF-8 charset.
        InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
        // Create temporary buffer to hold Stream data with specified max length.
        char[] buffer = new char[maxLength];
        // Populate temporary buffer with Stream data.
        int numChars = 0;
        int readSize = 0;
        while (numChars < maxLength && readSize != -1) {
            numChars += readSize;
            int pct = (100 * numChars) / maxLength;
            publishProgress(DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS, pct);
            readSize = reader.read(buffer, numChars, buffer.length - numChars);
        }
        if (numChars != -1) {
            // The stream was not empty.
            // Create String that is actual length of response body if actual length was less than
            // max length.
            numChars = Math.min(numChars, maxLength);
            //result = new String(buffer, 0, numChars);
            result = Arrays.copyOf(buffer, numChars);
        }
        return result;
    }
}
