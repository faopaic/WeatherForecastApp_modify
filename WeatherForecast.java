
public class WeatherForecast {
    private final String dateTime;
    private final String weather;

    public WeatherForecast(String dateTime, String weather) {
        this.dateTime = dateTime;
        this.weather = weather;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getWeather() {
        return weather;
    }
}