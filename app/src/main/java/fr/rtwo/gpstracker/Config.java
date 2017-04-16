package fr.rtwo.gpstracker;

public class Config {
    private static Config ourInstance = new Config();

    public static Config getInstance() {
        return ourInstance;
    }

    private Config() {
    }

    // GPS configuration
    public float mGpsAccuracy = 6.0f; // meters
    public long mGpsAcqPeriod = 60; // seconds
    public long mGpsAcqTimeout = 45; // seconds

    // Battery configuration
    public long mBatteryAcqPeriod = 120; // seconds
}
