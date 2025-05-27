public class WeatherForecast {
    private final String dateTime;
    private final String weather;
    private final String maxTemp;
    private final String minTemp;
    private final String pop; // 降水確率

    public WeatherForecast(String dateTime, String weather, String maxTemp, String minTemp, String pop) {
        this.dateTime = dateTime;
        this.weather = weather;
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.pop = pop;
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
}