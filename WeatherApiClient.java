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
                    JSONArray tempsArray = null;
                    if (timeSeriesArray.length() > 2) {
                        JSONObject tempObj = timeSeriesArray.getJSONObject(2);
                        JSONArray tempAreas = tempObj.optJSONArray("areas");
                        if (tempAreas != null && tempAreas.length() > 0) {
                            JSONObject tempArea = tempAreas.getJSONObject(0);
                            if (tempArea.has("temps")) {
                                tempsArray = tempArea.optJSONArray("temps");
                            }
                        }
                    }
                    if (timeDefinesArray != null && areasArray != null && areasArray.length() > 0) {
                        JSONArray weathersArray = areasArray.getJSONObject(0).optJSONArray("weathers");
                        if (weathersArray != null) {
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
                                        .replace("　", " ");
                                dateList.add(dateStr);
                                weatherList.add(weather);
                                // 気温データ取得（偶数:最低, 奇数:最高）
                                String minTemp = "-";
                                String maxTemp = "-";
                                if (tempsArray != null) {
                                    int minIdx = i * 2;
                                    int maxIdx = i * 2 + 1;
                                    String minVal = null;
                                    String maxVal = null;
                                    if (tempsArray.length() > minIdx) {
                                        minVal = tempsArray.isNull(minIdx) ? "-" : tempsArray.getString(minIdx);
                                        if (minVal != null && !minVal.isEmpty() && !minVal.equals("null"))
                                            minTemp = minVal;
                                    }
                                    if (tempsArray.length() > maxIdx) {
                                        maxVal = tempsArray.isNull(maxIdx) ? "-" : tempsArray.getString(maxIdx);
                                        if (maxVal != null && !maxVal.isEmpty() && !maxVal.equals("null"))
                                            maxTemp = maxVal;
                                    }
                                    // 最高気温と最低気温が同じ場合は最低気温をハイフンに
                                    if (minTemp.equals(maxTemp)) {
                                        minTemp = "-";
                                    }
                                }
                                maxTempList.add(maxTemp);
                                minTempList.add(minTemp);
                            }
                            int n = dateList.size();
                            int[] colWidths = new int[n];
                            for (int i = 0; i < n; i++) {
                                int maxLen = getDisplayWidth(dateList.get(i));
                                maxLen = Math.max(maxLen, getDisplayWidth(weatherList.get(i)));
                                maxLen = Math.max(maxLen, getDisplayWidth(maxTempList.get(i)));
                                maxLen = Math.max(maxLen, getDisplayWidth(minTempList.get(i)));
                                maxLen = Math.max(maxLen, getDisplayWidth("日付"));
                                maxLen = Math.max(maxLen, getDisplayWidth("天気"));
                                maxLen = Math.max(maxLen, getDisplayWidth("最高気温"));
                                maxLen = Math.max(maxLen, getDisplayWidth("最低気温"));
                                if (maxLen % 2 != 0)
                                    maxLen++;
                                colWidths[i] = maxLen;
                            }
                            int labelColWidth = getDisplayWidth("最高気温");
                            labelColWidth = Math.max(labelColWidth, getDisplayWidth("最低気温"));
                            labelColWidth = Math.max(labelColWidth, getDisplayWidth("天気"));
                            labelColWidth = Math.max(labelColWidth, getDisplayWidth("日付"));
                            if (labelColWidth % 2 != 0)
                                labelColWidth++;
                            StringBuilder dateRow = new StringBuilder("| ");
                            dateRow.append(padBoth("日付", labelColWidth));
                            for (int i = 0; i < n; i++) {
                                dateRow.append(" | ").append(padBoth(dateList.get(i), colWidths[i]));
                            }
                            dateRow.append(" |");
                            StringBuilder weatherRow = new StringBuilder("| ");
                            weatherRow.append(padBoth("天気", labelColWidth));
                            for (int i = 0; i < n; i++) {
                                weatherRow.append(" | ").append(padBoth(weatherList.get(i), colWidths[i]));
                            }
                            weatherRow.append(" |");
                            StringBuilder maxTempRow = new StringBuilder("| ");
                            maxTempRow.append(padBoth("最高気温", labelColWidth));
                            for (int i = 0; i < n; i++) {
                                maxTempRow.append(" | ").append(padBoth(maxTempList.get(i), colWidths[i]));
                            }
                            maxTempRow.append(" |");
                            StringBuilder minTempRow = new StringBuilder("| ");
                            minTempRow.append(padBoth("最低気温", labelColWidth));
                            for (int i = 0; i < n; i++) {
                                minTempRow.append(" | ").append(padBoth(minTempList.get(i), colWidths[i]));
                            }
                            minTempRow.append(" |");
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

    // --- 指定幅で両端パディング（全角考慮） ---
    private String padBoth(String s, int width) {
        int pad = width - getDisplayWidth(s);
        int left = pad / 2;
        int right = pad - left;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < left; i++)
            sb.append(' ');
        sb.append(s);
        for (int i = 0; i < right; i++)
            sb.append(' ');
        return sb.toString();
    }
}
