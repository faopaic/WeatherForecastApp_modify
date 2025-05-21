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
                // 日付を yyyy/MM/dd → yyyy年M月d日 に変換
                String[] dateParts = forecast.getDateTime().split("/");
                if (dateParts.length == 3) {
                    int year = Integer.parseInt(dateParts[0]);
                    int month = Integer.parseInt(dateParts[1]);
                    int day = Integer.parseInt(dateParts[2].split(" ")[0]);
                    String weather = forecast.getWeather();
                    // 天気情報が日付の後ろに含まれている場合の対応
                    if (forecast.getDateTime().contains(" ")) {
                        weather = forecast.getDateTime().substring(forecast.getDateTime().indexOf(" ") + 1);
                    }
                    System.out.println(year + "年" + month + "月" + day + "日 の天気は " + weather + " です。");
                } else {
                    // フォールバック（元の出力）
                    System.out.println(forecast.getDateTime() + " " + forecast.getWeather());
                }
            }
        } catch (Exception e) {
            System.out.println("天気予報の取得に失敗しました: " + e.getMessage());
        }
    }
}