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

    // 地域名を動的に設定できるようにするため、メソッドに引数を追加
    public List<WeatherForecast> fetchWeatherForecasts(String regionName) throws IOException {
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
                            List<String> windList = new ArrayList<>();
                            List<String> waveList = new ArrayList<>();
                            // 風向きと波高のデータを取得して格納
                            if (timeSeriesArray.length() > 0) {
                                JSONArray areas = timeSeriesArray.getJSONObject(0).optJSONArray("areas");
                                if (areas != null && areas.length() > 0) {
                                    JSONArray winds = areas.getJSONObject(0).optJSONArray("winds");
                                    JSONArray waves = areas.getJSONObject(0).optJSONArray("waves");

                                    if (winds != null) {
                                        for (int i = 0; i < winds.length(); i++) {
                                            String wind = winds.getString(i);
                                            windList.add(convertToHalfWidth(wind)); // 全角スペースを半角スペースに変換
                                        }
                                    }

                                    // 波高データを半角に変換して格納
                                    if (waves != null) {
                                        for (int i = 0; i < waves.length(); i++) {
                                            String wave = waves.getString(i);
                                            waveList.add(convertToHalfWidth(wave));
                                        }
                                    }
                                }
                            }
                            for (int i = 0; i < Math.min(timeDefinesArray.length(), weathersArray.length()); i++) {
                                String dateTimeStr = timeDefinesArray.getString(i);
                                LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr,
                                        DateTimeFormatter.ISO_DATE_TIME);
                                String dateStr = dateTime.toLocalDate().toString().replace("-", "/");
                                String weather = weathersArray.getString(i)
                                        .replace("/", "後")
                                        .replace("　", " ")
                                        .replace("時々", "時どき");

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
                                maxLen = Math.max(maxLen, getDisplayWidth(windList.get(i)));
                                maxLen = Math.max(maxLen, getDisplayWidth(waveList.get(i)));
                                maxLen = Math.max(maxLen, getDisplayWidth("日付"));
                                maxLen = Math.max(maxLen, getDisplayWidth("天気"));
                                maxLen = Math.max(maxLen, getDisplayWidth("最高気温"));
                                maxLen = Math.max(maxLen, getDisplayWidth("最低気温"));
                                maxLen = Math.max(maxLen, getDisplayWidth("風向き"));
                                maxLen = Math.max(maxLen, getDisplayWidth("波高"));
                                if (maxLen % 2 != 0)
                                    maxLen++;
                                colWidths[i] = maxLen;
                            }
                            int labelColWidth = getDisplayWidth("最高気温");
                            labelColWidth = Math.max(labelColWidth, getDisplayWidth("最低気温"));
                            labelColWidth = Math.max(labelColWidth, getDisplayWidth("天気"));
                            labelColWidth = Math.max(labelColWidth, getDisplayWidth("日付"));
                            labelColWidth = Math.max(labelColWidth, getDisplayWidth("風向き"));
                            labelColWidth = Math.max(labelColWidth, getDisplayWidth("波高"));
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
                            // 降水確率データ取得
                            JSONArray popsArray = null;
                            if (timeSeriesArray.length() > 1) {
                                JSONArray popAreas = timeSeriesArray.getJSONObject(1).optJSONArray("areas");
                                if (popAreas != null && popAreas.length() > 0) {
                                    popsArray = popAreas.getJSONObject(0).optJSONArray("pops");
                                }
                            }
                            // 降水確率の平均値を日ごとに計算
                            List<String> avgPopList = new ArrayList<>();
                            if (popsArray != null && popsArray.length() > 0) {
                                JSONArray popTimeDefines = null;
                                if (timeSeriesArray.length() > 1) {
                                    popTimeDefines = timeSeriesArray.getJSONObject(1).getJSONArray("timeDefines");
                                }
                                List<String> dateKeys = new ArrayList<>();
                                List<List<Integer>> popGroups = new ArrayList<>();
                                for (int i = 0; i < popsArray.length(); i++) {
                                    String dateTime = popTimeDefines.getString(i);
                                    String dateKey = dateTime.split("T")[0];
                                    if (dateKeys.isEmpty() || !dateKeys.get(dateKeys.size() - 1).equals(dateKey)) {
                                        dateKeys.add(dateKey);
                                        popGroups.add(new ArrayList<>());
                                    }
                                    String popStr = popsArray.isNull(i) ? "-" : popsArray.getString(i);
                                    try {
                                        int popVal = Integer.parseInt(popStr);
                                        popGroups.get(popGroups.size() - 1).add(popVal);
                                    } catch (Exception e) {
                                        // skip non-numeric
                                    }
                                }
                                // 平均値計算
                                for (List<Integer> group : popGroups) {
                                    if (group.isEmpty()) {
                                        avgPopList.add("-");
                                    } else {
                                        int sum = 0;
                                        for (int v : group)
                                            sum += v;
                                        avgPopList.add(String.valueOf(Math.round((double) sum / group.size())));
                                    }
                                }
                            }
                            // popRowの生成
                            StringBuilder popRow = new StringBuilder("| ");
                            popRow.append(padBoth("降水確率", labelColWidth));
                            for (int i = 0; i < n; i++) {
                                String popVal = (i < avgPopList.size()) ? avgPopList.get(i) + "%" : "-";
                                popRow.append(" | ").append(padBoth(popVal, colWidths[i]));
                            }
                            popRow.append(" |");
                            // 風向き・波高データ取得

                            // 風向きの行を追加
                            StringBuilder windRow = new StringBuilder("| ");
                            windRow.append(padBoth("風向き", labelColWidth));
                            for (int i = 0; i < n; i++) {
                                String windVal = (i < windList.size()) ? windList.get(i) : "-";
                                windRow.append(" | ").append(padBoth(windVal, colWidths[i]));
                            }
                            windRow.append(" |");

                            // 波高の行を追加
                            StringBuilder waveRow = new StringBuilder("| ");
                            waveRow.append(padBoth("波高", labelColWidth));
                            for (int i = 0; i < n; i++) {
                                String waveVal = (i < waveList.size()) ? waveList.get(i) : "-";
                                waveRow.append(" | ").append(padBoth(waveVal, colWidths[i]));
                            }
                            waveRow.append(" |");

                            // 地域名の行を追加
                            StringBuilder regionRow = new StringBuilder("| ");
                            regionRow.append(padBoth("地域", labelColWidth));
                            for (int i = 0; i < n; i++) {
                                regionRow.append(" | ").append(padBoth(regionName, colWidths[i]));
                            }
                            regionRow.append(" |");

                            // 地域名の行を最初に追加
                            forecasts.add(new WeatherForecast(regionRow.toString(), "", "", "", "", "", ""));
                            forecasts.add(new WeatherForecast(dateRow.toString(), "", "", "", "", "", ""));
                            forecasts.add(new WeatherForecast(weatherRow.toString(), "", "", "", "", "", ""));
                            forecasts.add(new WeatherForecast(maxTempRow.toString(), "", "", "", "", "", ""));
                            forecasts.add(new WeatherForecast(minTempRow.toString(), "", "", "", "", "", ""));
                            forecasts.add(new WeatherForecast(popRow.toString(), "", "", "", "", "", ""));
                            forecasts.add(new WeatherForecast(windRow.toString(), "", "", "", "", "", ""));
                            forecasts.add(new WeatherForecast(waveRow.toString(), "", "", "", "", "", ""));
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

    // --- 全角数字を半角数字に変換するユーティリティメソッド ---
    private String convertToHalfWidth(String input) {
        if (input == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            // 全角数字（U+FF10 - U+FF19）を半角数字（U+0030 - U+0039）に変換
            if (c >= '０' && c <= '９') {
                sb.append((char) (c - '０' + '0'));
            }
            // 全角小数点（U+FF0E）を半角小数点（U+002E）に変換
            else if (c == '．') {
                sb.append('.');
            }
            // 全角スペース（U+3000）を半角スペース（U+0020）に変換
            else if (c == '　') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
