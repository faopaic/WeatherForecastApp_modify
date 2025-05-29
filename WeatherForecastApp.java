import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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
    // 都道府県コードマップ（主要47都道府県）
    private static final Map<String, String> PREF_CODE_MAP = new LinkedHashMap<>();
    static {
        PREF_CODE_MAP.put("北海道", "016000");
        PREF_CODE_MAP.put("青森県", "020000");
        PREF_CODE_MAP.put("岩手県", "030000");
        PREF_CODE_MAP.put("宮城県", "040000");
        PREF_CODE_MAP.put("秋田県", "050000");
        PREF_CODE_MAP.put("山形県", "060000");
        PREF_CODE_MAP.put("福島県", "070000");
        PREF_CODE_MAP.put("茨城県", "080000");
        PREF_CODE_MAP.put("栃木県", "090000");
        PREF_CODE_MAP.put("群馬県", "100000");
        PREF_CODE_MAP.put("埼玉県", "110000");
        PREF_CODE_MAP.put("千葉県", "120000");
        PREF_CODE_MAP.put("東京都", "130000");
        PREF_CODE_MAP.put("神奈川県", "140000");
        PREF_CODE_MAP.put("新潟県", "150000");
        PREF_CODE_MAP.put("富山県", "160000");
        PREF_CODE_MAP.put("石川県", "170000");
        PREF_CODE_MAP.put("福井県", "180000");
        PREF_CODE_MAP.put("山梨県", "190000");
        PREF_CODE_MAP.put("長野県", "200000");
        PREF_CODE_MAP.put("岐阜県", "210000");
        PREF_CODE_MAP.put("静岡県", "220000");
        PREF_CODE_MAP.put("愛知県", "230000");
        PREF_CODE_MAP.put("三重県", "240000");
        PREF_CODE_MAP.put("滋賀県", "250000");
        PREF_CODE_MAP.put("京都府", "260000");
        PREF_CODE_MAP.put("大阪府", "270000");
        PREF_CODE_MAP.put("兵庫県", "280000");
        PREF_CODE_MAP.put("奈良県", "290000");
        PREF_CODE_MAP.put("和歌山県", "300000");
        PREF_CODE_MAP.put("鳥取県", "310000");
        PREF_CODE_MAP.put("島根県", "320000");
        PREF_CODE_MAP.put("岡山県", "330000");
        PREF_CODE_MAP.put("広島県", "340000");
        PREF_CODE_MAP.put("山口県", "350000");
        PREF_CODE_MAP.put("徳島県", "360000");
        PREF_CODE_MAP.put("香川県", "370000");
        PREF_CODE_MAP.put("愛媛県", "380000");
        PREF_CODE_MAP.put("高知県", "390000");
        PREF_CODE_MAP.put("福岡県", "400000");
        PREF_CODE_MAP.put("佐賀県", "410000");
        PREF_CODE_MAP.put("長崎県", "420000");
        PREF_CODE_MAP.put("熊本県", "430000");
        PREF_CODE_MAP.put("大分県", "440000");
        PREF_CODE_MAP.put("宮崎県", "450000");
        PREF_CODE_MAP.put("鹿児島県", "460100");
        PREF_CODE_MAP.put("沖縄県", "471000");
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("都道府県を選択してください：");
        String[] prefNames = new String[PREF_CODE_MAP.size()];
        // prefNames配列を正しく初期化
        int i = 0;
        for (String name : PREF_CODE_MAP.keySet()) {
            prefNames[i++] = name;
        }
        System.out.println("---------------------------------------------");
        int col = 0;
        for (i = 0; i < prefNames.length; i++) {
            String name = prefNames[i];
            // 全角幅を考慮して都道府県名の後ろに必要なスペースを追加
            int zenkaku = 0;
            for (char c : name.toCharArray()) {
                if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                    Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HIRAGANA ||
                    Character.UnicodeBlock.of(c) == Character.UnicodeBlock.KATAKANA) {
                    zenkaku += 2;
                } else {
                    zenkaku += 1;
                }
            }
            int pad = 8 - zenkaku; // 8幅に揃える
            StringBuilder sb = new StringBuilder(name);
            for (int j = 0; j < pad; j++) sb.append(' ');
            System.out.printf("%2d: %s ", i + 1, sb.toString()); // ←ここで末尾に半角スペース
            col++;
            if (col % 5 == 0) {
                System.out.println();
            }
        }
        if (col % 5 != 0) System.out.println();
        System.out.println("---------------------------------------------");
        int selected = -1;
        while (selected < 1 || selected > PREF_CODE_MAP.size()) {
            System.out.print("番号を入力: ");
            String input = scanner.nextLine();
            try {
                selected = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                selected = -1;
            }
        }
        String prefName = prefNames[selected - 1];
        String prefCode = PREF_CODE_MAP.get(prefName);
        String targetUrl = "https://www.jma.go.jp/bosai/forecast/data/forecast/" + prefCode + ".json";
        WeatherApiClient client = new WeatherApiClient(targetUrl);
        try {
            // 地域名を指定して天気予報を取得するように変更
            for (WeatherForecast forecast : client.fetchWeatherForecasts(prefName.replace("県", "").replace("府", "").replace("都", ""))) {
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
        scanner.close();
    }
}

class WeatherApiClient {
    private final String targetUrl;

    public WeatherApiClient(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public List<WeatherForecast> fetchWeatherForecasts(String region) throws IOException, URISyntaxException {
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