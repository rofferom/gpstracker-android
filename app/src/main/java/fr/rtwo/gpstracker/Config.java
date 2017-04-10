package fr.rtwo.gpstracker;

public class Config {
    private static Config ourInstance = new Config();

    public static Config getInstance() {
        return ourInstance;
    }

    public float mGpsAccuracy = 10.0f; // meters
    public long mGpsAcqPeriod = 60; // seconds
    public long mGpsAcqTimeout = 30; // seconds

}
