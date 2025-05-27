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
        try {
            for (WeatherForecast forecast : client.fetchWeatherForecasts()) {
                // 日付を yyyy-MM-dd または yyyy/MM/dd → yyyy年M月d日 に変換
                String dateStr = forecast.getDateTime();
                String year = "", month = "", day = "";
                if (dateStr.contains("T")) {
                    String[] dateParts = dateStr.split("T")[0].split("-");
                    if (dateParts.length == 3) {
                        year = dateParts[0];
                        month = String.valueOf(Integer.parseInt(dateParts[1]));
                        day = String.valueOf(Integer.parseInt(dateParts[2]));
                    }
                } else if (dateStr.contains("/")) {
                    String[] dateParts = dateStr.split("/");
                    if (dateParts.length == 3) {
                        year = dateParts[0];
                        month = String.valueOf(Integer.parseInt(dateParts[1]));
                        day = String.valueOf(Integer.parseInt(dateParts[2].split(" ")[0]));
                    }
                }
                String weather = forecast.getWeather();
                String maxTemp = forecast.getMaxTemp();
                String minTemp = forecast.getMinTemp();
                String pop = forecast.getPop();
                String wind = forecast.getWindDirection();
                String wave = forecast.getWaveHeight();
                if (!year.isEmpty()) {
                    System.out.println(year + "年" + month + "月" + day + "日 の天気: " + weather
                            + " 最高気温: " + maxTemp + "℃ 最低気温: " + minTemp + "℃ 降水確率: " + pop + "%"
                            + " 風向き: " + wind + " 波高: " + wave);
                } else {
                    // フォールバック（元の出力）
                    System.out.println(forecast.getDateTime());
                }
            }
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
            JSONArray timeSeries = rootArray.getJSONObject(0).getJSONArray("timeSeries");
            // 天気
            JSONObject weatherObj = timeSeries.getJSONObject(0);
            JSONArray timeDefinesArray = weatherObj.getJSONArray("timeDefines");
            JSONArray weatherAreas = weatherObj.getJSONArray("areas");
            JSONArray weathersArray = weatherAreas.getJSONObject(0).getJSONArray("weathers");
            // 降水確率
            JSONArray popsArray = null;
            if (timeSeries.length() > 1) {
                JSONArray popAreas = timeSeries.getJSONObject(1).getJSONArray("areas");
                popsArray = popAreas.getJSONObject(0).optJSONArray("pops");
            }
            // 気温
            JSONArray tempsArray = null;
            if (timeSeries.length() > 2) {
                JSONArray tempAreas = timeSeries.getJSONObject(2).getJSONArray("areas");
                tempsArray = tempAreas.getJSONObject(0).optJSONArray("temps");
            }
            // 風向きと波高のデータを取得して格納
            JSONArray windList = new JSONArray();
            JSONArray waveList = new JSONArray();
            if (timeSeries.length() > 0) {
                JSONArray windAreas = timeSeries.getJSONObject(0).getJSONArray("areas");
                if (windAreas.length() > 0) {
                    windList = windAreas.getJSONObject(0).optJSONArray("winds");
                    waveList = windAreas.getJSONObject(0).optJSONArray("waves");
                }
            }

            for (int i = 0; i < timeDefinesArray.length(); i++) {
                String dateTimeStr = timeDefinesArray.getString(i);
                String weather = weathersArray.optString(i, "-");
                String pop = (popsArray != null && i < popsArray.length()) ? popsArray.optString(i, "-") : "-";
                String maxTemp = "-";
                String minTemp = "-";
                if (tempsArray != null && tempsArray.length() >= (i * 2 + 2)) {
                    minTemp = tempsArray.optString(i * 2, "-");
                    maxTemp = tempsArray.optString(i * 2 + 1, "-");
                }
                String wind = (windList != null && i < windList.length()) ? windList.optString(i, "-") : "-";
                String wave = (waveList != null && i < waveList.length()) ? waveList.optString(i, "-") : "-";
                forecasts.add(new WeatherForecast(dateTimeStr, weather, maxTemp, minTemp, pop, wind, wave));
            }
        } else {
            throw new IOException("データの取得に失敗しました！");
        }

        return forecasts;
    }
}