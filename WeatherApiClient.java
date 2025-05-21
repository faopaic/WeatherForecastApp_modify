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
                    // --- 気温データ取得用 ---
                    JSONArray tempAreasArray = null;
                    JSONArray tempsMaxArray = null;
                    JSONArray tempsMinArray = null;
                    if (timeSeriesArray.length() > 1) {
                        JSONObject tempObject = timeSeriesArray.getJSONObject(1);
                        tempAreasArray = tempObject.optJSONArray("areas");
                        if (tempAreasArray != null && tempAreasArray.length() > 0) {
                            JSONObject tempArea = tempAreasArray.getJSONObject(0);
                            tempsMaxArray = tempArea.optJSONArray("tempsMax");
                            tempsMinArray = tempArea.optJSONArray("tempsMin");
                        }
                    }
                    if (timeDefinesArray != null && areasArray != null && areasArray.length() > 0) {
                        JSONArray weathersArray = areasArray.getJSONObject(0).optJSONArray("weathers");
                        if (weathersArray != null) {
                            // 日付ごとに天気をまとめて表形式で出力するための準備
                            List<String> dateList = new ArrayList<>();
                            List<String> weatherList = new ArrayList<>();
                            List<String> maxTempList = new ArrayList<>();
                            List<String> minTempList = new ArrayList<>();
                            for (int i = 0; i < Math.min(timeDefinesArray.length(), weathersArray.length()); i++) {
                                String dateTimeStr = timeDefinesArray.getString(i);
                                LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr,
                                        DateTimeFormatter.ISO_DATE_TIME);
                                String dateStr = dateTime.toLocalDate().toString().replace("-", "/");
                                String weather = weathersArray.getString(i)
                                        .replace("/", "後")
                                        .replace("　", " "); // 全角スペースを半角スペースに
                                dateList.add(dateStr);
                                weatherList.add(weather);
                                // 気温データ取得
                                String maxTemp = "-";
                                String minTemp = "-";
                                if (tempsMaxArray != null && i < tempsMaxArray.length()) {
                                    maxTemp = tempsMaxArray.isNull(i) ? "-" : tempsMaxArray.getString(i);
                                }
                                if (tempsMinArray != null && i < tempsMinArray.length()) {
                                    minTemp = tempsMinArray.isNull(i) ? "-" : tempsMinArray.getString(i);
                                }
                                maxTempList.add(maxTemp);
                                minTempList.add(minTemp);
                            }
                            // 列幅を揃えるため、日付・天気・気温の最大「表示幅」を計算（日本語は2文字分とする）
                            int[] colWidths = new int[dateList.size()];
                            for (int i = 0; i < dateList.size(); i++) {
                                int dateLen = getDisplayWidth(dateList.get(i));
                                int weatherLen = getDisplayWidth(weatherList.get(i));
                                int maxTempLen = getDisplayWidth(maxTempList.get(i));
                                int minTempLen = getDisplayWidth(minTempList.get(i));
                                colWidths[i] = Math.max(Math.max(dateLen, weatherLen), Math.max(maxTempLen, minTempLen));
                            }
                            // 表形式の1行目（日付）
                            StringBuilder dateRow = new StringBuilder("| 日付");
                            for (int i = 0; i < dateList.size(); i++) {
                                String date = dateList.get(i);
                                int pad = colWidths[i] - getDisplayWidth(date);
                                dateRow.append(" | ").append(date);
                                for (int j = 0; j < pad; j++)
                                    dateRow.append(" ");
                            }
                            dateRow.append("  |");
                            // 表形式の2行目（天気）
                            StringBuilder weatherRow = new StringBuilder("| 天気");
                            for (int i = 0; i < weatherList.size(); i++) {
                                String mark = weatherList.get(i);
                                int pad = colWidths[i] - getDisplayWidth(mark);
                                weatherRow.append(" | ").append(mark);
                                for (int j = 0; j < pad; j++)
                                    weatherRow.append(" ");
                            }
                            weatherRow.append(" |");
                            // 表形式の3行目（最高気温）
                            StringBuilder maxTempRow = new StringBuilder("| 最高気温");
                            for (int i = 0; i < maxTempList.size(); i++) {
                                String temp = maxTempList.get(i);
                                int pad = colWidths[i] - getDisplayWidth(temp);
                                maxTempRow.append(" | ").append(temp);
                                for (int j = 0; j < pad; j++)
                                    maxTempRow.append(" ");
                            }
                            maxTempRow.append(" |");
                            // 表形式の4行目（最低気温）
                            StringBuilder minTempRow = new StringBuilder("| 最低気温");
                            for (int i = 0; i < minTempList.size(); i++) {
                                String temp = minTempList.get(i);
                                int pad = colWidths[i] - getDisplayWidth(temp);
                                minTempRow.append(" | ").append(temp);
                                for (int j = 0; j < pad; j++)
                                    minTempRow.append(" ");
                            }
                            minTempRow.append(" |");
                            // 各行をWeatherForecastに格納
                            forecasts.add(new WeatherForecast(dateRow.toString(), ""));
                            forecasts.add(new WeatherForecast(weatherRow.toString(), ""));
                            forecasts.add(new WeatherForecast(maxTempRow.toString(), ""));
                            forecasts.add(new WeatherForecast(minTempRow.toString(), ""));
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

    // --- ユーティリティ: 日本語は2文字分で幅を計算 ---
    private int getDisplayWidth(String s) {
        int width = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // 全角（日本語など）は2、半角は1
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                    Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HIRAGANA ||
                    Character.UnicodeBlock.of(c) == Character.UnicodeBlock.KATAKANA ||
                    (c >= 0xFF01 && c <= 0xFF60) || (c >= 0xFFE0 && c <= 0xFFE6)) {
                width += 2;
            } else {
                width += 1;
            }
        }
        return width;
    }
}
