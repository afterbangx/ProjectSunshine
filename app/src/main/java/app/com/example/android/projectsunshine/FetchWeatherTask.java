package app.com.example.android.projectsunshine;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by amilbeck on 11/22/2016.
 */

/*************************************************************************************
 *   framework does not allow you to run network operations on the main thread       *
 *   android apps run by default on the main thread - also called the UI thread      *
 *   it handles all user IO, avoid any time consuming operations on the main thread  *
 *   otherwise this could cause the UI to stutter                                    *
 *   instead we should open a background worker thread and perform network IO there  *
 ************************************************************************************/

public class FetchWeatherTask extends AsyncTask<Void, Void, Void> {
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    @Override
    protected Void doInBackground(Void... params) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String forecastJsonStr = null;

        try {
            //construct the url for the openweathermapquery
            String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?q=32327&mode=json&units=metric&cnt=7";
            String apiKey = "&APPID=" + BuildConfig.OPEN_WEATHER_MAP_API_KEY;
            URL url = new URL(baseUrl.concat(apiKey));

            //create request to openweathermap and open connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            //read input stream
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if(inputStream == null) {
                //nothing to do
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while((line = reader.readLine()) != null) {
                //newline is only to make debugging easier, otherwise useless
                buffer.append(line + "\n");
            }

            if(buffer.length() == 0) {
                //stream was empty, no point in parsing
                return null;
            }

            forecastJsonStr = buffer.toString();

        } catch(IOException ex) {
            Log.e(LOG_TAG, "Error", ex);
            return null;
        } finally {
            if(urlConnection != null) {
                urlConnection.disconnect();
            }

            if(reader != null) {
                try {
                    reader.close();
                } catch(final IOException ex) {
                    Log.e(LOG_TAG, "Error closing stream", ex);
                }
            }
        }
        return null;
    }
}
