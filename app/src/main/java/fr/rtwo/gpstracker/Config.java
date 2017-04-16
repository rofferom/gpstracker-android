package fr.rtwo.gpstracker;

public class Config {
    private static Config ourInstance = new Config();

    public static Config getInstance() {
        return ourInstance;
    }

    private Config() {
    }

    // GPS configuration
    public float mGpsAccuracy = 5.0f; // meters
    public long mGpsAcqPeriod = 120; // seconds
    public long mGpsAcqTimeout = 60; // seconds

    // Battery configuration
    public long mBatteryAcqPeriod = 120; // seconds
}
