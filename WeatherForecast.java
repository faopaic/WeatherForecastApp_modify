public class WeatherForecast {
    private final String dateTime;
    private final String weather;
    private final String maxTemp;
    private final String minTemp;
    private final String pop; // 降水確率
    private final String windDirection; // 風向き
    private final String waveHeight; // 波高

    public WeatherForecast(String dateTime, String weather, String maxTemp, String minTemp, String pop,
            String windDirection, String waveHeight) {
        this.dateTime = dateTime;
        this.weather = weather;
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.pop = pop;
        this.windDirection = windDirection;
        this.waveHeight = waveHeight;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getWeather() {
        return weather;
    }

    public String getMaxTemp() {
        return maxTemp;
    }

    public String getMinTemp() {
        return minTemp;
    }

    public String getPop() {
        return pop;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public String getWaveHeight() {
        return waveHeight;
    }

    // 旧形式のコンストラクタ（互換性維持のため）
    public WeatherForecast(String dateTime, String weather, String maxTemp, String minTemp, String pop) {
        this(dateTime, weather, maxTemp, minTemp, pop, "-", "-");
    }
}