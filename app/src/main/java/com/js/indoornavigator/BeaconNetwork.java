package com.js.indoornavigator;


public class BeaconNetwork {

    BeaconNode[] nodes;

    public BeaconNetwork() {

    }

    // Sets up the network
    public void initializeNetwork(Beacon[] beacons) {

        // Set up BeaconNode Array
        nodes = new BeaconNode[beacons.length];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new BeaconNode(beacons[i]);
        }

        // HARD CODED NETWORK
        // Beacon 1 - connected to beacon 2
        nodes[0].addNeighbor(nodes[1]);

        // Beacon 2 - connected to beacon 1, 3, and 4
        nodes[1].addNeighbor(nodes[0]);
        nodes[1].addNeighbor(nodes[2]);
        nodes[1].addNeighbor(nodes[3]);

        // Beacon 3 - connected to beacon 2
        nodes[2].addNeighbor(nodes[1]);

        // Beacon 4 - connected to beacon 2 and 5
        nodes[3].addNeighbor(nodes[1]);
        nodes[3].addNeighbor(nodes[4]);

        // Beacon 5 - connected to beacon 4
        nodes[4].addNeighbor(nodes[3]);
    }

    public Beacon[] getNeighborBeacons(Beacon beacon) {
        Beacon[] result = null;

        for (BeaconNode node : nodes) {
            if (node.getBeacon().equals(beacon)) {
                result =  node.getNeighbors();
            }
        }

        return result;
    }


}
