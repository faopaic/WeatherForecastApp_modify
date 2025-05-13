import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherApiClient {
    private final String apiUrl;

    public WeatherApiClient(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public List<WeatherForecast> fetchWeatherForecasts() throws IOException {
        List<WeatherForecast> forecasts = new ArrayList<>();
        try {
            URI uri = new URI(apiUrl);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder responseBody = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBody.append(line);
                    }
                }
                JSONArray rootArray = new JSONArray(responseBody.toString());
                JSONObject timeStringObject = rootArray.getJSONObject(0).optJSONArray("timeSeries").getJSONObject(0);
                JSONArray timeDefinesArray = timeStringObject.getJSONArray("timeDefines");
                JSONArray weathersArray = timeStringObject.getJSONArray("areas").getJSONObject(0)
                        .getJSONArray("weathers");
                for (int i = 0; i < timeDefinesArray.length(); i++) {
                    String dateTime = timeDefinesArray.getString(i);
                    String weather = weathersArray.getString(i);
                    forecasts.add(new WeatherForecast(dateTime, weather));
                }
            } else {
                throw new IOException("データの取得に失敗しました！");
            }
        } catch (Exception e) {
            throw new IOException("天気予報データの取得に失敗しました: " + e.getMessage(), e);
        }
        return forecasts;
    }
}
