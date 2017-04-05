package fr.rtwo.gpstracker;

import android.location.Location;

import java.util.LinkedList;
import java.util.List;

public class LocationDatabase {
    public abstract class Listener {
        abstract void onNewLocation(Location location);
        abstract void onLocationsCleared();
    }

    private static LocationDatabase ourInstance = new LocationDatabase();

    public static LocationDatabase getInstance() {
        return ourInstance;
    }

    private List<Listener> mListeners;
    private int mCount;

    private LocationDatabase() {
        mListeners = new LinkedList<Listener>();
        mCount = 0;
    }

    public boolean registerListener(Listener listener) {
        mListeners.add(listener);
        return true;
    }

    public boolean unregisterListener(Listener listener) {
        return mListeners.remove(listener);
    }

    public void addLocation(Location location) {
        mCount++;

        for (Listener e : mListeners) {
            e.onNewLocation(location);
        }
    }

    public int getCount() {
        return mCount;
    }

    public void clear() {
        mCount = 0;

        for (Listener e : mListeners) {
            e.onLocationsCleared();
        }
    }
}
