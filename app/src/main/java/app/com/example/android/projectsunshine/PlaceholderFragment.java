package app.com.example.android.projectsunshine;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlaceholderFragment extends Fragment {
    ArrayAdapter<String> forecastAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_placeholder, container, false);

        //creating some fake data
        String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };
        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));

        forecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.listitem_forecast,
                R.id.listitem_forecast_textview,
                weekForecast);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(forecastAdapter);

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
            //framework does not allow you to run network operations on the main thread
            //android apps run by default on the main thread - also called the UI thread
            //it handles all user IO, avoid any time consuming operations on the main thread
            //otherwise this could cause the UI to stutter
            //instead we should open a background worker thread and perform network IO there
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
            Log.e("PlaceholderFragment", "Error", ex);
            return null;
        } finally {
            if(urlConnection != null) {
                urlConnection.disconnect();
            }

            if(reader != null) {
                try {
                    reader.close();
                } catch(final IOException ex) {
                    Log.e("PlaceholderFragment", "Error closing stream", ex);
                }
            }
        }

        return rootView;
    }
}
