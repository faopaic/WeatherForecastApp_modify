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
                // 表形式（| で始まる行）はそのまま出力
                if (forecast.getDateTime().startsWith("| ")) {
                    System.out.println(forecast.getDateTime());
                }
            }
        } catch (Exception e) {
            System.out.println("天気予報の取得に失敗しました: " + e.getMessage());
        }
    }
}