import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
/**
 * 天気予報アプリ - 本体
 * このアプリケーションは、気象庁のWeb APIから大阪府の天気予報データを取得取得して表示します
 * 
 * @author n.katayama
 * @version 1.0.1
 */


    public class WeatherForecastApp {
        private static final String TARGET_URL = "https://www.jma.go.jp/bosai/forecast/data/forecast/270000.json";

        public static void main(String[] args) {
            WeatherApiClient client = new WeatherApiClient(TARGET_URL);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("weather.html"))) {
                writer.write("<html><head><meta charset=\"UTF-8\"><title>天気予報</title></head><body>\n");
                for (WeatherForecast forecast : client.fetchWeatherForecasts()) {
                    String[] dateParts = forecast.getDateTime().split("/");
                    if (dateParts.length == 3) {
                        int year = Integer.parseInt(dateParts[0]);
                        int month = Integer.parseInt(dateParts[1]);
                        int day = Integer.parseInt(dateParts[2].split(" ")[0]);
                        String weather = forecast.getWeather();
                        if (forecast.getDateTime().contains(" ")) {
                            weather = forecast.getDateTime().substring(forecast.getDateTime().indexOf(" ") + 1);
                        }
                        writer.write(year + "年" + month + "月" + day + "日 の天気は " + weather + " です。<br>\n");
                    } else {
                        writer.write(forecast.getDateTime() + " " + forecast.getWeather() + "<br>\n");
                    }
                }
                writer.write("</body></html>");
            } catch (Exception e) {
                System.out.println("天気予報の取得に失敗しました: " + e.getMessage());
            }
        }
    }

class WeatherApiClient {
    private final String targetUrl;

    public WeatherApiClient(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public List<WeatherForecast> fetchWeatherForecasts() throws IOException, URISyntaxException {
        List<WeatherForecast> forecasts = new ArrayList<>();
        URI uri = new URI(targetUrl);
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
            JSONArray weathersArray = timeStringObject.getJSONArray("areas").getJSONObject(0).getJSONArray("weathers");

            for (int i = 0; i < timeDefinesArray.length(); i++) {
                String dateTimeStr = timeDefinesArray.getString(i);
                String weather = weathersArray.getString(i);
                forecasts.add(new WeatherForecast(dateTimeStr, weather));
            }
        } else {
            throw new IOException("データの取得に失敗しました！");
        }

        return forecasts;
    }
}