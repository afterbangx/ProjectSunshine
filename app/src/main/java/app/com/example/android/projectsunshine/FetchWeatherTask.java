package app.com.example.android.projectsunshine;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by amilbeck on 11/22/2016.
 */
public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
    private ForecastFragment forecastFragment;
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    private List<String> forecasts = new ArrayList<String>();

    public FetchWeatherTask(ForecastFragment forecastFragment) {
        this.forecastFragment = forecastFragment;
    }

    private String getReadableDateString(Date time) {
        //because the api returns a unix timestamp (measured in seconds)
        //it must be converted to milliseconds in order to be converted to valid date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd");
        return dateFormat.format(time);
    }

    /**
     * Prepare the weather min/max for presentation
     */
    private String formatMinMax(double min, double max) {
        long roundedMin = Math.round(min);
        long roundedMax = Math.round(max);

        String minMaxStr = roundedMax + "/" + roundedMin;
        return minMaxStr;
    }

    /**
     * Take the string representing the complete forecast in JSON format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p>
     * Fortunately parsing is easy: constructor takes the JSON string and converts it
     * into an Object Hierarchy for us
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numdays)
            throws JSONException {

        //names of JSON objects to be extracted
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        GregorianCalendar calendar = new GregorianCalendar();

        String[] weatherResults = new String[numdays];
        for (int i = 0; i < weatherArray.length(); i++) {
            String day;
            String description;
            String highAndLow;

            JSONObject dayForecast = weatherArray.getJSONObject(i);

            //get day of week
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(GregorianCalendar.DATE, i);
            day = getReadableDateString(calendar.getTime());

            //get weather description for current day
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            //get min and max temps
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double max = temperatureObject.getDouble(OWM_MAX);
            double min = temperatureObject.getDouble(OWM_MIN);
            highAndLow = formatMinMax(min, max);

            //add forecast for each day to the array
            weatherResults[i] = day + " - " + description + " - " + highAndLow;
        }

        return weatherResults;
    }

    @Override
    protected String[] doInBackground(String... params) {
        //if no zip, nothing to look up
        if (params.length == 0) {
            return null;
        }

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String forecastJsonStr = null;
        String format = "json";
        String units = "metric";
        int numDays = 7;

        try {
            //construct the url for the openweathermapquery
            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "APPID";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                    .build();

            URL url = new URL(builtUri.toString());

            //create request to openweathermap and open connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            //read input stream
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                //nothing to do
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                //newline is only to make debugging easier, otherwise useless
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                //stream was empty, no point in parsing
                return null;
            }

            forecastJsonStr = buffer.toString();
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Error", ex);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException ex) {
                    Log.e(LOG_TAG, "Error closing stream", ex);
                }
            }
        }

        try {
            return getWeatherDataFromJson(forecastJsonStr, numDays);
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage(), ex);
            ex.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String[] result) {
        if (result != null) {
            forecastFragment.forecastAdapter.clear();
            for (String dayForecast : result) {
                forecastFragment.forecastAdapter.add(dayForecast);
            }
        }
    }
}
