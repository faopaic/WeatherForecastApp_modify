import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
                JSONArray timeSeriesArray = rootArray.getJSONObject(0).optJSONArray("timeSeries");
                if (timeSeriesArray != null && timeSeriesArray.length() > 0) {
                    JSONObject timeStringObject = timeSeriesArray.getJSONObject(0);
                    JSONArray timeDefinesArray = timeStringObject.optJSONArray("timeDefines");
                    JSONArray areasArray = timeStringObject.optJSONArray("areas");
                    if (timeDefinesArray != null && areasArray != null && areasArray.length() > 0) {
                        JSONArray weathersArray = areasArray.getJSONObject(0).optJSONArray("weathers");
                        if (weathersArray != null) {
                            // 日付（yyyy/MM/dd）＋天気（スペース区切り）で1行出力用にまとめる
                            for (int i = 0; i < Math.min(timeDefinesArray.length(), weathersArray.length()); i++) {
                                String dateTimeStr = timeDefinesArray.getString(i);
                                LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr,
                                        DateTimeFormatter.ISO_DATE_TIME);
                                String weather = weathersArray.getString(i);
                                // 日付をyyyy/MM/dd形式に変換
                                String dateStr = dateTime.toLocalDate().toString().replace("-", "/");
                                forecasts.add(new WeatherForecast(dateStr + " " + weather, ""));
                            }
                        }
                    }
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
