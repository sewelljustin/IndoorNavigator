package com.js.indoornavigator;


public class Beacon {

    private int quadrant;
    private String id;
    private String uuid;
    private int rssi;

    // active is used to determine if the beacon's packet is currently being received
    private boolean active;

    // Sample count is used to count the number of times a beacon is in a sample
    private int sampleCount;

    public Beacon(int quadrant, String id, String uuid) {
        setQuadrant(quadrant);
        setId(id);
        setUuid(uuid);
        setActive(false);
        sampleCount = 0;
    }

    public int getQuadrant() {
        return quadrant;
    }

    public void setQuadrant(int quadrant) {
        this.quadrant = quadrant;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void incrementSampleCount() {
        sampleCount++;
    }

    public void decrementSampleCount() {
        if (sampleCount == 0) {
            return;
        }
        sampleCount--;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public String toString() {
        return "ID: " + id + ", Sample Count: " + sampleCount + ". ";
    }
}
