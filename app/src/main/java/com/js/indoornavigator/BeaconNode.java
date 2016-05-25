package com.js.indoornavigator;

import java.util.ArrayList;

// Class BeaconNode is used to store beacons in a network
public class BeaconNode {

    Beacon beacon;
    ArrayList<BeaconNode> neighborNodes;

    public BeaconNode(Beacon beacon) {
        this.beacon = beacon;
        neighborNodes = new ArrayList<>();
    }

    public BeaconNode() {

    }

    public void setBeacon(Beacon beacon) {
        this.beacon = beacon;
    }

    public Beacon getBeacon() {
        return beacon;
    }

    public void addNeighbor(BeaconNode node) {
        neighborNodes.add(node);
    }

    public Beacon[] getNeighbors() {
        Beacon[] result = new Beacon[neighborNodes.size()];
        for (int i = 0; i < neighborNodes.size(); i++) {
            result[i] = neighborNodes.get(i).getBeacon();
        }

        return result;
    }

}
